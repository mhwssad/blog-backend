package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileChunk;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.enums.storage.UploadModeEnum;
import com.cybzacg.blogbackend.module.file.convert.FileModelMapper;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadService;
import com.cybzacg.blogbackend.utils.BeanConverterUtils;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件上传服务实现。
 * 负责上传任务初始化、秒传检测、整文件上传、分片上传与合并。
 */
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private static final int CHUNK_STATUS_COMPLETED = 2;

    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileLifecycleService fileLifecycleService;
    private final StorageManager storageManager;
    private final FileUploadProperties fileUploadProperties;
    private final FileModelMapper fileModelMapper;

    @Override
    public UserTaskVO initUploadTask(Long userId, UserUploadInitRequest request) {
        validateInitRequest(request);
        String md5 = FileUtils.normalizeMd5(request.getFileMd5());
        Long fileSize = request.getFileSize();
        String storageKey = storageManager.getCurrentStorageKey();
        ExceptionThrowerCore.throwBusinessIfBlank(storageKey, FileResultCode.STORAGE_NODE_NOT_CONFIGURED);
        boolean chunked = isChunked(request);
        long chunkSize = resolveChunkSize(request);
        int totalChunks = chunked ? resolveTotalChunks(request, chunkSize) : 0;

        FileUploadTask task = new FileUploadTask();
        task.setUploadId(UUID.randomUUID().toString().replace("-", ""));
        task.setUploadUserId(userId);
        task.setStorageKey(storageKey);
        task.setSourceIp(null);
        task.setFileMd5(md5);
        task.setFileSize(fileSize);
        task.setOriginalName(request.getOriginalName());
        task.setMimeType(StringUtils.hasText(request.getMimeType()) ? request.getMimeType() : null);
        task.setReferenceType(FileReferenceTypeEnum.normalize(request.getReferenceType()));
        task.setReferenceId(request.getReferenceId() == null ? 0L : request.getReferenceId());
        task.setCategory(FileCategoryEnum.normalize(request.getCategory()));
        task.setIsPublic(CollectionUtils.defaultInt(request.getIsPublic(), 0));
        task.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        task.setIsChunked(chunked ? 1 : 0);
        task.setChunkSize(chunked ? chunkSize : null);
        task.setTotalChunks(chunked ? totalChunks : null);
        task.setUploadedChunks(0);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        task.setRetryCount(0);
        task.setStartTime(LocalDateTime.now());
        task.setExpireTime(expireAfterDays(fileUploadProperties.getTaskExpireDays()));
        fileUploadTaskRepository.save(task);

        UserTaskVO vo = new UserTaskVO();
        vo.setTaskId(task.getId());
        vo.setUploadId(task.getUploadId());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());

        FileInfo existing = tryFindExistingFile(md5, fileSize);
        if (existing != null) {
            UserTaskVO result = finalizeTaskWithReference(task, existing, true);
            vo.setUploadMode(UploadModeEnum.QUICK_UPLOAD.getValue());
            vo.setQuickUploadAvailable(true);
            vo.setCompleted(true);
            vo.setTaskStatus(result.getTaskStatus());
            vo.setFileId(result.getFileId());
            return vo;
        }
        vo.setQuickUploadAvailable(md5 != null);
        vo.setCompleted(false);
        vo.setUploadMode(chunked ? UploadModeEnum.CHUNKED_UPLOAD.getValue() : UploadModeEnum.FULL_UPLOAD.getValue());
        vo.setTaskStatus(task.getTaskStatus());
        return vo;
    }

    @Override
    public UserTaskVO quickCheck(Long userId, String md5) {
        FileUploadTask task = fileUploadTaskRepository.findByUploadIdAndUserId(md5, userId);
        ExceptionThrowerCore.throwBusinessIf(task == null, FileResultCode.UPLOAD_TASK_NOT_FOUND);
        if (fileLifecycleService.expireTaskIfNeeded(task)) {
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.UPLOAD_TASK_EXPIRED);
        }
        if (TaskStatusEnum.COMPLETED.getValue().equals(task.getTaskStatus()) && task.getFileId() != null) {
            FileInfo fileInfo = fileInfoRepository.getById(task.getFileId());
            Long businessId = resolveBusinessId(task.getFileId(), task.getUploadUserId(), task.getReferenceType(), task.getReferenceId());
            return toTaskVO(task, fileInfo, businessId);
        }
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        FileInfo existing = tryFindExistingFile(FileUtils.normalizeMd5(task.getFileMd5()), task.getFileSize());
        if (existing == null) {
            UserTaskVO vo = fileModelMapper.toUserTaskVO(task);
            vo.setQuickUpload(false);
            return vo;
        }
        return finalizeTaskWithReference(task, existing, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO uploadFile(Long userId, String md5, String taskId, FileInfo fileInfo, InputStream inputStream) {
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.CHUNK_TASK_REQUIRED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);

        String normalizedMd5 = FileUtils.normalizeMd5(md5);
        FileInfo existing = tryFindExistingFile(normalizedMd5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, true);
        }

        String objectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        StorageService storageService = requireStorageService(task.getStorageKey());
        String url;
        try {
            url = storageService.upload(inputStream, objectName, fileInfo.getMimeType());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.FILE_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_UPLOAD_FAILED);
            return null; // unreachable, but satisfies compiler
        }

        PersistedFileInfo persisted = createOrReuseFileInfo(task, objectName, url, normalizedMd5);
        return finalizeTaskWithReference(task, persisted.fileInfo(), persisted.reusedExisting());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO uploadChunk(Long userId, String taskId, Integer chunkIndex, FileInfo chunkFileInfo, InputStream inputStream) {
        ExceptionThrowerCore.throwBusinessIf(chunkIndex == null || chunkIndex < 1, FileResultCode.CHUNK_NUMBER_INVALID);
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || chunkIndex > task.getTotalChunks(), FileResultCode.CHUNK_NUMBER_EXCEEDED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);

        StorageService storageService = requireStorageService(task.getStorageKey());
        String chunkObjectName = task.getUploadId() + "/chunk-" + chunkIndex + ".part";
        try {
            storageService.uploadToTemp(inputStream, chunkObjectName, chunkFileInfo.getMimeType());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.CHUNK_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.CHUNK_UPLOAD_FAILED);
        }

        try {
            upsertChunkRecord(task.getId(), chunkIndex, chunkFileInfo.getFileSize(), chunkFileInfo.getMd5());
            int uploadedChunks = countUploadedChunks(task.getId());
            task.setUploadedChunks(uploadedChunks);
            fileUploadTaskRepository.updateById(task);

            UserTaskVO vo = fileModelMapper.toUserTaskVO(task);
            vo.setUploadId(task.getUploadId());
            vo.setChunkNumber(chunkIndex);
            vo.setUploadedChunks(uploadedChunks);
            vo.setTotalChunks(task.getTotalChunks());
            return vo;
        } catch (RuntimeException e) {
            cleanupUploadedObject(task.getStorageKey(), buildTempChunkObjectName(task.getUploadId(), chunkIndex));
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO completeUpload(Long userId, String taskId) {
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        assertTaskActionAllowed(task, TaskStatusEnum.UPLOADING, TaskStatusEnum.MERGING, TaskStatusEnum.FAILED);
        int uploaded = countUploadedChunks(task.getId());
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || uploaded < task.getTotalChunks(), FileResultCode.CHUNK_INCOMPLETE);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.MERGING);

        String md5 = FileUtils.normalizeMd5(task.getFileMd5());
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StringUtils.hasText(md5), FileResultCode.FILE_MD5_REQUIRED, "缺少文件MD5，无法完成上传");

        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, true);
        }

        StorageService storageService = requireStorageService(task.getStorageKey());
        String targetObjectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        List<String> sourceObjectNames = buildChunkObjectNames(task.getUploadId(), task.getTotalChunks());

        try {
            storageService.mergeFiles(sourceObjectNames, targetObjectName);
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.CHUNK_MERGE_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.CHUNK_MERGE_FAILED);
        }

        try {
            String url = storageService.getUrl(targetObjectName);
            PersistedFileInfo persisted = createOrReuseFileInfo(task, targetObjectName, url, md5);
            return finalizeTaskWithReference(task, persisted.fileInfo(), persisted.reusedExisting());
        } catch (RuntimeException e) {
            cleanupUploadedObject(task.getStorageKey(), targetObjectName);
            throw e;
        }
    }

    private FileUploadTask getTaskOrThrow(String uploadId, Long userId) {
        ExceptionThrowerCore.throwBusinessIfBlank(uploadId, FileResultCode.UPLOAD_ID_REQUIRED);
        FileUploadTask task = ExceptionThrowerCore.require(
                () -> fileUploadTaskRepository.findByUploadIdAndUserId(uploadId, userId),
                FileResultCode.UPLOAD_TASK_NOT_FOUND
        );
        if (fileLifecycleService.expireTaskIfNeeded(task)) {
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.UPLOAD_TASK_EXPIRED);
        }
        return task;
    }

    private StorageService requireStorageService(String storageKey) {
        StorageService storageService = storageManager.getStorageService(storageKey);
        ExceptionThrowerCore.throwBusinessIf(storageService == null, FileResultCode.STORAGE_NODE_UNAVAILABLE, "存储节点不可用: " + storageKey);
        return storageService;
    }

    private FileInfo tryFindExistingFile(String md5, Long fileSize) {
        if (!StringUtils.hasText(md5)) {
            return null;
        }
        FileInfo file = fileInfoRepository.findByMd5AndStatus(md5, FileStatusEnum.NORMAL.getValue());
        if (file == null) {
            return null;
        }
        if (fileSize != null && file.getFileSize() != null && !fileSize.equals(file.getFileSize())) {
            return null;
        }
        return file;
    }

    private FileInfo findFileByMd5(String md5) {
        if (!StringUtils.hasText(md5)) {
            return null;
        }
        return fileInfoRepository.findByMd5(md5);
    }

    private void assertTaskActionAllowed(FileUploadTask task, TaskStatusEnum... allowedStatuses) {
        TaskStatusEnum current = TaskStatusEnum.getByCode(task.getTaskStatus());
        ExceptionThrowerCore.throwBusinessIf(current == null, FileResultCode.UPLOAD_TASK_STATUS_INVALID);
        for (TaskStatusEnum allowedStatus : allowedStatuses) {
            if (current == allowedStatus) {
                return;
            }
        }
        ExceptionThrowerCore.throwBusinessEx(FileResultCode.UPLOAD_TASK_STATUS_INVALID);
    }

    private void updateTaskStatusIfNeeded(FileUploadTask task, TaskStatusEnum target) {
        TaskStatusEnum current = TaskStatusEnum.getByCode(task.getTaskStatus());
        if (current == null) {
            task.setTaskStatus(target.getValue());
            fileUploadTaskRepository.updateById(task);
            return;
        }
        if (!current.equals(target) && current.canTransitionTo(target)) {
            task.setTaskStatus(target.getValue());
            fileUploadTaskRepository.updateById(task);
        }
    }

    private UserTaskVO finalizeTaskWithReference(FileUploadTask task, FileInfo fileInfo, boolean quick) {
        FileBusinessInfo ref = createOrGetBusinessReference(task, fileInfo.getId(), null);
        fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), Integer.valueOf(1).equals(task.getIsPublic()));
        fileInfo = fileInfoRepository.getById(fileInfo.getId());
        task.setIsQuickUpload(quick ? 1 : CollectionUtils.defaultInt(task.getIsQuickUpload(), 0));
        if (quick) {
            task.setReferencedFileId(fileInfo.getId());
            task.setQuickUploadTime(LocalDateTime.now());
        }
        task.setFileId(fileInfo.getId());
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        fileUploadTaskRepository.updateById(task);
        cleanupChunkArtifacts(task);
        return toTaskVO(task, fileInfo, ref == null ? null : ref.getId());
    }

    private UserTaskVO toTaskVO(FileUploadTask task, FileInfo fileInfo, Long businessId) {
        UserTaskVO vo = fileModelMapper.toUserTaskVO(task);
        vo.setFileId(fileInfo == null ? null : fileInfo.getId());
        vo.setBusinessId(businessId);
        vo.setFileUrl(fileInfo == null ? null : fileInfo.getFileUrl());
        return vo;
    }

    private PersistedFileInfo createOrReuseFileInfo(FileUploadTask task, String objectName, String url, String md5) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setUploadTaskId(task.getId());
        fileInfo.setFileName(extractFileName(objectName));
        fileInfo.setOriginalName(task.getOriginalName());
        fileInfo.setFilePath(objectName);
        fileInfo.setStorageKey(task.getStorageKey());
        fileInfo.setFileUrl(url);
        fileInfo.setFileSize(task.getFileSize());
        fileInfo.setMimeType(task.getMimeType());
        fileInfo.setFileType(resolveFileType(task.getMimeType()));
        fileInfo.setFileExtension(FileUtils.normalizeExt(FileUtils.getExtension(task.getOriginalName())));
        fileInfo.setMd5(md5);
        fileInfo.setReferenceCount(0);
        fileInfo.setIsPublic(CollectionUtils.defaultInt(task.getIsPublic(), 0));
        fileInfo.setCategory(FileCategoryEnum.normalize(task.getCategory()));
        fileInfo.setDownloadCount(0);
        fileInfo.setUploadUserId(task.getUploadUserId());
        fileInfo.setRemark(task.getRemark());
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        try {
            fileInfoRepository.save(fileInfo);
            return new PersistedFileInfo(fileInfo, false);
        } catch (DuplicateKeyException e) {
            return handleFileInfoConflict(task, fileInfo, objectName, md5, e);
        }
    }

    private PersistedFileInfo handleFileInfoConflict(FileUploadTask task, FileInfo candidate, String objectName, String md5, DuplicateKeyException ex) {
        FileInfo existing = findFileByMd5(md5);
        if (existing == null || (task.getFileSize() != null && existing.getFileSize() != null && !task.getFileSize().equals(existing.getFileSize()))) {
            cleanupUploadedObject(task.getStorageKey(), objectName);
            throw ex;
        }
        if (FileStatusEnum.DELETED.getValue().equals(existing.getStatus())) {
            existing.setUploadTaskId(task.getId());
            existing.setFileName(candidate.getFileName());
            existing.setOriginalName(candidate.getOriginalName());
            existing.setFilePath(candidate.getFilePath());
            existing.setStorageKey(candidate.getStorageKey());
            existing.setFileUrl(candidate.getFileUrl());
            existing.setFileSize(candidate.getFileSize());
            existing.setFileType(candidate.getFileType());
            existing.setMimeType(candidate.getMimeType());
            existing.setFileExtension(candidate.getFileExtension());
            existing.setReferenceCount(0);
            existing.setIsPublic(candidate.getIsPublic());
            existing.setCategory(candidate.getCategory());
            existing.setDownloadCount(0);
            existing.setUploadUserId(candidate.getUploadUserId());
            existing.setRemark(candidate.getRemark());
            existing.setStatus(FileStatusEnum.NORMAL.getValue());
            fileInfoRepository.updateById(existing);
            return new PersistedFileInfo(existing, false);
        }
        cleanupUploadedObject(task.getStorageKey(), objectName);
        return new PersistedFileInfo(existing, true);
    }

    private void cleanupUploadedObject(String storageKey, String objectName) {
        if (!StringUtils.hasText(storageKey) || !StringUtils.hasText(objectName)) {
            return;
        }
        try {
            StorageService storageService = storageManager.getStorageService(storageKey);
            if (storageService != null) {
                storageService.delete(objectName);
            }
        } catch (Exception ignored) {
        }
    }

    private FileBusinessInfo createOrGetBusinessReference(FileUploadTask task, Long fileId, String sourceIp) {
        String referenceType = FileReferenceTypeEnum.normalize(task.getReferenceType());
        long referenceId = task.getReferenceId() == null ? 0L : task.getReferenceId();
        Long userId = task.getUploadUserId();
        FileBusinessInfo existing = fileBusinessInfoRepository.findByFileUserReference(fileId, userId, referenceType, referenceId);
        if (existing != null) {
            return existing;
        }
        FileBusinessInfo ref = BeanConverterUtils.convert(task, FileBusinessInfo::new, "id", "uploadTaskId", "fileMd5", "fileSize", "mimeType",
                "referenceType", "referenceId", "isPublic", "category", "remark", "uploadUserId");
        ref.setFileId(fileId);
        ref.setUserId(userId);
        ref.setReferenceType(referenceType);
        ref.setReferenceId(referenceId);
        ref.setSourceIp(sourceIp);
        ref.setIsPublic(CollectionUtils.defaultInt(task.getIsPublic(), 0));
        ref.setCategory(FileCategoryEnum.normalize(task.getCategory()));
        ref.setRemark(task.getRemark());
        try {
            fileBusinessInfoRepository.save(ref);
        } catch (DuplicateKeyException e) {
            return fileBusinessInfoRepository.findByFileUserReference(fileId, userId, referenceType, referenceId);
        }
        return ref;
    }

    private Long resolveBusinessId(Long fileId, Long userId, String referenceType, Long referenceId) {
        if (fileId == null) {
            return null;
        }
        String type = FileReferenceTypeEnum.normalize(referenceType);
        long rid = referenceId == null ? 0L : referenceId;
        FileBusinessInfo ref = fileBusinessInfoRepository.findLatestByFileUserReference(fileId, userId, type, rid);
        return ref == null ? null : ref.getId();
    }

    private void markTaskFailed(FileUploadTask task, FileResultCode resultCode, String message) {
        task.setTaskStatus(TaskStatusEnum.FAILED.getValue());
        task.setErrorCode(resultCode == null ? null : String.valueOf(resultCode.getCode()));
        task.setErrorMessage(FileUtils.truncate(StringUtils.hasText(message) ? message : (resultCode == null ? null : resultCode.getMessage()), 240));
        fileUploadTaskRepository.updateById(task);
    }

    private void cleanupChunkArtifacts(FileUploadTask task) {
        if (!Integer.valueOf(1).equals(task.getIsChunked()) || task.getId() == null) {
            return;
        }
        fileChunkRepository.deleteByUploadTaskId(task.getId());
        if (!StringUtils.hasText(task.getUploadId())) {
            return;
        }
        try {
            StorageService storageService = storageManager.getStorageService(task.getStorageKey());
            if (storageService != null) {
                storageService.deleteTempFiles(task.getUploadId());
            }
        } catch (Exception ignored) {
        }
    }

    private void upsertChunkRecord(Long taskId, Integer chunkNumber, long size, String md5) {
        FileChunk chunk = fileChunkRepository.findByTaskIdAndChunkNumber(taskId, chunkNumber);
        if (chunk == null) {
            chunk = new FileChunk();
            chunk.setUploadTaskId(taskId);
            chunk.setChunkNumber(chunkNumber);
            chunk.setChunkSize(size);
            chunk.setChunkMd5(md5);
            chunk.setUploadStatus(CHUNK_STATUS_COMPLETED);
            chunk.setRetryCount(0);
            chunk.setUploadTime(LocalDateTime.now());
            fileChunkRepository.save(chunk);
            return;
        }
        chunk.setChunkSize(size);
        chunk.setChunkMd5(md5);
        chunk.setUploadStatus(CHUNK_STATUS_COMPLETED);
        chunk.setUploadTime(LocalDateTime.now());
        fileChunkRepository.updateById(chunk);
    }

    private int countUploadedChunks(Long taskId) {
        return Math.toIntExact(fileChunkRepository.countByTaskIdAndStatus(taskId, CHUNK_STATUS_COMPLETED));
    }

    private List<String> buildChunkObjectNames(String uploadId, Integer totalChunks) {
        int total = totalChunks == null ? 0 : totalChunks;
        if (total <= 0) {
            return List.of();
        }
        String prefix = fileUploadProperties.getTempDirPrefix();
        List<String> names = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            names.add(prefix + "/" + uploadId + "/chunk-" + i + ".part");
        }
        return names;
    }

    private String buildTempChunkObjectName(String uploadId, Integer chunkNumber) {
        return fileUploadProperties.getTempDirPrefix() + "/" + uploadId + "/chunk-" + chunkNumber + ".part";
    }

    private void validateInitRequest(UserUploadInitRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, FileResultCode.INIT_REQUEST_EMPTY);
        ExceptionThrowerCore.throwBusinessIf(request.getFileSize() == null || request.getFileSize() <= 0, FileResultCode.FILE_SIZE_INVALID);
        if (fileUploadProperties.getMaxFileSize() != null && request.getFileSize() > fileUploadProperties.getMaxFileSize()) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.MAX_UPLOAD_SIZE_EXCEEDED);
        }
        String ext = FileUtils.getExtension(request.getOriginalName());
        ExceptionThrowerCore.throwBusinessIfNot(isAllowedExtension(ext), FileResultCode.FILE_EXTENSION_NOT_ALLOWED);
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StringUtils.hasText(request.getFileMd5()), FileResultCode.FILE_MD5_REQUIRED);
        validateVisibility(request.getIsPublic());
        validateChunkRequest(request);
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(request.getReferenceType()) && !FileReferenceTypeEnum.contains(request.getReferenceType()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件引用类型非法"
        );
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(request.getCategory()) && !FileCategoryEnum.contains(request.getCategory()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件分类非法"
        );
    }

    private boolean isAllowedExtension(String ext) {
        if (!StringUtils.hasText(ext)) {
            return true;
        }
        List<String> allowed = fileUploadProperties.getAllowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        for (String item : allowed) {
            if (StringUtils.hasText(item) && normalized.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private void validateVisibility(Integer isPublic) {
        ExceptionThrowerCore.throwBusinessIf(
                isPublic != null && !Integer.valueOf(0).equals(isPublic) && !Integer.valueOf(1).equals(isPublic),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件可见性非法"
        );
    }

    private void validateChunkRequest(UserUploadInitRequest request) {
        boolean hasExplicitChunkConfig = request.getTotalChunks() != null || request.getChunkSize() != null;
        ExceptionThrowerCore.throwBusinessIf(
                request.getTotalChunks() != null && request.getTotalChunks() <= 1,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "分片总数必须大于1"
        );
        ExceptionThrowerCore.throwBusinessIf(
                request.getChunkSize() != null && request.getChunkSize() <= 0,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "分片大小必须大于0"
        );
        ExceptionThrowerCore.throwBusinessIf(
                hasExplicitChunkConfig && (request.getTotalChunks() == null || request.getChunkSize() == null),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "分片参数不完整"
        );
    }

    private boolean isChunked(UserUploadInitRequest request) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 1) {
            return true;
        }
        long threshold = fileUploadProperties.getChunkSizeThreshold() == null ? 0L : fileUploadProperties.getChunkSizeThreshold();
        return threshold > 0 && request.getFileSize() != null && request.getFileSize() > threshold;
    }

    private long resolveChunkSize(UserUploadInitRequest request) {
        if (request.getChunkSize() != null && request.getChunkSize() > 0) {
            return request.getChunkSize();
        }
        return fileUploadProperties.getChunkSize() == null ? 5242880L : fileUploadProperties.getChunkSize();
    }

    private int resolveTotalChunks(UserUploadInitRequest request, long chunkSize) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 0) {
            return request.getTotalChunks();
        }
        long fileSize = request.getFileSize() == null ? 0L : request.getFileSize();
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    private String buildFinalObjectName(String category, String originalName) {
        String normalizedCategory = FileCategoryEnum.normalize(category);
        String ext = FileUtils.normalizeExt(FileUtils.getExtension(originalName));
        String datePath = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = StringUtils.hasText(ext) ? ("." + ext) : "";
        return normalizedCategory + "/" + datePath + "/" + uuid + suffix;
    }

    private String extractFileName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return objectName;
        }
        int idx = objectName.lastIndexOf('/');
        return idx < 0 ? objectName : objectName.substring(idx + 1);
    }

    private String resolveFileType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "other";
        }
        if (mimeType.startsWith("image/")) {
            return "image";
        }
        if (mimeType.startsWith("video/")) {
            return "video";
        }
        if (mimeType.startsWith("audio/")) {
            return "audio";
        }
        if (mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("excel") || mimeType.contains("text")) {
            return "document";
        }
        return "other";
    }

    private LocalDateTime expireAfterDays(Integer days) {
        int d = days == null ? 2 : Math.max(1, days);
        return LocalDateTime.now().plusDays(d);
    }

    private record PersistedFileInfo(FileInfo fileInfo, boolean reusedExisting) {
    }
}

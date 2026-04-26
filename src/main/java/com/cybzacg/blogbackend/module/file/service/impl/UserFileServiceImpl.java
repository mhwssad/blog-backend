package com.cybzacg.blogbackend.module.file.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.enums.storage.UploadModeEnum;
import com.cybzacg.blogbackend.module.file.convert.FileModelMapper;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户文件服务实现。
 * 负责上传任务初始化、秒传检测、整文件上传、分片上传与用户侧文件引用维护。
 */
@Service
@RequiredArgsConstructor
public class UserFileServiceImpl implements UserFileService {
    private static final int CHUNK_STATUS_COMPLETED = 2;
    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileLifecycleService fileLifecycleService;
    private final StorageManager storageManager;
    private final FileUploadProperties fileUploadProperties;
    private final FileModelMapper fileModelMapper;

    /**
     * 初始化上传任务，并根据文件指纹预判是否可以直接走秒传。
     * 无论是否秒传命中，都会先固化一条上传任务，保证客户端后续轮询、审计与异常补偿都能基于同一条任务链路展开。
     * 若已存在同 MD5 且大小匹配的物理文件，则当前任务会直接转为完成态并补建业务引用；否则返回整文件或分片上传所需上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadInitVO initUploadTask(FileUploadInitRequest request, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        validateInitRequest(request);
        String md5 = FileUtils.normalizeMd5(request.getFileMd5());
        Long fileSize = request.getFileSize();
        String storageKey = storageManager.getCurrentStorageKey();
        ExceptionThrowerCore.throwBusinessIfBlank(storageKey, FileResultCode.STORAGE_NODE_NOT_CONFIGURED);
        boolean chunked = isChunked(request);
        long chunkSize = resolveChunkSize(request);
        int totalChunks = chunked ? resolveTotalChunks(request, chunkSize) : 0;
        // 先固化本次上传上下文，后续即使秒传命中也能完整追踪用户操作链路。
        FileUploadTask task = new FileUploadTask();
        task.setUploadId(UUID.randomUUID().toString().replace("-", ""));
        task.setUploadUserId(userId);
        task.setStorageKey(storageKey);
        task.setSourceIp(sourceIp);
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
        FileUploadInitVO vo = new FileUploadInitVO();
        vo.setTaskId(task.getId());
        vo.setUploadId(task.getUploadId());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());
        // 优先复用已有物理文件，避免相同内容重复上传与重复落库。
        FileInfo existing = tryFindExistingFile(md5, fileSize);
        if (existing != null) {
            FileUploadResultVO result = finalizeTaskWithReference(task, existing, sourceIp, true);
            vo.setUploadMode(UploadModeEnum.QUICK_UPLOAD.getValue());
            vo.setQuickUploadAvailable(true);
            vo.setCompleted(true);
            vo.setTaskStatus(result.getTaskStatus());
            vo.setFileId(result.getFileId());
            vo.setFileUrl(result.getFileUrl());
            vo.setBusinessId(result.getBusinessId());
            return vo;
        }
        vo.setQuickUploadAvailable(md5 != null);
        vo.setCompleted(false);
        vo.setUploadMode(chunked ? UploadModeEnum.CHUNKED_UPLOAD.getValue() : UploadModeEnum.FULL_UPLOAD.getValue());
        vo.setTaskStatus(task.getTaskStatus());
        return vo;
    }

    /**
     * 针对已初始化任务再次执行秒传检查，兼容客户端稍后补发检测请求的场景。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO quickCheck(String uploadId, String sourceIp) {
        FileUploadTask task = getTaskOrThrow(uploadId);
        // 已完成任务直接回放结果，避免重复创建业务引用。
        if (TaskStatusEnum.COMPLETED.getValue().equals(task.getTaskStatus()) && task.getFileId() != null) {
            FileInfo fileInfo = fileInfoRepository.getById(task.getFileId());
            Long businessId = resolveBusinessId(task.getFileId(), task.getUploadUserId(), task.getReferenceType(), task.getReferenceId());
            return toResultVO(task, fileInfo, businessId);
        }
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        // 秒传检测只依赖文件指纹与大小，不要求客户端重新上传文件体。
        FileInfo existing = tryFindExistingFile(FileUtils.normalizeMd5(task.getFileMd5()), task.getFileSize());
        if (existing == null) {
            FileUploadResultVO vo = fileModelMapper.toFileUploadResultVO(task);
            vo.setQuickUpload(false);
            return vo;
        }
        return finalizeTaskWithReference(task, existing, sourceIp, true);
    }

    /**
     * 处理普通整文件上传，在落存储前完成 MD5 校验与重复文件复用判断。
     * 该方法先尝试复用同指纹文件，未命中时才真正写入存储；若落库阶段因唯一键竞争发现同 MD5 文件已被并发请求创建，
     * 会回收本次多上传的对象并切换到已有记录，避免重复物理文件和脏元数据残留。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO uploadFile(String uploadId, MultipartFile file, String sourceIp) {
        ExceptionThrowerCore.throwBusinessIf(file == null || file.isEmpty(), FileResultCode.UPLOAD_FILE_EMPTY);
        FileUploadTask task = getTaskOrThrow(uploadId);
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.CHUNK_TASK_REQUIRED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);
        // 单文件上传在落存储前先补齐并校验 MD5，确保秒传判断与最终落库一致。
        String md5 = resolveAndValidateMd5(task, file);
        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, sourceIp, true);
        }
        String objectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        StorageService storageService = requireStorageService(task.getStorageKey());
        String url = null;
        try (InputStream inputStream = file.getInputStream()) {
            url = storageService.upload(inputStream, objectName, task.getMimeType());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.FILE_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_UPLOAD_FAILED);
        }
        try {
            PersistedFileInfo persisted = createOrReuseFileInfo(task, objectName, url, md5);
            return finalizeTaskWithReference(task, persisted.fileInfo(), sourceIp, persisted.reusedExisting());
        } catch (RuntimeException e) {
            cleanupUploadedObject(task.getStorageKey(), objectName);
            throw e;
        }
    }

    /**
     * 上传单个分片并更新分片进度，为断点续传和最终合并提供元数据基础。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file, String chunkMd5, String sourceIp) {
        ExceptionThrowerCore.throwBusinessIf(file == null || file.isEmpty(), FileResultCode.CHUNK_FILE_EMPTY);
        ExceptionThrowerCore.throwBusinessIf(chunkNumber == null || chunkNumber < 1, FileResultCode.CHUNK_NUMBER_INVALID);
        FileUploadTask task = getTaskOrThrow(uploadId);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || chunkNumber > task.getTotalChunks(), FileResultCode.CHUNK_NUMBER_EXCEEDED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);
        if (Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && StringUtils.hasText(chunkMd5)) {
            String computed = md5Hex(file);
            ExceptionThrowerCore.throwBusinessIfNot(chunkMd5.trim().equalsIgnoreCase(computed), FileResultCode.CHUNK_MD5_MISMATCH);
        }
        StorageService storageService = requireStorageService(task.getStorageKey());
        // 分片统一写入临时目录，完成合并前不占用正式文件路径。
        String chunkObjectName = task.getUploadId() + "/chunk-" + chunkNumber + ".part";
        try (InputStream inputStream = file.getInputStream()) {
            storageService.uploadToTemp(inputStream, chunkObjectName, file.getContentType());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.CHUNK_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.CHUNK_UPLOAD_FAILED);
        }
        try {
            // 分片记录既用于断点续传，也用于最终合并前的完整性校验。
            upsertChunkRecord(task.getId(), chunkNumber, file.getSize(), FileUtils.normalizeMd5(chunkMd5));
            int uploadedChunks = countUploadedChunks(task.getId());
            task.setUploadedChunks(uploadedChunks);
            fileUploadTaskRepository.updateById(task);
            ChunkUploadVO vo = new ChunkUploadVO();
            vo.setUploadId(task.getUploadId());
            vo.setChunkNumber(chunkNumber);
            vo.setUploadedChunks(uploadedChunks);
            vo.setTotalChunks(task.getTotalChunks());
            vo.setTaskStatus(task.getTaskStatus());
            return vo;
        } catch (RuntimeException e) {
            cleanupUploadedObject(task.getStorageKey(), buildTempChunkObjectName(task.getUploadId(), chunkNumber));
            throw e;
        }
    }

    /**
     * 校验分片完整性并执行合并，必要时复用已存在的同指纹物理文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO completeUpload(String uploadId, String sourceIp) {
        FileUploadTask task = getTaskOrThrow(uploadId);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        assertTaskActionAllowed(task, TaskStatusEnum.UPLOADING, TaskStatusEnum.MERGING, TaskStatusEnum.FAILED);
        int uploaded = countUploadedChunks(task.getId());
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || uploaded < task.getTotalChunks(), FileResultCode.CHUNK_INCOMPLETE);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.MERGING);
        String md5 = FileUtils.normalizeMd5(task.getFileMd5());
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StringUtils.hasText(md5), FileResultCode.FILE_MD5_REQUIRED, "缺少文件MD5，无法完成上传");
        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, sourceIp, true);
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
            return finalizeTaskWithReference(task, persisted.fileInfo(), sourceIp, persisted.reusedExisting());
        } catch (RuntimeException e) {
            cleanupUploadedObject(task.getStorageKey(), targetObjectName);
            throw e;
        }
    }

    /**
     * 分页查询当前用户的文件引用，并按关键字回填物理文件信息。
     */
    @Override
    public PageResult<UserFileVO> pageMyFiles(UserFilePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        validateUserFileQuery(query);
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        Set<Long> fileIds = null;
        if (StringUtils.hasText(query.getKeyword()) || query.getStatus() != null) {
            fileIds = fileInfoRepository.findIdsByStatusAndKeyword(query.getStatus(), query.getKeyword());
            if (fileIds.isEmpty()) {
                return PageResult.<UserFileVO>builder().total(0L).current(current).size(size).records(List.of()).build();
            }
        }
        query.setCurrent(current);
        query.setSize(size);
        Page<FileBusinessInfo> page = fileBusinessInfoRepository.pageByUserAndFilters(userId, query, fileIds);
        Map<Long, FileInfo> fileMap = loadFileInfoMap(page.getRecords().stream().map(FileBusinessInfo::getFileId).collect(Collectors.toSet()));
        List<UserFileVO> records = page.getRecords().stream()
                .map(ref -> toUserFileVO(ref, fileMap.get(ref.getFileId())))
                .filter(Objects::nonNull)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 分页查询当前用户的上传任务，便于展示上传进度与异常状态。
     */
    @Override
    public PageResult<UserFileTaskVO> pageMyUploadTasks(UserFileTaskPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        validateTaskPageQuery(query.getTaskStatus());
        query.setCurrent(query.getCurrent() == null ? 1L : query.getCurrent());
        query.setSize(query.getSize() == null ? 10L : query.getSize());
        Page<FileUploadTask> page = fileUploadTaskRepository.pageByUserAndStatus(userId, query);
        List<UserFileTaskVO> records = page.getRecords().stream().map(this::toUserTaskVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 删除用户自己的业务引用，并在最后一个引用消失时回收物理文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyFile(Long businessId) {
        Long userId = SecurityUtils.requireUserId();
        FileBusinessInfo ref = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIf(ref == null || !userId.equals(ref.getUserId()), FileResultCode.FILE_REFERENCE_NOT_FOUND);
        Long fileId = ref.getFileId();
        fileBusinessInfoRepository.removeById(businessId);
        fileLifecycleService.syncFileAfterReferenceRemoval(fileId);
    }

    /**
     * 校验上传初始化请求的尺寸、扩展名与指纹约束。
     */
    private void validateInitRequest(FileUploadInitRequest request) {
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

    /**
     * 校验用户文件列表筛选参数，避免非法枚举值被静默归一化成默认值。
     */
    private void validateUserFileQuery(UserFilePageQuery query) {
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(query.getCategory()) && !FileCategoryEnum.contains(query.getCategory()),
                ResultErrorCode.ILLEGAL_ARGUMENT
        );
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(query.getReferenceType()) && !FileReferenceTypeEnum.contains(query.getReferenceType()),
                ResultErrorCode.ILLEGAL_ARGUMENT
        );
        ExceptionThrowerCore.throwBusinessIf(
                query.getStatus() != null && !FileStatusEnum.contains(query.getStatus()),
                FileResultCode.FILE_STATUS_INVALID
        );
    }

    /**
     * 校验任务列表状态筛选值，避免无效状态被当成空结果吞掉。
     */
    private void validateTaskPageQuery(Integer taskStatus) {
        ExceptionThrowerCore.throwBusinessIf(
                taskStatus != null && TaskStatusEnum.getByCode(taskStatus) == null,
                FileResultCode.UPLOAD_TASK_STATUS_INVALID
        );
    }

    /**
     * 根据配置白名单判断扩展名是否允许上传；未配置白名单时默认放行。
     */
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

    private boolean isChunked(FileUploadInitRequest request) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 1) {
            return true;
        }
        long threshold = fileUploadProperties.getChunkSizeThreshold() == null ? 0L : fileUploadProperties.getChunkSizeThreshold();
        return threshold > 0 && request.getFileSize() != null && request.getFileSize() > threshold;
    }

    private long resolveChunkSize(FileUploadInitRequest request) {
        if (request.getChunkSize() != null && request.getChunkSize() > 0) {
            return request.getChunkSize();
        }
        return fileUploadProperties.getChunkSize() == null ? 5242880L : fileUploadProperties.getChunkSize();
    }

    private int resolveTotalChunks(FileUploadInitRequest request, long chunkSize) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 0) {
            return request.getTotalChunks();
        }
        long fileSize = request.getFileSize() == null ? 0L : request.getFileSize();
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    private FileUploadTask getTaskOrThrow(String uploadId) {
        Long userId = SecurityUtils.requireUserId();
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

    /**
     * 根据 MD5 和大小尝试复用已有正常文件，避免错误命中不同内容的同指纹记录。
     */
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

    /**
     * 限制上传任务只在允许的生命周期节点执行当前动作，避免终态任务被重复消费。
     */
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

    /**
     * 仅在状态机允许时推进任务状态，避免任务在异常重试时发生非法回退。
     */
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

    /**
     * 将任务、物理文件和业务引用一次性收口，确保任务状态与引用计数保持同步。
     */
    private FileUploadResultVO finalizeTaskWithReference(FileUploadTask task, FileInfo fileInfo, String sourceIp, boolean quick) {
        FileBusinessInfo ref = createOrGetBusinessReference(task, fileInfo.getId(), sourceIp);
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
        return toResultVO(task, fileInfo, ref == null ? null : ref.getId());
    }

    /**
     * 根据上传任务生成物理文件记录，统一补齐分类、公开性和来源信息。
     */
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

    /**
     * 处理 MD5 唯一键冲突，优先复用已存在记录，并尽力清理当前请求产生的多余对象。
     */
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

    /**
     * 去重冲突时回收本次请求多上传的对象，避免外部存储残留孤儿文件。
     */
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

    /**
     * 按业务维度创建引用记录，遇到并发重复提交时复用已存在的引用。
     */
    private FileBusinessInfo createOrGetBusinessReference(FileUploadTask task, Long fileId, String sourceIp) {
        String referenceType = FileReferenceTypeEnum.normalize(task.getReferenceType());
        long referenceId = task.getReferenceId() == null ? 0L : task.getReferenceId();
        Long userId = task.getUploadUserId();
        FileBusinessInfo existing = fileBusinessInfoRepository.findByFileUserReference(fileId, userId, referenceType, referenceId);
        if (existing != null) {
            return existing;
        }
        FileBusinessInfo ref = new FileBusinessInfo();
        ref.setFileId(fileId);
        ref.setUserId(task.getUploadUserId());
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

    /**
     * 按任务绑定的业务维度回查引用记录 ID，用于已完成任务结果回放。
     */
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

    /**
     * 分片任务在完成后清理分片记录与临时文件，避免秒传复用和合并成功后残留临时资源。
     * 外部存储清理仅做尽力而为，不阻塞已完成的元数据收口。
     */
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

    /**
     * 对单个分片执行插入或覆盖，保证重复上传同一分片时记录保持最新。
     */
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

    /**
     * 按上传任务生成分片对象名列表，确保合并顺序与分片编号一致。
     */
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

    private void validateVisibility(Integer isPublic) {
        ExceptionThrowerCore.throwBusinessIf(
                isPublic != null && !Integer.valueOf(0).equals(isPublic) && !Integer.valueOf(1).equals(isPublic),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件可见性非法"
        );
    }

    private void validateChunkRequest(FileUploadInitRequest request) {
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

    private String buildTempChunkObjectName(String uploadId, Integer chunkNumber) {
        return fileUploadProperties.getTempDirPrefix() + "/" + uploadId + "/chunk-" + chunkNumber + ".part";
    }

    private Map<Long, FileInfo> loadFileInfoMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, FileInfo> map = new HashMap<>();
        for (FileInfo info : fileInfoRepository.listByIds(ids)) {
            map.put(info.getId(), info);
        }
        return map;
    }

    /**
     * 将业务引用与物理文件信息拼装成用户文件列表项。
     */
    private UserFileVO toUserFileVO(FileBusinessInfo ref, FileInfo fileInfo) {
        if (ref == null || fileInfo == null) {
            return null;
        }
        return fileModelMapper.toUserFileVOFromBoth(ref, fileInfo);
    }

    /**
     * 将上传任务转换为用户侧任务视图，便于展示当前上传进度和异常信息。
     */
    private UserFileTaskVO toUserTaskVO(FileUploadTask task) {
        return fileModelMapper.toUserFileTaskVO(task);
    }

    /**
     * 构造统一的上传结果对象，兼容秒传、普通上传和分片上传完成三种返回场景。
     */
    private FileUploadResultVO toResultVO(FileUploadTask task, FileInfo fileInfo, Long businessId) {
        FileUploadResultVO vo = fileModelMapper.toFileUploadResultVO(task);
        vo.setFileId(fileInfo == null ? null : fileInfo.getId());
        vo.setBusinessId(businessId);
        vo.setFileUrl(fileInfo == null ? null : fileInfo.getFileUrl());
        vo.setReferenceCount(fileInfo == null ? null : fileInfo.getReferenceCount());
        return vo;
    }

    /**
     * 统一计算并回填文件 MD5，保证整文件上传与秒传逻辑使用同一份指纹。
     */
    private String resolveAndValidateMd5(FileUploadTask task, MultipartFile file) {
        String computed = md5Hex(file);
        String expected = FileUtils.normalizeMd5(task.getFileMd5());
        if (Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && StringUtils.hasText(expected)) {
            ExceptionThrowerCore.throwBusinessIfNot(expected.equalsIgnoreCase(computed), FileResultCode.FILE_MD5_MISMATCH);
        }
        task.setFileMd5(computed);
        fileUploadTaskRepository.updateById(task);
        return computed;
    }

    /**
     * 计算上传文件的 MD5，用于与客户端指纹比对及秒传复用。
     */
    private String md5Hex(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            return FileUtils.toHex(md.digest());
        } catch (Exception e) {
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_MD5_CALCULATE_FAILED);
            return null;
        }
    }

    /**
     * 生成最终对象名，按业务分类和日期分桶，避免目录过深或名称冲突。
     */
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

    /**
     * 按 MIME 类型归并出前端更容易消费的文件类型分类。
     */
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





















package com.cybzacg.blogbackend.module.file.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.model.user.ChunkUploadVO;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitRequest;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitVO;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadResultVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * 用户文件服务实现。
 * 负责上传任务初始化、秒传检测、整文件上传、分片上传与用户侧文件引用维护。
 */
@Service
@RequiredArgsConstructor
public class UserFileServiceImpl implements UserFileService {
    private static final int CHUNK_STATUS_COMPLETED = 2;
    private final FileInfoService fileInfoService;
    private final FileUploadTaskService fileUploadTaskService;
    private final FileChunkService fileChunkService;
    private final FileBusinessInfoService fileBusinessInfoService;
    private final StorageManager storageManager;
    private final FileUploadProperties fileUploadProperties;
    /**
     * 初始化上传任务，并根据文件指纹预判是否可以直接走秒传。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadInitVO initUploadTask(FileUploadInitRequest request, String sourceIp) {
        Long userId = SecurityUtils.requireUserId();
        validateInitRequest(request);
        String md5 = normalizeMd5(request.getFileMd5());
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
        task.setIsPublic(defaultInt(request.getIsPublic(), 0));
        task.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        task.setIsChunked(chunked ? 1 : 0);
        task.setChunkSize(chunked ? chunkSize : null);
        task.setTotalChunks(chunked ? totalChunks : null);
        task.setUploadedChunks(0);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        task.setRetryCount(0);
        task.setStartTime(new Date());
        task.setExpireTime(expireAfterDays(fileUploadProperties.getTaskExpireDays()));
        fileUploadTaskService.save(task);
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
            FileInfo fileInfo = fileInfoService.getById(task.getFileId());
            Long businessId = resolveBusinessId(task.getFileId(), task.getReferenceType(), task.getReferenceId());
            return toResultVO(task, fileInfo, businessId);
        }
        // 秒传检测只依赖文件指纹与大小，不要求客户端重新上传文件体。
        FileInfo existing = tryFindExistingFile(normalizeMd5(task.getFileMd5()), task.getFileSize());
        if (existing == null) {
            FileUploadResultVO vo = new FileUploadResultVO();
            vo.setUploadId(task.getUploadId());
            vo.setTaskId(task.getId());
            vo.setQuickUpload(false);
            vo.setTaskStatus(task.getTaskStatus());
            return vo;
        }
        return finalizeTaskWithReference(task, existing, sourceIp, true);
    }
    /**
     * 处理普通整文件上传，在落存储前完成 MD5 校验与重复文件复用判断。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO uploadFile(String uploadId, MultipartFile file, String sourceIp) {
        ExceptionThrowerCore.throwBusinessIf(file == null || file.isEmpty(), FileResultCode.UPLOAD_FILE_EMPTY);
        FileUploadTask task = getTaskOrThrow(uploadId);
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.CHUNK_TASK_REQUIRED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);
        // 单文件上传在落存储前先补齐并校验 MD5，确保秒传判断与最终落库一致。
        String md5 = resolveAndValidateMd5(task, file);
        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, sourceIp, true);
        }
        String objectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        StorageService storageService = requireStorageService(task.getStorageKey());
        String url;
        try (InputStream inputStream = file.getInputStream()) {
            url = storageService.upload(inputStream, objectName, task.getMimeType());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.FILE_UPLOAD_FAILED, e.getMessage());
            throw new BusinessException(FileResultCode.FILE_UPLOAD_FAILED);
        }
        FileInfo fileInfo = createFileInfo(task, objectName, url, md5);
        return finalizeTaskWithReference(task, fileInfo, sourceIp, false);
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
            throw new BusinessException(FileResultCode.CHUNK_UPLOAD_FAILED);
        }
        // 分片记录既用于断点续传，也用于最终合并前的完整性校验。
        upsertChunkRecord(task.getId(), chunkNumber, file.getSize(), normalizeMd5(chunkMd5));
        int uploadedChunks = countUploadedChunks(task.getId());
        task.setUploadedChunks(uploadedChunks);
        fileUploadTaskService.updateById(task);
        ChunkUploadVO vo = new ChunkUploadVO();
        vo.setUploadId(task.getUploadId());
        vo.setChunkNumber(chunkNumber);
        vo.setUploadedChunks(uploadedChunks);
        vo.setTotalChunks(task.getTotalChunks());
        vo.setTaskStatus(task.getTaskStatus());
        return vo;
    }
    /**
     * 校验分片完整性并执行合并，必要时复用已存在的同指纹物理文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResultVO completeUpload(String uploadId, String sourceIp) {
        FileUploadTask task = getTaskOrThrow(uploadId);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        int uploaded = countUploadedChunks(task.getId());
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || uploaded < task.getTotalChunks(), FileResultCode.CHUNK_INCOMPLETE);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.MERGING);
        String md5 = normalizeMd5(task.getFileMd5());
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StringUtils.hasText(md5), FileResultCode.FILE_MD5_REQUIRED, "缺少文件MD5，无法完成上传");
        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            return finalizeTaskWithReference(task, existing, sourceIp, true);
        }
        StorageService storageService = requireStorageService(task.getStorageKey());
        String targetObjectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        List<String> sourceObjectNames = buildChunkObjectNames(task.getUploadId(), task.getTotalChunks());
        try {
            // 先合并再清理临时文件，保证失败时仍可保留现场用于重试或排查。
            storageService.mergeFiles(sourceObjectNames, targetObjectName);
            storageService.deleteTempFiles(task.getUploadId());
        } catch (Exception e) {
            markTaskFailed(task, FileResultCode.CHUNK_MERGE_FAILED, e.getMessage());
            throw new BusinessException(FileResultCode.CHUNK_MERGE_FAILED);
        }
        String url = storageService.getUrl(targetObjectName);
        FileInfo fileInfo = createFileInfo(task, targetObjectName, url, md5);
        return finalizeTaskWithReference(task, fileInfo, sourceIp, false);
    }
    /**
     * 分页查询当前用户的文件引用，并按关键字回填物理文件信息。
     */
    @Override
    public PageResult<UserFileVO> pageMyFiles(UserFilePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        Set<Long> fileIds = null;
        if (StringUtils.hasText(query.getKeyword())) {
            List<FileInfo> hits = fileInfoService.lambdaQuery()
                    .select(FileInfo::getId)
                    .and(w -> w.like(FileInfo::getOriginalName, query.getKeyword())
                            .or()
                            .like(FileInfo::getFileName, query.getKeyword()))
                    .list();
            fileIds = hits.stream().map(FileInfo::getId).collect(Collectors.toSet());
            if (fileIds.isEmpty()) {
                return PageResult.<UserFileVO>builder().total(0L).current(current).size(size).records(List.of()).build();
            }
        }
        LambdaQueryWrapper<FileBusinessInfo> wrapper = new LambdaQueryWrapper<FileBusinessInfo>()
                .eq(FileBusinessInfo::getUserId, userId)
                .eq(StringUtils.hasText(query.getCategory()), FileBusinessInfo::getCategory, FileCategoryEnum.normalize(query.getCategory()))
                .eq(StringUtils.hasText(query.getReferenceType()), FileBusinessInfo::getReferenceType, FileReferenceTypeEnum.normalize(query.getReferenceType()))
                .in(fileIds != null, FileBusinessInfo::getFileId, fileIds)
                .orderByDesc(FileBusinessInfo::getCreatedAt)
                .orderByDesc(FileBusinessInfo::getId);
        Page<FileBusinessInfo> page = fileBusinessInfoService.page(new Page<>(current, size), wrapper);
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
        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        LambdaQueryWrapper<FileUploadTask> wrapper = new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getUploadUserId, userId)
                .eq(query.getTaskStatus() != null, FileUploadTask::getTaskStatus, query.getTaskStatus())
                .eq(query.getIsQuickUpload() != null, FileUploadTask::getIsQuickUpload, query.getIsQuickUpload())
                .eq(query.getIsChunked() != null, FileUploadTask::getIsChunked, query.getIsChunked())
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId);
        Page<FileUploadTask> page = fileUploadTaskService.page(new Page<>(current, size), wrapper);
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
        FileBusinessInfo ref = fileBusinessInfoService.getById(businessId);
        ExceptionThrowerCore.throwBusinessIf(ref == null || !userId.equals(ref.getUserId()), FileResultCode.FILE_REFERENCE_NOT_FOUND);
        Long fileId = ref.getFileId();
        fileBusinessInfoService.removeById(businessId);
        // 删除的是用户引用而不是直接硬删文件，只有最后一个引用移除后才回收物理文件。
        long remaining = fileBusinessInfoService.lambdaQuery().eq(FileBusinessInfo::getFileId, fileId).count();
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            return;
        }
        fileInfo.setReferenceCount((int) remaining);
        if (remaining <= 0) {
            try {
                StorageService storageService = requireStorageService(fileInfo.getStorageKey());
                storageService.delete(fileInfo.getFilePath());
            } catch (Exception ignored) {
            }
            fileInfo.setStatus(FileStatusEnum.DELETED.getValue());
        }
        fileInfoService.updateById(fileInfo);
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
        return ExceptionThrowerCore.require(() -> fileUploadTaskService.lambdaQuery()
                .eq(FileUploadTask::getUploadId, uploadId)
                .eq(FileUploadTask::getUploadUserId, userId)
                .one(), FileResultCode.UPLOAD_TASK_NOT_FOUND);
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
        FileInfo file = fileInfoService.lambdaQuery()
                .eq(FileInfo::getMd5, md5)
                .eq(FileInfo::getStatus, FileStatusEnum.NORMAL.getValue())
                .one();
        if (file == null) {
            return null;
        }
        if (fileSize != null && file.getFileSize() != null && !fileSize.equals(file.getFileSize())) {
            return null;
        }
        return file;
    }
    /**
     * 仅在状态机允许时推进任务状态，避免任务在异常重试时发生非法回退。
     */
    private void updateTaskStatusIfNeeded(FileUploadTask task, TaskStatusEnum target) {
        TaskStatusEnum current = TaskStatusEnum.getByCode(task.getTaskStatus());
        if (current == null) {
            task.setTaskStatus(target.getValue());
            fileUploadTaskService.updateById(task);
            return;
        }
        if (!current.equals(target) && current.canTransitionTo(target)) {
            task.setTaskStatus(target.getValue());
            fileUploadTaskService.updateById(task);
        }
    }
    /**
     * 将任务、物理文件和业务引用一次性收口，确保任务状态与引用计数保持同步。
     */
    private FileUploadResultVO finalizeTaskWithReference(FileUploadTask task, FileInfo fileInfo, String sourceIp, boolean quick) {
        FileBusinessInfo ref = createOrGetBusinessReference(task, fileInfo.getId(), sourceIp);
        long count = fileBusinessInfoService.lambdaQuery().eq(FileBusinessInfo::getFileId, fileInfo.getId()).count();
        fileInfo.setReferenceCount((int) count);
        if (Integer.valueOf(1).equals(task.getIsPublic())) {
            fileInfo.setIsPublic(1);
        }
        fileInfoService.updateById(fileInfo);
        task.setIsQuickUpload(quick ? 1 : defaultInt(task.getIsQuickUpload(), 0));
        if (quick) {
            task.setReferencedFileId(fileInfo.getId());
            task.setQuickUploadTime(new Date());
        }
        task.setFileId(fileInfo.getId());
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setCompleteTime(new Date());
        fileUploadTaskService.updateById(task);
        return toResultVO(task, fileInfo, ref == null ? null : ref.getId());
    }
    /**
     * 根据上传任务生成物理文件记录，统一补齐分类、公开性和来源信息。
     */
    private FileInfo createFileInfo(FileUploadTask task, String objectName, String url, String md5) {
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
        fileInfo.setFileExtension(normalizeExt(FileUtils.getExtension(task.getOriginalName())));
        fileInfo.setMd5(md5);
        fileInfo.setReferenceCount(0);
        fileInfo.setIsPublic(defaultInt(task.getIsPublic(), 0));
        fileInfo.setCategory(FileCategoryEnum.normalize(task.getCategory()));
        fileInfo.setDownloadCount(0);
        fileInfo.setUploadUserId(task.getUploadUserId());
        fileInfo.setRemark(task.getRemark());
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        fileInfoService.save(fileInfo);
        return fileInfo;
    }
    /**
     * 按业务维度创建引用记录，遇到并发重复提交时复用已存在的引用。
     */
    private FileBusinessInfo createOrGetBusinessReference(FileUploadTask task, Long fileId, String sourceIp) {
        String referenceType = FileReferenceTypeEnum.normalize(task.getReferenceType());
        long referenceId = task.getReferenceId() == null ? 0L : task.getReferenceId();
        FileBusinessInfo existing = fileBusinessInfoService.lambdaQuery()
                .eq(FileBusinessInfo::getFileId, fileId)
                .eq(FileBusinessInfo::getReferenceType, referenceType)
                .eq(FileBusinessInfo::getReferenceId, referenceId)
                .one();
        if (existing != null) {
            return existing;
        }
        FileBusinessInfo ref = new FileBusinessInfo();
        ref.setFileId(fileId);
        ref.setUserId(task.getUploadUserId());
        ref.setReferenceType(referenceType);
        ref.setReferenceId(referenceId);
        ref.setSourceIp(sourceIp);
        ref.setIsPublic(defaultInt(task.getIsPublic(), 0));
        ref.setCategory(FileCategoryEnum.normalize(task.getCategory()));
        ref.setRemark(task.getRemark());
        try {
            fileBusinessInfoService.save(ref);
        } catch (DuplicateKeyException e) {
            return fileBusinessInfoService.lambdaQuery()
                    .eq(FileBusinessInfo::getFileId, fileId)
                    .eq(FileBusinessInfo::getReferenceType, referenceType)
                    .eq(FileBusinessInfo::getReferenceId, referenceId)
                    .one();
        }
        return ref;
    }
    /**
     * 按任务绑定的业务维度回查引用记录 ID，用于已完成任务结果回放。
     */
    private Long resolveBusinessId(Long fileId, String referenceType, Long referenceId) {
        if (fileId == null) {
            return null;
        }
        String type = FileReferenceTypeEnum.normalize(referenceType);
        long rid = referenceId == null ? 0L : referenceId;
        FileBusinessInfo ref = fileBusinessInfoService.lambdaQuery()
                .eq(FileBusinessInfo::getFileId, fileId)
                .eq(FileBusinessInfo::getReferenceType, type)
                .eq(FileBusinessInfo::getReferenceId, rid)
                .orderByDesc(FileBusinessInfo::getId)
                .last("limit 1")
                .one();
        return ref == null ? null : ref.getId();
    }
    private void markTaskFailed(FileUploadTask task, FileResultCode resultCode, String message) {
        task.setTaskStatus(TaskStatusEnum.FAILED.getValue());
        task.setErrorCode(resultCode == null ? null : String.valueOf(resultCode.getCode()));
        task.setErrorMessage(truncate(StringUtils.hasText(message) ? message : (resultCode == null ? null : resultCode.getMessage()), 240));
        fileUploadTaskService.updateById(task);
    }
    /**
     * 对单个分片执行插入或覆盖，保证重复上传同一分片时记录保持最新。
     */
    private void upsertChunkRecord(Long taskId, Integer chunkNumber, long size, String md5) {
        FileChunk chunk = fileChunkService.lambdaQuery()
                .eq(FileChunk::getUploadTaskId, taskId)
                .eq(FileChunk::getChunkNumber, chunkNumber)
                .one();
        if (chunk == null) {
            chunk = new FileChunk();
            chunk.setUploadTaskId(taskId);
            chunk.setChunkNumber(chunkNumber);
            chunk.setChunkSize(size);
            chunk.setChunkMd5(md5);
            chunk.setUploadStatus(CHUNK_STATUS_COMPLETED);
            chunk.setRetryCount(0);
            chunk.setUploadTime(new Date());
            fileChunkService.save(chunk);
            return;
        }
        chunk.setChunkSize(size);
        chunk.setChunkMd5(md5);
        chunk.setUploadStatus(CHUNK_STATUS_COMPLETED);
        chunk.setUploadTime(new Date());
        fileChunkService.updateById(chunk);
    }
    private int countUploadedChunks(Long taskId) {
        Long count = fileChunkService.lambdaQuery()
                .eq(FileChunk::getUploadTaskId, taskId)
                .eq(FileChunk::getUploadStatus, CHUNK_STATUS_COMPLETED)
                .count();
        if (count == null) {
            return 0;
        }
        return Math.toIntExact(count);
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
    private Map<Long, FileInfo> loadFileInfoMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, FileInfo> map = new HashMap<>();
        for (FileInfo info : fileInfoService.listByIds(ids)) {
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
        UserFileVO vo = new UserFileVO();
        vo.setBusinessId(ref.getId());
        vo.setFileId(fileInfo.getId());
        vo.setFileName(fileInfo.getFileName());
        vo.setOriginalName(fileInfo.getOriginalName());
        vo.setFileUrl(fileInfo.getFileUrl());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setFileType(fileInfo.getFileType());
        vo.setMimeType(fileInfo.getMimeType());
        vo.setCategory(ref.getCategory());
        vo.setReferenceType(ref.getReferenceType());
        vo.setReferenceId(ref.getReferenceId());
        vo.setIsPublic(ref.getIsPublic());
        vo.setStatus(fileInfo.getStatus());
        vo.setCreatedAt(ref.getCreatedAt());
        return vo;
    }
    /**
     * 将上传任务转换为用户侧任务视图，便于展示当前上传进度和异常信息。
     */
    private UserFileTaskVO toUserTaskVO(FileUploadTask task) {
        UserFileTaskVO vo = new UserFileTaskVO();
        vo.setId(task.getId());
        vo.setUploadId(task.getUploadId());
        vo.setFileId(task.getFileId());
        vo.setOriginalName(task.getOriginalName());
        vo.setFileSize(task.getFileSize());
        vo.setIsQuickUpload(task.getIsQuickUpload());
        vo.setIsChunked(task.getIsChunked());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setUploadedChunks(task.getUploadedChunks());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setErrorCode(task.getErrorCode());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartTime(task.getStartTime());
        vo.setCompleteTime(task.getCompleteTime());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }
    /**
     * 构造统一的上传结果对象，兼容秒传、普通上传和分片上传完成三种返回场景。
     */
    private FileUploadResultVO toResultVO(FileUploadTask task, FileInfo fileInfo, Long businessId) {
        FileUploadResultVO vo = new FileUploadResultVO();
        vo.setUploadId(task.getUploadId());
        vo.setTaskId(task.getId());
        vo.setFileId(fileInfo == null ? null : fileInfo.getId());
        vo.setBusinessId(businessId);
        vo.setQuickUpload(Integer.valueOf(1).equals(task.getIsQuickUpload()));
        vo.setTaskStatus(task.getTaskStatus());
        vo.setFileUrl(fileInfo == null ? null : fileInfo.getFileUrl());
        vo.setReferenceCount(fileInfo == null ? null : fileInfo.getReferenceCount());
        return vo;
    }
    /**
     * 统一计算并回填文件 MD5，保证整文件上传与秒传逻辑使用同一份指纹。
     */
    private String resolveAndValidateMd5(FileUploadTask task, MultipartFile file) {
        String computed = md5Hex(file);
        String expected = normalizeMd5(task.getFileMd5());
        if (Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && StringUtils.hasText(expected)) {
            ExceptionThrowerCore.throwBusinessIfNot(expected.equalsIgnoreCase(computed), FileResultCode.FILE_MD5_MISMATCH);
        }
        task.setFileMd5(computed);
        fileUploadTaskService.updateById(task);
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
            return toHex(md.digest());
        } catch (Exception e) {
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_MD5_CALCULATE_FAILED);
            return null;
        }
    }
    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
    private String normalizeMd5(String md5) {
        if (!StringUtils.hasText(md5)) {
            return null;
        }
        return md5.trim().toLowerCase(Locale.ROOT);
    }
    /**
     * 生成最终对象名，按业务分类和日期分桶，避免目录过深或名称冲突。
     */
    private String buildFinalObjectName(String category, String originalName) {
        String normalizedCategory = FileCategoryEnum.normalize(category);
        String ext = normalizeExt(FileUtils.getExtension(originalName));
        String datePath = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = StringUtils.hasText(ext) ? ("." + ext) : "";
        return normalizedCategory + "/" + datePath + "/" + uuid + suffix;
    }
    private String normalizeExt(String ext) {
        if (!StringUtils.hasText(ext)) {
            return "";
        }
        return ext.trim().toLowerCase(Locale.ROOT);
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
    private Date expireAfterDays(Integer days) {
        int d = days == null ? 2 : Math.max(1, days);
        long millis = (long) d * 24L * 60L * 60L * 1000L;
        return new Date(System.currentTimeMillis() + millis);
    }
    private String truncate(String value, int maxLen) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }
}

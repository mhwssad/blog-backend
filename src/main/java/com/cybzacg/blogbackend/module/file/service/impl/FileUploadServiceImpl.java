package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileChunk;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.dto.repository.file.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileChunkRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileInfoRepository;
import com.cybzacg.blogbackend.dto.repository.file.FileUploadTaskRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.enums.storage.UploadModeEnum;
import com.cybzacg.blogbackend.module.file.convert.FileModelConvert;
import com.cybzacg.blogbackend.module.file.convert.FileUploadConvert;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadService;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
@Slf4j
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
    private final FileModelConvert fileModelConvert;
    private final FileUploadConvert fileUploadConvert;

    /**
     * 初始化文件上传任务。
     * 根据请求参数判断采用秒传（MD5 已存在）、分片上传还是整文件上传模式，
     * 并创建对应的上传任务记录返回给前端。
     *
     * @param userId  上传用户ID
     * @param request 上传初始化请求
     * @return 用户任务视图对象，包含任务ID、上传ID、上传模式、秒传可用性等
     */
    @Override
    public UserTaskVO initUploadTask(Long userId, UserUploadInitRequest request) {
        // 参数合法性校验
        validateInitRequest(request);
        // 规范化 MD5，去除空格和大小写差异
        String md5 = FileUtils.normalizeMd5(request.getFileMd5());
        Long fileSize = request.getFileSize();
        // 获取当前存储节点 Key，若未配置则直接拒绝
        String storageKey = storageManager.getCurrentStorageKey();
        ExceptionThrowerCore.throwBusinessIfBlank(storageKey, FileResultCode.STORAGE_NODE_NOT_CONFIGURED);

        // 判断是否采用分片上传：显式配置分片数 > 1，或文件大小超过阈值
        boolean chunked = isChunked(request);
        long chunkSize = resolveChunkSize(request);
        int totalChunks = chunked ? resolveTotalChunks(request, chunkSize) : 0;

        // 构建并持久化上传任务
        FileUploadTask task = fileUploadConvert.toFileUploadTask(request, userId, storageKey, md5,
                chunked ? 1 : 0, chunked ? chunkSize : null, chunked ? totalChunks : null,
                LocalDateTime.now(), expireAfterDays(fileUploadProperties.getTaskExpireDays()));
        fileUploadTaskRepository.save(task);

        // 组装返回 VO
        UserTaskVO vo = new UserTaskVO();
        vo.setTaskId(task.getId());
        vo.setUploadId(task.getUploadId());
        vo.setChunkSize(task.getChunkSize());
        vo.setTotalChunks(task.getTotalChunks());

        // 秒传检测：MD5 + 文件大小同时匹配时直接复用已有文件
        FileInfo existing = tryFindExistingFile(md5, fileSize);
        if (existing != null) {
            log.debug("[上传初始化] 秒传命中，md5={}，fileSize={}，复用文件ID={}", md5, fileSize, existing.getId());
            UserTaskVO result = finalizeTaskWithReference(task, existing, true);
            vo.setUploadMode(UploadModeEnum.QUICK_UPLOAD.getValue());
            vo.setQuickUploadAvailable(true);
            vo.setCompleted(true);
            vo.setTaskStatus(result.getTaskStatus());
            vo.setFileId(result.getFileId());
            return vo;
        }
        // 无法秒传，返回后续上传所需信息
        vo.setQuickUploadAvailable(md5 != null);
        vo.setCompleted(false);
        vo.setUploadMode(chunked ? UploadModeEnum.CHUNKED_UPLOAD.getValue() : UploadModeEnum.FULL_UPLOAD.getValue());
        vo.setTaskStatus(task.getTaskStatus());
        return vo;
    }

    /**
     * 秒传检查（断点续传的前置探测）。
     * 根据 uploadId（即 MD5）定位上传任务，检查任务是否已完成或可复用已有文件。
     * 若任务已过期则抛出异常；若检测到 MD5 匹配的文件则触发秒传。
     *
     * @param userId 上传用户ID
     * @param md5    文件 MD5（对应 uploadId）
     * @return 用户任务视图对象，包含文件ID、业务ID 等
     */
    @Override
    public UserTaskVO quickCheck(Long userId, String md5) {
        // 通过 uploadId（实为 MD5）和 userId 查找任务
        FileUploadTask task = fileUploadTaskRepository.findByUploadIdAndUserId(md5, userId);
        ExceptionThrowerCore.throwBusinessIf(task == null, FileResultCode.UPLOAD_TASK_NOT_FOUND);
        // 任务已过期则直接拒绝
        if (fileLifecycleService.expireTaskIfNeeded(task)) {
            log.debug("[秒传检查] 任务已过期，uploadId={}", md5);
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.UPLOAD_TASK_EXPIRED);
        }
        // 任务已完成且已关联文件，直接返回文件信息
        if (TaskStatusEnum.COMPLETED.getValue().equals(task.getTaskStatus()) && task.getFileId() != null) {
            FileInfo fileInfo = fileInfoRepository.getById(task.getFileId());
            Long businessId = resolveBusinessId(task.getFileId(), task.getUploadUserId(), task.getReferenceType(), task.getReferenceId());
            return toTaskVO(task, fileInfo, businessId);
        }
        // 任务未完成，检查当前状态是否允许继续操作
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        // 再次尝试秒传：若存在 MD5 + 文件大小均匹配的已有文件，则复用
        FileInfo existing = tryFindExistingFile(FileUtils.normalizeMd5(task.getFileMd5()), task.getFileSize());
        if (existing == null) {
            log.debug("[秒传检查] 未命中秒传，uploadId={}", md5);
            UserTaskVO vo = fileModelConvert.toUserTaskVO(task);
            vo.setQuickUpload(false);
            return vo;
        }
        log.debug("[秒传检查] 秒传命中，uploadId={}，复用文件ID={}", md5, existing.getId());
        return finalizeTaskWithReference(task, existing, true);
    }

    /**
     * 整文件上传（非分片）。
     * 先检查 MD5 复用可能性，再将文件流上传至存储服务，最后完成任务并建立业务引用。
     * 若前端未传 MD5，上传过程中会自动计算并用于复用判断。
     *
     * @param userId     上传用户ID
     * @param md5        文件 MD5（可选，为空则自动计算）
     * @param taskId     上传任务ID
     * @param fileInfo   文件元信息（含 MIME 类型、原始名等）
     * @param inputStream 文件数据流
     * @return 用户任务视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO uploadFile(Long userId, String md5, String taskId, FileInfo fileInfo, InputStream inputStream) {
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        // 非分片任务禁止调用此方法
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.CHUNK_TASK_REQUIRED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);

        // 优先使用前端传来的 MD5，否则沿用任务中已保存的 MD5
        String effectiveMd5 = md5 != null ? md5 : task.getFileMd5();
        String normalizedMd5 = FileUtils.normalizeMd5(effectiveMd5);
        // 秒传复用检测
        FileInfo existing = tryFindExistingFile(normalizedMd5, task.getFileSize());
        if (existing != null) {
            log.debug("[整文件上传] 秒传命中，md5={}，复用文件ID={}", normalizedMd5, existing.getId());
            return finalizeTaskWithReference(task, existing, true);
        }

        // 构建存储对象名（按「分类/日期/UUID.扩展名」组织）
        String objectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        StorageService storageService = requireStorageService(task.getStorageKey());

        // 前端未传 MD5 时，在上传过程中通过 DigestInputStream 自动计算
        MessageDigest md5Digest = null;
        InputStream uploadStream = inputStream;
        if (!StrUtils.hasText(normalizedMd5)) {
            try {
                md5Digest = MessageDigest.getInstance("MD5");
                uploadStream = new DigestInputStream(inputStream, md5Digest);
            } catch (NoSuchAlgorithmException e) {
                // MD5 算法必定存在，此处不会触发
            }
        }

        // 上传文件到存储服务
        String url;
        try {
            url = storageService.upload(uploadStream, objectName, fileInfo.getMimeType());
        } catch (Exception e) {
            log.error("[整文件上传] 上传失败，taskId={}，error={}", taskId, e.getMessage());
            markTaskFailed(task, FileResultCode.FILE_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_UPLOAD_FAILED);
            return null; // unreachable, but satisfies compiler
        }

        // 上传后若自动计算了 MD5，则重新规范化
        if (md5Digest != null) {
            normalizedMd5 = FileUtils.toHex(md5Digest.digest());
        }

        // 创建或复用 FileInfo 记录，并建立业务引用完成任务
        PersistedFileInfo persisted = createOrReuseFileInfo(task, objectName, url, normalizedMd5);
        return finalizeTaskWithReference(task, persisted.fileInfo(), persisted.reusedExisting());
    }

    /**
     * 分片上传。
     * 将单个分片上传到存储服务的临时目录，完成后更新分片记录和任务进度。
     * 若后续步骤失败，已上传的分片对象会被清理。
     *
     * @param userId      上传用户ID
     * @param taskId      上传任务ID
     * @param chunkIndex  分片序号（从 1 开始）
     * @param chunkFileInfo 分片元信息（含 MIME 类型、文件大小、MD5 等）
     * @param inputStream 分片数据流
     * @return 用户任务视图对象，含当前已上传分片数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO uploadChunk(Long userId, String taskId, Integer chunkIndex, FileInfo chunkFileInfo, InputStream inputStream) {
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        // 必须是分片任务才能调用此方法
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        // 分片序号不能超过总分片数
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || chunkIndex > task.getTotalChunks(), FileResultCode.CHUNK_NUMBER_EXCEEDED);
        assertTaskActionAllowed(task, TaskStatusEnum.INIT, TaskStatusEnum.UPLOADING, TaskStatusEnum.FAILED);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.UPLOADING);

        StorageService storageService = requireStorageService(task.getStorageKey());
        // 分片临时对象名格式：<uploadId>/chunk-<index>.part
        String chunkObjectName = task.getUploadId() + "/chunk-" + chunkIndex + ".part";

        // 上传分片到临时目录
        try {
            storageService.uploadToTemp(inputStream, chunkObjectName, chunkFileInfo.getMimeType());
        } catch (Exception e) {
            log.error("[分片上传] 上传失败，taskId={}，chunkIndex={}，error={}", taskId, chunkIndex, e.getMessage());
            markTaskFailed(task, FileResultCode.CHUNK_UPLOAD_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.CHUNK_UPLOAD_FAILED);
        }

        try {
            // 插入或更新分片记录（幂等操作）
            upsertChunkRecord(task.getId(), chunkIndex, chunkFileInfo.getFileSize(), chunkFileInfo.getMd5());
            // 统计当前已完成分片数并回写任务进度
            int uploadedChunks = countUploadedChunks(task.getId());
            task.setUploadedChunks(uploadedChunks);
            fileUploadTaskRepository.updateById(task);

            // 组装返回 VO
            UserTaskVO vo = fileModelConvert.toUserTaskVO(task);
            vo.setUploadId(task.getUploadId());
            vo.setChunkNumber(chunkIndex);
            vo.setUploadedChunks(uploadedChunks);
            vo.setTotalChunks(task.getTotalChunks());
            return vo;
        } catch (RuntimeException e) {
            // 分片记录写入失败时，清理已上传的临时对象
            log.warn("[分片上传] 分片记录写入异常，清理临时对象，taskId={}，chunkIndex={}", taskId, chunkIndex);
            cleanupUploadedObject(task.getStorageKey(), buildTempChunkObjectName(task.getUploadId(), chunkIndex));
            throw e;
        }
    }

    /**
     * 完成分片上传（合并所有分片）。
     * 验证所有分片均已上传成功后，将临时分片文件合并为最终目标文件。
     * 若任务中缺少 MD5，合并后会从已合并文件重新计算并复用已有文件。
     *
     * @param userId 上传用户ID
     * @param taskId 上传任务ID
     * @return 用户任务视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserTaskVO completeUpload(Long userId, String taskId) {
        FileUploadTask task = getTaskOrThrow(taskId, userId);
        // 仅分片任务可调用此方法
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(task.getIsChunked()), FileResultCode.NON_CHUNK_TASK);
        assertTaskActionAllowed(task, TaskStatusEnum.UPLOADING, TaskStatusEnum.MERGING, TaskStatusEnum.FAILED);
        // 校验所有分片均已上传完成
        int uploaded = countUploadedChunks(task.getId());
        ExceptionThrowerCore.throwBusinessIf(task.getTotalChunks() == null || uploaded < task.getTotalChunks(), FileResultCode.CHUNK_INCOMPLETE);
        updateTaskStatusIfNeeded(task, TaskStatusEnum.MERGING);

        // 规范化 MD5，若未配置 MD5 校验则允许为空
        String md5 = FileUtils.normalizeMd5(task.getFileMd5());
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StrUtils.hasText(md5), FileResultCode.FILE_MD5_REQUIRED, "缺少文件MD5，无法完成上传");

        // 合并前再次尝试复用已有文件（覆盖场景下可能存在）
        FileInfo existing = tryFindExistingFile(md5, task.getFileSize());
        if (existing != null) {
            log.debug("[完成分片上传] 秒传命中，md5={}，复用文件ID={}", md5, existing.getId());
            return finalizeTaskWithReference(task, existing, true);
        }

        StorageService storageService = requireStorageService(task.getStorageKey());
        // 目标对象名及所有分片源对象名列表
        String targetObjectName = buildFinalObjectName(task.getCategory(), task.getOriginalName());
        List<String> sourceObjectNames = buildChunkObjectNames(task.getUploadId(), task.getTotalChunks());

        // 执行分片合并
        try {
            storageService.mergeFiles(sourceObjectNames, targetObjectName);
            log.debug("[完成分片上传] 合并成功，taskId={}，targetObjectName={}", taskId, targetObjectName);
        } catch (Exception e) {
            log.error("[完成分片上传] 合并失败，taskId={}，error={}", taskId, e.getMessage());
            markTaskFailed(task, FileResultCode.CHUNK_MERGE_FAILED, e.getMessage());
            ExceptionThrowerCore.throwBusinessEx(FileResultCode.CHUNK_MERGE_FAILED);
        }

        // 分片合并后，如果前端未传 MD5，从已合并的文件中计算
        if (!StrUtils.hasText(md5)) {
            md5 = computeMd5FromStorage(storageService, targetObjectName);
            if (!StrUtils.hasText(md5)) {
                log.error("[完成分片上传] MD5 计算失败，taskId={}", taskId);
                markTaskFailed(task, FileResultCode.FILE_MD5_REQUIRED, "无法计算文件MD5");
                ExceptionThrowerCore.throwBusinessEx(FileResultCode.FILE_MD5_REQUIRED);
            }
            // 重新计算后再次尝试复用（合并过程中文件可能已被其他任务创建）
            FileInfo found = tryFindExistingFile(md5, task.getFileSize());
            if (found != null) {
                log.debug("[完成分片上传] MD5 重算后秒传命中，md5={}，复用文件ID={}", md5, found.getId());
                // 清理刚合并的冗余文件
                cleanupUploadedObject(task.getStorageKey(), targetObjectName);
                return finalizeTaskWithReference(task, found, true);
            }
        }

        try {
            // 获取最终文件的访问 URL 并创建 FileInfo 记录
            String url = storageService.getUrl(targetObjectName);
            PersistedFileInfo persisted = createOrReuseFileInfo(task, targetObjectName, url, md5);
            return finalizeTaskWithReference(task, persisted.fileInfo(), persisted.reusedExisting());
        } catch (RuntimeException e) {
            // 创建记录失败时清理已合并的目标文件
            log.warn("[完成分片上传] FileInfo 创建异常，清理目标文件，taskId={}", taskId);
            cleanupUploadedObject(task.getStorageKey(), targetObjectName);
            throw e;
        }
    }

    /**
     * 根据 uploadId 和 userId 获取上传任务，未找到或已过期则抛出业务异常。
     */
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

    /**
     * 获取指定存储节点对应的 StorageService，不可用则抛出业务异常。
     */
    private StorageService requireStorageService(String storageKey) {
        StorageService storageService = storageManager.getStorageService(storageKey);
        ExceptionThrowerCore.throwBusinessIf(storageService == null, FileResultCode.STORAGE_NODE_UNAVAILABLE, "存储节点不可用: " + storageKey);
        return storageService;
    }

    /**
     * 尝试查找 MD5 和文件大小同时匹配的已存在文件，用于秒传复用。
     * 任一参数为空或 MD5 不合法时直接返回 null，不发起查询。
     */
    private FileInfo tryFindExistingFile(String md5, Long fileSize) {
        if (!StrUtils.hasText(md5)) {
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

    /**
     * 根据 MD5 查找文件记录，不限制状态，用于冲突处理时的兜底查询。
     */
    private FileInfo findFileByMd5(String md5) {
        if (!StrUtils.hasText(md5)) {
            return null;
        }
        return fileInfoRepository.findByMd5(md5);
    }

    /**
     * 校验任务当前状态是否在允许的状态列表中，不合法则抛出业务异常。
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
     * 必要时更新任务状态，仅在状态可以合法转换时才更新。
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
     * 完成任务并建立业务引用。
     * 将秒传或实际上传产生的文件与任务、用户建立引用关系，更新任务的完成状态，
     * 最后清理分片临时文件（若有）。
     *
     * @param task     上传任务
     * @param fileInfo 最终文件记录
     * @param quick    是否为秒传
     * @return 用户任务视图对象
     */
    private UserTaskVO finalizeTaskWithReference(FileUploadTask task, FileInfo fileInfo, boolean quick) {
        // 创建或获取文件业务引用
        FileBusinessInfo ref = createOrGetBusinessReference(task, fileInfo.getId(), null);
        // 刷新文件的引用元数据（引用计数、公开状态）
        fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), Integer.valueOf(1).equals(task.getIsPublic()));
        fileInfo = fileInfoRepository.getById(fileInfo.getId());
        // 标记任务为秒传及完成状态
        task.setIsQuickUpload(quick ? 1 : CollectionUtils.defaultInt(task.getIsQuickUpload(), 0));
        if (quick) {
            task.setReferencedFileId(fileInfo.getId());
            task.setQuickUploadTime(LocalDateTime.now());
        }
        task.setFileId(fileInfo.getId());
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        fileUploadTaskRepository.updateById(task);
        // 清理分片临时文件和记录
        cleanupChunkArtifacts(task);
        return toTaskVO(task, fileInfo, ref == null ? null : ref.getId());
    }

    /**
     * 将上传任务和文件信息组装为用户任务 VO。
     */
    private UserTaskVO toTaskVO(FileUploadTask task, FileInfo fileInfo, Long businessId) {
        UserTaskVO vo = fileModelConvert.toUserTaskVO(task);
        vo.setFileId(fileInfo == null ? null : fileInfo.getId());
        vo.setBusinessId(businessId);
        vo.setFileUrl(fileInfo == null ? null : fileInfo.getFileUrl());
        return vo;
    }

    /**
     * 创建或复用 FileInfo 记录。
     * 正常情况下保存新记录；若因 MD5 冲突抛出 DuplicateKeyException，
     * 则委托 handleFileInfoConflict 处理（可能复用已有记录）。
     */
    private PersistedFileInfo createOrReuseFileInfo(FileUploadTask task, String objectName, String url, String md5) {
        FileInfo fileInfo = fileUploadConvert.toFileInfo(task, objectName, url, md5);
        try {
            fileInfoRepository.save(fileInfo);
            return new PersistedFileInfo(fileInfo, false);
        } catch (DuplicateKeyException e) {
            return handleFileInfoConflict(task, fileInfo, objectName, md5, e);
        }
    }

    /**
     * 处理 FileInfo 保存时的 MD5 冲突。
     * - 若 MD5 对应的文件大小不一致，说明是不同文件，清理刚上传的对象并抛出异常。
     * - 若对应文件已被删除（软删除状态），则复用该记录并重新激活。
     * - 其他情况直接复用已有记录。
     */
    private PersistedFileInfo handleFileInfoConflict(FileUploadTask task, FileInfo candidate, String objectName, String md5, DuplicateKeyException ex) {
        FileInfo existing = findFileByMd5(md5);
        if (existing == null || (task.getFileSize() != null && existing.getFileSize() != null && !task.getFileSize().equals(existing.getFileSize()))) {
            cleanupUploadedObject(task.getStorageKey(), objectName);
            throw ex;
        }
        if (FileStatusEnum.DELETED.getValue().equals(existing.getStatus())) {
            existing.setUploadTaskId(task.getId());
            fileUploadConvert.updateFileInfoFromCandidate(existing, candidate);
            existing.setReferenceCount(0);
            existing.setDownloadCount(0);
            existing.setStatus(FileStatusEnum.NORMAL.getValue());
            fileInfoRepository.updateById(existing);
            return new PersistedFileInfo(existing, false);
        }
        cleanupUploadedObject(task.getStorageKey(), objectName);
        return new PersistedFileInfo(existing, true);
    }

    /**
     * 清理指定存储节点上的指定对象，若存储服务不可用则忽略。
     */
    private void cleanupUploadedObject(String storageKey, String objectName) {
        if (!StrUtils.hasText(storageKey) || !StrUtils.hasText(objectName)) {
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
     * 从存储服务下载文件内容并计算 MD5 值。
     * 用于分片合并后前端未传 MD5 的场景。
     */
    private String computeMd5FromStorage(StorageService storageService, String objectName) {
        try (InputStream in = storageService.download(objectName)) {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                md5Digest.update(buffer, 0, bytesRead);
            }
            return FileUtils.toHex(md5Digest.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 为上传任务创建或获取文件业务引用记录。
     * 若引用已存在（同一文件+用户+引用类型的组合），直接返回；否则新建。
     * 出现主键冲突时通过查询兜底获取已有记录，保证幂等。
     */
    private FileBusinessInfo createOrGetBusinessReference(FileUploadTask task, Long fileId, String sourceIp) {
        String referenceType = FileReferenceTypeEnum.normalize(task.getReferenceType());
        long referenceId = task.getReferenceId() == null ? 0L : task.getReferenceId();
        Long userId = task.getUploadUserId();
        FileBusinessInfo existing = fileBusinessInfoRepository.findByFileUserReference(fileId, userId, referenceType, referenceId);
        if (existing != null) {
            return existing;
        }
        FileBusinessInfo ref = fileUploadConvert.toFileBusinessInfo(fileId, userId, referenceType,
                referenceId, sourceIp, CollectionUtils.defaultInt(task.getIsPublic(), 0),
                FileCategoryEnum.normalize(task.getCategory()), task.getRemark());
        try {
            fileBusinessInfoRepository.save(ref);
        } catch (DuplicateKeyException e) {
            return fileBusinessInfoRepository.findByFileUserReference(fileId, userId, referenceType, referenceId);
        }
        return ref;
    }

    /**
     * 根据文件ID、用户ID、引用类型和引用ID解析业务ID。
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

    /**
     * 标记上传任务为失败状态，记录错误码和错误信息。
     */
    private void markTaskFailed(FileUploadTask task, FileResultCode resultCode, String message) {
        task.setTaskStatus(TaskStatusEnum.FAILED.getValue());
        task.setErrorCode(resultCode == null ? null : String.valueOf(resultCode.getCode()));
        task.setErrorMessage(FileUtils.truncate(StrUtils.hasText(message) ? message : (resultCode == null ? null : resultCode.getMessage()), 240));
        fileUploadTaskRepository.updateById(task);
    }

    /**
     * 清理分片上传的临时资源：删除数据库中的分片记录，并删除存储服务中的临时分片文件。
     * 仅对分片任务生效，非分片任务直接返回。
     */
    private void cleanupChunkArtifacts(FileUploadTask task) {
        if (!Integer.valueOf(1).equals(task.getIsChunked()) || task.getId() == null) {
            return;
        }
        fileChunkRepository.deleteByUploadTaskId(task.getId());
        if (!StrUtils.hasText(task.getUploadId())) {
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
     * 插入或更新分片记录（幂等操作）。
     * 若分片记录已存在则更新 MD5、大小和上传状态；否则新建记录。
     */
    private void upsertChunkRecord(Long taskId, Integer chunkNumber, long size, String md5) {
        FileChunk chunk = fileChunkRepository.findByTaskIdAndChunkNumber(taskId, chunkNumber);
        if (chunk == null) {
            chunk = fileUploadConvert.toFileChunk(taskId, chunkNumber, size, md5);
            fileChunkRepository.save(chunk);
            return;
        }
        chunk.setChunkSize(size);
        chunk.setChunkMd5(md5);
        chunk.setUploadStatus(CHUNK_STATUS_COMPLETED);
        chunk.setUploadTime(LocalDateTime.now());
        fileChunkRepository.updateById(chunk);
    }

    /**
     * 统计指定任务下已完成（状态为 COMPLETED）的分片数量。
     */
    private int countUploadedChunks(Long taskId) {
        return Math.toIntExact(fileChunkRepository.countByTaskIdAndStatus(taskId, CHUNK_STATUS_COMPLETED));
    }

    /**
     * 生成所有分片对应的存储对象名列表，顺序为 1~totalChunks。
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

    /**
     * 生成单个分片的临时存储对象名。
     */
    private String buildTempChunkObjectName(String uploadId, Integer chunkNumber) {
        return fileUploadProperties.getTempDirPrefix() + "/" + uploadId + "/chunk-" + chunkNumber + ".part";
    }

    /**
     * 校验上传初始化请求的合法性。
     * 检查请求对象、文件大小限制、扩展名、MD5、可见性、分片参数和引用类型。
     */
    private void validateInitRequest(UserUploadInitRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, FileResultCode.INIT_REQUEST_EMPTY);
        if (fileUploadProperties.getMaxFileSize() != null && request.getFileSize() > fileUploadProperties.getMaxFileSize()) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.MAX_UPLOAD_SIZE_EXCEEDED);
        }
        String ext = FileUtils.getExtension(request.getOriginalName());
        ExceptionThrowerCore.throwBusinessIfNot(isAllowedExtension(ext), FileResultCode.FILE_EXTENSION_NOT_ALLOWED);
        ExceptionThrowerCore.throwBusinessIf(Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check()) && !StrUtils.hasText(request.getFileMd5()), FileResultCode.FILE_MD5_REQUIRED);
        validateChunkRequest(request);
    }

    /**
     * 检查文件扩展名是否在允许列表中。
     * 若允许列表为空或扩展名为空，则默认允许。
     */
    private boolean isAllowedExtension(String ext) {
        if (!StrUtils.hasText(ext)) {
            return true;
        }
        List<String> allowed = fileUploadProperties.getAllowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        for (String item : allowed) {
            if (StrUtils.hasText(item) && normalized.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验分片上传相关参数。
     * 要求：总分片数 > 1、分片大小 > 0、若配置了分片参数则必须同时提供总数和大小。
     */
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

    /**
     * 判断请求是否应采用分片上传模式。
     * 满足以下任一条件时视为分片：
     * 1. 显式指定总分片数且 > 1；
     * 2. 文件大小超过配置的分片阈值。
     */
    private boolean isChunked(UserUploadInitRequest request) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 1) {
            return true;
        }
        long threshold = fileUploadProperties.getChunkSizeThreshold() == null ? 0L : fileUploadProperties.getChunkSizeThreshold();
        return threshold > 0 && request.getFileSize() != null && request.getFileSize() > threshold;
    }

    /**
     * 解析分片大小。
     * 优先使用请求中指定的值，否则使用配置默认值。
     */
    private long resolveChunkSize(UserUploadInitRequest request) {
        if (request.getChunkSize() != null && request.getChunkSize() > 0) {
            return request.getChunkSize();
        }
        return fileUploadProperties.getChunkSize() == null ? 5242880L : fileUploadProperties.getChunkSize();
    }

    /**
     * 计算总分片数。
     * 优先使用请求中指定的值，否则根据文件大小和分片大小自动计算。
     * 计算公式：ceil(文件大小 / 分片大小)。
     */
    private int resolveTotalChunks(UserUploadInitRequest request, long chunkSize) {
        if (request.getTotalChunks() != null && request.getTotalChunks() > 0) {
            return request.getTotalChunks();
        }
        long fileSize = request.getFileSize() == null ? 0L : request.getFileSize();
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    /**
     * 构建文件在存储服务中的最终对象名。
     * 格式：<分类>/<年/月/日>/<UUID><扩展名>，例如：image/2026/05/03/a1b2c3d4.jpg
     */
    private String buildFinalObjectName(String category, String originalName) {
        String normalizedCategory = FileCategoryEnum.normalize(category);
        String ext = FileUtils.normalizeExt(FileUtils.getExtension(originalName));
        String datePath = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = StrUtils.hasText(ext) ? ("." + ext) : "";
        return normalizedCategory + "/" + datePath + "/" + uuid + suffix;
    }

    /**
     * 计算过期时间点。
     * 传入天数若为 null 或 <= 0 则默认 2 天；同时保证最少为 1 天。
     */
    private LocalDateTime expireAfterDays(Integer days) {
        int d = days == null ? 2 : Math.max(1, days);
        return LocalDateTime.now().plusDays(d);
    }

    private record PersistedFileInfo(FileInfo fileInfo, boolean reusedExisting) {
    }
}

package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.MediaAssetPathUtils;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件生命周期收口服务实现。
 * 统一处理过期任务、引用计数与引用归零后的物理资源回收，避免逻辑散落在多个模块。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileLifecycleServiceImpl implements FileLifecycleService {
    private static final int EXPIRED_TASK_BATCH_SIZE = 100;
    private static final int PHYSICAL_DELETE_BATCH_SIZE = 100;

    private final FileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository fileUploadTaskRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final StorageManager storageManager;

    /**
     * 通过子查询实时重算引用数，避免并发上传或删除时由旧实体覆盖最新计数。
     */
    @Override
    public void refreshReferenceMetadata(Long fileId, boolean promotePublic) {
        if (fileId == null) {
            return;
        }
        fileInfoRepository.refreshReferenceMetadata(fileId, promotePublic);
    }

    /**
     * 在最后一个业务引用移除后，把文件标记为已删除并尽力回收物理对象。
     * 若并发下已有新引用写入，则只回刷引用计数，不执行删除动作。
     */
    @Override
    public void syncFileAfterReferenceRemoval(Long fileId) {
        if (fileId == null) {
            return;
        }
        // 查询文件实体，文件不存在直接返回
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        if (fileInfo == null) {
            return;
        }

        // 实时重算当前引用数（避免并发上传或删除时旧实体覆盖最新计数）
        long remaining = countReferences(fileId);
        if (remaining > 0) {
            // 仍有有效引用，仅回刷引用计数，不执行删除
            refreshReferenceMetadata(fileId, false);
            return;
        }

        // 无剩余引用，尝试将文件标记为已删除（PHYSICAL_DELETE_PENDING）
        boolean markedDeleted = fileInfoRepository.markDeletedIfNoReferences(fileId);
        if (!markedDeleted) {
            // 并发情况下已有新引用写入，仅回刷引用计数
            refreshReferenceMetadata(fileId, false);
            return;
        }

        // 标记成功，执行物理文件回收（派生资源 + 原文件）
        cleanupPhysicalFile(fileInfo);
    }

    /**
     * 请求链路命中过期任务时立即收口，避免过期任务在定时任务下一轮执行前继续被消费。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean expireTaskIfNeeded(FileUploadTask task) {
        // 参数校验：任务为空或未过期直接返回
        if (task == null || task.getExpireTime() == null || !task.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        // 已完成任务不处理（已完成的任务不应该被过期）
        if (TaskStatusEnum.COMPLETED.getValue().equals(task.getTaskStatus())) {
            return false;
        }
        // 执行过期清理：状态收口 + 分片资源清理
        cleanupExpiredTask(task);
        return true;
    }

    /**
     * 分批扫描并清理已过期的非完成任务，保证历史临时资源不会无限残留。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredUploadTasks() {
        int cleaned = 0;
        while (true) {
            // 批量加载过期任务（只查询 INIT/UPLOADING/MERGING 状态）
            List<FileUploadTask> batch = loadExpiredTasks(LocalDateTime.now());
            if (batch.isEmpty()) {
                return cleaned;
            }
            // 逐个清理过期任务，累加计数
            for (FileUploadTask task : batch) {
                cleanupExpiredTask(task);
                cleaned++;
            }
            // 若本批数量小于批次上限，说明已无更多待处理数据，退出循环
            if (batch.size() < EXPIRED_TASK_BATCH_SIZE) {
                return cleaned;
            }
        }
    }

    /**
     * 分批扫描待物理删除的文件，重试物理删除，成功后标记为已删除。
     */
    @Override
    public int retryPhysicalDeletePendingFiles() {
        int processed = 0;
        while (true) {
            // 批量查询待物理删除的文件（状态为 PHYSICAL_DELETE_PENDING）
            List<FileInfo> batch = fileInfoRepository.listByStatus(
                    FileStatusEnum.PHYSICAL_DELETE_PENDING.getValue(), PHYSICAL_DELETE_BATCH_SIZE);
            if (batch.isEmpty()) {
                return processed;
            }
            for (FileInfo file : batch) {
                try {
                    // 根据文件存储密钥获取对应存储服务
                    StorageService storageService = storageManager.getStorageService(file.getStorageKey());
                    if (storageService != null) {
                        // 执行物理删除
                        storageService.delete(file.getFilePath());
                    }
                    // 物理删除成功后更新数据库状态为 DELETED
                    file.setStatus(FileStatusEnum.DELETED.getValue());
                    fileInfoRepository.updateById(file);
                    processed++;
                } catch (Exception e) {
                    // 物理删除失败仅记日志，下次调度继续重试
                    log.warn("物理文件删除重试失败, fileId={}, path={}", file.getId(), file.getFilePath());
                }
            }
            // 若本批数量小于批次上限，说明已无更多待处理数据，退出循环
            if (batch.size() < PHYSICAL_DELETE_BATCH_SIZE) {
                return processed;
            }
        }
    }

    private List<FileUploadTask> loadExpiredTasks(LocalDateTime now) {
        return fileUploadTaskRepository.findExpiredTasks(
                now,
                List.of(
                        TaskStatusEnum.INIT.getValue(),
                        TaskStatusEnum.UPLOADING.getValue(),
                        TaskStatusEnum.MERGING.getValue()
                ),
                EXPIRED_TASK_BATCH_SIZE
        );
    }

    /**
     * 统一处理过期任务的终态更新与临时资源清理。数据库终态收口优先，存储清理仅尽力而为。
     */
    private void cleanupExpiredTask(FileUploadTask task) {
        if (task == null) {
            return;
        }
        Integer status = task.getTaskStatus();
        // 仅对初始化中/上传中/合并中的任务执行过期收口（已完成/取消的不处理）
        if (TaskStatusEnum.INIT.getValue().equals(status)
                || TaskStatusEnum.UPLOADING.getValue().equals(status)
                || TaskStatusEnum.MERGING.getValue().equals(status)) {
            // 状态收口为取消，设置错误码和完成时间
            task.setTaskStatus(TaskStatusEnum.CANCELLED.getValue());
            task.setErrorCode(String.valueOf(FileResultCode.UPLOAD_TASK_EXPIRED.getCode()));
            task.setErrorMessage(FileResultCode.UPLOAD_TASK_EXPIRED.getMessage());
            task.setCompleteTime(LocalDateTime.now());
            fileUploadTaskRepository.updateById(task);
        }
        // 清理分片元数据和临时目录资源
        cleanupChunkArtifacts(task);
    }

    /**
     * 过期或完成后的分片任务统一清理分片元数据和临时目录。
     * 存储删除失败只记日志，不阻塞数据库状态收口。
     */
    private void cleanupChunkArtifacts(FileUploadTask task) {
        // 非分片任务或任务ID为空，直接跳过
        if (!Integer.valueOf(1).equals(task.getIsChunked()) || task.getId() == null) {
            return;
        }
        // 删除分片元数据记录
        fileChunkRepository.deleteByUploadTaskId(task.getId());
        // 无 uploadId 说明从未真正使用过临时目录，跳过
        if (!StringUtils.hasText(task.getUploadId())) {
            return;
        }
        try {
            // 根据存储密钥获取存储服务，清理临时分片文件
            StorageService storageService = storageManager.getStorageService(task.getStorageKey());
            if (storageService != null) {
                storageService.deleteTempFiles(task.getUploadId());
            }
        } catch (Exception e) {
            // 临时目录清理失败仅记日志，不影响数据库收口
            log.warn("清理过期上传任务临时分片失败, uploadId={}", task.getUploadId(), e);
        }
    }

    private long countReferences(Long fileId) {
        return fileBusinessInfoRepository.countByFileId(fileId);
    }

    private void cleanupPhysicalFile(FileInfo fileInfo) {
        try {
            // 根据文件存储密钥获取对应存储服务
            StorageService storageService = storageManager.getStorageService(fileInfo.getStorageKey());
            if (storageService != null) {
                // 依次删除派生媒体资源（聊天图片缩略图、语音预览），删除失败不阻断原文件删除
                deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatImageThumbnailPath(fileInfo.getFilePath()));
                deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatVoicePreviewPath(fileInfo.getFilePath()));
                // 删除原文件
                storageService.delete(fileInfo.getFilePath());
            }
        } catch (Exception e) {
            // 物理文件回收失败记日志，后续由重试调度继续处理
            log.warn("回收无引用文件的物理对象失败, fileId={}", fileInfo.getId(), e);
        }
    }

    /**
     * 聊天附件的派生资源当前不单独建 file_info，因此在原文件物理回收时按约定路径一并尽力删除。
     * 删除失败仅记日志，不阻断主流程。
     */
    private void deleteDerivedMediaAsset(StorageService storageService, String objectPath) {
        // 路径为空直接跳过
        if (!StringUtils.hasText(objectPath)) {
            return;
        }
        try {
            storageService.delete(objectPath);
        } catch (Exception ex) {
            log.warn("回收派生媒体资源失败, objectPath={}", objectPath, ex);
        }
    }
}


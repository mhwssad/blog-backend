package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.common.storage.MediaAssetPathUtils;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
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

import java.util.Date;
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
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        if (fileInfo == null) {
            return;
        }
        long remaining = countReferences(fileId);
        if (remaining > 0) {
            refreshReferenceMetadata(fileId, false);
            return;
        }
        boolean markedDeleted = fileInfoRepository.markDeletedIfNoReferences(fileId);
        if (!markedDeleted) {
            refreshReferenceMetadata(fileId, false);
            return;
        }
        cleanupPhysicalFile(fileInfo);
    }

    /**
     * 请求链路命中过期任务时立即收口，避免过期任务在定时任务下一轮执行前继续被消费。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean expireTaskIfNeeded(FileUploadTask task) {
        if (task == null || task.getExpireTime() == null || !task.getExpireTime().before(new Date())) {
            return false;
        }
        if (TaskStatusEnum.COMPLETED.getValue().equals(task.getTaskStatus())) {
            return false;
        }
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
            List<FileUploadTask> batch = loadExpiredTasks(new Date());
            if (batch.isEmpty()) {
                return cleaned;
            }
            for (FileUploadTask task : batch) {
                cleanupExpiredTask(task);
                cleaned++;
            }
            if (batch.size() < EXPIRED_TASK_BATCH_SIZE) {
                return cleaned;
            }
        }
    }

    private List<FileUploadTask> loadExpiredTasks(Date now) {
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
        if (TaskStatusEnum.INIT.getValue().equals(status)
                || TaskStatusEnum.UPLOADING.getValue().equals(status)
                || TaskStatusEnum.MERGING.getValue().equals(status)) {
            task.setTaskStatus(TaskStatusEnum.CANCELLED.getValue());
            task.setErrorCode(String.valueOf(FileResultCode.UPLOAD_TASK_EXPIRED.getCode()));
            task.setErrorMessage(FileResultCode.UPLOAD_TASK_EXPIRED.getMessage());
            task.setCompleteTime(new Date());
            fileUploadTaskRepository.updateById(task);
        }
        cleanupChunkArtifacts(task);
    }

    /**
     * 过期或完成后的分片任务统一清理分片元数据和临时目录。
     * 存储删除失败只记日志，不阻塞数据库状态收口。
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
        } catch (Exception e) {
            log.warn("清理过期上传任务临时分片失败, uploadId={}", task.getUploadId(), e);
        }
    }

    private long countReferences(Long fileId) {
        return fileBusinessInfoRepository.countByFileId(fileId);
    }

    private void cleanupPhysicalFile(FileInfo fileInfo) {
        try {
            StorageService storageService = storageManager.getStorageService(fileInfo.getStorageKey());
            if (storageService != null) {
                deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatImageThumbnailPath(fileInfo.getFilePath()));
                deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatVoicePreviewPath(fileInfo.getFilePath()));
                storageService.delete(fileInfo.getFilePath());
            }
        } catch (Exception e) {
            log.warn("回收无引用文件的物理对象失败, fileId={}", fileInfo.getId(), e);
        }
    }

    /**
     * 聊天附件的派生资源当前不单独建 file_info，因此在原文件物理回收时按约定路径一并尽力删除。
     */
    private void deleteDerivedMediaAsset(StorageService storageService, String objectPath) {
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


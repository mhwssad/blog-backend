package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.domain.file.FileUploadTask;

/**
 * 文件生命周期收口服务。
 * 负责过期任务清理、引用计数刷新与引用归零后的文件状态同步。
 */
public interface FileLifecycleService {
    /**
     * 按真实引用关系刷新文件引用计数，并按需提升公开性。
     *
     * @param fileId        文件 ID
     * @param promotePublic 是否同步将文件标记为公开
     */
    void refreshReferenceMetadata(Long fileId, boolean promotePublic);

    /**
     * 在文件引用被删除后同步文件状态；若引用归零则尽力回收物理文件。
     *
     * @param fileId 文件 ID
     */
    void syncFileAfterReferenceRemoval(Long fileId);

    /**
     * 发现任务已过期时立即收口当前任务及其临时资源。
     *
     * @param task 上传任务
     * @return 是否已按过期逻辑收口
     */
    boolean expireTaskIfNeeded(FileUploadTask task);

    /**
     * 批量清理过期且未完成的上传任务。
     *
     * @return 本次清理的任务数量
     */
    int cleanupExpiredUploadTasks();

    /**
     * 扫描待物理删除的文件并重试删除，成功后标记为已删除。
     *
     * @return 本轮成功处理的文件数量
     */
    int retryPhysicalDeletePendingFiles();
}

# 文件生命周期管理

## 1. 概述

文件生命周期管理收口在 `FileLifecycleService`，统一处理：
- 引用计数刷新与状态同步
- 引用归零后的文件回收
- 过期任务清理
- 物理删除重试

## 2. 引用计数机制

### 2.1 核心概念

文件通过 `FileBusinessInfo`（业务引用表）与业务场景关联：
- 一个文件可以有多个业务引用
- `referenceCount` 字段记录引用数（存在数据不一致风险）

### 2.2 实时重算

为避免并发上传或删除时旧实体覆盖最新计数，File 模块通过子查询实时重算：

```java
// FileInfoRepository.refreshReferenceMetadata
fileInfoRepository.refreshReferenceMetadata(fileId, promotePublic);
```

**promotePublic 参数**：当有新公开引用时，同步将文件标记为公开。

## 3. 文件状态流转

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            文件状态流转图                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌──────────┐      上传完成       ┌──────────┐      引用归零      ┌──────┐  │
│    │ DELETED  │◄───────────────────│ PHYSICAL │◄─────────────────│NORMAL│  │
│    │  (已删除) │                    │ DELETE   │                   │(正常) │  │
│    └──────────┘      物理删除成功     │_PENDING  │                   └──────┘  │
│           ▲                     (待物理删除)    │    引用 > 0            │     │
│           │                            ▲        │                       │     │
│           │                            │        │                       │     │
│           │                     物理删除失败                           │     │
│           │                     (进入重试队列)                          │     │
│           │                                                         │     │
│           │                                                         │     │
│    ┌──────┴──────┐                                                  │     │
│    │  REVIEWING  │◄──────────────── 管理员下架 ────────────────────────┘     │
│    │   (审核中)  │                                                        │
│    └─────────────┘                                                        │
│           │                                                               │
│           │ 管理员认定违规                                                 │
│           ▼                                                               │
│    ┌──────────────┐                                                        │
│    │  VIOLATION   │                                                        │
│    │  (违规下架)  │                                                        │
│    └──────────────┘                                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.1 状态说明

| 状态值 | 枚举 | 说明 |
|--------|------|------|
| 0 | DELETED | 已删除（物理文件已清除） |
| 1 | NORMAL | 正常（可访问） |
| 2 | PHYSICAL_DELETE_PENDING | 待物理删除（引用归零，等待清理） |
| 3 | REVIEWING | 审核中 |
| 4 | VIOLATION | 违规下架 |

## 4. 引用归零回收流程

当文件的最后一个业务引用被删除时：

```java
// FileLifecycleServiceImpl.syncFileAfterReferenceRemoval
public void syncFileAfterReferenceRemoval(Long fileId) {
    // 1. 查询文件实体
    FileInfo fileInfo = fileInfoRepository.getById(fileId);
    if (fileInfo == null) return;

    // 2. 实时重算引用数
    long remaining = countReferences(fileId);
    if (remaining > 0) {
        refreshReferenceMetadata(fileId, false);
        return;
    }

    // 3. 标记为待物理删除
    boolean marked = fileInfoRepository.markDeletedIfNoReferences(fileId);
    if (!marked) {
        // 并发新引用已写入，仅回刷计数
        refreshReferenceMetadata(fileId, false);
        return;
    }

    // 4. 执行物理文件回收
    cleanupPhysicalFile(fileInfo);
}
```

### 4.1 并发保护

- 引用归零判断基于实时重算，避免旧实体覆盖
- 标记删除时使用乐观锁，防止并发创建新引用导致误删
- 物理删除失败不阻塞数据库状态更新

## 5. 后台删除流程

```java
// FileAdminServiceImpl.deleteFile
public void deleteFile(Long id) {
    // 1. 清理业务引用
    fileBusinessInfoRepository.deleteByFileId(id);

    // 2. 清理上传任务和临时分片
    List<FileUploadTask> tasks = fileUploadTaskRepository.listByFileId(id);
    for (FileUploadTask task : tasks) {
        fileChunkRepository.deleteByUploadTaskId(task.getId());
        storageService.deleteTempFiles(task.getUploadId());
    }

    // 3. 尝试物理删除
    boolean physicalDeleted = true;
    try {
        storageService.delete(file.getFilePath());
    } catch (Exception e) {
        physicalDeleted = false;
    }

    // 4. 标记状态
    file.setStatus(physicalDeleted ? DELETED : PHYSICAL_DELETE_PENDING);
    fileInfoRepository.updateById(file);
}
```

**设计考虑**：
- 物理文件删除失败时不回滚元数据删除，避免被外部存储异常长期阻塞
- 由 `FilePhysicalDeleteRetryScheduler` 后续重试

## 6. 定时任务

### 6.1 过期任务清理

```java
// FileUploadTaskCleanupScheduler
@Scheduled(cron = "${file-upload.expired-task-cleanup-cron:0 0 * * hour?}")
public void cleanupExpiredUploadTasks() {
    int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();
}
```

**扫描条件**：
- 任务状态：INIT、UPLOADING、MERGING
- 过期时间 < 当前时间

**清理动作**：
- 状态收口为 CANCELLED
- 设置错误码 UPLOAD_TASK_EXPIRED
- 清理分片元数据和临时目录

### 6.2 物理删除重试

```java
// FilePhysicalDeleteRetryScheduler
@Scheduled(cron = "${file-upload.physical-delete-retry-cron:0 30 * * hour?}")
public void retryPhysicalDelete() {
    int processed = fileLifecycleService.retryPhysicalDeletePendingFiles();
}
```

**扫描条件**：
- 文件状态：PHYSICAL_DELETE_PENDING

**重试动作**：
- 执行物理删除
- 成功则状态改为 DELETED
- 失败仅记日志，下次继续重试

## 7. 派生资源回收

聊天附件的派生资源（缩略图、预览音）不单独建 `FileInfo`，在原文件物理回收时按约定路径一并清理：

```java
// FileLifecycleServiceImpl.cleanupPhysicalFile
private void cleanupPhysicalFile(FileInfo fileInfo) {
    StorageService storageService = storageManager.getStorageService(fileInfo.getStorageKey());

    // 删除派生资源
    deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatImageThumbnailPath(fileInfo.getFilePath()));
    deleteDerivedMediaAsset(storageService, MediaAssetPathUtils.buildChatVoicePreviewPath(fileInfo.getFilePath()));

    // 删除原文件
    storageService.delete(fileInfo.getFilePath());
}
```

## 8. 与聊天模块的集成

文件生命周期与聊天模块通过 `FileChatFacadeService` 交互：

```java
// 绑定消息时刷新引用
fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), isPublic);

// 解绑消息时触发回收
fileIds.forEach(fileLifecycleService::syncFileAfterReferenceRemoval);
```

**关键保证**：
- 引用绑定/解绑与文件生命周期状态同步在统一事务边界内
- 聊天消息删除时自动触发文件回收判断
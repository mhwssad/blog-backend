# File 模块 Repository 迁移计划

## 当前状态

- 已完成 `FileInfoRepository`、`FileUploadTaskRepository`、`FileChunkRepository`、`FileBusinessInfoRepository` 及其实现。
- 已完成 `UserFileServiceImpl`、`FileAdminServiceImpl`、`FileLifecycleServiceImpl` 的 Repository 迁移，`file` 模块业务服务内不再直接拼装 `lambdaQuery/lambdaUpdate/LambdaQueryWrapper`。
- 已同步把 `ArticleAdminServiceImpl`、`ChatAdminServiceImpl`、`UserChatServiceImpl`、`ChatAttachmentAsyncProcessingServiceImpl` 的跨模块文件数据访问切到 Repository。
- 已删除 `FileInfoService`、`FileUploadTaskService`、`FileChunkService`、`FileBusinessInfoService` 及对应薄实现。
- 已同步调整 `UserFileServiceImplTest`、`FileAdminServiceImplTest`、`FileLifecycleServiceImplTest`，并更新受影响的 article/chat 测试依赖。
- 仓库全量 Maven 校验仍被无关的 `auth` 编译错误阻塞：`src/main/java/com/cybzacg/blogbackend/module/auth/service/impl/AuthServiceImpl.java` 中 `org.springframework.security.authentication.AuthenticationException` 无法解析。

## 模块信息

- **优先级**：第4轮
- **复杂度**：中
- **前置依赖**：无
- **涉及薄服务**：4个
- **涉及业务服务**：3个
- **数据访问总数**：约46处

## Repository 列表

| Repository 接口 | 对应实体 | 薄服务来源 | Mapper自定义方法 |
|---|---|---|---|
| `FileInfoRepository` | FileInfo | FileInfoService | 无 |
| `FileUploadTaskRepository` | FileUploadTask | FileUploadTaskService | 无 |
| `FileChunkRepository` | FileChunk | FileChunkService | 无 |
| `FileBusinessInfoRepository` | FileBusinessInfo | FileBusinessInfoService | 无 |

## 各 Repository 方法设计

### FileInfoRepository

```java
public interface FileInfoRepository extends IService<FileInfo> {
    FileInfo findByMd5AndNormalStatus(String md5, Integer status);
    FileInfo findByMd5(String md5);
    List<FileInfo> findIdsByStatusAndKeyword(Integer status, String keyword);
    Page<FileInfo> pageAdminFiles(FileAdminPageQuery query);
    void recalcReferenceCount(Long fileId, boolean isPublic);
    void markDeletedIfNoReferences(Long fileId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByMd5AndNormalStatus` | UserFileServiceImpl:465, `lambdaQuery().eq(md5).eq(status).one()` | 秒传查重 |
| `findByMd5` | UserFileServiceImpl:481, `lambdaQuery().eq(md5).one()` | MD5查文件 |
| `findIdsByStatusAndKeyword` | UserFileServiceImpl:291, `lambdaQuery().select(id).eq(status).and(keyword).list()` | 条件查ID列表 |
| `pageAdminFiles` | FileAdminServiceImpl:54, `LambdaQueryWrapper + page()` | 管理端分页 |
| `recalcReferenceCount` | FileLifecycleServiceImpl:53, `LambdaUpdateWrapper.setSql(...)` | 重算引用计数 |
| `markDeletedIfNoReferences` | FileLifecycleServiceImpl:80, `LambdaUpdateWrapper + not exists` | 无引用标记删除 |

### FileUploadTaskRepository

```java
public interface FileUploadTaskRepository extends IService<FileUploadTask> {
    FileUploadTask findByUploadIdAndUserId(String uploadId, Long userId);
    Page<FileUploadTask> pageByUserAndStatus(UserTaskPageQuery query);
    Page<FileUploadTask> pageAdminTasks(AdminTaskPageQuery query);
    List<FileUploadTask> findExpiredTasks(Date expireTime, List<Integer> statuses, int limit);
    List<FileUploadTask> listRecentByFileId(Long fileId, int limit);
    List<FileUploadTask> listByFileId(Long fileId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByUploadIdAndUserId` | UserFileServiceImpl:444, `lambdaQuery().eq(uploadId).eq(userId).one()` | 查上传任务 |
| `pageByUserAndStatus` | UserFileServiceImpl:327, `LambdaQueryWrapper + page()` | 用户任务分页 |
| `pageAdminTasks` | FileAdminServiceImpl:111, `LambdaQueryWrapper + page()` | 管理端任务分页 |
| `findExpiredTasks` | FileLifecycleServiceImpl:131, `lambdaQuery().le(expireTime).in(statuses).list()` | 查过期任务 |
| `listRecentByFileId` | FileAdminServiceImpl:91, `lambdaQuery().eq(fileId).last("limit 20").list()` | 文件关联任务 |
| `listByFileId` | FileAdminServiceImpl:150, `lambdaQuery().eq(fileId).list()` | 文件所有任务 |

### FileChunkRepository

```java
public interface FileChunkRepository extends IService<FileChunk> {
    FileChunk findByTaskAndChunkNumber(Long uploadTaskId, Integer chunkNumber);
    long countCompletedByTaskId(Long uploadTaskId);
    boolean deleteByUploadTaskId(Long uploadTaskId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByTaskAndChunkNumber` | UserFileServiceImpl:694, `lambdaQuery().eq(taskId).eq(chunkNumber).one()` | 查分片 |
| `countCompletedByTaskId` | UserFileServiceImpl:717, `lambdaQuery().eq(taskId).eq(COMPLETED).count()` | 统计完成分片 |
| `deleteByUploadTaskId` | UserFileServiceImpl:678, `remove(LambdaQueryWrapper)` | 删除任务分片 |

### FileBusinessInfoRepository

```java
public interface FileBusinessInfoRepository extends IService<FileBusinessInfo> {
    FileBusinessInfo findByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId);
    FileBusinessInfo findLatestByFileUserReference(Long fileId, Long userId, String referenceType, Long referenceId);
    Page<FileBusinessInfo> pageByUserAndFilters(UserFilePageQuery query);
    List<FileBusinessInfo> listByFileId(Long fileId);
    List<FileBusinessInfo> listByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<FileBusinessInfo> listArticleAttachments(Long articleId);
    List<FileBusinessInfo> listByChatMessageId(Long messageId);
    long countByFileId(Long fileId);
    boolean deleteByFileId(Long fileId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByFileUserReference` | UserFileServiceImpl:615, `lambdaQuery().eq(fileId).eq(userId).eq(refType).eq(refId).one()` | 查业务引用 |
| `findLatestByFileUserReference` | UserFileServiceImpl:654, `lambdaQuery()...last("limit 1").one()` | 查最新引用 |
| `pageByUserAndFilters` | UserFileServiceImpl:303, `LambdaQueryWrapper + page()` | 用户文件分页 |
| `listByFileId` | FileAdminServiceImpl:82, `lambdaQuery().eq(fileId).list()` | 文件关联业务 |
| `listByReferenceTypeAndReferenceId` | 跨模块通用 | 按类型+ID查引用 |
| `listArticleAttachments` | ArticleAdminServiceImpl:555 | 文章附件 |
| `listByChatMessageId` | UserChatServiceImpl:994, ChatAdminServiceImpl:565 | 聊天附件 |
| `countByFileId` | FileLifecycleServiceImpl:186, `lambdaQuery().eq(fileId).count()` | 引用计数 |
| `deleteByFileId` | FileAdminServiceImpl:148, `remove(LambdaQueryWrapper)` | 删除文件关联 |

## 执行步骤

### Step 1: 创建4个 Repository 接口 + 4个实现

- 已完成。

### Step 2: 修改3个业务服务

1. `UserFileServiceImpl` — 已完成
2. `FileAdminServiceImpl` — 已完成
3. `FileLifecycleServiceImpl` — 已完成

### Step 3: 更新测试

- 已完成 file 模块核心服务测试切换，并同步更新受影响的 article/chat 测试依赖。

### Step 4: 删除旧薄服务

- 已完成。

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.file.*Test"
```

当前验证结果：

- `rg` 已确认 file 模块业务服务内无直接数据访问拼装。
- `rg` 已确认仓库内无 `FileInfoService` / `FileUploadTaskService` / `FileChunkService` / `FileBusinessInfoService` 残留引用。
- `mvn -q -Dtest="UserFileServiceImplTest,FileAdminServiceImplTest,FileLifecycleServiceImplTest,ArticleAdminServiceImplTest,ChatAdminServiceImplTest,UserChatServiceImplTest,ChatAttachmentAsyncProcessingServiceImplTest" test` 被无关的 `auth` 编译错误阻塞，未能完成全量验证。

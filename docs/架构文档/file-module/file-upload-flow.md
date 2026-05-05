# 文件上传流程

## 1. 整体流程

File 模块支持三种上传模式：**普通上传**、**分片上传**、**秒传**。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              文件上传流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   用户                        服务端                        存储服务          │
│    │                           │                            │               │
│    │  1. initUploadTask        │                            │               │
│    │──────────────────────────>│                            │               │
│    │       uploadId + taskId   │                            │               │
│    │<─────────────────────────│                            │               │
│    │                           │                            │               │
│    │  2. quickCheck (可选)     │                            │               │
│    │──────────────────────────>│                            │               │
│    │       秒传结果            │                            │               │
│    │<─────────────────────────│                            │               │
│    │                           │                            │               │
│    │  3a. uploadFile           │  存储上传                   │               │
│    │  (普通模式)              ─────────────────────────────>│               │
│    │                           │                            │               │
│    │  OR                       │                            │               │
│    │                           │                            │               │
│    │  3b. uploadChunk × N      │  上传分片到临时目录          │               │
│    │  (分片模式)              ─────────────────────────────>│  temp/        │
│    │                           │                            │               │
│    │  4. completeUpload        │                            │               │
│    │──────────────────────────>│  合并分片                   │               │
│    │                           ─────────────────────────────>│               │
│    │                           │                            │               │
│    │  5. 业务引用创建           │                            │               │
│    │                           │                            │               │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. 接口详解

### 2.1 初始化上传任务

```
POST /api/user/files/upload-tasks/init
```

**请求体**：
```json
{
  "originalName": "example.jpg",
  "fileSize": 1048576,
  "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
  "mimeType": "image/jpeg",
  "referenceType": "temp",
  "referenceId": null,
  "isPublic": 0,
  "category": "image",
  "totalChunks": 1,
  "chunkSize": 1048576
}
```

**响应**：
```json
{
  "uploadId": "uuid-xxx",
  "taskId": 123,
  "chunkSize": 1048576,
  "totalChunks": 1,
  "uploadMode": "FULL_UPLOAD",
  "quickUploadAvailable": true,
  "completed": false
}
```

**核心逻辑**：
1. 参数合法性校验（文件大小、扩展名等）
2. 判断上传模式：分片数 > 1 或文件大小超过阈值 → 分片模式
3. 创建 `FileUploadTask` 记录，状态为 `INIT`
4. 秒传检测：MD5 + 文件大小命中 → 直接返回秒传结果
5. 返回后续上传所需信息

### 2.2 秒传检测

```
POST /api/user/files/upload-tasks/{uploadId}/quick-check
```

**响应**：
```json
{
  "fileId": 456,
  "businessId": 789,
  "quickUpload": true,
  "completed": true,
  "taskStatus": "COMPLETED"
}
```

**核心逻辑**：
1. 通过 uploadId 查找上传任务
2. 检查任务是否过期
3. 若任务已完成，直接返回关联文件
4. 再次尝试 MD5 秒传匹配

### 2.3 普通上传

```
POST /api/user/files/upload-tasks/{uploadId}/file
```

**核心逻辑**：
1. 获取上传任务，校验状态
2. 秒传复用检测
3. 上传到存储服务
4. 若未传 MD5，上传过程中自动计算
5. 创建或复用 FileInfo 记录
6. 建立业务引用，标记任务完成

### 2.4 分片上传

```
POST /api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}
```

**核心逻辑**：
1. 校验分片序号不超过总分片数
2. 上传分片到存储服务的临时目录
3. 插入或更新分片记录（幂等）
4. 更新任务进度（已上传分片数）

**分片临时对象名格式**：`{uploadId}/chunk-{chunkNumber}.part`

### 2.5 完成上传

```
POST /api/user/files/upload-tasks/{uploadId}/complete
```

**核心逻辑**：
1. 校验所有分片均已上传
2. 再次尝试 MD5 秒传（覆盖场景）
3. 执行分片合并
4. 若无 MD5，合并后从文件重新计算
5. 创建 FileInfo 记录，建立业务引用

## 3. 秒传机制

秒传的核心是 **MD5 + 文件大小** 双重匹配：

```
MD5 相同 + 文件大小相同 → 复用已有文件 → 直接完成
```

**秒传命中场景**：
- 初始化时命中：直接返回秒传结果
- 上传过程中命中：跳过实际上传，复用已有文件
- 合并后命中：删除刚合并的冗余文件，复用已有文件

**设计考虑**：
- 并发创建冲突处理：唯一键冲突时降级为查询已有记录
- 同一文件多业务引用：复用时引用计数仍需正确累加

## 4. 分片合并

分片上传完成后，存储服务执行合并操作：

```
temp/{uploadId}/chunk-1.part
temp/{uploadId}/chunk-2.part
...
temp/{uploadId}/chunk-N.part
        ↓
final/{category}/{date}/{uuid}.{ext}
```

**合并失败处理**：
- 数据库事务回滚，临时分片由定时任务清理
- 目标文件创建失败时，清理已合并的目标文件

## 5. 业务引用创建

上传完成后，通过 `finalizeTaskWithReference` 建立业务引用：

```
FileUploadTask + FileInfo → FileBusinessInfo
```

**关键字段**：
- `fileId`：关联的文件 ID
- `userId`：上传用户
- `referenceType`：引用类型（avatar、chat_message、article_attachment、temp）
- `referenceId`：业务 ID（如消息 ID）
- `isPublic`：是否公开
- `category`：文件分类

## 6. 存储服务抽象

File 模块不直接操作存储，而是通过 `StorageManager` 获取存储服务：

```java
StorageManager storageManager;
StorageService storageService = storageManager.getStorageService(storageKey);
storageService.upload(inputStream, objectName, mimeType);
```

**支持的存储类型**：
- `LocalStorageService`：本地文件系统
- `MinioStorageService`：MinIO 对象存储
- `OssStorageService`：阿里云 OSS

## 7. 错误处理

| 错误码 | 说明 | 处理方式 |
|--------|------|----------|
| 71001 | 未配置存储节点 | 初始化时阻止 |
| 71004 | 文件上传失败 | 标记任务失败 |
| 71006 | 分片序号非法 | 校验拒绝 |
| 71008 | 分片序号超限 | 校验拒绝 |
| 71011 | 分片未全部上传 | 完成时校验 |
| 71013 | 分片合并失败 | 事务回滚 |
| 71019 | 上传任务不存在 | 查询校验 |
| 71026 | 上传任务已过期 | 清理并拒绝 |

## 8. 定时清理

- **过期任务清理**（每小时）：扫描 INIT/UPLOADING/MERGING 状态且已过期的任务，标记为 CANCELLED，清理临时分片
- **物理删除重试**（每30分钟）：扫描 PHYSICAL_DELETE_PENDING 状态的文件，重试物理删除
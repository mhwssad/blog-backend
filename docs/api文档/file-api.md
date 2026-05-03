# 文件与上传 API

这份文档给前端同学处理上传、我的文件、后台文件库时使用。

相比单纯的接口清单，这里优先按上传流程来写，因为文件模块最常见的问题不是“某个接口怎么调”，而是“整个上传链路该怎么串起来”。

## 1. 快速接入

### 1.1 路由分组

| 路由前缀                 | 面向场景                 | 是否需要登录                    |
|----------------------|----------------------|---------------------------|
| `/api/public/files/**` | 公开文件访问代理（带文章权限校验） | 否，资源级校验仍生效               |
| `/api/user/files/**` | 登录用户上传、查询我的文件、查询上传任务 | 是                         |
| `/api/sys/files/**`  | 后台文件库、后台上传任务管理       | 是，且要求 `content:file:*` 权限 |

### 1.2 统一响应

统一返回：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

分页 `data` 固定为：

```json
{
  "total": 1,
  "current": 1,
  "size": 10,
  "records": []
}
```

### 1.3 上传场景怎么选

| 场景             | 推荐流程                    |
|----------------|-------------------------|
| 小文件直接上传        | 初始化任务 -> 普通上传           |
| 已算出 MD5，想先试秒传  | 初始化任务 -> 秒传检测           |
| 大文件分片上传        | 初始化任务 -> 多次上传分片 -> 完成上传 |
| 想展示上传历史 / 失败原因 | 查询上传任务列表                |
| 想展示“我的资源库”     | 查询我的文件列表                |

## 2. 用户上传接入流程

所有用户上传接口都要求：

```http
Authorization: Bearer <accessToken>
```

### 2.1 普通上传流程

适合小文件、头像、评论图片等单次提交场景。

1. `POST /api/user/files/upload-tasks/init`
2. 根据返回结果判断：
    - `completed=true`：初始化阶段已经完成，通常是秒传命中
    - `uploadMode=3`：继续走普通文件上传
3. `POST /api/user/files/upload-tasks/{uploadId}/file`

### 2.2 秒传流程

适合客户端已拿到 MD5，希望先判断是否能直接复用已有文件。

1. `POST /api/user/files/upload-tasks/init`
2. `POST /api/user/files/upload-tasks/{uploadId}/quick-check`
3. 如果返回 `quickUpload=true`，前端直接结束上传流程
4. 如果未命中，再按普通上传或分片上传继续

### 2.3 分片上传流程

适合大文件。

1. `POST /api/user/files/upload-tasks/init`
2. 根据 `totalChunks`、`chunkSize` 切片
3. 循环调用 `POST /api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}`
4. 全部分片成功后，调用 `POST /api/user/files/upload-tasks/{uploadId}/complete`

### 2.4 上传流程中的关键判断

| 字段                           | 位置     | 用途             |
|------------------------------|--------|----------------|
| `uploadId`                   | 初始化响应  | 后续所有上传接口都依赖它   |
| `uploadMode`                 | 初始化响应  | 判断是秒传、分片还是全量上传 |
| `completed`                  | 初始化响应  | 是否已在初始化阶段直接完成  |
| `taskStatus`                 | 多个响应都有 | 用于展示当前任务状态     |
| `fileId` / `fileUrl`         | 完成类响应  | 上传成功后用于回填业务表单  |
| `errorCode` / `errorMessage` | 上传任务列表 | 展示失败原因         |

## 3. 公开文件访问接口

### 3.1 代理访问文件

- 请求：`GET /api/public/files/{fileId}`
- 鉴权：否，但资源级权限校验仍生效
- 用途：代理文件下载并对关联文章做访问控制校验，防止私密/白名单文章附件被绕过文章权限直接访问
- 路径参数：`fileId`

响应说明：

- 返回文件二进制内容
- 对于文章附件，会校验当前用户是否有权访问该文件的关联文章
- 无权访问关联文章时返回 HTTP `403`

关键规则：

- 仅对 `article_attachment` 类型的引用做文章权限校验
- 其他类型的文件引用（如 `avatar`、`comment_image`）可直接访问
- 文件不存在或已删除时返回业务错误

## 4. 用户文件接口

### 4.1 初始化上传任务

- 请求：`POST /api/user/files/upload-tasks/init`
- 用途：任何上传流程的第一步
- 请求体：`FileUploadInitRequest`

| 字段              | 类型      | 必填   | 说明                                                   |
|-----------------|---------|------|------------------------------------------------------|
| `originalName`  | String  | 是    | 原始文件名                                                |
| `fileSize`      | Long    | 是    | 文件大小，字节                                              |
| `fileMd5`       | String  | 否    | 文件 MD5，建议传入以支持秒传；未传时服务端会在上传过程中自动计算             |
| `mimeType`      | String  | 否    | MIME 类型                                              |
| `referenceType` | String  | 否    | `avatar`、`article_attachment`、`comment_image`、`temp` |
| `referenceId`   | Long    | 否    | 引用对象 ID，未传按 `0` 处理                                   |
| `category`      | String  | 否    | 业务分类，如 `avatar`、`attachment`、`comment`、`temp`        |
| `isPublic`      | Integer | 否    | `0` 私有，`1` 公开                                        |
| `totalChunks`   | Integer | 否    | 分片上传时传总分片数                                           |
| `chunkSize`     | Long    | 否    | 分片大小                                                 |
| `remark`        | String  | 否    | 备注                                                   |

- 边界说明：
    - `fileMd5` 建议传入以支持秒传与完整性校验。未传时服务端会在上传过程中自动计算：普通上传使用流式 MD5 计算，分片上传在合并后从已合并文件计算。
    - `isPublic` 仅允许传 `0` 或 `1`，否则直接返回 `ILLEGAL_ARGUMENT`。
    - `referenceType` 仅支持 `avatar`、`article_attachment`、`comment_image`、`temp`。
    - `category` 仅支持 `avatar`、`attachment`、`comment`、`temp`。
    - 显式传分片参数时，`totalChunks` 与 `chunkSize` 必须同时传入；其中 `totalChunks` 必须大于 `1`，`chunkSize` 必须大于
      `0`。
    - 传入非法 `referenceType` 或 `category` 时，服务端会直接返回 `ILLEGAL_ARGUMENT`，不会再自动兜底成 `temp`。

- 响应字段：`FileUploadInitVO`

| 字段                     | 类型      | 说明                       |
|------------------------|---------|--------------------------|
| `taskId`               | Long    | 上传任务 ID                  |
| `uploadId`             | String  | 上传标识                     |
| `uploadMode`           | Integer | `1` 秒传，`2` 分片上传，`3` 全量上传 |
| `quickUploadAvailable` | Boolean | 是否具备秒传条件                 |
| `completed`            | Boolean | 是否已在初始化阶段直接完成            |
| `totalChunks`          | Integer | 总分片数                     |
| `chunkSize`            | Long    | 分片大小                     |
| `taskStatus`           | Integer | 任务状态                     |
| `fileId`               | Long    | 已命中的文件 ID                |
| `fileUrl`              | String  | 文件访问地址                   |
| `businessId`           | Long    | 业务引用 ID                  |

- 请求示例：

```json
{
  "originalName": "avatar.png",
  "fileSize": 245678,
  "fileMd5": "5d41402abc4b2a76b9719d911017c592",
  "mimeType": "image/png",
  "referenceType": "avatar",
  "referenceId": 1,
  "category": "avatar",
  "isPublic": 1,
  "remark": "用户头像上传"
}
```

- 响应示例（全量上传）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 101,
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "uploadMode": 3,
    "quickUploadAvailable": false,
    "completed": false,
    "totalChunks": 0,
    "chunkSize": 0,
    "taskStatus": 0,
    "fileId": null,
    "fileUrl": null,
    "businessId": null
  }
}
```

- 响应示例（秒传命中）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 102,
    "uploadId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "uploadMode": 1,
    "quickUploadAvailable": true,
    "completed": true,
    "totalChunks": 0,
    "chunkSize": 0,
    "taskStatus": 3,
    "fileId": 501,
    "fileUrl": "https://example.com/files/avatar_1.png",
    "businessId": 301
  }
}
```

### 4.2 秒传检测

- 请求：`POST /api/user/files/upload-tasks/{uploadId}/quick-check`
- 用途：在初始化后显式执行秒传判断
- 路径参数：`uploadId`
- 边界说明：
    - 若任务已过期，服务端会先把任务收口为 `5(已取消)`，并返回 `UPLOAD_TASK_EXPIRED`。
- 响应字段：`FileUploadResultVO`

| 字段               | 类型      | 说明       |
|------------------|---------|----------|
| `uploadId`       | String  | 上传标识     |
| `taskId`         | Long    | 任务 ID    |
| `fileId`         | Long    | 文件 ID    |
| `businessId`     | Long    | 业务引用 ID  |
| `quickUpload`    | Boolean | 是否通过秒传完成 |
| `taskStatus`     | Integer | 任务状态     |
| `fileUrl`        | String  | 文件访问地址   |
| `referenceCount` | Integer | 当前引用数    |

- 响应示例（秒传成功）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "taskId": 101,
    "fileId": 501,
    "businessId": 301,
    "quickUpload": true,
    "taskStatus": 3,
    "fileUrl": "https://example.com/files/avatar_1.png",
    "referenceCount": 2
  }
}
```

- 响应示例（未命中秒传）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "taskId": 101,
    "fileId": null,
    "businessId": null,
    "quickUpload": false,
    "taskStatus": 0,
    "fileUrl": null,
    "referenceCount": null
  }
}
```

### 4.3 普通上传

- 请求：`POST /api/user/files/upload-tasks/{uploadId}/file`
- 用途：全量上传一个完整文件
- `Content-Type`：`multipart/form-data`
- 表单字段：
    - `file`：必填，待上传文件
- 边界说明：
    - 如果初始化时已传 `fileMd5`，普通上传会先校验整文件 MD5；不一致时直接返回 `FILE_MD5_MISMATCH`。
    - 如果初始化时未传 `fileMd5`，服务端会在上传过程中自动计算 MD5（流式计算，不占用额外存储）。
    - 如果命中同 `MD5 + 文件大小` 的正常文件，会直接复用已有物理文件并完成当前任务。
    - 如果落存储失败，任务会被回写为失败状态，失败原因可通过“查询我的上传任务”接口查看。
    - 若任务已过期，服务端会立即收口当前任务并返回 `UPLOAD_TASK_EXPIRED`。
    - 已完成或已取消的任务不能继续上传，继续调用会返回 `UPLOAD_TASK_STATUS_INVALID`。
- 响应：`FileUploadResultVO`

### 4.4 上传分片

- 请求：`POST /api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}`
- 用途：上传单个分片
- `Content-Type`：`multipart/form-data`
- 路径参数：
    - `uploadId`
    - `chunkNumber`
- 表单字段：
    - `file`：必填，当前分片文件
    - `chunkMd5`：可选，当前分片 MD5
- 边界说明：
    - 如果服务端开启 MD5 校验且传入了 `chunkMd5`，会先校验当前分片 MD5；不一致时直接返回 `CHUNK_MD5_MISMATCH`。
    - 同一 `uploadId + chunkNumber` 重复上传时，会覆盖已有分片元数据并刷新该分片的上传时间与大小，不会新增第二条分片记录。
    - 若任务已过期，服务端会立即收口当前任务并返回 `UPLOAD_TASK_EXPIRED`。
    - 已完成、已取消或处于不允许续传状态的任务不能继续上传分片，继续调用会返回 `UPLOAD_TASK_STATUS_INVALID`。
- 响应字段：`ChunkUploadVO`

| 字段               | 类型      | 说明     |
|------------------|---------|--------|
| `uploadId`       | String  | 上传标识   |
| `chunkNumber`    | Integer | 当前分片序号 |
| `uploadedChunks` | Integer | 已上传分片数 |
| `totalChunks`    | Integer | 总分片数   |
| `taskStatus`     | Integer | 当前任务状态 |

### 4.5 完成上传

- 请求：`POST /api/user/files/upload-tasks/{uploadId}/complete`
- 用途：分片全部上传完成后触发合并
- 路径参数：`uploadId`
- 说明：
    - 仅适用于分片任务。
    - 服务端会校验分片完整性并执行合并。
    - 若分片未全部上传完成，会直接返回 `CHUNK_INCOMPLETE`。
    - 若完成阶段命中同 `MD5 + 文件大小` 的现有正常文件，会直接复用。
    - 若初始化时未传 `fileMd5`，服务端会在分片合并后自动从已合并文件计算 MD5；若计算失败会返回 `FILE_MD5_REQUIRED`。
    - 若分片合并失败，任务会被回写为失败状态，错误码为 `CHUNK_MERGE_FAILED`。
    - 合并完成后会尽力清理临时分片；清理失败不会改变已完成结果。
    - 本地存储模式下若任一临时分片缺失，完成上传会直接失败，不再跳过缺失分片继续合并。
    - 若任务已过期，服务端会立即收口当前任务并返回 `UPLOAD_TASK_EXPIRED`。
    - 已完成或已取消的任务不能重复调用完成接口，继续调用会返回 `UPLOAD_TASK_STATUS_INVALID`。
- 响应：`FileUploadResultVO`

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "uploadId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "taskId": 103,
    "fileId": 502,
    "businessId": 302,
    "quickUpload": false,
    "taskStatus": 3,
    "fileUrl": "https://example.com/files/report_2026.pdf",
    "referenceCount": 1
  }
}
```

### 4.6 查询我的文件

- 请求：`GET /api/user/files`
- 用途：我的资源库、上传结果选择器
- 查询参数：`UserFilePageQuery`

| 参数              | 类型      | 说明           |
|-----------------|---------|--------------|
| `current`       | Long    | 页码，默认 `1`    |
| `size`          | Long    | 每页条数，默认 `10` |
| `keyword`       | String  | 文件名关键字       |
| `status`        | Integer | 文件状态         |
| `category`      | String  | 业务分类         |
| `referenceType` | String  | 引用类型         |

- 边界说明：
    - `status` 过滤当前已生效。
    - 传入非法 `category`、`referenceType` 或 `status` 时会直接返回错误，不再静默兜底。

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "businessId": 301,
        "fileId": 501,
        "fileName": "avatar_1.png",
        "originalName": "avatar.png",
        "fileUrl": "https://example.com/files/avatar_1.png",
        "fileSize": 245678,
        "fileType": "image",
        "mimeType": "image/png",
        "category": "avatar",
        "referenceType": "avatar",
        "referenceId": 1,
        "isPublic": 1,
        "status": 1,
        "createdAt": "2026-03-28 15:30:00"
      },
      {
        "businessId": 302,
        "fileId": 502,
        "fileName": "report_2026.pdf",
        "originalName": "2026年度报告.pdf",
        "fileUrl": "https://example.com/files/report_2026.pdf",
        "fileSize": 1048576,
        "fileType": "document",
        "mimeType": "application/pdf",
        "category": "attachment",
        "referenceType": "article_attachment",
        "referenceId": 10,
        "isPublic": 0,
        "status": 1,
        "createdAt": "2026-03-29 10:00:00"
      }
    ]
  }
}
```

- 响应字段：`UserFileVO`

| 字段              | 类型       | 说明                                 |
|-----------------|----------|------------------------------------|
| `businessId`    | Long     | 业务引用 ID                            |
| `fileId`        | Long     | 文件 ID                              |
| `fileName`      | String   | 文件名                                |
| `originalName`  | String   | 原始文件名                              |
| `fileUrl`       | String   | 文件地址                               |
| `fileSize`      | Long     | 文件大小                               |
| `fileType`      | String   | `image`、`video`、`document`、`other` |
| `mimeType`      | String   | MIME 类型                            |
| `category`      | String   | 业务分类                               |
| `referenceType` | String   | 引用类型                               |
| `referenceId`   | Long     | 引用对象 ID                            |
| `isPublic`      | Integer  | 是否公开                               |
| `status`        | Integer  | 文件状态                               |
| `createdAt`     | DateTime | 引用创建时间                             |

### 4.7 查询我的上传任务

- 请求：`GET /api/user/files/upload-tasks`
- 用途：上传记录列表、失败重试提示
- 查询参数：`UserFileTaskPageQuery`

| 参数              | 类型      | 说明           |
|-----------------|---------|--------------|
| `current`       | Long    | 页码，默认 `1`    |
| `size`          | Long    | 每页条数，默认 `10` |
| `taskStatus`    | Integer | 任务状态         |
| `isQuickUpload` | Integer | 是否秒传，`0/1`   |
| `isChunked`     | Integer | 是否分片，`0/1`   |

- 边界说明：
    - 传入非法 `taskStatus` 时会直接返回 `UPLOAD_TASK_STATUS_INVALID`。
    - 进行中的上传任务若超过有效期，会被后台定时清理为 `5(已取消)`，并把 `UPLOAD_TASK_EXPIRED` 写入
      `errorCode/errorMessage`。

- 响应字段：`UserFileTaskVO`

| 字段               | 类型       | 说明     |
|------------------|----------|--------|
| `id`             | Long     | 任务 ID  |
| `uploadId`       | String   | 上传标识   |
| `fileId`         | Long     | 文件 ID  |
| `originalName`   | String   | 原始文件名  |
| `fileSize`       | Long     | 文件大小   |
| `isQuickUpload`  | Integer  | 是否秒传   |
| `isChunked`      | Integer  | 是否分片   |
| `chunkSize`      | Long     | 分片大小   |
| `totalChunks`    | Integer  | 总分片数   |
| `uploadedChunks` | Integer  | 已上传分片数 |
| `taskStatus`     | Integer  | 任务状态   |
| `errorCode`      | String   | 错误码    |
| `errorMessage`   | String   | 错误信息   |
| `startTime`      | DateTime | 开始时间   |
| `completeTime`   | DateTime | 完成时间   |
| `createdAt`      | DateTime | 创建时间   |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 103,
        "uploadId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
        "fileId": 502,
        "originalName": "2026年度报告.pdf",
        "fileSize": 10485760,
        "isQuickUpload": 0,
        "isChunked": 1,
        "chunkSize": 5242880,
        "totalChunks": 2,
        "uploadedChunks": 2,
        "taskStatus": 3,
        "errorCode": null,
        "errorMessage": null,
        "startTime": "2026-03-29 10:00:00",
        "completeTime": "2026-03-29 10:01:30",
        "createdAt": "2026-03-29 09:59:00"
      }
    ]
  }
}
```

### 4.8 删除我的文件引用

- 请求：`DELETE /api/user/files/{businessId}`
- 用途：删除当前用户自己的业务引用记录
- 路径参数：`businessId`
- 前端说明：
    - 删除的是“文件引用”，不是直接按 `fileId` 删除物理文件。
    - 若同一底层文件没有任何引用，系统会尝试清理存储对象，并把文件状态改为 `0`。

## 5. 后台文件管理接口

所有后台接口都要求登录，并且需要对应权限：

- `content:file:query`
- `content:file:update`
- `content:file:delete`

### 5.1 文件库接口

#### 接口速览

| 场景     | 方法     | 路径                           | 权限                    |
|--------|--------|------------------------------|-----------------------|
| 分页查询文件 | GET    | `/api/sys/files`             | `content:file:query`  |
| 查询文件详情 | GET    | `/api/sys/files/{id}`        | `content:file:query`  |
| 更新文件状态 | PUT    | `/api/sys/files/{id}/status` | `content:file:update` |
| 删除文件   | DELETE | `/api/sys/files/{id}`        | `content:file:delete` |

#### 分页查询文件

- 请求：`GET /api/sys/files`
- 查询参数：`FileAdminPageQuery`

| 参数              | 类型      | 说明             |
|-----------------|---------|----------------|
| `current`       | Long    | 页码，默认 `1`      |
| `size`          | Long    | 每页条数，默认 `10`   |
| `keyword`       | String  | 文件名 / 原始文件名关键字 |
| `uploadUserId`  | Long    | 上传用户 ID        |
| `status`        | Integer | 文件状态           |
| `category`      | String  | 业务分类           |
| `referenceType` | String  | 引用类型           |
| `isPublic`      | Integer | 是否公开           |

- 边界说明：
    - 传入非法 `category`、`referenceType` 或 `status` 时会直接返回错误。

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 501,
        "fileName": "avatar_1.png",
        "originalName": "avatar.png",
        "filePath": "/upload/2026/03/28/avatar_1.png",
        "fileUrl": "https://example.com/files/avatar_1.png",
        "storageKey": "local",
        "fileSize": 245678,
        "fileType": "image",
        "mimeType": "image/png",
        "fileExtension": ".png",
        "uploadUserId": 1,
        "isPublic": 1,
        "category": "avatar",
        "status": 1,
        "referenceCount": 2,
        "createdAt": "2026-03-28 15:30:00"
      }
    ]
  }
}
```

- 响应字段：`FileAdminVO`

| 字段               | 类型       | 说明      |
|------------------|----------|---------|
| `id`             | Long     | 文件 ID   |
| `fileName`       | String   | 文件名称    |
| `originalName`   | String   | 原始文件名   |
| `filePath`       | String   | 文件路径    |
| `fileUrl`        | String   | 文件地址    |
| `storageKey`     | String   | 存储节点标识  |
| `fileSize`       | Long     | 文件大小    |
| `fileType`       | String   | 文件类型    |
| `mimeType`       | String   | MIME 类型 |
| `fileExtension`  | String   | 扩展名     |
| `uploadUserId`   | Long     | 上传用户 ID |
| `isPublic`       | Integer  | 是否公开    |
| `category`       | String   | 业务分类    |
| `status`         | Integer  | 文件状态    |
| `referenceCount` | Integer  | 引用数     |
| `createdAt`      | DateTime | 创建时间    |

#### 查询文件详情

- 请求：`GET /api/sys/files/{id}`
- 响应字段：`FileDetailVO`
- 在 `FileAdminVO` 基础上额外返回：
    - `references`: `List<FileReferenceVO>`
    - `tasks`: `List<FileTaskAdminVO>`

- `FileReferenceVO`

| 字段              | 类型       | 说明      |
|-----------------|----------|---------|
| `id`            | Long     | 引用 ID   |
| `userId`        | Long     | 用户 ID   |
| `referenceType` | String   | 引用类型    |
| `referenceId`   | Long     | 引用对象 ID |
| `isPublic`      | Integer  | 是否公开    |
| `category`      | String   | 业务分类    |
| `remark`        | String   | 备注      |
| `createdAt`     | DateTime | 创建时间    |

- `FileTaskAdminVO`

| 字段               | 类型       | 说明      |
|------------------|----------|---------|
| `id`             | Long     | 任务 ID   |
| `uploadId`       | String   | 上传标识    |
| `fileId`         | Long     | 文件 ID   |
| `uploadUserId`   | Long     | 上传用户 ID |
| `originalName`   | String   | 原始文件名   |
| `fileSize`       | Long     | 文件大小    |
| `storageKey`     | String   | 存储节点    |
| `isQuickUpload`  | Integer  | 是否秒传    |
| `isChunked`      | Integer  | 是否分片    |
| `uploadedChunks` | Integer  | 已上传分片数  |
| `totalChunks`    | Integer  | 总分片数    |
| `taskStatus`     | Integer  | 任务状态    |
| `errorMessage`   | String   | 错误信息    |
| `createdAt`      | DateTime | 创建时间    |
| `completeTime`   | DateTime | 完成时间    |

### 5.2 后台上传任务管理

#### 接口速览

| 场景       | 方法  | 路径                            | 权限                   |
|----------|-----|-------------------------------|----------------------|
| 分页查询上传任务 | GET | `/api/sys/files/upload-tasks` | `content:file:query` |

#### 分页查询上传任务

- 请求：`GET /api/sys/files/upload-tasks`
- 查询参数：`FileTaskPageQuery`

| 参数              | 类型      | 说明           |
|-----------------|---------|--------------|
| `current`       | Long    | 页码，默认 `1`    |
| `size`          | Long    | 每页条数，默认 `10` |
| `uploadUserId`  | Long    | 上传用户 ID      |
| `taskStatus`    | Integer | 任务状态         |
| `isQuickUpload` | Integer | 是否秒传         |
| `isChunked`     | Integer | 是否分片         |

- 边界说明：
    - 传入非法 `taskStatus` 时会直接返回 `UPLOAD_TASK_STATUS_INVALID`。

- 响应字段：`FileTaskAdminVO`

### 5.3 更新文件状态

- 请求：`PUT /api/sys/files/{id}/status`
- 请求体：`FileStatusUpdateRequest`

```json
{
  "status": 3
}
```

- 状态说明：
    - `1`：正常
    - `2`：审核中
    - `3`：违规下架

- 边界说明：
    - 该接口不支持直接设置为 `0(已删除)`。
    - 真正删除文件请使用 `DELETE /api/sys/files/{id}`。
    - 已删除文件不支持通过该接口恢复状态。

### 5.4 删除文件

- 请求：`DELETE /api/sys/files/{id}`
- 前端说明：
    - 后台删除会移除文件记录及关联业务引用。
    - 同时会清理上传任务、分片和物理文件，具体以服务当前实现为准。

## 6. 取值速查

### 6.1 上传模式

| 值   | 说明   |
|-----|------|
| `1` | 秒传   |
| `2` | 分片上传 |
| `3` | 全量上传 |

### 6.2 任务状态

| 值   | 说明               |
|-----|------------------|
| `0` | 初始化              |
| `1` | 上传中              |
| `2` | 合并中              |
| `3` | 已完成              |
| `4` | 失败               |
| `5` | 已取消，过期任务也会收口到该状态 |

### 6.3 文件状态

| 值   | 说明   |
|-----|------|
| `0` | 已删除  |
| `1` | 正常   |
| `2` | 审核中  |
| `3` | 违规下架 |

### 6.4 引用类型

| 值                    | 说明   |
|----------------------|------|
| `avatar`             | 用户头像 |
| `article_attachment` | 文章附件 |
| `comment_image`      | 评论图片 |
| `temp`               | 临时文件 |

## 7. 常见联调问题

| 问题                      | 当前行为                                                             |
|-------------------------|------------------------------------------------------------------|
| 初始化时命中同 `MD5 + 文件大小` 文件 | 可直接走秒传                                                           |
| 是否一定分片上传                | 不一定，客户端可显式传分片信息，也可能由服务端按阈值判断                                     |
| 同一分片重复上传会怎样             | 会覆盖该分片的元数据与临时文件，不会新增重复分片记录                                       |
| 整文件或分片 MD5 不匹配会怎样       | 服务端会直接拒绝本次上传，不继续落存储                                              |
| 用户删除文件是否一定删掉底层物理文件      | 不一定，只有引用数归零才会尝试删除                                                |
| 后台文件权限前缀是什么             | `content:file:query`、`content:file:update`、`content:file:delete` |
| 分片完成后临时文件清理失败           | 上传结果仍保持成功，不回滚已完成任务                                               |
| 上传任务过期后再继续调用上传接口会怎样     | 服务端会先把任务收口为已取消，再返回 `UPLOAD_TASK_EXPIRED`                         |

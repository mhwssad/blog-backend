# File 模块接口文档

本文档覆盖 `module/file` 的用户上传接口与后台文件管理接口。

## 基础信息

- **Base URL**: `/api`
- **认证方式**:
  - 用户接口要求 `Authorization: Bearer <token>`
  - 后台接口要求登录且具备 `content:file:*` 权限
- **统一响应**: `Result<T>`
- **分页响应**: `Result<PageResult<T>>`

统一响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

分页响应中 `data` 固定包含：

```json
{
  "total": 1,
  "current": 1,
  "size": 10,
  "records": []
}
```

## 1. 用户文件接口

### 1.1 初始化上传任务

- **请求方式**: `POST /api/user/files/upload-tasks/init`
- **认证**: Bearer Token
- **请求体**: `FileUploadInitRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| originalName | String | 是 | 原始文件名 |
| fileSize | Long | 是 | 文件大小，单位字节 |
| fileMd5 | String | 否 | 文件 MD5，开启 MD5 校验或秒传时建议传入 |
| mimeType | String | 否 | MIME 类型 |
| referenceType | String | 否 | 引用类型，支持 `avatar`、`article_attachment`、`comment_image`、`temp` |
| referenceId | Long | 否 | 引用对象 ID，未传按 `0` 处理 |
| category | String | 否 | 文件业务分类，如 `avatar`、`attachment`、`comment`、`temp` |
| isPublic | Integer | 否 | 是否公开，`0` 私有，`1` 公开 |
| totalChunks | Integer | 否 | 总分片数，普通上传可为空 |
| chunkSize | Long | 否 | 分片大小，普通上传可为空 |
| remark | String | 否 | 备注 |

- **响应字段**: `FileUploadInitVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| taskId | Long | 上传任务 ID |
| uploadId | String | 上传标识，后续上传接口使用 |
| uploadMode | Integer | 上传模式，`1` 秒传，`2` 分片上传，`3` 全量上传 |
| quickUploadAvailable | Boolean | 是否具备秒传条件 |
| completed | Boolean | 是否已在初始化阶段直接完成 |
| totalChunks | Integer | 总分片数 |
| chunkSize | Long | 分片大小 |
| taskStatus | Integer | 任务状态，`0` 初始化，`1` 上传中，`2` 合并中，`3` 已完成，`4` 失败，`5` 已取消 |
| fileId | Long | 已命中的文件 ID |
| fileUrl | String | 文件访问地址 |
| businessId | Long | 业务引用 ID |

- **请求示例**:

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

### 1.2 秒传检测

- **请求方式**: `POST /api/user/files/upload-tasks/{uploadId}/quick-check`
- **认证**: Bearer Token
- **路径参数**: `uploadId`
- **响应字段**: `FileUploadResultVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| uploadId | String | 上传标识 |
| taskId | Long | 任务 ID |
| fileId | Long | 文件 ID |
| businessId | Long | 业务引用 ID |
| quickUpload | Boolean | 是否通过秒传完成 |
| taskStatus | Integer | 任务状态 |
| fileUrl | String | 文件访问地址 |
| referenceCount | Integer | 当前引用数 |

### 1.3 普通上传

- **请求方式**: `POST /api/user/files/upload-tasks/{uploadId}/file`
- **认证**: Bearer Token
- **Content-Type**: `multipart/form-data`
- **表单字段**:
  - `file`: 必填，上传文件
- **响应字段**: `FileUploadResultVO`

### 1.4 上传分片

- **请求方式**: `POST /api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}`
- **认证**: Bearer Token
- **Content-Type**: `multipart/form-data`
- **路径参数**:
  - `uploadId`
  - `chunkNumber`
- **表单字段**:
  - `file`: 必填，当前分片文件
  - `chunkMd5`: 可选，当前分片 MD5
- **响应字段**: `ChunkUploadVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| uploadId | String | 上传标识 |
| chunkNumber | Integer | 当前分片序号 |
| uploadedChunks | Integer | 已上传分片数 |
| totalChunks | Integer | 总分片数 |
| taskStatus | Integer | 当前任务状态 |

### 1.5 完成上传

- **请求方式**: `POST /api/user/files/upload-tasks/{uploadId}/complete`
- **认证**: Bearer Token
- **路径参数**: `uploadId`
- **说明**: 仅适用于分片上传任务；服务端会校验分片完整性并执行合并。
- **响应字段**: `FileUploadResultVO`

### 1.6 查询我的文件

- **请求方式**: `GET /api/user/files`
- **认证**: Bearer Token
- **查询参数**: `UserFilePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页条数，默认 `10` |
| keyword | String | 文件名关键字 |
| status | Integer | 文件状态 |
| category | String | 业务分类 |
| referenceType | String | 引用类型 |

- **响应字段**: `UserFileVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| businessId | Long | 业务引用 ID |
| fileId | Long | 文件 ID |
| fileName | String | 文件名称 |
| originalName | String | 原始文件名 |
| fileUrl | String | 文件地址 |
| fileSize | Long | 文件大小 |
| fileType | String | 文件类型，如 `image`、`video`、`document`、`other` |
| mimeType | String | MIME 类型 |
| category | String | 业务分类 |
| referenceType | String | 引用类型 |
| referenceId | Long | 引用对象 ID |
| isPublic | Integer | 是否公开 |
| status | Integer | 文件状态 |
| createdAt | DateTime | 引用创建时间 |

### 1.7 查询我的上传任务

- **请求方式**: `GET /api/user/files/upload-tasks`
- **认证**: Bearer Token
- **查询参数**: `UserFileTaskPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页条数，默认 `10` |
| taskStatus | Integer | 任务状态 |
| isQuickUpload | Integer | 是否秒传，`0/1` |
| isChunked | Integer | 是否分片，`0/1` |

- **响应字段**: `UserFileTaskVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 任务 ID |
| uploadId | String | 上传标识 |
| fileId | Long | 文件 ID |
| originalName | String | 原始文件名 |
| fileSize | Long | 文件大小 |
| isQuickUpload | Integer | 是否秒传 |
| isChunked | Integer | 是否分片 |
| chunkSize | Long | 分片大小 |
| totalChunks | Integer | 总分片数 |
| uploadedChunks | Integer | 已上传分片数 |
| taskStatus | Integer | 任务状态 |
| errorCode | String | 错误码 |
| errorMessage | String | 错误信息 |
| startTime | DateTime | 开始时间 |
| completeTime | DateTime | 完成时间 |
| createdAt | DateTime | 创建时间 |

### 1.8 删除我的文件引用

- **请求方式**: `DELETE /api/user/files/{businessId}`
- **认证**: Bearer Token
- **路径参数**: `businessId`
- **说明**:
  - 删除的是当前用户的业务引用记录，不是直接按文件 ID 删除。
  - 若同一文件已无任何引用，系统会尝试删除底层存储对象，并将文件状态改为 `0`。

## 2. 后台文件管理接口

### 2.1 分页查询文件

- **请求方式**: `GET /api/sys/files`
- **权限**: `content:file:query`
- **查询参数**: `FileAdminPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页条数，默认 `10` |
| keyword | String | 文件名/原始文件名关键字 |
| uploadUserId | Long | 上传用户 ID |
| status | Integer | 文件状态 |
| category | String | 业务分类 |
| referenceType | String | 引用类型 |
| isPublic | Integer | 是否公开 |

- **响应字段**: `FileAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 文件 ID |
| fileName | String | 文件名称 |
| originalName | String | 原始文件名 |
| filePath | String | 文件路径 |
| fileUrl | String | 文件地址 |
| storageKey | String | 存储节点标识 |
| fileSize | Long | 文件大小 |
| fileType | String | 文件类型 |
| mimeType | String | MIME 类型 |
| fileExtension | String | 文件扩展名 |
| uploadUserId | Long | 上传用户 ID |
| isPublic | Integer | 是否公开 |
| category | String | 业务分类 |
| status | Integer | 文件状态 |
| referenceCount | Integer | 引用数 |
| createdAt | DateTime | 创建时间 |

### 2.2 查询文件详情

- **请求方式**: `GET /api/sys/files/{id}`
- **权限**: `content:file:query`
- **路径参数**: `id`
- **响应字段**: `FileDetailVO`
  - 继承 `FileAdminVO` 全部字段
  - 额外包含：
    - `references`: `List<FileReferenceVO>`
    - `tasks`: `List<FileTaskAdminVO>`

`FileReferenceVO` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 引用 ID |
| userId | Long | 用户 ID |
| referenceType | String | 引用类型 |
| referenceId | Long | 引用对象 ID |
| isPublic | Integer | 是否公开 |
| category | String | 业务分类 |
| remark | String | 备注 |
| createdAt | DateTime | 创建时间 |

`FileTaskAdminVO` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 任务 ID |
| uploadId | String | 上传标识 |
| fileId | Long | 文件 ID |
| uploadUserId | Long | 上传用户 ID |
| originalName | String | 原始文件名 |
| fileSize | Long | 文件大小 |
| storageKey | String | 存储节点 |
| isQuickUpload | Integer | 是否秒传 |
| isChunked | Integer | 是否分片 |
| uploadedChunks | Integer | 已上传分片数 |
| totalChunks | Integer | 总分片数 |
| taskStatus | Integer | 任务状态 |
| errorMessage | String | 错误信息 |
| createdAt | DateTime | 创建时间 |
| completeTime | DateTime | 完成时间 |

### 2.3 分页查询上传任务

- **请求方式**: `GET /api/sys/files/upload-tasks`
- **权限**: `content:file:query`
- **查询参数**: `FileTaskPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页条数，默认 `10` |
| uploadUserId | Long | 上传用户 ID |
| taskStatus | Integer | 任务状态 |
| isQuickUpload | Integer | 是否秒传 |
| isChunked | Integer | 是否分片 |

- **响应字段**: `FileTaskAdminVO`

### 2.4 更新文件状态

- **请求方式**: `PUT /api/sys/files/{id}/status`
- **权限**: `content:file:update`
- **路径参数**: `id`
- **请求体**: `FileStatusUpdateRequest`

```json
{
  "status": 3
}
```

- **状态说明**:
  - `0`: 已删除
  - `1`: 正常
  - `2`: 审核中
  - `3`: 违规下架

### 2.5 删除文件

- **请求方式**: `DELETE /api/sys/files/{id}`
- **权限**: `content:file:delete`
- **路径参数**: `id`
- **说明**: 后台删除会移除文件记录及其关联业务引用，具体以当前服务实现为准。

## 3. 取值说明

### 3.1 上传模式

| 值 | 说明 |
| --- | --- |
| `1` | 秒传 |
| `2` | 分片上传 |
| `3` | 全量上传 |

### 3.2 任务状态

| 值 | 说明 |
| --- | --- |
| `0` | 初始化 |
| `1` | 上传中 |
| `2` | 合并中 |
| `3` | 已完成 |
| `4` | 失败 |
| `5` | 已取消 |

### 3.3 文件状态

| 值 | 说明 |
| --- | --- |
| `0` | 已删除 |
| `1` | 正常 |
| `2` | 审核中 |
| `3` | 违规下架 |

### 3.4 引用类型

| 值 | 说明 |
| --- | --- |
| `avatar` | 用户头像 |
| `article_attachment` | 文章附件 |
| `comment_image` | 评论图片 |
| `temp` | 临时文件 |

## 4. 典型规则

- 初始化上传任务时，如果命中相同 `MD5 + 文件大小` 的正常文件，可直接走秒传。
- 是否走分片上传由客户端显式传入分片信息，或由服务端根据阈值自动判断。
- 用户删除文件时删除的是“文件引用”；仅当引用数归零时才会尝试清理底层文件。
- 后台文件接口使用内容域权限前缀：`content:file:query`、`content:file:update`、`content:file:delete`。

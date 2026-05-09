# 文件与上传 API

> 本文档面向前端联调，覆盖上传流程、文件查询、后台管理全场景。

---

## 快速接口对照表

### 用户上传

| 用途 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 初始化上传任务 | POST | `/api/user/files/upload-tasks/init` | 上传流程入口，支持秒传检测 |
| 秒传检测 | POST | `/api/user/files/upload-tasks/{uploadId}/quick-check` | 显式触发秒传判断 |
| 普通上传 | POST | `/api/user/files/upload-tasks/{uploadId}/file` | 全量文件上传 |
| 上传分片 | POST | `/api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}` | 分片上传 |
| 完成上传 | POST | `/api/user/files/upload-tasks/{uploadId}/complete` | 触发分片合并 |

### 用户文件

| 用途 | 方法 | 路径 |
|---|---|---|
| 查询我的文件 | GET | `/api/user/files` |
| 查询我的上传任务 | GET | `/api/user/files/upload-tasks` |
| 删除我的文件引用 | DELETE | `/api/user/files/{businessId}` |

### 后台管理

| 用途 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 分页查询文件 | GET | `/api/sys/files` | `content:file:query` |
| 查询文件详情 | GET | `/api/sys/files/{id}` | `content:file:query` |
| 分页查询上传任务 | GET | `/api/sys/files/upload-tasks` | `content:file:query` |
| 更新文件状态 | PUT | `/api/sys/files/{id}/status` | `content:file:update` |
| 删除文件 | DELETE | `/api/sys/files/{id}` | `content:file:delete` |

---

## 1. 统一说明

### 1.1 路由分组

| 路由前缀 | 面向场景 | 鉴权要求 |
|---|---|---|
| `/api/user/files/**` | 登录用户上传、查询我的文件、查询上传任务 | 需要登录 |
| `/api/sys/files/**` | 后台文件库、后台上传任务管理 | 需要登录 + 对应权限 |

### 1.2 统一响应格式

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

**分页响应 `data` 格式：**

```json
{
  "total": 100,
  "current": 1,
  "size": 10,
  "records": []
}
```

### 1.3 统一错误码

| code | 说明 | 处理建议 |
|---|---|---|
| `ILLEGAL_ARGUMENT` | 参数校验失败 | 检查请求字段 |
| `UPLOAD_TASK_EXPIRED` | 上传任务已过期 | 重新初始化上传任务 |
| `UPLOAD_TASK_STATUS_INVALID` | 任务状态不允许该操作 | 刷新任务状态后再操作 |
| `FILE_MD5_MISMATCH` | 整文件 MD5 不匹配 | 重新选择文件上传 |
| `CHUNK_MD5_MISMATCH` | 分片 MD5 不匹配 | 重新上传该分片 |
| `CHUNK_INCOMPLETE` | 分片未全部上传完成 | 确保所有分片上传成功 |
| `CHUNK_MERGE_FAILED` | 分片合并失败 | 检查文件完整性或重试 |

### 1.4 上传场景选型

| 场景 | 推荐流程 |
|---|---|
| 小文件直传（头像、评论图片等） | 初始化任务 → 普通上传 |
| 已计算 MD5，想先尝试秒传 | 初始化任务 → 秒传检测 |
| 大文件分片上传 | 初始化任务 → 多次上传分片 → 完成上传 |
| 展示上传历史 / 失败原因 | 查询上传任务列表 |
| 展示"我的资源库" | 查询我的文件列表 |

---

## 2. 用户上传接入流程

### 2.1 流程概览

所有用户上传接口都需要携带登录 Token：

```http
Authorization: Bearer <accessToken>
```

**上传流程有两种分支：**

```
分支一（普通/秒传）：
初始化任务
    ↓
返回 completed=true？──是→ 流程结束（秒传命中）
    ↓否
返回 uploadMode=1？──是→ 秒传检测 → 成功？──是→ 流程结束
    ↓否                              ↓否
普通上传 → 流程结束               普通上传 → 流程结束

分支二（分片上传）：
初始化任务（带 totalChunks）
    ↓
循环上传分片（chunks/{chunkNumber}）
    ↓
完成上传（complete）
    ↓
流程结束
```

### 2.2 初始化上传任务

**接口信息**
- 路径：POST `/api/user/files/upload-tasks/init`
- 鉴权：是
- 说明：任何上传流程的第一步，用于创建上传任务并返回上传模式判断

**请求示例**

```javascript
// axios
axios.post('/api/user/files/upload-tasks/init', {
  originalName: 'photo.jpg',
  fileSize: 1024000,
  fileMd5: '5d41402abc4b2a76b9719d911017c592',
  mimeType: 'image/jpeg',
  referenceType: 'avatar',
  referenceId: 1,
  category: 'avatar',
  isPublic: 1,
  remark: '用户头像'
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `originalName` | String | 是 | 原始文件名 |
| `fileSize` | Long | 是 | 文件大小（字节） |
| `fileMd5` | String | 否 | 文件 MD5，建议传入以支持秒传 |
| `mimeType` | String | 否 | MIME 类型 |
| `referenceType` | String | 否 | `avatar`、`chat_message`、`article_attachment`、`temp` |
| `referenceId` | Long | 否 | 引用对象 ID |
| `category` | String | 否 | 业务分类：`avatar`、`attachment`、`comment`、`chat_attachment`、`temp` |
| `isPublic` | Integer | 否 | `0` 私有，`1` 公开 |
| `totalChunks` | Integer | 否 | 分片上传时传总分片数，必须大于 1 |
| `chunkSize` | Long | 否 | 分片大小，必须大于 0 |
| `remark` | String | 否 | 备注 |

**响应示例（秒传命中）**

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

**响应示例（需要普通上传）**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 101,
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "uploadMode": 3,
    "quickUploadAvailable": true,
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

**响应示例（需要分片上传）**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 103,
    "uploadId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "uploadMode": 2,
    "quickUploadAvailable": false,
    "completed": false,
    "totalChunks": 4,
    "chunkSize": 5242880,
    "taskStatus": 0,
    "fileId": null,
    "fileUrl": null,
    "businessId": null
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `taskId` | Long | 上传任务 ID |
| `uploadId` | String | 上传标识，后续所有上传接口都依赖它 |
| `uploadMode` | Integer | `1` 秒传，`2` 分片上传，`3` 全量上传 |
| `quickUploadAvailable` | Boolean | 是否具备秒传条件（传了 MD5 则为 true） |
| `completed` | Boolean | 是否已在初始化阶段直接完成（秒传命中） |
| `totalChunks` | Integer | 总分片数（分片上传时有效） |
| `chunkSize` | Long | 分片大小（分片上传时有效） |
| `taskStatus` | Integer | 任务状态：`0` 初始化，`1` 上传中，`2` 合并中，`3` 已完成，`4` 失败，`5` 已取消 |
| `fileId` | Long | 已命中文件的 ID（秒传成功时有值） |
| `fileUrl` | String | 文件访问地址（秒传成功时有值） |
| `businessId` | Long | 业务引用 ID（秒传成功时有值） |

**错误码说明**

| code | 说明 | 前端处理 |
|---|---|---|
| `ILLEGAL_ARGUMENT` | 参数校验失败 | 检查 `referenceType`、`category`、`isPublic` 是否合法 |

### 2.3 秒传检测

**接口信息**
- 路径：POST `/api/user/files/upload-tasks/{uploadId}/quick-check`
- 鉴权：是
- 说明：在初始化后显式执行秒传判断，适用于客户端已计算 MD5 想先探测是否能复用

**请求示例**

```javascript
// axios
axios.post('/api/user/files/upload-tasks/a1b2c3d4-e5f6-7890-abcd-ef1234567890/quick-check', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例（秒传成功）**

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

**响应示例（秒传未命中）**

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

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `uploadId` | String | 上传标识 |
| `taskId` | Long | 任务 ID |
| `fileId` | Long | 文件 ID（秒传成功时有值） |
| `businessId` | Long | 业务引用 ID（秒传成功时有值） |
| `quickUpload` | Boolean | 是否通过秒传完成 |
| `taskStatus` | Integer | 任务状态 |
| `fileUrl` | String | 文件访问地址（秒传成功时有值） |
| `referenceCount` | Integer | 当前引用数（秒传成功时有值） |

**错误码说明**

| code | 说明 | 前端处理 |
|---|---|---|
| `UPLOAD_TASK_EXPIRED` | 任务已过期 | 重新初始化上传任务 |
| `UPLOAD_TASK_STATUS_INVALID` | 任务状态不允许该操作 | 刷新任务状态后重试 |

### 2.4 普通上传

**接口信息**
- 路径：POST `/api/user/files/upload-tasks/{uploadId}/file`
- 鉴权：是
- 说明：全量上传一个完整文件，适用于小文件或初始化时未传分片参数的场景
- Content-Type：`multipart/form-data`

**请求示例**

```javascript
// axios
const formData = new FormData();
formData.append('file', fileObject); // fileObject 是 File 对象

axios.post('/api/user/files/upload-tasks/a1b2c3d4-e5f6-7890-abcd-ef1234567890/file', formData, {
  headers: { Authorization: 'Bearer xxx', 'Content-Type': 'multipart/form-data' }
})
```

**请求表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | File | 是 | 待上传的文件对象 |

**响应示例（上传成功）**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "taskId": 101,
    "fileId": 502,
    "businessId": 302,
    "quickUpload": false,
    "taskStatus": 3,
    "fileUrl": "https://example.com/files/report_2026.pdf",
    "referenceCount": 1
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `uploadId` | String | 上传标识 |
| `taskId` | Long | 任务 ID |
| `fileId` | Long | 文件 ID |
| `businessId` | Long | 业务引用 ID |
| `quickUpload` | Boolean | 是否通过秒传完成 |
| `taskStatus` | Integer | 任务状态，`3` 表示已完成 |
| `fileUrl` | String | 文件访问地址 |
| `referenceCount` | Integer | 当前引用数 |

**错误码说明**

| code | 说明 | 前端处理 |
|---|---|---|
| `FILE_MD5_MISMATCH` | 整文件 MD5 不匹配 | 重新选择正确的文件上传 |
| `UPLOAD_TASK_EXPIRED` | 任务已过期 | 重新初始化上传任务 |
| `UPLOAD_TASK_STATUS_INVALID` | 任务状态不允许该操作（已完成或已取消） | 刷新任务状态后重试 |

### 2.5 上传分片

**接口信息**
- 路径：POST `/api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}`
- 鉴权：是
- 说明：上传单个分片，分片序号从 1 开始
- Content-Type：`multipart/form-data`

**请求示例**

```javascript
// axios
const formData = new FormData();
formData.append('file', chunkBlob); // 当前分片的 Blob/File 对象
formData.append('chunkMd5', 'abc123def456'); // 可选，当前分片 MD5

axios.post('/api/user/files/upload-tasks/c3d4e5f6-a7b8-9012-cdef-123456789012/chunks/1', formData, {
  headers: { Authorization: 'Bearer xxx', 'Content-Type': 'multipart/form-data' }
})
```

**请求表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | File/Blob | 是 | 当前分片的文件对象 |
| `chunkMd5` | String | 否 | 当前分片的 MD5，用于服务端校验 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "uploadId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "chunkNumber": 1,
    "uploadedChunks": 1,
    "totalChunks": 4,
    "taskStatus": 1
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `uploadId` | String | 上传标识 |
| `chunkNumber` | Integer | 当前分片序号（从 1 开始） |
| `uploadedChunks` | Integer | 已上传分片数 |
| `totalChunks` | Integer | 总分片数 |
| `taskStatus` | Integer | 当前任务状态 |

**错误码说明**

| code | 说明 | 前端处理 |
|---|---|---|
| `CHUNK_MD5_MISMATCH` | 分片 MD5 不匹配 | 重新上传该分片 |
| `UPLOAD_TASK_EXPIRED` | 任务已过期 | 重新初始化上传任务 |
| `UPLOAD_TASK_STATUS_INVALID` | 任务状态不允许该操作 | 刷新任务状态后重试 |

### 2.6 完成上传

**接口信息**
- 路径：POST `/api/user/files/upload-tasks/{uploadId}/complete`
- 鉴权：是
- 说明：分片全部上传完成后触发合并，仅适用于分片上传任务

**请求示例**

```javascript
// axios
axios.post('/api/user/files/upload-tasks/c3d4e5f6-a7b8-9012-cdef-123456789012/complete', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

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

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `uploadId` | String | 上传标识 |
| `taskId` | Long | 任务 ID |
| `fileId` | Long | 文件 ID |
| `businessId` | Long | 业务引用 ID |
| `quickUpload` | Boolean | 是否通过秒传完成 |
| `taskStatus` | Integer | 任务状态，`3` 表示已完成 |
| `fileUrl` | String | 文件访问地址 |
| `referenceCount` | Integer | 当前引用数 |

**错误码说明**

| code | 说明 | 前端处理 |
|---|---|---|
| `CHUNK_INCOMPLETE` | 分片未全部上传完成 | 确保所有分片都上传成功后再调用 |
| `FILE_MD5_REQUIRED` | 初始化时未传 MD5 且合并后无法计算 | 重新初始化并传入 MD5 |
| `CHUNK_MERGE_FAILED` | 分片合并失败 | 检查文件完整性或重新上传 |
| `UPLOAD_TASK_EXPIRED` | 任务已过期 | 重新初始化上传任务 |
| `UPLOAD_TASK_STATUS_INVALID` | 任务状态不允许该操作 | 刷新任务状态后重试 |

---

## 3. 用户文件接口

### 3.1 查询我的文件

**接口信息**
- 路径：GET `/api/user/files`
- 鉴权：是
- 说明：查询当前用户的文件列表，用于"我的资源库"或上传结果选择器

**请求示例**

```javascript
// axios
axios.get('/api/user/files', {
  params: {
    current: 1,
    size: 10,
    keyword: 'avatar',
    status: 1,
    category: 'avatar',
    referenceType: 'avatar'
  },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `keyword` | String | 否 | 文件名关键字 |
| `status` | Integer | 否 | 文件状态：`0` 已删除，`1` 正常，`2` 待物理删除，`3` 审核中，`4` 违规下架 |
| `category` | String | 否 | 业务分类：`avatar`、`attachment`、`comment`、`chat_attachment`、`temp` |
| `referenceType` | String | 否 | 引用类型：`avatar`、`chat_message`、`article_attachment`、`temp` |

**响应示例**

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

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `businessId` | Long | 业务引用 ID |
| `fileId` | Long | 文件 ID |
| `fileName` | String | 文件名（存储名） |
| `originalName` | String | 原始文件名 |
| `fileUrl` | String | 文件访问地址 |
| `fileSize` | Long | 文件大小（字节） |
| `fileType` | String | 文件类型：`image`、`video`、`document`、`other` |
| `mimeType` | String | MIME 类型 |
| `category` | String | 业务分类 |
| `referenceType` | String | 引用类型 |
| `referenceId` | Long | 引用对象 ID |
| `isPublic` | Integer | 是否公开：`0` 私有，`1` 公开 |
| `status` | Integer | 文件状态 |
| `createdAt` | DateTime | 引用创建时间 |

### 3.2 查询我的上传任务

**接口信息**
- 路径：GET `/api/user/files/upload-tasks`
- 鉴权：是
- 说明：查询当前用户的上传任务列表，用于上传记录展示、失败重试提示

**请求示例**

```javascript
// axios
axios.get('/api/user/files/upload-tasks', {
  params: {
    current: 1,
    size: 10,
    taskStatus: 4,
    isQuickUpload: 0,
    isChunked: 1
  },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `taskStatus` | Integer | 否 | 任务状态：`0` 初始化，`1` 上传中，`2` 合并中，`3` 已完成，`4` 失败，`5` 已取消 |
| `isQuickUpload` | Integer | 否 | 是否秒传：`0` 否，`1` 是 |
| `isChunked` | Integer | 否 | 是否分片：`0` 否，`1` 是 |

**响应示例**

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
      },
      {
        "id": 104,
        "uploadId": "d4e5f6a7-b8c9-0123-def1-234567890123",
        "fileId": null,
        "originalName": "large_video.mp4",
        "fileSize": 524288000,
        "isQuickUpload": 0,
        "isChunked": 1,
        "chunkSize": 10485760,
        "totalChunks": 50,
        "uploadedChunks": 30,
        "taskStatus": 4,
        "errorCode": "CHUNK_MERGE_FAILED",
        "errorMessage": "分片合并失败，请重试",
        "startTime": "2026-03-29 11:00:00",
        "completeTime": null,
        "createdAt": "2026-03-29 11:00:05"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 任务 ID |
| `uploadId` | String | 上传标识 |
| `fileId` | Long | 文件 ID（成功时有值） |
| `originalName` | String | 原始文件名 |
| `fileSize` | Long | 文件大小（字节） |
| `isQuickUpload` | Integer | 是否秒传：`0` 否，`1` 是 |
| `isChunked` | Integer | 是否分片：`0` 否，`1` 是 |
| `chunkSize` | Long | 分片大小 |
| `totalChunks` | Integer | 总分片数 |
| `uploadedChunks` | Integer | 已上传分片数 |
| `taskStatus` | Integer | 任务状态 |
| `errorCode` | String | 错误码（失败时有值） |
| `errorMessage` | String | 错误信息（失败时有值） |
| `startTime` | DateTime | 开始时间 |
| `completeTime` | DateTime | 完成时间（成功时才有值） |
| `createdAt` | DateTime | 创建时间 |

### 3.3 删除我的文件引用

**接口信息**
- 路径：DELETE `/api/user/files/{businessId}`
- 鉴权：是
- 说明：删除当前用户自己的业务引用记录，而非物理文件

**请求示例**

```javascript
// axios
axios.delete('/api/user/files/301', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**

| 参数 | 类型 | 说明 |
|---|---|---|
| `businessId` | Long | 业务引用 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**前端说明**
- 删除的是"文件引用"，不是直接按 `fileId` 删除物理文件
- 若同一底层文件没有任何引用，系统会尝试清理存储对象，并把文件状态改为 `0`

---

## 4. 后台文件管理接口

### 4.1 权限说明

所有后台接口都需要：
1. 登录状态
2. 对应权限

| 权限标识 | 说明 |
|---|---|
| `content:file:query` | 文件查询权限 |
| `content:file:update` | 文件更新权限 |
| `content:file:delete` | 文件删除权限 |

### 4.2 分页查询文件

**接口信息**
- 路径：GET `/api/sys/files`
- 鉴权：是（需要 `content:file:query` 权限）
- 说明：后台文件库，用于管理所有上传的文件

**请求示例**

```javascript
// axios
axios.get('/api/sys/files', {
  params: {
    current: 1,
    size: 10,
    keyword: 'avatar',
    uploadUserId: 1,
    status: 1,
    category: 'avatar',
    referenceType: 'avatar',
    isPublic: 1
  },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `keyword` | String | 否 | 文件名 / 原始文件名关键字 |
| `uploadUserId` | Long | 否 | 上传用户 ID |
| `status` | Integer | 否 | 文件状态：`0` 已删除，`1` 正常，`2` 待物理删除，`3` 审核中，`4` 违规下架 |
| `category` | String | 否 | 业务分类 |
| `referenceType` | String | 否 | 引用类型 |
| `isPublic` | Integer | 否 | 是否公开：`0` 私有，`1` 公开 |

**响应示例**

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

**响应字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 文件 ID |
| `fileName` | String | 文件名（存储名） |
| `originalName` | String | 原始文件名 |
| `filePath` | String | 文件路径（服务端存储路径） |
| `fileUrl` | String | 文件访问地址 |
| `storageKey` | String | 存储节点标识 |
| `fileSize` | Long | 文件大小（字节） |
| `fileType` | String | 文件类型 |
| `mimeType` | String | MIME 类型 |
| `fileExtension` | String | 扩展名（带点，如 `.png`） |
| `uploadUserId` | Long | 上传用户 ID |
| `isPublic` | Integer | 是否公开 |
| `category` | String | 业务分类 |
| `status` | Integer | 文件状态 |
| `referenceCount` | Integer | 引用数 |
| `createdAt` | DateTime | 创建时间 |

### 4.3 查询文件详情

**接口信息**
- 路径：GET `/api/sys/files/{id}`
- 鉴权：是（需要 `content:file:query` 权限）
- 说明：查询单个文件的详细信息，包含引用列表和关联的上传任务

**请求示例**

```javascript
// axios
axios.get('/api/sys/files/501', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**

| 参数 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 文件 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
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
    "createdAt": "2026-03-28 15:30:00",
    "references": [
      {
        "id": 301,
        "userId": 1,
        "referenceType": "avatar",
        "referenceId": 1,
        "isPublic": 1,
        "category": "avatar",
        "remark": "用户头像",
        "createdAt": "2026-03-28 15:30:00"
      }
    ],
    "tasks": [
      {
        "id": 102,
        "uploadId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "fileId": 501,
        "uploadUserId": 1,
        "originalName": "avatar.png",
        "fileSize": 245678,
        "storageKey": "local",
        "isQuickUpload": 1,
        "isChunked": 0,
        "uploadedChunks": 0,
        "totalChunks": 0,
        "taskStatus": 3,
        "errorMessage": null,
        "createdAt": "2026-03-28 15:29:55",
        "completeTime": "2026-03-28 15:30:00"
      }
    ]
  }
}
```

**响应字段说明**

在 `FileAdminVO` 基础上额外返回：

| 字段 | 类型 | 说明 |
|---|---|---|
| `references` | List\<FileReferenceVO\> | 该文件的引用列表 |
| `tasks` | List\<FileTaskAdminVO\> | 该文件关联的上传任务列表 |

**FileReferenceVO 字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 引用 ID |
| `userId` | Long | 用户 ID |
| `referenceType` | String | 引用类型 |
| `referenceId` | Long | 引用对象 ID |
| `isPublic` | Integer | 是否公开 |
| `category` | String | 业务分类 |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |

**FileTaskAdminVO 字段说明**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 任务 ID |
| `uploadId` | String | 上传标识 |
| `fileId` | Long | 文件 ID |
| `uploadUserId` | Long | 上传用户 ID |
| `originalName` | String | 原始文件名 |
| `fileSize` | Long | 文件大小 |
| `storageKey` | String | 存储节点 |
| `isQuickUpload` | Integer | 是否秒传 |
| `isChunked` | Integer | 是否分片 |
| `uploadedChunks` | Integer | 已上传分片数 |
| `totalChunks` | Integer | 总分片数 |
| `taskStatus` | Integer | 任务状态 |
| `errorMessage` | String | 错误信息 |
| `createdAt` | DateTime | 创建时间 |
| `completeTime` | DateTime | 完成时间 |

### 4.4 分页查询上传任务

**接口信息**
- 路径：GET `/api/sys/files/upload-tasks`
- 鉴权：是（需要 `content:file:query` 权限）
- 说明：后台查看所有用户的上传任务，用于排查上传问题

**请求示例**

```javascript
// axios
axios.get('/api/sys/files/upload-tasks', {
  params: {
    current: 1,
    size: 10,
    uploadUserId: 1,
    taskStatus: 3,
    isQuickUpload: 1,
    isChunked: 0
  },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `uploadUserId` | Long | 否 | 上传用户 ID |
| `taskStatus` | Integer | 否 | 任务状态 |
| `isQuickUpload` | Integer | 否 | 是否秒传 |
| `isChunked` | Integer | 否 | 是否分片 |

**响应字段**：同 `FileTaskAdminVO`

### 4.5 更新文件状态

**接口信息**
- 路径：PUT `/api/sys/files/{id}/status`
- 鉴权：是（需要 `content:file:update` 权限）
- 说明：更新文件状态，用于审核、违规下架等场景

**请求示例**

```javascript
// axios
axios.put('/api/sys/files/501/status', {
  status: 4
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**

| 参数 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 文件 ID |

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | Integer | 是 | 文件状态：`1` 正常，`3` 审核中，`4` 违规下架 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**前端说明**
- 该接口不支持设置为 `0`（已删除），删除文件请使用 DELETE 接口
- 已删除文件（`status=0`）不支持通过该接口恢复状态

### 4.6 删除文件

**接口信息**
- 路径：DELETE `/api/sys/files/{id}`
- 鉴权：是（需要 `content:file:delete` 权限）
- 说明：删除文件记录及关联业务引用，清理上传任务、分片和物理文件

**请求示例**

```javascript
// axios
axios.delete('/api/sys/files/501', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**

| 参数 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 文件 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

## 5. 取值速查

### 5.1 上传模式（uploadMode）

| 值 | 说明 |
|---|---|
| `1` | 秒传 |
| `2` | 分片上传 |
| `3` | 全量上传 |

### 5.2 任务状态（taskStatus）

| 值 | 说明 |
|---|---|
| `0` | 初始化 |
| `1` | 上传中 |
| `2` | 合并中 |
| `3` | 已完成 |
| `4` | 失败 |
| `5` | 已取消（过期任务也会收口到该状态） |

### 5.3 文件状态（status）

| 值 | 说明 |
|---|---|
| `0` | 已删除 |
| `1` | 正常 |
| `2` | 待物理删除 |
| `3` | 审核中 |
| `4` | 违规下架 |

### 5.4 业务分类（category）

| 值 | 说明 |
|---|---|
| `avatar` | 头像 |
| `attachment` | 附件 |
| `comment` | 评论图片 |
| `chat_attachment` | 聊天附件 |
| `temp` | 临时文件 |

### 5.5 引用类型（referenceType）

| 值 | 说明 |
|---|---|
| `avatar` | 用户头像 |
| `chat_message` | 聊天消息文件 |
| `article_attachment` | 文章附件 |
| `temp` | 临时文件 |

---

## 6. 常见联调问题

| 问题 | 当前行为 |
|---|---|
| 初始化时命中同 MD5 + 文件大小文件 | 可直接走秒传，`completed=true` |
| 是否一定分片上传 | 不一定，客户端可显式传分片参数，也可能由服务端按阈值判断 |
| 同一分片重复上传会怎样 | 覆盖该分片的元数据与临时文件，不会新增重复分片记录 |
| 整文件或分片 MD5 不匹配会怎样 | 服务端直接拒绝本次上传，不继续落存储 |
| 用户删除文件是否一定删底层物理文件 | 不一定，只有引用数归零才会尝试删除 |
| 初始化时未传 MD5 会怎样 | 服务端会在上传过程中自动计算（普通上传流式计算，分片上传合并后计算） |
| 分片完成后临时文件清理失败 | 上传结果仍保持成功，不回滚已完成任务 |
| 上传任务过期后再调用上传接口会怎样 | 服务端先把任务收口为已取消，再返回 `UPLOAD_TASK_EXPIRED` |
| 秒传检测时任务已过期 | 服务端先将任务收口为 `5(已取消)`，再返回 `UPLOAD_TASK_EXPIRED` |
| 后台文件权限前缀是什么 | `content:file:query`、`content:file:update`、`content:file:delete` |

---

## 7. 公开文件访问

### 下载文件

**接口信息**

- 路径: `GET /api/public/files/{fileId}`
- 鉴权: 无（公开接口）
- 说明: 直接下载文件，返回文件流（非 `Result<>` 包装），浏览器会触发下载或内联显示

**路径参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `fileId` | Long | 是 | 文件ID |

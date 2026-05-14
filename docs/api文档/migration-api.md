# 外部博客迁移接口文档

> 本文档面向前端联调，覆盖外部博客 JSON 文件迁移的任务创建、预检、执行、查询与失败导出。

## 快速接口对照表

| 用途 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 创建任务 | POST | `/api/sys/migrations/blog/tasks` | `content:migration:create` |
| 执行预检 | POST | `/api/sys/migrations/blog/tasks/{id}/precheck` | `content:migration:execute` |
| 执行导入 | POST | `/api/sys/migrations/blog/tasks/{id}/execute` | `content:migration:execute` |
| 分页查询任务 | GET | `/api/sys/migrations/blog/tasks` | `content:migration:query` |
| 查询任务详情 | GET | `/api/sys/migrations/blog/tasks/{id}` | `content:migration:query` |
| 分页查询记录 | GET | `/api/sys/migrations/blog/tasks/{id}/records` | `content:migration:query` |
| 导出失败记录 | GET | `/api/sys/migrations/blog/tasks/{id}/failures/export` | `content:migration:export` |

---

## 1. 能力范围

- 后台上传 JSON 迁移文件并创建任务
- 任务指定站内作者，所有导入文章归属该作者
- 分类和标签必须预先存在，不自动创建
- 外部附件下载入库后替换正文和封面 URL，再复用后台文章创建链路导入文章
- 支持预检、执行、任务/记录查询和失败记录 Excel 导出

## 2. 鉴权要求

接口统一前缀：`/api/sys/migrations/blog`，均要求后台登录。

| 权限标识 | 说明 |
|---|---|
| `content:migration:query` | 查询迁移任务、详情和记录 |
| `content:migration:create` | 创建迁移任务 |
| `content:migration:execute` | 执行预检和导入 |
| `content:migration:export` | 导出失败记录 |

## 3. JSON v1 格式

上传的迁移文件必须为以下格式：

```json
{
  "sourcePlatform": "wordpress",
  "posts": [
    {
      "externalPostId": "post-1",
      "title": "标题",
      "summary": "摘要",
      "content": "正文，支持 Markdown/HTML",
      "coverImageUrl": "https://example.com/a.jpg",
      "categoryCodes": ["tech"],
      "tagNames": ["Java"],
      "isOriginal": 1,
      "sourceUrl": null,
      "status": 0,
      "publishTime": "2026-05-05 10:00:00",
      "attachments": [
        {
          "url": "https://example.com/a.jpg",
          "originalName": "a.jpg"
        }
      ]
    }
  ]
}
```

**关键约束**：
- `sourcePlatform` 必填，会标准化为小写
- 幂等键为 `sourcePlatform + ":" + externalPostId`
- 同一任务内重复 `externalPostId` 预检失败
- 全局已成功导入的幂等键，执行时跳过并记录 `SKIPPED`
- 附件 URL 仅支持 `http` / `https`
- 附件下载失败时，该文章导入失败，不创建部分文章

## 4. 接口详情

### 4.1 创建任务

```
POST /api/sys/migrations/blog/tasks
Content-Type: multipart/form-data
```

**请求参数**（表单字段）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `authorId` | Long | 是 | 导入文章归属作者 ID |
| `remark` | String | 否 | 备注，最多 256 字符 |
| `file` | File | 是 | JSON 迁移文件 |

**请求示例**：

```javascript
// axios
const formData = new FormData();
formData.append('authorId', 1);
formData.append('remark', '从 WordPress 迁移');
formData.append('file', fileInput.files[0]);

axios.post('/api/sys/migrations/blog/tasks', formData, {
  headers: {
    Authorization: 'Bearer xxx',
    'Content-Type': 'multipart/form-data'
  }
})
```

**响应** `BlogMigrationTaskVO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 任务 ID |
| `authorId` | Long | 作者 ID |
| `sourcePlatform` | String | 来源平台 |
| `status` | Integer | 任务状态 |
| `totalCount` | Integer | 总文章数 |
| `successCount` | Integer | 成功数 |
| `failedCount` | Integer | 失败数 |
| `skippedCount` | Integer | 跳过数 |
| `createdAt` | DateTime | 创建时间 |

---

### 4.2 执行预检

```
POST /api/sys/migrations/blog/tasks/{id}/precheck
```

**响应** `BlogMigrationPrecheckResultVO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `taskId` | Long | 任务 ID |
| `totalCount` | Integer | 总文章数 |
| `passed` | Boolean | 是否通过 |
| `errors` | Array | 失败明细，元素为 `BlogMigrationRecordVO` |

**响应示例**（通过）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 1,
    "totalCount": 10,
    "passed": true,
    "errors": []
  }
}
```

**响应示例**（未通过）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "taskId": 1,
    "totalCount": 10,
    "passed": false,
    "errors": [
      {
        "externalPostId": "post-3",
        "title": "第三篇文章",
        "errorMessage": "分类 tech 不存在"
      }
    ]
  }
}
```

> 预检失败不会抛业务异常，响应中 `passed=false` 并返回错误明细。

---

### 4.3 执行导入

```
POST /api/sys/migrations/blog/tasks/{id}/execute
```

**前置条件**：任务状态必须为 `PRECHECKED`（1）

**响应** `BlogMigrationTaskVO`：

**响应示例**：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "authorId": 1,
    "sourcePlatform": "wordpress",
    "status": 3,
    "totalCount": 10,
    "successCount": 8,
    "failedCount": 1,
    "skippedCount": 1,
    "createdAt": "2026-05-05T10:00:00"
  }
}
```

**错误码**：

| code | 说明 |
|---|---|
| 75002 | 任务状态不允许当前操作（如未预检就执行） |

---

### 4.4 分页查询任务

```
GET /api/sys/migrations/blog/tasks
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，最大 `100` |
| `status` | Integer | 否 | 任务状态 |
| `sourcePlatform` | String | 否 | 来源平台 |
| `authorId` | Long | 否 | 作者 ID |

**请求示例**：

```javascript
// axios
axios.get('/api/sys/migrations/blog/tasks', {
  params: { current: 1, size: 10, status: 3 },
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 5,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "authorId": 1,
        "sourcePlatform": "wordpress",
        "status": 3,
        "totalCount": 10,
        "successCount": 8,
        "failedCount": 1,
        "skippedCount": 1,
        "createdAt": "2026-05-05T10:00:00"
      }
    ]
  }
}
```

---

### 4.5 查询任务详情

```
GET /api/sys/migrations/blog/tasks/{id}
```

**响应** `BlogMigrationTaskVO`（同创建任务响应）

---

### 4.6 分页查询记录

```
GET /api/sys/migrations/blog/tasks/{id}/records
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，最大 `100` |
| `status` | Integer | 否 | 记录状态 |

**请求示例**：

```javascript
// axios
axios.get('/api/sys/migrations/blog/tasks/1/records', {
  params: { current: 1, size: 20, status: 2 },
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 1,
        "externalPostId": "post-3",
        "title": "第三篇文章",
        "status": 2,
        "errorMessage": "分类 tech 不存在"
      }
    ]
  }
}
```

---

### 4.7 导出失败记录

```
GET /api/sys/migrations/blog/tasks/{id}/failures/export
```

**响应**：Excel 文件流

| 响应头 | 说明 |
|---|---|
| `Content-Disposition` | `attachment; filename=blog-migration-failures-{id}.xlsx` |

**请求示例**：

```javascript
// axios
axios.get('/api/sys/migrations/blog/tasks/1/failures/export', {
  responseType: 'blob',
  headers: { Authorization: 'Bearer xxx' }
})
```

## 5. 枚举与错误码

**任务状态**：

| 值 | 说明 |
|---|---|
| `0` | CREATED，已创建 |
| `1` | PRECHECKED，预检通过 |
| `2` | RUNNING，执行中 |
| `3` | COMPLETED，已完成 |
| `4` | FAILED，失败 |
| `5` | CANCELLED，已取消 |

**记录状态**：

| 值 | 说明 |
|---|---|
| `0` | PENDING，待处理 |
| `1` | SUCCESS，成功 |
| `2` | FAILED，失败 |
| `3` | SKIPPED，已跳过 |

**常见错误码**：

| code | 说明 | 前端处理 |
|---|---|---|
| 75001 | 迁移任务不存在 | 提示任务不存在，检查 ID |
| 75002 | 任务状态不允许当前操作 | 检查任务状态是否正确（需 PRECHECKED 才能执行） |
| 75003 | 迁移文件无效 | 提示文件格式错误 |
| 75004 | 迁移预检未通过 | 提示预检失败，查看错误明细 |
| 75005 | 附件下载失败 | 提示网络问题，可重试 |

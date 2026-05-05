# 外部博客迁移接口文档

本文档面向后台管理台联调，覆盖外部博客 JSON 文件迁移 v1。

## 1. 能力范围

- 后台上传 JSON 迁移文件并创建任务。
- 任务指定站内作者，所有导入文章归属该作者。
- 分类和标签必须预先存在，不自动创建。
- 外部附件下载入库后替换正文和封面 URL，再复用后台文章创建链路导入文章。
- 支持预检、执行、任务/记录查询和失败记录 Excel 导出。

## 2. 鉴权要求

接口统一前缀：`/api/sys/migrations/blog`，均要求后台登录。

| 权限标识 | 说明 |
| --- | --- |
| `content:migration:query` | 查询迁移任务、详情和记录 |
| `content:migration:create` | 创建迁移任务 |
| `content:migration:execute` | 执行预检和导入 |
| `content:migration:export` | 导出失败记录 |

## 3. JSON v1 格式

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

关键约束：

- `sourcePlatform` 必填，会标准化为小写。
- 幂等键为 `sourcePlatform + ":" + externalPostId`。
- 同一任务内重复 `externalPostId` 预检失败。
- 全局已成功导入的幂等键，执行时跳过并记录 `SKIPPED`。
- 附件 URL 仅支持 `http` / `https`。
- 附件下载失败时，该文章导入失败，不创建部分文章。

## 4. 接口总览

| 接口 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 创建任务 | POST | `/api/sys/migrations/blog/tasks` | `content:migration:create` |
| 执行预检 | POST | `/api/sys/migrations/blog/tasks/{id}/precheck` | `content:migration:execute` |
| 执行导入 | POST | `/api/sys/migrations/blog/tasks/{id}/execute` | `content:migration:execute` |
| 分页查询任务 | GET | `/api/sys/migrations/blog/tasks` | `content:migration:query` |
| 查询任务详情 | GET | `/api/sys/migrations/blog/tasks/{id}` | `content:migration:query` |
| 分页查询记录 | GET | `/api/sys/migrations/blog/tasks/{id}/records` | `content:migration:query` |
| 导出失败记录 | GET | `/api/sys/migrations/blog/tasks/{id}/failures/export` | `content:migration:export` |

## 5. 接口详情

### 5.1 创建任务

- 请求：`POST /api/sys/migrations/blog/tasks`
- Content-Type：`multipart/form-data`
- 鉴权：`content:migration:create`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `authorId` | Long | 是 | 导入文章归属作者 ID |
| `remark` | String | 否 | 备注，最多 256 字符 |
| `file` | File | 是 | JSON 迁移文件 |

- 响应：`Result<BlogMigrationTaskVO>`

### 5.2 执行预检

- 请求：`POST /api/sys/migrations/blog/tasks/{id}/precheck`
- 鉴权：`content:migration:execute`
- 响应：`Result<BlogMigrationPrecheckResultVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | Long | 任务 ID |
| `totalCount` | Integer | 总文章数 |
| `passed` | Boolean | 是否通过 |
| `errors` | Array | 失败明细，元素为 `BlogMigrationRecordVO` |

预检失败不会抛业务异常，响应中 `passed=false` 并返回错误明细。

### 5.3 执行导入

- 请求：`POST /api/sys/migrations/blog/tasks/{id}/execute`
- 鉴权：`content:migration:execute`
- 仅允许 `PRECHECKED` 状态执行。
- 响应：`Result<BlogMigrationTaskVO>`

### 5.4 查询任务

- 请求：`GET /api/sys/migrations/blog/tasks`
- 鉴权：`content:migration:query`

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码 |
| `size` | Long | 否 | 每页条数，最大 100 |
| `status` | Integer | 否 | 任务状态 |
| `sourcePlatform` | String | 否 | 来源平台 |
| `authorId` | Long | 否 | 作者 ID |

### 5.5 查询记录

- 请求：`GET /api/sys/migrations/blog/tasks/{id}/records`
- 鉴权：`content:migration:query`

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码 |
| `size` | Long | 否 | 每页条数，最大 100 |
| `status` | Integer | 否 | 记录状态 |

### 5.6 导出失败记录

- 请求：`GET /api/sys/migrations/blog/tasks/{id}/failures/export`
- 鉴权：`content:migration:export`
- 响应：Excel 文件，文件名 `blog-migration-failures-{id}.xlsx`。

## 6. 枚举与错误码

任务状态：

| 值 | 说明 |
| --- | --- |
| `0` | CREATED，已创建 |
| `1` | PRECHECKED，预检通过 |
| `2` | RUNNING，执行中 |
| `3` | COMPLETED，已完成 |
| `4` | FAILED，失败 |
| `5` | CANCELLED，已取消 |

记录状态：

| 值 | 说明 |
| --- | --- |
| `0` | PENDING，待处理 |
| `1` | SUCCESS，成功 |
| `2` | FAILED，失败 |
| `3` | SKIPPED，已跳过 |

常见错误码：

| code | 枚举 | 说明 |
| --- | --- | --- |
| `75001` | `MIGRATION_TASK_NOT_FOUND` | 迁移任务不存在 |
| `75002` | `MIGRATION_TASK_STATUS_INVALID` | 任务状态不允许当前操作 |
| `75003` | `MIGRATION_FILE_INVALID` | 迁移文件无效 |
| `75004` | `MIGRATION_PRECHECK_FAILED` | 迁移预检未通过 |
| `75005` | `MIGRATION_ATTACHMENT_DOWNLOAD_FAILED` | 附件下载失败 |

# 举报模块接口文档

本文档面向前端联调，对应项目中举报模块的实现。

## 1. 当前能力范围

当前已支持：

- 用户提交举报（文章、评论、用户等）
- 用户查询自己的举报记录
- 后台举报列表筛选与详情查看
- 后台举报处理（接手、处理、驳回）
- 超管接管举报
- 处理日志查看

## 2. 鉴权要求

### 2.1 用户侧接口

用户侧接口统一走 `/api/user/reports/**`，要求登录：

```http
Authorization: Bearer <accessToken>
```

### 2.2 后台管理接口

后台管理接口统一走 `/api/sys/reports/**`，除登录外还要求对应权限：

| 权限标识 | 说明 |
| --- | --- |
| `sys:report:query` | 查询举报列表和详情 |
| `sys:report:handle` | 处理举报（接手、处理、驳回、接管） |

## 3. 用户侧接口

### 3.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 提交举报 | POST | `/api/user/reports` | 提交举报 |
| 查询我的举报记录 | GET | `/api/user/reports` | 分页查询举报列表 |
| 查询举报详情 | GET | `/api/user/reports/{id}` | 获取举报详情 |

### 3.2 提交举报

- 请求：`POST /api/user/reports`
- 鉴权：是
- 请求体：`ReportCreateRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetType` | String | 是 | 举报对象类型：article/comment/user/chat_message |
| `targetId` | Long | 是 | 举报对象ID |
| `reasonCode` | String | 是 | 举报原因编码 |
| `reasonDetail` | String | 否 | 补充说明 |

- 响应：`ReportVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |
| `targetType` | String | 举报对象类型 |
| `targetId` | Long | 举报对象ID |
| `reasonCode` | String | 举报原因编码 |
| `reasonDetail` | String | 补充说明 |
| `status` | Integer | 状态：0-待处理/1-处理中/2-已处理/3-已驳回 |
| `reportedAt` | DateTime | 举报时间 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "targetType": "comment",
    "targetId": 501,
    "reasonCode": "spam",
    "reasonDetail": "该评论为垃圾广告内容",
    "status": 0,
    "reportedAt": "2026-04-20 16:30:00",
    "handledAt": null,
    "resultType": null,
    "remark": null
  }
}
```

### 3.3 查询我的举报记录

- 请求：`GET /api/user/reports`
- 鉴权：是
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetType` | String | 否 | 举报对象类型筛选 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`PageResult<ReportVO>`

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
        "id": 2,
        "targetType": "article",
        "targetId": 100,
        "reasonCode": "plagiarism",
        "reasonDetail": "该文章抄袭他人原创内容",
        "status": 1,
        "reportedAt": "2026-04-21 09:00:00",
        "handledAt": null,
        "resultType": null,
        "remark": null
      },
      {
        "id": 1,
        "targetType": "comment",
        "targetId": 501,
        "reasonCode": "spam",
        "reasonDetail": "该评论为垃圾广告内容",
        "status": 2,
        "reportedAt": "2026-04-20 16:30:00",
        "handledAt": "2026-04-21 10:00:00",
        "resultType": "delete_content",
        "remark": "已核实并删除违规评论"
      }
    ]
  }
}
```

### 3.4 查询举报详情

- 请求：`GET /api/user/reports/{id}`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 响应：`ReportVO`

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "targetType": "comment",
    "targetId": 501,
    "reasonCode": "spam",
    "reasonDetail": "该评论为垃圾广告内容",
    "status": 2,
    "reportedAt": "2026-04-20 16:30:00",
    "handledAt": "2026-04-21 10:00:00",
    "resultType": "delete_content",
    "remark": "已核实并删除违规评论"
  }
}
```

## 4. 后台管理接口

### 4.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页筛选举报 | GET | `/api/sys/reports` | 查询举报列表 |
| 举报详情 | GET | `/api/sys/reports/{id}` | 获取举报详情 |
| 接手举报 | PUT | `/api/sys/reports/{id}/take` | 受理举报 |
| 处理举报 | PUT | `/api/sys/reports/{id}/handle` | 执行处理 |
| 驳回举报 | PUT | `/api/sys/reports/{id}/reject` | 驳回举报 |
| 超管接管举报 | PUT | `/api/sys/reports/{id}/override` | 强制接管 |
| 处理日志 | GET | `/api/sys/reports/{id}/logs` | 查看处理历史 |

### 4.2 分页筛选举报

- 请求：`GET /api/sys/reports`
- 鉴权：`sys:report:query`
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | Integer | 否 | 状态：0-待处理/1-处理中/2-已处理/3-已驳回 |
| `reportTargetType` | String | 否 | 举报对象类型：article/comment/chat_message |
| `reporterUserId` | Long | 否 | 举报人ID |
| `reportedStart` | LocalDateTime | 否 | 举报开始时间 |
| `reportedEnd` | LocalDateTime | 否 | 举报结束时间 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

- 响应：`PageResult<ReportAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |
| `reportTargetType` | String | 举报对象类型 |
| `reportTargetId` | Long | 举报对象ID |
| `reporterUserId` | Long | 举报人ID |
| `reporterUsername` | String | 举报人用户名 |
| `reasonCode` | String | 举报原因编码 |
| `reasonDetail` | String | 补充说明 |
| `status` | Integer | 状态 |
| `handlerUserId` | Long | 处理人ID |
| `handlerUsername` | String | 处理人用户名 |
| `resultType` | String | 处理结果类型 |
| `punishmentType` | String | 处罚类型 |
| `reportedAt` | DateTime | 举报时间 |
| `handledAt` | DateTime | 处理时间 |
| `createdAt` | DateTime | 创建时间 |

- 响应示例：

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
        "reportTargetType": "comment",
        "reportTargetId": 501,
        "reporterUserId": 5,
        "reporterUsername": "wangwu",
        "reasonCode": "spam",
        "reasonDetail": "该评论为垃圾广告内容",
        "status": 1,
        "handlerUserId": 1,
        "handlerUsername": "admin",
        "resultType": null,
        "punishmentType": null,
        "reportedAt": "2026-04-20 16:30:00",
        "handledAt": null,
        "remark": null,
        "createdAt": "2026-04-20 16:30:00"
      }
    ]
  }
}
```

### 4.3 举报详情

- 请求：`GET /api/sys/reports/{id}`
- 鉴权：`sys:report:query`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 响应：`ReportAdminVO`

### 4.4 接手举报

- 请求：`PUT /api/sys/reports/{id}/take`
- 鉴权：`sys:report:handle`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 响应：空

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

### 4.5 处理举报

- 请求：`PUT /api/sys/reports/{id}/handle`
- 鉴权：`sys:report:handle`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 请求体：`ReportHandleRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resultType` | String | 是 | 处理结果类型：delete_content/revoke_message/mute_user/ban_user/record_only |
| `punishmentType` | String | 否 | 处罚类型 |
| `remark` | String | 否 | 备注 |

- 响应：空

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

### 4.6 驳回举报

- 请求：`PUT /api/sys/reports/{id}/reject`
- 鉴权：`sys:report:handle`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 请求体：`ReportRejectRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `remark` | String | 否 | 驳回原因备注，最大512字符 |

- 响应：空

### 4.7 超管接管举报

- 请求：`PUT /api/sys/reports/{id}/override`
- 鉴权：`sys:report:handle`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 响应：空

### 4.8 处理日志

- 请求：`GET /api/sys/reports/{id}/logs`
- 鉴权：`sys:report:query`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 举报ID |

- 响应：`List<ReportHandleLogVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 日志ID |
| `fromStatus` | Integer | 变更前状态 |
| `toStatus` | Integer | 变更后状态 |
| `actionType` | String | 动作类型 |
| `actionResult` | String | 处理结果 |
| `operatorUserId` | Long | 操作人ID |
| `operatorUsername` | String | 操作人用户名 |
| `actionRemark` | String | 操作备注 |
| `createdAt` | DateTime | 创建时间 |

## 5. 枚举值说明

### 5.1 举报对象类型 (ReportTargetTypeEnum)

| 值 | 说明 |
| --- | --- |
| `article` | 文章 |
| `comment` | 评论 |
| `user` | 用户 |
| `chat_message` | 聊天消息 |

### 5.2 举报状态 (ReportRecordStatusEnum)

| 值 | 说明 |
| --- | --- |
| `0` | 待处理 |
| `1` | 处理中 |
| `2` | 已处理 |
| `3` | 已驳回 |

### 5.3 处理结果类型 (ReportActionTypeEnum)

| 值 | 说明 |
| --- | --- |
| `delete_content` | 删除内容 |
| `revoke_message` | 撤回消息 |
| `mute_user` | 禁言用户 |
| `ban_user` | 封禁用户 |
| `record_only` | 仅记录 |

### 5.4 操作类型

| 值 | 说明 |
| --- | --- |
| `claim` | 接手 |
| `handle` | 处理 |
| `reject` | 驳回 |
| `override` | 超管接管 |

## 6. 维护规则

- 新增、删除、修改前端可见接口时，必须同步更新对应文档。
- 如果只是补字段、改枚举或改边界行为，也不能只改代码不改文档。

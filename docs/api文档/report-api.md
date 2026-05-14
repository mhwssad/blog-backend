# 举报模块接口文档（前端参考手册）

> 本文档面向前端联调使用，提供每个接口的请求示例、响应示例、字段说明和错误处理指引。
> 举报功能分为两大模块：**用户举报**（提交举报、查询举报记录）和**后台举报管理**（处理举报、查看日志等）。

---

## 快速导航

| 前端页面 | 接口 |
|---|---|
| [举报提交页](#1-举报提交页) | 提交举报 |
| [我的举报记录页](#2-我的举报记录页) | 查询举报列表、举报详情 |
| [后台举报列表页](#3-后台举报列表页) | 分页筛选举报、举报详情 |
| [后台举报处理面板](#4-后台举报处理面板) | 接手、处理、驳回、超管接管 |
| [后台处理日志页](#5-后台处理日志页) | 处理日志查询 |

---

## 1. 举报提交页

### 提交举报

**接口信息**
- 路径: `POST /api/user/reports`
- 鉴权: 必须（需要登录态）
- 说明: 用户提交一个新的举报，支持文章、评论、聊天消息等对象类型。

**请求示例**

```javascript
// axios
axios.post('/api/user/reports', {
  targetType: 'comment',      // 必填，举报对象类型
  targetId: 501,              // 必填，举报对象ID
  reasonCode: 'spam',         // 必填，举报原因编码
  reasonDetail: '该评论为垃圾广告内容，重复发布多次'  // 可选，补充说明
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例（成功）**

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
    "reasonDetail": "该评论为垃圾广告内容，重复发布多次",
    "status": 0,
    "reportedAt": "2026-04-20T16:30:00",
    "handledAt": null,
    "resultType": null,
    "remark": null
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 举报记录ID |
| `targetType` | String | 举报对象类型：`article`/`comment`/`chat_message` |
| `targetId` | Long | 被举报对象的ID |
| `reasonCode` | String | 举报原因编码 |
| `reasonDetail` | String | 用户补充的说明 |
| `status` | Integer | 举报状态：0-待处理 / 1-处理中 / 2-已处理 / 3-已驳回 |
| `reportedAt` | DateTime | 举报提交时间 |
| `handledAt` | DateTime | 处理时间，有值时表示已处理 |
| `resultType` | String | 处理结果类型，有值时表示已处理 |
| `remark` | String | 处理备注，有值时表示已处理 |

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 401 | 未登录 | 跳转登录页 |
| 400 | 参数校验失败（如缺少必填字段） | 提示用户补充信息 |
| 403 | 无权限 | 提示用户无操作权限 |
| 500 | 服务器内部错误 | 提示"提交失败，请稍后重试" |

**举报对象类型枚举（targetType）**

| 值 | 说明 | 示例场景 |
|------|------|---------|
| `article` | 文章 | 举报文章内容违规 |
| `comment` | 评论 | 举报评论包含不当信息 |
| `chat_message` | 聊天消息 | 举报聊天消息 |

**举报原因编码枚举（reasonCode）**

| 值 | 说明 |
|------|------|
| `spam` | 垃圾广告 |
| `politics` | 政治敏感 |
| `porn` | 色情低俗 |
| `violence` | 暴力血腥 |
| `fraud` | 诈骗欺诈 |
| `plagiarism` | 抄袭侵权 |
| `harassment` | 人身攻击 |
| `other` | 其他违规 |

---

## 2. 我的举报记录页

### 查询我的举报列表

**接口信息**
- 路径: `GET /api/user/reports`
- 鉴权: 必须
- 说明: 分页查询当前用户的举报记录列表，支持按举报对象类型筛选。

**请求示例**

```javascript
// 不带筛选条件，默认第一页
axios.get('/api/user/reports', {
  params: {
    current: 1,
    size: 10
  },
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 按举报对象类型筛选
axios.get('/api/user/reports', {
  params: {
    targetType: 'comment',
    current: 1,
    size: 10
  },
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

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
        "id": 2,
        "targetType": "article",
        "targetId": 100,
        "reasonCode": "plagiarism",
        "reasonDetail": "该文章疑似抄袭他人原创内容",
        "status": 1,
        "reportedAt": "2026-04-21T09:00:00",
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
        "reportedAt": "2026-04-20T16:30:00",
        "handledAt": "2026-04-21T10:00:00",
        "resultType": "delete_content",
        "remark": "已核实并删除违规评论"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records[]` | Array | 举报记录数组 |
| `records[].id` | Long | 举报ID |
| `records[].targetType` | String | 举报对象类型 |
| `records[].targetId` | Long | 举报对象ID |
| `records[].reasonCode` | String | 举报原因编码 |
| `records[].reasonDetail` | String | 补充说明 |
| `records[].status` | Integer | 状态：0-待处理 / 1-处理中 / 2-已处理 / 3-已驳回 |
| `records[].reportedAt` | DateTime | 举报时间 |
| `records[].handledAt` | DateTime | 处理时间 |
| `records[].resultType` | String | 处理结果类型 |
| `records[].remark` | String | 处理备注 |

**前端状态展示建议**

| status | 显示文案 | 显示样式 |
|--------|----------|---------|
| 0 | 待处理 | 灰色/橙色标签 |
| 1 | 处理中 | 蓝色标签 |
| 2 | 已处理 | 绿色标签 |
| 3 | 已驳回 | 灰色标签 |

---

### 查询举报详情

**接口信息**
- 路径: `GET /api/user/reports/{id}`
- 鉴权: 必须
- 说明: 查询单条举报记录的完整详情。

**请求示例**

```javascript
axios.get('/api/user/reports/1', {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

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
    "reasonDetail": "该评论为垃圾广告内容，重复发布多次",
    "status": 2,
    "reportedAt": "2026-04-20T16:30:00",
    "handledAt": "2026-04-21T10:00:00",
    "resultType": "delete_content",
    "remark": "已核实并删除违规评论"
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 举报ID |
| `targetType` | String | 举报对象类型 |
| `targetId` | Long | 举报对象ID |
| `reasonCode` | String | 举报原因编码 |
| `reasonDetail` | String | 用户补充的说明 |
| `status` | Integer | 举报状态 |
| `reportedAt` | DateTime | 举报时间 |
| `handledAt` | DateTime | 处理时间（已处理/已驳回时有值） |
| `resultType` | String | 处理结果类型（已处理时有值） |
| `remark` | String | 处理备注（已处理/已驳回时有值） |

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 401 | 未登录 | 跳转登录页 |
| 403 | 无权限访问该举报 | 提示"无权查看该举报" |
| 404 | 举报记录不存在 | 提示"举报记录不存在" |

---

## 3. 后台举报列表页

### 分页筛选举报

**接口信息**
- 路径: `GET /api/sys/reports`
- 鉴权: 必须（需 `sys:report:query` 权限）
- 说明: 管理端分页查询举报列表，支持按状态、目标类型、举报人ID、时间范围等条件筛选。

**请求示例**

```javascript
// 查询待处理的举报列表
axios.get('/api/sys/reports', {
  params: {
    status: 0,
    current: 1,
    size: 20
  },
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 按时间范围筛选
axios.get('/api/sys/reports', {
  params: {
    reportedStart: '2026-04-01T00:00:00',
    reportedEnd: '2026-04-30T23:59:59',
    reportTargetType: 'comment',
    current: 1,
    size: 20
  },
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 按举报人ID精确筛选
axios.get('/api/sys/reports', {
  params: {
    reporterUserId: 5,
    current: 1,
    size: 20
  },
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

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
        "reportedAt": "2026-04-20T16:30:00",
        "handledAt": null,
        "remark": null,
        "createdAt": "2026-04-20T16:30:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Long | 符合条件的总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records[].id` | Long | 举报记录ID |
| `records[].reportTargetType` | String | 被举报对象类型 |
| `records[].reportTargetId` | Long | 被举报对象ID |
| `records[].reporterUserId` | Long | 举报人用户ID |
| `records[].reporterUsername` | String | 举报人用户名 |
| `records[].reasonCode` | String | 举报原因编码 |
| `records[].reasonDetail` | String | 举报补充说明 |
| `records[].status` | Integer | 状态：0-待处理 / 1-处理中 / 2-已处理 / 3-已驳回 |
| `records[].handlerUserId` | Long | 当前处理人ID（处理中/已处理/已驳回时有值） |
| `records[].handlerUsername` | String | 处理人用户名 |
| `records[].resultType` | String | 处理结果类型（已处理时有值） |
| `records[].punishmentType` | String | 处罚类型（已处理时有值） |
| `records[].reportedAt` | DateTime | 举报时间 |
| `records[].handledAt` | DateTime | 处理时间（已处理/已驳回时有值） |
| `records[].remark` | String | 处理备注（已处理/已驳回时有值） |
| `records[].createdAt` | DateTime | 记录创建时间 |

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | Integer | 否 | 状态筛选：0-待处理 / 1-处理中 / 2-已处理 / 3-已驳回 |
| `reportTargetType` | String | 否 | 举报对象类型筛选：`article`/`comment`/`chat_message` |
| `reporterUserId` | Long | 否 | 举报人ID精确筛选 |
| `reportedStart` | LocalDateTime | 否 | 举报时间范围起点（包含） |
| `reportedEnd` | LocalDateTime | 否 | 举报时间范围终点（包含） |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

---

### 举报详情（后台）

**接口信息**
- 路径: `GET /api/sys/reports/{id}`
- 鉴权: 必须（需 `sys:report:query` 权限）
- 说明: 查询单条举报记录的完整详情，包含处理人、被举报对象信息。

**请求示例**

```javascript
axios.get('/api/sys/reports/1', {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "reportTargetType": "comment",
    "reportTargetId": 501,
    "reporterUserId": 5,
    "reporterUsername": "wangwu",
    "reasonCode": "spam",
    "reasonDetail": "该评论为垃圾广告内容",
    "status": 2,
    "handlerUserId": 1,
    "handlerUsername": "admin",
    "resultType": "delete_content",
    "punishmentType": "content_delete",
    "reportedAt": "2026-04-20T16:30:00",
    "handledAt": "2026-04-21T10:00:00",
    "remark": "已核实并删除违规评论",
    "createdAt": "2026-04-20T16:30:00"
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 举报ID |
| `reportTargetType` | String | 被举报对象类型 |
| `reportTargetId` | Long | 被举报对象ID |
| `reporterUserId` | Long | 举报人ID |
| `reporterUsername` | String | 举报人用户名 |
| `reasonCode` | String | 举报原因编码 |
| `reasonDetail` | String | 补充说明 |
| `status` | Integer | 举报状态 |
| `handlerUserId` | Long | 处理人ID |
| `handlerUsername` | String | 处理人用户名 |
| `resultType` | String | 处理结果类型 |
| `punishmentType` | String | 处罚类型 |
| `reportedAt` | DateTime | 举报时间 |
| `handledAt` | DateTime | 处理时间 |
| `remark` | String | 处理备注 |
| `createdAt` | DateTime | 创建时间 |

---

## 4. 后台举报处理面板

### 接手举报

**接口信息**
- 路径: `PUT /api/sys/reports/{id}/take`
- 鉴权: 必须（需 `sys:report:handle` 权限）
- 说明: 将待处理状态的举报标记为处理中，记录当前操作人为处理人。仅在举报状态为待处理（status=0）时可操作。

**请求示例**

```javascript
axios.put('/api/sys/reports/1/take', {}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 400 | 举报状态不是待处理 | 提示"该举报已被其他管理员处理" |
| 403 | 无 `sys:report:handle` 权限 | 提示无权限操作 |
| 404 | 举报记录不存在 | 提示"举报记录不存在" |

**前端操作前置检查**
- 接手前需确认举报状态为 `0-待处理`
- 接手后状态变更为 `1-处理中`，处理人变更为当前管理员

---

### 处理举报

**接口信息**
- 路径: `PUT /api/sys/reports/{id}/handle`
- 鉴权: 必须（需 `sys:report:handle` 权限）
- 说明: 对举报进行处理并执行对应治理动作（删除内容、撤回消息、禁言、封禁等）。处理后将状态更新为已处理，记录审计日志，并自动向举报人发送站内通知。

**请求示例**

```javascript
// 示例1：删除违规内容
axios.put('/api/sys/reports/1/handle', {
  resultType: 'delete_content',
  punishmentType: 'content_delete',
  remark: '核实后确认内容违规，已删除'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 示例2：禁言用户（全局禁言7天）
axios.put('/api/sys/reports/3/handle', {
  resultType: 'mute_user',
  punishmentType: 'mute',
  muteScope: 'global',
  muteUntil: '2026-04-28T10:00:00',
  remark: '多次发布垃圾广告，予以禁言处理'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 示例3：禁言用户（在指定主题频道禁言）
axios.put('/api/sys/reports/5/handle', {
  resultType: 'mute_user',
  punishmentType: 'mute',
  conversationId: 1001,
  muteScope: 'topic_channel',
  muteUntil: '2026-04-25T10:00:00',
  remark: '在主题频道内多次刷屏'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 示例4：封禁用户
axios.put('/api/sys/reports/7/handle', {
  resultType: 'ban_user',
  punishmentType: 'ban',
  remark: '恶意刷屏、辱骂他人，永久封禁'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})

// 示例5：仅记录不处罚
axios.put('/api/sys/reports/9/handle', {
  resultType: 'record_only',
  remark: '举报内容不构成违规，仅作记录'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `resultType` | String | 是 | 处理结果类型 |
| `punishmentType` | String | 否 | 处罚类型 |
| `remark` | String | 否 | 处理备注，最大512字符 |
| `conversationId` | Long | 否 | 会话ID，举报聊天消息时必填；`resultType=mute_user` 且 `muteScope` 为 `topic_channel`/`group` 时作为禁言关联会话 |
| `muteScope` | String | 否 | 禁言范围，`resultType=mute_user` 时使用：`global`（全站）/ `lobby`（大厅）/ `topic_channel`（主题频道）/ `group`（群组） |
| `muteUntil` | DateTime | 否 | 禁言截止时间，`resultType=mute_user` 时使用；默认禁言1天；传入 `null` 表示永久禁言 |

**resultType 枚举值**

| 值 | 说明 | 触发动作 |
|------|------|---------|
| `delete_content` | 删除内容 | 删除被举报的文章/评论 |
| `revoke_message` | 撤回消息 | 撤回被举报的聊天消息 |
| `mute_user` | 禁言用户 | 调用禁言服务创建禁言记录 |
| `ban_user` | 封禁用户 | 封禁被举报用户 |
| `record_only` | 仅记录 | 不执行处罚，仅记录处理结果 |

**punishmentType 枚举值**

| 值 | 说明 |
|------|------|
| `content_delete` | 内容删除 |
| `message_revoke` | 消息撤回 |
| `mute` | 禁言 |
| `ban` | 封禁 |
| `none` | 无处罚 |

**禁言参数说明**

当 `resultType = mute_user` 时，系统会自动调用统一禁言服务：

| 参数组合 | 效果 |
|----------|------|
| 不传 `muteScope` | 默认全站禁言（`global`） |
| 不传 `muteUntil` | 默认禁言1天 |
| `muteScope=global` | 阻断所有聊天场景 |
| `muteScope=lobby` | 阻断大厅和全站频道 |
| `muteScope=topic_channel` + `conversationId` | 仅在指定主题频道禁言 |
| `muteScope=group` + `conversationId` | 仅在指定群组禁言 |
| `muteUntil=null` | 永久禁言 |

禁言记录来源标记为 `report`，并关联原举报ID。

**处理结果通知**

处理完成后，系统会自动向举报人发送站内通知（受用户 `report_result` 通知偏好控制）：

| 通知场景 | 通知标题 | 通知内容示例 |
|----------|----------|-------------|
| `delete_content` | 你的举报已处理 | 你举报的评论已被删除 |
| `revoke_message` | 你的举报已处理 | 你举报的消息已被撤回 |
| `mute_user` | 你的举报已处理 | 相关用户已被禁言 |
| `ban_user` | 你的举报已处理 | 相关用户已被封禁 |
| `record_only` | 你的举报已处理 | 你举报的内容经核实不构成违规 |
| 驳回（reject） | 你的举报已驳回 | 你举报的内容经核实不构成违规（附驳回原因） |

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 400 | 举报状态不是待处理或处理中 | 提示"该举报已处理完毕" |
| 400 | 缺少 resultType | 提示"请选择处理结果" |
| 403 | 无 `sys:report:handle` 权限 | 提示无权限操作 |
| 404 | 举报记录不存在 | 提示"举报记录不存在" |

---

### 驳回举报

**接口信息**
- 路径: `PUT /api/sys/reports/{id}/reject`
- 鉴权: 必须（需 `sys:report:handle` 权限）
- 说明: 驳回无效举报，将状态标记为已驳回并记录驳回备注。驳回后会向举报人发送站内通知。

**请求示例**

```javascript
axios.put('/api/sys/reports/1/reject', {
  remark: '该内容未发现违规行为，举报不成立'
}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `remark` | String | 否 | 驳回原因备注，最大512字符 |

**驳回结果通知**

驳回后系统自动向举报人发送站内通知：
- 通知类型：`REPORT_RESULT`
- 通知标题：你的举报已驳回
- 通知内容：举报内容经核实不构成违规（附驳回原因，截取前100字符）

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 400 | 举报状态不是待处理或处理中 | 提示"该举报已处理完毕" |
| 403 | 无 `sys:report:handle` 权限 | 提示无权限操作 |
| 404 | 举报记录不存在 | 提示"举报记录不存在" |

---

### 超管接管举报

**接口信息**
- 路径: `PUT /api/sys/reports/{id}/override`
- 鉴权: 必须（需 `sys:report:handle` 权限，仅超级管理员可操作）
- 说明: 超级管理员强制接管正在处理中的举报，重新认领并记录原处理人。已处理或已驳回的举报不可再接管。

**请求示例**

```javascript
axios.put('/api/sys/reports/1/override', {}, {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**前置条件**
- 举报状态必须为 `1-处理中`（已接手但未处理）
- 已处理（status=2）或已驳回（status=3）的举报不可接管

**错误码**

| code | 说明 | 前端处理 |
|------|------|---------|
| 400 | 举报状态不是处理中 | 提示"仅处理中的举报可接管" |
| 403 | 无 `sys:report:handle` 权限 | 提示无权限操作 |
| 404 | 举报记录不存在 | 提示"举报记录不存在" |

---

## 5. 后台处理日志页

### 处理日志查询

**接口信息**
- 路径: `GET /api/sys/reports/{id}/logs`
- 鉴权: 必须（需 `sys:report:query` 权限）
- 说明: 查询某条举报记录的所有处理操作日志，包含认领、处理、驳回、超管接管等操作记录。按时间倒序排列。

**请求示例**

```javascript
axios.get('/api/sys/reports/1/logs', {
  headers: {
    Authorization: 'Bearer <accessToken>'
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "id": 4,
      "fromStatus": 1,
      "toStatus": 2,
      "actionType": "handle",
      "actionResult": "delete_content",
      "operatorUserId": 1,
      "operatorUsername": "admin",
      "actionRemark": "已核实并删除违规评论",
      "createdAt": "2026-04-21T10:00:00"
    },
    {
      "id": 3,
      "fromStatus": 0,
      "toStatus": 1,
      "actionType": "claim",
      "actionResult": null,
      "operatorUserId": 1,
      "operatorUsername": "admin",
      "actionRemark": null,
      "createdAt": "2026-04-21T09:30:00"
    },
    {
      "id": 1,
      "fromStatus": null,
      "toStatus": 0,
      "actionType": "create",
      "actionResult": null,
      "operatorUserId": 5,
      "operatorUsername": "wangwu",
      "actionRemark": null,
      "createdAt": "2026-04-20T16:30:00"
    }
  ]
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 日志记录ID |
| `fromStatus` | Integer | 变更前状态（null 表示初始创建） |
| `toStatus` | Integer | 变更后状态 |
| `actionType` | String | 操作类型 |
| `actionResult` | String | 处理结果（仅 handle 操作时有值） |
| `operatorUserId` | Long | 操作人用户ID |
| `operatorUsername` | String | 操作人用户名 |
| `actionRemark` | String | 操作备注 |
| `createdAt` | DateTime | 操作时间 |

**actionType 操作类型枚举**

| 值 | 说明 | fromStatus | toStatus |
|------|------|------------|----------|
| `create` | 举报提交 | null | 0 |
| `claim` | 管理员接手 | 0 | 1 |
| `handle` | 管理员处理 | 1 | 2 |
| `reject` | 管理员驳回 | 1 | 3 |
| `override` | 超管接管 | 1 | 1 |

**fromStatus / toStatus 状态枚举**

| 值 | 说明 |
|------|------|
| 0 | 待处理 |
| 1 | 处理中 |
| 2 | 已处理 |
| 3 | 已驳回 |

---

## 6. 通用响应结构

所有接口均返回以下统一响应结构：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 业务状态码，200表示成功 |
| `message` | String | 操作结果描述 |
| `timestamp` | Long | 服务器响应时间戳（毫秒） |
| `data` | Object/Array | 响应数据，失败时通常为 null |

---

## 7. 通用错误码

| code | 说明 | 前端处理 |
|------|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数校验失败 | 提示具体校验失败原因 |
| 401 | 未登录或 Token 过期 | 跳转登录页 |
| 403 | 无权限 | 提示无权限操作 |
| 404 | 资源不存在 | 提示资源不存在 |
| 500 | 服务器内部错误 | 提示"服务异常，请稍后重试" |

---

## 8. 状态码与枚举对照表

**举报状态（status）**

| 值 | 前端显示 | 说明 |
|------|----------|------|
| 0 | 待处理 | 举报待分配处理人 |
| 1 | 处理中 | 已有管理员接手处理 |
| 2 | 已处理 | 举报已处理，处罚已执行 |
| 3 | 已驳回 | 举报无效，已被驳回 |

**举报对象类型（targetType / reportTargetType）**

| 值 | 说明 |
|------|------|
| `article` | 文章 |
| `comment` | 评论 |
| `chat_message` | 聊天消息 |

**处理结果类型（resultType）**

| 值 | 说明 |
|------|------|
| `delete_content` | 删除内容 |
| `revoke_message` | 撤回消息 |
| `mute_user` | 禁言用户 |
| `ban_user` | 封禁用户 |
| `record_only` | 仅记录 |

**处罚类型（punishmentType）**

| 值 | 说明 |
|------|------|
| `content_delete` | 内容删除 |
| `message_revoke` | 消息撤回 |
| `mute` | 禁言 |
| `ban` | 封禁 |
| `none` | 无处罚 |

---

## 9. 维护规则

- 新增、删除、修改前端可见接口时，必须同步更新对应文档。
- 如果只是补字段、改枚举或改边界行为，也不能只改代码不改文档。
- 接口文档更新后需在 `docs/tasks/README.md` 中同步任务状态。
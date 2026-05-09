# AI 模块接口文档

> 本文档为完整的前端参考手册，包含每个接口的完整请求代码示例和响应示例。

---

## AI 对话页

### 创建 AI 会话

**接口信息**
- 路径: `POST /api/user/ai/sessions`
- 鉴权: 是
- 说明: 创建一个新的 AI 对话会话，不传 `channelConfigId` 则使用默认渠道

**请求示例**
```javascript
// axios
axios.post('/api/user/ai/sessions', {
  title: 'Java 学习助手',
  channelConfigId: 1,
  sceneType: 'general'
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `title` | String | 否 | 会话标题 |
| `channelConfigId` | Long | 否 | 渠道配置ID，不填则使用默认渠道 |
| `sceneType` | String | 否 | 会话场景，默认 `general` |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "title": "Java 学习助手",
    "channelConfigId": 1,
    "sceneType": "general",
    "status": 1,
    "lastMessageAt": null,
    "createdAt": "2026-04-15T14:00:00",
    "updatedAt": "2026-04-15T14:00:00"
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 会话ID |
| `title` | String | 会话标题 |
| `channelConfigId` | Long | 渠道配置ID |
| `sceneType` | String | 会话场景 |
| `status` | Integer | 状态：0-关闭，1-正常 |
| `lastMessageAt` | DateTime | 最后消息时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

**错误码**
| code | 说明 | 前端处理 |
|-----|------|---------|
| 401 | 未登录 | 跳转登录页 |
| 403 | 无权限 | 提示用户 |
| 500 | 服务器错误 | 提示稍后重试 |

---

### 查询我的 AI 会话列表

**接口信息**
- 路径: `GET /api/user/ai/sessions`
- 鉴权: 是
- 说明: 分页查询当前用户的 AI 会话列表

**请求示例**
```javascript
// axios
axios.get('/api/user/ai/sessions', {
  params: { current: 1, size: 10 },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

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
        "id": 1,
        "title": "Java 学习助手",
        "channelConfigId": 1,
        "sceneType": "general",
        "status": 1,
        "lastMessageAt": "2026-04-15T14:05:00",
        "createdAt": "2026-04-15T14:00:00",
        "updatedAt": "2026-04-15T14:05:00"
      },
      {
        "id": 2,
        "title": "文章写作辅助",
        "channelConfigId": 1,
        "sceneType": "general",
        "status": 1,
        "lastMessageAt": "2026-04-14T09:30:00",
        "createdAt": "2026-04-14T09:00:00",
        "updatedAt": "2026-04-14T09:30:00"
      }
    ]
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records` | Array | 会话列表 |
| `records[].id` | Long | 会话ID |
| `records[].title` | String | 会话标题 |
| `records[].channelConfigId` | Long | 渠道配置ID |
| `records[].sceneType` | String | 会话场景 |
| `records[].status` | Integer | 状态：0-关闭，1-正常 |
| `records[].lastMessageAt` | DateTime | 最后消息时间 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

---

### 查询 AI 会话详情

**接口信息**
- 路径: `GET /api/user/ai/sessions/{id}`
- 鉴权: 是
- 说明: 查询指定会话的详细信息，包含渠道名称和模型名称

**请求示例**
```javascript
// axios
axios.get('/api/user/ai/sessions/1', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 会话ID |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "title": "Java 学习助手",
    "channelConfigId": 1,
    "sceneType": "general",
    "status": 1,
    "lastMessageAt": "2026-04-15T14:05:00",
    "createdAt": "2026-04-15T14:00:00",
    "updatedAt": "2026-04-15T14:05:00",
    "channelName": "DeepSeek 对话渠道",
    "modelName": "deepseek-chat"
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 会话ID |
| `title` | String | 会话标题 |
| `channelConfigId` | Long | 渠道配置ID |
| `sceneType` | String | 会话场景 |
| `status` | Integer | 状态：0-关闭，1-正常 |
| `lastMessageAt` | DateTime | 最后消息时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |
| `channelName` | String | 渠道名称 |
| `modelName` | String | 模型名称 |

**错误码**
| code | 说明 | 前端处理 |
|-----|------|---------|
| 403 | 无权访问该会话 | 提示用户 |
| 404 | 会话不存在 | 提示会话已删除 |

---

### 分页查询会话消息

**接口信息**
- 路径: `GET /api/user/ai/sessions/{id}/messages`
- 鉴权: 是
- 说明: 分页查询指定会话的历史消息，按时间正序排列

**请求示例**
```javascript
// axios
axios.get('/api/user/ai/sessions/1/messages', {
  params: { current: 1, size: 20 },
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 会话ID |

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 2,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 9,
        "roleType": "user",
        "content": "帮我解释一下 Java 的 Stream API",
        "tokenCount": 12,
        "responseStatus": 1,
        "errorMessage": null,
        "ragReferences": null,
        "createdAt": "2026-04-15T14:04:00"
      },
      {
        "id": 10,
        "roleType": "assistant",
        "content": "Java 中 Stream API 是 Java 8 引入的功能，用于对集合进行函数式操作...",
        "tokenCount": 156,
        "responseStatus": 1,
        "errorMessage": null,
        "ragReferences": [
          {
            "sourceType": "public_article",
            "sourceId": 1001,
            "entryId": 501,
            "title": "Java Stream API 入门",
            "sourceUrl": "/articles/1001",
            "chunkIndex": 0,
            "score": 0.8123
          }
        ],
        "createdAt": "2026-04-15T14:05:00"
      }
    ]
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records` | Array | 消息列表 |
| `records[].id` | Long | 消息ID |
| `records[].roleType` | String | 角色类型：`user`/`assistant`/`system` |
| `records[].content` | String | 消息内容 |
| `records[].tokenCount` | Integer | token 数量 |
| `records[].responseStatus` | Integer | 响应状态：0-失败，1-成功 |
| `records[].errorMessage` | String | 错误信息，失败时返回 |
| `records[].ragReferences` | Array | RAG 引用来源，仅助手消息可能包含 |
| `records[].createdAt` | DateTime | 创建时间 |

**ragReferences 字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `sourceType` | String | 来源类型：`public_article`/`forum_post`/`author_profile`/`admin_entry` |
| `sourceId` | Long | 来源对象ID |
| `entryId` | Long | 知识条目ID |
| `title` | String | 来源标题 |
| `sourceUrl` | String | 来源页面URL |
| `chunkIndex` | Integer | 命中分块序号 |
| `score` | Double | 相似度分数 |

---

### 发送消息

**接口信息**
- 路径: `POST /api/user/ai/sessions/{id}/messages`
- 鉴权: 是
- 说明: 向指定会话发送用户消息

**请求示例**
```javascript
// axios
axios.post('/api/user/ai/sessions/1/messages', {
  content: '帮我解释一下 Java 的 Stream API',
  requestSceneType: 'general',
  requestTargetId: null
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 会话ID |

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `content` | String | 是 | 消息内容，最大 2000 字符 |
| `requestSceneType` | String | 否 | 请求场景类型，默认 `general` |
| `requestTargetId` | Long | 否 | 关联目标ID |
| `attachmentFileIds` | Array\<Long\> | 否 | 附件文件ID列表（目前仅支持图片），最多5个 |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 9,
    "roleType": "user",
    "content": "帮我解释一下 Java 的 Stream API",
    "tokenCount": 12,
    "responseStatus": 1,
    "errorMessage": null,
    "ragReferences": null,
    "attachments": null,
    "createdAt": "2026-04-15T14:04:00"
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 消息ID |
| `roleType` | String | 角色类型：`user`/`assistant`/`system` |
| `content` | String | 消息内容 |
| `tokenCount` | Integer | token 数量 |
| `responseStatus` | Integer | 响应状态：0-失败，1-成功 |
| `errorMessage` | String | 错误信息 |
| `ragReferences` | Array | RAG 引用来源 |
| `attachments` | Array | 附件列表 |
| `createdAt` | DateTime | 创建时间 |

#### attachments 字段说明

| 字段 | 类型 | 说明 |
| ---- | ------ | ---- |
| `fileId` | Long | 文件ID |
| `fileType` | String | 文件类型 |
| `mimeType` | String | MIME 类型 |
| `fileUrl` | String | 文件访问URL |

**错误码**

| code | 说明 | 前端处理 |
| ---- | ------ | -------- |
| 400 | 消息内容为空或超过2000字符 | 提示用户 |
| 403 | 无权访问该会话 | 提示用户 |
| 404 | 会话不存在 | 提示会话已删除 |
| 429 | 配额不足 | 提示配额不足 |

---

### 流式发送消息（SSE）

**接口信息**

- 路径: `POST /api/user/ai/sessions/{id}/messages/stream`
- 鉴权: 是
- Content-Type: `application/json`
- 响应类型: `text/event-stream`
- 说明: 以 SSE 流式方式发送消息，实时接收 AI 响应。请求体与普通发送消息一致。

#### SSE 事件类型

| 事件名 | 说明 | data 格式 |
| ------ | ------ | -------- |
| `delta` | 增量文本片段 | `{"type":"delta","content":"文本"}` |
| `usage` | token 用量统计 | `{"type":"usage","requestTokens":10,"responseTokens":20,"totalTokens":30}` |
| `done` | 流式结束 | `{"type":"done"}` |
| `error` | 错误信息 | `{"type":"error","content":"错误描述"}` |

#### 前端调用示例
```javascript
const evtSource = new EventSourcePolyfill('/api/user/ai/sessions/1/messages/stream', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xxx'
  },
  body: JSON.stringify({
    content: '帮我解释一下 Java 的 Stream API',
    attachmentFileIds: [100, 101]
  })
});

evtSource.addEventListener('delta', (e) => {
  const data = JSON.parse(e.data);
  // 追加 data.content 到消息区域
});

evtSource.addEventListener('usage', (e) => {
  const data = JSON.parse(e.data);
  // 显示 token 用量
});

evtSource.addEventListener('done', () => {
  evtSource.close();
});

evtSource.addEventListener('error', (e) => {
  // 处理错误或关闭
  evtSource.close();
});
```

---

### 关闭会话

**接口信息**
- 路径: `DELETE /api/user/ai/sessions/{id}`
- 鉴权: 是
- 说明: 关闭指定的 AI 对话会话

**请求示例**
```javascript
// axios
axios.delete('/api/user/ai/sessions/1', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 会话ID |

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
|-----|------|---------|
| 403 | 无权关闭该会话 | 提示用户 |
| 404 | 会话不存在 | 提示会话已删除 |

---

### 查询我的 AI 配额

**接口信息**
- 路径: `GET /api/user/ai/sessions/quota`
- 鉴权: 是
- 说明: 查询当前用户在默认渠道的 AI 配额使用情况

**请求示例**
```javascript
// axios
axios.get('/api/user/ai/sessions/quota', {
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
    "dailyLimit": 50,
    "usedToday": 12,
    "remainingToday": 38
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `dailyLimit` | int | 每日限额 |
| `usedToday` | long | 今日已用 |
| `remainingToday` | long | 今日剩余 |

---

## 后台管理

### AI 渠道配置

#### 分页查询渠道配置

**接口信息**
- 路径: `GET /api/sys/ai/channels`
- 鉴权: `ai:channel-config:query`
- 说明: 分页查询所有 AI 渠道配置

**请求示例**
```javascript
// axios
axios.get('/api/sys/ai/channels', {
  params: { current: 1, size: 10 },
  headers: { Authorization: 'Bearer xxx' }
})
```

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

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
        "id": 1,
        "channelCode": "deepseek-chat",
        "channelName": "DeepSeek 对话渠道",
        "dailyQuota": 5000,
        "userDailyQuota": 50,
        "maxContextTokens": 64000,
        "maxInputTokens": 8000,
        "maxHistoryTokens": 16000,
        "maxRagTokens": 4000,
        "maxAttachmentTokens": 4000,
        "maxOutputTokens": 4000,
        "dataScopeJson": "[\"public_article\",\"forum_post\"]",
        "systemPromptTemplate": "你是一个有帮助的AI助手。",
        "status": 1,
        "isDefault": 1,
        "createdBy": 1,
        "updatedBy": 1,
        "createdAt": "2026-04-15T10:00:00",
        "updatedAt": "2026-04-15T10:00:00"
      }
    ]
  }
}
```

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records` | Array | 渠道配置列表 |
| `records[].id` | Long | 渠道配置ID |
| `records[].channelCode` | String | 渠道编码 |
| `records[].channelName` | String | 渠道名称 |
| `records[].dailyQuota` | Integer | 全局每日额度，0表示不限制 |
| `records[].userDailyQuota` | Integer | 单用户每日额度，0表示不限制 |
| `records[].maxContextTokens` | Integer | 上下文长度上限，0表示不限制 |
| `records[].maxInputTokens` | Integer | 单次输入最大 token 预算，null 表示不限 |
| `records[].maxHistoryTokens` | Integer | 历史上下文最大 token 预算，null 表示不限 |
| `records[].maxRagTokens` | Integer | RAG 上下文最大 token 预算，null 表示不限 |
| `records[].maxAttachmentTokens` | Integer | 附件最大 token 预算，null 表示不限 |
| `records[].maxOutputTokens` | Integer | 输出最大 token 预算，null 表示不限 |
| `records[].dataScopeJson` | String | 可读取数据范围配置 JSON |
| `records[].systemPromptTemplate` | String | 系统提示词模板 |
| `records[].status` | Integer | 状态：0-停用，1-启用 |
| `records[].isDefault` | Integer | 是否默认渠道：0-否，1-是 |
| `records[].createdBy` | Long | 创建人ID |
| `records[].updatedBy` | Long | 更新人ID |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

---

#### 查询渠道配置详情

**接口信息**
- 路径: `GET /api/sys/ai/channels/{id}`
- 鉴权: `ai:channel-config:query`
- 说明: 查询指定渠道配置的详细信息

**请求示例**
```javascript
// axios
axios.get('/api/sys/ai/channels/1', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 渠道配置ID |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "channelCode": "deepseek-chat",
    "channelName": "DeepSeek 对话渠道",
    "dailyQuota": 5000,
    "userDailyQuota": 50,
    "maxContextTokens": 64000,
    "maxInputTokens": 8000,
    "maxHistoryTokens": 16000,
    "maxRagTokens": 4000,
    "maxAttachmentTokens": 4000,
    "maxOutputTokens": 4000,
    "dataScopeJson": "[\"public_article\",\"forum_post\"]",
    "systemPromptTemplate": "你是一个有帮助的AI助手。",
    "status": 1,
    "isDefault": 1,
    "createdBy": 1,
    "updatedBy": 1,
    "createdAt": "2026-04-15T10:00:00",
    "updatedAt": "2026-04-15T10:00:00"
  }
}
```

**错误码**
| code | 说明 | 前端处理 |
|-----|------|---------|
| 403 | 无权限 | 提示无权限 |
| 404 | 渠道配置不存在 | 提示配置已删除 |

---

#### 创建渠道配置

**接口信息**
- 路径: `POST /api/sys/ai/channels`
- 鉴权: `ai:channel-config:create`
- 说明: 创建新的 AI 渠道配置

**请求示例**
```javascript
// axios
axios.post('/api/sys/ai/channels', {
  channelCode: 'deepseek-chat',
  channelName: 'DeepSeek 对话渠道',
  dailyQuota: 5000,
  userDailyQuota: 50,
  maxContextTokens: 64000,
  maxInputTokens: 8000,
  maxHistoryTokens: 16000,
  maxRagTokens: 4000,
  maxAttachmentTokens: 4000,
  maxOutputTokens: 4000,
  dataScopeJson: '["public_article","forum_post"]',
  systemPromptTemplate: '你是一个有帮助的AI助手。',
  status: 1,
  isDefault: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `channelCode` | String | 是 | 渠道编码 |
| `channelName` | String | 是 | 渠道名称 |
| `dailyQuota` | Integer | 否 | 全局每日额度，0表示不限制 |
| `userDailyQuota` | Integer | 否 | 单用户每日额度，0表示不限制 |
| `maxContextTokens` | Integer | 否 | 上下文长度上限，0表示不限制 |
| `maxInputTokens` | Integer | 否 | 单次输入最大 token 预算，null 表示不限 |
| `maxHistoryTokens` | Integer | 否 | 历史上下文最大 token 预算，null 表示不限 |
| `maxRagTokens` | Integer | 否 | RAG 上下文最大 token 预算，null 表示不限 |
| `maxAttachmentTokens` | Integer | 否 | 附件最大 token 预算，null 表示不限 |
| `maxOutputTokens` | Integer | 否 | 输出最大 token 预算，null 表示不限 |
| `dataScopeJson` | String | 否 | 可读取数据范围配置 JSON |
| `systemPromptTemplate` | String | 否 | 系统提示词模板 |
| `status` | Integer | 否 | 状态：0-停用，1-启用 |
| `isDefault` | Integer | 否 | 是否默认渠道：0-否，1-是 |
| `mfaTicket` | String | 否 | 二次验证票据（修改高风险字段时必填） |

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "channelCode": "deepseek-chat",
    "channelName": "DeepSeek 对话渠道",
    "dailyQuota": 5000,
    "userDailyQuota": 50,
    "maxContextTokens": 64000,
    "maxInputTokens": 8000,
    "maxHistoryTokens": 16000,
    "maxRagTokens": 4000,
    "maxAttachmentTokens": 4000,
    "maxOutputTokens": 4000,
    "dataScopeJson": "[\"public_article\",\"forum_post\"]",
    "systemPromptTemplate": "你是一个有帮助的AI助手。",
    "status": 1,
    "isDefault": 1,
    "createdBy": 1,
    "updatedBy": null,
    "createdAt": "2026-04-15T10:00:00",
    "updatedAt": "2026-04-15T10:00:00"
  }
}
```

**错误码**
| code | 说明 | 前端处理 |
|-----|------|---------|
| 400 | 参数校验失败 | 提示具体错误 |
| 403 | 无权限 | 提示无权限 |
| 409 | 渠道编码已存在 | 提示更换编码 |

---

#### 更新渠道配置

**接口信息**
- 路径: `PUT /api/sys/ai/channels/{id}`
- 鉴权: `ai:channel-config:update`
- 说明: 更新指定渠道配置的信息

**请求示例**
```javascript
// axios
axios.put('/api/sys/ai/channels/1', {
  channelCode: 'deepseek-chat',
  channelName: 'DeepSeek 对话渠道（更新）',
  dailyQuota: 6000,
  userDailyQuota: 60,
  maxContextTokens: 64000,
  maxInputTokens: 8000,
  maxHistoryTokens: 16000,
  maxRagTokens: 4000,
  maxAttachmentTokens: 4000,
  maxOutputTokens: 4000,
  dataScopeJson: '["public_article","forum_post","author_profile"]',
  systemPromptTemplate: '你是一个专业有帮助的AI助手。',
  status: 1,
  isDefault: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 渠道配置ID |

**请求体字段说明**: 同创建渠道配置

**响应示例**
```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "channelCode": "deepseek-chat",
    "channelName": "DeepSeek 对话渠道（更新）",
    "dailyQuota": 6000,
    "userDailyQuota": 60,
    "maxContextTokens": 64000,
    "maxInputTokens": 8000,
    "maxHistoryTokens": 16000,
    "maxRagTokens": 4000,
    "maxAttachmentTokens": 4000,
    "maxOutputTokens": 4000,
    "dataScopeJson": "[\"public_article\",\"forum_post\",\"author_profile\"]",
    "systemPromptTemplate": "你是一个专业有帮助的AI助手。",
    "status": 1,
    "isDefault": 1,
    "createdBy": 1,
    "updatedBy": 1,
    "createdAt": "2026-04-15T10:00:00",
    "updatedAt": "2026-04-15T15:30:00"
  }
}
```

**错误码**
| code | 说明 | 前端处理 |
|-----|------|---------|
| 400 | 参数校验失败 | 提示具体错误 |
| 403 | 无权限 | 提示无权限 |
| 404 | 渠道配置不存在 | 提示配置已删除 |
| 409 | 渠道编码已存在 | 提示更换编码 |

---

#### 更新渠道状态

**接口信息**
- 路径: `PUT /api/sys/ai/channels/{id}/status`
- 鉴权: `ai:channel-config:update`
- 说明: 启用或停用指定渠道配置

**请求示例**
```javascript
// axios
axios.put('/api/sys/ai/channels/1/status', {
  status: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 渠道配置ID |

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态：0-停用，1-启用 |

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
|-----|------|---------|
| 400 | 状态值无效 | 提示状态值范围 |
| 403 | 无权限 | 提示无权限 |
| 404 | 渠道配置不存在 | 提示配置已删除 |

---

#### 删除渠道配置

**接口信息**
- 路径: `DELETE /api/sys/ai/channels/{id}`
- 鉴权: `ai:channel-config:delete`
- 说明: 删除指定渠道配置

**请求示例**
```javascript
// axios
axios.delete('/api/sys/ai/channels/1', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 渠道配置ID |

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
|-----|------|---------|
| 403 | 无权限 | 提示无权限 |
| 404 | 渠道配置不存在 | 提示配置已删除 |
| 409 | 渠道正在使用中 | 提示无法删除 |

---

### AI 会话后台管理

#### 分页查询用户会话

**接口信息**
- 路径: `GET /api/sys/ai/sessions`
- 鉴权: `ai:session:query`
- 说明: 后台分页查询所有用户的 AI 会话

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |
| `userId` | Long | 否 | 用户ID |
| `status` | Integer | 否 | 会话状态：0-关闭，1-正常 |
| `channelConfigId` | Long | 否 | 渠道配置ID |
| `startTime` | DateTime | 否 | 创建开始时间 |
| `endTime` | DateTime | 否 | 创建结束时间 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 会话ID |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `nickname` | String | 用户昵称 |
| `channelConfigId` | Long | 渠道配置ID |
| `channelName` | String | 渠道名称 |
| `title` | String | 会话标题 |
| `sceneType` | String | 场景类型 |
| `status` | Integer | 状态：0-关闭，1-正常 |
| `lastMessageAt` | DateTime | 最后消息时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询会话详情（后台）

**接口信息**
- 路径: `GET /api/sys/ai/sessions/{id}`
- 鉴权: `ai:session:query`
- 说明: 后台查询指定会话详情

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `id` | Long | 是 | 会话ID |

---

### AI 调用统计

#### 分页查询使用日志

**接口信息**
- 路径: `GET /api/sys/ai/usage-logs`
- 鉴权: `ai:usage-stats:query`
- 说明: 分页查询 AI 调用日志

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |
| `userId` | Long | 否 | 用户ID |
| `channelConfigId` | Long | 否 | 渠道配置ID |
| `startTime` | DateTime | 否 | 开始时间 |
| `endTime` | DateTime | 否 | 结束时间 |
| `successStatus` | Integer | 否 | 成功状态：0-失败，1-成功 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 日志ID |
| `userId` | Long | 用户ID |
| `channelConfigId` | Long | 渠道配置ID |
| `sessionId` | Long | 会话ID |
| `requestSceneType` | String | 请求场景类型 |
| `requestTokens` | Integer | 请求 token 数 |
| `responseTokens` | Integer | 响应 token 数 |
| `totalTokens` | Integer | 总 token 数 |
| `quotaCost` | Integer | 额度消耗 |
| `successStatus` | Integer | 成功状态：0-失败，1-成功 |
| `errorCode` | String | 错误码 |
| `ragEnabled` | Integer | 是否启用 RAG：0-否，1-是 |
| `ragHitCount` | Integer | RAG 命中数量 |
| `ragDurationMs` | Long | RAG 检索耗时（毫秒） |
| `ragReferences` | Array | RAG 引用来源（同消息中的 ragReferences 结构） |
| `createdAt` | DateTime | 创建时间 |

---

#### 获取使用统计

**接口信息**
- 路径: `GET /api/sys/ai/usage-logs/stats`
- 鉴权: `ai:usage-stats:query`
- 说明: 获取 AI 调用汇总统计

**查询参数说明**: 同分页查询使用日志的筛选参数

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `totalCalls` | long | 总调用次数 |
| `successCalls` | long | 成功调用次数 |
| `failedCalls` | long | 失败调用次数 |
| `totalTokens` | long | 总 token 数 |
| `totalQuotaCost` | long | 总额度消耗 |

---

### AI 渠道账号池管理

> provider / modelName / apiBaseUrl / apiKey 已下沉到账号级别，一个渠道可配置多个账号。

#### 分页查询渠道账号列表

**接口信息**
- 路径: `GET /api/sys/ai/channels/{channelId}/accounts`
- 鉴权: `ai:channel-account:query`
- 说明: 分页查询指定渠道下的账号列表

**路径参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `channelId` | Long | 是 | 渠道配置ID |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 账号ID |
| `channelConfigId` | Long | 所属渠道配置ID |
| `accountName` | String | 账号名称（备注） |
| `provider` | String | 提供方（deepseek/openai/zhipu 等） |
| `modelName` | String | 模型名称 |
| `apiBaseUrl` | String | 接口基础地址 |
| `apiKeyEncrypted` | String | API Key（脱敏） |
| `weight` | Integer | 权重，默认 1 |
| `status` | Integer | 状态：0-停用，1-启用 |
| `dailyQuota` | Integer | 每日额度，0-不限 |
| `consecutiveErrors` | Integer | 连续错误次数 |
| `maxConsecutiveErrors` | Integer | 最大连续错误次数，默认 5 |
| `lastErrorAt` | DateTime | 最近错误时间 |
| `lastErrorMessage` | String | 最近错误信息 |
| `disabledAt` | DateTime | 自动禁用时间 |
| `autoRecoverAt` | DateTime | 计划自动恢复时间 |
| `totalCallCount` | Long | 累计调用次数 |
| `lastUsedAt` | DateTime | 最近使用时间 |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询渠道账号详情

**接口信息**
- 路径: `GET /api/sys/ai/channels/{channelId}/accounts/{id}`
- 鉴权: `ai:channel-account:query`

---

#### 创建渠道账号

**接口信息**
- 路径: `POST /api/sys/ai/channels/{channelId}/accounts`
- 鉴权: `ai:channel-account:create`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `accountName` | String | 是 | 账号名称（备注） |
| `provider` | String | 是 | 提供方（deepseek/openai/zhipu 等） |
| `modelName` | String | 是 | 模型名称 |
| `apiBaseUrl` | String | 是 | 接口基础地址 |
| `apiKeyEncrypted` | String | 是 | API Key |
| `weight` | Integer | 否 | 权重，默认 1 |
| `status` | Integer | 否 | 状态：0-停用，1-启用 |
| `dailyQuota` | Integer | 否 | 每日额度，0-不限 |
| `maxConsecutiveErrors` | Integer | 否 | 最大连续错误次数，默认 5 |
| `mfaTicket` | String | 否 | 二次验证票据（修改 API Key 时必填） |

---

#### 更新渠道账号

**接口信息**
- 路径: `PUT /api/sys/ai/channels/{channelId}/accounts/{id}`
- 鉴权: `ai:channel-account:update`
- 请求体字段: 同创建渠道账号

---

#### 更新渠道账号状态

**接口信息**
- 路径: `PUT /api/sys/ai/channels/{channelId}/accounts/{id}/status`
- 鉴权: `ai:channel-account:update`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态：0-停用，1-启用 |

---

#### 删除渠道账号

**接口信息**
- 路径: `DELETE /api/sys/ai/channels/{channelId}/accounts/{id}`
- 鉴权: `ai:channel-account:delete`

---

### AI 知识源配置

#### 查询所有知识源配置

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/source-config`
- 鉴权: `ai:knowledge:query`
- 说明: 查询所有知识源配置（返回列表，非分页）

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 配置ID |
| `sourceType` | String | 知识源类型编码 |
| `enabled` | Integer | 是否启用：0-禁用，1-启用 |
| `syncInterval` | Integer | 同步间隔（秒） |
| `lastSyncedAt` | DateTime | 最近一次同步完成时间 |
| `lastSyncStatus` | String | 最近同步状态 |
| `configJson` | String | 扩展配置 JSON |
| `updatedBy` | Long | 更新人ID |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询知识源配置详情

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/source-config/{id}`
- 鉴权: `ai:knowledge:query`

---

#### 更新知识源配置

**接口信息**
- 路径: `PUT /api/sys/ai/knowledge/source-config/{id}`
- 鉴权: `ai:knowledge:update`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `syncInterval` | Integer | 是 | 同步间隔（秒） |
| `configJson` | String | 否 | 扩展配置 JSON |
| `remark` | String | 否 | 备注 |

---

#### 切换知识源启停状态

**接口信息**
- 路径: `PUT /api/sys/ai/knowledge/source-config/{id}/toggle`
- 鉴权: `ai:knowledge:update`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `enabled` | Integer | 是 | 0-禁用，1-启用 |

---

### AI 知识条目管理

#### 分页查询知识条目

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/entries`
- 鉴权: `ai:knowledge:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `sourceType` | String | 否 | 来源类型 |
| `status` | Integer | 否 | 状态：0-禁用，1-正常，2-过期，3-已删除 |
| `keyword` | String | 否 | 标题关键词 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 条目ID |
| `sourceType` | String | 来源类型 |
| `sourceId` | Long | 来源对象ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `sourceUrl` | String | 来源页面 URL |
| `authorId` | Long | 原始作者ID |
| `status` | Integer | 状态：0-禁用，1-正常，2-过期，3-已删除 |
| `version` | Integer | 版本号 |
| `chunkCount` | Integer | 分块数量 |
| `sourceUpdatedAt` | DateTime | 源内容最后更新时间 |
| `syncedAt` | DateTime | 最近同步时间 |
| `tagJson` | String | 标签 JSON |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询知识条目详情

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/entries/{id}`
- 鉴权: `ai:knowledge:query`

---

#### 更新知识条目状态

**接口信息**
- 路径: `PUT /api/sys/ai/knowledge/entries/{id}/status`
- 鉴权: `ai:knowledge:update`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态值：0-禁用，1-正常，2-过期，3-已删除 |

---

#### 触发知识同步任务

**接口信息**
- 路径: `POST /api/sys/ai/knowledge/entries/sync`
- 鉴权: `ai:knowledge:sync`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `sourceType` | String | 是 | 知识源类型 |
| `taskType` | String | 否 | 任务类型，默认 `full_sync` |
| `sourceId` | Long | 否 | 来源对象ID（single_entry 时必填） |
| `remark` | String | 否 | 备注 |

---

#### 分页查询同步任务

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/entries/sync/tasks`
- 鉴权: `ai:knowledge:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `sourceType` | String | 否 | 知识源类型 |
| `status` | Integer | 否 | 状态：0-待执行，1-执行中，2-已完成，3-失败 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 任务ID |
| `taskType` | String | 任务类型 |
| `sourceType` | String | 知识源类型 |
| `status` | Integer | 状态：0-待执行，1-执行中，2-已完成，3-失败 |
| `totalCount` | Integer | 总条目数 |
| `successCount` | Integer | 成功条目数 |
| `failCount` | Integer | 失败条目数 |
| `skipCount` | Integer | 跳过条目数 |
| `errorMessage` | String | 错误信息 |
| `retryCount` | Integer | 已重试次数 |
| `maxRetry` | Integer | 最大重试次数 |
| `startedAt` | DateTime | 开始执行时间 |
| `completedAt` | DateTime | 执行完成时间 |
| `triggeredBy` | String | 触发方式 |
| `operatorId` | Long | 操作人ID |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询同步任务详情

**接口信息**
- 路径: `GET /api/sys/ai/knowledge/entries/sync/tasks/{taskId}`
- 鉴权: `ai:knowledge:query`

---

#### 重试失败的同步任务

**接口信息**
- 路径: `POST /api/sys/ai/knowledge/entries/sync/tasks/{taskId}/retry`
- 鉴权: `ai:knowledge:sync`

---

### AI Agent 定义管理

#### 分页查询 Agent 定义

**接口信息**
- 路径: `GET /api/sys/ai/agents/definitions`
- 鉴权: `ai:agent:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `keyword` | String | 否 | 名称关键词 |
| `enabled` | Integer | 否 | 启用状态：0-停用，1-启用 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | Agent ID |
| `name` | String | Agent 名称 |
| `description` | String | Agent 描述 |
| `systemPrompt` | String | 系统提示词 |
| `channelConfigId` | Long | 关联 AI 渠道配置 ID |
| `dataScopeJson` | String | 数据读取范围配置 JSON |
| `enabled` | Integer | 0-停用，1-启用 |
| `maxTurns` | Integer | 最大对话轮次 |
| `extraConfigJson` | String | 扩展配置 JSON |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询 Agent 定义详情

**接口信息**
- 路径: `GET /api/sys/ai/agents/definitions/{id}`
- 鉴权: `ai:agent:query`

---

#### 创建 Agent 定义

**接口信息**
- 路径: `POST /api/sys/ai/agents/definitions`
- 鉴权: `ai:agent:create`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `name` | String | 是 | Agent 名称（最多 64 字符） |
| `description` | String | 否 | Agent 描述（最多 512 字符） |
| `systemPrompt` | String | 是 | 系统提示词 |
| `channelConfigId` | Long | 是 | 关联 AI 渠道配置 ID |
| `dataScopeJson` | String | 否 | 数据读取范围配置 JSON |
| `maxTurns` | Integer | 否 | 最大对话轮次，默认 1 |
| `extraConfigJson` | String | 否 | 扩展配置 JSON |

---

#### 更新 Agent 定义

**接口信息**
- 路径: `PUT /api/sys/ai/agents/definitions/{id}`
- 鉴权: `ai:agent:update`
- 请求体字段: 同创建 Agent 定义

---

#### 切换 Agent 启停状态

**接口信息**
- 路径: `PUT /api/sys/ai/agents/definitions/{id}/toggle`
- 鉴权: `ai:agent:update`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `enabled` | Integer | 是 | 0-停用，1-启用 |

---

#### 删除 Agent 定义

**接口信息**
- 路径: `DELETE /api/sys/ai/agents/definitions/{id}`
- 鉴权: `ai:agent:delete`

---

### AI Agent 任务后台管理

#### 分页查询 Agent 任务（后台）

**接口信息**
- 路径: `GET /api/sys/ai/agents/tasks`
- 鉴权: `ai:agent:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `agentId` | Long | 否 | Agent 定义 ID |
| `status` | Integer | 否 | 状态：0-待执行，1-执行中，2-已完成，3-失败，4-已取消 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 任务ID |
| `userId` | Long | 发起用户ID |
| `agentId` | Long | 关联 Agent 定义 ID |
| `agentName` | String | Agent 名称 |
| `status` | Integer | 状态：0-待执行，1-执行中，2-已完成，3-失败，4-已取消 |
| `inputContent` | String | 用户输入 |
| `outputContent` | String | Agent 输出 |
| `errorMessage` | String | 错误信息 |
| `tokenCount` | Integer | 消耗 token 数 |
| `startedAt` | DateTime | 开始时间 |
| `completedAt` | DateTime | 完成时间 |
| `createdAt` | DateTime | 创建时间 |

---

#### 查询 Agent 任务详情（后台）

**接口信息**
- 路径: `GET /api/sys/ai/agents/tasks/{id}`
- 鉴权: `ai:agent:query`

---

### AI Agent 用户任务

#### 发起 Agent 任务

**接口信息**
- 路径: `POST /api/user/ai/agents/tasks`
- 鉴权: 是
- 说明: 用户发起 Agent 任务

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `agentId` | Long | 是 | Agent 定义 ID |
| `inputContent` | String | 是 | 用户输入内容 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 任务ID |
| `agentId` | Long | Agent 定义 ID |
| `agentName` | String | Agent 名称 |
| `status` | Integer | 状态：0-待执行，1-执行中，2-已完成，3-失败，4-已取消 |
| `inputContent` | String | 用户输入 |
| `outputContent` | String | Agent 输出 |
| `errorMessage` | String | 错误信息 |
| `tokenCount` | Integer | 消耗 token 数 |
| `createdAt` | DateTime | 创建时间 |
| `completedAt` | DateTime | 完成时间 |

---

#### 分页查询我的 Agent 任务

**接口信息**
- 路径: `GET /api/user/ai/agents/tasks`
- 鉴权: 是

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `status` | Integer | 否 | 状态：0-待执行，1-执行中，2-已完成，3-失败，4-已取消 |

---

#### 查询 Agent 任务详情（用户）

**接口信息**
- 路径: `GET /api/user/ai/agents/tasks/{id}`
- 鉴权: 是

---

#### 取消 Agent 任务

**接口信息**
- 路径: `PUT /api/user/ai/agents/tasks/{id}/cancel`
- 鉴权: 是

---

### AI 工具管理

#### 分页查询工具定义

**接口信息**
- 路径: `GET /api/sys/ai/tools`
- 鉴权: `ai:tool:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `toolCode` | String | 否 | 工具编码 |
| `toolName` | String | 否 | 工具名称 |
| `sourceType` | String | 否 | 来源类型 builtin/mcp |
| `enabled` | Integer | 否 | 启用状态：0-停用，1-启用 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `toolCode` | String | 工具编码 |
| `toolName` | String | 工具名称 |
| `sourceType` | String | 来源类型 |
| `mcpServerId` | Long | MCP 服务 ID |
| `mcpToolName` | String | MCP 原始工具名 |
| `description` | String | 描述 |
| `parametersSchema` | String | 参数 Schema |
| `resultSchema` | String | 返回 Schema |
| `riskLevel` | String | 风险等级 low/medium/high |
| `useScenarios` | String | 适用场景 |
| `enabled` | Integer | 启用状态 |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询工具详情

**接口信息**
- 路径: `GET /api/sys/ai/tools/{id}`
- 鉴权: `ai:tool:query`

---

#### 创建工具定义

**接口信息**
- 路径: `POST /api/sys/ai/tools`
- 鉴权: `ai:tool:create`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `toolCode` | String | 是 | 工具编码（最多 64 字符） |
| `toolName` | String | 是 | 工具名称（最多 128 字符） |
| `sourceType` | String | 是 | 来源类型 builtin/mcp |
| `mcpServerId` | Long | 否 | MCP 服务 ID |
| `mcpToolName` | String | 否 | MCP 原始工具名 |
| `description` | String | 否 | 工具描述 |
| `parametersSchema` | String | 否 | 参数 Schema JSON |
| `resultSchema` | String | 否 | 返回 Schema JSON |
| `riskLevel` | String | 是 | 风险等级 low/medium/high |
| `useScenarios` | String | 否 | 适用场景 JSON 数组 |
| `enabled` | Integer | 是 | 启用状态：0-停用，1-启用 |
| `mfaTicket` | String | 否 | MFA 票据 |

---

#### 更新工具定义

**接口信息**
- 路径: `PUT /api/sys/ai/tools/{id}`
- 鉴权: `ai:tool:update`
- 请求体字段: 同创建工具定义

---

#### 更新工具状态

**接口信息**
- 路径: `PUT /api/sys/ai/tools/{id}/status`
- 鉴权: `ai:tool:update`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `enabled` | Integer | 是 | 0-停用，1-启用 |

---

#### 删除工具定义

**接口信息**
- 路径: `DELETE /api/sys/ai/tools/{id}`
- 鉴权: `ai:tool:delete`

---

#### 后台测试执行工具

**接口信息**
- 路径: `POST /api/sys/ai/tools/{id}/execute`
- 鉴权: `ai:tool:execute`
- 说明: 在后台测试执行指定工具

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `toolCode` | String | 否 | 工具编码 |
| `arguments` | String | 否 | 工具参数 JSON |
| `agentId` | Long | 否 | Agent ID |
| `sessionId` | Long | 否 | 会话 ID |
| `taskId` | Long | 否 | 任务 ID |
| `sceneType` | String | 否 | 场景类型 |
| `dataScope` | String | 否 | 数据范围 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `success` | Boolean | 是否成功 |
| `resultText` | String | 结果文本 |
| `errorMessage` | String | 错误信息 |
| `elapsedMs` | Long | 耗时（毫秒） |
| `callLogId` | Long | 调用日志 ID |

---

#### 分页查询工具调用日志

**接口信息**
- 路径: `GET /api/sys/ai/tools/call-logs`
- 鉴权: `ai:tool:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `toolId` | Long | 否 | 工具 ID |
| `userId` | Long | 否 | 用户 ID |
| `agentId` | Long | 否 | Agent ID |
| `taskId` | Long | 否 | 任务 ID |
| `successStatus` | Integer | 否 | 成功状态：0-失败，1-成功 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `userId` | Long | 用户 ID |
| `agentId` | Long | Agent ID |
| `sessionId` | Long | 会话 ID |
| `taskId` | Long | 任务 ID |
| `toolId` | Long | 工具 ID |
| `toolCode` | String | 工具编码 |
| `toolName` | String | 工具名称 |
| `requestSceneType` | String | 请求场景 |
| `requestSummary` | String | 入参摘要 |
| `responseSummary` | String | 结果摘要 |
| `successStatus` | Integer | 成功状态 |
| `elapsedMs` | Long | 耗时（毫秒） |
| `errorMessage` | String | 错误信息 |
| `createdAt` | DateTime | 创建时间 |

---

#### 分页查询工具授权

**接口信息**
- 路径: `GET /api/sys/ai/tools/authorizations`
- 鉴权: `ai:tool:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `toolId` | Long | 否 | 工具 ID |
| `authorizationType` | String | 否 | 授权类型 |
| `authorizationKey` | String | 否 | 授权键 |
| `enabled` | Integer | 否 | 启用状态 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `toolId` | Long | 工具 ID |
| `authorizationType` | String | 授权类型 agent/scene/permission/data_scope |
| `authorizationKey` | String | 授权键 |
| `dataScope` | String | 数据范围 |
| `enabled` | Integer | 启用状态 |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 创建工具授权

**接口信息**
- 路径: `POST /api/sys/ai/tools/authorizations`
- 鉴权: `ai:tool:update`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `toolId` | Long | 是 | 工具 ID |
| `authorizationType` | String | 是 | 授权类型 agent/scene/permission/data_scope |
| `authorizationKey` | String | 是 | 授权键（最多 128 字符） |
| `dataScope` | String | 否 | 数据范围 |
| `enabled` | Integer | 是 | 启用状态：0-停用，1-启用 |

---

#### 更新工具授权

**接口信息**
- 路径: `PUT /api/sys/ai/tools/authorizations/{id}`
- 鉴权: `ai:tool:update`
- 请求体字段: 同创建工具授权

---

#### 删除工具授权

**接口信息**
- 路径: `DELETE /api/sys/ai/tools/authorizations/{id}`
- 鉴权: `ai:tool:update`

---

### AI MCP 服务管理

#### 分页查询 MCP 服务

**接口信息**
- 路径: `GET /api/sys/ai/mcp-servers`
- 鉴权: `ai:mcp:query`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `serverName` | String | 否 | 服务名称 |
| `transportType` | String | 否 | 传输类型 stdio/http |
| `enabled` | Integer | 否 | 启用状态 |

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `serverName` | String | 服务名称 |
| `transportType` | String | 传输类型 |
| `connectionConfigJson` | String | 连接配置 JSON |
| `timeoutSeconds` | Integer | 超时时间（秒） |
| `enabled` | Integer | 启用状态 |
| `lastHealthStatus` | String | 最近健康状态 |
| `lastDiscoveredAt` | DateTime | 最近发现时间 |
| `lastErrorSummary` | String | 最近错误摘要 |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询 MCP 服务详情

**接口信息**
- 路径: `GET /api/sys/ai/mcp-servers/{id}`
- 鉴权: `ai:mcp:query`

---

#### 创建 MCP 服务

**接口信息**
- 路径: `POST /api/sys/ai/mcp-servers`
- 鉴权: `ai:mcp:create`

**请求体字段说明**
| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `serverName` | String | 是 | 服务名称（最多 128 字符） |
| `transportType` | String | 是 | 传输类型 stdio/http |
| `connectionConfigJson` | String | 是 | 连接配置 JSON |
| `authConfigJson` | String | 否 | 鉴权配置 JSON |
| `timeoutSeconds` | Integer | 是 | 超时时间（秒） |
| `enabled` | Integer | 是 | 启用状态：0-停用，1-启用 |
| `mfaTicket` | String | 否 | MFA 票据 |

---

#### 更新 MCP 服务

**接口信息**
- 路径: `PUT /api/sys/ai/mcp-servers/{id}`
- 鉴权: `ai:mcp:update`
- 请求体字段: 同创建 MCP 服务

---

#### 更新 MCP 服务状态

**接口信息**
- 路径: `PUT /api/sys/ai/mcp-servers/{id}/status`
- 鉴权: `ai:mcp:update`

**查询参数说明**
| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `enabled` | Integer | 是 | 0-停用，1-启用 |

---

#### 删除 MCP 服务

**接口信息**
- 路径: `DELETE /api/sys/ai/mcp-servers/{id}`
- 鉴权: `ai:mcp:delete`

---

#### 发现 MCP 工具

**接口信息**
- 路径: `POST /api/sys/ai/mcp-servers/{id}/discover`
- 鉴权: `ai:mcp:discover`
- 说明: 连接 MCP 服务并发现可用工具

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `discoveredCount` | Integer | 发现工具数量 |
| `syncedCount` | Integer | 同步后的工具数量 |

---

#### 查询 MCP 工具快照

**接口信息**
- 路径: `GET /api/sys/ai/mcp-servers/{id}/tools`
- 鉴权: `ai:mcp:query`
- 说明: 查询指定 MCP 服务的工具快照（返回列表，非分页）

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `mcpServerId` | Long | MCP 服务 ID |
| `mcpToolName` | String | MCP 原始工具名 |
| `toolCode` | String | 工具编码 |
| `toolName` | String | 工具名称 |
| `description` | String | 描述 |
| `parametersSchema` | String | 参数 Schema |
| `resultSchema` | String | 返回 Schema |
| `riskLevel` | String | 风险等级 |
| `useScenarios` | String | 适用场景 |
| `enabled` | Integer | 启用状态 |
| `discoveredAt` | DateTime | 发现时间 |
| `rawDefinitionJson` | String | 原始定义 JSON |
| `lastErrorSummary` | String | 错误摘要 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询 MCP 连接状态

**接口信息**
- 路径: `GET /api/sys/ai/mcp-servers/{id}/health`
- 鉴权: `ai:mcp:query`

**响应字段说明**
| 字段 | 类型 | 说明 |
|-----|------|-----|
| `healthy` | Boolean | 是否健康 |
| `status` | String | 状态 |
| `errorSummary` | String | 错误摘要 |

---

## 枚举值说明

### 渠道状态 (AiChannelStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 停用 |
| 1 | 启用 |

### 会话状态 (AiChatSessionStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 关闭 |
| 1 | 正常 |

### 消息角色类型

| 值 | 说明 |
|---|---|
| `user` | 用户消息 |
| `assistant` | 助手消息 |
| `system` | 系统消息 |

### 消息响应状态 (AiMessageResponseStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 失败 |
| 1 | 成功 |

### 知识源类型 (AiKnowledgeSourceTypeEnum)

| 值 | 说明 |
|---|---|
| `public_article` | 公开文章 |
| `author_profile` | 作者公开资料 |
| `forum_post` | 论坛帖子 |
| `admin_entry` | 管理员维护知识条目 |

### 知识条目状态 (AiKnowledgeEntryStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 禁用 |
| 1 | 正常 |
| 2 | 过期 |
| 3 | 已删除 |

### 同步任务状态 (AiKnowledgeSyncTaskStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 待执行 |
| 1 | 执行中 |
| 2 | 已完成 |
| 3 | 失败 |

### Agent 任务状态 (AiAgentTaskStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 待执行 |
| 1 | 执行中 |
| 2 | 已完成 |
| 3 | 失败 |
| 4 | 已取消 |

### 是否默认渠道

| 值 | 说明 |
|---|---|
| 0 | 否 |
| 1 | 是 |

### 调用成功状态 (AiUsageSuccessStatusEnum)

| 值 | 说明 |
|---|---|
| 0 | 失败 |
| 1 | 成功 |

---

## 统一响应格式

所有接口均返回以下统一响应格式：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `code` | Integer | 状态码，200表示成功 |
| `message` | String | 响应消息 |
| `timestamp` | Long | 响应时间戳（毫秒） |
| `data` | Object/Array/Null | 响应数据，分页查询时为分页对象 |

### 分页响应格式

分页查询接口的 `data` 字段为分页对象：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": [ ... ]
  }
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records` | Array | 当前页数据列表 |

---

## 错误码说明

| code | 说明 | 处理建议 |
|-----|------|---------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未登录 | 跳转登录页 |
| 403 | 无权限 | 提示用户或跳转授权页 |
| 404 | 资源不存在 | 提示用户资源已删除 |
| 409 | 业务冲突 | 根据具体业务提示用户 |
| 429 | 请求过于频繁/配额不足 | 提示用户稍后重试或配额信息 |
| 500 | 服务器错误 | 提示用户稍后重试 |
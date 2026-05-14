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
        "provider": "deepseek",
        "modelName": "deepseek-chat",
        "apiBaseUrl": "https://api.deepseek.com/v1",
        "apiKeyEncrypted": "******",
        "dailyQuota": 5000,
        "userDailyQuota": 50,
        "maxContextTokens": 64000,
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
| `records[].provider` | String | 提供方 |
| `records[].modelName` | String | 模型名称 |
| `records[].apiBaseUrl` | String | 接口基础地址 |
| `records[].apiKeyEncrypted` | String | 加密后的 API Key（脱敏） |
| `records[].dailyQuota` | Integer | 全局每日额度，0表示不限制 |
| `records[].userDailyQuota` | Integer | 单用户每日额度，0表示不限制 |
| `records[].maxContextTokens` | Integer | 上下文长度上限，0表示不限制 |
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
    "provider": "deepseek",
    "modelName": "deepseek-chat",
    "apiBaseUrl": "https://api.deepseek.com/v1",
    "apiKeyEncrypted": "******",
    "dailyQuota": 5000,
    "userDailyQuota": 50,
    "maxContextTokens": 64000,
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
  provider: 'deepseek',
  modelName: 'deepseek-chat',
  apiBaseUrl: 'https://api.deepseek.com/v1',
  apiKeyEncrypted: 'sk-xxxxxxx',
  dailyQuota: 5000,
  userDailyQuota: 50,
  maxContextTokens: 64000,
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
| `provider` | String | 是 | 提供方 |
| `modelName` | String | 是 | 模型名称 |
| `apiBaseUrl` | String | 否 | 接口基础地址 |
| `apiKeyEncrypted` | String | 否 | API Key（加密存储） |
| `dailyQuota` | Integer | 否 | 全局每日额度，0表示不限制 |
| `userDailyQuota` | Integer | 否 | 单用户每日额度，0表示不限制 |
| `maxContextTokens` | Integer | 否 | 上下文长度上限，0表示不限制 |
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
    "provider": "deepseek",
    "modelName": "deepseek-chat",
    "apiBaseUrl": "https://api.deepseek.com/v1",
    "apiKeyEncrypted": "******",
    "dailyQuota": 5000,
    "userDailyQuota": 50,
    "maxContextTokens": 64000,
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
  provider: 'deepseek',
  modelName: 'deepseek-chat',
  apiBaseUrl: 'https://api.deepseek.com/v1',
  dailyQuota: 6000,
  userDailyQuota: 60,
  maxContextTokens: 64000,
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
    "provider": "deepseek",
    "modelName": "deepseek-chat",
    "apiBaseUrl": "https://api.deepseek.com/v1",
    "apiKeyEncrypted": "******",
    "dailyQuota": 6000,
    "userDailyQuota": 60,
    "maxContextTokens": 64000,
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
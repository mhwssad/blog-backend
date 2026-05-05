# AI 模块接口文档

本文档面向前端联调，对应项目中 AI 模块的实现。

## 1. 当前能力范围

当前已支持：

- AI 渠道配置管理（多渠道支持）
- 用户 AI 对话会话（创建、查询、关闭）
- 消息收发（流式/非流式）
- 用户配额管理（每日限额）
- 后台会话查询与统计
- AI 调用日志与使用统计
- 知识源配置管理（启停、同步间隔）
- 知识条目管理（查询、状态变更）
- 知识同步任务管理（触发、重试、查询）
- Agent 定义后台管理（创建、编辑、启停、删除）
- Agent 任务用户侧（发起、查询、取消）
- Agent 任务后台管理（分页查询、详情）
- AI 工具定义、授权、执行测试和调用日志
- MCP 服务配置、工具发现、连接健康检查和工具快照

## 2. 鉴权要求

### 2.1 用户侧接口

用户侧接口统一走 `/api/user/ai/**`，要求登录：

```http
Authorization: Bearer <accessToken>
```

### 2.2 后台管理接口

后台管理接口统一走 `/api/sys/ai/**`，除登录外还要求对应权限：

| 权限标识 | 说明 |
| --- | --- |
| `ai:channel-config:query` | 查询渠道配置 |
| `ai:channel-config:create` | 创建渠道配置 |
| `ai:channel-config:update` | 更新渠道配置 |
| `ai:channel-config:delete` | 删除渠道配置 |
| `ai:session:query` | 查询用户会话 |
| `ai:usage-stats:query` | 查询使用统计 |
| `ai:tool:query` | 查询工具定义、授权和调用日志 |
| `ai:tool:create` | 创建工具定义 |
| `ai:tool:update` | 更新工具定义、启停和授权关系 |
| `ai:tool:delete` | 删除工具定义 |
| `ai:tool:execute` | 后台测试执行工具 |
| `ai:mcp:query` | 查询 MCP 服务、工具快照和健康状态 |
| `ai:mcp:create` | 创建 MCP 服务 |
| `ai:mcp:update` | 更新 MCP 服务和启停 |
| `ai:mcp:delete` | 删除 MCP 服务 |
| `ai:mcp:discover` | 发现 MCP 工具 |

## 3. 用户侧接口

### 3.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 创建AI会话 | POST | `/api/user/ai/sessions` | 创建新会话 |
| 查询我的AI会话列表 | GET | `/api/user/ai/sessions` | 分页查询会话 |
| 查询AI会话详情 | GET | `/api/user/ai/sessions/{id}` | 获取会话详情 |
| 分页查询会话消息 | GET | `/api/user/ai/sessions/{id}/messages` | 获取历史消息 |
| 发送消息 | POST | `/api/user/ai/sessions/{id}/messages` | 发送用户消息 |
| 关闭会话 | DELETE | `/api/user/ai/sessions/{id}` | 删除会话 |
| 查询我的AI配额 | GET | `/api/user/ai/sessions/quota` | 获取当前配额 |
| 发起 Agent 任务 | POST | `/api/user/ai/agents/tasks` | 发起 agent 任务 |
| 查询我的 Agent 任务 | GET | `/api/user/ai/agents/tasks` | 分页查询任务 |
| 查询 Agent 任务详情 | GET | `/api/user/ai/agents/tasks/{id}` | 获取任务详情 |
| 取消 Agent 任务 | PUT | `/api/user/ai/agents/tasks/{id}/cancel` | 取消待执行任务 |

> **Agent 任务通知行为**：发起任务后，系统在任务执行完成（成功或失败）时向用户投递通知。通知遵守用户 `ai_task_done` 偏好开关，携带跳转元数据（`businessType=ai_agent_task`、`businessId`、`actionPath=/ai/agents/tasks/{taskId}`），前端可据此跳转到任务详情页。

### 3.2 创建AI会话

- 请求：`POST /api/user/ai/sessions`
- 鉴权：是
- 请求体：`AiSessionCreateRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `channelConfigId` | Long | 否 | 渠道配置ID，不填则使用默认渠道 |
| `title` | String | 否 | 会话标题 |
| `sceneType` | String | 否 | 会话场景，默认 `general` |

- 响应：`Result<AiSessionVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |
| `title` | String | 会话标题 |
| `channelConfigId` | Long | 渠道配置ID |
| `sceneType` | String | 会话场景 |
| `status` | Integer | 状态：0-关闭/1-正常 |
| `lastMessageAt` | DateTime | 最后消息时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

- 响应示例：

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
    "createdAt": "2026-04-15 14:00:00",
    "updatedAt": "2026-04-15 14:00:00"
  }
}
```

### 3.3 查询我的AI会话列表

- 请求：`GET /api/user/ai/sessions`
- 鉴权：是
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`PageResult<AiSessionVO>`

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
        "id": 1,
        "title": "Java 学习助手",
        "channelConfigId": 1,
        "sceneType": "general",
        "status": 1,
        "lastMessageAt": "2026-04-15 14:05:00",
        "createdAt": "2026-04-15 14:00:00",
        "updatedAt": "2026-04-15 14:05:00"
      },
      {
        "id": 2,
        "title": "文章写作辅助",
        "channelConfigId": 1,
        "sceneType": "general",
        "status": 1,
        "lastMessageAt": "2026-04-14 09:30:00",
        "createdAt": "2026-04-14 09:00:00",
        "updatedAt": "2026-04-14 09:30:00"
      }
    ]
  }
}
```

### 3.4 查询AI会话详情

- 请求：`GET /api/user/ai/sessions/{id}`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 响应：`AiSessionDetailVO`（继承 AiSessionVO 字段）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `channelName` | String | 渠道名称 |
| `modelName` | String | 模型名称 |

- 响应示例：

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
    "lastMessageAt": "2026-04-15 14:05:00",
    "createdAt": "2026-04-15 14:00:00",
    "updatedAt": "2026-04-15 14:05:00",
    "channelName": "DeepSeek 对话渠道",
    "modelName": "deepseek-chat"
  }
}
```

### 3.5 分页查询会话消息

- 请求：`GET /api/user/ai/sessions/{id}/messages`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

- 响应：`PageResult<AiMessageVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 消息ID |
| `roleType` | String | 角色类型：user/assistant/system |
| `content` | String | 消息内容 |
| `tokenCount` | Integer | 消息token数 |
| `responseStatus` | Integer | 响应状态：0-失败/1-成功 |
| `errorMessage` | String | 错误信息 |
| `ragReferences` | Array | RAG 引用来源；仅助手消息命中知识库时返回，字段见下方 |
| `createdAt` | DateTime | 创建时间 |

- `ragReferences` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sourceType` | String | 来源类型：`public_article`/`forum_post`/`author_profile`/`admin_entry` |
| `sourceId` | Long | 来源对象ID |
| `entryId` | Long | 知识条目ID |
| `title` | String | 来源标题 |
| `sourceUrl` | String | 来源页面URL |
| `chunkIndex` | Integer | 命中分块序号 |
| `score` | Double | 相似度 |

- 响应示例：

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
        "createdAt": "2026-04-15 14:05:00"
      },
      {
        "id": 9,
        "roleType": "user",
        "content": "帮我解释一下 Java 的 Stream API",
        "tokenCount": 12,
        "responseStatus": 1,
        "errorMessage": null,
        "createdAt": "2026-04-15 14:04:00"
      }
    ]
  }
}
```

### 3.6 发送消息

- 请求：`POST /api/user/ai/sessions/{id}/messages`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 请求体：`AiMessageSendRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | String | 是 | 消息内容，最大2000字符 |
| `requestSceneType` | String | 否 | 请求场景类型，默认 `general` |
| `requestTargetId` | Long | 否 | 关联目标ID |

- 响应：`Result<AiMessageVO>`

- 响应示例：

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
    "ragReferences": [],
    "createdAt": "2026-04-15 14:04:00"
  }
}
```

> RAG 当前仅检索公开可访问知识：公开文章、公开论坛帖子、公开作者资料和启用的后台知识条目；私聊、私密文章、白名单文章、登录可见内容、隐藏/删除内容不会作为引用返回。

### 3.7 关闭会话

- 请求：`DELETE /api/user/ai/sessions/{id}`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 响应：空

### 3.8 查询我的AI配额

- 请求：`GET /api/user/ai/sessions/quota`
- 鉴权：是
- 响应：`AiQuotaVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `dailyLimit` | int | 每日限额 |
| `usedToday` | long | 今日已用 |
| `remainingToday` | long | 今日剩余 |

- 响应示例：

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

## 4. 后台管理接口

### 4.1 后台 AI 渠道配置

#### 4.1.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询渠道配置 | GET | `/api/sys/ai/channels` | 查询渠道列表 |
| 查询渠道配置详情 | GET | `/api/sys/ai/channels/{id}` | 获取渠道详情 |
| 创建渠道配置 | POST | `/api/sys/ai/channels` | 新增渠道 |
| 更新渠道配置 | PUT | `/api/sys/ai/channels/{id}` | 修改渠道 |
| 更新渠道状态 | PUT | `/api/sys/ai/channels/{id}/status` | 启用/禁用 |
| 删除渠道配置 | DELETE | `/api/sys/ai/channels/{id}` | 删除渠道 |

#### 4.1.2 分页查询渠道配置

- 请求：`GET /api/sys/ai/channels`
- 鉴权：`ai:channel-config:query`
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`Result<PageResult<AiChannelConfigVO>>`

| 字段 | 类型 | 说明 |
| `id` | Long | 渠道ID |
| `channelCode` | String | 渠道编码 |
| `channelName` | String | 渠道名称 |
| `provider` | String | 提供方 |
| `modelName` | String | 模型名称 |
| `apiBaseUrl` | String | 接口基础地址 |
| `apiKeyEncrypted` | String | 加密后的API Key |
| `dailyQuota` | Integer | 全局每日额度，0表示不限制 |
| `userDailyQuota` | Integer | 单用户每日额度，0表示不限制 |
| `maxContextTokens` | Integer | 上下文长度上限，0表示不限制 |
| `dataScopeJson` | String | 可读取数据范围配置JSON |
| `systemPromptTemplate` | String | 系统提示词模板 |
| `status` | Integer | 状态：0-停用/1-启用 |
| `isDefault` | Integer | 是否默认渠道：0-否/1-是 |
| `createdBy` | Long | 创建人ID |
| `updatedBy` | Long | 更新人ID |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

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
        "id": 1,
        "channelCode": "deepseek-chat",
        "channelName": "DeepSeek 对话渠道",
        "provider": "deepseek",
        "modelName": "deepseek-chat",
        "apiBaseUrl": "https://api.deepseek.com/v1",
        "apiKeyEncrypted": "******",
        "dailyQuota": 10000,
        "userDailyQuota": 50,
        "maxContextTokens": 32000,
        "dataScopeJson": null,
        "systemPromptTemplate": "你是一个友好的AI助手，请用中文回答用户问题。",
        "status": 1,
        "isDefault": 1,
        "createdBy": 1,
        "updatedBy": 1,
        "createdAt": "2026-03-01 10:00:00",
        "updatedAt": "2026-04-10 15:00:00"
      }
    ]
  }
}
```

#### 4.1.3 查询渠道配置详情

- 请求：`GET /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:query`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 渠道配置ID |

- 响应：`Result<AiChannelConfigVO>`（字段同分页查询中的记录）

- 响应示例：

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
    "dailyQuota": 10000,
    "userDailyQuota": 50,
    "maxContextTokens": 32000,
    "dataScopeJson": null,
    "systemPromptTemplate": "你是一个友好的AI助手，请用中文回答用户问题。",
    "status": 1,
    "isDefault": 1,
    "createdBy": 1,
    "updatedBy": 1,
    "createdAt": "2026-03-01 10:00:00",
    "updatedAt": "2026-04-10 15:00:00"
  }
}
```

#### 4.1.4 创建渠道配置

- 请求：`POST /api/sys/ai/channels`
- 鉴权：`ai:channel-config:create`
- 请求体：`AiChannelConfigSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `channelCode` | String | 是 | 渠道编码 |
| `channelName` | String | 是 | 渠道名称 |
| `provider` | String | 是 | 提供方 |
| `modelName` | String | 是 | 模型名称 |
| `apiBaseUrl` | String | 否 | 接口基础地址 |
| `apiKeyEncrypted` | String | 否 | 加密后的API Key |
| `dailyQuota` | Integer | 否 | 全局每日额度，0表示不限制 |
| `userDailyQuota` | Integer | 否 | 单用户每日额度，0表示不限制 |
| `maxContextTokens` | Integer | 否 | 上下文长度上限，0表示不限制 |
| `dataScopeJson` | String | 否 | 可读取数据范围配置JSON |
| `systemPromptTemplate` | String | 否 | 系统提示词模板 |
| `status` | Integer | 否 | 状态：0-停用/1-启用 |
| `isDefault` | Integer | 否 | 是否默认渠道：0-否/1-是 |
| `mfaTicket` | String | 否 | 二次验证票据（修改高风险字段时必填） |

- 响应：`Result<AiChannelConfigVO>`（字段同渠道配置详情）

#### 4.1.5 更新渠道配置

- 请求：`PUT /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:update`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 渠道配置ID |

- 请求体：`AiChannelConfigSaveRequest`（字段同创建渠道配置）
- 响应：`Result<AiChannelConfigVO>`（字段同渠道配置详情）

#### 4.1.6 更新渠道状态

- 请求：`PUT /api/sys/ai/channels/{id}/status`
- 鉴权：`ai:channel-config:update`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 渠道配置ID |

- 请求体：`AiChannelStatusRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | Integer | 是 | 状态：0-停用/1-启用 |

- 响应：`Result<Void>`（空）

#### 4.1.7 删除渠道配置

- 请求：`DELETE /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:delete`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 渠道配置ID |

- 响应：`Result<Void>`（空）

### 4.2 后台 AI 会话管理

#### 4.2.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询用户会话 | GET | `/api/sys/ai/sessions` | 查询会话列表 |
| 查询会话详情 | GET | `/api/sys/ai/sessions/{id}` | 获取会话详情 |

#### 4.2.2 分页查询用户会话

- 请求：`GET /api/sys/ai/sessions`
- 鉴权：`ai:session:query`
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | Long | 否 | 用户ID |
| `status` | Integer | 否 | 会话状态：0-关闭/1-正常 |
| `channelConfigId` | Long | 否 | 渠道配置ID |
| `startTime` | LocalDateTime | 否 | 开始时间 |
| `endTime` | LocalDateTime | 否 | 结束时间 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

- 响应：`Result<PageResult<AiSessionAdminVO>>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `nickname` | String | 用户昵称 |
| `channelConfigId` | Long | 渠道配置ID |
| `channelName` | String | 渠道名称 |
| `title` | String | 会话标题 |
| `sceneType` | String | 场景类型 |
| `status` | Integer | 状态：0-关闭/1-正常 |
| `lastMessageAt` | DateTime | 最后消息时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

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
        "userId": 2,
        "username": "zhangsan",
        "nickname": "张三",
        "channelConfigId": 1,
        "channelName": "DeepSeek 对话渠道",
        "title": "Java 学习助手",
        "sceneType": "general",
        "status": 1,
        "lastMessageAt": "2026-04-15 14:05:00",
        "createdAt": "2026-04-15 14:00:00",
        "updatedAt": "2026-04-15 14:05:00"
      }
    ]
  }
}
```

#### 4.2.3 查询会话详情

- 请求：`GET /api/sys/ai/sessions/{id}`
- 鉴权：`ai:session:query`
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 响应：`Result<AiSessionAdminVO>`（字段同分页查询中的记录）

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "userId": 2,
    "username": "zhangsan",
    "nickname": "张三",
    "channelConfigId": 1,
    "channelName": "DeepSeek 对话渠道",
    "title": "Java 学习助手",
    "sceneType": "general",
    "status": 1,
    "lastMessageAt": "2026-04-15 14:05:00",
    "createdAt": "2026-04-15 14:00:00",
    "updatedAt": "2026-04-15 14:05:00"
  }
}
```

### 4.3 后台 AI 调用统计

#### 4.3.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询使用日志 | GET | `/api/sys/ai/usage-logs` | 查询调用记录 |
| 获取使用统计 | GET | `/api/sys/ai/usage-logs/stats` | 聚合统计 |

#### 4.3.2 分页查询使用日志

- 请求：`GET /api/sys/ai/usage-logs`
- 鉴权：`ai:usage-stats:query`
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | Long | 否 | 用户ID |
| `channelConfigId` | Long | 否 | 渠道配置ID |
| `startTime` | LocalDateTime | 否 | 开始时间 |
| `endTime` | LocalDateTime | 否 | 结束时间 |
| `successStatus` | Integer | 否 | 成功状态：0-失败/1-成功 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

- 响应：`Result<PageResult<AiUsageLogVO>>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 日志ID |
| `userId` | Long | 用户ID |
| `channelConfigId` | Long | 渠道配置ID |
| `sessionId` | Long | 会话ID |
| `requestSceneType` | String | 请求场景类型 |
| `requestTokens` | Integer | 请求token数 |
| `responseTokens` | Integer | 响应token数 |
| `totalTokens` | Integer | 总token数 |
| `quotaCost` | Integer | 额度消耗 |
| `successStatus` | Integer | 成功状态：0-失败/1-成功 |
| `errorCode` | String | 错误码 |
| `ragEnabled` | Integer | 是否启用 RAG：0-否/1-是 |
| `ragHitCount` | Integer | RAG 命中数量 |
| `ragDurationMs` | Long | RAG 检索耗时毫秒 |
| `ragReferences` | Array | RAG 引用来源 |
| `createdAt` | DateTime | 调用时间 |

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
        "userId": 2,
        "channelConfigId": 1,
        "sessionId": 1,
        "requestSceneType": "general",
        "requestTokens": 12,
        "responseTokens": 156,
        "totalTokens": 168,
        "quotaCost": 1,
        "successStatus": 1,
        "errorCode": null,
        "ragEnabled": 1,
        "ragHitCount": 1,
        "ragDurationMs": 18,
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
        "createdAt": "2026-04-15 14:05:00"
      }
    ]
  }
}
```

#### 4.3.3 获取使用统计

- 请求：`GET /api/sys/ai/usage-logs/stats`
- 鉴权：`ai:usage-stats:query`
- 查询参数：（同分页查询使用日志）
- 响应：`Result<AiUsageStatsVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalCalls` | long | 总调用次数 |
| `successCalls` | long | 成功调用次数 |
| `failedCalls` | long | 失败调用次数 |
| `totalTokens` | long | 总token数 |
| `totalQuotaCost` | long | 总额度消耗 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "totalCalls": 256,
    "successCalls": 240,
    "failedCalls": 16,
    "totalTokens": 128000,
    "totalQuotaCost": 256
  }
}
```

### 4.4 后台 AI 知识源配置

#### 4.4.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 查询所有知识源配置 | GET | `/api/sys/ai/knowledge/source-config` | 列出全部配置 |
| 查询配置详情 | GET | `/api/sys/ai/knowledge/source-config/{id}` | 单条详情 |
| 更新配置 | PUT | `/api/sys/ai/knowledge/source-config/{id}` | 修改同步间隔等 |
| 切换启停 | PUT | `/api/sys/ai/knowledge/source-config/{id}/toggle?enabled=0/1` | 启停切换 |

#### 4.4.2 查询所有知识源配置

- 请求：`GET /api/sys/ai/knowledge/source-config`
- 鉴权：`ai:knowledge:query`
- 响应：`Result<List<AiKnowledgeSourceConfigVO>>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 配置ID |
| `sourceType` | String | 知识源类型编码 |
| `enabled` | Integer | 0-禁用/1-启用 |
| `syncInterval` | Integer | 同步间隔（秒） |
| `lastSyncedAt` | DateTime | 最近同步时间 |
| `lastSyncStatus` | String | 最近同步状态 |
| `configJson` | String | 扩展配置JSON |
| `updatedBy` | Long | 更新人 |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

#### 4.4.3 更新配置

- 请求：`PUT /api/sys/ai/knowledge/source-config/{id}`
- 鉴权：`ai:knowledge:update`
- 请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `syncInterval` | Integer | 是 | 同步间隔（秒） |
| `configJson` | String | 否 | 扩展配置 |
| `remark` | String | 否 | 备注 |

- 响应：`Result<AiKnowledgeSourceConfigVO>`

#### 4.4.4 切换启停

- 请求：`PUT /api/sys/ai/knowledge/source-config/{id}/toggle?enabled=0`
- 鉴权：`ai:knowledge:update`
- 响应：`Result<Void>`

### 4.5 后台 AI 知识条目管理

#### 4.5.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询知识条目 | GET | `/api/sys/ai/knowledge/entries` | 支持筛选 |
| 查询条目详情 | GET | `/api/sys/ai/knowledge/entries/{id}` | 单条详情 |
| 更新条目状态 | PUT | `/api/sys/ai/knowledge/entries/{id}/status?status=` | 状态变更 |
| 触发同步任务 | POST | `/api/sys/ai/knowledge/entries/sync` | 新建同步 |
| 分页查询同步任务 | GET | `/api/sys/ai/knowledge/entries/sync/tasks` | 任务列表 |
| 查询同步任务详情 | GET | `/api/sys/ai/knowledge/entries/sync/tasks/{taskId}` | 单条详情 |
| 重试同步任务 | POST | `/api/sys/ai/knowledge/entries/sync/tasks/{taskId}/retry` | 重试失败任务 |

#### 4.5.2 分页查询知识条目

- 请求：`GET /api/sys/ai/knowledge/entries`
- 鉴权：`ai:knowledge:query`
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceType` | String | 否 | 知识源类型 |
| `status` | Integer | 否 | 状态：0-禁用/1-正常/2-过期/3-已删除 |
| `keyword` | String | 否 | 标题关键词 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `20` |

- 响应：`Result<PageResult<AiKnowledgeEntryVO>>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 条目ID |
| `sourceType` | String | 来源类型 |
| `sourceId` | Long | 来源对象ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `sourceUrl` | String | 来源URL |
| `authorId` | Long | 作者ID |
| `status` | Integer | 状态 |
| `version` | Integer | 版本号 |
| `chunkCount` | Integer | 分块数量 |
| `sourceUpdatedAt` | DateTime | 源内容更新时间 |
| `syncedAt` | DateTime | 同步时间 |
| `tagJson` | String | 标签JSON |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

#### 4.5.3 触发同步任务

- 请求：`POST /api/sys/ai/knowledge/entries/sync`
- 鉴权：`ai:knowledge:sync`
- 请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sourceType` | String | 是 | 知识源类型 |
| `taskType` | String | 否 | 任务类型，默认 `full_sync` |
| `remark` | String | 否 | 备注 |

- 响应：`Result<AiKnowledgeSyncTaskVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 任务ID |
| `taskType` | String | 任务类型 |
| `sourceType` | String | 知识源类型 |
| `status` | Integer | 状态：0-待执行/1-执行中/2-已完成/3-失败 |
| `totalCount` | Integer | 总条目数 |
| `successCount` | Integer | 成功数 |
| `failCount` | Integer | 失败数 |
| `skipCount` | Integer | 跳过数 |
| `errorMessage` | String | 错误信息 |
| `retryCount` | Integer | 已重试次数 |
| `maxRetry` | Integer | 最大重试次数 |
| `startedAt` | DateTime | 开始时间 |
| `completedAt` | DateTime | 完成时间 |
| `triggeredBy` | String | 触发方式 |
| `operatorId` | Long | 操作人ID |
| `createdAt` | DateTime | 创建时间 |

#### 4.5.4 异常场景

| 场景 | 错误码 | 说明 |
| --- | --- | --- |
| 知识源配置不存在 | 72001 | 配置ID无效 |
| 无效的知识源类型 | 72002 | sourceType 不在枚举范围 |
| 同步任务正在执行 | 72007 | 同一来源类型已有运行中任务 |
| 同步任务重试超限 | 72009 | retryCount >= maxRetry |

### 4.6 后台 AI Agent 定义管理

> 权限要求：`ai:agent:query` / `ai:agent:create` / `ai:agent:update` / `ai:agent:delete`

#### 4.6.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询 Agent 定义 | GET | `/api/sys/ai/agents/definitions` | 分页查询 |
| 查询 Agent 定义详情 | GET | `/api/sys/ai/agents/definitions/{id}` | 获取详情 |
| 创建 Agent 定义 | POST | `/api/sys/ai/agents/definitions` | 创建定义 |
| 更新 Agent 定义 | PUT | `/api/sys/ai/agents/definitions/{id}` | 更新定义 |
| 切换 Agent 启停 | PUT | `/api/sys/ai/agents/definitions/{id}/toggle?enabled=0\|1` | 启停切换 |
| 删除 Agent 定义 | DELETE | `/api/sys/ai/agents/definitions/{id}` | 删除定义 |

#### 4.6.2 分页查询 Agent 定义

```http
GET /api/sys/ai/agents/definitions?page=1&size=10&keyword=&enabled=
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| keyword | string | 否 | 名称关键词 |
| enabled | int | 否 | 0-停用，1-启用 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 1,
    "records": [
      {
        "id": 1,
        "name": "文章摘要生成器",
        "description": "为文章生成精炼摘要",
        "systemPrompt": "你是一个专业的文章摘要生成器...",
        "channelConfigId": 1,
        "dataScopeJson": "[\"PUBLIC_ARTICLES\"]",
        "enabled": 1,
        "maxTurns": 1,
        "createdBy": 1,
        "updatedBy": 1,
        "createdAt": "2026-05-04T21:00:00",
        "updatedAt": "2026-05-04T21:00:00"
      }
    ]
  }
}
```

#### 4.6.3 创建 Agent 定义

```http
POST /api/sys/ai/agents/definitions
Content-Type: application/json
```

```json
{
  "name": "文章摘要生成器",
  "description": "为文章生成精炼摘要",
  "systemPrompt": "你是一个专业的文章摘要生成器...",
  "channelConfigId": 1,
  "dataScopeJson": "[\"PUBLIC_ARTICLES\"]",
  "maxTurns": 1
}
```

`dataScopeJson` 仅支持 `AiDataScopeEnum` 声明的数据范围 code 或枚举名数组，例如 `["public_articles"]`、`["PUBLIC_ARTICLES"]`、`["PROFILE"]`。
若出现未知范围、非 JSON 数组或空数组，服务返回 `40011` 非法参数。

#### 4.6.4 异常场景

| 场景 | 错误码 | 说明 |
| --- | --- | --- |
| Agent 不存在 | 73001 | ID 无效 |
| Agent 已停用 | 73002 | 用户发起任务时 agent 未启用 |
| Agent 名称重复 | 73003 | 同名 agent 已存在 |
| 数据范围非法 | 40011 | `dataScopeJson` 不是合法数组或包含未知范围 |

### 4.7 后台 AI Agent 任务管理

> 权限要求：`ai:agent:query`

#### 4.7.1 接口总览

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询 Agent 任务 | GET | `/api/sys/ai/agents/tasks` | 分页查询 |
| 查询 Agent 任务详情 | GET | `/api/sys/ai/agents/tasks/{id}` | 获取详情 |

#### 4.7.2 分页查询 Agent 任务

```http
GET /api/sys/ai/agents/tasks?page=1&size=10&agentId=&status=
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| agentId | long | 否 | 按 Agent 定义 ID 筛选 |
| status | int | 否 | 0-待执行 1-执行中 2-已完成 3-失败 4-已取消 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 1,
    "records": [
      {
        "id": 1,
        "userId": 100,
        "agentId": 1,
        "agentName": "文章摘要生成器",
        "status": 2,
        "inputContent": "请为这篇文章生成摘要",
        "outputContent": "这篇文章主要讲述了...",
        "errorMessage": null,
        "tokenCount": 256,
        "startedAt": "2026-05-04T21:00:01",
        "completedAt": "2026-05-04T21:00:05",
        "createdAt": "2026-05-04T21:00:00"
      }
    ]
  }
}
```

### 4.8 后台 AI 工具与 MCP 管理

#### 4.8.1 工具接口总览

> 权限要求：`ai:tool:query/create/update/delete/execute`

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询工具定义 | GET | `/api/sys/ai/tools` | 支持按编码、名称、来源、启停筛选 |
| 查询工具详情 | GET | `/api/sys/ai/tools/{id}` | 获取工具定义 |
| 创建工具定义 | POST | `/api/sys/ai/tools` | 创建内置或 MCP 工具定义 |
| 更新工具定义 | PUT | `/api/sys/ai/tools/{id}` | 修改工具定义 |
| 更新工具状态 | PUT | `/api/sys/ai/tools/{id}/status?enabled=0\|1` | 启停工具 |
| 删除工具定义 | DELETE | `/api/sys/ai/tools/{id}` | v1 软删除为停用 |
| 后台测试执行工具 | POST | `/api/sys/ai/tools/{id}/execute` | 走统一授权、参数校验和调用日志 |
| 分页查询调用日志 | GET | `/api/sys/ai/tools/call-logs` | 查询工具调用记录 |
| 分页查询授权 | GET | `/api/sys/ai/tools/authorizations` | 查询工具授权关系 |
| 创建授权 | POST | `/api/sys/ai/tools/authorizations` | 新增授权关系 |
| 更新授权 | PUT | `/api/sys/ai/tools/authorizations/{id}` | 修改授权关系 |
| 删除授权 | DELETE | `/api/sys/ai/tools/authorizations/{id}` | 删除授权关系 |

工具定义请求字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `toolCode` | String | 是 | 工具编码，唯一 |
| `toolName` | String | 是 | 工具名称 |
| `sourceType` | String | 是 | `builtin` / `mcp` |
| `mcpServerId` | Long | MCP 必填 | MCP 服务 ID |
| `mcpToolName` | String | MCP 必填 | MCP 原始工具名 |
| `parametersSchema` | String | 否 | JSON Object，v1 校验 required 字段 |
| `resultSchema` | String | 否 | JSON Object |
| `riskLevel` | String | 是 | `low` / `medium` / `high` |
| `useScenarios` | String | 否 | JSON 数组，如 `["agent"]` |
| `enabled` | Integer | 是 | 0-停用，1-启用 |

授权关系请求字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `toolId` | Long | 是 | 工具 ID |
| `authorizationType` | String | 是 | `agent` / `scene` / `permission` / `data_scope` |
| `authorizationKey` | String | 是 | Agent ID、场景编码、权限码或数据范围编码 |
| `dataScope` | String | 否 | 额外数据范围说明 |
| `enabled` | Integer | 是 | 0-停用，1-启用 |

执行请求：

```json
{
  "arguments": "{\"query\":\"java\"}",
  "agentId": 1,
  "sessionId": null,
  "taskId": null,
  "sceneType": "agent",
  "dataScope": "public_articles"
}
```

执行响应：`AiToolExecuteVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | Boolean | 是否成功 |
| `resultText` | String | 成功结果文本 |
| `errorMessage` | String | 失败信息 |
| `elapsedMs` | Long | 耗时毫秒 |
| `callLogId` | Long | 调用日志 ID |

关键边界：

- 默认没有授权关系的工具不可调用，返回 `73507`。
- MCP 工具执行前会校验工具启用、MCP 服务启用、参数 JSON 与 required 字段。
- 调用日志会记录入参摘要、结果摘要、耗时和错误信息，摘要会脱敏敏感 key/token/password/secret。
- 内置工具 v1 先提供注册、授权和审计框架；未绑定执行器时按调用失败记录日志。

#### 4.8.2 MCP 服务接口总览

> 权限要求：`ai:mcp:query/create/update/delete/discover`

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 分页查询 MCP 服务 | GET | `/api/sys/ai/mcp-servers` | 支持按名称、传输、启停筛选 |
| 查询 MCP 服务详情 | GET | `/api/sys/ai/mcp-servers/{id}` | 不返回鉴权配置明文 |
| 创建 MCP 服务 | POST | `/api/sys/ai/mcp-servers` | 创建 stdio/http MCP 服务 |
| 更新 MCP 服务 | PUT | `/api/sys/ai/mcp-servers/{id}` | 更新连接配置 |
| 更新 MCP 状态 | PUT | `/api/sys/ai/mcp-servers/{id}/status?enabled=0\|1` | 启停服务 |
| 删除 MCP 服务 | DELETE | `/api/sys/ai/mcp-servers/{id}` | v1 软删除为停用 |
| 发现工具 | POST | `/api/sys/ai/mcp-servers/{id}/discover` | 拉取工具列表并同步快照/工具定义 |
| 查询工具快照 | GET | `/api/sys/ai/mcp-servers/{id}/tools` | 查询该服务已发现工具 |
| 健康检查 | GET | `/api/sys/ai/mcp-servers/{id}/health` | 检查连接状态并回写最近健康状态 |

MCP 服务请求字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `serverName` | String | 是 | 服务名称，唯一 |
| `transportType` | String | 是 | `stdio` / `http` |
| `connectionConfigJson` | String | 是 | 连接配置 JSON |
| `authConfigJson` | String | 否 | 鉴权配置 JSON，仅入库不在 VO 明文返回 |
| `timeoutSeconds` | Integer | 是 | 超时时间，必须大于 0 |
| `enabled` | Integer | 是 | 0-停用，1-启用 |

配置示例：

```json
{
  "serverName": "local-filesystem",
  "transportType": "stdio",
  "connectionConfigJson": "{\"command\":[\"npx\",\"-y\",\"@modelcontextprotocol/server-filesystem\",\".\"]}",
  "authConfigJson": null,
  "timeoutSeconds": 30,
  "enabled": 1
}
```

```json
{
  "serverName": "remote-search",
  "transportType": "http",
  "connectionConfigJson": "{\"url\":\"https://example.com/mcp\"}",
  "authConfigJson": "{\"bearerToken\":\"secret-token\"}",
  "timeoutSeconds": 30,
  "enabled": 1
}
```

异常场景：

| 场景 | 错误码 | 说明 |
| --- | --- | --- |
| 工具不存在 | 73501 | ID 或编码无效 |
| 工具编码重复 | 73502 | `toolCode` 已存在 |
| 工具未授权 | 73507 | 没有匹配授权关系 |
| 工具已停用 | 73508 | 不允许调用 |
| MCP 服务不存在 | 73521 | ID 无效 |
| MCP 传输类型无效 | 73522 | 仅支持 `stdio` / `http` |
| MCP 服务已停用 | 73523 | 不允许发现或调用 |
| MCP 工具发现失败 | 73524 | 连接失败或远端返回异常 |

## 5. 枚举值说明

### 5.1 渠道状态 (AiChannelStatusEnum)

| 值 | 说明 |
| --- | --- |
| `0` | 停用 |
| `1` | 启用 |

### 5.2 会话状态

| 值 | 说明 |
| --- | --- |
| `0` | 关闭 |
| `1` | 正常 |

### 5.3 消息角色类型

| 值 | 说明 |
| --- | --- |
| `user` | 用户消息 |
| `assistant` | AI回复 |
| `system` | 系统消息 |

### 5.4 默认渠道标识

| 值 | 说明 |
| --- | --- |
| `0` | 非默认 |
| `1` | 默认渠道 |

### 5.5 知识源类型 (AiKnowledgeSourceTypeEnum)

| 编码 | 说明 |
| --- | --- |
| `public_article` | 公开文章 |
| `author_profile` | 作者公开资料 |
| `forum_post` | 论坛帖子 |
| `admin_entry` | 管理员维护知识条目 |

### 5.6 知识条目状态

| 值 | 说明 |
| --- | --- |
| `0` | 禁用 |
| `1` | 正常 |
| `2` | 过期 |
| `3` | 已删除 |

### 5.7 同步任务状态

| 值 | 说明 |
| --- | --- |
| `0` | 待执行 |
| `1` | 执行中 |
| `2` | 已完成 |
| `3` | 失败 |

### 5.8 Agent 任务状态 (AiAgentTaskStatusEnum)

| 值 | 说明 |
| --- | --- |
| `0` | 待执行 |
| `1` | 执行中 |
| `2` | 已完成 |
| `3` | 失败 |
| `4` | 已取消 |

| 值 | 说明 |
| --- | --- |
| `0` | 非默认 |
| `1` | 默认渠道 |

## 6. 维护规则

- 新增、删除、修改前端可见接口时，必须同步更新对应文档。
- 如果只是补字段、改枚举或改边界行为，也不能只改代码不改文档。

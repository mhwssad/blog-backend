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
| `createdAt` | DateTime | 创建时间 |

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
    "createdAt": "2026-04-15 14:04:00"
  }
}
```

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

## 6. 维护规则

- 新增、删除、修改前端可见接口时，必须同步更新对应文档。
- 如果只是补字段、改枚举或改边界行为，也不能只改代码不改文档。

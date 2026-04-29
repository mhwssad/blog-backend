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
| `channelId` | Long | 否 | 渠道配置ID，不填则使用默认渠道 |

- 响应：`AiSessionVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |
| `channelId` | Long | 渠道ID |
| `channelName` | String | 渠道名称 |
| `status` | String | 状态：active/closed |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

### 3.3 查询我的AI会话列表

- 请求：`GET /api/user/ai/sessions`
- 鉴权：是
- 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`PageResult<AiSessionVO>`

### 3.4 查询AI会话详情

- 请求：`GET /api/user/ai/sessions/{id}`
- 鉴权：是
- 路径参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |

- 响应：`AiSessionDetailVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |
| `channelId` | Long | 渠道ID |
| `channelName` | String | 渠道名称 |
| `status` | String | 状态 |
| `modelName` | String | 模型名称 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

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
| `role` | String | 角色：user/assistant |
| `content` | String | 消息内容 |
| `createdAt` | DateTime | 创建时间 |

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
| `content` | String | 是 | 消息内容 |

- 响应：`AiMessageVO`

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

- 响应：`PageResult<AiChannelConfigVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 渠道ID |
| `channelCode` | String | 渠道编码 |
| `channelName` | String | 渠道名称 |
| `status` | Integer | 状态：0-禁用/1-启用 |
| `priority` | Integer | 优先级 |
| `modelName` | String | 模型名称 |
| `dailyLimit` | Integer | 每日限制次数 |
| `createdAt` | DateTime | 创建时间 |

#### 4.1.3 查询渠道配置详情

- 请求：`GET /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:query`
- 响应：`AiChannelConfigVO`

#### 4.1.4 创建渠道配置

- 请求：`POST /api/sys/ai/channels`
- 鉴权：`ai:channel-config:create`
- 请求体：`AiChannelConfigSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `channelCode` | String | 是 | 渠道编码 |
| `channelName` | String | 是 | 渠道名称 |
| `modelName` | String | 是 | 模型名称 |
| `apiKey` | String | 是 | API密钥 |
| `baseUrl` | String | 是 | API地址 |
| `status` | Integer | 否 | 状态，默认1启用 |
| `priority` | Integer | 否 | 优先级，默认0 |
| `dailyLimit` | Integer | 否 | 每日限制次数 |

#### 4.1.5 更新渠道配置

- 请求：`PUT /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:update`
- 请求体：`AiChannelConfigSaveRequest`（同上）

#### 4.1.6 更新渠道状态

- 请求：`PUT /api/sys/ai/channels/{id}/status`
- 鉴权：`ai:channel-config:update`
- 请求体：`AiChannelStatusRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | Integer | 是 | 状态：0-禁用/1-启用 |

#### 4.1.7 删除渠道配置

- 请求：`DELETE /api/sys/ai/channels/{id}`
- 鉴权：`ai:channel-config:delete`

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
| `channelId` | Long | 否 | 渠道ID |
| `status` | String | 否 | 会话状态 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`PageResult<AiSessionAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 会话ID |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `channelId` | Long | 渠道ID |
| `channelName` | String | 渠道名称 |
| `status` | String | 状态 |
| `messageCount` | Integer | 消息条数 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

#### 4.2.3 查询会话详情

- 请求：`GET /api/sys/ai/sessions/{id}`
- 鉴权：`ai:session:query`
- 响应：`AiSessionAdminVO`（同分页查询）

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
| `channelId` | Long | 否 | 渠道ID |
| `startDate` | String | 否 | 开始日期，格式：yyyy-MM-dd |
| `endDate` | String | 否 | 结束日期，格式：yyyy-MM-dd |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

- 响应：`PageResult<AiUsageLogVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 日志ID |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `channelId` | Long | 渠道ID |
| `channelName` | String | 渠道名称 |
| `sessionId` | Long | 会话ID |
| `modelName` | String | 模型名称 |
| `inputTokens` | Integer | 输入Token数 |
| `outputTokens` | Integer | 输出Token数 |
| `cost` | BigDecimal | 消耗金额 |
| `success` | Boolean | 是否成功 |
| `errorMessage` | String | 错误信息 |
| `createdAt` | DateTime | 调用时间 |

#### 4.3.3 获取使用统计

- 请求：`GET /api/sys/ai/usage-logs/stats`
- 鉴权：`ai:usage-stats:query`
- 查询参数：（同分页查询）
- 响应：`AiUsageStatsVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalCalls` | Long | 总调用次数 |
| `successCalls` | Long | 成功次数 |
| `failedCalls` | Long | 失败次数 |
| `totalInputTokens` | Long | 总输入Token |
| `totalOutputTokens` | Long | 总输出Token |
| `totalCost` | BigDecimal | 总消耗金额 |

## 5. 枚举值说明

### 5.1 渠道状态 (AiChannelStatusEnum)

| 值 | 说明 |
| --- | --- |
| `0` | 禁用 |
| `1` | 启用 |

### 5.2 会话状态 (AiChatSessionStatusEnum)

| 值 | 说明 |
| --- | --- |
| `active` | 进行中 |
| `closed` | 已关闭 |

### 5.3 消息角色

| 值 | 说明 |
| --- | --- |
| `user` | 用户消息 |
| `assistant` | AI回复 |

## 6. 维护规则

- 新增、删除、修改前端可见接口时，必须同步更新对应文档。
- 如果只是补字段、改枚举或改边界行为，也不能只改代码不改文档。

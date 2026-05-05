# AI 渠道配置

## 1. 渠道配置架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           渠道配置层级                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      ai_channel_config                               │   │
│  │                        渠道配置表                                    │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │   │
│  │  │ 渠道A    │ │ 渠道B    │ │ 渠道C    │ │ 渠道D    │               │   │
│  │  │ GPT-4    │ │ DeepSeek │ │ Claude   │ │ 通义千问  │               │   │
│  │  │ provider │ │ provider │ │ provider │ │ provider │               │   │
│  │  │ openai   │ │ deepseek│ │ anthropic│ │ alibaba  │               │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│                    ┌───────────────┼───────────────┐                      │
│                    ▼               ▼               ▼                      │
│             ┌───────────┐   ┌───────────┐   ┌───────────┐                │
│             │ AiSession │   │  Agent    │   │  Agent    │                │
│             │ 会话绑定   │   │ Definition│   │   Task    │                │
│             │ 一个渠道   │   │ 绑定一个   │   │ 绑定Agent │                │
│             └───────────┘   │ 渠道       │   │ 定义      │                │
│                             └───────────┘   └───────────┘                │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. 渠道配置属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `channelCode` | String | 渠道编码，唯一标识，如 `openai-gpt-4` |
| `channelName` | String | 渠道名称，展示用，如 `OpenAI GPT-4` |
| `provider` | String | 提供方，如 `openai`、`anthropic`、`deepseek` |
| `modelName` | String | 模型名称，如 `gpt-4-turbo`、`deepseek-chat` |
| `apiBaseUrl` | String | 接口基础地址，如 `https://api.openai.com/v1` |
| `apiKeyEncrypted` | String | 加密后的 API Key |
| `dailyQuota` | Integer | 全局每日额度，`0` = 不限制 |
| `userDailyQuota` | Integer | 单用户每日额度，`0` = 不限制 |
| `maxContextTokens` | Integer | 上下文长度上限，`0` = 不限制 |
| `dataScopeJson` | String | 可读取数据范围配置 JSON |
| `systemPromptTemplate` | String | 系统提示词模板 |
| `status` | Integer | 状态：`0` - 停用，`1` - 启用 |
| `isDefault` | Integer | 是否默认渠道：`0` - 否，`1` - 是 |

## 3. 渠道选择策略

```
创建会话时选择渠道
         │
         ▼
    ┌─────────────────────┐
    │ channelConfigId     │
    │ 是否传值？           │
    └─────────────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
  是         否
    │         │
    ▼         ▼
┌───────────────┐
│ 根据ID查询     │
│ 渠道配置       │
└───────────────┘
         │
         │         ┌───────────────┐
         │         │ 查询所有启用   │
         │         │ 渠道按默认排序 │
         │         └───────────────┘
         │              │
         ▼              ▼
    ┌─────────────────────┐
    │ 返回配置的渠道        │
    │ 或默认渠道（第一个）   │
    └─────────────────────┘
```

## 4. 后台管理接口

| 方法 | 路径 | 功能 |
|------|------|------|
| `POST` | `/api/sys/ai/channels` | 创建渠道配置 |
| `GET` | `/api/sys/ai/channels` | 分页查询渠道配置 |
| `GET` | `/api/sys/ai/channels/{id}` | 查询渠道配置详情 |
| `PUT` | `/api/sys/ai/channels/{id}` | 更新渠道配置 |
| `PUT` | `/api/sys/ai/channels/{id}/status` | 更新渠道状态 |
| `DELETE` | `/api/sys/ai/channels/{id}` | 删除渠道配置 |

### 权限要求

| 权限 | 说明 |
|------|------|
| `ai:channel-config:query` | 查询权限 |
| `ai:channel-config:create` | 创建权限 |
| `ai:channel-config:update` | 更新权限 |
| `ai:channel-config:delete` | 删除权限 |

## 5. 高风险操作审计

以下字段变更需要 **超级管理员权限 + 二次验证 (MFA)**：

| 变更字段 | 风险说明 |
|----------|----------|
| `apiKeyEncrypted` | API Key 泄露风险 |
| `status` | 渠道启停影响服务可用性 |
| `dataScopeJson` 包含 `PRIVATE_CHAT` | 私聊数据访问权限 |

```java
auditHighRiskChanges(config, request, operatorId) {
    // 1. API Key 变更
    // 2. 状态变更
    // 3. 私聊权限开启
    if (apiKeyChanged || statusChanged || dataScopePrivateChat) {
        superAdminVerifier.requireSuperAdmin(operatorId);
        twoFactorService.validateTicket(mfaTicket, operatorId);
        sysAuditLogService.record(auditRequest);
    }
}
```

## 6. 多渠道场景

### 6.1 会话绑定单一渠道

```
用户会话
    │
    ├─ channelConfigId = 渠道A (GPT-4)
    │
    ├─ 第1条消息 → 渠道A → GPT-4
    ├─ 第2条消息 → 渠道A → GPT-4
    ├─ 第3条消息 → 渠道A → GPT-4
    └─ ...
```

### 6.2 Agent 绑定渠道

```
Agent 定义
    │
    ├─ agentId = 1
    ├─ name = "博客写作助手"
    └─ channelConfigId = 渠道B (DeepSeek)
           │
           └─ Agent 任务全部使用 DeepSeek 模型
```

## 7. API Key 安全

### 7.1 存储安全

- `apiKeyEncrypted` 字段存储加密后的 Key
- 数据库不存储明文 API Key

### 7.2 展示脱敏

```java
maskApiKey(apiKey) {
    // 前3位 + "****" + 后4位
    // "sk-abc123456789" → "sk-********6789"
}
```

### 7.3 更新策略

- 更新时若新值包含 `****`，保留原值
- 若包含真实 Key，则写入并记录审计日志

## 8. 数据范围配置

`dataScopeJson` 示例：

```json
["PUBLIC_ARTICLES", "FORUM_POSTS", "AUTHOR_PROFILE"]
```

| 枚举值 | 说明 |
|--------|------|
| `PUBLIC_ARTICLES` | 公开文章 |
| `PUBLIC_POSTS` | 公开论坛帖子 |
| `AUTHOR_PROFILE` | 作者公开资料 |
| `KNOWLEDGE_ENTRY` | 后台维护知识条目 |
| `PRIVATE_CHAT` | 用户私聊（高风险） |

## 9. 关键文件

| 文件 | 职责 |
|------|------|
| `AiChannelConfigAdminController.java` | 后台渠道管理接口 |
| `AiChannelConfigAdminServiceImpl.java` | 渠道 CRUD + 审计 |
| `AiChannelConfigRepository.java` | 渠道数据访问 |
| `AiChannelConfig.java` | 渠道实体 |
| `AiChannelConfigVO.java` | 渠道展示对象 |
| `AiChannelConfigSaveRequest.java` | 渠道保存请求 |
| `LangChain4jConfig.java` | 底层模型构建 |
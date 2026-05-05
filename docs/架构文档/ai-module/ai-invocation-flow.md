# AI 调用流程详解

## 1. 整体调用链

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              用户发起请求                                      │
│                    POST /api/user/ai/sessions/{id}/messages                  │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        AiChatController                                       │
│                         发送消息入口                                          │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        AiChatService                                          │
│                        业务编排层                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  1. verifySessionOwnership() 会话归属校验                           │    │
│  │  2. 加载 AiChannelConfig 渠道配置                                   │    │
│  │  3. AiQuotaService.checkQuota() 额度校验                            │    │
│  │  4. 保存用户消息 AiChatMessage                                      │    │
│  │  5. 加载上下文消息列表 contextMessages                              │    │
│  │  6. AiModelClient.chat() 模型调用                                   │    │
│  │  7. 保存 AI 回复消息                                                │    │
│  │  8. AiQuotaService.recordUsage() 额度扣减                          │    │
│  │  9. AiUsageLogService.logUsage() 调用日志                          │    │
│  │  10. 更新会话 lastMessageAt                                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────────┐
│    AiQuotaService       │ │     AiModelClient       │ │    AiUsageLogService    │
│       额度服务            │ │      模型调用            │ │      日志服务            │
│  ┌───────────────────┐  │ │  ┌───────────────────┐  │ │                         │
│  │ checkQuota()      │  │ │  │ buildMessages()   │  │ │  logUsage()             │
│  │ recordUsage()     │  │ │  │ trimContext()     │  │ │                         │
│  └───────────────────┘  │ │  │ buildModel()      │  │ │                         │
└─────────────────────────┘ │ │  └───────────────────┘  │ └─────────────────────────┘
              │            │ └─────────────────────────┘             │
              │            │                   │                     │
              ▼            │                   ▼                     ▼
┌─────────────────────────┐│         ┌─────────────────────────────────────┐
│       Redis             ││         │         LangChain4j                │
│    额度计数器            ││         │    OpenAiChatModel (每次 new)       │
│   TTL 当日午夜           ││         └─────────────────────────────────────┘
└─────────────────────────┘│                   │
              │            │                   ▼
              └────────────┴──────────┌─────────────────────────┐
                                      │    第三方 AI API         │
                                      │  OpenAI / DeepSeek      │
                                      └─────────────────────────┘
```

## 2. 额度校验流程

```
AiQuotaService.checkQuota(userId, config)
│
├─1─ 全局开关校验
│         │
│         ▼
│    SysConfigService.getValue("ai:global:enabled")
│         │
│         ├─ "true"  → 继续
│         └─ "false" → 抛出 AI_GLOBAL_DISABLED
│
├─2─ 平台每日额度校验
│         │
│         ▼
│    Redis Counter: ai:quota:platform:{yyyy-MM-dd}
│         │
│         ├─ 已用量 < 平台额度 → 继续
│         └─ 已用量 >= 平台额度 → 抛出 AI_QUOTA_PLATFORM_EXCEEDED
│
└─3─ 用户每日额度校验
          │
          ▼
    computeEffectiveLimit(userId, config)
    │
    ├─ 获取用户等级 levelConfig.aiDailyQuota
    ├─ 获取渠道配置 userDailyQuota
    └─ 取较小值作为有效额度
          │
          ▼
    Redis Counter: ai:quota:user:{userId}:{yyyy-MM-dd}
          │
          ├─ 已用量 < 有效额度 → 校验通过
          └─ 已用量 >= 有效额度 → 抛出 AI_QUOTA_EXCEEDED
```

## 3. 模型调用流程

```
AiModelClient.chat(config, systemPrompt, contextMessages, userQuestion)
│
├─1─ 构建消息列表
│         │
│         ▼
│    buildMessages(config, systemPrompt, contextMessages, userQuestion)
│         │
│         ├─ convertHistory() 将 AiChatMessage 转为 ChatMessage
│         ├─ trimContext() 按 maxContextTokens 裁剪历史
│         ├─ 添加 SystemMessage (systemPrompt)
│         ├─ 添加历史消息 (UserMessage / AiMessage)
│         └─ 添加当前提问 (UserMessage)
│
├─2─ 构建模型实例
│         │
│         ▼
│    LangChain4jConfig.buildModel(config)
│         │
│         ├─ apiBaseUrl    → config.getApiBaseUrl()
│         ├─ apiKey        → config.getApiKeyEncrypted()
│         ├─ modelName     → config.getModelName() (默认 deepseek-chat)
│         ├─ temperature   → AiConstants.DEFAULT_TEMPERATURE
│         ├─ maxTokens     → AiConstants.DEFAULT_MAX_TOKENS
│         └─ timeout       → AiConstants.DEFAULT_TIMEOUT_SECONDS
│
├─3─ 执行调用
│         │
│         ▼
│    OpenAiChatModel.chat(messages)
│         │
│         ├─ 成功 → mapResponse() 封装 AiModelCallResult
│         └─ 失败 → 捕获异常，返回 success=false + errorMessage
│
└─4─ 返回结果
          │
          ▼
    AiModelCallResult
    ├─ success: boolean
    ├─ content: String (AI 回复)
    ├─ requestTokens: Integer
    ├─ responseTokens: Integer
    ├─ totalTokens: Integer
    └─ errorMessage: String (失败时)
```

## 4. 上下文裁剪策略

```
trimContext(history, maxContextTokens)
│
├─ 估算当前 token 数
│     totalChars = sum(message.text.length)
│     estimatedTokens = totalChars / 2
│
├─ 如果 estimatedTokens <= maxContextTokens
│     └─ 直接返回，不裁剪
│
└─ 如果 estimatedTokens > maxContextTokens
      │
      └─ 从头部（最老消息）开始裁剪
         │
         ├─ 移除第 0 条消息
         ├─ 重新估算 token 数
         └─ 循环直到满足限制
```

## 5. 会话创建流程

```
POST /api/user/ai/sessions (AiSessionCreateRequest)
│
├─1─ 确定渠道配置
│         │
│         ▼
│    if (request.channelConfigId != null) {
│        config = AiChannelConfigRepository.getById(id)
│    } else {
│        // 使用默认渠道
│        channels = AiChannelConfigRepository.listEnabledOrderByDefault()
│        config = channels.get(0)
│    }
│
├─2─ 校验渠道状态
│         │
│         ▼
│    config.status == 1 (启用) ? 继续 : 抛出 AI_CHANNEL_DISABLED
│
├─3─ 额度预检
│         │
│         ▼
│    AiQuotaService.checkQuota(userId, config)
│
└─4─ 创建会话
          │
          ▼
    AiChatSession
    ├─ userId        ← 当前用户
    ├─ channelConfigId ← 选定渠道
    ├─ title         ← 请求中的 title
    ├─ sceneType     ← SCENE_TYPE_GENERAL 或请求值
    ├─ status        ← NORMAL (1)
    └─ lastMessageAt ← now()
```

## 6. 调用日志记录

```
AiUsageLogService.logUsage(...)
│
├─ 构建 AiUsageLog
│     ├─ userId
│     ├─ channelConfigId
│     ├─ sessionId
│     ├─ requestSceneType
│     ├─ requestTokens
│     ├─ responseTokens
│     ├─ totalTokens
│     ├─ quotaCost (根据 totalTokens 计算)
│     ├─ successStatus
│     └─ errorCode
│
└─ AiUsageLogRepository.save(log)
```

## 7. 关键文件索引

| 文件 | 职责 |
|------|------|
| `AiChatController.java` | 用户侧接口入口 |
| `AiChatServiceImpl.java` | 业务编排：会话/消息/上下文 |
| `AiQuotaServiceImpl.java` | 额度校验与扣减 |
| `AiModelClientImpl.java` | 模型调用：消息组装/裁剪 |
| `LangChain4jConfig.java` | 底层模型实例构建 |
| `AiUsageLogServiceImpl.java` | 调用日志记录 |
| `AiChannelConfigRepository.java` | 渠道配置数据访问 |
| `AiChatSessionRepository.java` | 会话数据访问 |
| `AiChatMessageRepository.java` | 消息数据访问 |
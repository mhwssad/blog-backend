# AI 额度体系

## 1. 额度架构总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           额度控制层级                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    第一层：全局开关                                  │   │
│  │                 SysConfig: ai:global:enabled                        │   │
│  │                 true = 开放 | false = 完全关闭                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│                                    ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    第二层：平台每日额度                               │   │
│  │     Redis: ai:quota:platform:{yyyy-MM-dd}                            │   │
│  │     全平台所有用户共享的每日调用上限                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│                                    ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    第三层：用户每日额度                               │   │
│  │     Redis: ai:quota:user:{userId}:{yyyy-MM-dd}                      │   │
│  │     effectiveLimit = min(用户等级额度, 渠道配置额度)                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. 额度校验流程

```
用户发起 AI 请求
         │
         ▼
┌─────────────────────┐
│  checkQuota()       │
│  额度校验入口        │
└─────────────────────┘
         │
         ▼
    ┌─────────────────────────┐
    │ 全局开关 ai:global:enabled │
    │ "false" → 抛出 GLOBAL_DISABLED │
    │ "true"  → 继续          │
    └─────────────────────────┘
         │
         ▼
    ┌─────────────────────────┐
    │ 平台额度 SysConfig        │
    │ AI_PLATFORM_DAILY_QUOTA │
    │                        │
    │ Redis Counter:         │
    │ ai:quota:platform:     │
    │     {yyyy-MM-dd}       │
    │                        │
    │ 已用量 >= 额度 ?        │
    │   是 → PLATFORM_EXCEEDED │
    │   否 → 继续           │
    └─────────────────────────┘
         │
         ▼
    ┌─────────────────────────┐
    │ 用户额度 computeEffectiveLimit │
    └─────────────────────────┘
         │
    ┌────┴────────────────────┐
    │                         │
    ▼                         ▼
┌─────────────┐      ┌─────────────┐
│ 用户等级额度 │      │ 渠道额度     │
│ LevelConfig │      │ userDailyQuota │
│ .aiDailyQuota│     │              │
└─────────────┘      └─────────────┘
         │                 │
         └───────┬─────────┘
                 ▼
         effectiveLimit = min(等级额度, 渠道额度)
         渠道额度=0 表示不限制，取等级额度
                 │
                 ▼
    ┌─────────────────────────┐
    │ Redis Counter:          │
    │ ai:quota:user:          │
    │   {userId}:{yyyy-MM-dd} │
    │                        │
    │ 已用量 >= effectiveLimit?│
    │   是 → QUOTA_EXCEEDED  │
    │   否 → 校验通过        │
    └─────────────────────────┘
                 │
                 ▼
          调用模型成功
```

## 3. 等级额度配置

```java
// LevelConfig.java
public enum LevelConfig {
    LV0(0, 5),   // 游客/未登录 每日5次
    LV1(1, 10),  // Lv1 每日10次
    LV2(2, 20),  // Lv2 每日20次
    LV3(3, 50),  // Lv3 每日50次
    LV4(4, 100), // Lv4 每日100次
    LV5(5, 200); // Lv5 每日200次
}
```

| 等级 | 日额度 |
|------|--------|
| LV0 | 5 |
| LV1 | 10 |
| LV2 | 20 |
| LV3 | 50 |
| LV4 | 100 |
| LV5 | 200 |

## 4. 额度扣减流程

```
recordUsage(userId, channelConfigId)
│
├─ 平台计数器 +1
│     key = ai:quota:platform:{yyyy-MM-dd}
│     TTL  = 当日午夜
│
└─ 用户计数器 +1
          key = ai:quota:user:{userId}:{yyyy-MM-dd}
          TTL  = 当日午夜
```

## 5. Redis Key 设计

| Key Pattern | 用途 | TTL |
|-------------|------|-----|
| `ai:quota:platform:{date}` | 平台每日总量计数器 | 当日午夜 |
| `ai:quota:user:{userId}:{date}` | 用户每日计数器 | 当日午夜 |

## 6. 配额查询

```
GET /api/user/ai/sessions/quota
│
└─ getUserQuotaForDefaultChannel(userId)
          │
          ├─ 获取默认渠道配置
          ├─ 获取用户等级 aiDailyQuota
          ├─ 获取渠道 userDailyQuota
          ├─ 计算 effectiveLimit = min(level, channel)
          ├─ 查询 Redis Counter
          └─ 返回 AiQuotaVO
              {
                  dailyLimit: 20,
                  usedToday: 5,
                  remainingToday: 15
              }
```

## 7. 异常场景

| 错误码 | 异常 | 触发条件 |
|--------|------|----------|
| `AI_GLOBAL_DISABLED` | AI服务已关闭 | 全局开关为 false |
| `AI_QUOTA_PLATFORM_EXCEEDED` | 平台额度已用完 | 平台每日总量超限 |
| `AI_QUOTA_EXCEEDED` | 您的额度已用完 | 用户每日额度超限 |
| `AI_CHANNEL_DISABLED` | 渠道已停用 | 渠道状态非启用 |
| `AI_CHANNEL_NOT_FOUND` | 渠道不存在 | 渠道ID无效或未找到 |

## 8. 关键文件

| 文件 | 职责 |
|------|------|
| `AiQuotaService.java` | 额度服务接口 |
| `AiQuotaServiceImpl.java` | 额度校验/扣减/查询实现 |
| `LevelConfig.java` | 用户等级额度配置枚举 |
| `ResultErrorCode.java` | 额度相关错误码定义 |
| `RedisOperator.java` | Redis 计数器操作 |
| `SysConfigService.java` | 全局开关读取 |
# Auth 经验等级体系

## 1. 概述

经验等级体系是用户成长系统的核心，通过经验值（XP）驱动用户活跃度：

- **经验值获取**：发帖、评论、点赞等行为可获得经验值
- **等级计算**：基于经验值自动计算用户等级（1-10级）
- **等级权益**：不同等级享有不同权益（如发文限制、功能解锁）

## 2. 核心数据模型

### 2.1 用户表扩展字段

| 字段 | 类型 | 说明 |
|------|------|------|
| userLevel | Integer | 用户等级（1-10） |
| experiencePoints | Integer | 当前经验值 |
| levelUpdatedAt | LocalDateTime | 最近等级变更时间 |

### 2.2 经验流水表 (user_experience_log)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户ID |
| sourceType | String | 经验来源类型 |
| sourceBizId | String | 来源业务ID |
| xpValue | Integer | 经验值变化量 |
| idempotentKey | String | 幂等键（防重复） |
| logDate | LocalDate | 日志日期 |
| createdAt | LocalDateTime | 创建时间 |

## 3. 经验来源

### 3.1 来源类型 (ExperienceSourceTypeEnum)

| 来源类型 | 说明 | 经验值 |
|----------|------|--------|
| POST_ARTICLE | 发布文章 | 固定值 |
| POST_COMMENT | 发布评论 | 固定值 |
| RECEIVE_LIKE | 收到点赞 | 固定值 |
| RECEIVE_COLLECT | 收到收藏 | 固定值 |
| RECEIVE_FOLLOW | 收到关注 | 固定值 |
| DAILY_LOGIN | 每日登录 | 固定值 |
| ADMIN_ADJUST | 管理员调整 | 可变值 |

### 3.2 经验常量 (ExperienceConstants)

```java
public static class SourceType {
    public static final String POST_ARTICLE = "post_article";
    public static final String POST_COMMENT = "post_comment";
    public static final String RECEIVE_LIKE = "receive_like";
    public static final String RECEIVE_COLLECT = "receive_collect";
    public static final String RECEIVE_FOLLOW = "receive_follow";
    public static final String DAILY_LOGIN = "daily_login";
    public static final String ADMIN_ADJUST = "admin_adjust";
}

public static class DefaultXp {
    public static final int POST_ARTICLE = 10;
    public static final int POST_COMMENT = 2;
    public static final int RECEIVE_LIKE = 1;
    public static final int RECEIVE_COLLECT = 3;
    public static final int RECEIVE_FOLLOW = 5;
    public static final int DAILY_LOGIN = 1;
}
```

## 4. 等级计算

### 4.1 等级计算器 (LevelCalculator)

```java
public class LevelCalculator {
    /**
     * 根据经验值计算等级
     *
     * @param experiencePoints 经验值
     * @return 等级（1-10）
     */
    public int calculateLevel(int experiencePoints) {
        if (experiencePoints < 100) return 1;
        if (experiencePoints < 300) return 2;
        if (experiencePoints < 600) return 3;
        if (experiencePoints < 1000) return 4;
        if (experiencePoints < 1500) return 5;
        if (experiencePoints < 2100) return 6;
        if (experiencePoints < 2800) return 7;
        if (experiencePoints < 3600) return 8;
        if (experiencePoints < 4500) return 9;
        return 10;
    }

    /**
     * 获取当前等级所需下一级经验值
     */
    public int getNextLevelThreshold(int level) { ... }

    /**
     * 获取当前等级进度百分比
     */
    public double getLevelProgress(int experiencePoints) { ... }
}
```

### 4.2 等级阈值配置

| 等级 | 最低经验值 | 下一级所需 |
|------|-----------|-----------|
| 1 | 0 | 100 |
| 2 | 100 | 200 |
| 3 | 300 | 300 |
| 4 | 600 | 400 |
| 5 | 1000 | 500 |
| 6 | 1500 | 600 |
| 7 | 2100 | 700 |
| 8 | 2800 | 800 |
| 9 | 3600 | 900 |
| 10 | 4500 | MAX |

## 5. 事件驱动架构

### 5.1 XpAwardEvent

经验值发放通过 Spring Event 异步处理：

```java
public class XpAwardEvent extends ApplicationEvent {
    private final Long userId;
    private final String sourceType;
    private final String sourceBizId;
    private final int xpValue;
    private final String idempotentKey;
}
```

### 5.2 事件处理流程

```
业务操作（如发帖）
    │
    ▼
发布 XpAwardEvent 事件
    │
    ▼
XpAwardEventListener 监听器
    │
    ├──► 校验幂等性（idempotentKey）
    ├──► 写入 user_experience_log
    ├──► 原子递增 sys_user.experience_points
    ├──► 检查是否需要升级
    │      └──► 若升级，更新 sys_user.user_level 和 level_updated_at
    └──► 返回处理结果
```

### 5.3 幂等性保证

```java
// Redis Key 设计
Key: xp:idempotent:{idempotentKey}
TTL: 24小时

// 处理逻辑
if (redis.setIfAbsent(key, "1", 24h)) {
    // 首次处理，执行经验值发放
} else {
    // 重复请求，直接返回
}
```

## 6. 接口列表

### 6.1 用户侧接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/experience/level-info` | GET | 获取当前用户等级信息 |

### 6.2 管理端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/experience/logs/page` | GET | 分页查询经验日志 |
| `/api/admin/experience/summary` | GET | 获取用户经验汇总 |
| `/api/admin/experience/adjust-xp` | POST | 调整用户经验值 |
| `/api/admin/experience/adjust-level` | POST | 调整用户等级 |
| `/api/admin/experience/source-config` | GET | 获取经验来源配置 |

### 6.3 经验日志查询字段

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| sourceType | String | 来源类型 |
| sourceBizId | String | 来源业务ID |
| xpValue | Integer | 经验值变化量 |
| logDate | LocalDate | 日志日期 |
| createdAt | LocalDateTime | 创建时间 |

## 7. 服务层组件

### 7.1 UserExperienceService

| 方法 | 说明 |
|------|------|
| awardExperience(event) | 处理经验入账事件 |
| getUserLevel(userId) | 获取用户当前等级 |
| getLevelInfo(userId) | 获取用户等级展示信息 |
| checkLevelPermission(userId, requiredLevel) | 检查用户等级是否满足门槛 |

### 7.2 ExperienceAdminService

| 方法 | 说明 |
|------|------|
| pageExperienceLogs(query) | 分页查询经验日志 |
| getExperienceSummary(userId) | 获取用户经验汇总 |
| adjustExperience(userId, delta, reason) | 调整经验值 |
| adjustLevel(userId, level, reason) | 调整用户等级 |

## 8. 关键设计

### 8.1 原子性保证

经验值递增使用数据库原子操作：

```sql
UPDATE sys_user
SET experience_points = experience_points + #{delta}
WHERE id = #{userId}
```

### 8.2 事务边界

经验值发放与业务操作在统一事务边界内：

```java
@Transactional(rollbackFor = Exception.class)
public void awardExperience(XpAwardEvent event) {
    // 1. 写入经验流水
    // 2. 递增用户经验值
    // 3. 检查并更新等级
}
```

### 8.3 等级权益示例

| 等级 | 每日发文限制 | 附件上传 | 高级编辑器 |
|------|-------------|----------|-----------|
| 1 | 3 | ❌ | ❌ |
| 2 | 5 | ❌ | ❌ |
| 3 | 8 | ✅ | ❌ |
| 4 | 10 | ✅ | ❌ |
| 5 | 15 | ✅ | ✅ |
| 6+ | 不限 | ✅ | ✅ |

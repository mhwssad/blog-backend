# Article 状态流转

## 1. 状态机概述

Article 模块通过 `ArticleStatusMachine` 组件统一管理文章状态、审核状态和可见范围的合法性校验与状态推导。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Article 状态机                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐                                                        │
│  │   ArticleStatus │  文章状态：0-草稿，1-已发布，2-已下架                    │
│  └────────┬────────┘                                                        │
│           │                                                                 │
│  ┌────────▼────────┐                                                        │
│  │ArticleReviewStatus│ 审核状态：0-未送审/免审，1-审核中，2-审核通过，3-拒绝  │
│  └────────┬────────┘                                                        │
│           │                                                                 │
│  ┌────────▼────────┐                                                        │
│  │VisibilityScope  │ 可见范围：0-公开，1-仅自己，2-白名单，3-登录可见         │
│  └────────┬────────┘                                                        │
│           │                                                                 │
│  ┌────────▼────────┐                                                        │
│  │ AccessLevel     │ 访问级别：0-公开，1-登录，2-密码，3-白名单               │
│  └─────────────────┘                                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. 核心状态定义

### 2.1 文章状态 (ArticleStatus)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | PUBLISHED | 已发布 |
| 2 | OFFLINE | 已下架 |

### 2.2 审核状态 (ArticleReviewStatus)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | NOT_SUBMITTED | 未送审/免审 |
| 1 | REVIEWING | 审核中 |
| 2 | APPROVED | 审核通过 |
| 3 | REJECTED | 审核拒绝 |

### 2.3 可见范围 (VisibilityScope)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | PUBLIC | 公开 |
| 1 | SELF_ONLY | 仅自己可见 |
| 2 | WHITELIST | 白名单可见 |
| 3 | LOGIN_REQUIRED | 登录可见 |

## 3. 状态流转图

```
                                    ┌─────────────────┐
                                    │     草稿        │
                                    │   (status=0)    │
                                    │ review_status=0 │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
           ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
           │    送审         │      │   直接发布       │      │   定时发布       │
           │  送审 (status=0)│      │   (status=1)    │      │   (status=0)    │
           │ review_status=1 │      │ review_status=0 │      │ review_status=0 │
           └────────┬────────┘      └────────┬────────┘      └────────┬────────┘
                    │                        │                        │
           ┌────────┴────────┐              │                        │
           │                 │              │                        │
           ▼                 ▼              │                        │
    ┌─────────────┐   ┌─────────────┐       │                        │
    │   审核通过   │   │   审核拒绝   │       │                        │
    │review_status=2│   │review_status=3│    │                        │
    └──────┬───────┘   └─────────────┘       │                        │
           │                                 │                        │
           │                                 │                        │
           └─────────────┬───────────────────┘                        │
                         │                                            │
                         ▼                                            ▼
                ┌─────────────────┐                        ┌─────────────────┐
                │   已发布        │◀──────────────────────│   定时时间到     │
                │   (status=1)   │                        │   自动发布       │
                │ review_status=2 │                        │                 │
                └────────┬────────┘                        └─────────────────┘
                         │
                         │
                         ▼
                ┌─────────────────┐
                │    下架         │
                │   (status=2)   │
                └─────────────────┘
```

## 4. 状态机核心方法

### 4.1 状态校验

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleStatusMachine.java#L20-45
/**
 * 校验并归一化文章状态。
 */
public Integer normalizeStatus(Integer status) {
    int actualStatus = CollectionUtils.defaultInt(status);
    ExceptionThrowerCore.throwBusinessIf(
            actualStatus < 0 || actualStatus > 2,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "文章状态非法");
    return actualStatus;
}

/**
 * 校验并归一化审核状态。
 */
public Integer normalizeReviewStatus(Integer reviewStatus) {
    int actualReviewStatus = reviewStatus == null
            ? ArticleReviewStatusEnum.NOT_SUBMITTED.getValue()
            : reviewStatus;
    ExceptionThrowerCore.throwBusinessIf(
            !ArticleReviewStatusEnum.contains(actualReviewStatus),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "文章审核状态非法");
    return actualReviewStatus;
}
```

### 4.2 保存时状态推导

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleStatusMachine.java#L47-77
/**
 * 保存时校验并推导最终状态。
 */
public void validateSaveState(Integer status,
                              Integer reviewStatus,
                              Integer visibilityScope,
                              Integer accessLevel,
                              LocalDateTime scheduledPublishTime) {
    // ... 校验各状态合法性
    // 定时发布时不能是已下架状态
    // 定时发布时审核状态不能是审核中或审核拒绝
}

/**
 * 推导保存后的状态。
 * 如果请求发布且设置了未来定时时间，则实际状态为草稿。
 */
public Integer resolveStatusForSave(Integer requestedStatus, LocalDateTime scheduledPublishTime) {
    int actualStatus = normalizeStatus(requestedStatus);
    if (actualStatus == 1 && isScheduledForFuture(scheduledPublishTime, LocalDateTime.now())) {
        return 0; // 草稿
    }
    return actualStatus;
}
```

### 4.3 发布状态判断

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleStatusMachine.java#L107-123
/**
 * 判断文章是否已发布供普通用户查看。
 */
public boolean isPublishedForNormalUsers(BlogArticle article, LocalDateTime now) {
    if (article == null) {
        return false;
    }
    // 状态必须为已发布
    if (!Integer.valueOf(1).equals(normalizeStatus(article.getStatus()))) {
        return false;
    }
    // 审核状态不能是审核中或审核拒绝
    Integer reviewStatus = normalizeReviewStatus(article.getReviewStatus());
    if (reviewStatus.equals(ArticleReviewStatusEnum.REVIEWING.getValue())
            || reviewStatus.equals(ArticleReviewStatusEnum.REJECTED.getValue())) {
        return false;
    }
    // 定时发布时间已过
    return !isScheduledForFuture(article.getScheduledPublishTime(), now);
}
```

## 5. 审核流程

### 5.1 用户送审

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/UserArticleReviewService.java#L25-28
/**
 * 用户提交文章进行审核。
 */
void submitForReview(Long articleId);
```

流程：
1. 校验文章存在且属于当前用户
2. 校验文章状态为草稿
3. 校验审核状态为未送审
4. 更新审核状态为审核中
5. 记录审核日志

### 5.2 后台审核

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleReviewAdminService.java#L18-28
/**
 * 审核通过。
 */
void approve(Long articleId, String comment, Long operatorId, String ip, String ua);

/**
 * 审核拒绝。
 */
void reject(Long articleId, String reason, Long operatorId, String ip, String ua);
```

流程：
- **审核通过**：review_status → 2（审核通过），如果设置了定时发布则按定时规则处理
- **审核拒绝**：review_status → 3（审核拒绝），状态保持草稿

### 5.3 用户撤回

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/UserArticleReviewService.java#L30-31
/**
 * 用户撤回待审核文章。
 */
void cancelReview(Long articleId);
```

流程：
1. 校验文章审核状态为审核中
2. 更新审核状态为未送审
3. 记录审核日志

## 6. 定时发布

### 6.1 定时发布调度任务

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/task/ScheduledPublishTask.java#L1-30
/**
 * 定时发布任务。
 * 每分钟执行一次，查询待发布文章并发布。
 */
@Scheduled(cron = "0 * * * * ?")
public void processScheduledPublish() {
    LocalDateTime now = LocalDateTime.now();
    List<BlogArticle> articles = blogArticleRepository.listReadyForScheduledPublish(now, 100);
    for (BlogArticle article : articles) {
        // 发布文章
    }
}
```

### 6.2 发布条件

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleStatusMachine.java#L129-139
/**
 * 判断文章是否等待定时发布。
 */
public boolean isAwaitingScheduledPublish(BlogArticle article, LocalDateTime now) {
    if (article == null || article.getScheduledPublishTime() == null) {
        return false;
    }
    // 状态为草稿、定时时间已到、审核状态不是审核中或审核拒绝
    return Integer.valueOf(0).equals(normalizeStatus(article.getStatus()))
            && !article.getScheduledPublishTime().isAfter(now)
            && !reviewStatus.equals(ArticleReviewStatusEnum.REVIEWING.getValue())
            && !reviewStatus.equals(ArticleReviewStatusEnum.REJECTED.getValue());
}
```

## 7. 状态联动规则

| 场景 | 状态 | 审核状态 | 可见范围 | 访问级别 | 说明 |
|------|------|---------|---------|---------|------|
| 新建草稿 | 0 | 0 | 0 | 0 | 默认值 |
| 直接发布 | 1 | 0 | 0 | 0 | 免审发布 |
| 送审待审 | 0 | 1 | - | - | 审核中不能发布 |
| 审核通过 | - | 2 | - | - | 审核通过后按原状态 |
| 审核拒绝 | 0 | 3 | - | - | 退回草稿 |
| 定时发布 | 0 | 0/2 | - | - | 到时间自动发布 |
| 下架 | 2 | - | - | - | 停止展示 |
| 公开列表可见 | 1 | 0/2 | 0 | 0 | 需同时满足 |

## 8. 审核日志

所有审核操作都会记录到 `blog_article_review_log` 表：

| 字段 | 说明 |
|------|------|
| action_type | 操作类型：submit/review/approve/reject/cancel |
| previous_status | 操作前状态 |
| new_status | 操作后状态 |
| review_comment | 审核备注/拒绝原因 |
| ip | 操作人 IP |
| user_agent | 操作人 User-Agent |

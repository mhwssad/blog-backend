# Article 数据模型

## 1. 数据库表结构

### 1.1 文章主表 (blog_article)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 ID |
| title | VARCHAR(200) | 文章标题 |
| summary | TEXT | 文章摘要 |
| content | LONGTEXT | 文章内容（Markdown 格式） |
| cover_image | VARCHAR(500) | 封面图片 URL |
| author_id | BIGINT | 作者 ID |
| is_top | TINYINT | 是否置顶：0-否，1-是 |
| is_recommend | TINYINT | 是否推荐：0-否，1-是 |
| is_original | TINYINT | 是否原创：0-转载，1-原创 |
| source_url | VARCHAR(500) | 转载来源 URL |
| status | TINYINT | 状态：0-草稿，1-已发布，2-已下架 |
| review_status | TINYINT | 审核状态：0-未送审/免审，1-审核中，2-审核通过，3-审核拒绝 |
| publish_time | DATETIME | 发布时间 |
| scheduled_publish_time | DATETIME | 定时发布时间 |
| access_level | TINYINT | 访问级别：0-公开，1-登录可见，2-密码访问，3-指定用户 |
| visibility_scope | TINYINT | 可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见 |
| view_count | INT | 浏览数 |
| like_count | INT | 点赞数 |
| comment_count | INT | 评论数 |
| collect_count | INT | 收藏数 |
| share_count | INT | 分享数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| remark | VARCHAR(500) | 备注 |

### 1.2 文章访问授权表 (blog_article_access)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 ID |
| article_id | BIGINT | 文章 ID |
| user_id | BIGINT | 授权用户 ID |
| access_type | TINYINT | 授权类型：1-密码访问，2-指定用户 |
| grant_time | DATETIME | 授权时间 |
| expire_time | DATETIME | 过期时间 |
| grant_reason | VARCHAR(255) | 授权原因 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 1.3 文章分类关联表 (blog_article_category)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 ID |
| article_id | BIGINT | 文章 ID |
| category_id | BIGINT | 分类 ID |
| sort_order | INT | 排序顺序 |

### 1.4 文章审核日志表 (blog_article_review_log)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 ID |
| article_id | BIGINT | 文章 ID |
| operator_id | BIGINT | 操作人 ID |
| action_type | VARCHAR(20) | 操作类型：submit/review/approve/reject/cancel |
| previous_status | INT | 操作前状态 |
| new_status | INT | 操作后状态 |
| review_comment | TEXT | 审核备注/拒绝原因 |
| ip | VARCHAR(50) | 操作 IP |
| user_agent | VARCHAR(500) | 用户代理 |
| created_at | DATETIME | 创建时间 |

### 1.5 文章系列表 (blog_article_series)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 系列 ID（自增） |
| owner_user_id | BIGINT | 创建人 ID |
| title | VARCHAR(200) | 系列标题 |
| description | TEXT | 系列描述 |
| cover_image | VARCHAR(500) | 封面图 |
| status | TINYINT | 状态：0-停用，1-正常 |
| visibility_scope | TINYINT | 可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见 |
| article_count | INT | 文章数 |
| sort_order | INT | 排序值 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 1.6 系列文章项表 (blog_article_series_item)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 ID |
| series_id | BIGINT | 系列 ID |
| article_id | BIGINT | 文章 ID |
| sort_order | INT | 排序顺序 |
| created_at | DATETIME | 创建时间 |

## 2. 实体类映射

| 表名 | 实体类 | 所在包 |
|------|--------|--------|
| blog_article | BlogArticle | domain.article |
| blog_article_access | BlogArticleAccess | domain.article |
| blog_article_category | BlogArticleCategory | domain.article |
| blog_article_review_log | BlogArticleReviewLog | domain.article |
| blog_article_series | BlogArticleSeries | domain.article |
| blog_article_series_item | BlogArticleSeriesItem | domain.article |

## 3. 数据流关系

```
┌─────────────────┐     ┌─────────────────┐
│   SysCategory   │────▶│ BlogArticle     │
│   分类表        │     │  Category       │
└─────────────────┘     └───────┬─────────┘
                               │
                               ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   SysTag        │────▶│ SysTagRelation  │◀────│   标签关联       │
│   标签表        │     │  标签关联表      │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐
│ BlogArticle     │────▶│ BlogArticle     │
│ Series          │     │ SeriesItem      │
│ 系列主表        │     │ 系列文章项       │
└─────────────────┘     └────────┬─────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │ BlogArticle     │
                        │ 文章主表        │
                        └─────────────────┘

┌─────────────────┐     ┌─────────────────┐
│ BlogArticle     │────▶│ BlogArticle     │
│ 文章主表        │     │ Access          │
│                 │     │ 访问授权        │
└─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐
│ BlogArticle     │────▶│ BlogArticle     │
│ 文章主表        │     │ ReviewLog       │
│                 │     │ 审核日志        │
└─────────────────┘     └─────────────────┘
```

## 4. 枚举值定义

### 4.1 文章状态 (ArticleStatusEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | PUBLISHED | 已发布 |
| 2 | OFFLINE | 已下架 |

### 4.2 审核状态 (ArticleReviewStatusEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | NOT_SUBMITTED | 未送审/免审 |
| 1 | REVIEWING | 审核中 |
| 2 | APPROVED | 审核通过 |
| 3 | REJECTED | 审核拒绝 |

### 4.3 可见范围 (ArticleVisibilityScopeEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | PUBLIC | 公开 |
| 1 | SELF_ONLY | 仅自己可见 |
| 2 | WHITELIST | 白名单可见 |
| 3 | LOGIN_REQUIRED | 登录可见 |

### 4.4 访问级别 (ArticleAccessLevelEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | PUBLIC | 公开（无限制） |
| 1 | LOGIN_REQUIRED | 登录可见 |
| 2 | PASSWORD_PROTECTED | 密码访问 |
| 3 | WHITELIST | 指定用户白名单 |
| 4 | - | （保留） |

### 4.5 审核操作类型 (ReviewActionTypeEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| submit | SUBMIT | 提交审核 |
| review | REVIEW | 开始审核 |
| approve | APPROVE | 审核通过 |
| reject | REJECT | 审核拒绝 |
| cancel | CANCEL | 撤回审核 |

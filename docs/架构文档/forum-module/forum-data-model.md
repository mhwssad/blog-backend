# Forum 数据模型

## 1. 数据库表结构

### 1.1 论坛版块表 (forum_section)

```sql
CREATE TABLE forum_section (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL COMMENT '版块名称',
    description     VARCHAR(512) COMMENT '版块简介',
    sort_order      INT          DEFAULT 0 COMMENT '排序，数值越小越靠前',
    visibility_scope TINYINT      DEFAULT 0 COMMENT '可见范围：0-公开，1-登录可见',
    post_level_limit INT          DEFAULT 0 COMMENT '发帖最低等级',
    status          TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛版块表';
```

### 1.2 论坛帖子表 (forum_post)

```sql
CREATE TABLE forum_post (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    section_id      BIGINT       NOT NULL COMMENT '版块ID',
    author_id       BIGINT       NOT NULL COMMENT '作者ID',
    title           VARCHAR(256) NOT NULL COMMENT '标题',
    content         TEXT         NOT NULL COMMENT '内容',
    status          TINYINT      DEFAULT 0 COMMENT '状态：0-草稿，1-已发布，2-审核中，3-已拒绝，4-已删除',
    visibility_scope TINYINT      DEFAULT 0 COMMENT '可见范围：0-公开，1-登录可见',
    is_top          TINYINT      DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    is_essence      TINYINT      DEFAULT 0 COMMENT '是否精华：0-否，1-是',
    view_count      INT          DEFAULT 0 COMMENT '浏览数',
    like_count      INT          DEFAULT 0 COMMENT '点赞数',
    reply_count     INT          DEFAULT 0 COMMENT '回复数',
    collect_count   INT          DEFAULT 0 COMMENT '收藏数',
    share_count     INT          DEFAULT 0 COMMENT '分享数',
    published_at    DATETIME     COMMENT '发布时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_section_id (section_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_is_top (is_top),
    INDEX idx_is_essence (is_essence),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子表';
```

### 1.3 论坛回复表 (forum_reply)

```sql
CREATE TABLE forum_reply (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    post_id         BIGINT       NOT NULL COMMENT '帖子ID',
    parent_id       BIGINT       DEFAULT 0 COMMENT '父回复ID，0表示根回复',
    root_id         BIGINT       DEFAULT 0 COMMENT '根回复ID，0表示自身为根',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    content         TEXT         NOT NULL COMMENT '回复内容',
    status          TINYINT      DEFAULT 1 COMMENT '状态：1-正常，2-隐藏，3-删除，4-审核中',
    floor_no        INT          DEFAULT 0 COMMENT '楼层号，按帖子内回复创建顺序递增',
    like_count      INT          DEFAULT 0 COMMENT '点赞数',
    reply_count     INT          DEFAULT 0 COMMENT '子回复数',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_root_id (root_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛回复表';
```

### 1.4 帖子与频道关联表 (forum_post_channel_link)

```sql
CREATE TABLE forum_post_channel_link (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    forum_post_id   BIGINT       NOT NULL COMMENT '论坛帖子ID',
    conversation_id BIGINT       NOT NULL COMMENT '频道会话ID',
    link_type       VARCHAR(32)  DEFAULT 'manual_share' COMMENT '关联方式：manual_share',
    linked_by       BIGINT       NOT NULL COMMENT '关联人ID',
    linked_at       DATETIME     COMMENT '关联时间',
    status          TINYINT      DEFAULT 1 COMMENT '状态：0-失效，1-正常',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_forum_post_id (forum_post_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_linked_by (linked_by),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子与频道关联表';
```

## 2. 实体类映射

| 数据库表 | 实体类 | 包路径 |
|----------|--------|--------|
| forum_section | ForumSection | domain.forum |
| forum_post | ForumPost | domain.forum |
| forum_reply | ForumReply | domain.forum |
| forum_post_channel_link | ForumPostChannelLink | domain.forum |

## 3. 数据流关系

```
┌─────────────┐       ┌─────────────┐
│ForumSection │──1:N──│  ForumPost  │
│   版块      │       │    帖子     │
└─────────────┘       └──────┬──────┘
                             │
                    ┌────────┼────────┐
                    │                 │
                    ▼                 ▼
             ┌─────────────┐   ┌─────────────┐
             │ ForumReply  │   │ForumPostCh │
             │    回复      │   │annelLink   │
             └──────┬──────┘   │  分享到频道  │
                    │           └─────────────┘
                    │
                    ▼
             ┌─────────────┐
             │SysInteraction│
             │   点赞       │
             └─────────────┘
                    │
                    ▼
             ┌─────────────┐
             │SysCollection │
             │   收藏       │
             └─────────────┘
```

## 4. 枚举值定义

### 帖子状态 (ForumPostStatusEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | PUBLISHED | 已发布 |
| 2 | PENDING_REVIEW | 审核中 |
| 3 | REJECTED | 已拒绝 |
| 4 | DELETED | 已删除 |
| 5 | HIDDEN | 隐藏 |

### 回复状态 (ForumReplyStatusEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 1 | NORMAL | 正常 |
| 2 | HIDDEN | 隐藏 |
| 3 | DELETED | 删除 |
| 4 | PENDING_REVIEW | 审核中 |

### 可见范围 (ForumVisibilityScopeEnum)

| 值 | 枚举 | 说明 |
|----|------|------|
| 0 | PUBLIC | 公开 |
| 1 | LOGIN_ONLY | 登录可见 |

### 版块可见范围 (ForumSectionVisibilityEnum)

与 ForumVisibilityScopeEnum 共用相同枚举值。

## 5. Repository 层方法

### ForumSectionRepository

| 方法 | 说明 |
|------|------|
| listPublicVisibleSections(Integer visibilityScope) | 查询可见的版块列表 |
| existsById(Long id) | 检查版块是否存在 |

### ForumPostRepository

| 方法 | 说明 |
|------|------|
| pagePublicPosts(query, loginUser, visibleSectionIds) | 分页查询公开帖子 |
| pageUserPosts(authorId, query) | 分页查询用户的帖子 |
| pageAdminPosts(query) | 后台分页查询帖子 |
| listPublicVisibleForRag(limit) | 查询可进入 RAG 知识库的公开帖子 |
| findPublicVisibleForRag(postId) | 查询指定帖子是否可进入 RAG |
| incrementLikeCount/ReplyCount/CollectCount/ShareCount/ViewCount | 计数增量的原子更新 |
| softDeleteById / updateStatusById / updateTopById / updateEssenceById | 状态更新 |

### ForumReplyRepository

| 方法 | 说明 |
|------|------|
| pageRootReplies(postId, current, size) | 分页查询根回复 |
| pageAdminReplies(query) | 后台分页查询回复 |
| listByPostId(postId) | 查询帖子所有回复（用于构建树形） |
| listRepliesByRootIds(rootIds) | 批量查询子回复 |
| countByPostId(postId) | 统计回复数 |
| nextFloorNo(postId) | 获取下一个楼层号 |
| softDeleteById / updateStatusById | 状态更新 |

### ForumPostChannelLinkRepository

| 方法 | 说明 |
|------|------|
| getByForumPostId(forumPostId) | 查询帖子关联的频道 |
| updateStatusByForumPostId(forumPostId, status) | 更新关联状态 |
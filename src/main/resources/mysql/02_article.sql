CREATE DATABASE IF NOT EXISTS blog_backend CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
use blog_backend;
DROP TABLE IF EXISTS blog_article;
CREATE TABLE blog_article
(
    id            BIGINT AUTO_INCREMENT COMMENT '文章ID（主键）',
    title         VARCHAR(128)                       NOT NULL COMMENT '文章标题',
    summary       TEXT NULL COMMENT '文章摘要（SEO/列表展示）',
    content       LONGTEXT NULL COMMENT '文章内容',
    cover_image   VARCHAR(512) NULL COMMENT '封面图片',
    author_id     BIGINT                             NOT NULL COMMENT '作者ID',
    is_top        TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否置顶：0-否，1-是',
    is_original   TINYINT  DEFAULT 1                 NOT NULL COMMENT '是否原创：0-转载，1-原创',
    source_url    VARCHAR(512) NULL COMMENT '原文链接（转载用）',
    status        TINYINT  DEFAULT 0                 NOT NULL COMMENT '状态：0-草稿，1-已发布，2-已下架',
    publish_time  DATETIME NULL COMMENT '发布时间（草稿为NULL）',
    access_level  TINYINT  DEFAULT 0                 NOT NULL COMMENT '访问级别：0-公开，1-登录可见，2-付费可见，3-VIP可见，4-指定用户可见',
    view_count    INT      DEFAULT 0                 NOT NULL COMMENT '浏览量',
    like_count    INT      DEFAULT 0                 NOT NULL COMMENT '点赞数',
    comment_count INT      DEFAULT 0                 NOT NULL COMMENT '评论数',
    collect_count INT      DEFAULT 0                 NOT NULL COMMENT '收藏数',
    share_count   INT      DEFAULT 0                 NOT NULL COMMENT '分享数',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark        VARCHAR(256) NULL COMMENT '备注',
    PRIMARY KEY (id),
    INDEX         idx_author_status_publish (author_id, status, publish_time DESC) COMMENT '作者文章列表（含发布时间）',
    INDEX         idx_core_query (status, is_top, publish_time DESC) COMMENT '核心列表查询：状态+置顶+发布时间',
    INDEX         idx_access_level_status (access_level, status) COMMENT '按访问级别过滤'
) COMMENT '文章表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS blog_article_access;
CREATE TABLE blog_article_access
(
    -- 基础字段
    id           BIGINT AUTO_INCREMENT COMMENT '权限ID（主键）',

    -- 核心关联字段
    article_id   BIGINT                             NOT NULL COMMENT '文章ID',
    user_id      BIGINT                             NOT NULL COMMENT '用户ID',

    -- 核心权限配置
    access_type  TINYINT  DEFAULT 1                 NOT NULL COMMENT '访问类型：1-白名单，2-黑名单',
    grant_time   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '授权时间',
    expire_time  DATETIME NULL COMMENT '过期时间（NULL=永久）',
    grant_reason VARCHAR(256) NULL COMMENT '授权原因',

    -- 基础时间字段
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 主键
    PRIMARY KEY (id),

    -- 精简索引：保留核心查询维度
    UNIQUE KEY uk_article_user_type (article_id, user_id, access_type) COMMENT '防重复授权',
    INDEX        idx_user_id (user_id) COMMENT '查询用户权限',
    INDEX        idx_expire_time (expire_time) COMMENT '清理过期权限'
) COMMENT '文章访问权限表（仅access_level=4时使用）'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


DROP TABLE IF EXISTS blog_article_category;
CREATE TABLE blog_article_category
(
    id          BIGINT AUTO_INCREMENT COMMENT '关联ID（主键）',
    article_id  BIGINT                             NOT NULL COMMENT '文章ID（关联blog_article.id）',
    category_id BIGINT                             NOT NULL COMMENT '分类ID（关联sys_category.id）',
    sort_order  INT      DEFAULT 0                 NOT NULL COMMENT '排序（用于多分类时的优先级）',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_article_category (article_id, category_id) COMMENT '文章+分类唯一约束',
    INDEX       idx_article_id (article_id) COMMENT '查询文章的所有分类',
    INDEX       idx_category_id (category_id) COMMENT '查询分类下的所有文章'
) COMMENT '文章-分类关联表（多对多）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_category;
CREATE TABLE sys_category
(
    id          BIGINT AUTO_INCREMENT COMMENT '分类ID' PRIMARY KEY,
    parent_id   BIGINT        DEFAULT 0                 NOT NULL COMMENT '父分类ID (0为根节点)',
    name        VARCHAR(64)                             NOT NULL COMMENT '分类名称',
    code        VARCHAR(32)                             NOT NULL COMMENT '分类编码 (业务唯一标识)',
    type        VARCHAR(32)                             NOT NULL COMMENT '业务类型 (article, product, file)',

    ancestors   VARCHAR(1024) DEFAULT ''                NOT NULL COMMENT '祖级列表 (逗号分隔，如 0,1,5)',
    level       INT           DEFAULT 1                 NOT NULL COMMENT '层级',
    sort_order  INT           DEFAULT 0                 NOT NULL COMMENT '显示顺序',

    icon        VARCHAR(128) NULL COMMENT '图标URL或CSS类名',
    description VARCHAR(256) NULL COMMENT '分类描述',
    status      TINYINT       DEFAULT 1                 NOT NULL COMMENT '状态: 1-启用, 0-禁用',

    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_type_code (type, code),
    INDEX       idx_type_parent (type, parent_id, status) COMMENT '业务类型+父分类+状态（树形查询核心）',
    INDEX       idx_type_sort (type, sort_order) COMMENT '业务类型+排序（列表展示）'
) COMMENT '通用分类表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_tag;
CREATE TABLE sys_tag
(
    id         BIGINT AUTO_INCREMENT COMMENT '标签ID' PRIMARY KEY,
    name       VARCHAR(64)                        NOT NULL COMMENT '标签名称',
    color      VARCHAR(16) NULL COMMENT '标签颜色 (如 #FF0000)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',

    UNIQUE KEY uk_name (name)
) COMMENT '通用标签表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_tag_relation;
CREATE TABLE sys_tag_relation
(
    id          BIGINT AUTO_INCREMENT COMMENT '关联ID' PRIMARY KEY,
    tag_id      BIGINT                             NOT NULL COMMENT '标签ID',
    target_id   BIGINT                             NOT NULL COMMENT '目标ID (文章ID, 文件ID等)',
    target_type VARCHAR(32)                        NOT NULL COMMENT '目标类型 (article, file)',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',

    UNIQUE KEY  uk_tag_target (tag_id, target_id, target_type) COMMENT '标签与目标唯一约束',
    INDEX       idx_tag_relation_target (target_id, target_type) COMMENT '目标ID+类型（查询对象的所有标签）',
    INDEX       idx_tag_relation_tag (tag_id, target_type) COMMENT '标签ID+类型（查询标签关联的对象）'
) COMMENT '通用标签关联表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_interaction;
CREATE TABLE sys_interaction
(
    id          BIGINT                             NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT                             NOT NULL COMMENT '用户ID',
    target_id   BIGINT                             NOT NULL COMMENT '目标ID（文章/评论/文件等）',
    target_type VARCHAR(32)                        NOT NULL COMMENT '目标类型 (article/comment/file/securityUser)',
    action_type VARCHAR(32)                        NOT NULL COMMENT '动作类型: like-点赞, dislike-点踩',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target_action (user_id, target_id, target_type, action_type) COMMENT '防止重复操作',
    INDEX       idx_target_action (target_id, target_type, action_type) COMMENT '目标+动作（统计互动数）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '通用互动表(点赞/点踩)';

DROP TABLE IF EXISTS sys_collection_folder;
CREATE TABLE sys_collection_folder
(
    id               BIGINT AUTO_INCREMENT COMMENT '收藏夹ID' PRIMARY KEY,
    user_id          BIGINT                                NOT NULL COMMENT '用户ID',
    folder_name      VARCHAR(64)                           NOT NULL COMMENT '收藏夹名称',
    folder_type      VARCHAR(32) DEFAULT 'article'         NOT NULL COMMENT '收藏夹类型（article/file/video）',
    description      VARCHAR(256) NULL COMMENT '描述',
    is_public        TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否公开：0-私有，1-公开',
    is_default       TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否默认收藏夹：0-否，1-是',
    default_folder_type VARCHAR(32) GENERATED ALWAYS AS (IF(is_default = 1, folder_type, NULL)) STORED COMMENT '默认收藏夹唯一键辅助列',
    sort_order       INT         DEFAULT 0                 NOT NULL COMMENT '排序',
    collection_count INT         DEFAULT 0                 NOT NULL COMMENT '收藏数量',

    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_user_default_type (user_id, default_folder_type) COMMENT '每个用户每种类型仅允许一个默认收藏夹',
    INDEX            idx_collection_user_type (user_id, folder_type) COMMENT '用户+类型（查询用户收藏夹）',
    INDEX            idx_public_folder (is_public, folder_type) COMMENT '公开收藏夹查询'
) COMMENT '通用收藏夹表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_collection;
CREATE TABLE sys_collection
(
    id           BIGINT AUTO_INCREMENT COMMENT '收藏ID' PRIMARY KEY,
    user_id      BIGINT                             NOT NULL COMMENT '用户ID',
    folder_id    BIGINT                             NOT NULL COMMENT '收藏夹ID',
    target_id    BIGINT                             NOT NULL COMMENT '目标ID（文章ID/文件ID等）',
    target_type  VARCHAR(32)                        NOT NULL COMMENT '目标类型（article/file/video）',
    remark       VARCHAR(256) NULL COMMENT '收藏备注',
    target_title VARCHAR(128) NULL COMMENT '目标标题',
    target_url   VARCHAR(256) NULL COMMENT '目标URL',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '收藏时间',

    UNIQUE KEY uk_user_folder_target (user_id, folder_id, target_id, target_type) COMMENT '用户+收藏夹+目标唯一',
    INDEX        idx_user_folder (user_id, folder_id) COMMENT '用户+收藏夹（查询收藏夹内容）',
    INDEX        idx_user_target_type (user_id, target_id, target_type) COMMENT '用户+目标+类型（检查是否已收藏）',
    INDEX        idx_target_type (target_id, target_type) COMMENT '目标+类型（统计收藏数）',
    INDEX        idx_folder_type (folder_id, target_type) COMMENT '收藏夹+类型（分类统计）'
) COMMENT '通用收藏记录表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_comment;
CREATE TABLE sys_comment
(
    id          BIGINT AUTO_INCREMENT COMMENT '评论ID' PRIMARY KEY,
    target_id   BIGINT                             NOT NULL COMMENT '目标ID (文章ID, 视频ID)',
    target_type VARCHAR(32)                        NOT NULL COMMENT '目标类型 (article, video)',
    content     TEXT                               NOT NULL COMMENT '评论内容',
    images      JSON NULL COMMENT '评论图片 (JSON数组，如 ["url1","url2"])',
    user_id     BIGINT                             NOT NULL COMMENT '评论者ID',
    root_id     BIGINT   DEFAULT 0                 NOT NULL COMMENT '根评论ID (顶级评论为0)',
    parent_id   BIGINT   DEFAULT 0                 NOT NULL COMMENT '父评论ID (回复某条评论)',
    like_count  INT      DEFAULT 0                 NOT NULL COMMENT '点赞数',
    reply_count INT      DEFAULT 0                 NOT NULL COMMENT '回复数',
    status      TINYINT  DEFAULT 1                 NOT NULL COMMENT '状态: 0-待审核, 1-正常, 2-隐藏',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX       idx_comment_target (target_id, target_type, status) COMMENT '目标+类型+状态（查询对象评论）',
    INDEX       idx_comment_root (root_id, status) COMMENT '根评论+状态（查询评论回复树）',
    INDEX       idx_comment_parent (parent_id, status) COMMENT '父评论+状态（查询直接回复）'
) COMMENT '通用评论表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS sys_user_footprint;
CREATE TABLE sys_user_footprint
(
    id          BIGINT AUTO_INCREMENT COMMENT '足迹ID' PRIMARY KEY,
    user_id     BIGINT                             NOT NULL COMMENT '用户ID',
    target_id   BIGINT                             NOT NULL COMMENT '目标ID（文章/视频/文件等）',
    target_type VARCHAR(32)                        NOT NULL COMMENT '目标类型 (article/file/video/comment/securityUser)',
    title       VARCHAR(128) NULL COMMENT '目标标题',
    url         VARCHAR(256) NULL COMMENT '目标URL',
    ip_address  VARCHAR(45) NULL COMMENT '访问IP（IPv4/IPv6）',
    user_agent  VARCHAR(512) NULL COMMENT 'User-Agent（设备/浏览器信息）',
    visited_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '访问时间',

    -- 约束：同一用户对同一目标只保留最新一次记录（业务层可定期去重或使用 ON DUPLICATE KEY UPDATE）
    UNIQUE KEY uk_user_target (user_id, target_id, target_type),

    -- 常用查询索引
    INDEX       idx_user_visited (user_id, visited_at DESC) COMMENT '按用户+时间倒序（获取最近浏览）',
    INDEX       idx_target_type (target_id, target_type) COMMENT '按目标统计热度',
    INDEX       idx_visited_time (visited_at) COMMENT '全局按时间清理旧数据'
) COMMENT '通用用户足迹表（浏览历史）'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


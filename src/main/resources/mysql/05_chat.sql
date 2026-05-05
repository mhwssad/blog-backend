-- ============================================
-- 聊天模块表结构
-- 包含：会话主表、会话成员表、消息主表、消息接收状态表、会话已读游标表、附件异步处理任务表
-- 设计目标：支持单聊、群聊、全站特殊群聊，并兼容已送达、已读、撤回及持久化媒体处理能力
-- ============================================

USE
blog_backend;

-- ----------------------------
-- 聊天会话主表
-- ----------------------------
DROP TABLE IF EXISTS forum_post_channel_link;
DROP TABLE IF EXISTS forum_reply;
DROP TABLE IF EXISTS forum_post;
DROP TABLE IF EXISTS forum_section;
DROP TABLE IF EXISTS chat_group_invite_link;
DROP TABLE IF EXISTS chat_group_join_application;
DROP TABLE IF EXISTS chat_channel_create_application;
DROP TABLE IF EXISTS chat_message_read_cursor;
DROP TABLE IF EXISTS chat_message_recipient;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_conversation_member;
DROP TABLE IF EXISTS chat_conversation;
DROP TABLE IF EXISTS chat_attachment_process_task;

CREATE TABLE chat_conversation
(
    id                BIGINT AUTO_INCREMENT COMMENT '会话ID' PRIMARY KEY,
    conversation_type VARCHAR(16)                        NOT NULL COMMENT '会话类型：single-单聊，group-群聊，global-全站特殊群聊',
    scene_type        VARCHAR(32) DEFAULT 'user_group'   NOT NULL COMMENT '业务场景：single_chat/user_group/hall_channel/topic_channel/global_channel',
    name              VARCHAR(128) NULL COMMENT '会话名称（群聊/全站群使用，单聊可为空）',
    avatar            VARCHAR(512) NULL COMMENT '会话头像',
    owner_id          BIGINT NULL COMMENT '会话拥有者ID（普通群聊群主）',
    single_pair_key   VARCHAR(64) NULL COMMENT '单聊唯一键，格式建议为 userIdMin:userIdMax',
    is_all_site       TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否全站特殊群聊：0-否，1-是',
    all_site_key      VARCHAR(16) GENERATED ALWAYS AS (CASE WHEN is_all_site = 1 THEN 'global' ELSE NULL END) STORED COMMENT '全站群唯一键辅助列',
    status            TINYINT  DEFAULT 1                 NOT NULL COMMENT '会话状态：0-禁用，1-正常，2-已解散',
    visibility_scope  VARCHAR(16) DEFAULT 'private'      NOT NULL COMMENT '可见范围：public/member/private',
    allow_guest_view  TINYINT  DEFAULT 0                 NOT NULL COMMENT '访客是否可见：0-否，1-是',
    require_join_to_speak TINYINT DEFAULT 1              NOT NULL COMMENT '是否需要加入后发言：0-否，1-是',
    join_rule         VARCHAR(16) DEFAULT 'free'         NOT NULL COMMENT '加入规则：free/approval/invite_only',
    speak_level_limit TINYINT  DEFAULT 1                 NOT NULL COMMENT '发言最低等级限制',
    member_limit      INT      DEFAULT 0                 NOT NULL COMMENT '成员上限：0-不限制',
    remark            VARCHAR(256) NULL COMMENT '备注',
    announcement      VARCHAR(512) NULL COMMENT '频道/群公告',
    slow_mode_seconds INT      DEFAULT 0                 NOT NULL COMMENT '慢速模式秒数：0-关闭',
    display_sort      INT      DEFAULT 0                 NOT NULL COMMENT '展示排序',
    channel_category_code VARCHAR(32) NULL COMMENT '频道分类编码/群分类编码',
    last_message_id   BIGINT NULL COMMENT '最后一条消息ID',
    last_message_time DATETIME NULL COMMENT '最后一条消息时间',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_single_pair (single_pair_key) COMMENT '单聊会话唯一键',
    UNIQUE KEY uk_all_site_key (all_site_key) COMMENT '全站特殊群聊唯一键',
    INDEX             idx_type_status (conversation_type, status) COMMENT '按类型和状态筛选会话',
    INDEX             idx_last_message_time (last_message_time DESC) COMMENT '按最近消息时间排序'
) COMMENT '聊天会话表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天会话成员表
-- ----------------------------
CREATE TABLE chat_conversation_member
(
    id                        BIGINT AUTO_INCREMENT COMMENT '成员ID' PRIMARY KEY,
    conversation_id           BIGINT                                NOT NULL COMMENT '会话ID',
    user_id                   BIGINT                                NOT NULL COMMENT '用户ID',
    member_role               VARCHAR(16) DEFAULT 'member'          NOT NULL COMMENT '成员角色：owner/admin/member',
    join_source               VARCHAR(16) DEFAULT 'manual'          NOT NULL COMMENT '入群来源：manual/system',
    status                    TINYINT     DEFAULT 1                 NOT NULL COMMENT '成员状态：0-已退出，1-正常，2-已移除，3-已禁用',
    mute_until                DATETIME NULL COMMENT '禁言截至时间（NULL 表示未禁言）',
    joined_at                 DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '加入时间',
    last_read_message_id      BIGINT NULL COMMENT '最后已读消息ID',
    last_read_at              DATETIME NULL COMMENT '最后已读时间',
    last_delivered_message_id BIGINT NULL COMMENT '最后已送达消息ID',
    last_delivered_at         DATETIME NULL COMMENT '最后已送达时间',
    remark                    VARCHAR(256) NULL COMMENT '备注',
    created_at                DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at                DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_conversation_user (conversation_id, user_id) COMMENT '同一会话下成员唯一',
    INDEX                     idx_user_status (user_id, status) COMMENT '按用户查询有效会话成员',
    INDEX                     idx_conversation_status (conversation_id, status) COMMENT '按会话查询有效成员'
) COMMENT '聊天会话成员表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天消息主表
-- ----------------------------
CREATE TABLE chat_message
(
    id                 BIGINT AUTO_INCREMENT COMMENT '消息ID' PRIMARY KEY,
    conversation_id    BIGINT                             NOT NULL COMMENT '会话ID',
    sender_id          BIGINT                             NOT NULL COMMENT '发送人ID',
    message_type       VARCHAR(16)                        NOT NULL COMMENT '消息类型：text/image/file/system',
    content            TEXT NULL COMMENT '消息正文（文本消息或系统提示文案）',
    payload_json       JSON NULL COMMENT '扩展载荷（图片、文件、业务扩展元数据）',
    reply_message_id   BIGINT NULL COMMENT '回复的消息ID',
    mention_all        TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否 @所有人：0-否，1-是',
    mentioned_user_ids JSON NULL COMMENT '被@用户ID列表（JSON数组）',
    send_status        TINYINT  DEFAULT 1                 NOT NULL COMMENT '发送状态：0-待发送，1-已发送，2-发送失败',
    revoke_status      TINYINT  DEFAULT 0                 NOT NULL COMMENT '撤回状态：0-正常，1-已撤回',
    revoked_by         BIGINT NULL COMMENT '撤回操作者ID',
    revoked_at         DATETIME NULL COMMENT '撤回时间',
    client_message_id  VARCHAR(64) NULL COMMENT '客户端消息幂等键',
    pinned_by          BIGINT NULL COMMENT '置顶操作人ID（NULL 表示未置顶）',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_sender_client_message (sender_id, client_message_id) COMMENT '发送人 + 客户端消息ID唯一',
    INDEX              idx_conversation_message (conversation_id, id DESC) COMMENT '按会话倒序查询消息',
    INDEX              idx_sender_message (sender_id, id DESC) COMMENT '按发送人倒序查询消息',
    INDEX              idx_reply_message (reply_message_id) COMMENT '按被回复消息查询'
) COMMENT '聊天消息表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天消息接收状态表
-- ----------------------------
CREATE TABLE chat_message_recipient
(
    id                BIGINT AUTO_INCREMENT COMMENT '接收状态ID' PRIMARY KEY,
    message_id        BIGINT                                NOT NULL COMMENT '消息ID',
    conversation_id   BIGINT                                NOT NULL COMMENT '会话ID',
    recipient_user_id BIGINT                                NOT NULL COMMENT '接收人ID',
    receive_type      VARCHAR(16) DEFAULT 'normal'          NOT NULL COMMENT '接收类型：normal/mention/system',
    delivery_status   TINYINT     DEFAULT 0                 NOT NULL COMMENT '投递状态：0-待投递，1-已送达，2-已读',
    delivered_at      DATETIME NULL COMMENT '送达时间',
    read_at           DATETIME NULL COMMENT '已读时间',
    visible_status    TINYINT     DEFAULT 1                 NOT NULL COMMENT '可见状态：0-已隐藏，1-可见',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_message_recipient (message_id, recipient_user_id) COMMENT '同一消息对同一接收人只生成一条接收状态',
    INDEX             idx_recipient_unread (recipient_user_id, delivery_status, visible_status, id DESC) COMMENT '接收人未读列表查询',
    INDEX             idx_conversation_recipient (conversation_id, recipient_user_id, id DESC) COMMENT '按会话和接收人查询消息状态'
) COMMENT '聊天消息接收状态表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天会话已读游标表
-- ----------------------------
CREATE TABLE chat_message_read_cursor
(
    id                   BIGINT AUTO_INCREMENT COMMENT '游标ID' PRIMARY KEY,
    conversation_id      BIGINT                             NOT NULL COMMENT '会话ID',
    user_id              BIGINT                             NOT NULL COMMENT '用户ID',
    read_message_id      BIGINT NULL COMMENT '最后已读消息ID',
    read_at              DATETIME NULL COMMENT '最后已读时间',
    delivered_message_id BIGINT NULL COMMENT '最后已送达消息ID',
    delivered_at         DATETIME NULL COMMENT '最后已送达时间',
    unread_count         INT      DEFAULT 0                 NOT NULL COMMENT '会话未读数',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_cursor_conversation_user (conversation_id, user_id) COMMENT '会话已读游标唯一',
    INDEX                idx_user_updated (user_id, updated_at DESC) COMMENT '按用户查询最近更新的会话游标'
) COMMENT '聊天会话已读游标表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天附件异步处理任务表
-- ----------------------------
CREATE TABLE chat_attachment_process_task
(
    id                    BIGINT AUTO_INCREMENT COMMENT '任务ID' PRIMARY KEY,
    message_id            BIGINT                             NOT NULL COMMENT '关联消息ID',
    message_type          VARCHAR(16)                        NOT NULL COMMENT '消息类型：image/voice',
    task_status           TINYINT  DEFAULT 0                 NOT NULL COMMENT '任务状态：0-待执行，1-处理中，2-成功，3-失败',
    retry_count           INT      DEFAULT 0                 NOT NULL COMMENT '累计重试次数',
    max_retry_count       INT      DEFAULT 3                 NOT NULL COMMENT '最大重试次数',
    next_retry_at         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '下次执行时间',
    lease_expire_at       DATETIME NULL COMMENT '当前处理租约过期时间',
    started_at            DATETIME NULL COMMENT '最近一次开始处理时间',
    completed_at          DATETIME NULL COMMENT '完成时间',
    last_error            VARCHAR(512) NULL COMMENT '最近一次错误信息',
    message_snapshot_json JSON NULL COMMENT '消息快照JSON，用于回推message_updated',
    push_user_ids_json    JSON NULL COMMENT '待推送用户ID列表JSON',
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_message_id (message_id) COMMENT '同一消息只保留一条媒体处理任务',
    INDEX                 idx_status_retry_time (task_status, next_retry_at, id) COMMENT '按状态和下次执行时间扫描任务',
    INDEX                 idx_status_lease_expire (task_status, lease_expire_at) COMMENT '按租约过期时间恢复处理中任务'
) COMMENT '聊天附件异步处理任务表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


CREATE TABLE chat_channel_create_application
(
    id                    BIGINT AUTO_INCREMENT COMMENT '申请ID' PRIMARY KEY,
    applicant_user_id     BIGINT                                NOT NULL COMMENT '申请用户ID',
    desired_name          VARCHAR(128)                          NOT NULL COMMENT '期望频道名称',
    desired_scene_type    VARCHAR(32) DEFAULT 'topic_channel'   NOT NULL COMMENT '期望频道类型',
    desired_category_code VARCHAR(32)                           NULL COMMENT '期望分类编码',
    description           VARCHAR(1024)                         NULL COMMENT '申请说明',
    apply_status          TINYINT     DEFAULT 0                 NOT NULL COMMENT '申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充',
    conversation_id       BIGINT                                NULL COMMENT '审核通过后关联频道ID',
    reviewer_id           BIGINT                                NULL COMMENT '审核人ID',
    review_comment        VARCHAR(512)                          NULL COMMENT '审核意见',
    submitted_at          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '提交时间',
    reviewed_at           DATETIME                              NULL COMMENT '审核时间',
    created_at            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at            DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_applicant_status (applicant_user_id, apply_status, submitted_at DESC) COMMENT '按申请人查看记录',
    INDEX idx_status_submitted (apply_status, submitted_at DESC) COMMENT '按状态处理申请'
) COMMENT '频道创建申请表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chat_group_join_application
(
    id                BIGINT AUTO_INCREMENT COMMENT '申请ID' PRIMARY KEY,
    conversation_id   BIGINT                                NOT NULL COMMENT '会话/群ID',
    applicant_user_id BIGINT                                NOT NULL COMMENT '申请用户ID',
    apply_message     VARCHAR(512)                          NULL COMMENT '申请附言',
    apply_status      TINYINT     DEFAULT 0                 NOT NULL COMMENT '申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消',
    reviewer_id       BIGINT                                NULL COMMENT '审核人ID',
    review_comment    VARCHAR(512)                          NULL COMMENT '审核意见',
    submitted_at      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '提交时间',
    reviewed_at       DATETIME                              NULL COMMENT '审核时间',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_conversation_status (conversation_id, apply_status, submitted_at DESC) COMMENT '按群查看申请',
    INDEX idx_applicant_status (applicant_user_id, apply_status, submitted_at DESC) COMMENT '按用户查看申请'
) COMMENT '群聊入群申请表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE chat_group_invite_link
(
    id             BIGINT AUTO_INCREMENT COMMENT '邀请链接ID' PRIMARY KEY,
    conversation_id BIGINT                              NOT NULL COMMENT '群聊会话ID',
    invite_token   VARCHAR(64)                          NOT NULL COMMENT '邀请链接令牌',
    created_by     BIGINT                               NOT NULL COMMENT '创建人ID',
    expire_at      DATETIME                             NULL COMMENT '过期时间，空表示不过期',
    max_use_count  INT        DEFAULT 0                 NOT NULL COMMENT '最大使用次数，0表示不限制',
    used_count     INT        DEFAULT 0                 NOT NULL COMMENT '已使用次数',
    status         TINYINT    DEFAULT 1                 NOT NULL COMMENT '状态：0-停用，1-启用',
    created_at     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_invite_token (invite_token) COMMENT '邀请链接令牌唯一',
    INDEX idx_conversation_status (conversation_id, status, created_at DESC) COMMENT '按群查看邀请链接'
) COMMENT '群聊邀请链接表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE forum_section
(
    id                BIGINT AUTO_INCREMENT COMMENT '版块ID' PRIMARY KEY,
    name              VARCHAR(64)                         NOT NULL COMMENT '版块名称',
    description       VARCHAR(512)                        NULL COMMENT '版块简介',
    sort_order        INT       DEFAULT 0                 NOT NULL COMMENT '排序值，越小越靠前',
    visibility_scope  TINYINT   DEFAULT 0                 NOT NULL COMMENT '可见范围：0-公开，1-登录可见',
    post_level_limit  INT       DEFAULT 1                 NOT NULL COMMENT '发帖最低等级',
    status            TINYINT   DEFAULT 1                 NOT NULL COMMENT '状态：0-禁用，1-启用',
    created_at        DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_forum_section_name (name) COMMENT '版块名称唯一',
    INDEX idx_status_sort (status, sort_order, id) COMMENT '公开版块排序查询'
) COMMENT '论坛版块表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE forum_post
(
    id                BIGINT AUTO_INCREMENT COMMENT '帖子ID' PRIMARY KEY,
    section_id        BIGINT                              NOT NULL COMMENT '版块ID',
    author_id         BIGINT                              NOT NULL COMMENT '作者用户ID',
    title             VARCHAR(128)                        NOT NULL COMMENT '帖子标题',
    content           LONGTEXT                            NULL COMMENT '帖子内容',
    status            TINYINT   DEFAULT 0                 NOT NULL COMMENT '状态：0-草稿，1-已发布，2-审核中，3-已拒绝，4-已删除，5-隐藏',
    visibility_scope  TINYINT   DEFAULT 0                 NOT NULL COMMENT '可见范围：0-公开，1-登录可见',
    is_top            TINYINT   DEFAULT 0                 NOT NULL COMMENT '是否置顶：0-否，1-是',
    is_essence        TINYINT   DEFAULT 0                 NOT NULL COMMENT '是否精华：0-否，1-是',
    view_count        INT       DEFAULT 0                 NOT NULL COMMENT '浏览数',
    like_count        INT       DEFAULT 0                 NOT NULL COMMENT '点赞数',
    reply_count       INT       DEFAULT 0                 NOT NULL COMMENT '回复数',
    collect_count     INT       DEFAULT 0                 NOT NULL COMMENT '收藏数',
    share_count       INT       DEFAULT 0                 NOT NULL COMMENT '分享数',
    published_at      DATETIME                            NULL COMMENT '发布时间',
    created_at        DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_section_status_top (section_id, status, is_top DESC, published_at DESC, id DESC) COMMENT '版块帖子列表',
    INDEX idx_author_status (author_id, status, updated_at DESC) COMMENT '用户帖子列表',
    INDEX idx_status_visibility (status, visibility_scope, published_at DESC) COMMENT '公开可见帖子查询'
) COMMENT '论坛帖子表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE forum_reply
(
    id            BIGINT AUTO_INCREMENT COMMENT '回复ID' PRIMARY KEY,
    post_id       BIGINT                              NOT NULL COMMENT '帖子ID',
    parent_id     BIGINT    DEFAULT 0                 NOT NULL COMMENT '父回复ID，顶级回复为0',
    root_id       BIGINT    DEFAULT 0                 NOT NULL COMMENT '根回复ID，顶级回复为0',
    user_id       BIGINT                              NOT NULL COMMENT '回复用户ID',
    content       TEXT                                NOT NULL COMMENT '回复内容',
    status        TINYINT   DEFAULT 1                 NOT NULL COMMENT '状态：1-正常，2-隐藏，3-删除，4-审核中',
    floor_no      INT       DEFAULT 0                 NOT NULL COMMENT '楼层号',
    like_count    INT       DEFAULT 0                 NOT NULL COMMENT '点赞数',
    reply_count   INT       DEFAULT 0                 NOT NULL COMMENT '回复数',
    created_at    DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at    DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_post_floor (post_id, floor_no, id) COMMENT '帖子回复楼层查询',
    INDEX idx_root_status (root_id, status, floor_no, id) COMMENT '根回复下子回复查询',
    INDEX idx_user_status (user_id, status, created_at DESC) COMMENT '用户回复查询'
) COMMENT '论坛回复表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE forum_post_channel_link
(
    id              BIGINT AUTO_INCREMENT COMMENT '关联ID' PRIMARY KEY,
    forum_post_id   BIGINT                                NOT NULL COMMENT '论坛帖子ID（预留）',
    conversation_id BIGINT                                NOT NULL COMMENT '频道会话ID',
    link_type       VARCHAR(16) DEFAULT 'manual_share'    NOT NULL COMMENT '关联方式：manual_share',
    linked_by       BIGINT                                NOT NULL COMMENT '关联人ID',
    linked_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '关联时间',
    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',

    UNIQUE KEY uk_forum_post (forum_post_id) COMMENT '一个帖子第一阶段只能挂一个频道',
    INDEX idx_conversation_linked (conversation_id, linked_at DESC) COMMENT '按频道查看挂接帖子'
) COMMENT '论坛帖子与频道关联表（第一阶段手动分享预留）'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ========== 统一禁言治理 ==========

DROP TABLE IF EXISTS `chat_user_mute_record`;
CREATE TABLE `chat_user_mute_record`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         bigint       NOT NULL COMMENT '被禁言用户 ID',
    `scope`           varchar(32)  NOT NULL COMMENT '禁言范围：global/lobby/topic_channel/group',
    `conversation_id` bigint       COMMENT '关联会话 ID（lobby/topic_channel/group 时必填）',
    `mute_until`      datetime     COMMENT '禁言截止时间（NULL 表示永久禁言）',
    `status`          tinyint      DEFAULT 1 NOT NULL COMMENT '0-已解除 1-生效中',
    `reason`          varchar(512) COMMENT '禁言原因',
    `source_type`     varchar(32)  COMMENT '来源：admin/report/auto',
    `report_id`       bigint       COMMENT '关联举报 ID',
    `operator_id`     bigint       NOT NULL COMMENT '操作人 ID',
    `released_by`     bigint       COMMENT '解除人 ID',
    `released_at`     datetime     COMMENT '解除时间',
    `created_at`      datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`      datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_mute_user_status` (`user_id`, `status`),
    INDEX `idx_mute_conversation_status` (`conversation_id`, `status`),
    INDEX `idx_mute_report` (`report_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='统一禁言记录表';

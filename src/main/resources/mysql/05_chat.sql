-- ============================================
-- 聊天模块表结构
-- 包含：会话主表、会话成员表、消息主表、消息接收状态表、会话已读游标表
-- 设计目标：支持单聊、群聊、全站特殊群聊，并兼容已送达、已读、撤回等基础能力
-- ============================================

USE blog_backend;

-- ----------------------------
-- 聊天会话主表
-- ----------------------------
DROP TABLE IF EXISTS chat_message_read_cursor;
DROP TABLE IF EXISTS chat_message_recipient;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_conversation_member;
DROP TABLE IF EXISTS chat_conversation;

CREATE TABLE chat_conversation
(
    id                BIGINT AUTO_INCREMENT COMMENT '会话ID' PRIMARY KEY,
    conversation_type VARCHAR(16)                        NOT NULL COMMENT '会话类型：single-单聊，group-群聊，global-全站特殊群聊',
    name              VARCHAR(128) NULL COMMENT '会话名称（群聊/全站群使用，单聊可为空）',
    avatar            VARCHAR(512) NULL COMMENT '会话头像',
    owner_id          BIGINT NULL COMMENT '会话拥有者ID（普通群聊群主）',
    single_pair_key   VARCHAR(64) NULL COMMENT '单聊唯一键，格式建议为 userIdMin:userIdMax',
    is_all_site       TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否全站特殊群聊：0-否，1-是',
    all_site_key      VARCHAR(16) GENERATED ALWAYS AS (CASE WHEN is_all_site = 1 THEN 'global' ELSE NULL END) STORED COMMENT '全站群唯一键辅助列',
    status            TINYINT      DEFAULT 1                 NOT NULL COMMENT '会话状态：0-禁用，1-正常，2-已解散',
    remark            VARCHAR(256) NULL COMMENT '备注',
    last_message_id   BIGINT NULL COMMENT '最后一条消息ID',
    last_message_time DATETIME NULL COMMENT '最后一条消息时间',
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_single_pair (single_pair_key) COMMENT '单聊会话唯一键',
    UNIQUE KEY uk_all_site_key (all_site_key) COMMENT '全站特殊群聊唯一键',
    INDEX      idx_type_status (conversation_type, status) COMMENT '按类型和状态筛选会话',
    INDEX      idx_last_message_time (last_message_time DESC) COMMENT '按最近消息时间排序'
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
    conversation_id           BIGINT                             NOT NULL COMMENT '会话ID',
    user_id                   BIGINT                             NOT NULL COMMENT '用户ID',
    member_role               VARCHAR(16) DEFAULT 'member'       NOT NULL COMMENT '成员角色：owner/admin/member',
    join_source               VARCHAR(16) DEFAULT 'manual'       NOT NULL COMMENT '入群来源：manual/system',
    status                    TINYINT     DEFAULT 1              NOT NULL COMMENT '成员状态：0-已退出，1-正常，2-已移除，3-已禁用',
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
    INDEX      idx_user_status (user_id, status) COMMENT '按用户查询有效会话成员',
    INDEX      idx_conversation_status (conversation_id, status) COMMENT '按会话查询有效成员'
) COMMENT '聊天会话成员表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天消息主表
-- ----------------------------
CREATE TABLE chat_message
(
    id                BIGINT AUTO_INCREMENT COMMENT '消息ID' PRIMARY KEY,
    conversation_id   BIGINT                             NOT NULL COMMENT '会话ID',
    sender_id         BIGINT                             NOT NULL COMMENT '发送人ID',
    message_type      VARCHAR(16)                        NOT NULL COMMENT '消息类型：text/image/file/system',
    content           TEXT NULL COMMENT '消息正文（文本消息或系统提示文案）',
    payload_json      JSON NULL COMMENT '扩展载荷（图片、文件、业务扩展元数据）',
    reply_message_id  BIGINT NULL COMMENT '回复的消息ID',
    mention_all       TINYINT      DEFAULT 0             NOT NULL COMMENT '是否 @所有人：0-否，1-是',
    mentioned_user_ids JSON NULL COMMENT '被@用户ID列表（JSON数组）',
    send_status       TINYINT      DEFAULT 1             NOT NULL COMMENT '发送状态：0-待发送，1-已发送，2-发送失败',
    revoke_status     TINYINT      DEFAULT 0             NOT NULL COMMENT '撤回状态：0-正常，1-已撤回',
    revoked_by        BIGINT NULL COMMENT '撤回操作者ID',
    revoked_at        DATETIME NULL COMMENT '撤回时间',
    client_message_id VARCHAR(64) NULL COMMENT '客户端消息幂等键',
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_sender_client_message (sender_id, client_message_id) COMMENT '发送人 + 客户端消息ID唯一',
    INDEX      idx_conversation_message (conversation_id, id DESC) COMMENT '按会话倒序查询消息',
    INDEX      idx_sender_message (sender_id, id DESC) COMMENT '按发送人倒序查询消息',
    INDEX      idx_reply_message (reply_message_id) COMMENT '按被回复消息查询'
) COMMENT '聊天消息表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 聊天消息接收状态表
-- ----------------------------
CREATE TABLE chat_message_recipient
(
    id               BIGINT AUTO_INCREMENT COMMENT '接收状态ID' PRIMARY KEY,
    message_id       BIGINT                             NOT NULL COMMENT '消息ID',
    conversation_id  BIGINT                             NOT NULL COMMENT '会话ID',
    recipient_user_id BIGINT                            NOT NULL COMMENT '接收人ID',
    receive_type     VARCHAR(16) DEFAULT 'normal'       NOT NULL COMMENT '接收类型：normal/mention/system',
    delivery_status  TINYINT     DEFAULT 0              NOT NULL COMMENT '投递状态：0-待投递，1-已送达，2-已读',
    delivered_at     DATETIME NULL COMMENT '送达时间',
    read_at          DATETIME NULL COMMENT '已读时间',
    visible_status   TINYINT     DEFAULT 1              NOT NULL COMMENT '可见状态：0-已隐藏，1-可见',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_message_recipient (message_id, recipient_user_id) COMMENT '同一消息对同一接收人只生成一条接收状态',
    INDEX      idx_recipient_unread (recipient_user_id, delivery_status, visible_status, id DESC) COMMENT '接收人未读列表查询',
    INDEX      idx_conversation_recipient (conversation_id, recipient_user_id, id DESC) COMMENT '按会话和接收人查询消息状态'
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
    unread_count         INT        DEFAULT 0               NOT NULL COMMENT '会话未读数',
    created_at           DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at           DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_cursor_conversation_user (conversation_id, user_id) COMMENT '会话已读游标唯一',
    INDEX      idx_user_updated (user_id, updated_at DESC) COMMENT '按用户查询最近更新的会话游标'
) COMMENT '聊天会话已读游标表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

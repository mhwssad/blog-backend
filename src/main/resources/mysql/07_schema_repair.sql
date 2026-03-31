-- ============================================
-- 存量库结构修复脚本
-- 目标：
-- 1. 为已有 blog_backend 库补齐 file / chat / follow 缺失表
-- 2. 修复系统基础表的唯一约束、索引与存储引擎问题
-- 说明：
-- - 本脚本面向“已有库升级”，不替代空库初始化脚本
-- - 空库初始化仍按 1.sys.sql、02_article.sql、04_file.sql、05_chat.sql、06_follow.sql、03_permission_init.sql 执行
-- ============================================

USE blog_backend;

-- ----------------------------
-- 文件模块缺失表补齐
-- ----------------------------
CREATE TABLE IF NOT EXISTS file_info
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    upload_task_id  BIGINT NULL COMMENT '关联上传任务ID（普通上传时有效；秒传或离线导入时可为空）',
    file_name       VARCHAR(128)      NOT NULL COMMENT '最终存储文件名（含扩展名）',
    original_name   VARCHAR(128)      NOT NULL COMMENT '原始文件名',
    file_path       VARCHAR(256)      NOT NULL COMMENT '存储路径或对象存储Key',
    storage_key     VARCHAR(64)       NOT NULL COMMENT '实际存储节点Key',
    file_url        VARCHAR(512) NULL COMMENT '文件访问URL（CDN地址）',
    file_size       BIGINT  DEFAULT 0 NOT NULL COMMENT '文件大小（字节）',
    file_type       VARCHAR(32) NULL COMMENT '文件类型：image/video/document/audio/other',
    mime_type       VARCHAR(64) NULL COMMENT 'MIME类型',
    file_extension  VARCHAR(16) NULL COMMENT '文件扩展名（小写）',
    md5             CHAR(32)          NOT NULL COMMENT '32位小写MD5值（用于全局去重与校验）',
    reference_count INT     DEFAULT 0 NOT NULL COMMENT '被秒传引用次数（>=0）',
    is_public       TINYINT DEFAULT 0 NOT NULL COMMENT '是否公开：0-私有（仅所有者可访问），1-公开',
    category        VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp等',
    download_count  INT     DEFAULT 0 NOT NULL COMMENT '下载次数',
    upload_user_id  BIGINT            NOT NULL COMMENT '上传用户ID',
    remark          VARCHAR(256) NULL COMMENT '备注',
    status          TINYINT DEFAULT 1 NOT NULL COMMENT '文件状态：0-已删除，1-正常，2-审核中，3-违规下架',
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL COMMENT '创建时间',
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_md5 (md5) COMMENT '全局文件去重（MD5唯一）',
    INDEX idx_user_created (upload_user_id, created_at DESC) COMMENT '用户文件列表',
    INDEX idx_category_status (category, status) COMMENT '按分类筛选有效文件',
    INDEX idx_storage_key (storage_key) COMMENT '按存储节点查询文件',
    INDEX idx_reference_count (reference_count DESC) COMMENT '热门文件排序'
) COMMENT '文件信息表（支持秒传、引用计数、逻辑删除）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS file_upload_task
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    upload_id          VARCHAR(64)       NOT NULL COMMENT '上传任务唯一标识（UUID）',
    file_id            BIGINT NULL COMMENT '关联文件ID（任务完成后填充；秒传时指向原文件）',
    upload_user_id     BIGINT            NOT NULL COMMENT '上传用户ID',
    storage_key        VARCHAR(64)       NOT NULL COMMENT '任务绑定的存储节点Key',
    source_ip          VARCHAR(45) NULL COMMENT '客户端IP（IPv4/IPv6，用于安全审计）',
    is_quick_upload    TINYINT DEFAULT 0 NOT NULL COMMENT '是否秒传：0-否，1-是',
    referenced_file_id BIGINT NULL COMMENT '秒传引用的原文件ID',
    file_md5           CHAR(32) NULL COMMENT '预计算MD5（32位小写）',
    file_size          BIGINT NULL COMMENT '文件大小（字节）',
    original_name      VARCHAR(128) NULL COMMENT '原始文件名（秒传时用于记录）',
    mime_type          VARCHAR(64) NULL COMMENT 'MIME类型',
    reference_type     VARCHAR(32) NULL COMMENT '引用类型：avatar/article_attachment/comment_image/temp',
    reference_id       BIGINT NULL COMMENT '引用记录ID',
    category           VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp',
    is_public          TINYINT DEFAULT 0 NOT NULL COMMENT '业务可见性：0-私有，1-公开',
    remark             VARCHAR(256) NULL COMMENT '备注',
    is_chunked         TINYINT DEFAULT 0 NOT NULL COMMENT '是否分片上传',
    chunk_size         BIGINT NULL COMMENT '分片大小（字节）',
    total_chunks       INT NULL COMMENT '总分片数',
    uploaded_chunks    INT     DEFAULT 0 NOT NULL COMMENT '已上传分片数',
    task_status        TINYINT DEFAULT 0 NOT NULL COMMENT '0-初始化，1-上传中，2-合并中，3-已完成，4-失败，5-已取消',
    retry_count        INT     DEFAULT 0 NOT NULL COMMENT '累计重试次数',
    start_time         DATETIME(3) NULL COMMENT '开始时间',
    complete_time      DATETIME(3) NULL COMMENT '完成时间',
    quick_upload_time  DATETIME(3) NULL COMMENT '秒传完成时间',
    expire_time        DATETIME(3) NOT NULL COMMENT '过期时间（自动清理未完成任务）',
    error_code         VARCHAR(32) NULL COMMENT '错误码（如: FILE_TOO_LARGE, CHUNK_MD5_MISMATCH）',
    error_message      VARCHAR(256) NULL COMMENT '错误描述',
    created_at         DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at         DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_upload_id (upload_id) COMMENT '任务ID唯一',
    INDEX idx_user_status_expire (upload_user_id, task_status, expire_time) COMMENT '用户任务筛选+过期',
    INDEX idx_md5_user (file_md5, upload_user_id) COMMENT '秒传检测（MD5+用户）',
    INDEX idx_file_md5 (file_md5) COMMENT '按MD5查询上传任务',
    INDEX idx_reference (reference_type, reference_id) COMMENT '按业务对象查询上传任务',
    INDEX idx_referenced_file (referenced_file_id) COMMENT '引用关系查询',
    INDEX idx_status_expire (task_status, expire_time) COMMENT '过期任务清理',
    INDEX idx_quick_upload_time (is_quick_upload, quick_upload_time) COMMENT '秒传统计'
) COMMENT '文件上传任务表（支持秒传、分片、断点续传、安全审计）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS file_chunk
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分片ID',
    upload_task_id BIGINT            NOT NULL COMMENT '关联上传任务ID',
    chunk_number   INT               NOT NULL COMMENT '分片序号（从1开始）',
    chunk_size     BIGINT            NOT NULL COMMENT '分片大小（字节）',
    chunk_md5      CHAR(32) NULL COMMENT '分片MD5（可选校验）',
    upload_status  TINYINT DEFAULT 0 NOT NULL COMMENT '0-待上传，1-上传中，2-已完成，3-失败',
    retry_count    INT     DEFAULT 0 NOT NULL COMMENT '分片重试次数',
    upload_time    DATETIME(3) NULL COMMENT '上传完成时间',
    created_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_task_chunk (upload_task_id, chunk_number) COMMENT '任务内分片唯一',
    INDEX idx_upload_status (upload_task_id, upload_status) COMMENT '分片进度查询',
    INDEX idx_chunk_number (upload_task_id, chunk_number) COMMENT '按序号快速定位'
) COMMENT '文件分片元数据表（实际文件由对象存储管理）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS file_business_info
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '业务属性ID',
    file_id        BIGINT            NOT NULL COMMENT '文件ID',
    user_id        BIGINT NULL COMMENT '所属用户ID',
    reference_type VARCHAR(32) NULL COMMENT '引用类型：avatar/article_attachment/comment_image/temp',
    reference_id   BIGINT NULL COMMENT '引用记录ID',
    source_ip      VARCHAR(45) NULL COMMENT '客户端IP（IPv4/IPv6，用于安全审计）',
    is_public      TINYINT DEFAULT 0 NOT NULL COMMENT '业务可见性：0-私有，1-公开',
    category       VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp等',
    remark         VARCHAR(256) NULL COMMENT '备注',
    created_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL COMMENT '创建时间',
    updated_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_file_reference (file_id, user_id, reference_type, reference_id) COMMENT '同一用户对同一业务对象绑定同一文件只允许一次',
    INDEX idx_user_created (user_id, created_at DESC) COMMENT '按用户查询引用记录',
    INDEX idx_reference (reference_type, reference_id) COMMENT '按业务对象查询文件引用',
    INDEX idx_category (category) COMMENT '按分类查询'
) COMMENT '文件业务引用表，表示文件与业务对象之间的引用关系'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- 聊天模块缺失表补齐
-- ----------------------------
CREATE TABLE IF NOT EXISTS chat_conversation
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
    INDEX idx_type_status (conversation_type, status) COMMENT '按类型和状态筛选会话',
    INDEX idx_last_message_time (last_message_time DESC) COMMENT '按最近消息时间排序'
) COMMENT '聊天会话表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_conversation_member
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
    INDEX idx_user_status (user_id, status) COMMENT '按用户查询有效会话成员',
    INDEX idx_conversation_status (conversation_id, status) COMMENT '按会话查询有效成员'
) COMMENT '聊天会话成员表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message
(
    id                 BIGINT AUTO_INCREMENT COMMENT '消息ID' PRIMARY KEY,
    conversation_id    BIGINT                                NOT NULL COMMENT '会话ID',
    sender_id          BIGINT                                NOT NULL COMMENT '发送人ID',
    message_type       VARCHAR(16)                           NOT NULL COMMENT '消息类型：text/image/file/system',
    content            TEXT NULL COMMENT '消息正文（文本消息或系统提示文案）',
    payload_json       JSON NULL COMMENT '扩展载荷（图片、文件、业务扩展元数据）',
    reply_message_id   BIGINT NULL COMMENT '回复的消息ID',
    mention_all        TINYINT      DEFAULT 0                NOT NULL COMMENT '是否 @所有人：0-否，1-是',
    mentioned_user_ids JSON NULL COMMENT '被@用户ID列表（JSON数组）',
    send_status        TINYINT      DEFAULT 1                NOT NULL COMMENT '发送状态：0-待发送，1-已发送，2-发送失败',
    revoke_status      TINYINT      DEFAULT 0                NOT NULL COMMENT '撤回状态：0-正常，1-已撤回',
    revoked_by         BIGINT NULL COMMENT '撤回操作者ID',
    revoked_at         DATETIME NULL COMMENT '撤回时间',
    client_message_id  VARCHAR(64) NULL COMMENT '客户端消息幂等键',
    created_at         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sender_client_message (sender_id, client_message_id) COMMENT '发送人 + 客户端消息ID唯一',
    INDEX idx_conversation_message (conversation_id, id DESC) COMMENT '按会话倒序查询消息',
    INDEX idx_sender_message (sender_id, id DESC) COMMENT '按发送人倒序查询消息',
    INDEX idx_reply_message (reply_message_id) COMMENT '按被回复消息查询'
) COMMENT '聊天消息表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message_recipient
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
    INDEX idx_recipient_unread (recipient_user_id, delivery_status, visible_status, id DESC) COMMENT '接收人未读列表查询',
    INDEX idx_conversation_recipient (conversation_id, recipient_user_id, id DESC) COMMENT '按会话和接收人查询消息状态'
) COMMENT '聊天消息接收状态表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message_read_cursor
(
    id                   BIGINT AUTO_INCREMENT COMMENT '游标ID' PRIMARY KEY,
    conversation_id      BIGINT                                NOT NULL COMMENT '会话ID',
    user_id              BIGINT                                NOT NULL COMMENT '用户ID',
    read_message_id      BIGINT NULL COMMENT '最后已读消息ID',
    read_at              DATETIME NULL COMMENT '最后已读时间',
    delivered_message_id BIGINT NULL COMMENT '最后已送达消息ID',
    delivered_at         DATETIME NULL COMMENT '最后已送达时间',
    unread_count         INT        DEFAULT 0                  NOT NULL COMMENT '会话未读数',
    created_at           DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at           DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cursor_conversation_user (conversation_id, user_id) COMMENT '会话已读游标唯一',
    INDEX idx_user_updated (user_id, updated_at DESC) COMMENT '按用户查询最近更新的会话游标'
) COMMENT '聊天会话已读游标表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_attachment_process_task
(
    id                    BIGINT AUTO_INCREMENT COMMENT '任务ID' PRIMARY KEY,
    message_id            BIGINT                                NOT NULL COMMENT '关联消息ID',
    message_type          VARCHAR(16)                           NOT NULL COMMENT '消息类型：image/voice',
    task_status           TINYINT      DEFAULT 0                NOT NULL COMMENT '任务状态：0-待执行，1-处理中，2-成功，3-失败',
    retry_count           INT          DEFAULT 0                NOT NULL COMMENT '累计重试次数',
    max_retry_count       INT          DEFAULT 3                NOT NULL COMMENT '最大重试次数',
    next_retry_at         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '下次执行时间',
    lease_expire_at       DATETIME NULL COMMENT '当前处理租约过期时间',
    started_at            DATETIME NULL COMMENT '最近一次开始处理时间',
    completed_at          DATETIME NULL COMMENT '完成时间',
    last_error            VARCHAR(512) NULL COMMENT '最近一次错误信息',
    message_snapshot_json JSON NULL COMMENT '消息快照JSON，用于回推message_updated',
    push_user_ids_json    JSON NULL COMMENT '待推送用户ID列表JSON',
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id) COMMENT '同一消息只保留一条媒体处理任务',
    INDEX idx_status_retry_time (task_status, next_retry_at, id) COMMENT '按状态和下次执行时间扫描任务',
    INDEX idx_status_lease_expire (task_status, lease_expire_at) COMMENT '按租约过期时间恢复处理中任务'
) COMMENT '聊天附件异步处理任务表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

-- ----------------------------
-- 粉丝关注缺失表补齐
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_follow
(
    id                BIGINT AUTO_INCREMENT COMMENT '关注关系ID' PRIMARY KEY,
    follower_id       BIGINT                             NOT NULL COMMENT '关注人ID',
    following_id      BIGINT                             NOT NULL COMMENT '被关注人ID',
    follow_status     TINYINT  DEFAULT 1                 NOT NULL COMMENT '关注状态：0-已取关，1-已关注',
    is_special_follow TINYINT DEFAULT 0                  NOT NULL COMMENT '是否特别关注：0-否，1-是',
    source            VARCHAR(32) DEFAULT 'manual'       NOT NULL COMMENT '关注来源：manual/recommend/system',
    follow_time       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '最近一次关注时间',
    unfollow_time     DATETIME NULL COMMENT '最近一次取关时间',
    remark            VARCHAR(256) NULL COMMENT '备注',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_follower_following (follower_id, following_id) COMMENT '同一关注关系唯一',
    INDEX idx_follower_status_time (follower_id, follow_status, follow_time DESC) COMMENT '查询我的关注列表',
    INDEX idx_following_status_time (following_id, follow_status, follow_time DESC) COMMENT '查询我的粉丝列表',
    INDEX idx_follow_pair_status (follower_id, following_id, follow_status) COMMENT '判断是否已关注/互关',
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> following_id)
) COMMENT '用户关注关系表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DELIMITER $$

DROP PROCEDURE IF EXISTS apply_schema_repair $$
CREATE PROCEDURE apply_schema_repair()
BEGIN
    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND index_name = 'uk_username') > 0 THEN
        SET @ddl = 'ALTER TABLE sys_user DROP INDEX uk_username';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND column_name = 'active_username') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD COLUMN active_username VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN username ELSE NULL END) STORED COMMENT ''有效用户名唯一键辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND column_name = 'active_email') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD COLUMN active_email VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN NULLIF(TRIM(email), '''') ELSE NULL END) STORED COMMENT ''有效邮箱唯一键辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND column_name = 'active_phone') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD COLUMN active_phone VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN NULLIF(TRIM(phone), '''') ELSE NULL END) STORED COMMENT ''有效手机号唯一键辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND index_name = 'uk_sys_user_active_username') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_active_username (active_username)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND index_name = 'uk_sys_user_active_email') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_active_email (active_email)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND index_name = 'uk_sys_user_active_phone') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_active_phone (active_phone)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user'
          AND index_name = 'idx_user_deleted_username') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user ADD KEY idx_user_deleted_username (deleted_flag, username)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'uk_name') > 0 THEN
        SET @ddl = 'ALTER TABLE sys_role DROP INDEX uk_name';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'uk_code') > 0 THEN
        SET @ddl = 'ALTER TABLE sys_role DROP INDEX uk_code';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND column_name = 'active_role_name') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD COLUMN active_role_name VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(name), '''') ELSE NULL END) STORED COMMENT ''有效角色名称唯一辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND column_name = 'active_role_code') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD COLUMN active_role_code VARCHAR(32) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(code), '''') ELSE NULL END) STORED COMMENT ''有效角色编码唯一辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'uk_sys_role_active_name') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD UNIQUE KEY uk_sys_role_active_name (active_role_name)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'uk_sys_role_active_code') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD UNIQUE KEY uk_sys_role_active_code (active_role_code)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'idx_role_deleted_name') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD KEY idx_role_deleted_name (is_deleted, name)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role'
          AND index_name = 'idx_role_deleted_code') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role ADD KEY idx_role_deleted_code (is_deleted, code)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    SET @file_reference_columns = (SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'file_business_info'
          AND index_name = 'uk_file_reference');

    IF @file_reference_columns IS NULL THEN
        SET @ddl = 'ALTER TABLE file_business_info ADD UNIQUE KEY uk_file_reference (file_id, user_id, reference_type, reference_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    ELSEIF @file_reference_columns <> 'file_id,user_id,reference_type,reference_id' THEN
        SET @ddl = 'ALTER TABLE file_business_info DROP INDEX uk_file_reference';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @ddl = 'ALTER TABLE file_business_info ADD UNIQUE KEY uk_file_reference (file_id, user_id, reference_type, reference_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user_notice'
          AND index_name = 'uk_notice_user') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user_notice ADD UNIQUE KEY uk_notice_user (notice_id, user_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user_notice'
          AND index_name = 'idx_user_notice_inbox') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user_notice ADD KEY idx_user_notice_inbox (user_id, is_deleted, is_read, notice_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_notice'
          AND index_name = 'idx_notice_admin_page') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_notice ADD KEY idx_notice_admin_page (is_deleted, create_time, id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_notice'
          AND index_name = 'idx_notice_publish_scope_time') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_notice ADD KEY idx_notice_publish_scope_time (is_deleted, publish_status, target_type, publish_time, id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_config'
          AND column_name = 'active_config_key') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_config ADD COLUMN active_config_key VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(config_key), '''') ELSE NULL END) STORED COMMENT ''有效配置键唯一辅助列''';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_config'
          AND index_name = 'uk_sys_config_active_key') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_config ADD UNIQUE KEY uk_sys_config_active_key (active_config_key)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_config'
          AND index_name = 'idx_config_key_deleted') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_config ADD KEY idx_config_key_deleted (config_key, is_deleted)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_role_menu'
          AND index_name = 'idx_role_menu_menu_id') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_role_menu ADD KEY idx_role_menu_menu_id (menu_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT COUNT(1)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_user_role'
          AND index_name = 'idx_user_role_role_id') = 0 THEN
        SET @ddl = 'ALTER TABLE sys_user_role ADD KEY idx_user_role_role_id (role_id)';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF (SELECT ENGINE
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_log') <> 'InnoDB' THEN
        SET @ddl = 'ALTER TABLE sys_log ENGINE = InnoDB';
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

CALL apply_schema_repair() $$
DROP PROCEDURE IF EXISTS apply_schema_repair $$

DELIMITER ;

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`)
VALUES
    (1790, 1700, '0,1700', '聊天管理', 'M', 'ContentChat', 'chats', 'content/chat/index', NULL, 0, 1, 1, 9, 'chat-dot-round', NULL, NOW(), NOW(), NULL),
    (1791, 1790, '0,1700,1790', '会话查询', 'B', NULL, NULL, NULL, 'content:chat:query', 0, 0, 1, 1, NULL, NULL, NOW(), NOW(), NULL),
    (1792, 1790, '0,1700,1790', '会话状态', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 2, NULL, NULL, NOW(), NOW(), NULL),
    (1793, 1700, '0,1700', '关注管理', 'M', 'ContentFollow', 'follows', 'content/follow/index', NULL, 0, 1, 1, 10, 'user-filled', NULL, NOW(), NOW(), NULL),
    (1794, 1793, '0,1700,1793', '关注查询', 'B', NULL, NULL, NULL, 'content:follow:query', 0, 0, 1, 1, NULL, NULL, NOW(), NOW(), NULL),
    (1795, 1793, '0,1700,1793', '关注清理', 'B', NULL, NULL, NULL, 'content:follow:clean', 0, 0, 1, 2, NULL, NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    tree_path = VALUES(tree_path),
    name = VALUES(name),
    type = VALUES(type),
    route_name = VALUES(route_name),
    route_path = VALUES(route_path),
    component = VALUES(component),
    perm = VALUES(perm),
    always_show = VALUES(always_show),
    keep_alive = VALUES(keep_alive),
    visible = VALUES(visible),
    sort = VALUES(sort),
    icon = VALUES(icon),
    redirect = VALUES(redirect),
    update_time = NOW(),
    params = VALUES(params);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1790 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1791 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1792 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1793 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1794 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1795 FROM DUAL WHERE EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 1);

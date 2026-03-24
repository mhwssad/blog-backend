-- ============================================
-- 文件模块表结构
-- 包含：文件主表、上传任务表、文件分片表、文件业务属性表
-- ============================================

-- ----------------------------
-- 文件主表（已完成上传的文件元数据）
-- ----------------------------
DROP TABLE IF EXISTS file_info;
CREATE TABLE file_info
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    upload_task_id  BIGINT NULL COMMENT '关联上传任务ID（普通上传时有效；秒传或离线导入时可为空）',
    file_name       VARCHAR(128)      NOT NULL COMMENT '最终存储文件名（含扩展名）',
    original_name   VARCHAR(128)      NOT NULL COMMENT '原始文件名',
    file_path       VARCHAR(256)      NOT NULL COMMENT '存储路径或对象存储Key',
    storage_key     VARCHAR(64)       NOT NULL COMMENT '实际存储节点Key',
    file_url        VARCHAR(512) NULL COMMENT '文件访问URL（CDN地址）',

    -- 核心元数据
    file_size       BIGINT  DEFAULT 0 NOT NULL COMMENT '文件大小（字节）',
    file_type       VARCHAR(32) NULL COMMENT '文件类型：image/video/document/audio/other',
    mime_type       VARCHAR(64) NULL COMMENT 'MIME类型',
    file_extension  VARCHAR(16) NULL COMMENT '文件扩展名（小写）',
    md5             CHAR(32)          NOT NULL COMMENT '32位小写MD5值（用于全局去重与校验）',

    -- 引用与访问控制
    reference_count INT     DEFAULT 0 NOT NULL COMMENT '被秒传引用次数（>=0）',
    is_public       TINYINT DEFAULT 0 NOT NULL COMMENT '是否公开：0-私有（仅所有者可访问），1-公开',

    -- 业务属性
    category        VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp等',
    download_count  INT     DEFAULT 0 NOT NULL COMMENT '下载次数',
    upload_user_id  BIGINT            NOT NULL COMMENT '上传用户ID',
    remark          VARCHAR(256) NULL COMMENT '备注',

    -- 状态与时间（毫秒级）
    status          TINYINT DEFAULT 1 NOT NULL COMMENT '文件状态：0-已删除，1-正常，2-审核中，3-违规下架',
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL COMMENT '创建时间',
    updated_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    -- 索引
    UNIQUE KEY uk_md5 (md5) COMMENT '全局文件去重（MD5唯一）',
    INDEX           idx_user_created (upload_user_id, created_at DESC) COMMENT '用户文件列表',
    INDEX           idx_category_status (category, status) COMMENT '按分类筛选有效文件',
    INDEX           idx_storage_key (storage_key) COMMENT '按存储节点查询文件',
    INDEX           idx_reference_count (reference_count DESC) COMMENT '热门文件排序'
) COMMENT '文件信息表（支持秒传、引用计数、逻辑删除）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- 文件上传任务表（管控上传生命周期）
-- ----------------------------
DROP TABLE IF EXISTS file_upload_task;
CREATE TABLE file_upload_task
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    upload_id          VARCHAR(64)       NOT NULL COMMENT '上传任务唯一标识（UUID）',
    file_id            BIGINT NULL COMMENT '关联文件ID（任务完成后填充；秒传时指向原文件）',
    upload_user_id     BIGINT            NOT NULL COMMENT '上传用户ID',
    storage_key        VARCHAR(64)       NOT NULL COMMENT '任务绑定的存储节点Key',
    source_ip          VARCHAR(45) NULL COMMENT '客户端IP（IPv4/IPv6，用于安全审计）',

    -- 秒传字段
    is_quick_upload    TINYINT DEFAULT 0 NOT NULL COMMENT '是否秒传：0-否，1-是',
    referenced_file_id BIGINT NULL COMMENT '秒传引用的原文件ID',
    file_md5           CHAR(32) NULL COMMENT '预计算MD5（32位小写）',
    file_size          BIGINT NULL COMMENT '文件大小（字节）',
    original_name      VARCHAR(128) NULL COMMENT '原始文件名（秒传时用于记录）',
    mime_type          VARCHAR(64) NULL COMMENT 'MIME类型',

    -- 业务引用字段（用于在分片/普通上传完成后创建引用记录）
    reference_type     VARCHAR(32) NULL COMMENT '引用类型：avatar/article_attachment/comment_image/temp',
    reference_id       BIGINT NULL COMMENT '引用记录ID',
    category           VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp',
    is_public          TINYINT DEFAULT 0 NOT NULL COMMENT '业务可见性：0-私有，1-公开',
    remark             VARCHAR(256) NULL COMMENT '备注',

    -- 分片字段
    is_chunked         TINYINT DEFAULT 0 NOT NULL COMMENT '是否分片上传',
    chunk_size         BIGINT NULL COMMENT '分片大小（字节）',
    total_chunks       INT NULL COMMENT '总分片数',
    uploaded_chunks    INT     DEFAULT 0 NOT NULL COMMENT '已上传分片数',

    -- 状态与时间
    task_status        TINYINT DEFAULT 0 NOT NULL COMMENT '0-初始化，1-上传中，2-合并中，3-已完成，4-失败，5-已取消',
    retry_count        INT     DEFAULT 0 NOT NULL COMMENT '累计重试次数',
    start_time         DATETIME(3)                          NULL COMMENT '开始时间',
    complete_time      DATETIME(3)                          NULL COMMENT '完成时间',
    quick_upload_time  DATETIME(3)                          NULL COMMENT '秒传完成时间',
    expire_time        DATETIME(3) NOT NULL                 COMMENT '过期时间（自动清理未完成任务）',

    -- 错误信息（结构化）
    error_code         VARCHAR(32) NULL COMMENT '错误码（如: FILE_TOO_LARGE, CHUNK_MD5_MISMATCH）',
    error_message      VARCHAR(256) NULL COMMENT '错误描述',

    created_at         DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at         DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),

    -- 索引
    UNIQUE KEY uk_upload_id (upload_id) COMMENT '任务ID唯一',
    INDEX              idx_user_status_expire (upload_user_id, task_status, expire_time) COMMENT '用户任务筛选+过期',
    INDEX              idx_md5_user (file_md5, upload_user_id) COMMENT '秒传检测（MD5+用户）',
    INDEX              idx_file_md5 (file_md5) COMMENT '按MD5查询上传任务',
    INDEX              idx_reference (reference_type, reference_id) COMMENT '按业务对象查询上传任务',
    INDEX              idx_referenced_file (referenced_file_id) COMMENT '引用关系查询',
    INDEX              idx_status_expire (task_status, expire_time) COMMENT '过期任务清理',
    INDEX              idx_quick_upload_time (is_quick_upload, quick_upload_time) COMMENT '秒传统计'
) COMMENT '文件上传任务表（支持秒传、分片、断点续传、安全审计）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- 文件分片表（仅存元数据，不含二进制）
-- ----------------------------
DROP TABLE IF EXISTS file_chunk;
CREATE TABLE file_chunk
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分片ID',
    upload_task_id BIGINT            NOT NULL COMMENT '关联上传任务ID',
    chunk_number   INT               NOT NULL COMMENT '分片序号（从1开始）',
    chunk_size     BIGINT            NOT NULL COMMENT '分片大小（字节）',
    chunk_md5      CHAR(32) NULL COMMENT '分片MD5（可选校验）',
    upload_status  TINYINT DEFAULT 0 NOT NULL COMMENT '0-待上传，1-上传中，2-已完成，3-失败',
    retry_count    INT     DEFAULT 0 NOT NULL COMMENT '分片重试次数',
    upload_time    DATETIME(3)                        NULL COMMENT '上传完成时间',
    created_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),

    -- 索引
    UNIQUE KEY uk_task_chunk (upload_task_id, chunk_number) COMMENT '任务内分片唯一',
    INDEX          idx_upload_status (upload_task_id, upload_status) COMMENT '分片进度查询',
    INDEX          idx_chunk_number (upload_task_id, chunk_number) COMMENT '按序号快速定位'
) COMMENT '文件分片元数据表（实际文件由对象存储管理）'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- 文件业务属性表
-- ----------------------------
DROP TABLE IF EXISTS file_business_info;
CREATE TABLE file_business_info
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '业务属性ID',
    file_id        BIGINT            NOT NULL COMMENT '文件ID',
    user_id        BIGINT NULL COMMENT '所属用户ID',
    reference_type VARCHAR(32) NULL COMMENT '引用类型：avatar/article_attachment/comment_image/temp',
    reference_id   BIGINT NULL COMMENT '引用记录ID',
    source_ip      VARCHAR(45) NULL COMMENT '客户端IP（IPv4/IPv6，用于安全审计）',

    -- 业务分类与访问控制
    is_public      TINYINT DEFAULT 0 NOT NULL COMMENT '业务可见性：0-私有，1-公开',
    category       VARCHAR(32) NULL COMMENT '业务分类：avatar/attachment/comment/temp等',

    -- 备注
    remark         VARCHAR(256) NULL COMMENT '备注',

    -- 时间
    created_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL COMMENT '创建时间',
    updated_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    -- 索引
    UNIQUE KEY uk_file_reference (file_id, reference_type, reference_id) COMMENT '同一业务对象对同一文件只允许绑定一次',
    INDEX          idx_user_created (user_id, created_at DESC) COMMENT '按用户查询引用记录',
    INDEX          idx_reference (reference_type, reference_id) COMMENT '按业务对象查询文件引用',
    INDEX          idx_category (category) COMMENT '按分类查询'
) COMMENT '文件业务引用表，表示文件与业务对象之间的引用关系'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

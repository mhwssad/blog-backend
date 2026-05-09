CREATE DATABASE IF NOT EXISTS blog_backend CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE blog_backend;

DROP TABLE IF EXISTS blog_migration_attachment;
DROP TABLE IF EXISTS blog_migration_record;
DROP TABLE IF EXISTS blog_migration_task;

CREATE TABLE blog_migration_task
(
    id                 BIGINT AUTO_INCREMENT COMMENT '迁移任务ID',
    source_platform    VARCHAR(64)                        NOT NULL COMMENT '来源平台',
    original_file_name VARCHAR(128)                       NOT NULL COMMENT '原始文件名',
    file_md5           CHAR(32)                           NOT NULL COMMENT '迁移文件MD5',
    file_size          BIGINT   DEFAULT 0                 NOT NULL COMMENT '迁移文件大小',
    file_content_json  LONGTEXT                           NOT NULL COMMENT '迁移文件原始JSON内容',
    author_id          BIGINT                             NOT NULL COMMENT '导入文章归属作者ID',
    status             TINYINT  DEFAULT 0                 NOT NULL COMMENT '状态：0-已创建，1-预检通过，2-执行中，3-已完成，4-失败，5-已取消',
    total_count        INT      DEFAULT 0                 NOT NULL COMMENT '总文章数',
    success_count      INT      DEFAULT 0                 NOT NULL COMMENT '成功数',
    fail_count         INT      DEFAULT 0                 NOT NULL COMMENT '失败数',
    skip_count         INT      DEFAULT 0                 NOT NULL COMMENT '跳过数',
    error_summary      VARCHAR(512)                       NULL COMMENT '错误摘要',
    created_by         BIGINT                             NOT NULL COMMENT '创建人',
    updated_by         BIGINT                             NULL COMMENT '更新人',
    prechecked_at      DATETIME                           NULL COMMENT '预检时间',
    started_at         DATETIME                           NULL COMMENT '开始执行时间',
    completed_at       DATETIME                           NULL COMMENT '完成时间',
    remark             VARCHAR(256)                       NULL COMMENT '备注',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    INDEX idx_status_created (status, created_at DESC),
    INDEX idx_author_created (author_id, created_at DESC),
    INDEX idx_source_platform (source_platform)
) COMMENT '外部博客迁移任务表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE blog_migration_record
(
    id                BIGINT AUTO_INCREMENT COMMENT '迁移记录ID',
    task_id           BIGINT                             NOT NULL COMMENT '迁移任务ID',
    source_platform   VARCHAR(64)                        NOT NULL COMMENT '来源平台',
    external_post_id  VARCHAR(128)                       NOT NULL COMMENT '外部文章ID',
    idempotent_key    VARCHAR(256)                       NOT NULL COMMENT '幂等键：sourcePlatform:externalPostId',
    original_title    VARCHAR(128)                       NULL COMMENT '原始文章标题',
    status            TINYINT  DEFAULT 0                 NOT NULL COMMENT '状态：0-待处理，1-成功，2-失败，3-已跳过',
    target_article_id BIGINT                             NULL COMMENT '导入后的站内文章ID',
    error_message     VARCHAR(512)                       NULL COMMENT '错误信息',
    raw_content_json  LONGTEXT                           NULL COMMENT '原始文章JSON快照',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_task_idempotent (task_id, idempotent_key),
    INDEX idx_idempotent_key (idempotent_key),
    INDEX idx_task_status (task_id, status),
    INDEX idx_target_article (target_article_id)
) COMMENT '外部博客迁移文章记录表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

DROP TABLE IF EXISTS blog_migration_attachment;
CREATE TABLE blog_migration_attachment
(
    id            BIGINT AUTO_INCREMENT COMMENT '迁移附件ID',
    task_id       BIGINT                             NOT NULL COMMENT '迁移任务ID',
    record_id     BIGINT                             NOT NULL COMMENT '迁移文章记录ID',
    external_url  VARCHAR(1024)                      NOT NULL COMMENT '外部附件URL',
    original_name VARCHAR(128)                       NULL COMMENT '原始文件名',
    status        TINYINT  DEFAULT 0                 NOT NULL COMMENT '状态：0-待下载，1-成功，2-失败，3-已跳过',
    file_id       BIGINT                             NULL COMMENT '站内文件ID',
    file_url      VARCHAR(512)                       NULL COMMENT '站内访问URL',
    error_message VARCHAR(512)                       NULL COMMENT '错误信息',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_record_url (record_id, external_url(766)),
    INDEX idx_task_status (task_id, status),
    INDEX idx_file_id (file_id)
) COMMENT '外部博客迁移附件记录表'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS blog_backend CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
use blog_backend;
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint                               NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        varchar(50)                          NOT NULL COMMENT '用户名',
    `password`        varchar(255) COMMENT '密码（BCrypt加密）',
    `nickname`        varchar(100) COMMENT '昵称',
    `email`           varchar(128) COMMENT '邮箱',
    `phone`           varchar(20) COMMENT '手机号',
    `avatar`          varchar(500) COMMENT '头像URL',
    `bio`             varchar(500) COMMENT '个人简介',
    `website`         varchar(255) COMMENT '个人站点',
    `gender`          tinyint(1) DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女, 3-保密',
    `birthday`        date COMMENT '生日',
    `status`          tinyint(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `user_level`      tinyint(1) DEFAULT 1 COMMENT '用户等级：1-10，第一阶段默认 1',
    `experience_points` int DEFAULT 0 COMMENT '经验值',
    `level_updated_at` datetime COMMENT '最近一次等级变更时间',
    `last_login_time` datetime COMMENT '最后登录时间',
    `last_login_ip`   varchar(50) COMMENT '最后登录IP',
    `created_at`      datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`      datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_flag`    tinyint(1) DEFAULT 0                 NOT NULL COMMENT '删除标记：0-未删除，1-已删除',
    `remark`          varchar(500) COMMENT '备注',
    `mfa_enabled`     tinyint(1) DEFAULT 0 COMMENT 'MFA是否启用：0-否，1-是',
    `active_username` varchar(50) GENERATED ALWAYS AS (IF(deleted_flag = 0, username, NULL)) STORED COMMENT '有效用户名唯一键辅助列',
    `active_email`    varchar(128) GENERATED ALWAYS AS (IF(deleted_flag = 0, NULLIF(TRIM(email), ''), NULL)) STORED COMMENT '有效邮箱唯一键辅助列',
    `active_phone`    varchar(20) GENERATED ALWAYS AS (IF(deleted_flag = 0, NULLIF(TRIM(phone), ''), NULL)) STORED COMMENT '有效手机号唯一键辅助列',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_sys_user_active_username` (`active_username`) COMMENT '有效用户名唯一索引',
    UNIQUE KEY `uk_sys_user_active_email` (`active_email`) COMMENT '有效邮箱唯一索引',
    UNIQUE KEY `uk_sys_user_active_phone` (`active_phone`) COMMENT '有效手机号唯一索引',
    KEY `idx_user_created_at` (`created_at`) COMMENT '创建时间索引',
    KEY `idx_user_deleted_username` (`deleted_flag`, `username`) COMMENT '有效用户名查询索引',
    KEY `idx_user_email` (`email`) COMMENT '邮箱索引',
    KEY `idx_user_phone` (`phone`) COMMENT '手机号索引'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '用户信息表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `parent_id`   bigint      NOT NULL COMMENT '父菜单ID',
    `tree_path`   varchar(255) COMMENT '父节点ID路径',
    `name`        varchar(64) NOT NULL COMMENT '菜单名称',
    `type`        char(1)     NOT NULL COMMENT '菜单类型（C-目录 M-菜单 B-按钮）',
    `route_name`  varchar(255) COMMENT '路由名称（Vue Router 中用于命名路由）',
    `route_path`  varchar(128) COMMENT '路由路径（Vue Router 中定义的 URL 路径）',
    `component`   varchar(128) COMMENT '组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）',
    `perm`        varchar(128) COMMENT '【按钮】权限标识',
    `always_show` tinyint    DEFAULT 0 COMMENT '【目录】只有一个子路由是否始终显示（1-是 0-否）',
    `keep_alive`  tinyint    DEFAULT 0 COMMENT '【菜单】是否开启页面缓存（1-是 0-否）',
    `visible`     tinyint(1) DEFAULT 1 COMMENT '显示状态（1-显示 0-隐藏）',
    `sort`        int        DEFAULT 0 COMMENT '排序',
    `icon`        varchar(64) COMMENT '菜单图标',
    `redirect`    varchar(128) COMMENT '跳转路径',
    `create_time` datetime    NULL COMMENT '创建时间',
    `update_time` datetime    NULL COMMENT '更新时间',
    `params`      json        NULL COMMENT '路由参数',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '系统菜单表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`               bigint      NOT NULL AUTO_INCREMENT,
    `name`             varchar(64) NOT NULL COMMENT '角色名称',
    `code`             varchar(32) NOT NULL COMMENT '角色编码',
    `sort`             int         NULL COMMENT '显示顺序',
    `status`           tinyint(1) DEFAULT 1 COMMENT '角色状态(1-正常 0-停用)',
    `data_scope`       tinyint     NULL COMMENT '数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)',
    `create_by`        bigint      NULL COMMENT '创建人 ID',
    `create_time`      datetime    NULL COMMENT '创建时间',
    `update_by`        bigint      NULL COMMENT '更新人ID',
    `update_time`      datetime    NULL COMMENT '更新时间',
    `is_deleted`       tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
    `active_role_name` varchar(64) GENERATED ALWAYS AS (IF(is_deleted = 0, NULLIF(TRIM(name), ''), NULL)) STORED COMMENT '有效角色名称唯一辅助列',
    `active_role_code` varchar(32) GENERATED ALWAYS AS (IF(is_deleted = 0, NULLIF(TRIM(code), ''), NULL)) STORED COMMENT '有效角色编码唯一辅助列',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_sys_role_active_name` (`active_role_name` ASC) USING BTREE COMMENT '有效角色名称唯一索引',
    UNIQUE INDEX `uk_sys_role_active_code` (`active_role_code` ASC) USING BTREE COMMENT '有效角色编码唯一索引',
    KEY `idx_role_deleted_name` (`is_deleted`, `name`) COMMENT '角色名称查询索引',
    KEY `idx_role_deleted_code` (`is_deleted`, `code`) COMMENT '角色编码查询索引'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '系统角色表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `menu_id` bigint NOT NULL COMMENT '菜单ID',
    UNIQUE INDEX `uk_roleid_menuid` (`role_id` ASC, `menu_id` ASC) USING BTREE COMMENT '角色菜单唯一索引',
    KEY `idx_role_menu_menu_id` (`menu_id`) COMMENT '按菜单清理角色关系索引'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '角色菜单关联表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
    KEY `idx_user_role_role_id` (`role_id`) COMMENT '按角色清理用户关系索引'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '用户角色关联表';

DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module`           varchar(50)  NOT NULL COMMENT '日志模块',
    `request_method`   varchar(64)  NOT NULL COMMENT '请求方式',
    `request_params`   text COMMENT '请求参数(批量请求参数可能会超过text)',
    `response_content` mediumtext COMMENT '返回参数',
    `content`          varchar(255) NOT NULL COMMENT '日志内容',
    `request_uri`      varchar(255) COMMENT '请求路径',
    `method`           varchar(255) COMMENT '方法名',
    `ip`               varchar(45) COMMENT 'IP地址',
    `province`         varchar(100) COMMENT '省份',
    `city`             varchar(100) COMMENT '城市',
    `execution_time`   bigint COMMENT '执行时间(ms)',
    `browser`          varchar(100) COMMENT '浏览器',
    `browser_version`  varchar(100) COMMENT '浏览器版本',
    `os`               varchar(100) COMMENT '终端系统',
    `create_by`        bigint COMMENT '创建人ID',
    `create_time`      datetime COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统操作日志表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `id`                bigint                 NOT NULL AUTO_INCREMENT,
    `config_name`       varchar(50)            NOT NULL COMMENT '配置名称',
    `config_key`        varchar(50)            NOT NULL COMMENT '配置key',
    `config_value`      varchar(100)           NOT NULL COMMENT '配置值',
    `remark`            varchar(255) COMMENT '备注',
    `create_time`       datetime COMMENT '创建时间',
    `create_by`         bigint COMMENT '创建人ID',
    `update_time`       datetime COMMENT '更新时间',
    `update_by`         bigint COMMENT '更新人ID',
    `is_deleted`        tinyint(4) DEFAULT '0' NOT NULL COMMENT '逻辑删除标识(0-未删除 1-已删除)',
    `active_config_key` varchar(50) GENERATED ALWAYS AS (IF(is_deleted = 0, NULLIF(TRIM(config_key), ''), NULL)) STORED COMMENT '有效配置键唯一辅助列',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_active_key` (`active_config_key`) COMMENT '有效配置键唯一索引',
    KEY `idx_config_key_deleted` (`config_key`, `is_deleted`) COMMENT '配置键查询索引'
) ENGINE = InnoDB COMMENT ='系统配置表';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `title`           varchar(50) COMMENT '通知标题',
    `content`         text COMMENT '通知内容',
    `type`            tinyint     NOT NULL COMMENT '通知类型（关联字典编码：notice_type）',
    `level`           varchar(16) NOT NULL COMMENT '通知等级（字典code：notice_level，如 info/success/warning/danger）',
    `target_type`     tinyint     NOT NULL COMMENT '目标类型（1: 全体, 2: 指定）',
    `target_user_ids` varchar(255) COMMENT '目标人ID集合（多个使用英文逗号,分割）',
    `publisher_id`    bigint COMMENT '发布人ID',
    `publish_status`  tinyint    DEFAULT '0' COMMENT '发布状态（0: 未发布, 1: 已发布, -1: 已撤回）',
    `publish_time`    datetime COMMENT '发布时间',
    `revoke_time`     datetime COMMENT '撤回时间',
    `create_by`       bigint      NOT NULL COMMENT '创建人ID',
    `create_time`     datetime    NOT NULL COMMENT '创建时间',
    `update_by`       bigint COMMENT '更新人ID',
    `update_time`     datetime COMMENT '更新时间',
    `is_deleted`      tinyint(1) DEFAULT '0' COMMENT '是否删除（0: 未删除, 1: 已删除）',
    `business_type`   varchar(32) COMMENT '业务目标类型，例如 ai_agent_task',
    `business_id`     bigint COMMENT '业务目标 ID',
    `action_path`     varchar(255) COMMENT '前端跳转路径',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_notice_admin_page` (`is_deleted`, `create_time`, `id`) COMMENT '后台通知列表索引',
    KEY `idx_notice_publish_scope_time` (`is_deleted`, `publish_status`, `target_type`, `publish_time`, `id`) COMMENT '用户收件箱索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统通知公告表';

DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT 'id',
    `notice_id`   bigint   NOT NULL COMMENT '公共通知id',
    `user_id`     bigint   NOT NULL COMMENT '用户id',
    `is_read`     tinyint DEFAULT '0' COMMENT '读取状态（0: 未读, 1: 已读）',
    `read_time`   datetime COMMENT '阅读时间',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime COMMENT '更新时间',
    `is_deleted`  tinyint DEFAULT '0' COMMENT '逻辑删除(0: 未删除, 1: 已删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`) COMMENT '同一通知同一用户唯一',
    KEY `idx_user_notice_inbox` (`user_id`, `is_deleted`, `is_read`, `notice_id`) COMMENT '用户收件箱索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户通知公告关联表';





DROP TABLE IF EXISTS `sys_author_application`;
CREATE TABLE `sys_author_application`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `user_id`           bigint      NOT NULL COMMENT '申请用户ID',
    `apply_status`      tinyint     DEFAULT 0 COMMENT '申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充',
    `apply_reason`      varchar(512) COMMENT '申请说明',
    `content_direction` varchar(128) COMMENT '擅长内容方向',
    `introduction`      varchar(1024) COMMENT '个人简介',
    `sample_links_json` json COMMENT '示例链接JSON数组',
    `reviewer_id`       bigint COMMENT '审核人ID',
    `review_comment`    varchar(512) COMMENT '审核备注',
    `submitted_at`      datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '提交时间',
    `reviewed_at`       datetime COMMENT '审核时间',
    `created_at`        datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`        datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_author_application_user_status` (`user_id`, `apply_status`, `submitted_at` DESC) COMMENT '按用户查询申请记录',
    KEY `idx_author_application_status_submitted` (`apply_status`, `submitted_at` DESC) COMMENT '按状态处理申请'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作者申请表';

DROP TABLE IF EXISTS `sys_user_notification_setting`;
CREATE TABLE `sys_user_notification_setting`
(
    `id`                           bigint   NOT NULL AUTO_INCREMENT COMMENT '设置ID',
    `user_id`                      bigint   NOT NULL COMMENT '用户ID',
    `comment_notice_enabled`       tinyint DEFAULT 1 COMMENT '评论通知开关',
    `like_notice_enabled`          tinyint DEFAULT 1 COMMENT '点赞通知开关',
    `collect_notice_enabled`       tinyint DEFAULT 1 COMMENT '收藏通知开关',
    `follow_notice_enabled`        tinyint DEFAULT 1 COMMENT '关注通知开关',
    `private_chat_notice_enabled`  tinyint DEFAULT 1 COMMENT '私聊通知开关',
    `mention_notice_enabled`       tinyint DEFAULT 1 COMMENT '@提醒开关',
    `channel_announcement_enabled` tinyint DEFAULT 1 COMMENT '频道公告通知开关',
    `system_notice_enabled`        tinyint DEFAULT 1 COMMENT '系统公告通知开关',
    `ai_task_notice_enabled`       tinyint DEFAULT 1 COMMENT 'AI任务完成通知开关',
    `report_result_notice_enabled` tinyint DEFAULT 1 COMMENT '举报处理结果通知开关',
    `forum_essence_notice_enabled`  tinyint DEFAULT 1 COMMENT '论坛精华通知开关',
    `forum_reply_notice_enabled`    tinyint DEFAULT 1 COMMENT '论坛回复通知开关',
    `forum_like_notice_enabled`     tinyint DEFAULT 1 COMMENT '论坛点赞通知开关',
    `created_at`                   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`                   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user_notification_setting` (`user_id`) COMMENT '每个用户一份通知设置'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户通知偏好设置表';

DROP TABLE IF EXISTS `ai_channel_config`;
CREATE TABLE `ai_channel_config`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `channel_code`           varchar(64) NOT NULL COMMENT '渠道编码',
    `channel_name`           varchar(128) NOT NULL COMMENT '渠道名称',
    `provider`               varchar(64) NOT NULL COMMENT '提供方',
    `model_name`             varchar(128) NOT NULL COMMENT '模型名称',
    `api_base_url`           varchar(512) COMMENT '接口基础地址',
    `api_key_encrypted`      text COMMENT '加密后的 API Key',
    `daily_quota`            int          DEFAULT 0 COMMENT '全局每日额度：0-不限制',
    `user_daily_quota`       int          DEFAULT 0 COMMENT '单用户每日额度：0-不限制',
    `max_context_tokens`     int          DEFAULT 0 COMMENT '上下文长度上限：0-不限制',
    `data_scope_json`        json COMMENT '可读取数据范围配置JSON',
    `system_prompt_template` text COMMENT '系统提示词模板',
    `status`                 tinyint      DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `is_default`             tinyint      DEFAULT 0 COMMENT '是否默认渠道：0-否，1-是',
    `created_by`             bigint COMMENT '创建人ID',
    `updated_by`             bigint COMMENT '更新人ID',
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`             datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_channel_config_channel_code` (`channel_code`) COMMENT '渠道编码唯一',
    KEY `idx_ai_channel_config_status_default` (`status`, `is_default`, `id`) COMMENT '启用和默认渠道查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 渠道配置表';

DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session`
(
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`           bigint      NOT NULL COMMENT '用户ID',
    `channel_config_id` bigint      NOT NULL COMMENT '渠道配置ID',
    `title`             varchar(256) COMMENT '会话标题',
    `scene_type`        varchar(32) DEFAULT 'general' COMMENT '会话场景：general/article/chat/profile',
    `status`            tinyint     DEFAULT 1 COMMENT '状态：0-关闭，1-正常',
    `last_message_at`   datetime COMMENT '最后消息时间',
    `created_at`        datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`        datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_chat_session_user_updated` (`user_id`, `updated_at` DESC) COMMENT '用户最近会话',
    KEY `idx_ai_chat_session_channel_scene` (`channel_config_id`, `scene_type`, `status`) COMMENT '按渠道和场景查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 对话会话表';

DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message`
(
    `id`                  bigint      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`          bigint      NOT NULL COMMENT '会话ID',
    `user_id`             bigint COMMENT '关联用户ID（助手消息可为空）',
    `role_type`           varchar(16) NOT NULL COMMENT '角色类型：user/assistant/system',
    `content`             longtext    NOT NULL COMMENT '消息内容',
    `request_scene_type`  varchar(32) DEFAULT 'general' COMMENT '请求场景：general/article/chat/profile',
    `request_target_id`   bigint COMMENT '关联目标ID（文章/聊天等）',
    `token_count`         int         DEFAULT 0 COMMENT '消息 token 数',
    `data_scope_snapshot` json COMMENT '当次读取范围快照 JSON',
    `response_status`     tinyint     DEFAULT 1 COMMENT '响应状态：0-失败，1-成功',
    `error_message`       varchar(512) COMMENT '错误信息',
    `created_at`          datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_chat_message_session_created` (`session_id`, `id`) COMMENT '按会话顺序查询消息',
    KEY `idx_ai_chat_message_user_created` (`user_id`, `created_at` DESC) COMMENT '按用户查看消息历史'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 对话消息表';

DROP TABLE IF EXISTS `ai_usage_log`;
CREATE TABLE `ai_usage_log`
(
    `id`                 bigint      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`            bigint      NOT NULL COMMENT '用户ID',
    `channel_config_id`  bigint      NOT NULL COMMENT '渠道配置ID',
    `session_id`         bigint COMMENT '会话ID',
    `request_scene_type` varchar(32) DEFAULT 'general' COMMENT '请求场景：general/article/chat/profile',
    `request_tokens`     int         DEFAULT 0 COMMENT '请求 token 数',
    `response_tokens`    int         DEFAULT 0 COMMENT '响应 token 数',
    `total_tokens`       int         DEFAULT 0 COMMENT '总 token 数',
    `quota_cost`         int         DEFAULT 1 COMMENT '额度消耗',
    `success_status`     tinyint     DEFAULT 1 COMMENT '成功状态：0-失败，1-成功',
    `error_code`         varchar(64) COMMENT '错误码',
    `created_at`         datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_usage_log_user_created` (`user_id`, `created_at` DESC) COMMENT '按用户查询使用记录',
    KEY `idx_ai_usage_log_channel_created` (`channel_config_id`, `created_at` DESC) COMMENT '按渠道查询调用记录'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 使用日志表';

-- ============================================
-- AI 知识源配置表
-- 管理员可启用/禁用各类知识源，配置同步间隔
-- ============================================
DROP TABLE IF EXISTS `ai_knowledge_source_config`;
CREATE TABLE `ai_knowledge_source_config`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `source_type`      varchar(32)  NOT NULL COMMENT '知识源类型：public_article/author_profile/forum_post/admin_entry',
    `enabled`          tinyint      DEFAULT 1 NOT NULL COMMENT '是否启用：0-禁用，1-启用',
    `sync_interval`    int          DEFAULT 3600 COMMENT '同步间隔（秒），默认1小时',
    `last_synced_at`   datetime     NULL COMMENT '最近一次同步完成时间',
    `last_sync_status` varchar(16)  NULL COMMENT '最近同步状态：success/failed',
    `config_json`      json         NULL COMMENT '扩展配置JSON（预留）',
    `updated_by`       bigint       NULL COMMENT '更新人ID',
    `remark`           varchar(512) NULL COMMENT '备注',
    `created_at`       datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`       datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_knowledge_source_type` (`source_type`) COMMENT '知识源类型唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 知识源配置表';

-- ============================================
-- AI 知识条目表
-- 存放从各知识源同步提取后的结构化条目
-- ============================================
DROP TABLE IF EXISTS `ai_knowledge_entry`;
CREATE TABLE `ai_knowledge_entry`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '条目ID',
    `source_type`       varchar(32)  NOT NULL COMMENT '来源类型：public_article/author_profile/forum_post/admin_entry',
    `source_id`         bigint       NOT NULL COMMENT '来源对象ID（文章ID/帖子ID/用户ID等）',
    `title`             varchar(256) NOT NULL COMMENT '标题',
    `summary`           text         NULL COMMENT '摘要',
    `content_snapshot`  longtext     NULL COMMENT '内容快照（用于向量化和检索）',
    `source_url`        varchar(512) NULL COMMENT '来源页面URL（预留）',
    `author_id`         bigint       NULL COMMENT '原始作者ID',
    `status`            tinyint      DEFAULT 1 NOT NULL COMMENT '状态：0-禁用，1-正常，2-过期，3-已删除',
    `version`           int          DEFAULT 1 NOT NULL COMMENT '版本号（源内容变更时递增）',
    `chunk_count`       int          DEFAULT 0 NOT NULL COMMENT '分块数量',
    `source_updated_at` datetime     NULL COMMENT '源内容最后更新时间（用于增量同步判断）',
    `synced_at`         datetime     NULL COMMENT '最近一次同步时间',
    `tag_json`          json         NULL COMMENT '标签JSON数组（预留）',
    `extra_json`        json         NULL COMMENT '扩展字段JSON（预留）',
    `created_at`        datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`        datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_knowledge_entry_source` (`source_type`, `source_id`) COMMENT '同源同对象唯一',
    KEY `idx_ai_knowledge_entry_type_status` (`source_type`, `status`, `updated_at` DESC) COMMENT '按类型和状态查询',
    KEY `idx_ai_knowledge_entry_author` (`author_id`, `status`) COMMENT '按作者查询',
    KEY `idx_ai_knowledge_entry_synced` (`synced_at`) COMMENT '按同步时间查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 知识条目表';

-- ============================================
-- AI 知识同步任务表
-- 记录每次知识源同步任务的执行状态
-- ============================================
DROP TABLE IF EXISTS `ai_knowledge_sync_task`;
CREATE TABLE `ai_knowledge_sync_task`
(
    `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_type`       varchar(32)   NOT NULL COMMENT '任务类型：full_sync/incremental_sync/single_entry',
    `source_type`     varchar(32)   NOT NULL COMMENT '知识源类型：public_article/author_profile/forum_post/admin_entry',
    `status`          tinyint       DEFAULT 0 NOT NULL COMMENT '状态：0-待执行，1-执行中，2-已完成，3-失败',
    `total_count`     int           DEFAULT 0 NOT NULL COMMENT '总条目数',
    `success_count`   int           DEFAULT 0 NOT NULL COMMENT '成功条目数',
    `fail_count`      int           DEFAULT 0 NOT NULL COMMENT '失败条目数',
    `skip_count`      int           DEFAULT 0 NOT NULL COMMENT '跳过条目数',
    `error_message`   varchar(1024) NULL COMMENT '错误信息',
    `retry_count`     int           DEFAULT 0 NOT NULL COMMENT '已重试次数',
    `max_retry`       int           DEFAULT 3 NOT NULL COMMENT '最大重试次数',
    `started_at`      datetime      NULL COMMENT '开始执行时间',
    `completed_at`    datetime      NULL COMMENT '执行完成时间',
    `triggered_by`    varchar(32)   DEFAULT 'system' NOT NULL COMMENT '触发方式：system/admin/manual',
    `operator_id`     bigint        NULL COMMENT '操作人ID（admin/manual触发时）',
    `remark`          varchar(512)  NULL COMMENT '备注',
    `created_at`      datetime      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`      datetime      DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_sync_task_type_status` (`source_type`, `status`, `created_at` DESC) COMMENT '按类型和状态查询',
    KEY `idx_ai_sync_task_created` (`created_at` DESC) COMMENT '按创建时间排序'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 知识同步任务表';

DROP TABLE IF EXISTS `sys_report_record`;
CREATE TABLE `sys_report_record`
(
    `id`                 bigint      NOT NULL AUTO_INCREMENT COMMENT '举报单ID',
    `report_target_type` varchar(32) NOT NULL COMMENT '举报对象类型：article/comment/chat_message',
    `report_target_id`   bigint      NOT NULL COMMENT '举报对象ID',
    `reporter_user_id`   bigint      NOT NULL COMMENT '举报人ID',
    `reason_code`        varchar(32) COMMENT '举报原因编码',
    `reason_detail`      varchar(1024) COMMENT '举报说明',
    `status`             tinyint     DEFAULT 0 COMMENT '状态：0-待处理，1-处理中，2-已处理，3-已驳回',
    `handler_user_id`    bigint COMMENT '当前处理人ID',
    `result_type`        varchar(32) COMMENT '处理结果：delete_content/revoke_message/mute_user/ban_user/record_only',
    `punishment_type`    varchar(32) COMMENT '处罚类型：content_delete/message_revoke/mute/ban/none',
    `evidence_json`      json COMMENT '补充证据JSON',
    `reported_at`        datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '举报时间',
    `handled_at`         datetime COMMENT '处理完成时间',
    `remark`             varchar(512) COMMENT '备注',
    `created_at`         datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`         datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_sys_report_record_target_status` (`report_target_type`, `report_target_id`, `status`) COMMENT '按对象查看举报记录',
    KEY `idx_sys_report_record_reporter_time` (`reporter_user_id`, `reported_at` DESC) COMMENT '按举报人查看历史',
    KEY `idx_sys_report_record_status_reported` (`status`, `reported_at` DESC) COMMENT '按状态处理举报'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='举报单主表';

DROP TABLE IF EXISTS `sys_report_handle_log`;
CREATE TABLE `sys_report_handle_log`
(
    `id`               bigint      NOT NULL AUTO_INCREMENT COMMENT '处理日志ID',
    `report_id`        bigint      NOT NULL COMMENT '举报单ID',
    `from_status`      tinyint COMMENT '变更前状态',
    `to_status`        tinyint     NOT NULL COMMENT '变更后状态',
    `action_type`      varchar(32) NOT NULL COMMENT '动作类型：claim/approve/reject/close/reassign',
    `action_result`    varchar(32) COMMENT '动作结果/处罚类型',
    `operator_user_id` bigint      NOT NULL COMMENT '操作人ID',
    `action_remark`    varchar(512) COMMENT '处理备注',
    `created_at`       datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_sys_report_handle_log_report_created` (`report_id`, `created_at` DESC) COMMENT '按举报单查看处理历史',
    KEY `idx_sys_report_handle_log_operator_created` (`operator_user_id`, `created_at` DESC) COMMENT '按操作人查看历史'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='举报处理日志表';

DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operator_user_id` bigint       NOT NULL COMMENT '操作人ID',
    `target_user_id`   bigint       COMMENT '目标用户ID',
    `operation_type`   varchar(64)  NOT NULL COMMENT '操作类型',
    `target_type_name` varchar(64)  COMMENT '目标对象类型名称',
    `target_id`        bigint       COMMENT '目标对象ID',
    `before_state`     json         COMMENT '操作前状态(JSON)',
    `after_state`      json         COMMENT '操作后状态(JSON)',
    `mfa_passed`       tinyint(1)   DEFAULT 0 COMMENT '2FA是否通过',
    `request_ip`       varchar(45)  COMMENT '请求IP',
    `user_agent`       varchar(512) COMMENT 'User-Agent',
    `remark`           varchar(512) COMMENT '备注',
    `created_at`       datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_audit_operator_created` (`operator_user_id`, `created_at` DESC),
    KEY `idx_audit_target_created` (`target_user_id`, `created_at` DESC),
    KEY `idx_audit_operation_type` (`operation_type`, `created_at` DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='高风险审计日志表';

-- ========== AI Agent 模块 ==========

DROP TABLE IF EXISTS `ai_agent_definition`;
CREATE TABLE `ai_agent_definition`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              varchar(64)  NOT NULL COMMENT 'agent 名称',
    `description`       varchar(512) COMMENT 'agent 描述',
    `system_prompt`     text         NOT NULL COMMENT '系统提示词',
    `channel_config_id` bigint       NOT NULL COMMENT '关联 AI 渠道配置 ID',
    `data_scope_json`   json         COMMENT '数据读取范围配置（复用 AiDataScopeEnum）',
    `enabled`           tinyint      DEFAULT 1 NOT NULL COMMENT '0-停用 1-启用',
    `max_turns`         int          DEFAULT 1 NOT NULL COMMENT '最大对话轮次',
    `extra_config_json` json         COMMENT '扩展配置（预留）',
    `created_by`        bigint       COMMENT '创建人',
    `updated_by`        bigint       COMMENT '更新人',
    `created_at`        datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`        datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_def_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI Agent 定义表';

DROP TABLE IF EXISTS `ai_agent_task`;
CREATE TABLE `ai_agent_task`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        bigint       NOT NULL COMMENT '发起用户',
    `agent_id`       bigint       NOT NULL COMMENT '关联 agent 定义 ID',
    `status`         tinyint      DEFAULT 0 NOT NULL COMMENT '0-待执行 1-执行中 2-已完成 3-失败 4-已取消',
    `input_content`  text         NOT NULL COMMENT '用户输入',
    `output_content` text         COMMENT 'agent 输出',
    `error_message`  varchar(1024) COMMENT '错误信息',
    `token_count`    int          DEFAULT 0 COMMENT '消耗 token 数',
    `started_at`     datetime     COMMENT '开始时间',
    `completed_at`   datetime     COMMENT '完成时间',
    `created_at`     datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`     datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_task_user_status` (`user_id`, `status`),
    KEY `idx_agent_task_agent_status` (`agent_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI Agent 任务表';

DROP TABLE IF EXISTS `ai_agent_task_log`;
CREATE TABLE `ai_agent_task_log`
(
    `id`         bigint  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`    bigint  NOT NULL COMMENT '关联任务 ID',
    `turn_index` int     NOT NULL COMMENT '轮次序号',
    `role_type`  varchar(16) NOT NULL COMMENT 'user / assistant / system',
    `content`    text    NOT NULL COMMENT '消息内容',
    `token_count` int    DEFAULT 0 COMMENT 'token 数',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_task_log_task_turn` (`task_id`, `turn_index`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI Agent 任务日志表';

-- ========== AI 工具与 MCP 模块 ==========

DROP TABLE IF EXISTS `ai_tool_definition`;
CREATE TABLE `ai_tool_definition`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tool_code`          varchar(64)  NOT NULL COMMENT '工具编码',
    `tool_name`          varchar(128) NOT NULL COMMENT '工具名称',
    `source_type`        varchar(16)  NOT NULL COMMENT '来源类型 builtin/mcp',
    `mcp_server_id`      bigint       NULL COMMENT 'MCP 服务ID',
    `mcp_tool_name`      varchar(128) NULL COMMENT 'MCP 原始工具名',
    `description`        varchar(512) NULL COMMENT '工具描述',
    `parameters_schema`  json         NULL COMMENT '参数 Schema',
    `result_schema`      json         NULL COMMENT '返回 Schema',
    `risk_level`         varchar(16)  NOT NULL COMMENT '风险等级 low/medium/high',
    `use_scenarios`      json         NULL COMMENT '适用场景 JSON 数组',
    `enabled`            tinyint      DEFAULT 1 NOT NULL COMMENT '状态：0-停用，1-启用',
    `created_by`         bigint       NULL COMMENT '创建人ID',
    `updated_by`         bigint       NULL COMMENT '更新人ID',
    `created_at`         datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`         datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_tool_definition_code` (`tool_code`) COMMENT '工具编码唯一',
    KEY `idx_ai_tool_definition_source_enabled` (`source_type`, `enabled`, `id`) COMMENT '按来源和状态查询',
    KEY `idx_ai_tool_definition_mcp_server` (`mcp_server_id`, `enabled`) COMMENT '按 MCP 服务查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 工具定义表';

DROP TABLE IF EXISTS `ai_tool_authorization`;
CREATE TABLE `ai_tool_authorization`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tool_id`             bigint       NOT NULL COMMENT '工具ID',
    `authorization_type`  varchar(32)  NOT NULL COMMENT '授权类型 agent/scene/permission/data_scope',
    `authorization_key`   varchar(128) NOT NULL COMMENT '授权键',
    `data_scope`          varchar(128) NULL COMMENT '数据范围',
    `enabled`             tinyint      DEFAULT 1 NOT NULL COMMENT '状态：0-停用，1-启用',
    `created_by`          bigint       NULL COMMENT '创建人ID',
    `updated_by`          bigint       NULL COMMENT '更新人ID',
    `created_at`          datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`          datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_tool_authorization_tool_type` (`tool_id`, `authorization_type`, `enabled`) COMMENT '按工具授权查询',
    KEY `idx_ai_tool_authorization_key` (`authorization_type`, `authorization_key`) COMMENT '按授权键查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 工具授权表';

DROP TABLE IF EXISTS `ai_tool_call_log`;
CREATE TABLE `ai_tool_call_log`
(
    `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`            bigint        NULL COMMENT '用户ID',
    `agent_id`           bigint        NULL COMMENT 'Agent ID',
    `session_id`         bigint        NULL COMMENT '会话ID',
    `task_id`            bigint        NULL COMMENT '任务ID',
    `tool_id`            bigint        NOT NULL COMMENT '工具ID',
    `tool_code`          varchar(64)   NOT NULL COMMENT '工具编码',
    `tool_name`          varchar(128)  NOT NULL COMMENT '工具名称',
    `request_scene_type` varchar(32)   NULL COMMENT '请求场景',
    `request_summary`    varchar(1024) NULL COMMENT '入参摘要',
    `response_summary`   varchar(1024) NULL COMMENT '结果摘要',
    `success_status`     tinyint       DEFAULT 1 NOT NULL COMMENT '成功状态：0-失败，1-成功',
    `elapsed_ms`         bigint        NULL COMMENT '耗时毫秒',
    `error_message`      varchar(1024) NULL COMMENT '错误信息',
    `created_at`         datetime      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_ai_tool_call_log_tool_created` (`tool_id`, `created_at` DESC) COMMENT '按工具查询调用记录',
    KEY `idx_ai_tool_call_log_user_created` (`user_id`, `created_at` DESC) COMMENT '按用户查询调用记录',
    KEY `idx_ai_tool_call_log_agent_created` (`agent_id`, `created_at` DESC) COMMENT '按 Agent 查询调用记录'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 工具调用日志表';

DROP TABLE IF EXISTS `ai_mcp_server_config`;
CREATE TABLE `ai_mcp_server_config`
(
    `id`                  bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `server_name`         varchar(128) NOT NULL COMMENT '服务名称',
    `transport_type`      varchar(16)  NOT NULL COMMENT '传输类型 stdio/http',
    `connection_config_json` json      NOT NULL COMMENT '连接配置 JSON',
    `auth_config_json`     json         NULL COMMENT '鉴权配置 JSON',
    `timeout_seconds`      int          DEFAULT 60 NOT NULL COMMENT '超时时间（秒）',
    `enabled`              tinyint      DEFAULT 1 NOT NULL COMMENT '状态：0-停用，1-启用',
    `last_health_status`   varchar(32)  NULL COMMENT '最近健康状态',
    `last_discovered_at`   datetime     NULL COMMENT '最近发现时间',
    `last_error_summary`   varchar(1024) NULL COMMENT '最近错误摘要',
    `created_by`           bigint       NULL COMMENT '创建人ID',
    `updated_by`           bigint       NULL COMMENT '更新人ID',
    `created_at`           datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`           datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_mcp_server_config_name` (`server_name`) COMMENT '服务名称唯一',
    KEY `idx_ai_mcp_server_config_enabled` (`enabled`, `id`) COMMENT '按启停状态查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI MCP 服务配置表';

DROP TABLE IF EXISTS `ai_mcp_tool_snapshot`;
CREATE TABLE `ai_mcp_tool_snapshot`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mcp_server_id`        bigint       NOT NULL COMMENT 'MCP 服务ID',
    `mcp_tool_name`         varchar(128) NOT NULL COMMENT 'MCP 原始工具名',
    `tool_code`             varchar(64)  NOT NULL COMMENT '同步生成的工具编码',
    `tool_name`             varchar(128) NOT NULL COMMENT '同步生成的工具名称',
    `description`           varchar(512) NULL COMMENT '描述',
    `parameters_schema`     json         NULL COMMENT '参数 Schema',
    `result_schema`         json         NULL COMMENT '返回 Schema',
    `risk_level`            varchar(16)  NOT NULL COMMENT '风险等级',
    `use_scenarios`         json         NULL COMMENT '适用场景',
    `enabled`               tinyint      DEFAULT 1 NOT NULL COMMENT '状态：0-停用，1-启用',
    `discovered_at`         datetime     NULL COMMENT '发现时间',
    `raw_definition_json`   json         NULL COMMENT '原始定义 JSON',
    `last_error_summary`    varchar(1024) NULL COMMENT '错误摘要',
    `created_at`            datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`            datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_ai_mcp_tool_snapshot_tool` (`mcp_server_id`, `mcp_tool_name`) COMMENT '同一服务下工具唯一',
    KEY `idx_ai_mcp_tool_snapshot_tool_code` (`tool_code`) COMMENT '同步工具编码查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI MCP 工具快照表';

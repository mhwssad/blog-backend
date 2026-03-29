CREATE DATABASE IF NOT EXISTS blog_backend CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
use blog_backend;
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        varchar(50) NOT NULL COMMENT '用户名',
    `password`        varchar(255) COMMENT '密码（BCrypt加密）',
    `nickname`        varchar(100) COMMENT '昵称',
    `email`           varchar(128) COMMENT '邮箱',
    `phone`           varchar(20) COMMENT '手机号',
    `avatar`          varchar(500) COMMENT '头像URL',
    `gender`          tinyint(1) DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女, 3-保密',
    `birthday`        date COMMENT '生日',
    `status`          tinyint(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `last_login_time` datetime COMMENT '最后登录时间',
    `last_login_ip`   varchar(50) COMMENT '最后登录IP',
    `created_at`      datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updated_at`      datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_flag`    tinyint(1) DEFAULT 0 NOT NULL COMMENT '删除标记：0-未删除，1-已删除',
    `remark`          varchar(500) COMMENT '备注',
    `active_username` varchar(50) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN username ELSE NULL END) STORED COMMENT '有效用户名唯一键辅助列',
    `active_email`    varchar(128) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN NULLIF(TRIM(email), '') ELSE NULL END) STORED COMMENT '有效邮箱唯一键辅助列',
    `active_phone`    varchar(20) GENERATED ALWAYS AS (CASE WHEN deleted_flag = 0 THEN NULLIF(TRIM(phone), '') ELSE NULL END) STORED COMMENT '有效手机号唯一键辅助列',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_sys_user_active_username` (`active_username`) COMMENT '有效用户名唯一索引',
    UNIQUE KEY `uk_sys_user_active_email` (`active_email`) COMMENT '有效邮箱唯一索引',
    UNIQUE KEY `uk_sys_user_active_phone` (`active_phone`) COMMENT '有效手机号唯一索引',
    KEY `idx_user_created_at` (`created_at`) COMMENT '创建时间索引',
    KEY `idx_user_deleted_username` (`deleted_flag`, `username`) COMMENT '有效用户名查询索引',
    KEY `idx_user_email` (`email`) COMMENT '邮箱索引',
    KEY `idx_user_phone` (`phone`) COMMENT '手机号索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户信息表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `parent_id` bigint NOT NULL COMMENT '父菜单ID',
                             `tree_path` varchar(255) COMMENT '父节点ID路径',
                             `name` varchar(64) NOT NULL COMMENT '菜单名称',
                             `type` char(1) NOT NULL COMMENT '菜单类型（C-目录 M-菜单 B-按钮）',
                             `route_name` varchar(255) COMMENT '路由名称（Vue Router 中用于命名路由）',
                             `route_path` varchar(128) COMMENT '路由路径（Vue Router 中定义的 URL 路径）',
                             `component` varchar(128) COMMENT '组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）',
                             `perm` varchar(128) COMMENT '【按钮】权限标识',
                             `always_show` tinyint DEFAULT 0 COMMENT '【目录】只有一个子路由是否始终显示（1-是 0-否）',
                             `keep_alive` tinyint DEFAULT 0 COMMENT '【菜单】是否开启页面缓存（1-是 0-否）',
                             `visible` tinyint(1) DEFAULT 1 COMMENT '显示状态（1-显示 0-隐藏）',
                             `sort` int DEFAULT 0 COMMENT '排序',
                             `icon` varchar(64) COMMENT '菜单图标',
                             `redirect` varchar(128) COMMENT '跳转路径',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `params` json NULL COMMENT '路由参数',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统菜单表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `name` varchar(64) NOT NULL COMMENT '角色名称',
                             `code` varchar(32) NOT NULL COMMENT '角色编码',
                             `sort` int NULL COMMENT '显示顺序',
                             `status` tinyint(1) DEFAULT 1 COMMENT '角色状态(1-正常 0-停用)',
                             `data_scope` tinyint NULL COMMENT '数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)',
                             `create_by` bigint NULL COMMENT '创建人 ID',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_by` bigint NULL COMMENT '更新人ID',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
                             `active_role_name` varchar(64) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(name), '') ELSE NULL END) STORED COMMENT '有效角色名称唯一辅助列',
                             `active_role_code` varchar(32) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(code), '') ELSE NULL END) STORED COMMENT '有效角色编码唯一辅助列',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_sys_role_active_name`(`active_role_name` ASC) USING BTREE COMMENT '有效角色名称唯一索引',
                             UNIQUE INDEX `uk_sys_role_active_code`(`active_role_code` ASC) USING BTREE COMMENT '有效角色编码唯一索引',
                             KEY `idx_role_deleted_name` (`is_deleted`, `name`) COMMENT '角色名称查询索引',
                             KEY `idx_role_deleted_code` (`is_deleted`, `code`) COMMENT '角色编码查询索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统角色表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                  UNIQUE INDEX `uk_roleid_menuid`(`role_id` ASC, `menu_id` ASC) USING BTREE COMMENT '角色菜单唯一索引',
                                  KEY `idx_role_menu_menu_id` (`menu_id`) COMMENT '按菜单清理角色关系索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '角色菜单关联表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
                                  KEY `idx_user_role_role_id` (`role_id`) COMMENT '按角色清理用户关系索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户角色关联表';

DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                           `module` varchar(50) NOT NULL COMMENT '日志模块',
                           `request_method` varchar(64) NOT NULL COMMENT '请求方式',
                           `request_params` text COMMENT '请求参数(批量请求参数可能会超过text)',
                           `response_content` mediumtext COMMENT '返回参数',
                           `content` varchar(255) NOT NULL COMMENT '日志内容',
                           `request_uri` varchar(255) COMMENT '请求路径',
                           `method` varchar(255) COMMENT '方法名',
                           `ip` varchar(45) COMMENT 'IP地址',
                           `province` varchar(100) COMMENT '省份',
                           `city` varchar(100) COMMENT '城市',
                           `execution_time` bigint COMMENT '执行时间(ms)',
                           `browser` varchar(100) COMMENT '浏览器',
                           `browser_version` varchar(100) COMMENT '浏览器版本',
                           `os` varchar(100) COMMENT '终端系统',
                           `create_by` bigint COMMENT '创建人ID',
                           `create_time` datetime COMMENT '创建时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `config_name` varchar(50) NOT NULL COMMENT '配置名称',
                              `config_key` varchar(50) NOT NULL COMMENT '配置key',
                              `config_value` varchar(100) NOT NULL COMMENT '配置值',
                              `remark` varchar(255) COMMENT '备注',
                              `create_time` datetime COMMENT '创建时间',
                              `create_by` bigint COMMENT '创建人ID',
                              `update_time` datetime COMMENT '更新时间',
                              `update_by` bigint COMMENT '更新人ID',
                              `is_deleted` tinyint(4) DEFAULT '0' NOT NULL COMMENT '逻辑删除标识(0-未删除 1-已删除)',
                              `active_config_key` varchar(50) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN NULLIF(TRIM(config_key), '') ELSE NULL END) STORED COMMENT '有效配置键唯一辅助列',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_sys_config_active_key` (`active_config_key`) COMMENT '有效配置键唯一索引',
                              KEY `idx_config_key_deleted` (`config_key`, `is_deleted`) COMMENT '配置键查询索引'
) ENGINE=InnoDB COMMENT='系统配置表';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `title` varchar(50) COMMENT '通知标题',
                              `content` text COMMENT '通知内容',
                              `type` tinyint NOT NULL COMMENT '通知类型（关联字典编码：notice_type）',
                              `level` varchar(16) NOT NULL COMMENT '通知等级（字典code：notice_level，如 info/success/warning/danger）',
                              `target_type` tinyint NOT NULL COMMENT '目标类型（1: 全体, 2: 指定）',
                              `target_user_ids` varchar(255) COMMENT '目标人ID集合（多个使用英文逗号,分割）',
                              `publisher_id` bigint COMMENT '发布人ID',
                              `publish_status` tinyint DEFAULT '0' COMMENT '发布状态（0: 未发布, 1: 已发布, -1: 已撤回）',
                              `publish_time` datetime COMMENT '发布时间',
                              `revoke_time` datetime COMMENT '撤回时间',
                              `create_by` bigint NOT NULL COMMENT '创建人ID',
                              `create_time` datetime NOT NULL COMMENT '创建时间',
                              `update_by` bigint COMMENT '更新人ID',
                              `update_time` datetime COMMENT '更新时间',
                              `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除（0: 未删除, 1: 已删除）',
                              PRIMARY KEY (`id`) USING BTREE,
                              KEY `idx_notice_admin_page` (`is_deleted`, `create_time`, `id`) COMMENT '后台通知列表索引',
                              KEY `idx_notice_publish_scope_time` (`is_deleted`, `publish_status`, `target_type`, `publish_time`, `id`) COMMENT '用户收件箱索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告表';

DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                                   `notice_id` bigint NOT NULL COMMENT '公共通知id',
                                   `user_id` bigint NOT NULL COMMENT '用户id',
                                   `is_read` tinyint DEFAULT '0' COMMENT '读取状态（0: 未读, 1: 已读）',
                                   `read_time` datetime COMMENT '阅读时间',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `update_time` datetime COMMENT '更新时间',
                                   `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除(0: 未删除, 1: 已删除)',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`) COMMENT '同一通知同一用户唯一',
                                   KEY `idx_user_notice_inbox` (`user_id`, `is_deleted`, `is_read`, `notice_id`) COMMENT '用户收件箱索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知公告关联表';




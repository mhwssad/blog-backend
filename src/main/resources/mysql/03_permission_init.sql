USE blog_backend;

-- 权限初始化脚本
-- 执行顺序建议：
-- 1. 先执行 1.sys.sql 建表
-- 2. 再执行 02_article.sql、04_file.sql、05_chat.sql、06_follow.sql 建立业务域表
-- 3. 最后执行本脚本初始化基础数据、菜单、权限和演示内容

-- 兼容旧版本表结构：通知等级可能仍为 varchar(5)，无法容纳 success/warning
ALTER TABLE `sys_notice`
    MODIFY COLUMN `level` varchar(16) NOT NULL COMMENT '通知等级（字典code：notice_level，如 info/success/warning/danger）';

START TRANSACTION;

-- 清理内容域初始化数据
DELETE
FROM `sys_user_footprint`
WHERE `id` IN (1, 2);
DELETE
FROM `sys_interaction`
WHERE `id` IN (1, 2);
DELETE
FROM `sys_comment`
WHERE `id` IN (1, 2);
DELETE
FROM `sys_collection`
WHERE `id` IN (1);
DELETE
FROM `sys_collection_folder`
WHERE `id` IN (1);
DELETE
FROM `blog_article_access`
WHERE `id` IN (1, 2);
DELETE
FROM `blog_article_category`
WHERE `id` IN (1, 2, 3);
DELETE
FROM `sys_tag_relation`
WHERE `id` IN (1, 2, 3, 4, 5);
DELETE
FROM `blog_article`
WHERE `id` IN (1, 2, 3);
DELETE
FROM `sys_tag`
WHERE `id` IN (1, 2, 3, 4, 5);
DELETE
FROM `sys_category`
WHERE `id` IN (1, 2, 3, 4, 5);

-- 清理系统初始化数据
DELETE
FROM `sys_user_notice`
WHERE `id` IN (1)
   OR `notice_id` IN (1, 2);
DELETE
FROM `sys_notice`
WHERE `id` IN (1, 2);
DELETE
FROM `sys_config`
WHERE `id` IN (1, 2, 3, 4, 5, 6);
DELETE
FROM `sys_user_role`
WHERE `user_id` IN (1, 2)
   OR `role_id` IN (1, 2);
DELETE
FROM `sys_user`
WHERE `id` IN (1, 2);

-- 清理本脚本使用的菜单与授权数据
DELETE
FROM `sys_role_menu`
WHERE `menu_id` BETWEEN 1000 AND 1999;
DELETE
FROM `sys_menu`
WHERE `id` BETWEEN 1000 AND 1999;

-- 初始化超级管理员角色与普通用户角色
DELETE
FROM `sys_role_menu`
WHERE `role_id` IN (1, 2);
DELETE
FROM `sys_role`
WHERE `id` IN (1, 2);

INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`,
                        `update_time`, `is_deleted`)
VALUES (1, '超级管理员', 'admin', 1, 1, 1, 1, NOW(), 1, NOW(), 0),
       (2, '普通用户', 'user', 2, 1, 4, 1, NOW(), 1, NOW(), 0);

-- 初始化基础用户数据
-- 默认密码：password
INSERT INTO `sys_user`
(`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `birthday`, `status`,
 `last_login_time`, `last_login_ip`, `created_at`, `updated_at`, `deleted_flag`, `remark`)
VALUES (1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmweuJ1CwV5G6IwM/7Aic7QvYu2L3U3OBNun', '管理员', 'admin@blog.local',
        '13800000000', NULL, 3, NULL, 1, NULL, NULL, NOW(), NOW(), 0, '系统初始化超级管理员账号'),
       (2, 'demo', '$2a$10$dXJ3SW6G7P50lGmMkkmweuJ1CwV5G6IwM/7Aic7QvYu2L3U3OBNun', '演示用户', 'demo@blog.local',
        '13900000000', NULL, 3, NULL, 1, NULL, NULL, NOW(), NOW(), 0, '系统初始化演示账号');

-- 初始化系统参数
INSERT INTO `sys_config`
(`id`, `config_name`, `config_key`, `config_value`, `remark`, `create_time`, `create_by`, `update_time`, `update_by`,
 `is_deleted`)
VALUES (1, '站点名称', 'site.name', 'Cybz Blog', '系统初始化站点名称', NOW(), 1, NOW(), 1, 0),
       (2, '站点副标题', 'site.subtitle', 'Spring Boot Blog Backend', '系统初始化站点副标题', NOW(), 1, NOW(), 1, 0),
       (3, '开放注册', 'auth.allow-register', 'true', '控制前台是否允许开放注册', NOW(), 1, NOW(), 1, 0),
       (4, '默认文章访问级别', 'article.default-access-level', '0',
        '0-公开，1-登录可见，2-付费可见，3-VIP可见，4-指定用户可见', NOW(), 1, NOW(), 1, 0),
       (5, '登录失败锁定阈值', 'auth.login-fail.max-attempts', '5', '连续失败达到该次数后临时锁定账号，0 表示关闭该能力',
        NOW(), 1, NOW(), 1, 0),
       (6, '登录失败锁定时长(分钟)', 'auth.login-fail.lock-minutes', '15', '登录失败达到阈值后的锁定时长（分钟）', NOW(),
        1, NOW(), 1, 0);

-- 初始化系统通知
INSERT INTO `sys_notice`
(`id`, `title`, `content`, `type`, `level`, `target_type`, `target_user_ids`, `publisher_id`, `publish_status`,
 `publish_time`, `revoke_time`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES (1, '欢迎使用博客后台', '系统已完成初始化，您现在可以使用 admin 账号登录并开始配置角色、菜单和通知。', 1, 'info', 1,
        NULL, 1, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
       (2, '欢迎体验演示账号', '演示账号已预置示例文章、分类、标签和通知数据，可直接体验基础功能。', 1, 'success', 2, '2',
        1, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0);

-- 系统管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1000, 0, '0', '系统管理', 'C', 'System', '/system', 'Layout', NULL, 1, 0, 1, 1, 'system', NULL, NOW(), NOW(),
        NULL);

-- 用户管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1100, 1000, '0,1000', '用户管理', 'M', 'SysUser', 'users', 'system/user/index', NULL, 0, 1, 1, 1, 'user', NULL,
        NOW(), NOW(), NULL),
       (1101, 1100, '0,1000,1100', '用户查询', 'B', NULL, NULL, NULL, 'sys:user:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1102, 1100, '0,1000,1100', '用户新增', 'B', NULL, NULL, NULL, 'sys:user:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1103, 1100, '0,1000,1100', '用户修改', 'B', NULL, NULL, NULL, 'sys:user:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1104, 1100, '0,1000,1100', '用户删除', 'B', NULL, NULL, NULL, 'sys:user:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL),
       (1105, 1100, '0,1000,1100', '重置密码', 'B', NULL, NULL, NULL, 'sys:user:reset-password', 0, 0, 1, 5, NULL, NULL,
        NOW(), NOW(), NULL),
       (1106, 1100, '0,1000,1100', '分配角色', 'B', NULL, NULL, NULL, 'sys:user:assign-role', 0, 0, 1, 6, NULL, NULL,
        NOW(), NOW(), NULL);

-- 角色管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1200, 1000, '0,1000', '角色管理', 'M', 'SysRole', 'roles', 'system/role/index', NULL, 0, 1, 1, 2, 'peoples',
        NULL, NOW(), NOW(), NULL),
       (1201, 1200, '0,1000,1200', '角色查询', 'B', NULL, NULL, NULL, 'sys:role:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1202, 1200, '0,1000,1200', '角色新增', 'B', NULL, NULL, NULL, 'sys:role:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1203, 1200, '0,1000,1200', '角色修改', 'B', NULL, NULL, NULL, 'sys:role:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1204, 1200, '0,1000,1200', '角色删除', 'B', NULL, NULL, NULL, 'sys:role:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL),
       (1205, 1200, '0,1000,1200', '分配菜单', 'B', NULL, NULL, NULL, 'sys:role:assign-menu', 0, 0, 1, 5, NULL, NULL,
        NOW(), NOW(), NULL);

-- 菜单管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1300, 1000, '0,1000', '菜单管理', 'M', 'SysMenu', 'menus', 'system/menu/index', NULL, 0, 1, 1, 3, 'tree-table',
        NULL, NOW(), NOW(), NULL),
       (1301, 1300, '0,1000,1300', '菜单查询', 'B', NULL, NULL, NULL, 'sys:menu:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1302, 1300, '0,1000,1300', '菜单新增', 'B', NULL, NULL, NULL, 'sys:menu:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1303, 1300, '0,1000,1300', '菜单修改', 'B', NULL, NULL, NULL, 'sys:menu:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1304, 1300, '0,1000,1300', '菜单删除', 'B', NULL, NULL, NULL, 'sys:menu:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL);

-- 配置管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1400, 1000, '0,1000', '参数配置', 'M', 'SysConfig', 'configs', 'system/config/index', NULL, 0, 1, 1, 4, 'edit',
        NULL, NOW(), NOW(), NULL),
       (1401, 1400, '0,1000,1400', '配置查询', 'B', NULL, NULL, NULL, 'sys:config:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1402, 1400, '0,1000,1400', '配置新增', 'B', NULL, NULL, NULL, 'sys:config:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1403, 1400, '0,1000,1400', '配置修改', 'B', NULL, NULL, NULL, 'sys:config:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1404, 1400, '0,1000,1400', '配置删除', 'B', NULL, NULL, NULL, 'sys:config:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 通知管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1500, 1000, '0,1000', '通知管理', 'M', 'SysNotice', 'notices', 'system/notice/index', NULL, 0, 1, 1, 5,
        'message', NULL, NOW(), NOW(), NULL),
       (1501, 1500, '0,1000,1500', '通知查询', 'B', NULL, NULL, NULL, 'sys:notice:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1502, 1500, '0,1000,1500', '通知新增', 'B', NULL, NULL, NULL, 'sys:notice:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1503, 1500, '0,1000,1500', '通知修改', 'B', NULL, NULL, NULL, 'sys:notice:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1504, 1500, '0,1000,1500', '通知发布', 'B', NULL, NULL, NULL, 'sys:notice:publish', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL),
       (1505, 1500, '0,1000,1500', '通知撤回', 'B', NULL, NULL, NULL, 'sys:notice:revoke', 0, 0, 1, 5, NULL, NULL,
        NOW(), NOW(), NULL),
       (1506, 1500, '0,1000,1500', '通知删除', 'B', NULL, NULL, NULL, 'sys:notice:delete', 0, 0, 1, 6, NULL, NULL,
        NOW(), NOW(), NULL);

-- 日志管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1600, 1000, '0,1000', '日志管理', 'M', 'SysLog', 'logs', 'system/log/index', NULL, 0, 1, 1, 6, 'log', NULL,
        NOW(), NOW(), NULL),
       (1601, 1600, '0,1000,1600', '日志查询', 'B', NULL, NULL, NULL, 'sys:log:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1602, 1600, '0,1000,1600', '日志删除', 'B', NULL, NULL, NULL, 'sys:log:delete', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1603, 1600, '0,1000,1600', '日志清理', 'B', NULL, NULL, NULL, 'sys:log:clean', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL);

-- 内容管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1700, 0, '0', '内容管理', 'C', 'Content', '/content', 'Layout', NULL, 1, 0, 1, 2, 'document',
        '/content/articles', NOW(), NOW(), NULL);

-- 文章管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1710, 1700, '0,1700', '文章管理', 'M', 'ContentArticle', 'articles', 'content/article/index', NULL, 0, 1, 1, 1,
        'edit-pen', NULL, NOW(), NOW(), NULL),
       (1711, 1710, '0,1700,1710', '文章查询', 'B', NULL, NULL, NULL, 'content:article:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1712, 1710, '0,1700,1710', '文章新增', 'B', NULL, NULL, NULL, 'content:article:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1713, 1710, '0,1700,1710', '文章修改', 'B', NULL, NULL, NULL, 'content:article:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1714, 1710, '0,1700,1710', '文章删除', 'B', NULL, NULL, NULL, 'content:article:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL),
       (1715, 1710, '0,1700,1710', '文章状态', 'B', NULL, NULL, NULL, 'content:article:update', 0, 0, 1, 5, NULL, NULL,
        NOW(), NOW(), NULL),
       (1716, 1710, '0,1700,1710', '访问控制', 'B', NULL, NULL, NULL, 'content:article:access', 0, 0, 1, 6, NULL, NULL,
        NOW(), NOW(), NULL);

-- 分类管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1720, 1700, '0,1700', '分类管理', 'M', 'ContentCategory', 'categories', 'content/category/index', NULL, 0, 1, 1,
        2, 'folder-opened', NULL, NOW(), NOW(), NULL),
       (1721, 1720, '0,1700,1720', '分类查询', 'B', NULL, NULL, NULL, 'content:category:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1722, 1720, '0,1700,1720', '分类新增', 'B', NULL, NULL, NULL, 'content:category:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1723, 1720, '0,1700,1720', '分类修改', 'B', NULL, NULL, NULL, 'content:category:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1724, 1720, '0,1700,1720', '分类删除', 'B', NULL, NULL, NULL, 'content:category:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 标签管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1730, 1700, '0,1700', '标签管理', 'M', 'ContentTag', 'tags', 'content/tag/index', NULL, 0, 1, 1, 3, 'price-tag',
        NULL, NOW(), NOW(), NULL),
       (1731, 1730, '0,1700,1730', '标签查询', 'B', NULL, NULL, NULL, 'content:tag:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1732, 1730, '0,1700,1730', '标签新增', 'B', NULL, NULL, NULL, 'content:tag:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1733, 1730, '0,1700,1730', '标签修改', 'B', NULL, NULL, NULL, 'content:tag:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1734, 1730, '0,1700,1730', '标签删除', 'B', NULL, NULL, NULL, 'content:tag:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 评论管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1740, 1700, '0,1700', '评论管理', 'M', 'ContentComment', 'comments', 'content/comment/index', NULL, 0, 1, 1, 4,
        'chat-dot-round', NULL, NOW(), NOW(), NULL),
       (1741, 1740, '0,1700,1740', '评论查询', 'B', NULL, NULL, NULL, 'content:comment:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1742, 1740, '0,1700,1740', '评论修改', 'B', NULL, NULL, NULL, 'content:comment:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1743, 1740, '0,1700,1740', '评论删除', 'B', NULL, NULL, NULL, 'content:comment:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 收藏管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1750, 1700, '0,1700', '收藏管理', 'M', 'ContentCollection', 'collections', 'content/collection/index', NULL, 0,
        1, 1, 5, 'star', NULL, NOW(), NOW(), NULL),
       (1751, 1750, '0,1700,1750', '收藏查询', 'B', NULL, NULL, NULL, 'content:collection:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1752, 1750, '0,1700,1750', '收藏删除', 'B', NULL, NULL, NULL, 'content:collection:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 互动管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1760, 1700, '0,1700', '互动管理', 'M', 'ContentInteraction', 'interactions', 'content/interaction/index', NULL,
        0, 1, 1, 6, 'pointer', NULL, NOW(), NOW(), NULL),
       (1761, 1760, '0,1700,1760', '互动查询', 'B', NULL, NULL, NULL, 'content:interaction:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1762, 1760, '0,1700,1760', '互动删除', 'B', NULL, NULL, NULL, 'content:interaction:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 足迹管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1770, 1700, '0,1700', '足迹管理', 'M', 'ContentFootprint', 'footprints', 'content/footprint/index', NULL, 0, 1,
        1, 7, 'clock', NULL, NOW(), NOW(), NULL),
       (1771, 1770, '0,1700,1770', '足迹查询', 'B', NULL, NULL, NULL, 'content:footprint:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1772, 1770, '0,1700,1770', '足迹删除', 'B', NULL, NULL, NULL, 'content:footprint:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 文件管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1780, 1700, '0,1700', '文件管理', 'M', 'ContentFile', 'files', 'content/file/index', NULL, 0, 1, 1, 8, 'folder',
        NULL, NOW(), NOW(), NULL),
       (1781, 1780, '0,1700,1780', '文件查询', 'B', NULL, NULL, NULL, 'content:file:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1782, 1780, '0,1700,1780', '文件修改', 'B', NULL, NULL, NULL, 'content:file:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1783, 1780, '0,1700,1780', '文件删除', 'B', NULL, NULL, NULL, 'content:file:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 聊天管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1790, 1700, '0,1700', '聊天管理', 'M', 'ContentChat', 'chats', 'content/chat/index', NULL, 0, 1, 1, 9,
        'chat-dot-round', NULL, NOW(), NOW(), NULL),
       (1791, 1790, '0,1700,1790', '会话查询', 'B', NULL, NULL, NULL, 'content:chat:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1792, 1790, '0,1700,1790', '会话状态', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL);

-- 关注管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1793, 1700, '0,1700', '关注管理', 'M', 'ContentFollow', 'follows', 'content/follow/index', NULL, 0, 1, 1, 10,
        'user-filled', NULL, NOW(), NOW(), NULL),
       (1794, 1793, '0,1700,1793', '关注查询', 'B', NULL, NULL, NULL, 'content:follow:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1795, 1793, '0,1700,1793', '关注清理', 'B', NULL, NULL, NULL, 'content:follow:clean', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL);

-- 超级管理员授权全部系统管理权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1000),
       (1, 1100),
       (1, 1101),
       (1, 1102),
       (1, 1103),
       (1, 1104),
       (1, 1105),
       (1, 1106),
       (1, 1200),
       (1, 1201),
       (1, 1202),
       (1, 1203),
       (1, 1204),
       (1, 1205),
       (1, 1300),
       (1, 1301),
       (1, 1302),
       (1, 1303),
       (1, 1304),
       (1, 1400),
       (1, 1401),
       (1, 1402),
       (1, 1403),
       (1, 1404),
       (1, 1500),
       (1, 1501),
       (1, 1502),
       (1, 1503),
       (1, 1504),
       (1, 1505),
       (1, 1506),
       (1, 1600),
       (1, 1601),
       (1, 1602),
       (1, 1603),
       (1, 1700),
       (1, 1710),
       (1, 1711),
       (1, 1712),
       (1, 1713),
       (1, 1714),
       (1, 1715),
       (1, 1716),
       (1, 1720),
       (1, 1721),
       (1, 1722),
       (1, 1723),
       (1, 1724),
       (1, 1730),
       (1, 1731),
       (1, 1732),
       (1, 1733),
       (1, 1734),
       (1, 1740),
       (1, 1741),
       (1, 1742),
       (1, 1743),
       (1, 1750),
       (1, 1751),
       (1, 1752),
       (1, 1760),
       (1, 1761),
       (1, 1762),
       (1, 1770),
       (1, 1771),
       (1, 1772),
       (1, 1780),
       (1, 1781),
       (1, 1782),
       (1, 1783),
       (1, 1790),
       (1, 1791),
       (1, 1792),
       (1, 1793),
       (1, 1794),
       (1, 1795);

-- 初始化用户角色关系
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1),
       (2, 2);

-- 初始化指定用户通知关系
INSERT INTO `sys_user_notice`
(`id`, `notice_id`, `user_id`, `is_read`, `read_time`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 2, 2, 0, NULL, NOW(), NOW(), 0);

-- 初始化文章分类
INSERT INTO `sys_category`
(`id`, `parent_id`, `name`, `code`, `type`, `ancestors`, `level`, `sort_order`, `icon`, `description`, `status`,
 `created_at`, `updated_at`)
VALUES (1, 0, '技术分享', 'article-tech', 'article', '0', 1, 1, 'cpu', '技术类文章根分类', 1, NOW(), NOW()),
       (2, 0, '随笔记录', 'article-notes', 'article', '0', 1, 2, 'edit', '随笔与总结根分类', 1, NOW(), NOW()),
       (3, 1, 'Java 后端', 'article-java-backend', 'article', '0,1', 2, 1, 'java',
        'Spring Boot、MyBatis Plus 等后端内容', 1, NOW(), NOW()),
       (4, 1, '前端工程', 'article-frontend', 'article', '0,1', 2, 2, 'monitor', '前端工程化与交互体验', 1, NOW(),
        NOW()),
       (5, 2, '项目总结', 'article-project-summary', 'article', '0,2', 2, 1, 'document', '项目复盘与方案记录', 1, NOW(),
        NOW());

-- 初始化文章标签
INSERT INTO `sys_tag`
    (`id`, `name`, `color`, `created_at`)
VALUES (1, 'Spring Boot', '#409EFF', NOW()),
       (2, 'JWT', '#67C23A', NOW()),
       (3, 'MyBatis-Plus', '#E6A23C', NOW()),
       (4, '前端工程化', '#F56C6C', NOW()),
       (5, '项目实践', '#909399', NOW());

-- 初始化示例文章
INSERT INTO `blog_article`
(`id`, `title`, `summary`, `content`, `cover_image`, `author_id`, `is_top`, `is_original`, `source_url`, `status`,
 `publish_time`, `access_level`, `view_count`, `like_count`, `comment_count`, `collect_count`, `share_count`,
 `created_at`, `updated_at`, `remark`)
VALUES (1, 'Spring Boot 4 + JWT 认证实践', '使用当前项目的认证模块快速搭建账号登录、注册与令牌刷新能力。',
        '这是系统初始化的示例文章，用于验证文章、分类、标签、评论和互动等基础功能是否可用。', NULL, 1, 1, 1, NULL, 1,
        DATE_SUB(NOW(), INTERVAL 7 DAY), 0, 128, 1, 2, 1, 6, DATE_SUB(NOW(), INTERVAL 10 DAY),
        DATE_SUB(NOW(), INTERVAL 1 DAY), '系统初始化示例文章'),
       (2, '博客后台初始化说明', '记录数据库脚本、权限菜单以及演示账号的初始化约定。',
        '该文章用于说明初始化脚本执行顺序，以及管理员账号、演示账号和普通用户角色之间的关系。', NULL, 1, 0, 1, NULL, 1,
        DATE_SUB(NOW(), INTERVAL 3 DAY), 1, 52, 0, 0, 0, 2, DATE_SUB(NOW(), INTERVAL 5 DAY),
        DATE_SUB(NOW(), INTERVAL 2 DAY), '登录后可见的文章示例'),
       (3, '仅对白名单开放的专栏样例', '演示 access_level=4 时的白名单授权数据结构。',
        '当文章访问级别为指定用户可见时，可通过 blog_article_access 为白名单用户授权。', NULL, 2, 0, 1, NULL, 1,
        DATE_SUB(NOW(), INTERVAL 1 DAY), 4, 18, 0, 0, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), '白名单文章示例');

-- 初始化文章分类关联
INSERT INTO `blog_article_category`
    (`id`, `article_id`, `category_id`, `sort_order`, `created_at`)
VALUES (1, 1, 3, 1, NOW()),
       (2, 2, 5, 1, NOW()),
       (3, 3, 3, 1, NOW());

-- 初始化文章标签关联
INSERT INTO `sys_tag_relation`
    (`id`, `tag_id`, `target_id`, `target_type`, `created_at`)
VALUES (1, 1, 1, 'article', NOW()),
       (2, 2, 1, 'article', NOW()),
       (3, 3, 1, 'article', NOW()),
       (4, 5, 2, 'article', NOW()),
       (5, 1, 3, 'article', NOW());

-- 初始化白名单访问权限
INSERT INTO `blog_article_access`
(`id`, `article_id`, `user_id`, `access_type`, `grant_time`, `expire_time`, `grant_reason`, `created_at`, `updated_at`)
VALUES (1, 3, 1, 1, NOW(), NULL, '允许管理员预览专栏内容', NOW(), NOW()),
       (2, 3, 2, 1, NOW(), NULL, '允许作者本人查看白名单文章', NOW(), NOW());

-- 初始化收藏夹与收藏记录
INSERT INTO `sys_collection_folder`
(`id`, `user_id`, `folder_name`, `folder_type`, `description`, `is_public`, `is_default`, `sort_order`,
 `collection_count`, `created_at`, `updated_at`)
VALUES (1, 2, '默认收藏夹', 'article', '演示账号默认文章收藏夹', 0, 1, 1, 1, NOW(), NOW());

INSERT INTO `sys_collection`
(`id`, `user_id`, `folder_id`, `target_id`, `target_type`, `remark`, `target_title`, `target_url`, `created_at`)
VALUES (1, 2, 1, 1, 'article', '初始化收藏记录', 'Spring Boot 4 + JWT 认证实践', '/article/1', NOW());

-- 初始化评论数据
INSERT INTO `sys_comment`
(`id`, `target_id`, `target_type`, `content`, `images`, `user_id`, `root_id`, `parent_id`, `like_count`, `reply_count`,
 `status`, `created_at`, `updated_at`)
VALUES (1, 1, 'article', '这篇文章把认证流程串得比较完整，适合作为初始化后的联调样例。', NULL, 2, 0, 0, 1, 1, 1,
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (2, 1, 'article', '已补充注册接口，后续可以继续接默认角色分配。', NULL, 1, 1, 1, 0, 0, 1,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 初始化互动数据
INSERT INTO `sys_interaction`
(`id`, `user_id`, `target_id`, `target_type`, `action_type`, `created_at`)
VALUES (1, 2, 1, 'article', 'like', NOW()),
       (2, 1, 1, 'comment', 'like', NOW());

-- 初始化浏览足迹
INSERT INTO `sys_user_footprint`
(`id`, `user_id`, `target_id`, `target_type`, `title`, `url`, `ip_address`, `user_agent`, `visited_at`)
VALUES (1, 2, 1, 'article', 'Spring Boot 4 + JWT 认证实践', '/article/1', '127.0.0.1', 'Mozilla/5.0 Demo Browser',
        DATE_SUB(NOW(), INTERVAL 6 HOUR)),
       (2, 2, 3, 'article', '仅对白名单开放的专栏样例', '/article/3', '127.0.0.1', 'Mozilla/5.0 Demo Browser',
        DATE_SUB(NOW(), INTERVAL 2 HOUR));

COMMIT;



USE blog_backend;

-- 系统管理菜单初始化脚本 (ID 范围: 1000-1600)
-- 前置依赖：无（菜单数据独立）

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1000 AND 1699;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1000 AND 1699;

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
        NOW(), NOW(), NULL),
	       (1150, 1100, '0,1000,1100', '封禁用户', 'B', NULL, NULL, NULL, 'sys:user:ban', 0, 0, 1, 7, NULL, NULL, NOW(),
	        NOW(), NULL),
	       (1151, 1100, '0,1000,1100', '解封用户', 'B', NULL, NULL, NULL, 'sys:user:unban', 0, 0, 1, 8, NULL, NULL, NOW(),
	        NOW(), NULL),
	       (1152, 1100, '0,1000,1100', '等级调整', 'B', NULL, NULL, NULL, 'sys:user:adjust-level', 0, 0, 1, 9, NULL, NULL,
	        NOW(), NOW(), NULL),
	       (1153, 1100, '0,1000,1100', '经验调整', 'B', NULL, NULL, NULL, 'sys:user:adjust-experience', 0, 0, 1, 10, NULL,
	        NULL, NOW(), NOW(), NULL),
	       (1154, 1100, '0,1000,1100', '账号接管', 'B', NULL, NULL, NULL, 'sys:user:takeover', 0, 0, 1, 11, NULL, NULL, NOW(),
	        NOW(), NULL);

-- 作者申请管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1110, 1000, '0,1000', '作者申请', 'M', 'SysAuthorApplication', 'author-applications',
        'system/author-application/index', NULL, 0, 1, 1, 2, 'document-checked', NULL, NOW(), NOW(), NULL),
       (1111, 1110, '0,1000,1110', '作者申请查询', 'B', NULL, NULL, NULL, 'sys:author-application:query', 0, 0, 1, 1,
        NULL, NULL, NOW(), NOW(), NULL),
       (1112, 1110, '0,1000,1110', '作者申请审核', 'B', NULL, NULL, NULL, 'sys:author-application:review', 0, 0, 1, 2,
        NULL, NULL, NOW(), NOW(), NULL),
       (1113, 1110, '0,1000,1110', '作者状态修正', 'B', NULL, NULL, NULL, 'sys:author-application:repair', 0, 0, 1, 3,
        NULL, NULL, NOW(), NOW(), NULL);

-- 经验体系管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1140, 1000, '0,1000', '经验管理', 'M', 'SysExperience', 'experience', 'system/experience/index', NULL, 0, 1, 1, 5, 'star',
        NULL, NOW(), NOW(), NULL),
       (1141, 1140, '0,1000,1140', '经验查询', 'B', NULL, NULL, NULL, 'sys:experience:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1142, 1140, '0,1000,1140', '等级调整', 'B', NULL, NULL, NULL, 'sys:experience:adjust', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1143, 1140, '0,1000,1140', '经验配置', 'B', NULL, NULL, NULL, 'sys:experience:config', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL);

-- 角色管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1200, 1000, '0,1000', '角色管理', 'M', 'SysRole', 'roles', 'system/role/index', NULL, 0, 1, 1, 3, 'peoples',
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

COMMIT;

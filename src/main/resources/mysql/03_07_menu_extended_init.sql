USE blog_backend;

-- 扩展功能菜单初始化脚本 (ID 范围: 1800-1929)
-- 包含：系列文章、频道申请、论坛管理、AI 管理、举报管理、高风险审计
-- 前置依赖：无（菜单数据独立）

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1800 AND 1929;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1800 AND 1929;

-- 系列文章管理（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1800, 1700, '0,1700', '系列文章', 'M', 'ContentSeries', '/admin/series', 'admin/series/SeriesManagement', NULL, 0, 1, 1, 11,
        'list', NULL, NOW(), NOW(), NULL),
       (1801, 1800, '0,1700,1800', '系列查询', 'B', NULL, NULL, NULL, 'content:series:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1802, 1800, '0,1700,1800', '系列新增', 'B', NULL, NULL, NULL, 'content:series:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1803, 1800, '0,1700,1800', '系列修改', 'B', NULL, NULL, NULL, 'content:series:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1804, 1800, '0,1700,1800', '系列删除', 'B', NULL, NULL, NULL, 'content:series:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL);

-- 频道申请审核（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1810, 1700, '0,1700', '频道申请', 'M', 'ContentChannelApplication', '/admin/channel-applications',
        'admin/channel/ChannelAudit', NULL, 0, 1, 1, 12, 'phone', NULL, NOW(), NOW(), NULL),
       (1811, 1810, '0,1700,1810', '频道申请查询', 'B', NULL, NULL, NULL, 'content:channel-application:query', 0, 0, 1, 1,
        NULL, NULL, NOW(), NOW(), NULL),
       (1812, 1810, '0,1700,1810', '频道申请审核', 'B', NULL, NULL, NULL, 'content:channel-application:review', 0, 0, 1, 2,
        NULL, NULL, NOW(), NOW(), NULL);

-- 入群申请管理（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1820, 1700, '0,1700', '入群申请', 'M', 'ContentGroupJoin', '/admin/group-join-applications',
        'admin/chat/GroupJoinApplications', NULL, 0, 1, 1, 13, 'user-filled', NULL, NOW(), NOW(), NULL),
       (1821, 1820, '0,1700,1820', '入群申请查询', 'B', NULL, NULL, NULL, 'content:group-join:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1822, 1820, '0,1700,1820', '入群申请审核', 'B', NULL, NULL, NULL, 'content:group-join:review', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 论坛管理（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1830, 1700, '0,1700', '论坛管理', 'M', 'ContentForum', '/admin/forum/sections', 'admin/forum/ForumSections', NULL,
        0, 1, 1, 14, 'chat-line-round', NULL, NOW(), NOW(), NULL),
       (1831, 1830, '0,1700,1830', '论坛查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1832, 1830, '0,1700,1830', '论坛新增', 'B', NULL, NULL, NULL, 'content:forum:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1833, 1830, '0,1700,1830', '论坛修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1834, 1830, '0,1700,1830', '论坛删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- AI 管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1850, 0, '0', 'AI 管理', 'C', 'Ai', '/admin/ai', 'layouts/RouteView', NULL, 1, 0, 1, 3, 'robot',
        '/admin/ai/channel-config', NOW(), NOW(), NULL);

-- AI 渠道配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1860, 1850, '0,1850', '渠道配置', 'M', 'AiChannelConfig', '/admin/ai/channel-config', 'admin/ai/AiConfigCenter', NULL, 0, 1,
        1, 1, 'setting', NULL, NOW(), NOW(), NULL),
       (1861, 1860, '0,1850,1860', '渠道查询', 'B', NULL, NULL, NULL, 'ai:channel-config:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1862, 1860, '0,1850,1860', '渠道新增', 'B', NULL, NULL, NULL, 'ai:channel-config:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1863, 1860, '0,1850,1860', '渠道修改', 'B', NULL, NULL, NULL, 'ai:channel-config:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1864, 1860, '0,1850,1860', '渠道删除', 'B', NULL, NULL, NULL, 'ai:channel-config:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- AI 调用统计
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1870, 1850, '0,1850', '调用统计', 'M', 'AiUsageStats', '/admin/ai/usage-stats', 'admin/ai/AiUsageStats', NULL, 0, 1, 1, 2,
        'data-analysis', NULL, NOW(), NOW(), NULL),
       (1871, 1870, '0,1850,1870', '统计查询', 'B', NULL, NULL, NULL, 'ai:usage-stats:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL);

-- AI 会话管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1880, 1850, '0,1850', '会话管理', 'M', 'AiSessionManage', '/admin/ai/sessions', 'admin/ai/AiSessionManage', NULL, 0, 1, 1, 3,
        'message', NULL, NOW(), NOW(), NULL),
       (1881, 1880, '0,1850,1880', '会话查询', 'B', NULL, NULL, NULL, 'ai:session:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL);

-- 举报管理（系统管理 1000 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1900, 1000, '0,1000', '举报管理', 'M', 'SysReport', '/admin/reports', 'admin/report/ReportList', NULL, 0, 1, 1, 7,
        'warning', NULL, NOW(), NOW(), NULL),
       (1901, 1900, '0,1000,1900', '举报查询', 'B', NULL, NULL, NULL, 'sys:report:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1902, 1900, '0,1000,1900', '举报处理', 'B', NULL, NULL, NULL, 'sys:report:handle', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL);

-- 高风险审计查询（系统管理 1000 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1920, 1000, '0,1000', '高风险审计', 'M', 'SysAudit', '/admin/audit', 'admin/audit/AuditLog', NULL, 0, 1, 1, 9, 'lock',
        NULL, NOW(), NOW(), NULL),
       (1921, 1920, '0,1000,1920', '审计查询', 'B', NULL, NULL, NULL, 'sys:audit:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL);

COMMIT;

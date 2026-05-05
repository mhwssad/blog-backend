USE blog_backend;

-- 扩展功能菜单初始化脚本 (ID 范围: 1800-1954)
-- 包含：系列文章、频道申请、论坛管理、博客迁移、AI 管理、举报管理、高风险审计
-- 前置依赖：无（菜单数据独立）

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1800 AND 1954;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1800 AND 1954;

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

-- AI 工具管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1890, 1850, '0,1850', '工具管理', 'M', 'AiToolManage', '/admin/ai/tools', 'admin/ai/AiTools', NULL, 0, 1, 1, 4,
        'operation', NULL, NOW(), NOW(), NULL),
       (1891, 1890, '0,1850,1890', '工具查询', 'B', NULL, NULL, NULL, 'ai:tool:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1892, 1890, '0,1850,1890', '工具新增', 'B', NULL, NULL, NULL, 'ai:tool:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1893, 1890, '0,1850,1890', '工具修改', 'B', NULL, NULL, NULL, 'ai:tool:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1894, 1890, '0,1850,1890', '工具删除', 'B', NULL, NULL, NULL, 'ai:tool:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL),
       (1895, 1890, '0,1850,1890', '工具执行', 'B', NULL, NULL, NULL, 'ai:tool:execute', 0, 0, 1, 5, NULL, NULL, NOW(),
        NOW(), NULL);

-- AI MCP 服务
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1910, 1850, '0,1850', 'MCP 服务', 'M', 'AiMcpServers', '/admin/ai/mcp-servers', 'admin/ai/AiMcpServers', NULL, 0, 1,
        1, 5, 'connection', NULL, NOW(), NOW(), NULL),
       (1911, 1910, '0,1850,1910', 'MCP查询', 'B', NULL, NULL, NULL, 'ai:mcp:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1912, 1910, '0,1850,1910', 'MCP新增', 'B', NULL, NULL, NULL, 'ai:mcp:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1913, 1910, '0,1850,1910', 'MCP修改', 'B', NULL, NULL, NULL, 'ai:mcp:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1914, 1910, '0,1850,1910', 'MCP删除', 'B', NULL, NULL, NULL, 'ai:mcp:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL),
       (1915, 1910, '0,1850,1910', 'MCP发现', 'B', NULL, NULL, NULL, 'ai:mcp:discover', 0, 0, 1, 5, NULL, NULL, NOW(),
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

-- 论坛帖子管理（内容管理 1700 -> 论坛管理 1830 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1930, 1830, '0,1700,1830', '帖子管理', 'M', 'ContentForumPosts', '/admin/forum/posts', 'admin/forum/ForumPosts', NULL,
        0, 1, 1, 2, 'documentation', NULL, NOW(), NOW(), NULL),
       (1931, 1930, '0,1700,1830,1930', '帖子查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1932, 1930, '0,1700,1830,1930', '帖子修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1933, 1930, '0,1700,1830,1930', '帖子删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 论坛回复管理（内容管理 1700 -> 论坛管理 1830 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1940, 1830, '0,1700,1830', '回复管理', 'M', 'ContentForumReplies', '/admin/forum/replies', 'admin/forum/ForumReplies', NULL,
        0, 1, 1, 3, 'edit', NULL, NOW(), NOW(), NULL),
       (1941, 1940, '0,1700,1830,1940', '回复查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1942, 1940, '0,1700,1830,1940', '回复修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1943, 1940, '0,1700,1830,1940', '回复删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 博客迁移（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1950, 1700, '0,1700', '博客迁移', 'M', 'ContentBlogMigration', '/admin/migrations/blog',
        'admin/migration/BlogMigration', NULL, 0, 1, 1, 15, 'upload-filled', NULL, NOW(), NOW(), NULL),
       (1951, 1950, '0,1700,1950', '迁移查询', 'B', NULL, NULL, NULL, 'content:migration:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1952, 1950, '0,1700,1950', '迁移创建', 'B', NULL, NULL, NULL, 'content:migration:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1953, 1950, '0,1700,1950', '迁移执行', 'B', NULL, NULL, NULL, 'content:migration:execute', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1954, 1950, '0,1700,1950', '迁移导出', 'B', NULL, NULL, NULL, 'content:migration:export', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 高风险审计查询（系统管理 1000 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1920, 1000, '0,1000', '高风险审计', 'M', 'SysAudit', '/admin/audit', 'admin/audit/AuditLog', NULL, 0, 1, 1, 9, 'lock',
        NULL, NOW(), NOW(), NULL),
       (1921, 1920, '0,1000,1920', '审计查询', 'B', NULL, NULL, NULL, 'sys:audit:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL);

-- 友情链接管理（内容管理 1700 下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1960, 1700, '0,1700', '友情链接', 'M', 'ContentFriendLink', '/admin/friend-links', 'admin/content/FriendLinks', NULL, 0, 1, 1, 16,
        'link', NULL, NOW(), NOW(), NULL),
       (1961, 1960, '0,1700,1960', '友情链接查询', 'B', NULL, NULL, NULL, 'content:friend-link:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1962, 1960, '0,1700,1960', '友情链接新增', 'B', NULL, NULL, NULL, 'content:friend-link:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1963, 1960, '0,1700,1960', '友情链接修改', 'B', NULL, NULL, NULL, 'content:friend-link:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1964, 1960, '0,1700,1960', '友情链接删除', 'B', NULL, NULL, NULL, 'content:friend-link:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

COMMIT;

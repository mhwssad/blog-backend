USE blog_backend;

-- 社区、AI 与治理菜单初始化脚本 (ID 范围: 1800-1999)
-- 包含：社区管理、AI 管理、治理管理
-- 前置依赖：无（菜单数据独立）

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1800 AND 1999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1800 AND 1999;

-- 社区管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1800, 0, '0', '社区管理', 'C', 'Community', '/admin/community', 'layouts/RouteView', NULL, 1, 0, 1, 4, 'chat-dot-round',
        '/admin/chats', NOW(), NOW(), NULL);

-- 会话治理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1810, 1800, '0,1800', '会话治理', 'M', 'CommunityChat', '/admin/chats', 'admin/chat/Chats', NULL, 0, 1, 1, 1,
        'chat-dot-round', NULL, NOW(), NOW(), NULL),
       (1811, 1810, '0,1800,1810', '会话查询', 'B', NULL, NULL, NULL, 'content:chat:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1812, 1810, '0,1800,1810', '会话状态', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1813, 1810, '0,1800,1810', '撤回消息', 'B', NULL, NULL, NULL, 'content:chat:revoke', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 大厅管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1820, 1800, '0,1800', '大厅管理', 'M', 'CommunityLobby', '/admin/chat/lobby', 'admin/chat/LobbyManagement', NULL, 0, 1,
        1, 2, 'guide', NULL, NOW(), NOW(), NULL),
       (1821, 1820, '0,1800,1820', '大厅设置', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1822, 1820, '0,1800,1820', '成员治理', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL);

-- 频道管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1830, 1800, '0,1800', '频道管理', 'M', 'CommunityChannel', '/admin/channels', 'admin/channel/ChannelManagement', NULL, 0,
        1, 1, 3, 'menu', NULL, NOW(), NOW(), NULL),
       (1831, 1830, '0,1800,1830', '频道查询', 'B', NULL, NULL, NULL, 'content:chat:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1832, 1830, '0,1800,1830', '频道维护', 'B', NULL, NULL, NULL, 'content:chat:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL);

-- 频道申请审核
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1840, 1800, '0,1800', '频道申请', 'M', 'CommunityChannelApplication', '/admin/channel-applications',
        'admin/channel/ChannelAudit', NULL, 0, 1, 1, 4, 'phone', NULL, NOW(), NOW(), NULL),
       (1841, 1840, '0,1800,1840', '频道申请查询', 'B', NULL, NULL, NULL, 'content:channel-application:query', 0, 0, 1, 1,
        NULL, NULL, NOW(), NOW(), NULL),
       (1842, 1840, '0,1800,1840', '频道申请审核', 'B', NULL, NULL, NULL, 'content:channel-application:review', 0, 0, 1, 2,
        NULL, NULL, NOW(), NOW(), NULL);

-- 入群申请管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1845, 1800, '0,1800', '入群申请', 'M', 'CommunityGroupJoin', '/admin/group-join-applications',
        'admin/chat/GroupJoinApplications', NULL, 0, 1, 1, 5, 'user-filled', NULL, NOW(), NOW(), NULL),
       (1846, 1845, '0,1800,1845', '入群申请查询', 'B', NULL, NULL, NULL, 'content:group-join:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1847, 1845, '0,1800,1845', '入群申请审核', 'B', NULL, NULL, NULL, 'content:group-join:review', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- AI 管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1850, 0, '0', 'AI 管理', 'C', 'Ai', '/admin/ai', 'layouts/RouteView', NULL, 1, 0, 1, 5, 'robot',
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

-- 论坛管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1885, 1800, '0,1800', '论坛管理', 'C', 'CommunityForum', '/admin/forum', 'layouts/RouteView', NULL, 1, 0, 1, 6,
        'reading', '/admin/forum/sections', NOW(), NOW(), NULL);

-- 论坛版块管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1886, 1885, '0,1800,1885', '版块管理', 'M', 'CommunityForumSections', '/admin/forum/sections',
        'admin/forum/ForumSections', NULL, 0, 1, 1, 1, 'grid', NULL, NOW(), NOW(), NULL),
       (1887, 1886, '0,1800,1885,1886', '版块查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1888, 1886, '0,1800,1885,1886', '版块新增', 'B', NULL, NULL, NULL, 'content:forum:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1889, 1886, '0,1800,1885,1886', '版块修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1890, 1886, '0,1800,1885,1886', '版块删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 论坛帖子管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1891, 1885, '0,1800,1885', '帖子管理', 'M', 'CommunityForumPosts', '/admin/forum/posts',
        'admin/forum/ForumPosts', NULL, 0, 1, 1, 2, 'document', NULL, NOW(), NOW(), NULL),
       (1892, 1891, '0,1800,1885,1891', '帖子查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1893, 1891, '0,1800,1885,1891', '帖子修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1894, 1891, '0,1800,1885,1891', '帖子删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 论坛回复管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1895, 1885, '0,1800,1885', '回复管理', 'M', 'CommunityForumReplies', '/admin/forum/replies',
        'admin/forum/ForumReplies', NULL, 0, 1, 1, 3, 'chat-line-round', NULL, NOW(), NOW(), NULL),
       (1896, 1895, '0,1800,1885,1895', '回复查询', 'B', NULL, NULL, NULL, 'content:forum:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1897, 1895, '0,1800,1885,1895', '回复修改', 'B', NULL, NULL, NULL, 'content:forum:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1898, 1895, '0,1800,1885,1895', '回复删除', 'B', NULL, NULL, NULL, 'content:forum:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 治理管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1900, 0, '0', '治理管理', 'C', 'Governance', '/admin/governance', 'layouts/RouteView', NULL, 1, 0, 1, 8, 'warning',
        '/admin/reports', NOW(), NOW(), NULL);

-- 举报管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1910, 1900, '0,1900', '举报管理', 'M', 'SysReport', '/admin/reports', 'admin/report/ReportList', NULL, 0, 1, 1, 1,
        'warning', NULL, NOW(), NOW(), NULL),
       (1911, 1910, '0,1900,1910', '举报查询', 'B', NULL, NULL, NULL, 'sys:report:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1912, 1910, '0,1900,1910', '举报处理', 'B', NULL, NULL, NULL, 'sys:report:handle', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1913, 1910, '0,1900,1910', '举报修复', 'B', NULL, NULL, NULL, 'sys:report:repair', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL);

-- 作者申请管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1960, 1900, '0,1900', '作者申请', 'M', 'SysAuthorApplication', '/admin/author-applications',
        'admin/author/AuthorApplications', NULL, 0, 1, 1, 2, 'document-checked', NULL, NOW(), NOW(), NULL),
       (1961, 1960, '0,1900,1960', '作者申请查询', 'B', NULL, NULL, NULL, 'sys:author-application:query', 0, 0, 1, 1,
        NULL, NULL, NOW(), NOW(), NULL),
       (1962, 1960, '0,1900,1960', '作者申请审核', 'B', NULL, NULL, NULL, 'sys:author-application:review', 0, 0, 1, 2,
        NULL, NULL, NOW(), NOW(), NULL),
       (1963, 1960, '0,1900,1960', '作者状态修正', 'B', NULL, NULL, NULL, 'sys:author-application:repair', 0, 0, 1, 3,
        NULL, NULL, NOW(), NOW(), NULL);

COMMIT;

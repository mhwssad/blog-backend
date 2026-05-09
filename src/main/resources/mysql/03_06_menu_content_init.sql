USE blog_backend;

-- 内容、互动与文件菜单初始化脚本 (ID 范围: 1700-1799)
-- 包含：内容管理、互动管理、文件管理
-- 前置依赖：无（菜单数据独立）

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1700 AND 1799;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 1700 AND 1799;

-- 内容管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1700, 0, '0', '内容管理', 'C', 'Content', '/admin/content', 'layouts/RouteView', NULL, 1, 0, 1, 2, 'document',
        '/admin/articles', NOW(), NOW(), NULL);

-- 文章管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1710, 1700, '0,1700', '文章管理', 'M', 'ContentArticle', '/admin/articles', 'admin/article/Articles', NULL, 0, 1, 1, 1,
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

-- 文章审核管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1717, 1700, '0,1700', '文章审核', 'M', 'ContentArticleReview', '/admin/article-reviews',
        'admin/article/ArticleReview', NULL, 0, 1, 1, 2, 'document-checked', NULL, NOW(), NOW(), NULL),
       (1718, 1717, '0,1700,1717', '审核查询', 'B', NULL, NULL, NULL, 'content:article-review:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1719, 1717, '0,1700,1717', '审核处理', 'B', NULL, NULL, NULL, 'content:article-review:review', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1729, 1717, '0,1700,1717', '状态修正', 'B', NULL, NULL, NULL, 'content:article-review:repair', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 分类管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1720, 1700, '0,1700', '分类管理', 'M', 'ContentCategory', '/admin/categories', 'admin/category/Categories', NULL, 0, 1, 1,
        3, 'folder-opened', NULL, NOW(), NOW(), NULL),
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
VALUES (1730, 1700, '0,1700', '标签管理', 'M', 'ContentTag', '/admin/tags', 'admin/tag/Tags', NULL, 0, 1, 1, 4, 'price-tag',
        NULL, NOW(), NOW(), NULL),
       (1731, 1730, '0,1700,1730', '标签查询', 'B', NULL, NULL, NULL, 'content:tag:query', 0, 0, 1, 1, NULL, NULL, NOW(),
        NOW(), NULL),
       (1732, 1730, '0,1700,1730', '标签新增', 'B', NULL, NULL, NULL, 'content:tag:create', 0, 0, 1, 2, NULL, NULL, NOW(),
        NOW(), NULL),
       (1733, 1730, '0,1700,1730', '标签修改', 'B', NULL, NULL, NULL, 'content:tag:update', 0, 0, 1, 3, NULL, NULL, NOW(),
        NOW(), NULL),
       (1734, 1730, '0,1700,1730', '标签删除', 'B', NULL, NULL, NULL, 'content:tag:delete', 0, 0, 1, 4, NULL, NULL, NOW(),
        NOW(), NULL);

-- 评论管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1740, 1700, '0,1700', '评论管理', 'M', 'ContentComment', '/admin/comments', 'admin/comment/Comments', NULL, 0, 1, 1, 5,
        'chat-dot-round', NULL, NOW(), NOW(), NULL),
       (1741, 1740, '0,1700,1740', '评论查询', 'B', NULL, NULL, NULL, 'content:comment:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1742, 1740, '0,1700,1740', '评论修改', 'B', NULL, NULL, NULL, 'content:comment:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1743, 1740, '0,1700,1740', '评论删除', 'B', NULL, NULL, NULL, 'content:comment:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 迁移管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1790, 1700, '0,1700', '迁移管理', 'M', 'ContentMigration', '/admin/migrations', 'admin/migration/MigrationTasks', NULL, 0,
        1, 1, 6, 'upload', NULL, NOW(), NOW(), NULL),
       (1791, 1790, '0,1700,1790', '迁移查询', 'B', NULL, NULL, NULL, 'content:migration:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1792, 1790, '0,1700,1790', '迁移创建', 'B', NULL, NULL, NULL, 'content:migration:create', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1796, 1790, '0,1700,1790', '迁移执行', 'B', NULL, NULL, NULL, 'content:migration:execute', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL),
       (1797, 1790, '0,1700,1790', '失败导出', 'B', NULL, NULL, NULL, 'content:migration:export', 0, 0, 1, 4, NULL, NULL,
        NOW(), NOW(), NULL);

-- 互动管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1750, 0, '0', '互动管理', 'C', 'Interaction', '/admin/interaction', 'layouts/RouteView', NULL, 1, 0, 1, 3, 'connection',
        '/admin/collections', NOW(), NOW(), NULL);

-- 收藏管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1755, 1750, '0,1750', '收藏管理', 'M', 'ContentCollection', '/admin/collections', 'admin/collection/Collections', NULL, 0,
        1, 1, 1, 'star', NULL, NOW(), NOW(), NULL),
       (1756, 1755, '0,1750,1755', '收藏查询', 'B', NULL, NULL, NULL, 'content:collection:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1757, 1755, '0,1750,1755', '收藏删除', 'B', NULL, NULL, NULL, 'content:collection:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 互动记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1760, 1750, '0,1750', '互动记录', 'M', 'ContentInteraction', '/admin/interactions', 'admin/interaction/Interactions', NULL,
        0, 1, 1, 2, 'pointer', NULL, NOW(), NOW(), NULL),
       (1761, 1760, '0,1750,1760', '互动查询', 'B', NULL, NULL, NULL, 'content:interaction:query', 0, 0, 1, 1, NULL,
        NULL, NOW(), NOW(), NULL),
       (1762, 1760, '0,1750,1760', '互动删除', 'B', NULL, NULL, NULL, 'content:interaction:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 足迹管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1770, 1750, '0,1750', '足迹管理', 'M', 'ContentFootprint', '/admin/footprints', 'admin/footprint/Footprints', NULL, 0, 1,
        1, 3, 'clock', NULL, NOW(), NOW(), NULL),
       (1771, 1770, '0,1750,1770', '足迹查询', 'B', NULL, NULL, NULL, 'content:footprint:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1772, 1770, '0,1750,1770', '足迹删除', 'B', NULL, NULL, NULL, 'content:footprint:delete', 0, 0, 1, 2, NULL,
        NULL, NOW(), NOW(), NULL);

-- 文件管理（独立根菜单）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1780, 0, '0', '文件管理', 'M', 'ContentFile', '/admin/files', 'admin/file/Files', NULL, 0, 1, 1, 7, 'folder',
        NULL, NOW(), NOW(), NULL),
       (1781, 1780, '0,1780', '文件查询', 'B', NULL, NULL, NULL, 'content:file:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1782, 1780, '0,1780', '文件修改', 'B', NULL, NULL, NULL, 'content:file:update', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL),
       (1783, 1780, '0,1780', '文件删除', 'B', NULL, NULL, NULL, 'content:file:delete', 0, 0, 1, 3, NULL, NULL,
        NOW(), NOW(), NULL);

-- 关注管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`,
                        `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`,
                        `update_time`, `params`)
VALUES (1793, 1750, '0,1750', '关注管理', 'M', 'ContentFollow', '/admin/follows', 'admin/follow/Follows', NULL, 0, 1, 1, 4,
        'user-filled', NULL, NOW(), NOW(), NULL),
       (1794, 1793, '0,1750,1793', '关注查询', 'B', NULL, NULL, NULL, 'content:follow:query', 0, 0, 1, 1, NULL, NULL,
        NOW(), NOW(), NULL),
       (1795, 1793, '0,1750,1793', '关注清理', 'B', NULL, NULL, NULL, 'content:follow:clean', 0, 0, 1, 2, NULL, NULL,
        NOW(), NOW(), NULL);

COMMIT;

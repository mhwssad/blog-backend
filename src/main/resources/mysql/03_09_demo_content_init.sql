USE blog_backend;

-- 演示内容数据初始化脚本
-- 包含：分类、标签、示例文章、文章关联、白名单访问、收藏夹/收藏、评论、互动、浏览足迹
-- 前置依赖：03_02_user_init.sql

START TRANSACTION;

-- 清理演示内容数据
DELETE FROM `sys_user_footprint` WHERE `id` IN (1, 2);
DELETE FROM `sys_interaction` WHERE `id` IN (1, 2);
DELETE FROM `sys_comment` WHERE `id` IN (1, 2);
DELETE FROM `sys_collection` WHERE `id` IN (1);
DELETE FROM `sys_collection_folder` WHERE `id` IN (1);
DELETE FROM `blog_article_access` WHERE `id` IN (1, 2);
DELETE FROM `blog_article_category` WHERE `id` IN (1, 2, 3);
DELETE FROM `sys_tag_relation` WHERE `id` IN (1, 2, 3, 4, 5);
DELETE FROM `blog_article` WHERE `id` IN (1, 2, 3);
DELETE FROM `sys_tag` WHERE `id` IN (1, 2, 3, 4, 5);
DELETE FROM `sys_category` WHERE `id` IN (1, 2, 3, 4, 5);

-- 文章分类
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

-- 文章标签
INSERT INTO `sys_tag`
    (`id`, `name`, `color`, `created_at`)
VALUES (1, 'Spring Boot', '#409EFF', NOW()),
       (2, 'JWT', '#67C23A', NOW()),
       (3, 'MyBatis-Plus', '#E6A23C', NOW()),
       (4, '前端工程化', '#F56C6C', NOW()),
       (5, '项目实践', '#909399', NOW());

-- 示例文章
INSERT INTO `blog_article`
(`id`, `title`, `summary`, `content`, `cover_image`, `author_id`, `is_top`, `is_original`, `source_url`, `status`,
 `review_status`, `publish_time`, `scheduled_publish_time`, `access_level`, `visibility_scope`,
 `view_count`, `like_count`, `comment_count`, `collect_count`, `share_count`,
 `created_at`, `updated_at`, `remark`)
VALUES (1, 'Spring Boot 4 + JWT 认证实践', '使用当前项目的认证模块快速搭建账号登录、注册与令牌刷新能力。',
        '这是系统初始化的示例文章，用于验证文章、分类、标签、评论和互动等基础功能是否可用。', NULL, 1, 1, 1, NULL, 1,
        2, DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, 0, 0,
        128, 1, 2, 1, 6,
        DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), '系统初始化示例文章'),
       (2, '博客后台初始化说明', '记录数据库脚本、权限菜单以及演示账号的初始化约定。',
        '该文章用于说明初始化脚本执行顺序，以及管理员账号、演示账号和普通用户角色之间的关系。', NULL, 1, 0, 1, NULL, 1,
        2, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 1, 0,
        52, 0, 0, 0, 2,
        DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '登录后可见的文章示例'),
       (3, '仅对白名单开放的专栏样例', '演示 visibility_scope=2 时的白名单授权数据结构。',
        '当文章可见范围为白名单时，可通过 blog_article_access 为白名单用户授权。', NULL, 2, 0, 1, NULL, 1,
        2, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 0, 2,
        18, 0, 0, 0, 0,
        DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), '白名单文章示例');

-- 文章分类关联
INSERT INTO `blog_article_category`
    (`id`, `article_id`, `category_id`, `sort_order`, `created_at`)
VALUES (1, 1, 3, 1, NOW()),
       (2, 2, 5, 1, NOW()),
       (3, 3, 3, 1, NOW());

-- 文章标签关联
INSERT INTO `sys_tag_relation`
    (`id`, `tag_id`, `target_id`, `target_type`, `created_at`)
VALUES (1, 1, 1, 'article', NOW()),
       (2, 2, 1, 'article', NOW()),
       (3, 3, 1, 'article', NOW()),
       (4, 5, 2, 'article', NOW()),
       (5, 1, 3, 'article', NOW());

-- 白名单访问权限
INSERT INTO `blog_article_access`
(`id`, `article_id`, `user_id`, `access_type`, `grant_time`, `expire_time`, `grant_reason`, `created_at`, `updated_at`)
VALUES (1, 3, 1, 1, NOW(), NULL, '允许管理员预览专栏内容', NOW(), NOW()),
       (2, 3, 2, 1, NOW(), NULL, '允许作者本人查看白名单文章', NOW(), NOW());

-- 收藏夹与收藏记录
INSERT INTO `sys_collection_folder`
(`id`, `user_id`, `folder_name`, `folder_type`, `description`, `is_public`, `is_default`, `sort_order`,
 `collection_count`, `created_at`, `updated_at`)
VALUES (1, 2, '默认收藏夹', 'article', '演示账号默认文章收藏夹', 0, 1, 1, 1, NOW(), NOW());

INSERT INTO `sys_collection`
(`id`, `user_id`, `folder_id`, `target_id`, `target_type`, `remark`, `target_title`, `target_url`, `created_at`)
VALUES (1, 2, 1, 1, 'article', '初始化收藏记录', 'Spring Boot 4 + JWT 认证实践', '/article/1', NOW());

-- 评论数据
INSERT INTO `sys_comment`
(`id`, `target_id`, `target_type`, `content`, `images`, `user_id`, `root_id`, `parent_id`, `like_count`, `reply_count`,
 `status`, `created_at`, `updated_at`)
VALUES (1, 1, 'article', '这篇文章把认证流程串得比较完整，适合作为初始化后的联调样例。', NULL, 2, 0, 0, 1, 1, 1,
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
       (2, 1, 'article', '已补充注册接口，后续可以继续接默认角色分配。', NULL, 1, 1, 1, 0, 0, 1,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 互动数据
INSERT INTO `sys_interaction`
(`id`, `user_id`, `target_id`, `target_type`, `action_type`, `created_at`)
VALUES (1, 2, 1, 'article', 'like', NOW()),
       (2, 1, 1, 'comment', 'like', NOW());

-- 浏览足迹
INSERT INTO `sys_user_footprint`
(`id`, `user_id`, `target_id`, `target_type`, `title`, `url`, `ip_address`, `user_agent`, `visited_at`)
VALUES (1, 2, 1, 'article', 'Spring Boot 4 + JWT 认证实践', '/article/1', '127.0.0.1', 'Mozilla/5.0 Demo Browser',
        DATE_SUB(NOW(), INTERVAL 6 HOUR)),
       (2, 2, 3, 'article', '仅对白名单开放的专栏样例', '/article/3', '127.0.0.1', 'Mozilla/5.0 Demo Browser',
        DATE_SUB(NOW(), INTERVAL 2 HOUR));

COMMIT;

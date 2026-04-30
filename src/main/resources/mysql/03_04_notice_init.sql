USE blog_backend;

-- 通知与用户通知初始化脚本
-- 前置依赖：03_01_role_init.sql、03_02_user_init.sql

-- 兼容旧版本表结构：通知等级可能仍为 varchar(5)，无法容纳 success/warning
ALTER TABLE `sys_notice`
    MODIFY COLUMN `level` varchar(16) NOT NULL COMMENT '通知等级（字典code：notice_level，如 info/success/warning/danger）';

START TRANSACTION;

DELETE FROM `sys_user_notice` WHERE `id` IN (1) OR `notice_id` IN (1, 2);
DELETE FROM `sys_notice` WHERE `id` IN (1, 2);

INSERT INTO `sys_notice`
(`id`, `title`, `content`, `type`, `level`, `target_type`, `target_user_ids`, `publisher_id`, `publish_status`,
 `publish_time`, `revoke_time`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES (1, '欢迎使用博客后台', '系统已完成初始化，您现在可以使用 admin 账号登录并开始配置角色、菜单和通知。', 1, 'info', 1,
        NULL, 1, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
       (2, '欢迎体验演示账号', '演示账号已预置示例文章、分类、标签和通知数据，可直接体验基础功能。', 1, 'success', 2, '2',
        1, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0);

-- 指定用户通知关系
INSERT INTO `sys_user_notice`
(`id`, `notice_id`, `user_id`, `is_read`, `read_time`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 2, 2, 0, NULL, NOW(), NOW(), 0);

COMMIT;

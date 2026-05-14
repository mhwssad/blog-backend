USE blog_backend;

-- 用户与用户角色初始化脚本
-- 前置依赖：03_01_role_init.sql

START TRANSACTION;

-- 清理
DELETE FROM `sys_user_role` WHERE `user_id` IN (1, 2) OR `role_id` IN (1, 2, 3);
DELETE FROM `sys_user` WHERE `id` IN (1, 2);

-- 默认密码：QWEasdzxc123
INSERT INTO `sys_user`
(`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `birthday`, `status`,
 `user_level`, `experience_points`, `level_updated_at`, `mfa_enabled`,
 `last_login_time`, `last_login_ip`, `created_at`, `updated_at`, `deleted_flag`, `remark`)
VALUES (1, 'admin', '$2a$10$eqluKnKwJ8NY2Bku33Ol.evobljXBXX0nUS2Yy0Z3XukbyqYp8AnK', '管理员', 'admin@blog.local',
        '13800000000', NULL, 3, NULL, 1,
        1, 0, NULL, 0,
        NULL, NULL, NOW(), NOW(), 0, '系统初始化超级管理员账号'),
       (2, 'demo', '$2a$10$eqluKnKwJ8NY2Bku33Ol.evobljXBXX0nUS2Yy0Z3XukbyqYp8AnK', '演示用户', 'demo@blog.local',
        '13900000000', NULL, 3, NULL, 1,
        1, 0, NULL, 0,
        NULL, NULL, NOW(), NOW(), 0, '系统初始化演示账号');

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1),
       (2, 2);

COMMIT;

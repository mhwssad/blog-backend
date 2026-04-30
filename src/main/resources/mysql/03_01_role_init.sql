USE blog_backend;

-- 角色初始化脚本
-- 执行顺序：03_01（本文件）→ 03_02_user_init.sql → ...

START TRANSACTION;

DELETE FROM `sys_role_menu` WHERE `role_id` IN (1, 2, 3);
DELETE FROM `sys_role` WHERE `id` IN (1, 2, 3);

INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `data_scope`, `create_by`, `create_time`, `update_by`,
                        `update_time`, `is_deleted`)
VALUES (1, '超级管理员', 'admin', 1, 1, 1, 1, NOW(), 1, NOW(), 0),
       (2, '普通用户', 'user', 2, 1, 4, 1, NOW(), 1, NOW(), 0),
       (3, '作者', 'author', 3, 1, 4, 1, NOW(), 1, NOW(), 0);

COMMIT;

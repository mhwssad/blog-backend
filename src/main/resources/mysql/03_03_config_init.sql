USE blog_backend;

-- 系统参数配置初始化脚本
-- 前置依赖：03_01_role_init.sql

START TRANSACTION;

DELETE FROM `sys_config` WHERE `id` IN (1, 2, 3, 4, 5, 6, 7, 8);

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
        1, NOW(), 1, 0),
       (7, '普通用户文章总量上限', 'article.max-count.normal-user', '20', '普通注册用户可创建的文章总数上限，0 表示不限制', NOW(),
        1, NOW(), 1, 0),
       (8, '作者文章总量上限', 'article.max-count.author', '200', '作者角色可创建的文章总数上限，0 表示不限制', NOW(),
        1, NOW(), 1, 0);

COMMIT;

CREATE DATABASE IF NOT EXISTS blog_backend CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
use blog_backend;
-- 用户经验流水表
DROP TABLE IF EXISTS `user_experience_log`;
CREATE TABLE `user_experience_log`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        bigint       NOT NULL COMMENT '用户ID',
    `source_type`    varchar(32)  NOT NULL COMMENT '经验来源类型',
    `source_biz_id`  varchar(64) DEFAULT NULL COMMENT '来源业务ID',
    `xp_value`       int          NOT NULL COMMENT '本次经验值',
    `idempotent_key` varchar(128) NOT NULL COMMENT '幂等键',
    `log_date`       date         NOT NULL COMMENT '入账日期',
    `created_at`     datetime    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_exp_log_idempotent` (`idempotent_key`) COMMENT '幂等键唯一索引',
    KEY `idx_exp_log_user_date_source` (`user_id`, `log_date`, `source_type`) COMMENT '按用户日期来源聚合',
    KEY `idx_exp_log_user_date` (`user_id`, `log_date`) COMMENT '按用户日期聚合'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COMMENT = '用户经验流水表';

-- 经验体系配置项
INSERT IGNORE INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`, `create_time`, `update_time`)
VALUES ('xp.source.daily_login.value', '每日登录经验值', '10', '每日首次登录获得', NOW(), NOW()),
       ('xp.source.article_publish.value', '文章发布经验值', '20', '每篇文章获得', NOW(), NOW()),
       ('xp.source.comment_create.value', '评论发布经验值', '5', '每条评论获得', NOW(), NOW()),
       ('xp.source.like_given.value', '点赞经验值', '2', '主动点赞获得', NOW(), NOW()),
       ('xp.source.like_received.value', '被点赞经验值', '3', '内容被点赞获得', NOW(), NOW()),
       ('xp.source.chat_message.value', '聊天消息经验值', '1', '每条聊天消息获得', NOW(), NOW()),
       ('xp.source.daily_login.enabled', '每日登录经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.source.article_publish.enabled', '文章发布经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.source.comment_create.enabled', '评论发布经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.source.like_given.enabled', '点赞经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.source.like_received.enabled', '被点赞经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.source.chat_message.enabled', '聊天消息经验开关', '1', '1-启用 0-停用', NOW(), NOW()),
       ('xp.daily.total-cap', '每日经验总上限', '200', '每日可获得经验上限', NOW(), NOW()),
       ('xp.daily.chat_message.cap', '每日聊天经验上限', '30', '聊天消息单项上限', NOW(), NOW()),
       ('xp.daily.comment_create.cap', '每日评论经验上限', '50', '评论单项上限', NOW(), NOW()),
       ('chat.hall.speak.min-level', '大厅发言最低等级', '1', '全站大厅默认发言等级门槛，1 表示不限制', NOW(), NOW()),
       ('chat.group.create.min-level', '建群最低等级', '2', '创建普通群聊所需最低等级，1 表示不限制', NOW(), NOW()),
       ('chat.group.create.max-count', '用户可创建群聊数量上限', '20', '单个用户可创建的正常普通群数量上限，0 表示不限制', NOW(), NOW()),
       ('chat.channel-create-application.min-level', '频道创建申请最低等级', '2', '申请创建主题频道所需最低等级，1 表示不限制', NOW(), NOW());

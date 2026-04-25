-- ============================================
-- 粉丝关注模块表结构
-- 包含：用户关注关系表
-- 设计目标：支持关注、取关、粉丝列表、关注列表、互关判断与软取消后恢复
-- ============================================

USE
blog_backend;

DROP TABLE IF EXISTS sys_user_follow;

CREATE TABLE sys_user_follow
(
    id                BIGINT AUTO_INCREMENT COMMENT '关注关系ID' PRIMARY KEY,
    follower_id       BIGINT                                NOT NULL COMMENT '关注人ID',
    following_id      BIGINT                                NOT NULL COMMENT '被关注人ID',
    follow_status     TINYINT     DEFAULT 1                 NOT NULL COMMENT '关注状态：0-已取关，1-已关注',
    is_special_follow TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否特别关注：0-否，1-是',
    source            VARCHAR(32) DEFAULT 'manual'          NOT NULL COMMENT '关注来源：manual/recommend/system',
    follow_time       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '最近一次关注时间',
    unfollow_time     DATETIME NULL COMMENT '最近一次取关时间',
    remark            VARCHAR(256) NULL COMMENT '备注',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_follower_following (follower_id, following_id) COMMENT '同一关注关系唯一',
    INDEX             idx_follower_status_time (follower_id, follow_status, follow_time DESC) COMMENT '查询我的关注列表',
    INDEX             idx_following_status_time (following_id, follow_status, follow_time DESC) COMMENT '查询我的粉丝列表',
    INDEX             idx_follow_pair_status (follower_id, following_id, follow_status) COMMENT '判断是否已关注/互关',
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> following_id)
) COMMENT '用户关注关系表'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

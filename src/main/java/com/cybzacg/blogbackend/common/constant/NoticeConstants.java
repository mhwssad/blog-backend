package com.cybzacg.blogbackend.common.constant;

/**
 * 通知公告相关常量。<p>定义通知目标类型、发布状态和阅读状态。
 */
public final class NoticeConstants {
    public static final Integer TARGET_ALL = 1;
    public static final Integer TARGET_SPECIFIED = 2;

    public static final Integer PUBLISH_STATUS_REVOKED = -1;
    public static final Integer PUBLISH_STATUS_DRAFT = 0;
    public static final Integer PUBLISH_STATUS_PUBLISHED = 1;

    public static final Integer READ_UNREAD = 0;
    public static final Integer READ_READ = 1;

    private NoticeConstants() {
    }
}

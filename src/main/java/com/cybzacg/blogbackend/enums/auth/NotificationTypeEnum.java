package com.cybzacg.blogbackend.enums.auth;

import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotificationSetting;
import lombok.Getter;

/**
 * 用户通知偏好类型枚举。
 */
@Getter
public enum NotificationTypeEnum {
    COMMENT_ME("comment_me", "评论我"),
    LIKE_ME("like_me", "点赞我"),
    COLLECT_ARTICLE("collect_article", "收藏我文章"),
    FOLLOW_ME("follow_me", "有人关注我"),
    PRIVATE_MESSAGE("private_message", "收到私聊"),
    GROUP_MENTION("group_mention", "群聊有人@我"),
    CHANNEL_ANNOUNCEMENT("channel_announcement", "频道公告"),
    SYSTEM_ANNOUNCEMENT("system_announcement", "系统公告"),
    AI_TASK_DONE("ai_task_done", "AI任务完成"),
    REPORT_RESULT("report_result", "举报处理结果"),
    FORUM_POST_ESSENCE("forum_post_essence", "论坛帖子设为精华"),
    FORUM_REPLY_ME("forum_reply_me", "论坛回复我"),
    FORUM_LIKE_ME("forum_like_me", "论坛点赞我");

    private final String code;
    private final String label;

    NotificationTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static NotificationTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (NotificationTypeEnum item : values()) {
            if (item.code.equals(code.trim())) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }

    public boolean isEnabled(SysUserNotificationSetting setting) {
        if (setting == null) {
            return true;
        }
        return switch (this) {
            case COMMENT_ME -> !Integer.valueOf(0).equals(setting.getCommentNoticeEnabled());
            case LIKE_ME -> !Integer.valueOf(0).equals(setting.getLikeNoticeEnabled());
            case COLLECT_ARTICLE -> !Integer.valueOf(0).equals(setting.getCollectNoticeEnabled());
            case FOLLOW_ME -> !Integer.valueOf(0).equals(setting.getFollowNoticeEnabled());
            case PRIVATE_MESSAGE -> !Integer.valueOf(0).equals(setting.getPrivateChatNoticeEnabled());
            case GROUP_MENTION -> !Integer.valueOf(0).equals(setting.getMentionNoticeEnabled());
            case CHANNEL_ANNOUNCEMENT -> !Integer.valueOf(0).equals(setting.getChannelAnnouncementEnabled());
            case SYSTEM_ANNOUNCEMENT -> !Integer.valueOf(0).equals(setting.getSystemNoticeEnabled());
            case AI_TASK_DONE -> !Integer.valueOf(0).equals(setting.getAiTaskNoticeEnabled());
            case REPORT_RESULT -> !Integer.valueOf(0).equals(setting.getReportResultNoticeEnabled());
            case FORUM_POST_ESSENCE -> !Integer.valueOf(0).equals(setting.getForumEssenceNoticeEnabled());
            case FORUM_REPLY_ME -> !Integer.valueOf(0).equals(setting.getForumReplyNoticeEnabled());
            case FORUM_LIKE_ME -> !Integer.valueOf(0).equals(setting.getForumLikeNoticeEnabled());
        };
    }

    public void apply(SysUserNotificationSetting setting, boolean enabled) {
        int value = enabled ? 1 : 0;
        switch (this) {
            case COMMENT_ME -> setting.setCommentNoticeEnabled(value);
            case LIKE_ME -> setting.setLikeNoticeEnabled(value);
            case COLLECT_ARTICLE -> setting.setCollectNoticeEnabled(value);
            case FOLLOW_ME -> setting.setFollowNoticeEnabled(value);
            case PRIVATE_MESSAGE -> setting.setPrivateChatNoticeEnabled(value);
            case GROUP_MENTION -> setting.setMentionNoticeEnabled(value);
            case CHANNEL_ANNOUNCEMENT -> setting.setChannelAnnouncementEnabled(value);
            case SYSTEM_ANNOUNCEMENT -> setting.setSystemNoticeEnabled(value);
            case AI_TASK_DONE -> setting.setAiTaskNoticeEnabled(value);
            case REPORT_RESULT -> setting.setReportResultNoticeEnabled(value);
            case FORUM_POST_ESSENCE -> setting.setForumEssenceNoticeEnabled(value);
            case FORUM_REPLY_ME -> setting.setForumReplyNoticeEnabled(value);
            case FORUM_LIKE_ME -> setting.setForumLikeNoticeEnabled(value);
        }
    }
}

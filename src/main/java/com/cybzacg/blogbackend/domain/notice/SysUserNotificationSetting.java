package com.cybzacg.blogbackend.domain.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知偏好设置表。
 *
 * @TableName sys_user_notification_setting
 */
@TableName(value = "sys_user_notification_setting")
@Data
public class SysUserNotificationSetting {
    /**
     * 设置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评论通知开关
     */
    private Integer commentNoticeEnabled;

    /**
     * 点赞通知开关
     */
    private Integer likeNoticeEnabled;

    /**
     * 收藏通知开关
     */
    private Integer collectNoticeEnabled;

    /**
     * 关注通知开关
     */
    private Integer followNoticeEnabled;

    /**
     * 私聊通知开关
     */
    private Integer privateChatNoticeEnabled;

    /**
     * @提醒开关
     */
    private Integer mentionNoticeEnabled;

    /**
     * 频道公告通知开关
     */
    private Integer channelAnnouncementEnabled;

    /**
     * 系统公告通知开关
     */
    private Integer systemNoticeEnabled;

    /**
     * AI任务完成通知开关
     */
    private Integer aiTaskNoticeEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

package com.cybzacg.blogbackend.dto.domain.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子与频道关联表。
 */
@Data
@TableName("forum_post_channel_link")
public class ForumPostChannelLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 论坛帖子ID */
    private Long forumPostId;
    /** 频道会话ID */
    private Long conversationId;
    /** 关联方式：manual_share */
    private String linkType;
    /** 关联人ID */
    private Long linkedBy;
    /** 关联时间 */
    private LocalDateTime linkedAt;
    /** 状态：0-失效，1-正常 */
    private Integer status;
    private LocalDateTime createdAt;
}

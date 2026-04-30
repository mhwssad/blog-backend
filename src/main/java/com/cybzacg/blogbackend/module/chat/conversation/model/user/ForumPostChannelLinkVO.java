package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "论坛帖子频道关联")
public class ForumPostChannelLinkVO {
    @Schema(description = "关联ID")
    private Long id;
    @Schema(description = "论坛帖子ID")
    private Long forumPostId;
    @Schema(description = "频道会话ID")
    private Long conversationId;
    @Schema(description = "频道名称")
    private String channelName;
    @Schema(description = "关联方式")
    private String linkType;
    @Schema(description = "关联人ID")
    private Long linkedBy;
    @Schema(description = "关联时间")
    private LocalDateTime linkedAt;
}

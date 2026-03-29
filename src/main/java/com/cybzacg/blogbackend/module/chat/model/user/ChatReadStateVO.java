package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 会话已读状态。
 */
@Data
@Schema(description = "会话已读状态")
public class ChatReadStateVO {
    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "最后已读消息ID")
    private Long readMessageId;

    @Schema(description = "最后已读时间")
    private Date readAt;

    @Schema(description = "最后已送达消息ID")
    private Long deliveredMessageId;

    @Schema(description = "最后已送达时间")
    private Date deliveredAt;

    @Schema(description = "未读数")
    private Integer unreadCount;
}

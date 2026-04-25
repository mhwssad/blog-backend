package com.cybzacg.blogbackend.module.chat.model.admin;

import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台消息详情。
 */
@Data
@Schema(description = "后台消息详情")
public class ChatAdminMessageDetailVO {
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送人ID")
    private Long senderId;

    @Schema(description = "发送人用户名")
    private String senderUsername;

    @Schema(description = "发送人昵称")
    private String senderNickname;

    @Schema(description = "发送人头像")
    private String senderAvatar;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "文件消息载荷")
    private ChatFilePayloadVO file;

    @Schema(description = "回复的消息ID")
    private Long replyMessageId;

    @Schema(description = "回复消息快照")
    private ChatReplyMessageVO reply;

    @Schema(description = "客户端消息ID")
    private String clientMessageId;

    @Schema(description = "发送状态")
    private Integer sendStatus;

    @Schema(description = "撤回状态")
    private Integer revokeStatus;

    @Schema(description = "撤回操作人ID")
    private Long revokedBy;

    @Schema(description = "撤回时间")
    private LocalDateTime revokedAt;

    @Schema(description = "接收成员总数")
    private Long totalRecipientCount;

    @Schema(description = "已送达成员数")
    private Long deliveredRecipientCount;

    @Schema(description = "已读成员数")
    private Long readRecipientCount;

    @Schema(description = "是否编辑过")
    private Boolean edited;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}

package com.cybzacg.blogbackend.module.chat.model.user;

import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 聊天消息信息。
 */
@Data
@Schema(description = "聊天消息信息")
public class ChatMessageVO {
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

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "文件消息载荷")
    private ChatFilePayloadVO file;

    @Schema(description = "回复的消息ID")
    private Long replyMessageId;

    @Schema(description = "客户端消息ID")
    private String clientMessageId;

    @Schema(description = "是否当前用户自己发送")
    private Boolean self;

    @Schema(description = "当前用户视角下的投递状态：0待投递，1已送达，2已读")
    private Integer deliveryStatus;

    @Schema(description = "当前用户是否已读")
    private Boolean readByCurrentUser;

    @Schema(description = "当前用户读到该消息的时间")
    private Date readAt;

    @Schema(description = "是否已撤回")
    private Boolean revoked;

    @Schema(description = "是否编辑过")
    private Boolean edited;

    @Schema(description = "更新时间")
    private Date updatedAt;

    @Schema(description = "发送时间")
    private Date createdAt;
}

package com.cybzacg.blogbackend.module.chat.model.admin;

import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationLastMessageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台会话信息。
 */
@Data
@Schema(description = "后台会话信息")
public class ChatAdminConversationVO {
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话类型：single/group/global")
    private String conversationType;

    @Schema(description = "会话名称")
    private String name;

    @Schema(description = "会话头像")
    private String avatar;

    @Schema(description = "群主ID")
    private Long ownerId;

    @Schema(description = "群公告")
    private String notice;

    @Schema(description = "群主用户名")
    private String ownerUsername;

    @Schema(description = "群主昵称")
    private String ownerNickname;

    @Schema(description = "是否全站群聊")
    private Boolean allSite;

    @Schema(description = "会话状态")
    private Integer status;

    @Schema(description = "当前活跃成员数")
    private Long memberCount;

    @Schema(description = "最后一条消息")
    private ChatConversationLastMessageVO lastMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

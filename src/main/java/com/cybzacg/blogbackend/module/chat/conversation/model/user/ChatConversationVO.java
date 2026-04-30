package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话信息。
 */
@Data
@Schema(description = "会话信息")
public class ChatConversationVO {
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话类型：single/group/global")
    private String conversationType;

    @Schema(description = "业务场景")
    private String sceneType;

    @Schema(description = "会话名称")
    private String name;

    @Schema(description = "会话头像")
    private String avatar;

    @Schema(description = "群主ID")
    private Long ownerId;

    @Schema(description = "群公告")
    private String notice;

    @Schema(description = "是否全站群聊")
    private Boolean allSite;

    @Schema(description = "会话状态")
    private Integer status;

    @Schema(description = "可见范围")
    private String visibilityScope;

    @Schema(description = "访客是否可见：0-否，1-是")
    private Integer allowGuestView;

    @Schema(description = "是否需要加入后发言：0-否，1-是")
    private Integer requireJoinToSpeak;

    @Schema(description = "加入规则")
    private String joinRule;

    @Schema(description = "发言最低等级限制")
    private Integer speakLevelLimit;

    @Schema(description = "成员上限")
    private Integer memberLimit;

    @Schema(description = "慢速模式秒数")
    private Integer slowModeSeconds;

    @Schema(description = "展示排序")
    private Integer displaySort;

    @Schema(description = "频道或群分类编码")
    private String channelCategoryCode;

    @Schema(description = "当前用户在会话中的角色")
    private String selfRole;

    @Schema(description = "活跃成员数量")
    private Long memberCount;

    @Schema(description = "未读数")
    private Integer unreadCount;

    @Schema(description = "单聊目标用户ID")
    private Long targetUserId;

    @Schema(description = "单聊目标用户名")
    private String targetUsername;

    @Schema(description = "单聊目标昵称")
    private String targetNickname;

    @Schema(description = "最后已读消息ID")
    private Long lastReadMessageId;

    @Schema(description = "最后已读时间")
    private LocalDateTime lastReadAt;

    @Schema(description = "最后已送达消息ID")
    private Long lastDeliveredMessageId;

    @Schema(description = "最后已送达时间")
    private LocalDateTime lastDeliveredAt;

    @Schema(description = "最后一条消息")
    private ChatConversationLastMessageVO lastMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

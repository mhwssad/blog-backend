package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群邀请链接视图。
 */
@Data
@Schema(description = "群邀请链接视图")
public class ChatGroupInviteLinkVO {
    private Long id;
    private Long conversationId;
    private String inviteToken;
    private Long createdBy;
    private LocalDateTime expireAt;
    private Integer maxUseCount;
    private Integer usedCount;
    private Integer status;
    private Boolean expired;
    private Boolean usageExhausted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.cybzacg.blogbackend.module.forum.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "论坛帖子分享到频道请求")
public class ForumPostChannelShareRequest {
    @NotNull(message = "目标频道会话ID不能为空")
    @Schema(description = "目标频道会话ID")
    private Long conversationId;
}

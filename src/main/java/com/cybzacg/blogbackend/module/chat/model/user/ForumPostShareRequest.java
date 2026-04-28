package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "帖子分享到频道请求")
public class ForumPostShareRequest {
    @NotNull
    @Schema(description = "论坛帖子ID")
    private Long forumPostId;
    @NotNull
    @Schema(description = "目标频道会话ID")
    private Long conversationId;
}

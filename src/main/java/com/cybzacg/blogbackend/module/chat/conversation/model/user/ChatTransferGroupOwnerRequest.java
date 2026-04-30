package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转让群主请求。
 */
@Data
@Schema(description = "转让群主请求")
public class ChatTransferGroupOwnerRequest {
    @NotNull(message = "新群主用户ID不能为空")
    @Schema(description = "新群主用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetUserId;
}

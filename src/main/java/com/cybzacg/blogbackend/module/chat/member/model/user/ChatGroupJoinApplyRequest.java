package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 入群申请请求。
 */
@Data
@Schema(description = "入群申请请求")
public class ChatGroupJoinApplyRequest {
    @Size(max = 512, message = "申请附言长度不能超过512")
    @Schema(description = "申请附言")
    private String applyMessage;
}

package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 群成员操作请求。
 */
@Data
@Schema(description = "群成员操作请求")
public class ChatGroupMemberOperateRequest {
    @NotEmpty(message = "成员用户ID不能为空")
    @Schema(description = "成员用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberUserIds;
}

package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建群聊请求。
 */
@Data
@Schema(description = "创建群聊请求")
public class ChatCreateGroupRequest {
    @NotBlank(message = "群名称不能为空")
    @Size(max = 128, message = "群名称长度不能超过128")
    @Schema(description = "群名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 512, message = "群头像长度不能超过512")
    @Schema(description = "群头像")
    private String avatar;

    @NotEmpty(message = "群成员不能为空")
    @Schema(description = "初始成员用户ID列表，不需要包含自己", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberUserIds;
}

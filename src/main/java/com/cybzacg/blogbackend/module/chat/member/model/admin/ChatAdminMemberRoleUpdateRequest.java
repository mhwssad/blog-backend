package com.cybzacg.blogbackend.module.chat.member.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 后台更新成员角色请求。
 */
@Data
@Schema(description = "后台更新成员角色请求")
public class ChatAdminMemberRoleUpdateRequest {
    @NotBlank(message = "成员角色不能为空")
    @Pattern(regexp = "owner|admin|member", message = "成员角色不合法")
    @Schema(description = "成员角色：owner/admin/member", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;
}

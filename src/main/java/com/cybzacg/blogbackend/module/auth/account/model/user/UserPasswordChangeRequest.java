package com.cybzacg.blogbackend.module.auth.account.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改密码请求。
 */
@Data
@Schema(description = "修改密码请求")
public class UserPasswordChangeRequest {
    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度为8-64位")
    @Schema(description = "新密码")
    private String newPassword;
}

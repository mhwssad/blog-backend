package com.cybzacg.blogbackend.module.auth.account.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码重置请求。
 */
@Data
@Schema(description = "找回密码重置请求")
public class PasswordResetRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度为8-64位")
    @Schema(description = "新密码")
    private String newPassword;
}

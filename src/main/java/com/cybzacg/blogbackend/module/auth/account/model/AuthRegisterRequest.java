package com.cybzacg.blogbackend.module.auth.account.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class AuthRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "new_user")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "昵称", example = "新用户")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "user@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}

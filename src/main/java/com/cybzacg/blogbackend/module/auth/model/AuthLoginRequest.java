package com.cybzacg.blogbackend.module.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class AuthLoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号，支持用户名/邮箱/手机号", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}

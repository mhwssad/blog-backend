package com.cybzacg.blogbackend.module.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "邮箱验证码登录请求")
public class AuthEmailLoginRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱地址", example = "admin@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "邮箱验证码", example = "123456")
    private String code;
}

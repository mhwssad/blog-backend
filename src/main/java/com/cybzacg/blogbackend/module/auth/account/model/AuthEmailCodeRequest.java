package com.cybzacg.blogbackend.module.auth.account.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "邮箱验证码请求")
public class AuthEmailCodeRequest {

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱地址", example = "admin@example.com")
    private String email;
}

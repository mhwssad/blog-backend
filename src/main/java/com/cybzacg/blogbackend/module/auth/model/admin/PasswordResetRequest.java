package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "密码重置请求")
public class PasswordResetRequest {
    @NotBlank(message = "密码不能为空")
    @Schema(description = "新密码", example = "123456")
    private String password;
}

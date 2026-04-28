package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "2FA验证码校验请求")
public class MfaVerifyRequest {
    @NotBlank
    @Schema(description = "验证码", example = "123456")
    private String code;
}

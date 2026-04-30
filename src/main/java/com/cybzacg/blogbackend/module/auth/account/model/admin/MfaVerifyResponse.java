package com.cybzacg.blogbackend.module.auth.account.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "2FA验证响应")
public class MfaVerifyResponse {
    @Schema(description = "操作票据")
    private String ticket;
    @Schema(description = "过期时间（秒）")
    private Long expiresIn;
}

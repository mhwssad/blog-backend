package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "账号接管请求")
public class AccountTakeoverRequest {
    @NotNull
    @Schema(description = "目标用户ID")
    private Long targetUserId;
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

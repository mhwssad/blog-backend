package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "调整用户等级请求")
public class AdjustLevelRequest {
    @NotNull
    @Schema(description = "等级值")
    private Integer level;
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

package com.cybzacg.blogbackend.module.auth.config.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "调整用户经验请求")
public class AdjustExperienceRequest {
    @NotNull
    @Schema(description = "经验值")
    private Integer experience;
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

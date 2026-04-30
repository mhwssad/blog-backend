package com.cybzacg.blogbackend.module.auth.account.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "封禁/解封用户请求")
public class BanUserRequest {
    @NotNull
    @Schema(description = "状态：0-解封，1-封禁")
    private Integer status;
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

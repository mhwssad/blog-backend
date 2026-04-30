package com.cybzacg.blogbackend.module.auth.account.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "通用2FA票据请求")
public class MfaTicketRequest {
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

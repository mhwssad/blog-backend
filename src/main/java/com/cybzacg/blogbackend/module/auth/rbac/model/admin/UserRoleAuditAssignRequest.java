package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "带审计的用户角色分配请求")
public class UserRoleAuditAssignRequest {
    @NotNull(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
    @NotBlank
    @Schema(description = "2FA操作票据")
    private String mfaTicket;
}

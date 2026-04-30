package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色菜单分配请求")
public class RoleMenuAssignRequest {
    @NotNull(message = "菜单ID列表不能为空")
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}

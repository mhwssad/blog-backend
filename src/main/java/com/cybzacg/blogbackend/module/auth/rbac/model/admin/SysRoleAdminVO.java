package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "后台角色信息")
public class SysRoleAdminVO {
    @Schema(description = "角色ID")
    private Long id;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "角色编码")
    private String code;
    @Schema(description = "显示顺序")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "数据权限")
    private Integer dataScope;
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}

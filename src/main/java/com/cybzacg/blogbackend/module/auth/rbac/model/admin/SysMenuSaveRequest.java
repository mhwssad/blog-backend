package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "菜单新增/修改请求")
public class SysMenuSaveRequest {
    @NotNull(message = "父菜单ID不能为空")
    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "树路径")
    private String treePath;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称")
    private String name;

    @NotBlank(message = "菜单类型不能为空")
    @Schema(description = "菜单类型")
    private String type;

    @Schema(description = "路由名称")
    private String routeName;

    @Schema(description = "路由路径")
    private String routePath;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perm;

    @Schema(description = "始终显示")
    private Integer alwaysShow;

    @Schema(description = "缓存")
    private Integer keepAlive;

    @Schema(description = "显示状态")
    private Integer visible;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "跳转路径")
    private String redirect;

    @Schema(description = "路由参数")
    private Object params;
}

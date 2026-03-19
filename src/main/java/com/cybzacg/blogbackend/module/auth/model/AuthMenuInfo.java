package com.cybzacg.blogbackend.module.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "当前用户菜单信息")
public class AuthMenuInfo {
    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

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

    @Schema(description = "是否显示")
    private Integer visible;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "跳转地址")
    private String redirect;

    @Schema(description = "始终显示")
    private Integer alwaysShow;

    @Schema(description = "缓存")
    private Integer keepAlive;

    @Schema(description = "路由参数")
    private Object params;

    @Schema(description = "子菜单")
    private List<AuthMenuInfo> children;
}

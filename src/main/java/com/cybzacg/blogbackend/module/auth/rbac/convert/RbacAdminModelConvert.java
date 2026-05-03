package com.cybzacg.blogbackend.module.auth.rbac.convert;

import com.cybzacg.blogbackend.domain.auth.SysMenu;
import com.cybzacg.blogbackend.domain.auth.SysRole;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.*;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * RBAC 管理端对象转换器，处理用户、角色、菜单的增删改查映射。
 */
@Mapper(
        componentModel = "spring",
        imports = StrUtils.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RbacAdminModelConvert {

    SysUserAdminVO toUserVO(SysUser user);

    SysRoleAdminVO toRoleVO(SysRole role);

    SysMenuAdminVO toMenuVO(SysMenu menu);

    @Mapping(target = "username", expression = "java(StrUtils.normalize(request.getUsername()))")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", expression = "java(StrUtils.normalize(request.getEmail()))")
    @Mapping(target = "phone", expression = "java(StrUtils.normalize(request.getPhone()))")
    @Mapping(target = "status", expression = "java(request.getStatus() != null ? request.getStatus() : 1)")
    SysUser toUser(SysUserSaveRequest request);

    @InheritConfiguration(name = "toUser")
    void updateUser(SysUserSaveRequest request, @MappingTarget SysUser user);

    @Mapping(target = "name", expression = "java(StrUtils.normalize(request.getName()))")
    @Mapping(target = "code", expression = "java(StrUtils.normalize(request.getCode()))")
    @Mapping(target = "status", expression = "java(request.getStatus() != null ? request.getStatus() : 1)")
    SysRole toRole(SysRoleSaveRequest request);

    @InheritConfiguration(name = "toRole")
    void updateRole(SysRoleSaveRequest request, @MappingTarget SysRole role);

    @Mapping(target = "treePath", ignore = true)
    @Mapping(target = "name", expression = "java(StrUtils.normalize(request.getName()))")
    @Mapping(target = "type", expression = "java(StrUtils.normalize(request.getType()))")
    @Mapping(target = "routeName", expression = "java(StrUtils.normalize(request.getRouteName()))")
    @Mapping(target = "routePath", expression = "java(StrUtils.normalize(request.getRoutePath()))")
    @Mapping(target = "component", expression = "java(StrUtils.normalize(request.getComponent()))")
    @Mapping(target = "perm", expression = "java(StrUtils.normalize(request.getPerm()))")
    @Mapping(target = "alwaysShow", expression = "java(request.getAlwaysShow() != null ? request.getAlwaysShow() : 0)")
    @Mapping(target = "keepAlive", expression = "java(request.getKeepAlive() != null ? request.getKeepAlive() : 0)")
    @Mapping(target = "visible", expression = "java(request.getVisible() != null ? request.getVisible() : 1)")
    @Mapping(target = "sort", expression = "java(request.getSort() != null ? request.getSort() : 0)")
    @Mapping(target = "icon", expression = "java(StrUtils.normalize(request.getIcon()))")
    @Mapping(target = "redirect", expression = "java(StrUtils.normalize(request.getRedirect()))")
    SysMenu toMenu(SysMenuSaveRequest request);

    @InheritConfiguration(name = "toMenu")
    void updateMenu(SysMenuSaveRequest request, @MappingTarget SysMenu menu);

    default SysUserAdminVO toUserVO(SysUser user, List<Long> roleIds) {
        SysUserAdminVO userVO = toUserVO(user);
        userVO.setRoleIds(roleIds);
        return userVO;
    }

    default SysRoleAdminVO toRoleVO(SysRole role, List<Long> menuIds) {
        SysRoleAdminVO roleVO = toRoleVO(role);
        roleVO.setMenuIds(menuIds);
        return roleVO;
    }

    @AfterMapping
    default void initChildren(@MappingTarget SysMenuAdminVO menuVO) {
        menuVO.setChildren(new ArrayList<>());
    }
}

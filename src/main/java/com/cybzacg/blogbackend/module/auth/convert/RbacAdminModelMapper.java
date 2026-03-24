package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", imports = StrUtils.class)
public interface RbacAdminModelMapper {

    @Mapping(target = "roleIds", ignore = true)
    SysUserAdminVO toUserVO(SysUser user);

    @Mapping(target = "menuIds", ignore = true)
    SysRoleAdminVO toRoleVO(SysRole role);

    @Mapping(target = "children", ignore = true)
    SysMenuAdminVO toMenuVO(SysMenu menu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", expression = "java(StrUtils.normalize(request.getUsername()))")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "nickname", source = "nickname")
    @Mapping(target = "email", expression = "java(StrUtils.normalize(request.getEmail()))")
    @Mapping(target = "phone", expression = "java(StrUtils.normalize(request.getPhone()))")
    @Mapping(target = "avatar", source = "avatar")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "birthday", source = "birthday")
    @Mapping(target = "status", expression = "java(request.getStatus() != null ? request.getStatus() : 1)")
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedFlag", ignore = true)
    @Mapping(target = "remark", source = "remark")
    SysUser toUser(SysUserSaveRequest request);

    @InheritConfiguration(name = "toUser")
    void updateUser(SysUserSaveRequest request, @MappingTarget SysUser user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(StrUtils.normalize(request.getName()))")
    @Mapping(target = "code", expression = "java(StrUtils.normalize(request.getCode()))")
    @Mapping(target = "sort", source = "sort")
    @Mapping(target = "status", expression = "java(request.getStatus() != null ? request.getStatus() : 1)")
    @Mapping(target = "dataScope", source = "dataScope")
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    SysRole toRole(SysRoleSaveRequest request);

    @InheritConfiguration(name = "toRole")
    void updateRole(SysRoleSaveRequest request, @MappingTarget SysRole role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", source = "parentId")
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
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "params", source = "params")
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

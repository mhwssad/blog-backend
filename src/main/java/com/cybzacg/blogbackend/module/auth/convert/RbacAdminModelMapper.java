package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RbacAdminModelMapper {

    @Mapping(target = "roleIds", ignore = true)
    SysUserAdminVO toUserVO(SysUser user);

    @Mapping(target = "menuIds", ignore = true)
    SysRoleAdminVO toRoleVO(SysRole role);

    @Mapping(target = "children", ignore = true)
    SysMenuAdminVO toMenuVO(SysMenu menu);

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

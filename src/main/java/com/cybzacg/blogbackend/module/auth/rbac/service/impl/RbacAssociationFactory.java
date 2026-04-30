package com.cybzacg.blogbackend.module.auth.rbac.service.impl;

import com.cybzacg.blogbackend.domain.auth.SysRoleMenu;
import com.cybzacg.blogbackend.domain.auth.SysUserRole;
import org.springframework.stereotype.Component;

/**
 * RBAC 关联对象工厂方法，收口 SysRoleMenu 和 SysUserRole 的创建逻辑。
 */
@Component
public class RbacAssociationFactory {

    public SysRoleMenu createRoleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    public SysUserRole createUserRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }
}

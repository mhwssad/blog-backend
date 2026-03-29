package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.cybzacg.blogbackend.mapper.SysRoleMenuMapper;
import com.cybzacg.blogbackend.module.auth.service.SysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 角色菜单关联服务实现。
 *
 * <p>负责角色与菜单关系的读取、替换与清理，供 RBAC 分配流程统一复用。
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu>
    implements SysRoleMenuService{

    /**
     * 查询角色当前绑定的菜单 ID 列表，并按去重结果返回给上层装配。
     */
    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return lambdaQuery()
                .eq(SysRoleMenu::getRoleId, roleId)
                .list()
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
    }

    /**
     * 以“先删后建”的方式重建角色菜单关系，确保分配结果与请求保持一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        removeByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        saveBatch(menuIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(menuId -> {
                    SysRoleMenu roleMenu = new SysRoleMenu();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuId(menuId);
                    return roleMenu;
                })
                .toList());
    }

    /**
     * 清理指定角色下的全部菜单绑定关系。
     */
    @Override
    public void removeByRoleId(Long roleId) {
        lambdaUpdate().eq(SysRoleMenu::getRoleId, roleId).remove();
    }

    /**
     * 清理指定菜单在所有角色中的历史绑定关系。
     */
    @Override
    public void removeByMenuId(Long menuId) {
        lambdaUpdate().eq(SysRoleMenu::getMenuId, menuId).remove();
    }
}





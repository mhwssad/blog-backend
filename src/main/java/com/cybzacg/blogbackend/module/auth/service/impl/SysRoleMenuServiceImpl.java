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
* @author liujian
* @description 针对表【sys_role_menu(角色菜单关联表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu>
    implements SysRoleMenuService{

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

    @Override
    public void removeByRoleId(Long roleId) {
        lambdaUpdate().eq(SysRoleMenu::getRoleId, roleId).remove();
    }

    @Override
    public void removeByMenuId(Long menuId) {
        lambdaUpdate().eq(SysRoleMenu::getMenuId, menuId).remove();
    }
}





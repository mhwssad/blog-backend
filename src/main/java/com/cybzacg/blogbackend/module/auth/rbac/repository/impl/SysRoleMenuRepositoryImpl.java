package com.cybzacg.blogbackend.module.auth.rbac.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.auth.SysRoleMenu;
import com.cybzacg.blogbackend.mapper.auth.SysRoleMenuMapper;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleMenuRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色菜单关系 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysRoleMenuRepositoryImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu>
        implements SysRoleMenuRepository {

    /**
     * 根据角色 ID 查询关联的菜单 ID 列表并去重。
     */
    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return list(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
    }

    /**
     * 根据角色 ID 删除所有关联的菜单关系。
     */
    @Override
    public void deleteByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
    }

    /**
     * 根据菜单 ID 删除所有关联的角色关系。
     */
    @Override
    public void deleteByMenuId(Long menuId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, menuId));
    }
}

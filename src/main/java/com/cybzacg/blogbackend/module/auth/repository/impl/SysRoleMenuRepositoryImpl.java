package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.cybzacg.blogbackend.mapper.SysRoleMenuMapper;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleMenuRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色菜单关系 Repository 实现。
 */
@Repository
public class SysRoleMenuRepositoryImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu>
        implements SysRoleMenuRepository {

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return list(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        remove(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, menuId));
    }
}

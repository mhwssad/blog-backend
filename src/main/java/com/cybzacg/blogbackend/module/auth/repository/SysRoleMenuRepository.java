package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysRoleMenu;

import java.util.List;

/**
 * 角色菜单关系 Repository。
 */
public interface SysRoleMenuRepository extends IService<SysRoleMenu> {
    List<Long> findMenuIdsByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    void deleteByMenuId(Long menuId);
}

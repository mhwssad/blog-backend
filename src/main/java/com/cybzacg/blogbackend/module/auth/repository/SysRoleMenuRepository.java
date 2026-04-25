package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysRoleMenu;

import java.util.List;

/**
 * 角色菜单关系 Repository。
 * <p>封装角色与菜单之间关联关系的持久化操作，提供按角色/菜单查询和删除等能力。
 */
public interface SysRoleMenuRepository extends IService<SysRoleMenu> {
    /**
     * 根据角色 ID 查询关联的菜单 ID 列表（去重）。
     */
    List<Long> findMenuIdsByRoleId(Long roleId);

    /**
     * 根据角色 ID 删除所有关联的菜单关系。
     */
    void deleteByRoleId(Long roleId);

    /**
     * 根据菜单 ID 删除所有关联的角色关系。
     */
    void deleteByMenuId(Long menuId);
}

package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统角色菜单服务接口。
 *
 * <p>定义系统角色菜单相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysRoleMenuService extends IService<SysRoleMenu> {

    List<Long> listMenuIdsByRoleId(Long roleId);

    void replaceRoleMenus(Long roleId, List<Long> menuIds);

    void removeByRoleId(Long roleId);

    void removeByMenuId(Long menuId);
}

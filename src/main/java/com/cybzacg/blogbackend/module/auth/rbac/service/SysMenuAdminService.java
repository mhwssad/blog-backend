package com.cybzacg.blogbackend.module.auth.rbac.service;

import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuSaveRequest;

import java.util.List;

/**
 * 系统菜单后台管理服务接口。
 *
 * <p>定义系统菜单后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysMenuAdminService {
    List<SysMenuAdminVO> listMenuTree();

    SysMenuAdminVO getMenu(Long id);

    SysMenuAdminVO createMenu(SysMenuSaveRequest request);

    SysMenuAdminVO updateMenu(Long id, SysMenuSaveRequest request);

    void deleteMenu(Long id);
}

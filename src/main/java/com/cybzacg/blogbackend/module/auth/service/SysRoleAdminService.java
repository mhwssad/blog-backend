package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;

import java.util.List;

/**
 * 系统角色后台管理服务接口。
 *
 * <p>定义系统角色后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysRoleAdminService {
    PageResult<SysRoleAdminVO> pageRoles(SysRolePageQuery query);

    SysRoleAdminVO getRole(Long id);

    SysRoleAdminVO createRole(SysRoleSaveRequest request);

    SysRoleAdminVO updateRole(Long id, SysRoleSaveRequest request);

    void updateStatus(Long id, Integer status);

    void deleteRole(Long id);

    List<Long> listMenuIds(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}

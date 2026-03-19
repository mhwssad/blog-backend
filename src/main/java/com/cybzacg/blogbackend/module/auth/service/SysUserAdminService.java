package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;

import java.util.List;

/**
 * 系统用户后台管理服务接口。
 *
 * <p>定义系统用户后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysUserAdminService {
    PageResult<SysUserAdminVO> pageUsers(SysUserPageQuery query);

    SysUserAdminVO getUser(Long id);

    SysUserAdminVO createUser(SysUserSaveRequest request);

    SysUserAdminVO updateUser(Long id, SysUserSaveRequest request);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String password);

    void deleteUser(Long id);

    List<Long> listRoleIds(Long userId);

    void assignRoles(Long userId, List<Long> roleIds);
}

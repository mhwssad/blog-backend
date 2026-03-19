package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysUserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统用户角色服务接口。
 *
 * <p>定义系统用户角色相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    List<Long> listRoleIdsByUserId(Long userId);

    void replaceUserRoles(Long userId, List<Long> roleIds);

    void removeByUserId(Long userId);

    void removeByRoleId(Long roleId);
}

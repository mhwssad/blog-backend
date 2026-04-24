package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserRole;

import java.util.List;

/**
 * 用户角色关系 Repository。
 */
public interface SysUserRoleRepository extends IService<SysUserRole> {
    List<Long> findRoleIdsByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);
}

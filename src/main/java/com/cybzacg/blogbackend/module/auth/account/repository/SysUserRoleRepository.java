package com.cybzacg.blogbackend.module.auth.account.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.auth.SysUserRole;

import java.util.List;

/**
 * 用户角色关系 Repository。
 * <p>封装用户与角色之间关联关系的持久化操作，提供按用户/角色查询和删除等能力。
 */
public interface SysUserRoleRepository extends IService<SysUserRole> {
    /**
     * 根据用户 ID 查询关联的角色 ID 列表（去重）。
     */
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 根据用户 ID 删除所有关联的角色关系。
     */
    void deleteByUserId(Long userId);

    /**
     * 根据角色 ID 删除所有关联的用户关系。
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除指定用户与角色的关联关系。
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}

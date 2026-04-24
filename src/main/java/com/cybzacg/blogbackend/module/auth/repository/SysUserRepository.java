package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;

import java.util.Collection;

/**
 * 系统用户 Repository。
 */
public interface SysUserRepository extends IService<SysUser> {
    SysUser findByUsername(String username);

    SysUser findByEmail(String email);

    boolean updateLoginInfo(Long userId, String ip);

    boolean existsActiveByIdentity(String identity);

    boolean existsActiveByField(String fieldName, String value);

    boolean existsActiveByUsername(String username, Long excludeId);

    boolean existsActiveByEmail(String email, Long excludeId);

    boolean existsActiveByPhone(String phone, Long excludeId);

    long countActiveByIds(Collection<Long> ids);

    Page<SysUser> pageByAdminConditions(SysUserPageQuery query);
}

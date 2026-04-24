package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 系统角色 Repository。
 */
public interface SysRoleRepository extends IService<SysRole> {
    List<String> findRoleCodesByUserId(Long userId);

    boolean existsActiveByName(String name, Long excludeId);

    boolean existsActiveByCode(String code, Long excludeId);

    long countActiveByIds(Collection<Long> ids);

    Page<SysRole> pageByAdminConditions(SysRolePageQuery query);
}

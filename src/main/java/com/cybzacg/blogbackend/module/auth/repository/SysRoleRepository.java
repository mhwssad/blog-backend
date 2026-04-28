package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 系统角色 Repository。
 * <p>封装角色实体的持久化操作，提供角色编码查询、唯一性校验及管理端分页等能力。
 */
public interface SysRoleRepository extends IService<SysRole> {
    /**
     * 根据用户 ID 查询其关联的角色编码列表。
     */
    List<String> findRoleCodesByUserId(Long userId);

    /**
     * 判断角色名称是否已被其他未删除角色占用（排除指定 ID）。
     */
    boolean existsActiveByName(String name, Long excludeId);

    /**
     * 判断角色编码是否已被其他未删除角色占用（排除指定 ID）。
     */
    boolean existsActiveByCode(String code, Long excludeId);

    /**
     * 根据角色编码查询未删除角色。
     */
    SysRole findByCode(String code);

    /**
     * 统计给定 ID 集合中未删除角色的数量。
     */
    long countActiveByIds(Collection<Long> ids);

    /**
     * 根据管理端查询条件对未删除角色进行分页。
     */
    Page<SysRole> pageByAdminConditions(SysRolePageQuery query);
}

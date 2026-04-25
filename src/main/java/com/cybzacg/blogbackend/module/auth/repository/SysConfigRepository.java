package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;

/**
 * 系统配置 Repository。
 * <p>封装系统配置实体的持久化操作，提供按配置键查询、唯一性校验及管理端分页等能力。
 */
public interface SysConfigRepository extends IService<SysConfig> {
    /**
     * 根据配置键查找配置项。
     */
    SysConfig findByConfigKey(String configKey);

    /**
     * 判断配置键是否已被其他未删除配置占用（排除指定 ID）。
     */
    boolean existsActiveByConfigKey(String configKey, Long excludeId);

    /**
     * 根据管理端查询条件对未删除配置进行分页。
     */
    Page<SysConfig> pageByAdminConditions(SysConfigPageQuery query);
}

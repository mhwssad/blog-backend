package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;

/**
 * 系统配置 Repository。
 */
public interface SysConfigRepository extends IService<SysConfig> {
    SysConfig findByConfigKey(String configKey);

    boolean existsActiveByConfigKey(String configKey, Long excludeId);

    Page<SysConfig> pageByAdminConditions(SysConfigPageQuery query);
}

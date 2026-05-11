package com.cybzacg.blogbackend.dto.repository.auth.config.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.config.SysConfig;
import com.cybzacg.blogbackend.dto.mapper.config.SysConfigMapper;
import com.cybzacg.blogbackend.dto.repository.auth.config.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

/**
 * 系统配置 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysConfigRepositoryImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigRepository {

    /**
     * 根据配置键查找配置项。
     */
    @Override
    public SysConfig findByConfigKey(String configKey) {
        return baseMapper.selectByConfigKey(configKey);
    }

    /**
     * 判断配置键是否已被其他未删除配置占用。
     */
    @Override
    public boolean existsActiveByConfigKey(String configKey, Long excludeId) {
        if (!StrUtils.hasText(configKey)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getIsDeleted, 0)
                .ne(excludeId != null, SysConfig::getId, excludeId));
    }

    /**
     * 根据管理端查询条件进行分页，按创建时间降序排列。
     */
    @Override
    public Page<SysConfig> pageByAdminConditions(SysConfigPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysConfig>()
                .like(StrUtils.hasText(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                .like(StrUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                .ge(query.getCreateTimeStart() != null, SysConfig::getCreateTime, query.getCreateTimeStart())
                .le(query.getCreateTimeEnd() != null, SysConfig::getCreateTime, query.getCreateTimeEnd())
                .eq(SysConfig::getIsDeleted, 0)
                .orderByDesc(SysConfig::getCreateTime)
                .orderByDesc(SysConfig::getId));
    }
}

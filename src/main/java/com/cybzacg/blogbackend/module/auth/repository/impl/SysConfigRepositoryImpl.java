package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.mapper.SysConfigMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysConfigRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 系统配置 Repository 实现。
 */
@Repository
public class SysConfigRepositoryImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigRepository {

    @Override
    public SysConfig findByConfigKey(String configKey) {
        return baseMapper.selectByConfigKey(configKey);
    }

    @Override
    public boolean existsActiveByConfigKey(String configKey, Long excludeId) {
        if (!StringUtils.hasText(configKey)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getIsDeleted, 0)
                .ne(excludeId != null, SysConfig::getId, excludeId));
    }

    @Override
    public Page<SysConfig> pageByAdminConditions(SysConfigPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysConfig>()
                .like(StringUtils.hasText(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                .like(StringUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                .ge(query.getCreateTimeStart() != null, SysConfig::getCreateTime, query.getCreateTimeStart())
                .le(query.getCreateTimeEnd() != null, SysConfig::getCreateTime, query.getCreateTimeEnd())
                .eq(SysConfig::getIsDeleted, 0)
                .orderByDesc(SysConfig::getCreateTime)
                .orderByDesc(SysConfig::getId));
    }
}

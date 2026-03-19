package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.mapper.SysConfigMapper;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
* @author liujian
* @description 针对表【sys_config(系统配置表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
    implements SysConfigService{
    private final RedisOperator redisOperator;

    @Override
    public SysConfig getByConfigKey(String configKey) {
        String normalizedKey = normalize(configKey);
        return StringUtils.hasText(normalizedKey) ? baseMapper.selectByConfigKey(normalizedKey) : null;
    }

    @Override
    public String getValueByKey(String configKey) {
        String normalizedKey = normalize(configKey);
        if (!StringUtils.hasText(normalizedKey)) {
            return null;
        }
        String cacheKey = buildCacheKey(normalizedKey);
        Object cached = redisOperator.get(cacheKey);
        if (cached != null) {
            return String.valueOf(cached);
        }
        SysConfig config = getByConfigKey(normalizedKey);
        if (config == null) {
            return null;
        }
        redisOperator.set(cacheKey, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    public String getValueOrDefault(String configKey, String defaultValue) {
        String value = getValueByKey(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void evictConfigCache(String configKey) {
        String normalizedKey = normalize(configKey);
        if (!StringUtils.hasText(normalizedKey)) {
            return;
        }
        redisOperator.delete(buildCacheKey(normalizedKey));
    }

    private String buildCacheKey(String configKey) {
        return RedisKeyUtils.build(ConfigConstants.CACHE_KEY_PREFIX, configKey);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}

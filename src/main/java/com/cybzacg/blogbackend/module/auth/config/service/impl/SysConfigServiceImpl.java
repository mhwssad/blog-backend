package com.cybzacg.blogbackend.module.auth.config.service.impl;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.config.SysConfig;
import com.cybzacg.blogbackend.dto.repository.auth.config.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现。
 *
 * <p>提供基于 Redis 缓存的配置查询、回写与缓存失效处理。
 */
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {
    private final SysConfigRepository sysConfigRepository;
    private final RedisOperator redisOperator;

    /**
     * 根据配置键查询配置实体（不经过缓存）。
     */
    @Override
    public SysConfig getByConfigKey(String configKey) {
        String normalizedKey = StrUtils.normalize(configKey);
        return StrUtils.hasText(normalizedKey) ? sysConfigRepository.findByConfigKey(normalizedKey) : null;
    }

    /**
     * 根据配置键查询配置值，优先从 Redis 缓存读取，未命中时回写缓存。
     */
    @Override
    public String getValueByKey(String configKey) {
        String normalizedKey = StrUtils.normalize(configKey);
        if (!StrUtils.hasText(normalizedKey)) {
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
        redisOperator.set(cacheKey, config.getConfigValue(), ConfigConstants.CACHE_TTL);
        return config.getConfigValue();
    }

    /**
     * 根据配置键查询配置值，不存在时返回默认值。
     */
    @Override
    public String getValueOrDefault(String configKey, String defaultValue) {
        String value = getValueByKey(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 更新配置实体，并在成功后清理新旧键对应的缓存。
     */
    @Override
    public boolean updateConfig(SysConfig config) {
        if (config == null || config.getId() == null) {
            return false;
        }
        SysConfig existing = sysConfigRepository.getById(config.getId());
        boolean updated = sysConfigRepository.updateById(config);
        if (updated) {
            if (existing != null && StrUtils.hasText(existing.getConfigKey())) {
                evictConfigCache(existing.getConfigKey());
            }
            if (StrUtils.hasText(config.getConfigKey())) {
                evictConfigCache(config.getConfigKey());
            }
        }
        return updated;
    }

    /**
     * 清除指定配置键的 Redis 缓存。
     */
    @Override
    public void evictConfigCache(String configKey) {
        String normalizedKey = StrUtils.normalize(configKey);
        if (!StrUtils.hasText(normalizedKey)) {
            return;
        }
        redisOperator.delete(buildCacheKey(normalizedKey));
    }

    private String buildCacheKey(String configKey) {
        return RedisKeyUtils.build(ConfigConstants.CACHE_KEY_PREFIX, configKey);
    }

}

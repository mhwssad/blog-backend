package com.cybzacg.blogbackend.module.auth.config.service;

import com.cybzacg.blogbackend.dto.domain.config.SysConfig;

/**
 * 系统配置服务接口。
 *
 * <p>定义系统配置相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysConfigService {
    /**
     * 根据配置键查询配置实体。
     */
    SysConfig getByConfigKey(String configKey);

    /**
     * 根据配置键查询配置值。
     */
    String getValueByKey(String configKey);

    /**
     * 根据配置键查询配置值，不存在时返回默认值。
     */
    String getValueOrDefault(String configKey, String defaultValue);

    /**
     * 更新配置实体。
     */
    boolean updateConfig(SysConfig config);

    /**
     * 清除指定配置键缓存。
     */
    void evictConfigCache(String configKey);
}

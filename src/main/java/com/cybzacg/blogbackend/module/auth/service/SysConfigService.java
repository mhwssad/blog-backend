package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统配置服务接口。
 *
 * <p>定义系统配置相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysConfigService extends IService<SysConfig> {
    SysConfig getByConfigKey(String configKey);

    String getValueByKey(String configKey);

    String getValueOrDefault(String configKey, String defaultValue);

    void evictConfigCache(String configKey);
}

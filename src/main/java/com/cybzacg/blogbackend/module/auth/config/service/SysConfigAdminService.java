package com.cybzacg.blogbackend.module.auth.config.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigSaveRequest;

/**
 * 系统配置后台管理服务接口。
 *
 * <p>定义系统配置后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysConfigAdminService {
    PageResult<SysConfigAdminVO> pageConfigs(SysConfigPageQuery query);

    SysConfigAdminVO getConfig(Long id);

    SysConfigAdminVO createConfig(SysConfigSaveRequest request);

    SysConfigAdminVO updateConfig(Long id, SysConfigSaveRequest request);

    void deleteConfig(Long id);

    String getValueByKey(String configKey);
}

package com.cybzacg.blogbackend.module.auth.config.convert;

import com.cybzacg.blogbackend.domain.config.SysConfig;
import com.cybzacg.blogbackend.module.auth.config.model.admin.SysConfigAdminVO;
import org.mapstruct.Mapper;

/**
 * 系统配置对象转换器。
 */
@Mapper(componentModel = "spring")
public interface SysConfigModelConvert {
    SysConfigAdminVO toConfigVO(SysConfig config);
}

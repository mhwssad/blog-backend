package com.cybzacg.blogbackend.module.auth.audit.convert;

import com.cybzacg.blogbackend.domain.system.SysLog;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogAdminVO;
import org.mapstruct.Mapper;

/**
 * 系统日志对象转换器。
 */
@Mapper(componentModel = "spring")
public interface SysLogModelConvert {
    SysLogAdminVO toLogVO(SysLog log);
}

package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import org.mapstruct.Mapper;

/** 系统日志对象转换器。 */
@Mapper(componentModel = "spring")
public interface SysLogModelMapper {
    SysLogAdminVO toLogVO(SysLog log);
}

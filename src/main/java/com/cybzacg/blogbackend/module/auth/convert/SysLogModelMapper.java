package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SysLogModelMapper {
    SysLogAdminVO toLogVO(SysLog log);
}

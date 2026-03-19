package com.cybzacg.blogbackend.module.auth.convert;

import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SysConfigModelMapper {
    SysConfigAdminVO toConfigVO(SysConfig config);
}

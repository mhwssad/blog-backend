package com.cybzacg.blogbackend.module.auth.audit.convert;

import com.cybzacg.blogbackend.domain.system.SysAuditLog;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogAdminVO;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 审计日志对象转换器。
 */
@Mapper(componentModel = "spring", imports = SysAuditOperationType.class)
public interface SysAuditLogModelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SysAuditLog toEntity(SysAuditLogCreateRequest request);

    @Mapping(target = "operatorUsername", ignore = true)
    @Mapping(target = "targetUsername", ignore = true)
    @Mapping(target = "operationTypeDesc", expression = "java(SysAuditOperationType.getDescriptionByCode(entity.getOperationType()))")
    SysAuditLogAdminVO toVO(SysAuditLog entity);
}

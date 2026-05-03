package com.cybzacg.blogbackend.module.auth.audit.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审计日志分页查询参数")
public class SysAuditLogPageQuery extends PageQuery {
    @Schema(description = "操作人ID")
    private Long operatorUserId;
    @Schema(description = "目标用户ID")
    private Long targetUserId;
    @Schema(description = "操作类型")
    private String operationType;

}

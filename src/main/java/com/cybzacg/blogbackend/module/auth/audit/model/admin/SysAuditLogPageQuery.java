package com.cybzacg.blogbackend.module.auth.audit.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "审计日志分页查询参数")
public class SysAuditLogPageQuery {
    @Schema(description = "操作人ID")
    private Long operatorUserId;
    @Schema(description = "目标用户ID")
    private Long targetUserId;
    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "当前页码", example = "1")
    private long current = 1;
    @Schema(description = "每页条数", example = "10")
    private long size = 10;
}

package com.cybzacg.blogbackend.module.auth.audit.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "审计日志后台VO")
public class SysAuditLogAdminVO {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "操作人ID")
    private Long operatorUserId;
    @Schema(description = "操作人用户名")
    private String operatorUsername;
    @Schema(description = "目标用户ID")
    private Long targetUserId;
    @Schema(description = "目标用户名")
    private String targetUsername;
    @Schema(description = "操作类型")
    private String operationType;
    @Schema(description = "操作类型描述")
    private String operationTypeDesc;
    @Schema(description = "目标对象类型名称")
    private String targetTypeName;
    @Schema(description = "目标对象ID")
    private Long targetId;
    @Schema(description = "操作前状态")
    private String beforeState;
    @Schema(description = "操作后状态")
    private String afterState;
    @Schema(description = "2FA是否通过")
    private Integer mfaPassed;
    @Schema(description = "请求IP")
    private String requestIp;
    @Schema(description = "User-Agent")
    private String userAgent;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

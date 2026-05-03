package com.cybzacg.blogbackend.module.auth.audit.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统审计日志创建请求。
 */
@Data
@Schema(description = "系统审计日志创建请求")
public class SysAuditLogCreateRequest {
    @Schema(description = "操作用户ID")
    private Long operatorUserId;

    @Schema(description = "目标用户ID")
    private Long targetUserId;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "目标类型名称")
    private String targetTypeName;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "变更前状态")
    private String beforeState;

    @Schema(description = "变更后状态")
    private String afterState;

    @Schema(description = "MFA是否通过：0-未验证，1-已验证")
    private Integer mfaPassed;

    @Schema(description = "请求IP地址")
    private String requestIp;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "备注")
    private String remark;
}

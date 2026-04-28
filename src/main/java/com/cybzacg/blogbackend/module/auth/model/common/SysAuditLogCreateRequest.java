package com.cybzacg.blogbackend.module.auth.model.common;

import lombok.Data;

@Data
public class SysAuditLogCreateRequest {
    private Long operatorUserId;
    private Long targetUserId;
    private String operationType;
    private String targetTypeName;
    private Long targetId;
    private String beforeState;
    private String afterState;
    private Integer mfaPassed;
    private String requestIp;
    private String userAgent;
    private String remark;
}

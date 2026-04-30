package com.cybzacg.blogbackend.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
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
    private LocalDateTime createdAt;
}

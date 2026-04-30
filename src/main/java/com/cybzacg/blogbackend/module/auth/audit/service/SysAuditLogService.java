package com.cybzacg.blogbackend.module.auth.audit.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogAdminVO;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogPageQuery;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;

/**
 * 高风险审计日志服务接口。
 */
public interface SysAuditLogService {
    void record(SysAuditLogCreateRequest request);

    PageResult<SysAuditLogAdminVO> pageLogs(SysAuditLogPageQuery query);

    SysAuditLogAdminVO getLog(Long id);
}

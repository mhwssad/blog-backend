package com.cybzacg.blogbackend.dto.repository.auth.audit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.system.SysAuditLog;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogPageQuery;

/**
 * 高风险审计日志 Repository。
 */
public interface SysAuditLogRepository extends IService<SysAuditLog> {
    /**
     * 根据查询条件分页查询审计日志。
     */
    Page<SysAuditLog> pageByConditions(SysAuditLogPageQuery query);
}

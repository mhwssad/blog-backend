package com.cybzacg.blogbackend.module.auth.audit.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.system.SysAuditLog;
import com.cybzacg.blogbackend.mapper.system.SysAuditLogMapper;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogPageQuery;
import com.cybzacg.blogbackend.module.auth.audit.repository.SysAuditLogRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SysAuditLogRepositoryImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements SysAuditLogRepository {

    @Override
    public Page<SysAuditLog> pageByConditions(SysAuditLogPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<SysAuditLog>()
                        .eq(query.getOperatorUserId() != null, SysAuditLog::getOperatorUserId, query.getOperatorUserId())
                        .eq(query.getTargetUserId() != null, SysAuditLog::getTargetUserId, query.getTargetUserId())
                        .eq(StringUtils.hasText(query.getOperationType()), SysAuditLog::getOperationType, query.getOperationType())
                        .orderByDesc(SysAuditLog::getCreatedAt));
    }
}

package com.cybzacg.blogbackend.dto.repository.auth.audit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.system.SysAuditLog;
import com.cybzacg.blogbackend.dto.mapper.system.SysAuditLogMapper;
import com.cybzacg.blogbackend.dto.repository.auth.audit.SysAuditLogRepository;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

@Repository
public class SysAuditLogRepositoryImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog>
        implements SysAuditLogRepository {

    @Override
    public Page<SysAuditLog> pageByConditions(SysAuditLogPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<SysAuditLog>()
                        .eq(query.getOperatorUserId() != null, SysAuditLog::getOperatorUserId, query.getOperatorUserId())
                        .eq(query.getTargetUserId() != null, SysAuditLog::getTargetUserId, query.getTargetUserId())
                        .eq(StrUtils.hasText(query.getOperationType()), SysAuditLog::getOperationType, query.getOperationType())
                        .orderByDesc(SysAuditLog::getCreatedAt));
    }
}

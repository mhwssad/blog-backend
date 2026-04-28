package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysAuditLog;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.convert.SysAuditLogModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuditLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuditLogPageQuery;
import com.cybzacg.blogbackend.module.auth.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysAuditLogRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高风险审计日志服务实现。
 *
 * <p>负责审计日志的记录、分页查询以及详情查询，并批量加载操作人与目标用户的用户名。
 */
@Service
@RequiredArgsConstructor
public class SysAuditLogServiceImpl implements SysAuditLogService {
    private final SysAuditLogRepository sysAuditLogRepository;
    private final SysUserRepository sysUserRepository;
    private final SysAuditLogModelMapper sysAuditLogModelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(SysAuditLogCreateRequest request) {
        SysAuditLog entity = sysAuditLogModelMapper.toEntity(request);
        sysAuditLogRepository.save(entity);
    }

    @Override
    public PageResult<SysAuditLogAdminVO> pageLogs(SysAuditLogPageQuery query) {
        Page<SysAuditLog> page = sysAuditLogRepository.pageByConditions(query);
        Map<Long, String> usernameMap = buildUsernameMap(page.getRecords());
        List<SysAuditLogAdminVO> records = page.getRecords().stream()
                .map(entity -> {
                    SysAuditLogAdminVO vo = sysAuditLogModelMapper.toVO(entity);
                    vo.setOperatorUsername(usernameMap.get(entity.getOperatorUserId()));
                    vo.setTargetUsername(usernameMap.get(entity.getTargetUserId()));
                    return vo;
                })
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysAuditLogAdminVO getLog(Long id) {
        SysAuditLog entity = sysAuditLogRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(entity, ResultErrorCode.AUDIT_LOG_NOT_FOUND);
        Map<Long, String> usernameMap = buildUsernameMap(List.of(entity));
        SysAuditLogAdminVO vo = sysAuditLogModelMapper.toVO(entity);
        vo.setOperatorUsername(usernameMap.get(entity.getOperatorUserId()));
        vo.setTargetUsername(usernameMap.get(entity.getTargetUserId()));
        return vo;
    }

    private Map<Long, String> buildUsernameMap(List<SysAuditLog> logs) {
        Set<Long> userIds = new HashSet<>();
        for (SysAuditLog log : logs) {
            if (log.getOperatorUserId() != null) {
                userIds.add(log.getOperatorUserId());
            }
            if (log.getTargetUserId() != null) {
                userIds.add(log.getTargetUserId());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (a, b) -> a));
    }
}

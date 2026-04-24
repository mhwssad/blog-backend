package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.convert.SysLogModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysLogRepository;
import com.cybzacg.blogbackend.module.auth.service.SysLogAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统日志后台管理服务实现。
 *
 * <p>负责日志分页查询、详情查看、单条删除和按条件批量清理。
 */
@Service
@RequiredArgsConstructor
public class SysLogAdminServiceImpl implements SysLogAdminService {
    private final SysLogRepository sysLogRepository;
    private final SysLogModelMapper sysLogModelMapper;

    @Override
    public PageResult<SysLogAdminVO> pageLogs(SysLogPageQuery query) {
        var page = sysLogRepository.pageByAdminConditions(query);
        List<SysLogAdminVO> records = page.getRecords().stream()
                .map(sysLogModelMapper::toLogVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysLogAdminVO getLog(Long id) {
        return sysLogModelMapper.toLogVO(getLogOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        getLogOrThrow(id);
        sysLogRepository.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanLogs(SysLogCleanRequest request) {
        ExceptionThrowerCore.throwBusinessIfNot(hasAnyCondition(request), ResultErrorCode.ILLEGAL_ARGUMENT, "清理日志必须至少指定一个条件");
        return sysLogRepository.removeByConditions(request);
    }

    private boolean hasAnyCondition(SysLogCleanRequest request) {
        return org.springframework.util.StringUtils.hasText(request.getModule())
                || org.springframework.util.StringUtils.hasText(request.getRequestMethod())
                || org.springframework.util.StringUtils.hasText(request.getRequestUri())
                || org.springframework.util.StringUtils.hasText(request.getIp())
                || request.getCreateBy() != null
                || request.getCreateTimeStart() != null
                || request.getCreateTimeEnd() != null;
    }

    private SysLog getLogOrThrow(Long id) {
        SysLog log = sysLogRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(log, ResultErrorCode.ILLEGAL_ARGUMENT, "日志不存在");
        return log;
    }
}

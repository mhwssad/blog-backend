package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysLogModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.service.SysLogAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysLogAdminServiceImpl implements SysLogAdminService {
    private final SysLogService sysLogService;
    private final SysLogModelMapper sysLogModelMapper;

    @Override
    public PageResult<SysLogAdminVO> pageLogs(SysLogPageQuery query) {
        Page<SysLog> page = sysLogService.lambdaQuery()
                .like(StringUtils.hasText(query.getModule()), SysLog::getModule, query.getModule())
                .like(StringUtils.hasText(query.getRequestMethod()), SysLog::getRequestMethod, query.getRequestMethod())
                .like(StringUtils.hasText(query.getRequestUri()), SysLog::getRequestUri, query.getRequestUri())
                .like(StringUtils.hasText(query.getIp()), SysLog::getIp, query.getIp())
                .eq(query.getCreateBy() != null, SysLog::getCreateBy, query.getCreateBy())
                .ge(query.getCreateTimeStart() != null, SysLog::getCreateTime, query.getCreateTimeStart())
                .le(query.getCreateTimeEnd() != null, SysLog::getCreateTime, query.getCreateTimeEnd())
                .orderByDesc(SysLog::getCreateTime)
                .orderByDesc(SysLog::getId)
                .page(new Page<>(query.getCurrent(), query.getSize()));

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
        sysLogService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanLogs(SysLogCleanRequest request) {
        if (!hasAnyCondition(request)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "清理日志必须至少指定一个条件");
        }
        long count = sysLogService.lambdaQuery()
                .like(StringUtils.hasText(request.getModule()), SysLog::getModule, request.getModule())
                .like(StringUtils.hasText(request.getRequestMethod()), SysLog::getRequestMethod, request.getRequestMethod())
                .like(StringUtils.hasText(request.getRequestUri()), SysLog::getRequestUri, request.getRequestUri())
                .like(StringUtils.hasText(request.getIp()), SysLog::getIp, request.getIp())
                .eq(request.getCreateBy() != null, SysLog::getCreateBy, request.getCreateBy())
                .ge(request.getCreateTimeStart() != null, SysLog::getCreateTime, request.getCreateTimeStart())
                .le(request.getCreateTimeEnd() != null, SysLog::getCreateTime, request.getCreateTimeEnd())
                .count();
        if (count == 0) {
            return 0L;
        }
        sysLogService.lambdaUpdate()
                .like(StringUtils.hasText(request.getModule()), SysLog::getModule, request.getModule())
                .like(StringUtils.hasText(request.getRequestMethod()), SysLog::getRequestMethod, request.getRequestMethod())
                .like(StringUtils.hasText(request.getRequestUri()), SysLog::getRequestUri, request.getRequestUri())
                .like(StringUtils.hasText(request.getIp()), SysLog::getIp, request.getIp())
                .eq(request.getCreateBy() != null, SysLog::getCreateBy, request.getCreateBy())
                .ge(request.getCreateTimeStart() != null, SysLog::getCreateTime, request.getCreateTimeStart())
                .le(request.getCreateTimeEnd() != null, SysLog::getCreateTime, request.getCreateTimeEnd())
                .remove();
        return count;
    }

    private boolean hasAnyCondition(SysLogCleanRequest request) {
        return StringUtils.hasText(request.getModule())
                || StringUtils.hasText(request.getRequestMethod())
                || StringUtils.hasText(request.getRequestUri())
                || StringUtils.hasText(request.getIp())
                || request.getCreateBy() != null
                || request.getCreateTimeStart() != null
                || request.getCreateTimeEnd() != null;
    }

    private SysLog getLogOrThrow(Long id) {
        SysLog log = sysLogService.getById(id);
        if (log == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "日志不存在");
        }
        return log;
    }
}

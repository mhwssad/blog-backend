package com.cybzacg.blogbackend.module.auth.audit.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.system.SysLog;
import com.cybzacg.blogbackend.dto.repository.auth.audit.SysLogRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.audit.convert.SysLogModelConvert;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.audit.service.SysLogAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统日志后台管理服务实现。
 *
 * <p>
 * 负责日志分页查询、详情查看、单条删除和按条件批量清理。
 */
@Service
@RequiredArgsConstructor
public class SysLogAdminServiceImpl implements SysLogAdminService {
    private final SysLogRepository sysLogRepository;
    private final SysLogModelConvert sysLogModelConvert;

    /**
     * 分页查询系统日志列表。
     */
    @Override
    public PageResult<SysLogAdminVO> pageLogs(SysLogPageQuery query) {
        var page = sysLogRepository.pageByAdminConditions(query);
        List<SysLogAdminVO> records = page.getRecords().stream()
                .map(sysLogModelConvert::toLogVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 根据 ID 获取日志详情。
     */
    @Override
    public SysLogAdminVO getLog(Long id) {
        return sysLogModelConvert.toLogVO(getLogOrThrow(id));
    }

    /**
     * 物理删除单条日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        getLogOrThrow(id);
        sysLogRepository.removeById(id);
    }

    /**
     * 按条件批量清理日志，至少需指定一个筛选条件。
     *
     * @param request 清理条件（模块、请求方法、URI、IP、时间范围等）
     * @return 实际删除的记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanLogs(SysLogCleanRequest request) {
        ExceptionThrowerCore.throwBusinessIfNot(hasAnyCondition(request), ResultErrorCode.ILLEGAL_ARGUMENT,
                "清理日志必须至少指定一个条件");
        return sysLogRepository.removeByConditions(request);
    }

    private boolean hasAnyCondition(SysLogCleanRequest request) {
        return StrUtils.hasText(request.getModule())
                || StrUtils.hasText(request.getRequestMethod())
                || StrUtils.hasText(request.getRequestUri())
                || StrUtils.hasText(request.getIp())
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

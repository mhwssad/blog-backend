package com.cybzacg.blogbackend.module.auth.audit.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.system.SysLog;
import com.cybzacg.blogbackend.mapper.system.SysLogMapper;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.audit.repository.SysLogRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统日志 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysLogRepositoryImpl extends ServiceImpl<SysLogMapper, SysLog>
        implements SysLogRepository {

    /**
     * 根据管理端查询条件进行分页，按创建时间降序排列。
     */
    @Override
    public Page<SysLog> pageByAdminConditions(SysLogPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), buildConditionWrapper(
                query.getModule(),
                query.getRequestMethod(),
                query.getRequestUri(),
                query.getIp(),
                query.getCreateBy(),
                query.getCreateTimeStart(),
                query.getCreateTimeEnd())
                .orderByDesc(SysLog::getCreateTime)
                .orderByDesc(SysLog::getId));
    }

    /**
     * 根据清理条件统计匹配的日志数量。
     */
    @Override
    public long countByConditions(SysLogCleanRequest request) {
        return count(buildConditionWrapper(
                request.getModule(),
                request.getRequestMethod(),
                request.getRequestUri(),
                request.getIp(),
                request.getCreateBy(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd()));
    }

    /**
     * 根据清理条件删除匹配的日志，先统计数量再执行删除。
     */
    @Override
    public long removeByConditions(SysLogCleanRequest request) {
        LambdaQueryWrapper<SysLog> queryWrapper = buildConditionWrapper(
                request.getModule(),
                request.getRequestMethod(),
                request.getRequestUri(),
                request.getIp(),
                request.getCreateBy(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd());
        long count = count(queryWrapper);
        if (count == 0L) {
            return 0L;
        }
        remove(queryWrapper);
        return count;
    }

    /**
     * 构建通用的日志条件查询包装器，按模块、请求方法、URI、IP、操作人和时间范围进行过滤。
     */
    private LambdaQueryWrapper<SysLog> buildConditionWrapper(String module,
                                                             String requestMethod,
                                                             String requestUri,
                                                             String ip,
                                                             Long createBy,
                                                             LocalDateTime createTimeStart,
                                                             LocalDateTime createTimeEnd) {
        return new LambdaQueryWrapper<SysLog>()
                .like(StringUtils.hasText(module), SysLog::getModule, module)
                .like(StringUtils.hasText(requestMethod), SysLog::getRequestMethod, requestMethod)
                .like(StringUtils.hasText(requestUri), SysLog::getRequestUri, requestUri)
                .like(StringUtils.hasText(ip), SysLog::getIp, ip)
                .eq(createBy != null, SysLog::getCreateBy, createBy)
                .ge(createTimeStart != null, SysLog::getCreateTime, createTimeStart)
                .le(createTimeEnd != null, SysLog::getCreateTime, createTimeEnd);
    }
}

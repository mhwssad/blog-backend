package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.mapper.SysLogMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysLogRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 系统日志 Repository 实现。
 */
@Repository
public class SysLogRepositoryImpl extends ServiceImpl<SysLogMapper, SysLog>
        implements SysLogRepository {

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

    private LambdaQueryWrapper<SysLog> buildConditionWrapper(String module,
                                                             String requestMethod,
                                                             String requestUri,
                                                             String ip,
                                                             Long createBy,
                                                             Date createTimeStart,
                                                             Date createTimeEnd) {
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

package com.cybzacg.blogbackend.module.report.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysReportRecord;
import com.cybzacg.blogbackend.mapper.SysReportRecordMapper;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * SysReportRecord Repository 实现。
 */
@Repository
public class SysReportRecordRepositoryImpl extends ServiceImpl<SysReportRecordMapper, SysReportRecord>
        implements SysReportRecordRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysReportRecord> pageByFilters(Integer status,
                                               String reportTargetType,
                                               Long reporterUserId,
                                               LocalDateTime reportedStart,
                                               LocalDateTime reportedEnd,
                                               long current,
                                               long size) {
        LambdaQueryWrapper<SysReportRecord> wrapper = new LambdaQueryWrapper<SysReportRecord>()
                .eq(status != null, SysReportRecord::getStatus, status)
                .eq(reportTargetType != null && !reportTargetType.isBlank(), SysReportRecord::getReportTargetType, reportTargetType)
                .eq(reporterUserId != null, SysReportRecord::getReporterUserId, reporterUserId)
                .ge(reportedStart != null, SysReportRecord::getReportedAt, reportedStart)
                .le(reportedEnd != null, SysReportRecord::getReportedAt, reportedEnd)
                .orderByDesc(SysReportRecord::getReportedAt)
                .orderByDesc(SysReportRecord::getId);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    public boolean existsByReporterAndTarget(Long reporterUserId, String reportTargetType, Long reportTargetId) {
        return count(new LambdaQueryWrapper<SysReportRecord>()
                .eq(SysReportRecord::getReporterUserId, reporterUserId)
                .eq(SysReportRecord::getReportTargetType, reportTargetType)
                .eq(SysReportRecord::getReportTargetId, reportTargetId)
                .ge(SysReportRecord::getReportedAt, LocalDateTime.now().minusHours(24))) > 0;
    }
}

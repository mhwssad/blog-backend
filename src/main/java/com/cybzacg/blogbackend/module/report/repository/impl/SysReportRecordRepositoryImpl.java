package com.cybzacg.blogbackend.module.report.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.mapper.report.SysReportRecordMapper;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

/**
 * SysReportRecord Repository 实现。
 */
@Repository
public class SysReportRecordRepositoryImpl
    extends ServiceImpl<SysReportRecordMapper, SysReportRecord>
    implements SysReportRecordRepository
{

    /**
     * 根据多条件分页查询举报记录。
     *
     * @param status          举报状态筛选（可选）
     * @param reportTargetType 举报对象类型筛选（可选，非空时生效）
     * @param reporterUserId  举报人用户ID筛选（可选）
     * @param reportedStart   举报时间范围起始（可选）
     * @param reportedEnd     举报时间范围截止（可选）
     * @param current         当前页码（从1开始）
     * @param size            每页记录数
     * @return 分页后的举报记录列表
     */
    @Override
    public Page<SysReportRecord> pageByFilters(
        Integer status,
        String reportTargetType,
        Long reporterUserId,
        LocalDateTime reportedStart,
        LocalDateTime reportedEnd,
        long current,
        long size
    ) {
        LambdaQueryWrapper<SysReportRecord> wrapper = new LambdaQueryWrapper<
            SysReportRecord
        >()
            .eq(status != null, SysReportRecord::getStatus, status)
            .eq(
                reportTargetType != null && !reportTargetType.isBlank(),
                SysReportRecord::getReportTargetType,
                reportTargetType
            )
            .eq(
                reporterUserId != null,
                SysReportRecord::getReporterUserId,
                reporterUserId
            )
            .ge(
                reportedStart != null,
                SysReportRecord::getReportedAt,
                reportedStart
            )
            .le(
                reportedEnd != null,
                SysReportRecord::getReportedAt,
                reportedEnd
            )
            .orderByDesc(SysReportRecord::getReportedAt)
            .orderByDesc(SysReportRecord::getId);
        return page(new Page<>(current, size), wrapper);
    }

    /**
     * 检查举报人在过去24小时内是否对同一目标提交过举报。
     * 用于防止重复举报。
     *
     * @param reporterUserId   举报人用户ID
     * @param reportTargetType 举报对象类型
     * @param reportTargetId   举报对象ID
     * @return 24小时内是否存在相同目标的举报记录
     */
    @Override
    public boolean existsByReporterAndTarget(
        Long reporterUserId,
        String reportTargetType,
        Long reportTargetId
    ) {
        return (
            count(
                new LambdaQueryWrapper<SysReportRecord>()
                    .eq(SysReportRecord::getReporterUserId, reporterUserId)
                    .eq(SysReportRecord::getReportTargetType, reportTargetType)
                    .eq(SysReportRecord::getReportTargetId, reportTargetId)
                    .ge(
                        SysReportRecord::getReportedAt,
                        LocalDateTime.now().minusHours(24)
                    )
            ) >
            0
        );
    }
}

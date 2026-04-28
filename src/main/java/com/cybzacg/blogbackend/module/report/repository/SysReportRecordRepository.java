package com.cybzacg.blogbackend.module.report.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysReportRecord;

import java.time.LocalDateTime;

/**
 * SysReportRecord Repository。
 */
public interface SysReportRecordRepository extends IService<SysReportRecord> {

    /**
     * 按状态、对象类型、举报人和时间范围分页查询举报单。
     */
    Page<SysReportRecord> pageByFilters(Integer status,
                                        String reportTargetType,
                                        Long reporterUserId,
                                        LocalDateTime reportedStart,
                                        LocalDateTime reportedEnd,
                                        long current,
                                        long size);
}

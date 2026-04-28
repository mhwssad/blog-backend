package com.cybzacg.blogbackend.module.report.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysReportRecord;
import com.cybzacg.blogbackend.mapper.SysReportRecordMapper;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * SysReportRecord Repository 实现。
 */
@Repository
public class SysReportRecordRepositoryImpl extends ServiceImpl<SysReportRecordMapper, SysReportRecord>
        implements SysReportRecordRepository {
}

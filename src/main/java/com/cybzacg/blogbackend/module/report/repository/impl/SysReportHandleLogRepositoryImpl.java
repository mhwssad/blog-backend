package com.cybzacg.blogbackend.module.report.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysReportHandleLog;
import com.cybzacg.blogbackend.mapper.SysReportHandleLogMapper;
import com.cybzacg.blogbackend.module.report.repository.SysReportHandleLogRepository;
import org.springframework.stereotype.Repository;

/**
 * SysReportHandleLog Repository 实现。
 */
@Repository
public class SysReportHandleLogRepositoryImpl extends ServiceImpl<SysReportHandleLogMapper, SysReportHandleLog>
        implements SysReportHandleLogRepository {
}

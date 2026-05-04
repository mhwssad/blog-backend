package com.cybzacg.blogbackend.module.report.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.report.SysReportHandleLog;
import com.cybzacg.blogbackend.mapper.report.SysReportHandleLogMapper;
import com.cybzacg.blogbackend.module.report.repository.SysReportHandleLogRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * SysReportHandleLog Repository 实现。
 */
@Repository
public class SysReportHandleLogRepositoryImpl
    extends ServiceImpl<SysReportHandleLogMapper, SysReportHandleLog>
    implements SysReportHandleLogRepository
{

    /**
     * 根据举报记录ID查询其关联的处理日志列表。
     *
     * @param reportId 举报记录ID
     * @return 按创建时间倒序排列的处理日志列表
     */
    @Override
    public List<SysReportHandleLog> listByReportId(Long reportId) {
        return list(
            new LambdaQueryWrapper<SysReportHandleLog>()
                .eq(SysReportHandleLog::getReportId, reportId)
                .orderByDesc(SysReportHandleLog::getCreatedAt)
                .orderByDesc(SysReportHandleLog::getId)
        );
    }
}

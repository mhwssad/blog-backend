package com.cybzacg.blogbackend.dto.repository.report;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.report.SysReportHandleLog;

import java.util.List;

/**
 * SysReportHandleLog Repository。
 */
public interface SysReportHandleLogRepository extends IService<SysReportHandleLog> {

    /**
     * 按举报单读取处理日志。
     */
    List<SysReportHandleLog> listByReportId(Long reportId);
}

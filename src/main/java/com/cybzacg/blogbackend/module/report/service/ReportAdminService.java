package com.cybzacg.blogbackend.module.report.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import java.util.List;

/**
 * 后台举报管理服务接口。
 */
public interface ReportAdminService {
    PageResult<ReportAdminVO> pageReports(ReportAdminPageQuery query);

    ReportAdminVO getReportDetail(Long reportId);

    void claimReport(Long reportId, Long operatorId);

    void handleReport(
        Long reportId,
        Long operatorId,
        ReportHandleRequest request,
        String ip,
        String ua
    );

    void rejectReport(
        Long reportId,
        Long operatorId,
        String remark,
        String ip,
        String ua
    );

    void overrideClaim(Long reportId, Long operatorId, String ip, String ua);

    List<ReportHandleLogVO> listHandleLogs(Long reportId);
}

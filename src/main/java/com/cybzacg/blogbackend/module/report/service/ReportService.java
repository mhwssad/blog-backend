package com.cybzacg.blogbackend.module.report.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;

/**
 * 用户侧举报服务接口。
 */
public interface ReportService {

    ReportVO submitReport(Long userId, ReportCreateRequest request);

    PageResult<ReportVO> listMyReports(Long userId, String targetType, long current, long size);

    ReportVO getMyReport(Long userId, Long reportId);
}

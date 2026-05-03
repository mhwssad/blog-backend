package com.cybzacg.blogbackend.module.report.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.report.ReportRecordStatusEnum;
import com.cybzacg.blogbackend.enums.report.ReportTargetTypeEnum;
import com.cybzacg.blogbackend.module.report.convert.ReportModelConvert;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import com.cybzacg.blogbackend.module.report.service.ReportService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧举报服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SysReportRecordRepository sysReportRecordRepository;
    private final ReportModelConvert reportModelConvert;

    @Override
    public ReportVO submitReport(Long userId, ReportCreateRequest request) {
        ExceptionThrowerCore.throwBusinessIfNot(
                ReportTargetTypeEnum.contains(request.getTargetType()),
                ResultErrorCode.REPORT_TARGET_TYPE_INVALID);

        // 重复举报频率限制：同一用户同一对象24小时内只能举报一次
        boolean exists = sysReportRecordRepository.existsByReporterAndTarget(
                userId, request.getTargetType(), request.getTargetId());
        ExceptionThrowerCore.throwBusinessIf(exists, ResultErrorCode.REPORT_DUPLICATE_RATE_LIMITED);

        SysReportRecord record = reportModelConvert.toRecord(request);
        record.setReporterUserId(userId);
        record.setStatus(ReportRecordStatusEnum.PENDING.getValue());
        record.setReportedAt(LocalDateTime.now());
        sysReportRecordRepository.save(record);

        return reportModelConvert.toUserVO(record);
    }

    @Override
    public PageResult<ReportVO> listMyReports(Long userId, String targetType, long current, long size) {
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 10L, 50L);

        Page<SysReportRecord> page = sysReportRecordRepository.pageByFilters(
                null, targetType, userId, null, null, current, size);
        List<ReportVO> records = page.getRecords().stream()
                .map(reportModelConvert::toUserVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public ReportVO getMyReport(Long userId, Long reportId) {
        SysReportRecord record = sysReportRecordRepository.getById(reportId);
        ExceptionThrowerCore.throwBusinessIfNull(record, ResultErrorCode.REPORT_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIfNot(
                record.getReporterUserId().equals(userId), ResultErrorCode.REPORT_NOT_FOUND);
        return reportModelConvert.toUserVO(record);
    }
}

package com.cybzacg.blogbackend.module.report;

import com.cybzacg.blogbackend.dto.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.report.convert.ReportModelConvert;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;
import com.cybzacg.blogbackend.dto.repository.report.SysReportRecordRepository;
import com.cybzacg.blogbackend.module.report.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户侧举报服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private SysReportRecordRepository sysReportRecordRepository;
    @Mock
    private ReportModelConvert reportModelConvert;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(sysReportRecordRepository, reportModelConvert);
    }

    // ==================== submitReport ====================

    @Test
    void submitReportShouldCreateRecordForArticle() {
        ReportCreateRequest request = buildCreateRequest("article", 100L, "spam", "广告内容");
        SysReportRecord record = new SysReportRecord();
        ReportVO vo = buildReportVO(1L, "article", 100L, "spam", 0);

        when(sysReportRecordRepository.existsByReporterAndTarget(1L, "article", 100L)).thenReturn(false);
        when(reportModelConvert.toRecord(request)).thenReturn(record);
        when(sysReportRecordRepository.save(record)).thenReturn(true);
        when(reportModelConvert.toUserVO(record)).thenReturn(vo);

        ReportVO result = reportService.submitReport(1L, request);

        assertSame(vo, result);
        assertEquals(1L, record.getReporterUserId());
        assertEquals(0, record.getStatus());
        assertNotNull(record.getReportedAt());
        verify(sysReportRecordRepository).save(record);
    }

    @Test
    void submitReportShouldCreateRecordForComment() {
        ReportCreateRequest request = buildCreateRequest("comment", 200L, "abuse", "辱骂");
        SysReportRecord record = new SysReportRecord();
        ReportVO vo = buildReportVO(2L, "comment", 200L, "abuse", 0);

        when(sysReportRecordRepository.existsByReporterAndTarget(1L, "comment", 200L)).thenReturn(false);
        when(reportModelConvert.toRecord(request)).thenReturn(record);
        when(sysReportRecordRepository.save(record)).thenReturn(true);
        when(reportModelConvert.toUserVO(record)).thenReturn(vo);

        ReportVO result = reportService.submitReport(1L, request);

        assertSame(vo, result);
        assertEquals(1L, record.getReporterUserId());
        assertEquals(0, record.getStatus());
        assertNotNull(record.getReportedAt());
        verify(sysReportRecordRepository).save(record);
    }

    @Test
    void submitReportShouldCreateRecordForChatMessage() {
        ReportCreateRequest request = buildCreateRequest("chat_message", 300L, "porn", "色情内容");
        SysReportRecord record = new SysReportRecord();
        ReportVO vo = buildReportVO(3L, "chat_message", 300L, "porn", 0);

        when(sysReportRecordRepository.existsByReporterAndTarget(1L, "chat_message", 300L)).thenReturn(false);
        when(reportModelConvert.toRecord(request)).thenReturn(record);
        when(sysReportRecordRepository.save(record)).thenReturn(true);
        when(reportModelConvert.toUserVO(record)).thenReturn(vo);

        ReportVO result = reportService.submitReport(1L, request);

        assertSame(vo, result);
        assertEquals(1L, record.getReporterUserId());
        assertEquals(0, record.getStatus());
        assertNotNull(record.getReportedAt());
        verify(sysReportRecordRepository).save(record);
    }

    @Test
    void submitReportShouldRejectDuplicateRateLimited() {
        ReportCreateRequest request = buildCreateRequest("article", 100L, "spam", "广告");

        when(sysReportRecordRepository.existsByReporterAndTarget(1L, "article", 100L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportService.submitReport(1L, request));

        assertEquals(ResultErrorCode.REPORT_DUPLICATE_RATE_LIMITED.getCode(), exception.getCode());
        verify(reportModelConvert, never()).toRecord(any());
        verify(sysReportRecordRepository, never()).save(any());
    }

    // ==================== helper fixtures ====================

    private ReportCreateRequest buildCreateRequest(String targetType, Long targetId,
                                                    String reasonCode, String reasonDetail) {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        request.setReasonCode(reasonCode);
        request.setReasonDetail(reasonDetail);
        return request;
    }

    private ReportVO buildReportVO(Long id, String targetType, Long targetId,
                                   String reasonCode, Integer status) {
        ReportVO vo = new ReportVO();
        vo.setId(id);
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        vo.setReasonCode(reasonCode);
        vo.setStatus(status);
        return vo;
    }
}

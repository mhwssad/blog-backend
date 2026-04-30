package com.cybzacg.blogbackend.module.report;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.report.SysReportHandleLog;
import com.cybzacg.blogbackend.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminService;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.comment.service.CommentAdminService;
import com.cybzacg.blogbackend.module.report.convert.ReportModelMapper;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.repository.SysReportHandleLogRepository;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import com.cybzacg.blogbackend.module.report.service.impl.ReportAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 后台举报管理服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ReportAdminServiceImplTest {

    @Mock
    private SysReportRecordRepository sysReportRecordRepository;
    @Mock
    private SysReportHandleLogRepository sysReportHandleLogRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysAuditLogService sysAuditLogService;
    @Mock
    private SuperAdminVerifier superAdminVerifier;
    @Mock
    private ReportModelMapper reportModelMapper;
    @Mock
    private ArticleAdminService articleAdminService;
    @Mock
    private CommentAdminService commentAdminService;
    @Mock
    private ChatAdminService chatAdminService;
    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private SysCommentRepository sysCommentRepository;
    @Mock
    private SysUserAdminService sysUserAdminService;

    private ReportAdminServiceImpl reportAdminService;

    @BeforeEach
    void setUp() {
        reportAdminService = new ReportAdminServiceImpl(
                sysReportRecordRepository,
                sysReportHandleLogRepository,
                sysUserRepository,
                sysAuditLogService,
                superAdminVerifier,
                reportModelMapper,
                articleAdminService,
                commentAdminService,
                chatAdminService,
                blogArticleRepository,
                sysCommentRepository,
                sysUserAdminService
        );
    }

    // ==================== claimReport ====================

    @Test
    void claimReportShouldChangeStatusToProcessing() {
        SysReportRecord record = buildPendingRecord(1L, "article", 100L);
        when(sysReportRecordRepository.getById(1L)).thenReturn(record);
        when(sysReportRecordRepository.updateById(any())).thenReturn(true);
        when(sysReportHandleLogRepository.save(any())).thenReturn(true);

        reportAdminService.claimReport(1L, 99L);

        assertEquals(1, record.getStatus()); // PROCESSING
        assertEquals(99L, record.getHandlerUserId());
        verify(sysReportRecordRepository).updateById(record);
        verify(sysReportHandleLogRepository).save(any(SysReportHandleLog.class));
    }

    @Test
    void claimReportShouldRejectAlreadyClaimed() {
        SysReportRecord record = buildRecordWithStatus(1L, 1); // PROCESSING
        when(sysReportRecordRepository.getById(1L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportAdminService.claimReport(1L, 99L));

        assertEquals(ResultErrorCode.REPORT_ALREADY_HANDLED.getCode(), exception.getCode());
        verify(sysReportRecordRepository, never()).updateById(any());
    }

    // ==================== handleReport ====================

    @Test
    void handleReportShouldExecuteDeleteContentAction() {
        SysReportRecord record = buildRecordWithStatus(1L, 0); // PENDING, article type
        record.setReportTargetType("article");
        record.setReportTargetId(100L);
        ReportHandleRequest request = buildHandleRequest("delete_content", "content_delete", "违规删除");

        when(sysReportRecordRepository.getById(1L)).thenReturn(record);
        when(sysReportRecordRepository.updateById(any())).thenReturn(true);
        when(sysReportHandleLogRepository.save(any())).thenReturn(true);

        reportAdminService.handleReport(1L, 99L, request, "127.0.0.1", "test-ua");

        assertEquals(2, record.getStatus()); // HANDLED
        assertEquals("delete_content", record.getResultType());
        assertNotNull(record.getHandledAt());
        verify(sysReportRecordRepository).updateById(record);
        verify(articleAdminService).deleteArticle(100L);
        verify(sysAuditLogService).record(any());
    }

    @Test
    void handleReportShouldRejectTerminalStateReport() {
        SysReportRecord record = buildRecordWithStatus(1L, 2); // already HANDLED
        when(sysReportRecordRepository.getById(1L)).thenReturn(record);
        ReportHandleRequest request = buildHandleRequest("record_only", "none", null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportAdminService.handleReport(1L, 99L, request, "127.0.0.1", "test-ua"));

        assertEquals(ResultErrorCode.REPORT_ALREADY_HANDLED.getCode(), exception.getCode());
        verify(sysReportRecordRepository, never()).updateById(any());
    }

    @Test
    void handleReportShouldBanUserForBanAction() {
        SysReportRecord record = buildRecordWithStatus(1L, 0); // PENDING
        record.setReportTargetType("article");
        record.setReportTargetId(100L);

        BlogArticle article = new BlogArticle();
        article.setId(100L);
        article.setAuthorId(50L);

        ReportHandleRequest request = buildHandleRequest("ban_user", "ban", "封禁违规用户");

        when(sysReportRecordRepository.getById(1L)).thenReturn(record);
        when(sysReportRecordRepository.updateById(any())).thenReturn(true);
        when(sysReportHandleLogRepository.save(any())).thenReturn(true);
        when(blogArticleRepository.getById(100L)).thenReturn(article);

        reportAdminService.handleReport(1L, 99L, request, "127.0.0.1", "test-ua");

        assertEquals(2, record.getStatus()); // HANDLED
        verify(sysUserAdminService).banUserByReport(99L, 50L, "封禁违规用户", "127.0.0.1", "test-ua");
        verify(sysAuditLogService).record(any());
    }

    // ==================== rejectReport ====================

    @Test
    void rejectReportShouldSetRejectedStatus() {
        SysReportRecord record = buildRecordWithStatus(1L, 0); // PENDING
        when(sysReportRecordRepository.getById(1L)).thenReturn(record);
        when(sysReportRecordRepository.updateById(any())).thenReturn(true);
        when(sysReportHandleLogRepository.save(any())).thenReturn(true);

        reportAdminService.rejectReport(1L, 99L, "证据不足", "127.0.0.1", "test-ua");

        assertEquals(3, record.getStatus()); // REJECTED
        assertEquals(99L, record.getHandlerUserId());
        assertEquals("证据不足", record.getRemark());
        assertNotNull(record.getHandledAt());
        verify(sysReportRecordRepository).updateById(record);
    }

    // ==================== overrideClaim ====================

    @Test
    void overrideClaimShouldRequireSuperAdmin() {
        Long operatorId = 99L;
        doThrow(new BusinessException(ResultErrorCode.NOT_SUPER_ADMIN))
                .when(superAdminVerifier).requireSuperAdmin(operatorId);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportAdminService.overrideClaim(1L, operatorId, "127.0.0.1", "test-ua"));

        assertEquals(ResultErrorCode.NOT_SUPER_ADMIN.getCode(), exception.getCode());
        verify(sysReportRecordRepository, never()).getById(anyLong());
    }

    // ==================== listHandleLogs ====================

    @Test
    void listHandleLogsShouldReturnOrderedLogs() {
        SysReportRecord record = buildPendingRecord(1L, "article", 100L);
        when(sysReportRecordRepository.getById(1L)).thenReturn(record);

        SysReportHandleLog log1 = buildHandleLog(1L, 0, 1, "claim", null, 99L, null);
        SysReportHandleLog log2 = buildHandleLog(2L, 1, 2, "approve", "delete_content", 99L, "违规删除");
        when(sysReportHandleLogRepository.listByReportId(1L)).thenReturn(List.of(log1, log2));

        ReportHandleLogVO vo1 = buildHandleLogVO(1L, 0, 1, "claim", 99L);
        ReportHandleLogVO vo2 = buildHandleLogVO(2L, 1, 2, "approve", 99L);
        when(reportModelMapper.toHandleLogVO(log1)).thenReturn(vo1);
        when(reportModelMapper.toHandleLogVO(log2)).thenReturn(vo2);

        SysUser operator = new SysUser();
        operator.setId(99L);
        operator.setUsername("admin01");
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(operator));

        List<ReportHandleLogVO> result = reportAdminService.listHandleLogs(1L);

        assertEquals(2, result.size());
        assertSame(vo1, result.get(0));
        assertSame(vo2, result.get(1));
        assertEquals("admin01", result.get(0).getOperatorUsername());
        assertEquals("admin01", result.get(1).getOperatorUsername());
        verify(sysReportHandleLogRepository).listByReportId(1L);
    }

    // ==================== helper fixtures ====================

    private SysReportRecord buildPendingRecord(Long id, String targetType, Long targetId) {
        SysReportRecord record = new SysReportRecord();
        record.setId(id);
        record.setReportTargetType(targetType);
        record.setReportTargetId(targetId);
        record.setReporterUserId(10L);
        record.setStatus(0); // PENDING
        return record;
    }

    private SysReportRecord buildRecordWithStatus(Long id, Integer status) {
        SysReportRecord record = new SysReportRecord();
        record.setId(id);
        record.setStatus(status);
        record.setReportTargetType("article");
        record.setReportTargetId(100L);
        record.setReporterUserId(10L);
        return record;
    }

    private ReportHandleRequest buildHandleRequest(String resultType, String punishmentType, String remark) {
        ReportHandleRequest request = new ReportHandleRequest();
        request.setResultType(resultType);
        request.setPunishmentType(punishmentType);
        request.setRemark(remark);
        return request;
    }

    private SysReportHandleLog buildHandleLog(Long id, Integer fromStatus, Integer toStatus,
                                               String actionType, String actionResult,
                                               Long operatorId, String remark) {
        SysReportHandleLog log = new SysReportHandleLog();
        log.setId(id);
        log.setReportId(1L);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(actionType);
        log.setActionResult(actionResult);
        log.setOperatorUserId(operatorId);
        log.setActionRemark(remark);
        return log;
    }

    private ReportHandleLogVO buildHandleLogVO(Long id, Integer fromStatus, Integer toStatus,
                                                String actionType, Long operatorId) {
        ReportHandleLogVO vo = new ReportHandleLogVO();
        vo.setId(id);
        vo.setFromStatus(fromStatus);
        vo.setToStatus(toStatus);
        vo.setActionType(actionType);
        vo.setOperatorUserId(operatorId);
        return vo;
    }
}

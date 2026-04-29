package com.cybzacg.blogbackend.module.report.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysReportHandleLog;
import com.cybzacg.blogbackend.domain.SysReportRecord;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.report.ReportActionTypeEnum;
import com.cybzacg.blogbackend.enums.report.ReportRecordStatusEnum;
import com.cybzacg.blogbackend.enums.report.ReportTargetTypeEnum;
import com.cybzacg.blogbackend.module.auth.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.report.convert.ReportModelMapper;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminService;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.service.CommentAdminService;
import com.cybzacg.blogbackend.module.report.repository.SysReportHandleLogRepository;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import com.cybzacg.blogbackend.module.report.service.ReportAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台举报管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAdminServiceImpl implements ReportAdminService {

    private final SysReportRecordRepository sysReportRecordRepository;
    private final SysReportHandleLogRepository sysReportHandleLogRepository;
    private final SysUserRepository sysUserRepository;
    private final SysAuditLogService sysAuditLogService;
    private final SuperAdminVerifier superAdminVerifier;
    private final ReportModelMapper reportModelMapper;
    private final ArticleAdminService articleAdminService;
    private final CommentAdminService commentAdminService;
    private final ChatAdminService chatAdminService;
    private final BlogArticleRepository blogArticleRepository;
    private final SysCommentRepository sysCommentRepository;
    private final SysUserAdminService sysUserAdminService;

    @Override
    public PageResult<ReportAdminVO> pageReports(ReportAdminPageQuery query) {
        query.setCurrent(PaginationUtils.normalizeCurrent(query.getCurrent()));
        query.setSize(PaginationUtils.normalizeSize(query.getSize(), 10L, 100L));

        Page<SysReportRecord> page = sysReportRecordRepository.pageByFilters(
                query.getStatus(), query.getReportTargetType(), query.getReporterUserId(),
                query.getReportedStart(), query.getReportedEnd(),
                query.getCurrent(), query.getSize());

        List<ReportAdminVO> records = page.getRecords().stream()
                .map(reportModelMapper::toAdminVO)
                .toList();

        fillUserInfo(records);
        return PageResult.of(page, records);
    }

    @Override
    public ReportAdminVO getReportDetail(Long reportId) {
        SysReportRecord record = getReport(reportId);
        ReportAdminVO vo = reportModelMapper.toAdminVO(record);
        fillUserInfo(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimReport(Long reportId, Long operatorId) {
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
                !ReportRecordStatusEnum.PENDING.getValue().equals(record.getStatus()),
                ResultErrorCode.REPORT_ALREADY_HANDLED);

        record.setStatus(ReportRecordStatusEnum.PROCESSING.getValue());
        record.setHandlerUserId(operatorId);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(reportId, ReportRecordStatusEnum.PENDING.getValue(),
                ReportRecordStatusEnum.PROCESSING.getValue(),
                ReportActionTypeEnum.CLAIM.getCode(), null, operatorId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, Long operatorId, ReportHandleRequest request, String ip, String ua) {
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
                ReportRecordStatusEnum.HANDLED.getValue().equals(record.getStatus())
                        || ReportRecordStatusEnum.REJECTED.getValue().equals(record.getStatus()),
                ResultErrorCode.REPORT_ALREADY_HANDLED);

        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.HANDLED.getValue());
        record.setHandlerUserId(operatorId);
        record.setResultType(request.getResultType());
        record.setPunishmentType(request.getPunishmentType());
        record.setHandledAt(LocalDateTime.now());
        record.setRemark(request.getRemark());
        sysReportRecordRepository.updateById(record);

        saveHandleLog(reportId, fromStatus, ReportRecordStatusEnum.HANDLED.getValue(),
                ReportActionTypeEnum.APPROVE.getCode(), request.getResultType(), operatorId, request.getRemark());

        // 审计日志
        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(SysAuditOperationType.FINALIZE_REPORT.getCode());
        auditRequest.setTargetTypeName("SysReportRecord");
        auditRequest.setTargetId(reportId);
        auditRequest.setBeforeState("status=" + fromStatus);
        auditRequest.setAfterState("status=HANDLED,resultType=" + request.getResultType()
                + ",punishmentType=" + request.getPunishmentType());
        auditRequest.setRequestIp(ip);
        auditRequest.setUserAgent(ua);
        sysAuditLogService.record(auditRequest);

        // 执行治理动作
        executeGovernanceAction(record, request, operatorId, ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(Long reportId, Long operatorId, String remark, String ip, String ua) {
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
                ReportRecordStatusEnum.HANDLED.getValue().equals(record.getStatus())
                        || ReportRecordStatusEnum.REJECTED.getValue().equals(record.getStatus()),
                ResultErrorCode.REPORT_ALREADY_HANDLED);

        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.REJECTED.getValue());
        record.setHandlerUserId(operatorId);
        record.setHandledAt(LocalDateTime.now());
        record.setRemark(remark);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(reportId, fromStatus, ReportRecordStatusEnum.REJECTED.getValue(),
                ReportActionTypeEnum.REJECT.getCode(), null, operatorId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideClaim(Long reportId, Long operatorId, String ip, String ua) {
        superAdminVerifier.requireSuperAdmin(operatorId);

        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
                ReportRecordStatusEnum.HANDLED.getValue().equals(record.getStatus())
                        || ReportRecordStatusEnum.REJECTED.getValue().equals(record.getStatus()),
                ResultErrorCode.REPORT_ALREADY_HANDLED);

        Long previousHandler = record.getHandlerUserId();
        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.PROCESSING.getValue());
        record.setHandlerUserId(operatorId);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(reportId, fromStatus, ReportRecordStatusEnum.PROCESSING.getValue(),
                ReportActionTypeEnum.REASSIGN.getCode(), null, operatorId,
                "超管接管，原处理人: " + previousHandler);

        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(SysAuditOperationType.OVERRIDE_CLAIM_REPORT.getCode());
        auditRequest.setTargetTypeName("SysReportRecord");
        auditRequest.setTargetId(reportId);
        auditRequest.setBeforeState("status=" + fromStatus + ",handler=" + previousHandler);
        auditRequest.setAfterState("status=PROCESSING,handler=" + operatorId);
        auditRequest.setRequestIp(ip);
        auditRequest.setUserAgent(ua);
        sysAuditLogService.record(auditRequest);
    }

    @Override
    public List<ReportHandleLogVO> listHandleLogs(Long reportId) {
        getReport(reportId);
        List<SysReportHandleLog> logs = sysReportHandleLogRepository.listByReportId(reportId);
        List<ReportHandleLogVO> vos = logs.stream()
                .map(reportModelMapper::toHandleLogVO)
                .toList();

        // 填充操作人用户名
        Set<Long> operatorIds = vos.stream()
                .map(ReportHandleLogVO::getOperatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!operatorIds.isEmpty()) {
            Map<Long, SysUser> userMap = sysUserRepository.listByIds(operatorIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity()));
            for (ReportHandleLogVO vo : vos) {
                SysUser user = userMap.get(vo.getOperatorUserId());
                if (user != null) {
                    vo.setOperatorUsername(user.getUsername());
                }
            }
        }
        return vos;
    }

    /**
     * 根据处理结果执行对应治理动作。
     * 调用各业务模块服务，不直接改表。
     */
    private void executeGovernanceAction(SysReportRecord record, ReportHandleRequest request,
                                         Long operatorId, String ip, String ua) {
        String resultType = request.getResultType();
        if (resultType == null || "record_only".equals(resultType)) {
            return;
        }
        Long targetId = record.getReportTargetId();
        ReportTargetTypeEnum targetType = ReportTargetTypeEnum.fromCode(record.getReportTargetType());

        switch (resultType) {
            case "delete_content" -> {
                if (targetType == ReportTargetTypeEnum.ARTICLE) {
                    articleAdminService.deleteArticle(targetId);
                } else if (targetType == ReportTargetTypeEnum.COMMENT) {
                    commentAdminService.deleteComment(targetId);
                }
            }
            case "revoke_message" -> {
                if (targetType == ReportTargetTypeEnum.CHAT_MESSAGE && request.getConversationId() != null) {
                    chatAdminService.revokeMessage(request.getConversationId(), targetId);
                }
            }
            case "mute_user" -> {
                Long reportedUserId = resolveReportedUserId(record, targetType, request.getConversationId());
                if (reportedUserId == null) {
                    log.warn("无法获取被举报用户ID，跳过禁言 [reportId={}, targetType={}]", record.getId(), targetType);
                    return;
                }
                LocalDateTime muteUntil = LocalDateTime.now().plusDays(1);
                ChatAdminMemberMuteUpdateRequest muteRequest = new ChatAdminMemberMuteUpdateRequest();
                muteRequest.setMuteUntil(muteUntil);
                if (targetType == ReportTargetTypeEnum.CHAT_MESSAGE && request.getConversationId() != null) {
                    chatAdminService.updateMemberMute(request.getConversationId(), reportedUserId, muteRequest);
                } else {
                    log.info("举报对象类型({})不支持聊天禁言，仅记录处罚 [reportId={}, targetUserId={}]",
                            targetType, record.getId(), reportedUserId);
                }
            }
            case "ban_user" -> {
                Long reportedUserId = resolveReportedUserId(record, targetType, request.getConversationId());
                if (reportedUserId == null) {
                    log.warn("无法获取被举报用户ID，跳过封禁 [reportId={}, targetType={}]", record.getId(), targetType);
                    return;
                }
                String reason = request.getRemark() != null ? request.getRemark() : "举报处理封禁";
                sysUserAdminService.banUserByReport(operatorId, reportedUserId, reason, ip, ua);
            }
            default -> { /* record_only 或其他不执行动作 */ }
        }
    }

    private Long resolveReportedUserId(SysReportRecord record, ReportTargetTypeEnum targetType, Long conversationId) {
        Long targetId = record.getReportTargetId();
        return switch (targetType) {
            case ARTICLE -> {
                BlogArticle article = blogArticleRepository.getById(targetId);
                yield article != null ? article.getAuthorId() : null;
            }
            case COMMENT -> {
                SysComment comment = sysCommentRepository.getById(targetId);
                yield comment != null ? comment.getUserId() : null;
            }
            case CHAT_MESSAGE -> {
                if (conversationId != null) {
                    ChatAdminMessageDetailVO detail = chatAdminService.getMessageDetail(conversationId, targetId);
                    yield detail != null ? detail.getSenderId() : null;
                }
                yield null;
            }
        };
    }

    private SysReportRecord getReport(Long reportId) {
        return ExceptionThrowerCore.requireNonNull(
                sysReportRecordRepository.getById(reportId), ResultErrorCode.REPORT_NOT_FOUND);
    }

    private void saveHandleLog(Long reportId, Integer fromStatus, Integer toStatus,
                               String actionType, String actionResult,
                               Long operatorId, String remark) {
        SysReportHandleLog log = new SysReportHandleLog();
        log.setReportId(reportId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(actionType);
        log.setActionResult(actionResult);
        log.setOperatorUserId(operatorId);
        log.setActionRemark(remark);
        sysReportHandleLogRepository.save(log);
    }

    private void fillUserInfo(List<ReportAdminVO> records) {
        Set<Long> userIds = records.stream()
                .flatMap(vo -> java.util.stream.Stream.of(vo.getReporterUserId(), vo.getHandlerUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        Map<Long, SysUser> userMap = sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        for (ReportAdminVO vo : records) {
            SysUser reporter = userMap.get(vo.getReporterUserId());
            if (reporter != null) vo.setReporterUsername(reporter.getUsername());
            SysUser handler = userMap.get(vo.getHandlerUserId());
            if (handler != null) vo.setHandlerUsername(handler.getUsername());
        }
    }
}

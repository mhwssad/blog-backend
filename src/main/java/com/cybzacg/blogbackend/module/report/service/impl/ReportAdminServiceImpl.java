package com.cybzacg.blogbackend.module.report.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.content.SysComment;
import com.cybzacg.blogbackend.domain.report.SysReportHandleLog;
import com.cybzacg.blogbackend.domain.report.SysReportRecord;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.report.ReportActionTypeEnum;
import com.cybzacg.blogbackend.enums.report.ReportRecordStatusEnum;
import com.cybzacg.blogbackend.enums.report.ReportTargetTypeEnum;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMuteGovernanceService;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.comment.service.CommentAdminService;
import com.cybzacg.blogbackend.module.forum.service.ForumPostAdminService;
import com.cybzacg.blogbackend.module.forum.service.ForumReplyAdminService;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.domain.forum.ForumReply;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import com.cybzacg.blogbackend.module.report.convert.ReportModelConvert;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.repository.SysReportHandleLogRepository;
import com.cybzacg.blogbackend.module.report.repository.SysReportRecordRepository;
import com.cybzacg.blogbackend.module.report.service.ReportAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ReportModelConvert reportModelConvert;
    private final ArticleAdminService articleAdminService;
    private final CommentAdminService commentAdminService;
    private final ChatAdminService chatAdminService;
    private final ChatMuteGovernanceService chatMuteGovernanceService;
    private final BlogArticleRepository blogArticleRepository;
    private final SysCommentRepository sysCommentRepository;
    private final SysUserAdminService sysUserAdminService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final ForumPostAdminService forumPostAdminService;
    private final ForumReplyAdminService forumReplyAdminService;

    /**
     * 分页查询举报记录（管理端）。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<ReportAdminVO> pageReports(ReportAdminPageQuery query) {
        query.setCurrent(PaginationUtils.normalizeCurrent(query.getCurrent()));
        query.setSize(
            PaginationUtils.normalizeSize(query.getSize(), 10L, 100L)
        );
        log.debug(
            "[举报管理] 分页查询 current={} size={} status={} targetType={}",
            query.getCurrent(),
            query.getSize(),
            query.getStatus(),
            query.getReportTargetType()
        );

        Page<SysReportRecord> page = sysReportRecordRepository.pageByFilters(
            query.getStatus(),
            query.getReportTargetType(),
            query.getReporterUserId(),
            query.getReportedStart(),
            query.getReportedEnd(),
            query.getCurrent(),
            query.getSize()
        );

        List<ReportAdminVO> records = page
            .getRecords()
            .stream()
            .map(reportModelConvert::toAdminVO)
            .toList();

        fillUserInfo(records);
        log.debug("[举报管理] 查询完成 total={}", page.getTotal());
        return PageResult.of(page, records);
    }

    /**
     * 查询单条举报详情（管理端）。
     *
     * @param reportId 举报记录ID
     * @return 举报详情VO
     */
    @Override
    public ReportAdminVO getReportDetail(Long reportId) {
        log.debug("[举报管理] 查询详情 reportId={}", reportId);
        SysReportRecord record = getReport(reportId);
        ReportAdminVO vo = reportModelConvert.toAdminVO(record);
        fillUserInfo(List.of(vo));
        return vo;
    }

    /**
     * 认领举报。
     * 将待处理的举报标记为处理中，记录当前操作人为处理人。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimReport(Long reportId, Long operatorId) {
        log.info(
            "[举报管理] 认领举报 reportId={} operatorId={}",
            reportId,
            operatorId
        );
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
            !ReportRecordStatusEnum.PENDING.getValue().equals(
                record.getStatus()
            ),
            ResultErrorCode.REPORT_ALREADY_HANDLED
        );

        record.setStatus(ReportRecordStatusEnum.PROCESSING.getValue());
        record.setHandlerUserId(operatorId);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(
            reportId,
            ReportRecordStatusEnum.PENDING.getValue(),
            ReportRecordStatusEnum.PROCESSING.getValue(),
            ReportActionTypeEnum.CLAIM.getCode(),
            null,
            operatorId,
            null
        );
    }

    /**
     * 处理举报。
     * 更新状态为已处理，写入处理结果与处罚类型，执行业务治理动作并记录审计日志。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     * @param request    处理请求
     * @param ip         请求IP
     * @param ua          UserAgent
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(
        Long reportId,
        Long operatorId,
        ReportHandleRequest request,
        String ip,
        String ua
    ) {
        log.info(
            "[举报管理] 处理举报 reportId={} operatorId={} resultType={} punishmentType={}",
            reportId,
            operatorId,
            request.getResultType(),
            request.getPunishmentType()
        );
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
            ReportRecordStatusEnum.HANDLED.getValue().equals(
                    record.getStatus()
                ) ||
                ReportRecordStatusEnum.REJECTED.getValue().equals(
                    record.getStatus()
                ),
            ResultErrorCode.REPORT_ALREADY_HANDLED
        );

        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.HANDLED.getValue());
        record.setHandlerUserId(operatorId);
        record.setResultType(request.getResultType());
        record.setPunishmentType(request.getPunishmentType());
        record.setHandledAt(LocalDateTime.now());
        record.setRemark(request.getRemark());
        sysReportRecordRepository.updateById(record);

        saveHandleLog(
            reportId,
            fromStatus,
            ReportRecordStatusEnum.HANDLED.getValue(),
            ReportActionTypeEnum.APPROVE.getCode(),
            request.getResultType(),
            operatorId,
            request.getRemark()
        );

        // 审计日志
        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(
            SysAuditOperationType.FINALIZE_REPORT.getCode()
        );
        auditRequest.setTargetTypeName("SysReportRecord");
        auditRequest.setTargetId(reportId);
        auditRequest.setBeforeState("status=" + fromStatus);
        auditRequest.setAfterState(
            "status=HANDLED,resultType=" +
                request.getResultType() +
                ",punishmentType=" +
                request.getPunishmentType()
        );
        auditRequest.setRequestIp(ip);
        auditRequest.setUserAgent(ua);
        sysAuditLogService.record(auditRequest);

        // 执行治理动作，
        executeGovernanceAction(record, request, operatorId, ip, ua);

        notifyReporterHandled(record, request);
    }

    /**
     * 驳回举报。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     * @param remark     驳回备注
     * @param ip         请求IP
     * @param ua         UserAgent
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(
        Long reportId,
        Long operatorId,
        String remark,
        String ip,
        String ua
    ) {
        log.info(
            "[举报管理] 驳回举报 reportId={} operatorId={}",
            reportId,
            operatorId
        );
        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
            ReportRecordStatusEnum.HANDLED.getValue().equals(
                    record.getStatus()
                ) ||
                ReportRecordStatusEnum.REJECTED.getValue().equals(
                    record.getStatus()
                ),
            ResultErrorCode.REPORT_ALREADY_HANDLED
        );

        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.REJECTED.getValue());
        record.setHandlerUserId(operatorId);
        record.setHandledAt(LocalDateTime.now());
        record.setRemark(remark);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(
            reportId,
            fromStatus,
            ReportRecordStatusEnum.REJECTED.getValue(),
            ReportActionTypeEnum.REJECT.getCode(),
            null,
            operatorId,
            remark
        );

        notifyReporterRejected(record, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideClaim(
        Long reportId,
        Long operatorId,
        String ip,
        String ua
    ) {
        superAdminVerifier.requireSuperAdmin(operatorId);

        SysReportRecord record = getReport(reportId);
        ExceptionThrowerCore.throwBusinessIf(
            ReportRecordStatusEnum.HANDLED.getValue().equals(
                    record.getStatus()
                ) ||
                ReportRecordStatusEnum.REJECTED.getValue().equals(
                    record.getStatus()
                ),
            ResultErrorCode.REPORT_ALREADY_HANDLED
        );

        Long previousHandler = record.getHandlerUserId();
        Integer fromStatus = record.getStatus();

        record.setStatus(ReportRecordStatusEnum.PROCESSING.getValue());
        record.setHandlerUserId(operatorId);
        sysReportRecordRepository.updateById(record);

        saveHandleLog(
            reportId,
            fromStatus,
            ReportRecordStatusEnum.PROCESSING.getValue(),
            ReportActionTypeEnum.REASSIGN.getCode(),
            null,
            operatorId,
            "超管接管，原处理人: " + previousHandler
        );

        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(
            SysAuditOperationType.OVERRIDE_CLAIM_REPORT.getCode()
        );
        auditRequest.setTargetTypeName("SysReportRecord");
        auditRequest.setTargetId(reportId);
        auditRequest.setBeforeState(
            "status=" + fromStatus + ",handler=" + previousHandler
        );
        auditRequest.setAfterState("status=PROCESSING,handler=" + operatorId);
        auditRequest.setRequestIp(ip);
        auditRequest.setUserAgent(ua);
        sysAuditLogService.record(auditRequest);
    }

    @Override
    public List<ReportHandleLogVO> listHandleLogs(Long reportId) {
        getReport(reportId);
        List<SysReportHandleLog> logs =
            sysReportHandleLogRepository.listByReportId(reportId);
        List<ReportHandleLogVO> vos = logs
            .stream()
            .map(reportModelConvert::toHandleLogVO)
            .toList();

        // 填充操作人用户名
        Set<Long> operatorIds = vos
            .stream()
            .map(ReportHandleLogVO::getOperatorUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (!operatorIds.isEmpty()) {
            Map<Long, SysUser> userMap = sysUserRepository
                .listByIds(operatorIds)
                .stream()
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
    private void executeGovernanceAction(
        SysReportRecord record,
        ReportHandleRequest request,
        Long operatorId,
        String ip,
        String ua
    ) {
        String resultType = request.getResultType();
        if (resultType == null || "record_only".equals(resultType)) {
            return;
        }
        Long targetId = record.getReportTargetId();
        ReportTargetTypeEnum targetType = ReportTargetTypeEnum.fromCode(
            record.getReportTargetType()
        );

        switch (resultType) {
            case "delete_content" -> {
                if (targetType == ReportTargetTypeEnum.ARTICLE) {
                    articleAdminService.deleteArticle(targetId);
                } else if (targetType == ReportTargetTypeEnum.COMMENT) {
                    commentAdminService.deleteComment(targetId);
                } else if (targetType == ReportTargetTypeEnum.FORUM_POST) {
                    forumPostAdminService.deletePost(targetId, operatorId, ip, ua);
                } else if (targetType == ReportTargetTypeEnum.FORUM_REPLY) {
                    forumReplyAdminService.deleteReply(targetId, operatorId, ip, ua);
                }
            }
            case "revoke_message" -> {
                if (
                    targetType == ReportTargetTypeEnum.CHAT_MESSAGE &&
                    request.getConversationId() != null
                ) {
                    chatAdminService.revokeMessage(
                        request.getConversationId(),
                        targetId
                    );
                }
            }
            case "mute_user" -> {
                Long reportedUserId = resolveReportedUserId(
                    record,
                    targetType,
                    request.getConversationId()
                );
                if (reportedUserId == null) {
                    log.warn(
                        "无法获取被举报用户ID，跳过禁言 [reportId={}, targetType={}]",
                        record.getId(),
                        targetType
                    );
                    return;
                }
                String muteScope = request.getMuteScope() != null ? request.getMuteScope() : "global";
                LocalDateTime muteUntil = request.getMuteUntil() != null
                        ? request.getMuteUntil()
                        : LocalDateTime.now().plusDays(1);
                chatMuteGovernanceService.createMuteFromReport(
                        reportedUserId, muteScope, request.getConversationId(),
                        "举报处罚 id=" + record.getId(), record.getId(),
                        operatorId, muteUntil);
            }
            case "ban_user" -> {
                Long reportedUserId = resolveReportedUserId(
                    record,
                    targetType,
                    request.getConversationId()
                );
                if (reportedUserId == null) {
                    log.warn(
                        "无法获取被举报用户ID，跳过封禁 [reportId={}, targetType={}]",
                        record.getId(),
                        targetType
                    );
                    return;
                }
                String reason =
                    request.getRemark() != null
                        ? request.getRemark()
                        : "举报处理封禁";
                sysUserAdminService.banUserByReport(
                    operatorId,
                    reportedUserId,
                    reason,
                    ip,
                    ua
                );
            }
            default -> {
                /* record_only 或其他不执行动作 */
            }
        }
    }

    private Long resolveReportedUserId(
        SysReportRecord record,
        ReportTargetTypeEnum targetType,
        Long conversationId
    ) {
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
                    ChatAdminMessageDetailVO detail =
                        chatAdminService.getMessageDetail(
                            conversationId,
                            targetId
                        );
                    yield detail != null ? detail.getSenderId() : null;
                }
                yield null;
            }
            case FORUM_POST -> {
                ForumPost post = forumPostRepository.getById(targetId);
                yield post != null ? post.getAuthorId() : null;
            }
            case FORUM_REPLY -> {
                ForumReply reply = forumReplyRepository.getById(targetId);
                yield reply != null ? reply.getUserId() : null;
            }
        };
    }

    private SysReportRecord getReport(Long reportId) {
        return ExceptionThrowerCore.requireNonNull(
            sysReportRecordRepository.getById(reportId),
            ResultErrorCode.REPORT_NOT_FOUND
        );
    }

    private void saveHandleLog(
        Long reportId,
        Integer fromStatus,
        Integer toStatus,
        String actionType,
        String actionResult,
        Long operatorId,
        String remark
    ) {
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

    /**
     * 通知举报人处理结果。内容不包含处理人信息。
     */
    private void notifyReporterHandled(SysReportRecord record, ReportHandleRequest request) {
        String targetLabel = resolveTargetTypeLabel(record.getReportTargetType());
        String title = "你的举报已处理";
        String content = buildHandledContent(targetLabel, request.getResultType());
        notificationDeliveryService.deliverAfterCommit(
                record.getReporterUserId(), NotificationTypeEnum.REPORT_RESULT,
                title, content, null);
    }

    /**
     * 通知举报人举报已被驳回。
     */
    private void notifyReporterRejected(SysReportRecord record, String remark) {
        String targetLabel = resolveTargetTypeLabel(record.getReportTargetType());
        String title = "你的举报已驳回";
        String content = "你举报的" + targetLabel + "经审核未违反社区规范。"
                + (remark != null ? "处理说明：" + truncate(remark, 100) : "");
        notificationDeliveryService.deliverAfterCommit(
                record.getReporterUserId(), NotificationTypeEnum.REPORT_RESULT,
                title, content, null);
    }

    private String buildHandledContent(String targetLabel, String resultType) {
        if (resultType == null) {
            return "你举报的" + targetLabel + "已经审核处理。";
        }
        return switch (resultType) {
            case "delete_content" -> "你举报的" + targetLabel + "已被删除。";
            case "revoke_message" -> "你举报的" + targetLabel + "已被撤回。";
            case "mute_user" -> "你举报的" + targetLabel + "相关用户已被禁言。";
            case "ban_user" -> "你举报的" + targetLabel + "相关用户已被封禁。";
            default -> "你举报的" + targetLabel + "已经审核处理。";
        };
    }

    private String resolveTargetTypeLabel(String targetType) {
        ReportTargetTypeEnum type = ReportTargetTypeEnum.fromCode(targetType);
        return type != null ? type.getLabel() : "内容";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private void fillUserInfo(List<ReportAdminVO> records) {
        Set<Long> userIds = records
            .stream()
            .flatMap(vo ->
                java.util.stream.Stream.of(
                    vo.getReporterUserId(),
                    vo.getHandlerUserId()
                )
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        Map<Long, SysUser> userMap = sysUserRepository
            .listByIds(userIds)
            .stream()
            .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        for (ReportAdminVO vo : records) {
            SysUser reporter = userMap.get(vo.getReporterUserId());
            if (reporter != null) vo.setReporterUsername(
                reporter.getUsername()
            );
            SysUser handler = userMap.get(vo.getHandlerUserId());
            if (handler != null) vo.setHandlerUsername(handler.getUsername());
        }
    }
}

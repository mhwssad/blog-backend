package com.cybzacg.blogbackend.module.forum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumReplyRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumReplyAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 论坛回复后台治理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ForumReplyAdminServiceImpl implements ForumReplyAdminService {

    private final ForumReplyRepository forumReplyRepository;
    private final ForumPostRepository forumPostRepository;
    private final SysUserRepository sysUserRepository;
    private final ForumModelConvert forumModelConvert;
    private final SysAuditLogService sysAuditLogService;

    @Override
    public PageResult<ForumReplyAdminVO> pageReplies(ForumReplyAdminPageQuery query) {
        ForumReplyAdminPageQuery safeQuery = normalizeQuery(query);
        Page<ForumReply> page = forumReplyRepository.pageAdminReplies(safeQuery);
        List<ForumReplyAdminVO> records = page.getRecords().stream()
                .map(forumModelConvert::toReplyAdminVO)
                .toList();
        enrichReplyVOs(records);
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideReply(Long id, Long operatorId, String ip, String ua) {
        ForumReply reply = getReplyOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(reply.getStatus(), ForumReplyStatusEnum.DELETED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "回复已删除，无法隐藏");
        String beforeState = "{\"status\":" + reply.getStatus() + "}";
        forumReplyRepository.updateStatusById(id, ForumReplyStatusEnum.HIDDEN.getValue());
        recordAudit(operatorId, reply.getUserId(), SysAuditOperationType.HIDE_FORUM_REPLY,
                id, beforeState, "{\"status\":" + ForumReplyStatusEnum.HIDDEN.getValue() + "}", ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreReply(Long id, Long operatorId, String ip, String ua) {
        ForumReply reply = getReplyOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(reply.getStatus(), ForumReplyStatusEnum.HIDDEN.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "仅隐藏状态的回复可恢复");
        String beforeState = "{\"status\":" + reply.getStatus() + "}";
        forumReplyRepository.updateStatusById(id, ForumReplyStatusEnum.NORMAL.getValue());
        recordAudit(operatorId, reply.getUserId(), SysAuditOperationType.RESTORE_FORUM_REPLY,
                id, beforeState, "{\"status\":" + ForumReplyStatusEnum.NORMAL.getValue() + "}", ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long id, Long operatorId, String ip, String ua) {
        ForumReply reply = getReplyOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(reply.getStatus(), ForumReplyStatusEnum.DELETED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "回复已删除");
        String beforeState = "{\"status\":" + reply.getStatus() + "}";
        forumReplyRepository.updateStatusById(id, ForumReplyStatusEnum.DELETED.getValue());
        forumPostRepository.incrementReplyCount(reply.getPostId(), -1);
        if (reply.getParentId() != null && reply.getParentId() > 0) {
            forumReplyRepository.incrementReplyCount(reply.getParentId(), -1);
        }
        recordAudit(operatorId, reply.getUserId(), SysAuditOperationType.DELETE_FORUM_REPLY,
                id, beforeState, "{\"status\":" + ForumReplyStatusEnum.DELETED.getValue() + "}", ip, ua);
    }

    // ==================== private helpers ====================

    private ForumReplyAdminPageQuery normalizeQuery(ForumReplyAdminPageQuery query) {
        ForumReplyAdminPageQuery safeQuery = query == null ? new ForumReplyAdminPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), 10L, 100L));
        safeQuery.setKeyword(StrUtils.trimToNull(safeQuery.getKeyword()));
        return safeQuery;
    }

    private ForumReply getReplyOrThrow(Long id) {
        ForumReply reply = forumReplyRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(reply, ResultErrorCode.ILLEGAL_ARGUMENT, "回复不存在");
        return reply;
    }

    private void enrichReplyVOs(List<ForumReplyAdminVO> vos) {
        if (vos == null || vos.isEmpty()) return;
        Set<Long> userIds = vos.stream().map(ForumReplyAdminVO::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> postIds = vos.stream().map(ForumReplyAdminVO::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNameMap = batchQueryUserNames(userIds);
        Map<Long, String> postTitleMap = batchQueryPostTitles(postIds);
        for (ForumReplyAdminVO vo : vos) {
            if (vo.getUserId() != null) vo.setUserName(userNameMap.get(vo.getUserId()));
            if (vo.getPostId() != null) vo.setPostTitle(postTitleMap.get(vo.getPostId()));
            vo.setStatusName(resolveReplyStatusName(vo.getStatus()));
        }
    }

    private Map<Long, String> batchQueryUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
    }

    private Map<Long, String> batchQueryPostTitles(Set<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();
        return forumPostRepository.listByIds(postIds).stream()
                .collect(Collectors.toMap(ForumPost::getId, ForumPost::getTitle, (a, b) -> a));
    }

    private String resolveReplyStatusName(Integer status) {
        if (status == null) return null;
        for (ForumReplyStatusEnum e : ForumReplyStatusEnum.values()) {
            if (e.getValue().equals(status)) return e.getDescription();
        }
        return null;
    }

    private void recordAudit(Long operatorId, Long targetUserId, SysAuditOperationType operationType,
                             Long targetId, String beforeState, String afterState, String ip, String ua) {
        SysAuditLogCreateRequest request = new SysAuditLogCreateRequest();
        request.setOperatorUserId(operatorId);
        request.setTargetUserId(targetUserId);
        request.setOperationType(operationType.getCode());
        request.setTargetTypeName("forum_reply");
        request.setTargetId(targetId);
        request.setBeforeState(beforeState);
        request.setAfterState(afterState);
        request.setMfaPassed(0);
        request.setRequestIp(ip);
        request.setUserAgent(ua);
        sysAuditLogService.record(request);
    }
}

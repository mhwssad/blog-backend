package com.cybzacg.blogbackend.module.forum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminDetailVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumPostAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 论坛帖子后台治理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ForumPostAdminServiceImpl implements ForumPostAdminService {

    private final ForumPostRepository forumPostRepository;
    private final ForumSectionRepository forumSectionRepository;
    private final SysUserRepository sysUserRepository;
    private final ForumModelConvert forumModelConvert;
    private final SysAuditLogService sysAuditLogService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int LINK_STATUS_INVALID = 0;
    private static final int LINK_STATUS_NORMAL = 1;

    @Override
    public PageResult<ForumPostAdminVO> pagePosts(ForumPostAdminPageQuery query) {
        ForumPostAdminPageQuery safeQuery = normalizeQuery(query);
        Page<ForumPost> page = forumPostRepository.pageAdminPosts(safeQuery);
        List<ForumPostAdminVO> records = page.getRecords().stream()
                .map(forumModelConvert::toPostAdminVO)
                .toList();
        enrichPostVOs(records);
        return PageResult.of(page, records);
    }

    @Override
    public ForumPostAdminDetailVO getPost(Long id) {
        ForumPost post = getPostOrThrow(id);
        ForumPostAdminDetailVO vo = forumModelConvert.toPostAdminDetailVO(post);
        enrichPostVO(List.of(vo));
        vo.setStatusName(resolvePostStatusName(post.getStatus()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hidePost(Long id, Long operatorId, String ip, String ua) {
        ForumPost post = getPostOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(post.getStatus(), ForumPostStatusEnum.DELETED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "帖子已删除，无法隐藏");
        String beforeState = "{\"status\":" + post.getStatus() + "}";
        forumPostRepository.updateStatusById(id, ForumPostStatusEnum.HIDDEN.getValue());
        forumPostChannelLinkRepository.updateStatusByForumPostId(id, LINK_STATUS_INVALID);
        recordAudit(operatorId, post.getAuthorId(), SysAuditOperationType.HIDE_FORUM_POST,
                id, beforeState, "{\"status\":" + ForumPostStatusEnum.HIDDEN.getValue() + "}", ip, ua);
        eventPublisher.publishEvent(new ContentChangeEvent(
                AiKnowledgeSourceTypeEnum.FORUM_POST.getCode(),
                id, ContentChangeAction.HIDE, post.getAuthorId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restorePost(Long id, Long operatorId, String ip, String ua) {
        ForumPost post = getPostOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(post.getStatus(), ForumPostStatusEnum.HIDDEN.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "仅隐藏状态的帖子可恢复");
        String beforeState = "{\"status\":" + post.getStatus() + "}";
        forumPostRepository.updateStatusById(id, ForumPostStatusEnum.PUBLISHED.getValue());
        forumPostChannelLinkRepository.updateStatusByForumPostId(id, LINK_STATUS_NORMAL);
        recordAudit(operatorId, post.getAuthorId(), SysAuditOperationType.RESTORE_FORUM_POST,
                id, beforeState, "{\"status\":" + ForumPostStatusEnum.PUBLISHED.getValue() + "}", ip, ua);
        eventPublisher.publishEvent(new ContentChangeEvent(
                AiKnowledgeSourceTypeEnum.FORUM_POST.getCode(),
                id, ContentChangeAction.RESTORE, post.getAuthorId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id, Long operatorId, String ip, String ua) {
        ForumPost post = getPostOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(post.getStatus(), ForumPostStatusEnum.DELETED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "帖子已删除");
        String beforeState = "{\"status\":" + post.getStatus() + "}";
        forumPostRepository.updateStatusById(id, ForumPostStatusEnum.DELETED.getValue());
        forumPostChannelLinkRepository.updateStatusByForumPostId(id, LINK_STATUS_INVALID);
        recordAudit(operatorId, post.getAuthorId(), SysAuditOperationType.DELETE_FORUM_POST,
                id, beforeState, "{\"status\":" + ForumPostStatusEnum.DELETED.getValue() + "}", ip, ua);
        eventPublisher.publishEvent(new ContentChangeEvent(
                AiKnowledgeSourceTypeEnum.FORUM_POST.getCode(),
                id, ContentChangeAction.DELETE, post.getAuthorId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        ForumPost post = getPostOrThrow(id);
        String beforeState = "{\"isTop\":" + post.getIsTop() + "}";
        forumPostRepository.updateTopById(id, enabled ? 1 : 0);
        recordAudit(operatorId, post.getAuthorId(), SysAuditOperationType.TOGGLE_FORUM_POST_PIN,
                id, beforeState, "{\"isTop\":" + (enabled ? 1 : 0) + "}", ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEssence(Long id, boolean enabled, Long operatorId, String ip, String ua) {
        ForumPost post = getPostOrThrow(id);
        String beforeState = "{\"isEssence\":" + post.getIsEssence() + "}";
        forumPostRepository.updateEssenceById(id, enabled ? 1 : 0);
        recordAudit(operatorId, post.getAuthorId(), SysAuditOperationType.TOGGLE_FORUM_POST_ESSENCE,
                id, beforeState, "{\"isEssence\":" + (enabled ? 1 : 0) + "}", ip, ua);
        if (enabled) {
            notificationDeliveryService.deliverAfterCommit(
                    post.getAuthorId(),
                    NotificationTypeEnum.FORUM_POST_ESSENCE,
                    "帖子被设为精华",
                    "您的帖子「" + post.getTitle() + "」已被管理员设为精华",
                    operatorId,
                    "forum_post", post.getId(), "/forum/posts/" + post.getId()
            );
        }
    }

    // ==================== private helpers ====================

    private ForumPostAdminPageQuery normalizeQuery(ForumPostAdminPageQuery query) {
        ForumPostAdminPageQuery safeQuery = query == null ? new ForumPostAdminPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), 10L, 100L));
        safeQuery.setKeyword(StrUtils.trimToNull(safeQuery.getKeyword()));
        return safeQuery;
    }

    private ForumPost getPostOrThrow(Long id) {
        ForumPost post = forumPostRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(post, ResultErrorCode.ILLEGAL_ARGUMENT, "帖子不存在");
        return post;
    }

    private void enrichPostVOs(List<ForumPostAdminVO> vos) {
        if (vos == null || vos.isEmpty()) return;
        Set<Long> sectionIds = vos.stream().map(ForumPostAdminVO::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> authorIds = vos.stream().map(ForumPostAdminVO::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> sectionNameMap = batchQuerySectionNames(sectionIds);
        Map<Long, String> authorNameMap = batchQueryUserNames(authorIds);
        for (ForumPostAdminVO vo : vos) {
            if (vo.getSectionId() != null) vo.setSectionName(sectionNameMap.get(vo.getSectionId()));
            if (vo.getAuthorId() != null) vo.setAuthorName(authorNameMap.get(vo.getAuthorId()));
            vo.setStatusName(resolvePostStatusName(vo.getStatus()));
        }
    }

    private void enrichPostVO(List<? extends ForumPostAdminVO> vos) {
        enrichPostVOs(new ArrayList<>(vos));
    }

    private Map<Long, String> batchQuerySectionNames(Set<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return Map.of();
        return forumSectionRepository.listByIds(sectionIds).stream()
                .collect(Collectors.toMap(ForumSection::getId, ForumSection::getName, (a, b) -> a));
    }

    private Map<Long, String> batchQueryUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
    }

    private String resolvePostStatusName(Integer status) {
        if (status == null) return null;
        for (ForumPostStatusEnum e : ForumPostStatusEnum.values()) {
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
        request.setTargetTypeName("forum_post");
        request.setTargetId(targetId);
        request.setBeforeState(beforeState);
        request.setAfterState(afterState);
        request.setMfaPassed(0);
        request.setRequestIp(ip);
        request.setUserAgent(ua);
        sysAuditLogService.record(request);
    }
}

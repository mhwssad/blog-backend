package com.cybzacg.blogbackend.module.chat.member.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.dto.domain.chat.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ChatConversationRepository;
import com.cybzacg.blogbackend.dto.repository.chat.member.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.dto.repository.chat.member.ChatGroupJoinApplicationRepository;
import com.cybzacg.blogbackend.enums.chat.ChatGroupJoinApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplicationVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplyRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinReviewRequest;
import com.cybzacg.blogbackend.module.chat.member.service.UserChatGroupJoinApplicationService;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelConvert;
import com.cybzacg.blogbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户侧入群申请服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserChatGroupJoinApplicationServiceImpl implements UserChatGroupJoinApplicationService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final ChatGroupJoinApplicationRepository chatGroupJoinApplicationRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelConvert chatModelConvert;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupJoinApplicationVO submitApplication(Long conversationId, ChatGroupJoinApplyRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ChatConversation conversation = requireJoinableConversation(conversationId);
        ensureNotActiveMember(conversationId, userId);
        ensureMemberLimitNotReached(conversation);
        ChatGroupJoinApplication latest = chatGroupJoinApplicationRepository.findLatestByConversationAndApplicant(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(latest != null
                        && Objects.equals(latest.getApplyStatus(), ChatGroupJoinApplicationStatusEnum.PENDING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前已有待审核入群申请，请勿重复提交");

        ChatGroupJoinApplication application = new ChatGroupJoinApplication();
        application.setConversationId(conversationId);
        application.setApplicantUserId(userId);
        application.setApplyMessage(request == null ? null : StrUtils.trimToNull(request.getApplyMessage()));
        application.setApplyStatus(ChatGroupJoinApplicationStatusEnum.PENDING.getValue());
        application.setSubmittedAt(LocalDateTime.now());
        chatGroupJoinApplicationRepository.save(application);
        return toVO(application, loadUserMap(List.of(application)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatGroupJoinApplicationVO> pageMyApplications(ChatGroupJoinApplicationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        ChatGroupJoinApplicationPageQuery safeQuery = normalizeQuery(query);
        Page<ChatGroupJoinApplication> page = chatGroupJoinApplicationRepository.pageByApplicantUserId(userId, safeQuery);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords());
        List<ChatGroupJoinApplicationVO> records = page.getRecords().stream()
                .map(application -> toVO(application, userMap))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatGroupJoinApplicationVO> pageGroupApplications(Long conversationId, ChatGroupJoinApplicationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupManager(userId, conversationId);
        ChatGroupJoinApplicationPageQuery safeQuery = normalizeQuery(query);
        Page<ChatGroupJoinApplication> page = chatGroupJoinApplicationRepository.pageByConversationId(conversationId, safeQuery);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords());
        List<ChatGroupJoinApplicationVO> records = page.getRecords().stream()
                .map(application -> toVO(application, userMap))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long conversationId, Long applicationId, ChatGroupJoinReviewRequest request) {
        Long reviewerId = SecurityUtils.requireUserId();
        ChatConversation conversation = requireGroupManager(reviewerId, conversationId);
        validateReviewRequest(request);
        ChatGroupJoinApplication application = requireApplication(conversationId, applicationId);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(application.getApplyStatus(), ChatGroupJoinApplicationStatusEnum.PENDING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前申请状态不可审核");

        application.setApplyStatus(request.getReviewStatus());
        application.setReviewerId(reviewerId);
        application.setReviewComment(StrUtils.trimToNull(request.getReviewComment()));
        application.setReviewedAt(LocalDateTime.now());
        if (Objects.equals(request.getReviewStatus(), ChatGroupJoinApplicationStatusEnum.APPROVED.getValue())) {
            ensureMemberLimitNotReached(conversation);
            upsertApprovedMember(conversationId, application.getApplicantUserId());
        }
        chatGroupJoinApplicationRepository.updateById(application);
    }

    private ChatConversation requireJoinableConversation(Long conversationId) {
        ChatConversation conversation = requireNormalGroupConversation(conversationId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(conversation.getJoinRule(), ChatConstants.JOIN_RULE_INVITE_ONLY),
                ResultErrorCode.FORBIDDEN,
                "当前群聊仅允许邀请加入");
        return conversation;
    }

    private ChatConversation requireGroupManager(Long userId, Long conversationId) {
        ChatConversation conversation = requireNormalGroupConversation(conversationId);
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(member == null || !Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL),
                ResultErrorCode.FORBIDDEN,
                "当前用户不在该群聊中");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)
                        && !Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN),
                ResultErrorCode.FORBIDDEN,
                "只有群主或管理员可以审核入群申请");
        return conversation;
    }

    private ChatConversation requireNormalGroupConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "群聊不存在或不可用");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || Integer.valueOf(1).equals(conversation.getIsAllSite()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前会话不支持入群申请");
        return conversation;
    }

    private ChatGroupJoinApplication requireApplication(Long conversationId, Long applicationId) {
        ExceptionThrowerCore.throwBusinessIfNull(applicationId, ResultErrorCode.ILLEGAL_ARGUMENT, "申请ID不能为空");
        ChatGroupJoinApplication application = chatGroupJoinApplicationRepository.getById(applicationId);
        ExceptionThrowerCore.throwBusinessIf(application == null || !Objects.equals(application.getConversationId(), conversationId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "入群申请不存在");
        return application;
    }

    private void ensureNotActiveMember(Long conversationId, Long userId) {
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(member != null && Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前用户已在群聊中");
    }

    private void ensureMemberLimitNotReached(ChatConversation conversation) {
        Integer limit = conversation.getMemberLimit();
        if (limit == null || limit <= 0) {
            return;
        }
        int activeCount = chatConversationMemberRepository.listActiveByConversationId(conversation.getId()).size();
        ExceptionThrowerCore.throwBusinessIf(activeCount >= limit,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前群聊人数已达上限");
    }

    private void upsertApprovedMember(Long conversationId, Long applicantUserId) {
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, applicantUserId);
        if (member == null) {
            member = new ChatConversationMember();
            member.setConversationId(conversationId);
            member.setUserId(applicantUserId);
            member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
            member.setJoinSource(ChatConstants.JOIN_SOURCE_APPLICATION);
            member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
            member.setJoinedAt(LocalDateTime.now());
            chatConversationMemberRepository.save(member);
            return;
        }
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        member.setJoinSource(ChatConstants.JOIN_SOURCE_APPLICATION);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        member.setJoinedAt(member.getJoinedAt() == null ? LocalDateTime.now() : member.getJoinedAt());
        member.setMuteUntil(null);
        chatConversationMemberRepository.updateById(member);
    }

    private void validateReviewRequest(ChatGroupJoinReviewRequest request) {
        // Validation handled by JSR-303 annotations
    }

    private ChatGroupJoinApplicationPageQuery normalizeQuery(ChatGroupJoinApplicationPageQuery query) {
        ChatGroupJoinApplicationPageQuery safeQuery = query == null ? new ChatGroupJoinApplicationPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        return safeQuery;
    }

    private Map<Long, SysUser> loadUserMap(List<ChatGroupJoinApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = applications.stream()
                .flatMap(application -> Arrays.stream(new Long[]{application.getApplicantUserId(), application.getReviewerId()}))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
    }

    private ChatGroupJoinApplicationVO toVO(ChatGroupJoinApplication application, Map<Long, SysUser> userMap) {
        ChatGroupJoinApplicationVO vo = chatModelConvert.toGroupJoinApplicationVO(application);
        ChatGroupJoinApplicationStatusEnum status = ChatGroupJoinApplicationStatusEnum.fromValue(application.getApplyStatus());
        vo.setApplyStatusLabel(status == null ? null : status.getLabel());
        SysUser applicant = userMap.get(application.getApplicantUserId());
        vo.setApplicantUsername(applicant == null ? null : applicant.getUsername());
        vo.setApplicantNickname(UserDisplayNameUtils.resolveDisplayName(applicant, application.getApplicantUserId()));
        vo.setApplicantAvatar(applicant == null ? null : applicant.getAvatar());
        SysUser reviewer = userMap.get(application.getReviewerId());
        vo.setReviewerUsername(reviewer == null ? null : reviewer.getUsername());
        vo.setReviewerNickname(UserDisplayNameUtils.resolveDisplayName(reviewer, application.getReviewerId()));
        return vo;
    }
}

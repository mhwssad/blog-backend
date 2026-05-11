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
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationReviewRequest;
import com.cybzacg.blogbackend.module.chat.member.service.ChatGroupJoinApplicationAdminService;
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
 * 后台群入群申请管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatGroupJoinApplicationAdminServiceImpl implements ChatGroupJoinApplicationAdminService {
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
    public PageResult<ChatGroupJoinApplicationAdminVO> pageApplications(ChatGroupJoinApplicationAdminPageQuery query) {
        ChatGroupJoinApplicationAdminPageQuery safeQuery = normalizeQuery(query);
        Page<ChatGroupJoinApplication> page = chatGroupJoinApplicationRepository.pageByAdminConditions(safeQuery);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords());
        Map<Long, ChatConversation> conversationMap = loadConversationMap(page.getRecords());
        List<ChatGroupJoinApplicationAdminVO> records = page.getRecords().stream()
                .map(application -> toAdminVO(application, userMap, conversationMap))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatGroupJoinApplicationAdminVO getApplication(Long id) {
        ChatGroupJoinApplication application = requireApplication(id);
        return toAdminVO(application, loadUserMap(List.of(application)), loadConversationMap(List.of(application)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long id, ChatGroupJoinApplicationReviewRequest request) {
        validateReviewRequest(request);
        ChatGroupJoinApplication application = requireApplication(id);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(application.getApplyStatus(), ChatGroupJoinApplicationStatusEnum.PENDING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前申请状态不可审核"
        );
        ChatConversation conversation = requireReviewableConversation(application.getConversationId());
        application.setApplyStatus(request.getReviewStatus());
        application.setReviewerId(SecurityUtils.requireUserId());
        application.setReviewComment(StrUtils.trimToNull(request.getReviewComment()));
        application.setReviewedAt(LocalDateTime.now());
        if (Objects.equals(request.getReviewStatus(), ChatGroupJoinApplicationStatusEnum.APPROVED.getValue())) {
            ensureMemberLimitNotReached(conversation);
            upsertApprovedMember(conversation.getId(), application.getApplicantUserId());
        }
        chatGroupJoinApplicationRepository.updateById(application);
    }

    private ChatConversation requireReviewableConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || Objects.equals(conversation.getIsAllSite(), 1)
                        || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "群聊不存在或不可用");
        return conversation;
    }

    private ChatGroupJoinApplication requireApplication(Long id) {
        ExceptionThrowerCore.throwBusinessIfNull(id, ResultErrorCode.ILLEGAL_ARGUMENT, "申请ID不能为空");
        ChatGroupJoinApplication application = chatGroupJoinApplicationRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(application, ResultErrorCode.ILLEGAL_ARGUMENT, "入群申请不存在");
        return application;
    }

    private void validateReviewRequest(ChatGroupJoinApplicationReviewRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "审核参数不能为空");
        Integer reviewStatus = request.getReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(reviewStatus, ChatGroupJoinApplicationStatusEnum.APPROVED.getValue())
                        && !Objects.equals(reviewStatus, ChatGroupJoinApplicationStatusEnum.REJECTED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "审核状态不合法");
    }

    private ChatGroupJoinApplicationAdminPageQuery normalizeQuery(ChatGroupJoinApplicationAdminPageQuery query) {
        ChatGroupJoinApplicationAdminPageQuery safeQuery = query == null ? new ChatGroupJoinApplicationAdminPageQuery() : query;
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

    private Map<Long, ChatConversation> loadConversationMap(List<ChatGroupJoinApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return Map.of();
        }
        Set<Long> conversationIds = applications.stream()
                .map(ChatGroupJoinApplication::getConversationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        return chatConversationRepository.listByIds(conversationIds).stream()
                .collect(Collectors.toMap(ChatConversation::getId, Function.identity(), (left, right) -> left));
    }

    private ChatGroupJoinApplicationAdminVO toAdminVO(ChatGroupJoinApplication application,
                                                      Map<Long, SysUser> userMap,
                                                      Map<Long, ChatConversation> conversationMap) {
        ChatGroupJoinApplicationAdminVO vo = chatModelConvert.toGroupJoinApplicationAdminVO(application);
        ChatGroupJoinApplicationStatusEnum status = ChatGroupJoinApplicationStatusEnum.fromValue(application.getApplyStatus());
        vo.setApplyStatusLabel(status == null ? null : status.getLabel());
        SysUser applicant = userMap.get(application.getApplicantUserId());
        vo.setApplicantUsername(applicant == null ? null : applicant.getUsername());
        vo.setApplicantNickname(UserDisplayNameUtils.resolveDisplayName(applicant, application.getApplicantUserId()));
        vo.setApplicantAvatar(applicant == null ? null : applicant.getAvatar());
        SysUser reviewer = userMap.get(application.getReviewerId());
        vo.setReviewerUsername(reviewer == null ? null : reviewer.getUsername());
        vo.setReviewerNickname(UserDisplayNameUtils.resolveDisplayName(reviewer, application.getReviewerId()));
        ChatConversation conversation = conversationMap.get(application.getConversationId());
        if (conversation != null) {
            vo.setConversationName(conversation.getName());
            vo.setConversationAvatar(conversation.getAvatar());
            vo.setConversationType(conversation.getConversationType());
            vo.setConversationSceneType(conversation.getSceneType());
            vo.setConversationJoinRule(conversation.getJoinRule());
            vo.setConversationStatus(conversation.getStatus());
        }
        return vo;
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
}

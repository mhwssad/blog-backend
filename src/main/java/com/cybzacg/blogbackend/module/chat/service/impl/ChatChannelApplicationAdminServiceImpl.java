package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.chat.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.chat.ChatChannelApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatChannelApplicationReviewRequest;
import com.cybzacg.blogbackend.module.chat.repository.ChatChannelCreateApplicationRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.service.ChatChannelApplicationAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台频道创建申请管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatChannelApplicationAdminServiceImpl implements ChatChannelApplicationAdminService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final ChatChannelCreateApplicationRepository chatChannelCreateApplicationRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelMapper chatModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatChannelApplicationAdminVO> pageApplications(ChatChannelApplicationAdminPageQuery query) {
        ChatChannelApplicationAdminPageQuery safeQuery = normalizeQuery(query);
        Page<ChatChannelCreateApplication> page = chatChannelCreateApplicationRepository.pageByAdminConditions(safeQuery);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords());
        List<ChatChannelApplicationAdminVO> records = page.getRecords().stream()
                .map(application -> toAdminVO(application, userMap))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatChannelApplicationAdminVO getApplication(Long id) {
        ChatChannelCreateApplication application = requireApplication(id);
        return toAdminVO(application, loadUserMap(List.of(application)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long id, ChatChannelApplicationReviewRequest request) {
        validateReviewRequest(request);
        ChatChannelCreateApplication application = requireApplication(id);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(application.getApplyStatus(), ChatChannelApplicationStatusEnum.PENDING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前申请状态不可审核"
        );

        application.setApplyStatus(request.getReviewStatus());
        application.setReviewerId(SecurityUtils.requireUserId());
        application.setReviewComment(StrUtils.trimToNull(request.getReviewComment()));
        application.setReviewedAt(LocalDateTime.now());
        if (Objects.equals(request.getReviewStatus(), ChatChannelApplicationStatusEnum.APPROVED.getValue())) {
            ChatConversation conversation = createTopicChannel(application);
            application.setConversationId(conversation.getId());
        }
        chatChannelCreateApplicationRepository.updateById(application);
    }

    private ChatConversation createTopicChannel(ChatChannelCreateApplication application) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setName(application.getDesiredName());
        conversation.setOwnerId(application.getApplicantUserId());
        conversation.setSceneType(ChatConstants.SCENE_TYPE_TOPIC_CHANNEL);
        conversation.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_MEMBER);
        conversation.setAllowGuestView(0);
        conversation.setRequireJoinToSpeak(1);
        conversation.setJoinRule(ChatConstants.JOIN_RULE_APPROVAL);
        conversation.setSpeakLevelLimit(1);
        conversation.setMemberLimit(0);
        conversation.setIsAllSite(0);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setAnnouncement(application.getDescription());
        conversation.setSlowModeSeconds(0);
        conversation.setDisplaySort(0);
        conversation.setChannelCategoryCode(application.getDesiredCategoryCode());
        chatConversationRepository.save(conversation);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversation.getId());
        ownerMember.setUserId(application.getApplicantUserId());
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setJoinSource(ChatConstants.JOIN_SOURCE_MANUAL);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setJoinedAt(LocalDateTime.now());
        chatConversationMemberRepository.save(ownerMember);
        return conversation;
    }

    private ChatChannelCreateApplication requireApplication(Long id) {
        ExceptionThrowerCore.throwBusinessIfNull(id, ResultErrorCode.ILLEGAL_ARGUMENT, "申请ID不能为空");
        ChatChannelCreateApplication application = chatChannelCreateApplicationRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(application, ResultErrorCode.ILLEGAL_ARGUMENT, "频道申请不存在");
        return application;
    }

    private void validateReviewRequest(ChatChannelApplicationReviewRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "审核参数不能为空");
        Integer reviewStatus = request.getReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(reviewStatus, ChatChannelApplicationStatusEnum.APPROVED.getValue())
                        && !Objects.equals(reviewStatus, ChatChannelApplicationStatusEnum.REJECTED.getValue())
                        && !Objects.equals(reviewStatus, ChatChannelApplicationStatusEnum.NEED_MORE_INFO.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "审核状态不合法"
        );
    }

    private ChatChannelApplicationAdminPageQuery normalizeQuery(ChatChannelApplicationAdminPageQuery query) {
        ChatChannelApplicationAdminPageQuery safeQuery = query == null ? new ChatChannelApplicationAdminPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        return safeQuery;
    }

    private Map<Long, SysUser> loadUserMap(List<ChatChannelCreateApplication> applications) {
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

    private ChatChannelApplicationAdminVO toAdminVO(ChatChannelCreateApplication application, Map<Long, SysUser> userMap) {
        ChatChannelApplicationAdminVO vo = chatModelMapper.toChannelApplicationAdminVO(application);
        ChatChannelApplicationStatusEnum status = ChatChannelApplicationStatusEnum.fromValue(application.getApplyStatus());
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

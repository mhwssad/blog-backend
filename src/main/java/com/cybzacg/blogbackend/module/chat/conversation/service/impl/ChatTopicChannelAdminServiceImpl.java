package com.cybzacg.blogbackend.module.chat.conversation.service.impl;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatTopicChannelSaveRequest;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatTopicChannelAdminService;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 后台主题频道管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatTopicChannelAdminServiceImpl implements ChatTopicChannelAdminService {
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelMapper chatModelMapper;
    private final ChatNotificationService chatNotificationService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatAdminConversationVO createTopicChannel(ChatTopicChannelSaveRequest request) {
        validateRequest(request);
        Long ownerId = request.getOwnerId() == null ? SecurityUtils.requireUserId() : request.getOwnerId();
        requireActiveUser(ownerId);
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setSceneType(ChatConstants.SCENE_TYPE_TOPIC_CHANNEL);
        conversation.setOwnerId(ownerId);
        conversation.setIsAllSite(0);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        applyTopicChannelFields(conversation, request);
        chatConversationRepository.save(conversation);
        upsertOwnerMember(conversation, ownerId);
        return getTopicChannelVO(conversation.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatAdminConversationVO updateTopicChannel(Long conversationId, ChatTopicChannelSaveRequest request) {
        validateRequest(request);
        ChatConversation conversation = requireTopicChannel(conversationId);
        Long ownerId = request.getOwnerId();
        if (ownerId != null && !Objects.equals(ownerId, conversation.getOwnerId())) {
            requireActiveUser(ownerId);
            conversation.setOwnerId(ownerId);
            upsertOwnerMember(conversation, ownerId);
        }
        String oldAnnouncement = StrUtils.trimToNull(conversation.getAnnouncement());
        applyTopicChannelFields(conversation, request);
        chatConversationRepository.updateById(conversation);
        if (!Objects.equals(oldAnnouncement, conversation.getAnnouncement()) && StrUtils.hasText(conversation.getAnnouncement())) {
            chatNotificationService.deliverChannelAnnouncementNotifications(
                    conversation,
                    chatConversationMemberRepository.listActiveByConversationId(conversationId),
                    SecurityUtils.requireUserId()
            );
        }
        return getTopicChannelVO(conversationId);
    }

    private void applyTopicChannelFields(ChatConversation conversation, ChatTopicChannelSaveRequest request) {
        conversation.setName(StrUtils.trim(request.getName()));
        conversation.setAvatar(StrUtils.trimToNull(request.getAvatar()));
        conversation.setRemark(StrUtils.trimToNull(request.getDescription()));
        conversation.setAnnouncement(StrUtils.trimToNull(request.getAnnouncement()));
        conversation.setVisibilityScope(resolveVisibilityScope(request.getVisibilityScope()));
        conversation.setAllowGuestView(0);
        conversation.setRequireJoinToSpeak(1);
        conversation.setJoinRule(resolveJoinRule(request.getJoinRule()));
        conversation.setSpeakLevelLimit(request.getSpeakLevelLimit() == null ? 1 : request.getSpeakLevelLimit());
        conversation.setMemberLimit(request.getMemberLimit() == null ? 0 : request.getMemberLimit());
        conversation.setSlowModeSeconds(request.getSlowModeSeconds() == null ? 0 : request.getSlowModeSeconds());
        conversation.setDisplaySort(request.getDisplaySort() == null ? 0 : request.getDisplaySort());
        conversation.setChannelCategoryCode(StrUtils.trimToNull(request.getCategoryCode()));
    }

    private void validateRequest(ChatTopicChannelSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "频道参数不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(StrUtils.trimToNull(request.getName()), ResultErrorCode.ILLEGAL_ARGUMENT, "频道名称不能为空");
        resolveVisibilityScope(request.getVisibilityScope());
        resolveJoinRule(request.getJoinRule());
    }

    private String resolveVisibilityScope(String value) {
        String visibilityScope = StrUtils.trimToNull(value);
        if (visibilityScope == null) {
            return ChatConstants.VISIBILITY_SCOPE_MEMBER;
        }
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(visibilityScope, ChatConstants.VISIBILITY_SCOPE_PUBLIC)
                        && !Objects.equals(visibilityScope, ChatConstants.VISIBILITY_SCOPE_MEMBER)
                        && !Objects.equals(visibilityScope, ChatConstants.VISIBILITY_SCOPE_PRIVATE),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "频道可见范围不合法");
        return visibilityScope;
    }

    private String resolveJoinRule(String value) {
        String joinRule = StrUtils.trimToNull(value);
        if (joinRule == null) {
            return ChatConstants.JOIN_RULE_APPROVAL;
        }
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(joinRule, ChatConstants.JOIN_RULE_FREE)
                        && !Objects.equals(joinRule, ChatConstants.JOIN_RULE_APPROVAL)
                        && !Objects.equals(joinRule, ChatConstants.JOIN_RULE_INVITE_ONLY),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "频道加入规则不合法");
        return joinRule;
    }

    private ChatConversation requireTopicChannel(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || !Objects.equals(conversation.getSceneType(), ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                        || Integer.valueOf(1).equals(conversation.getIsAllSite()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "主题频道不存在");
        return conversation;
    }

    private void requireActiveUser(Long userId) {
        ExceptionThrowerCore.throwBusinessIfNull(userId, ResultErrorCode.ILLEGAL_ARGUMENT, "频道负责人不能为空");
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(user == null || !Objects.equals(user.getDeletedFlag(), 0) || !Objects.equals(user.getStatus(), 1),
                ResultErrorCode.USER_NOT_FOUND,
                "频道负责人不存在或不可用");
    }

    private void upsertOwnerMember(ChatConversation conversation, Long ownerId) {
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversation.getId(), ownerId);
        if (member == null) {
            member = new ChatConversationMember();
            member.setConversationId(conversation.getId());
            member.setUserId(ownerId);
            member.setJoinedAt(LocalDateTime.now());
        }
        member.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        member.setJoinSource(ChatConstants.JOIN_SOURCE_MANUAL);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        member.setMuteUntil(null);
        if (member.getId() == null) {
            chatConversationMemberRepository.save(member);
        } else {
            chatConversationMemberRepository.updateById(member);
        }
    }

    private ChatAdminConversationVO getTopicChannelVO(Long conversationId) {
        ChatAdminConversationListItem item = chatConversationRepository.selectAdminConversationDetail(conversationId);
        ExceptionThrowerCore.throwBusinessIfNull(item, ResultErrorCode.ILLEGAL_ARGUMENT, "主题频道不存在");
        return chatModelMapper.toAdminConversationVO(item);
    }
}

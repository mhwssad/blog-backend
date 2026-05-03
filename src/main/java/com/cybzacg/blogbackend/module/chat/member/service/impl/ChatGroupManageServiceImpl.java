package com.cybzacg.blogbackend.module.chat.member.service.impl;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupNoticeUpdateRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatTransferGroupOwnerRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMuteMemberRequest;
import com.cybzacg.blogbackend.module.chat.member.service.ChatGroupManageService;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatPushPayloadBuilder;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatServiceSupport;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 群组管理子服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatGroupManageServiceImpl implements ChatGroupManageService {

    private final ChatServiceSupport s;
    private final ChatPushService chatPushService;
    private final ChatPushPayloadBuilder chatPushPayloadBuilder;
    private final ChatNotificationService chatNotificationService;
    private final UserExperienceService userExperienceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO createGroup(Long userId, ChatCreateGroupRequest request) {
        validateCreateGroupPermission(userId);
        validateCreateGroupCountLimit(userId);
        validateCreateGroupOptions(request);
        List<Long> memberUserIds = s.normalizeMemberIds(request.getMemberUserIds(), userId);
        ExceptionThrowerCore.throwBusinessIf(memberUserIds.isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "群成员不能为空");
        s.requireActiveUsers(memberUserIds, true);
        s.ensureMemberLimitAllows(request.getMemberLimit(), memberUserIds.size() + 1, "初始群成员数量超过群人数上限");
        ChatConversation conversation = s.getModelConvert().toGroupConversation(request);
        normalizeGroupConversationOptions(conversation);
        conversation.setOwnerId(userId);
        s.getConversationRepository().save(conversation);
        s.upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_OWNER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        for (Long memberUserId : memberUserIds) {
            s.upsertConversationMembership(conversation, memberUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        }
        return s.getConversationVO(userId, conversation.getId());
    }

    @Override
    public ChatConversationVO getGroupDetail(Long userId, Long conversationId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupAccess(userId, conversationId);
        return s.getConversationVO(userId, context.conversation().getId());
    }

    @Override
    public List<ChatMemberVO> listGroupMembers(Long userId, Long conversationId) {
        s.requireGroupAccess(userId, conversationId);
        return s.buildMemberRecords(s.listActiveMembers(conversationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> inviteGroupMembers(Long userId, Long conversationId, ChatGroupMemberOperateRequest request) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupManager(userId, conversationId);
        List<Long> memberUserIds = s.normalizeMemberIds(request.getMemberUserIds(), userId);
        ExceptionThrowerCore.throwBusinessIf(memberUserIds.isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "成员用户ID不能为空");
        s.requireActiveUsers(memberUserIds, true);
        s.ensureConversationMemberLimitAllows(context.conversation(), s.countAdditionalMembers(conversationId, memberUserIds));
        for (Long memberUserId : memberUserIds) {
            s.upsertConversationMembership(context.conversation(), memberUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        }
        List<ChatConversationMember> members = s.listActiveMembers(conversationId);
        List<ChatMemberVO> records = s.buildMemberRecords(members);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("members_invited", conversationId, null, records), s.activeUserIds(members));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> appointGroupAdmin(Long userId, Long conversationId, Long memberUserId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupOwner(userId, conversationId);
        ChatConversationMember member = s.requireActiveGroupMember(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.ILLEGAL_ARGUMENT, "群主无需重复设置为管理员");
        member.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        s.getConversationMemberRepository().updateById(member);
        List<ChatConversationMember> members = s.listActiveMembers(conversationId);
        List<ChatMemberVO> records = s.buildMemberRecords(members);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("admin_appointed", conversationId, memberUserId, records), s.activeUserIds(members));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> removeGroupAdmin(Long userId, Long conversationId, Long memberUserId) {
        s.requireGroupOwner(userId, conversationId);
        ChatConversationMember member = s.requireActiveGroupMember(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.ILLEGAL_ARGUMENT, "不能取消群主角色");
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        s.getConversationMemberRepository().updateById(member);
        List<ChatConversationMember> members = s.listActiveMembers(conversationId);
        List<ChatMemberVO> records = s.buildMemberRecords(members);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("admin_removed", conversationId, memberUserId, records), s.activeUserIds(members));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO transferGroupOwner(Long userId, Long conversationId, ChatTransferGroupOwnerRequest request) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupOwner(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(request == null || request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "新群主用户ID不能为空");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(request.getTargetUserId(), userId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能把群主转让给自己");
        ChatConversationMember targetMember = s.requireActiveGroupMember(conversationId, request.getTargetUserId());
        context.selfMember().setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        context.conversation().setOwnerId(targetMember.getUserId());
        s.getConversationMemberRepository().updateById(context.selfMember());
        s.getConversationMemberRepository().updateById(targetMember);
        s.getConversationRepository().updateById(context.conversation());
        List<ChatConversationMember> members = s.listActiveMembers(conversationId);
        List<ChatMemberVO> memberRecords = s.buildMemberRecords(members);
        List<Long> activeUserIds = s.activeUserIds(members);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("owner_transferred", conversationId, targetMember.getUserId(), memberRecords), activeUserIds);
        chatPushService.pushConversationUpdated(chatPushPayloadBuilder.buildConversationUpdatedPayload("owner_transferred", context.conversation(), members), activeUserIds);
        return s.getConversationVO(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> muteGroupMember(Long userId, Long conversationId, Long memberUserId, ChatMuteMemberRequest request) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupManager(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, memberUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能对自己执行禁言操作");
        ChatConversationMember targetMember = s.requireActiveGroupMember(conversationId, memberUserId);
        s.validateManagerCanOperateMember(context.selfMember(), targetMember);
        LocalDateTime muteUntil = request == null ? null : request.getMuteUntil();
        targetMember.setMuteUntil(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()) ? muteUntil : null);
        s.getConversationMemberRepository().updateById(targetMember);
        List<ChatConversationMember> members = s.listActiveMembers(conversationId);
        List<ChatMemberVO> records = s.buildMemberRecords(members);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("member_mute_updated", conversationId, memberUserId, records), s.activeUserIds(members));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO updateGroupNotice(Long userId, Long conversationId, ChatGroupNoticeUpdateRequest request) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupManager(userId, conversationId);
        String oldAnnouncement = StrUtils.trimToNull(context.conversation().getAnnouncement());
        context.conversation().setAnnouncement(request == null ? null : StrUtils.trimToNull(request.getNotice()));
        s.getConversationRepository().updateById(context.conversation());
        chatPushService.pushConversationUpdated(chatPushPayloadBuilder.buildConversationUpdatedPayload("notice_updated", context.conversation(), s.listActiveMembers(conversationId)), context.activeUserIds());
        if (Objects.equals(context.conversation().getSceneType(), ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                && !Objects.equals(oldAnnouncement, context.conversation().getAnnouncement())
                && StrUtils.hasText(context.conversation().getAnnouncement())) {
            chatNotificationService.deliverChannelAnnouncementNotifications(context.conversation(), context.activeMembers(), userId);
        }
        return s.getConversationVO(userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupMember(Long userId, Long conversationId, Long memberUserId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupManager(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, memberUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能移除自己，请使用退群接口");
        ChatConversationMember member = s.requireActiveGroupMember(conversationId, memberUserId);
        s.validateManagerCanOperateMember(context.selfMember(), member);
        member.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        s.getConversationMemberRepository().updateById(member);
        List<ChatMemberVO> records = s.buildMemberRecords(s.listActiveMembers(conversationId));
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("member_removed", conversationId, memberUserId, records), notifyUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long userId, Long conversationId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupAccess(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(context.selfMember().getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.UNSUPPORTED_OPERATION, "群主不能直接退群，请先解散群聊");
        context.selfMember().setStatus(ChatConstants.MEMBER_STATUS_LEFT);
        s.getConversationMemberRepository().updateById(context.selfMember());
        List<ChatMemberVO> records = s.buildMemberRecords(s.listActiveMembers(conversationId));
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("member_left", conversationId, userId, records), notifyUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long userId, Long conversationId) {
        ChatServiceSupport.ConversationAccessContext context = s.requireGroupOwner(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        context.conversation().setStatus(ChatConstants.CONVERSATION_STATUS_DISSOLVED);
        s.getConversationRepository().updateById(context.conversation());
        s.getConversationMemberRepository().removeAllActiveMembers(conversationId);
        chatPushService.pushConversationUpdated(chatPushPayloadBuilder.buildConversationUpdatedPayload("conversation_dissolved", context.conversation(), List.of()), notifyUserIds);
    }

    // ========== Private helpers ==========

    private void validateCreateGroupPermission(Long userId) {
        int requiredLevel = s.getConfigInt(
                ConfigConstants.CHAT_GROUP_CREATE_MIN_LEVEL_KEY,
                ConfigConstants.DEFAULT_CHAT_GROUP_CREATE_MIN_LEVEL
        );
        if (requiredLevel <= 1) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                !userExperienceService.checkLevelPermission(userId, requiredLevel),
                ResultErrorCode.FORBIDDEN,
                "当前等级不足，至少达到 Lv." + requiredLevel + " 才能创建群聊"
        );
    }

    private void validateCreateGroupCountLimit(Long userId) {
        int maxCount = s.getConfigInt(
                ConfigConstants.CHAT_GROUP_CREATE_MAX_COUNT_KEY,
                ConfigConstants.DEFAULT_CHAT_GROUP_CREATE_MAX_COUNT
        );
        if (maxCount <= 0) {
            return;
        }
        long currentCount = s.getConversationRepository().countNormalGroupsByOwner(userId);
        ExceptionThrowerCore.throwBusinessIf(currentCount >= maxCount,
                ResultErrorCode.FORBIDDEN,
                "当前创建群聊数量已达上限");
    }

    private void validateCreateGroupOptions(ChatCreateGroupRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "创建群聊参数不能为空");
        String visibilityScope = StrUtils.trimToNull(request.getVisibilityScope());
        ExceptionThrowerCore.throwBusinessIf(visibilityScope != null
                        && !Objects.equals(visibilityScope, ChatConstants.VISIBILITY_SCOPE_PUBLIC)
                        && !Objects.equals(visibilityScope, ChatConstants.VISIBILITY_SCOPE_PRIVATE),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "群可见范围不合法");
        String joinRule = StrUtils.trimToNull(request.getJoinRule());
        ExceptionThrowerCore.throwBusinessIf(joinRule != null
                        && !Objects.equals(joinRule, ChatConstants.JOIN_RULE_FREE)
                        && !Objects.equals(joinRule, ChatConstants.JOIN_RULE_APPROVAL)
                        && !Objects.equals(joinRule, ChatConstants.JOIN_RULE_INVITE_ONLY),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "群加入规则不合法");
    }

    private void normalizeGroupConversationOptions(ChatConversation conversation) {
        if (!StrUtils.hasText(conversation.getSceneType())) {
            conversation.setSceneType(ChatConstants.SCENE_TYPE_USER_GROUP);
        }
        if (!StrUtils.hasText(conversation.getVisibilityScope())) {
            conversation.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_PRIVATE);
        }
        if (!StrUtils.hasText(conversation.getJoinRule())) {
            conversation.setJoinRule(ChatConstants.JOIN_RULE_FREE);
        }
        if (conversation.getAllowGuestView() == null) {
            conversation.setAllowGuestView(0);
        }
        if (conversation.getRequireJoinToSpeak() == null) {
            conversation.setRequireJoinToSpeak(1);
        }
        if (conversation.getSpeakLevelLimit() == null || conversation.getSpeakLevelLimit() < 1) {
            conversation.setSpeakLevelLimit(1);
        }
        if (conversation.getMemberLimit() == null || conversation.getMemberLimit() < 0) {
            conversation.setMemberLimit(0);
        }
        if (conversation.getSlowModeSeconds() == null || conversation.getSlowModeSeconds() < 0) {
            conversation.setSlowModeSeconds(0);
        }
        if (conversation.getDisplaySort() == null) {
            conversation.setDisplaySort(0);
        }
    }
}

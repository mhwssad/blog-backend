package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminGovernanceService;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminQueryService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.support.ChatPayloadHelper;
import com.cybzacg.blogbackend.module.chat.support.ChatPushPayloadBuilder;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 后台聊天治理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatAdminGovernanceServiceImpl implements ChatAdminGovernanceService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelMapper chatModelMapper;
    private final ChatPushService chatPushService;
    private final FileChatFacadeService fileChatFacadeService;
    private final ChatPushPayloadBuilder chatPushPayloadBuilder;
    private final ChatPayloadHelper chatPayloadHelper;
    private final ChatAdminQueryService chatAdminQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> updateMemberRole(Long conversationId, Long memberUserId, ChatAdminMemberRoleUpdateRequest request) {
        ChatConversation conversation = requireManageableGroupConversation(conversationId);
        ChatConversationMember member = requireMember(conversationId, memberUserId);
        String role = normalizeRole(request);
        if (Objects.equals(role, ChatConstants.MEMBER_ROLE_OWNER)) {
            ChatConversationMember currentOwner = findOwnerMember(conversationId);
            if (currentOwner != null && !Objects.equals(currentOwner.getUserId(), memberUserId)) {
                currentOwner.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
                chatConversationMemberRepository.updateById(currentOwner);
            }
            conversation.setOwnerId(memberUserId);
            chatConversationRepository.updateById(conversation);
        } else if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER) && conversation.getOwnerId() != null
                && !Objects.equals(conversation.getOwnerId(), memberUserId)) {
            conversation.setOwnerId(null);
            chatConversationRepository.updateById(conversation);
        } else if (!Objects.equals(role, ChatConstants.MEMBER_ROLE_OWNER) && Objects.equals(conversation.getOwnerId(), memberUserId)) {
            conversation.setOwnerId(null);
            chatConversationRepository.updateById(conversation);
        }
        member.setMemberRole(role);
        chatConversationMemberRepository.updateById(member);
        List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);
        List<ChatMemberVO> records = chatAdminQueryService.listMembers(conversationId);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("admin_member_role_updated", conversationId, memberUserId, records), activeUserIds(activeMembers));
        chatPushService.pushConversationUpdated(chatPushPayloadBuilder.buildConversationUpdatedPayload("admin_member_role_updated", conversation, activeMembers), activeUserIds(activeMembers));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> updateMemberStatus(Long conversationId, Long memberUserId, ChatAdminMemberStatusUpdateRequest request) {
        requireManageableGroupConversation(conversationId);
        ChatConversationMember member = requireMember(conversationId, memberUserId);
        List<Long> notifyUserIds = new ArrayList<>(activeUserIds(listActiveMembers(conversationId)));
        if (member.getUserId() != null && !notifyUserIds.contains(member.getUserId())) {
            notifyUserIds.add(member.getUserId());
        }
        Integer status = request == null ? null : request.getStatus();
        validateMemberStatus(status);
        member.setStatus(status);
        if (!Objects.equals(status, ChatConstants.MEMBER_STATUS_NORMAL)) {
            member.setMuteUntil(null);
        }
        if (Objects.equals(status, ChatConstants.MEMBER_STATUS_NORMAL) && member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }
        chatConversationMemberRepository.updateById(member);
        List<ChatMemberVO> records = chatAdminQueryService.listMembers(conversationId);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("admin_member_status_updated", conversationId, memberUserId, records), notifyUserIds);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> updateMemberMute(Long conversationId, Long memberUserId, ChatAdminMemberMuteUpdateRequest request) {
        requireManageableGroupConversation(conversationId);
        ChatConversationMember member = requireMember(conversationId, memberUserId);
        LocalDateTime muteUntil = request == null ? null : request.getMuteUntil();
        member.setMuteUntil(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()) ? muteUntil : null);
        chatConversationMemberRepository.updateById(member);
        List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);
        List<ChatMemberVO> records = chatAdminQueryService.listMembers(conversationId);
        chatPushService.pushMembersUpdated(chatPushPayloadBuilder.buildMembersUpdatedPayload("admin_member_mute_updated", conversationId, memberUserId, records), activeUserIds(activeMembers));
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMessage(Long conversationId, Long messageId) {
        requireConversation(conversationId);
        ChatMessage message = requireMessage(conversationId, messageId);
        if (Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_REVOKED);
        message.setRevokedBy(0L);
        message.setRevokedAt(now);
        message.setContent(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER);
        message.setPayloadJson(null);
        chatMessageRepository.updateById(message);
        releaseFileReferencesForMessage(message);
        chatPushService.pushMessageRevoked(buildMessagePushVO(message), activeUserIds(listActiveMembers(conversationId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConversationStatus(Long conversationId, Integer status) {
        validateConversationStatus(status);
        ChatConversation conversation = requireConversation(conversationId);
        if (Objects.equals(conversation.getStatus(), status)) {
            return;
        }
        conversation.setStatus(status);
        chatConversationRepository.updateById(conversation);
        chatPushService.pushConversationUpdated(chatPushPayloadBuilder.buildConversationUpdatedPayload("admin_conversation_status_updated", conversation, listActiveMembers(conversationId)),
                activeUserIds(listActiveMembers(conversationId)));
    }

    // ==================== 私有辅助方法 ====================

    private List<ChatConversationMember> listActiveMembers(Long conversationId) {
        return chatConversationMemberRepository.listActiveByConversationId(conversationId);
    }

    private List<Long> activeUserIds(List<ChatConversationMember> members) {
        if (members == null || members.isEmpty()) return List.of();
        return members.stream().map(ChatConversationMember::getUserId).distinct().toList();
    }

    private ChatConversation requireConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在");
        return conversation;
    }

    private ChatConversation requireManageableGroupConversation(Long conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || Objects.equals(conversation.getIsAllSite(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT, "后台成员治理仅支持普通群聊会话");
        return conversation;
    }

    private ChatMessage requireMessage(Long conversationId, Long messageId) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        ChatMessage message = chatMessageRepository.getById(messageId);
        ExceptionThrowerCore.throwBusinessIf(message == null || !Objects.equals(message.getConversationId(), conversationId), ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在");
        return message;
    }

    private ChatConversationMember requireMember(Long conversationId, Long memberUserId) {
        ExceptionThrowerCore.throwBusinessIfNull(memberUserId, ResultErrorCode.ILLEGAL_ARGUMENT, "成员用户ID不能为空");
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIfNull(member, ResultErrorCode.ILLEGAL_ARGUMENT, "成员不存在");
        return member;
    }

    private ChatConversationMember findOwnerMember(Long conversationId) {
        return chatConversationMemberRepository.findOwnerByConversationId(conversationId);
    }

    private String normalizeRole(ChatAdminMemberRoleUpdateRequest request) {
        String role = request == null ? null : StrUtils.trimToNull(request.getRole());
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(role, ChatConstants.MEMBER_ROLE_OWNER)
                        && !Objects.equals(role, ChatConstants.MEMBER_ROLE_ADMIN)
                        && !Objects.equals(role, ChatConstants.MEMBER_ROLE_MEMBER),
                ResultErrorCode.ILLEGAL_ARGUMENT, "成员角色不合法");
        return role;
    }

    private void validateMemberStatus(Integer status) {
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(status, ChatConstants.MEMBER_STATUS_LEFT)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_NORMAL)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_REMOVED)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_DISABLED),
                ResultErrorCode.ILLEGAL_ARGUMENT, "成员状态不合法");
    }

    private void validateConversationStatus(Integer status) {
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(status, ChatConstants.CONVERSATION_STATUS_DISABLED)
                && !Objects.equals(status, ChatConstants.CONVERSATION_STATUS_NORMAL), ResultErrorCode.ILLEGAL_ARGUMENT, "后台只支持将会话状态更新为禁用或正常");
    }

    private void releaseFileReferencesForMessage(ChatMessage message) {
        if (message == null || !chatPayloadHelper.isAttachmentMessageType(message.getMessageType())) {
            return;
        }
        fileChatFacadeService.releaseReferences(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE, message.getId());
    }

    private ChatMessageVO buildMessagePushVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setFile(chatPayloadHelper.extractFilePayload(message.getPayloadJson()));
        vo.setReplyMessageId(message.getReplyMessageId());
        vo.setReply(resolveReplySnapshot(message.getReplyMessageId(), chatPayloadHelper.extractReplyPayload(message.getPayloadJson()),
                loadAdminReplySnapshot(message.getConversationId(), message.getReplyMessageId())));
        SysUser sender = loadUsers(Set.of(message.getSenderId())).get(message.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(UserDisplayNameUtils.resolveDisplayName(sender, message.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setRevoked(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED));
        vo.setEdited(chatPayloadHelper.isEdited(message.getMessageType(), message.getCreatedAt(), message.getUpdatedAt()));
        vo.setUpdatedAt(message.getUpdatedAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        Map<Long, SysUser> userMap = new HashMap<>();
        sysUserRepository.listByIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        return userMap;
    }

    private ChatReplyMessageVO loadAdminReplySnapshot(Long conversationId, Long replyMessageId) {
        if (replyMessageId == null) return null;
        return chatAdminQueryService.getMessageDetail(conversationId, replyMessageId).getReply();
    }

    private ChatReplyMessageVO resolveReplySnapshot(Long replyMessageId, ChatReplyMessageVO payloadReply, ChatReplyMessageVO fallbackReply) {
        if (replyMessageId == null) return null;
        if (fallbackReply != null && !Boolean.TRUE.equals(fallbackReply.getDeleted())) return fallbackReply;
        if (payloadReply != null) return payloadReply;
        return fallbackReply != null ? fallbackReply : chatPayloadHelper.buildUnavailableReplySnapshot(replyMessageId);
    }
}
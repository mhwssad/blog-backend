package com.cybzacg.blogbackend.module.chat.message.service.impl;

import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.chat.*;
import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.chat.attachment.service.ChatAttachmentAsyncProcessingService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMetricsService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMuteGovernanceService;
import com.cybzacg.blogbackend.module.chat.member.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatSendFileRequest;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.message.service.ChatMessageGovernanceService;
import com.cybzacg.blogbackend.module.chat.message.service.ChatMessageSendService;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatServiceSupport;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 消息发送子服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatMessageSendServiceImpl implements ChatMessageSendService {

    private final ChatServiceSupport s;
    private final ChatPushService chatPushService;
    private final ChatWebSocketSessionRegistry chatWebSocketSessionRegistry;
    private final FileChatFacadeService fileChatFacadeService;
    private final ChatAttachmentAsyncProcessingService chatAttachmentAsyncProcessingService;
    private final ChatMessageGovernanceService chatMessageGovernanceService;
    private final ChatMetricsService chatMetricsService;
    private final ChatMuteGovernanceService chatMuteGovernanceService;
    private final ChatNotificationService chatNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserExperienceService userExperienceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request) {
        try {
            validateSendRequest(request);
            chatMessageGovernanceService.validateTextMessage(userId, request.getContent());
            ChatConversation conversation = resolveSendConversation(userId, request);
            ChatServiceSupport.ConversationAccessContext context = s.requireConversationAccess(userId, conversation.getId());
            validateMemberCanSend(userId, context.selfMember(), context.conversation());
            validateConversationSpeakPermission(userId, context.conversation());
            validateSlowMode(userId, context.conversation());
            List<ChatConversationMember> activeMembers = context.activeMembers();
            ChatMessageHistoryItem existing = s.findExistingMessage(userId, request.getClientMessageId(), conversation.getId());
            if (existing != null) {
                ChatMessageVO existingMessage = s.buildMessageVO(userId, existing, s.loadUsers(Set.of(existing.getSenderId())));
                chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
                return existingMessage;
            }
            ChatMessageHistoryItem replyMessage = resolveReplyMessage(userId, conversation.getId(), request.getReplyMessageId());

            ChatMessage message = s.getModelConvert().toTextMessage(request);
            message.setConversationId(conversation.getId());
            message.setSenderId(userId);
            message.setReplyMessageId(replyMessage != null ? replyMessage.getId() : null);
            message.setPayloadJson(s.buildMessagePayloadJson(null, s.buildReplySnapshot(replyMessage)));
            try {
                s.getMessageRepository().save(message);
            } catch (DuplicateKeyException ex) {
                ChatMessageVO existingMessage = resolveDuplicateClientMessage(userId, request.getClientMessageId(), conversation.getId(), ex);
                chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
                return existingMessage;
            }

            LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageTime(now);
            s.getConversationRepository().updateById(conversation);

            List<Long> activeUserIds = activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
            persistRecipients(message, activeUserIds, userId, now);
            updateSenderCursorAfterSend(conversation, userId, message.getId(), now);
            incrementUnreadForRecipients(conversation.getId(), activeUserIds, userId);
            markDeliveredForOnlineRecipients(conversation.getId(), activeUserIds, userId, message.getId(), now);

            ChatMessageHistoryItem item = s.requireVisibleMessage(userId, conversation.getId(), message.getId());
            ChatMessageVO messageVO = s.buildMessageVO(userId, item, s.loadUsers(Set.of(userId)));
            chatPushService.pushMessageCreated(messageVO, activeUserIds);
            chatNotificationService.deliverMessageNotifications(conversation, message, userId, activeMembers, request.getContent());
            chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
            eventPublisher.publishEvent(new XpAwardEvent(
                    userId, ExperienceSourceTypeEnum.CHAT_MESSAGE.getValue(),
                    String.valueOf(message.getId()),
                    "chat_message:" + userId + ":" + message.getId()));
            return messageVO;
        } catch (RuntimeException ex) {
            chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, s.metricResultOf(ex));
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendFileMessage(Long userId, ChatSendFileRequest request) {
        String metricMessageType = ChatConstants.MESSAGE_TYPE_FILE;
        try {
            validateSendFileRequest(request);
            chatMessageGovernanceService.validateAttachmentMessage(userId);
            ChatConversation conversation = resolveSendConversation(userId, request.getConversationId(), request.getTargetUserId());
            ChatServiceSupport.ConversationAccessContext context = s.requireConversationAccess(userId, conversation.getId());
            validateMemberCanSend(userId, context.selfMember(), context.conversation());
            validateConversationSpeakPermission(userId, context.conversation());
            validateSlowMode(userId, context.conversation());
            List<ChatConversationMember> activeMembers = context.activeMembers();
            ChatMessageHistoryItem existing = s.findExistingMessage(userId, request.getClientMessageId(), conversation.getId());
            if (existing != null) {
                ChatMessageVO existingMessage = s.buildMessageVO(userId, existing, s.loadUsers(Set.of(existing.getSenderId())));
                chatMetricsService.recordSend(existingMessage.getMessageType(), "success");
                return existingMessage;
            }

            ChatServiceSupport.PreparedFileMessage preparedFile = prepareFileMessage(userId, request.getBusinessId());
            metricMessageType = s.resolveAttachmentMessageType(preparedFile.fileInfo());
            ChatMessageHistoryItem replyMessage = resolveReplyMessage(userId, conversation.getId(), request.getReplyMessageId());
            Long replyMessageId = replyMessage != null ? replyMessage.getId() : null;
            ChatMessage message = buildFileMessage(conversation.getId(), userId, request.getClientMessageId(), replyMessageId, preparedFile.fileInfo());
            try {
                s.getMessageRepository().save(message);
            } catch (DuplicateKeyException ex) {
                ChatMessageVO existingMessage = resolveDuplicateClientMessage(userId, request.getClientMessageId(), conversation.getId(), ex);
                chatMetricsService.recordSend(existingMessage.getMessageType(), "success");
                return existingMessage;
            }
            ChatFilePayloadVO filePayload = bindFileReferenceToMessage(preparedFile, message.getId(), message.getMessageType());
            message.setPayloadJson(s.buildMessagePayloadJson(filePayload, s.buildReplySnapshot(replyMessage)));
            s.getMessageRepository().updateById(message);

            LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageTime(now);
            s.getConversationRepository().updateById(conversation);

            List<Long> activeUserIds = activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
            persistRecipients(message, activeUserIds, userId, now);
            updateSenderCursorAfterSend(conversation, userId, message.getId(), now);
            incrementUnreadForRecipients(conversation.getId(), activeUserIds, userId);
            markDeliveredForOnlineRecipients(conversation.getId(), activeUserIds, userId, message.getId(), now);

            ChatMessageHistoryItem item = s.requireVisibleMessage(userId, conversation.getId(), message.getId());
            ChatMessageVO messageVO = s.buildMessageVO(userId, item, s.loadUsers(Set.of(userId)));
            chatPushService.pushMessageCreated(messageVO, activeUserIds);
            chatAttachmentAsyncProcessingService.scheduleAfterCommit(message.getId(), messageVO, activeUserIds);
            chatNotificationService.deliverMessageNotifications(conversation, message, userId, activeMembers, null);
            chatMetricsService.recordSend(metricMessageType, "success");
            return messageVO;
        } catch (RuntimeException ex) {
            chatMetricsService.recordSend(metricMessageType, s.metricResultOf(ex));
            throw ex;
        }
    }

    // ========== Private helpers ==========

    private ChatConversation resolveSendConversation(Long userId, ChatSendTextRequest request) {
        return resolveSendConversation(userId, request.getConversationId(), request.getTargetUserId());
    }

    private ChatConversation resolveSendConversation(Long userId, Long conversationId, Long targetUserId) {
        if (conversationId != null) {
            return s.requireConversationAccess(userId, conversationId).conversation();
        }
        ExceptionThrowerCore.throwBusinessIfNull(targetUserId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
        SysUser targetUser = s.requireActiveUser(targetUserId, true);
        return ensureSingleConversation(userId, targetUser.getId());
    }

    private ChatConversation ensureSingleConversation(Long userId, Long targetUserId) {
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, targetUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能给自己发送单聊消息");
        String pairKey = s.buildSinglePairKey(userId, targetUserId);
        ChatConversation conversation = s.getConversationRepository().findBySinglePairKey(pairKey);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
            conversation.setSinglePairKey(pairKey);
            conversation.setIsAllSite(0);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                s.getConversationRepository().save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation = s.getConversationRepository().findBySinglePairKey(pairKey);
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "单聊会话创建失败");
        if (!Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL)) {
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            s.getConversationRepository().updateById(conversation);
        }
        s.upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        s.upsertConversationMembership(conversation, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        return conversation;
    }

    private static final int MAX_MESSAGE_CONTENT_LENGTH = 5000;

    private void validateSendRequest(ChatSendTextRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "发送参数不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(StrUtils.trimToNull(request.getContent()), ResultErrorCode.ILLEGAL_ARGUMENT, "消息内容不能为空");
        ExceptionThrowerCore.throwBusinessIf(
                request.getContent() != null && request.getContent().length() > MAX_MESSAGE_CONTENT_LENGTH,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "消息内容长度不能超过" + MAX_MESSAGE_CONTENT_LENGTH + "个字符"
        );
        ExceptionThrowerCore.throwBusinessIf(request.getConversationId() == null && request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
    }

    private void validateSendFileRequest(ChatSendFileRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "发送参数不能为空");
        ExceptionThrowerCore.throwBusinessIfNull(request.getBusinessId(), ResultErrorCode.ILLEGAL_ARGUMENT, "文件业务引用ID不能为空");
        ExceptionThrowerCore.throwBusinessIf(request.getConversationId() == null && request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
    }

    private void validateMemberCanSend(Long userId, ChatConversationMember selfMember, ChatConversation conversation) {
        // 兼容旧禁言：成员级 muteUntil 字段
        LocalDateTime muteUntil = selfMember == null ? null : selfMember.getMuteUntil();
        ExceptionThrowerCore.throwBusinessIf(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()),
                ResultErrorCode.FORBIDDEN,
                "当前用户已被禁言，暂时不能发送消息");

        // 统一禁言记录检查
        String scope = resolveMuteScope(conversation);
        if (scope != null) {
            ExceptionThrowerCore.throwBusinessIf(
                    chatMuteGovernanceService.isUserMuted(userId, conversation.getId(), scope),
                    ResultErrorCode.CHAT_USER_MUTED);
        }
    }

    /**
     * 根据会话 sceneType 映射到禁言 scope。
     * 单聊不禁言，返回 null。
     */
    private String resolveMuteScope(ChatConversation conversation) {
        if (conversation == null) return null;
        String sceneType = conversation.getSceneType();
        if (sceneType == null) return null;
        return switch (sceneType) {
            case ChatConstants.SCENE_TYPE_HALL_CHANNEL, ChatConstants.SCENE_TYPE_GLOBAL_CHANNEL -> "lobby";
            case ChatConstants.SCENE_TYPE_TOPIC_CHANNEL -> "topic_channel";
            case ChatConstants.SCENE_TYPE_USER_GROUP -> "group";
            default -> null;
        };
    }

    private void validateSlowMode(Long userId, ChatConversation conversation) {
        Integer slowModeSeconds = conversation.getSlowModeSeconds();
        if (slowModeSeconds == null || slowModeSeconds <= 0) {
            return;
        }
        ChatMessage lastMessage = s.getMessageRepository().findLatestBySenderAndConversation(userId, conversation.getId());
        if (lastMessage == null || lastMessage.getCreatedAt() == null) {
            return;
        }
        LocalDateTime earliestNext = lastMessage.getCreatedAt().plusSeconds(slowModeSeconds);
        ExceptionThrowerCore.throwBusinessIf(
                earliestNext.isAfter(LocalDateTime.now()),
                ResultErrorCode.FORBIDDEN,
                "当前会话处于慢速模式，请等待 " + slowModeSeconds + " 秒后再发言"
        );
    }

    private void validateConversationSpeakPermission(Long userId, ChatConversation conversation) {
        if (userId == null || conversation == null) {
            return;
        }
        int requiredLevel = s.resolveConversationSpeakRequiredLevel(conversation);
        if (requiredLevel <= 1) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                !userExperienceService.checkLevelPermission(userId, requiredLevel),
                ResultErrorCode.FORBIDDEN,
                "当前等级不足，至少达到 Lv." + requiredLevel + " 才能在该会话发言"
        );
    }

    private ChatServiceSupport.PreparedFileMessage prepareFileMessage(Long userId, Long businessId) {
        return new ChatServiceSupport.PreparedFileMessage(
                businessId,
                fileChatFacadeService.requireSendableChatFile(userId, businessId, ChatConstants.FILE_MESSAGE_REFERENCE_TYPE)
        );
    }

    private ChatMessage buildFileMessage(Long conversationId,
                                         Long userId,
                                         String clientMessageId,
                                         Long replyMessageId,
                                         FileInfo fileInfo) {
        String messageType = s.resolveAttachmentMessageType(fileInfo);
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessageType(messageType);
        message.setContent(s.buildFileMessageSummary(messageType, fileInfo));
        message.setSendStatus(ChatConstants.SEND_STATUS_SENT);
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);
        message.setMentionAll(0);
        message.setReplyMessageId(replyMessageId);
        message.setClientMessageId(StrUtils.trimToNull(clientMessageId));
        return message;
    }

    private ChatFilePayloadVO bindFileReferenceToMessage(ChatServiceSupport.PreparedFileMessage preparedFile, Long messageId, String messageType) {
        FileInfo fileInfo = preparedFile.fileInfo();
        FileBusinessInfo chatReference = fileChatFacadeService.bindChatMessageReference(
                SecurityUtils.requireUserId(),
                preparedFile.sourceBusinessId(),
                messageId,
                ChatConstants.FILE_MESSAGE_REFERENCE_TYPE,
                ChatConstants.FILE_MESSAGE_CATEGORY
        );
        return s.buildFilePayload(chatReference, fileInfo, messageType);
    }

    private ChatMessageVO resolveDuplicateClientMessage(Long userId,
                                                        String clientMessageId,
                                                        Long conversationId,
                                                        DuplicateKeyException ex) {
        ChatMessageHistoryItem existing = s.findExistingMessage(userId, clientMessageId, conversationId);
        if (existing == null) {
            throw ex;
        }
        return s.buildMessageVO(userId, existing, s.loadUsers(Set.of(existing.getSenderId())));
    }

    private ChatMessageHistoryItem resolveReplyMessage(Long userId, Long conversationId, Long replyMessageId) {
        if (replyMessageId == null) {
            return null;
        }
        return s.requireVisibleMessage(userId, conversationId, replyMessageId);
    }

    private void persistRecipients(ChatMessage message, List<Long> userIds, Long senderId, LocalDateTime now) {
        List<ChatMessageRecipient> recipients = new ArrayList<>();
        for (Long recipientUserId : userIds) {
            ChatMessageRecipient recipient = new ChatMessageRecipient();
            recipient.setMessageId(message.getId());
            recipient.setConversationId(message.getConversationId());
            recipient.setRecipientUserId(recipientUserId);
            recipient.setReceiveType("normal");
            recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);
            if (Objects.equals(recipientUserId, senderId)) {
                recipient.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
                recipient.setDeliveredAt(now);
                recipient.setReadAt(now);
            } else {
                recipient.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_PENDING);
            }
            recipients.add(recipient);
        }
        s.getMessageRecipientRepository().saveBatch(recipients);
    }

    private void updateSenderCursorAfterSend(ChatConversation conversation, Long senderId, Long messageId, LocalDateTime now) {
        ChatMessageReadCursor cursor = s.getOrCreateCursor(conversation.getId(), senderId, messageId, now);
        cursor.setReadMessageId(messageId);
        cursor.setReadAt(now);
        cursor.setDeliveredMessageId(messageId);
        cursor.setDeliveredAt(now);
        cursor.setUnreadCount(0);
        s.saveOrUpdateCursor(cursor);
        ChatConversationMember member = s.findMember(conversation.getId(), senderId);
        if (member != null) {
            member.setLastReadMessageId(messageId);
            member.setLastReadAt(now);
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(now);
            s.getConversationMemberRepository().updateById(member);
        }
    }

    private void incrementUnreadForRecipients(Long conversationId, List<Long> userIds, Long senderId) {
        for (Long recipientUserId : userIds) {
            if (Objects.equals(recipientUserId, senderId)) {
                continue;
            }
            ChatMessageReadCursor cursor = s.getOrCreateCursor(conversationId, recipientUserId, null, null);
            cursor.setUnreadCount(Objects.requireNonNullElse(cursor.getUnreadCount(), 0) + 1);
            s.saveOrUpdateCursor(cursor);
        }
    }

    private void markDeliveredForOnlineRecipients(Long conversationId,
                                                  List<Long> userIds,
                                                  Long senderId,
                                                  Long messageId,
                                                  LocalDateTime now) {
        for (Long recipientUserId : userIds) {
            if (Objects.equals(recipientUserId, senderId) || chatWebSocketSessionRegistry.getSessions(recipientUserId).isEmpty()) {
                continue;
            }
            s.getMessageRecipientRepository().markDelivered(conversationId, recipientUserId, messageId, now);
            s.advanceCursorDeliveredState(conversationId, recipientUserId, messageId, now);
            ChatConversationMember member = s.findMember(conversationId, recipientUserId);
            s.advanceMemberDeliveredState(member, messageId, now);
        }
    }
}

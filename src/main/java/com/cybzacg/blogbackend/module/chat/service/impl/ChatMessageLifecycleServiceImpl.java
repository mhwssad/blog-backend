package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.*;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.user.*;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMessageDeletedPayload;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageLifecycleService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.support.ChatPayloadHelper;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 消息生命周期子服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatMessageLifecycleServiceImpl implements ChatMessageLifecycleService {

    private final ChatServiceSupport s;
    private final ChatPushService chatPushService;
    private final FileChatFacadeService fileChatFacadeService;
    private final ChatPayloadHelper chatPayloadHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO editMessage(Long userId, Long messageId, ChatEditMessageRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "编辑参数不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(StrUtils.trimToNull(request.getContent()), ResultErrorCode.ILLEGAL_ARGUMENT, "消息内容不能为空");
        ChatMessage message = requireEditableOwnTextMessage(userId, messageId);
        message.setContent(StrUtils.trim(request.getContent()));
        s.getMessageRepository().updateById(message);
        ChatMessageHistoryItem item = s.requireVisibleMessage(userId, message.getConversationId(), messageId);
        ChatMessageVO messageVO = s.buildMessageVO(userId, item, s.loadUsers(Set.of(message.getSenderId())));
        chatPushService.pushMessageUpdated(messageVO, s.listActiveUserIds(message.getConversationId()));
        return messageVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMessage(Long userId, Long messageId) {
        ChatServiceSupport.RevocableMessageContext revokeContext = requireRevocableMessage(userId, messageId);
        List<Long> activeUserIds = s.listActiveUserIds(revokeContext.message().getConversationId());
        revokeMessageInternal(revokeContext.message(), userId);
        ChatMessageHistoryItem item = s.requireVisibleMessage(userId, revokeContext.message().getConversationId(), messageId);
        ChatMessageVO messageVO = s.buildMessageVO(userId, item, s.loadUsers(Set.of(revokeContext.message().getSenderId())));
        chatPushService.pushMessageRevoked(messageVO, activeUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long userId, Long messageId) {
        ChatMessage message = s.requireVisibleMessageEntity(userId, messageId);
        s.getMessageRecipientRepository().hideMessage(message.getConversationId(), userId, messageId);
        ChatMessageReadCursor cursor = s.getOrCreateCursor(message.getConversationId(), userId, null, null);
        int unreadCount = (int) s.countUnread(message.getConversationId(), userId);
        cursor.setUnreadCount(unreadCount);
        s.saveOrUpdateCursor(cursor);
        chatPushService.pushMessageDeleted(ChatWsMessageDeletedPayload.builder()
                .conversationId(message.getConversationId())
                .messageId(messageId)
                .userId(userId)
                .unreadCount(unreadCount)
                .build(), List.of(userId));
    }

    @Override
    public PageResult<ChatMessageVO> pageMyMessages(Long userId, Long conversationId, ChatMessagePageQuery query) {
        s.requireConversationAccess(userId, conversationId);
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Long beforeMessageId = query.getBeforeMessageId();
        long total = Objects.requireNonNullElse(s.getMessageRepository().countMessagePage(conversationId, userId, beforeMessageId), 0L);
        if (total == 0L) {
            return PageResult.<ChatMessageVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatMessageHistoryItem> items = new ArrayList<>(s.getMessageRepository().selectMessagePage(conversationId, userId, beforeMessageId, offset, size));
        markMessagesDelivered(userId, conversationId, items);
        Collections.reverse(items);
        Map<Long, SysUser> userMap = s.loadUsers(s.collectSenderIds(items));
        Map<Long, ChatReplyMessageVO> replySnapshots = s.loadReplySnapshotsForVisibleMessages(userId, conversationId, s.collectReplyMessageIds(items));
        List<ChatMessageVO> records = items.stream().map(item -> s.buildMessageVO(userId, item, userMap, replySnapshots)).toList();
        return PageResult.<ChatMessageVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long userId, Long conversationId, ChatMarkReadRequest request) {
        return markRead(userId, conversationId, request.getReadMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long userId, Long conversationId, Long readMessageId) {
        ExceptionThrowerCore.throwBusinessIfNull(readMessageId, ResultErrorCode.ILLEGAL_ARGUMENT, "已读消息ID不能为空");
        ChatServiceSupport.ConversationAccessContext context = s.requireConversationAccess(userId, conversationId);
        ChatMessageHistoryItem message = s.requireVisibleMessage(userId, conversationId, readMessageId);
        ChatMessageReadCursor cursor = s.getOrCreateCursor(conversationId, userId, null, null);
        if (cursor.getReadMessageId() != null && cursor.getReadMessageId() >= message.getId()) {
            ChatReadStateVO state = s.toReadStateVO(cursor, userId);
            chatPushService.pushReadUpdated(state, context.activeUserIds());
            return state;
        }

        LocalDateTime now = LocalDateTime.now();
        s.getMessageRecipientRepository().markReadUpTo(conversationId, userId, message.getId(), now);

        long unread = s.countUnread(conversationId, userId);
        cursor.setReadMessageId(message.getId());
        cursor.setReadAt(now);
        if (cursor.getDeliveredMessageId() == null || cursor.getDeliveredMessageId() < message.getId()) {
            cursor.setDeliveredMessageId(message.getId());
            cursor.setDeliveredAt(now);
        }
        cursor.setUnreadCount((int) unread);
        s.saveOrUpdateCursor(cursor);
        s.updateMemberReadState(context.selfMember(), message.getId(), now);

        ChatReadStateVO state = s.toReadStateVO(cursor, userId);
        chatPushService.pushReadUpdated(state, context.activeUserIds());
        return state;
    }

    // ========== Private helpers ==========

    private ChatMessage requireEditableOwnTextMessage(Long userId, Long messageId) {
        ChatMessage message = s.requireVisibleMessageEntity(userId, messageId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getSenderId(), userId), ResultErrorCode.FORBIDDEN, "只能编辑自己发送的消息");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getMessageType(), ChatConstants.MESSAGE_TYPE_TEXT), ResultErrorCode.ILLEGAL_ARGUMENT, "只有文本消息支持编辑");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED), ResultErrorCode.ILLEGAL_ARGUMENT, "已撤回消息不能编辑");
        return message;
    }

    private ChatServiceSupport.RevocableMessageContext requireRevocableMessage(Long userId, Long messageId) {
        ChatMessage message = s.requireVisibleMessageEntity(userId, messageId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getSenderId(), userId), ResultErrorCode.FORBIDDEN, "只能撤回自己发送的消息");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED), ResultErrorCode.ILLEGAL_ARGUMENT, "消息已撤回");
        return new ChatServiceSupport.RevocableMessageContext(message);
    }

    private void revokeMessageInternal(ChatMessage message, Long operatorUserId) {
        LocalDateTime now = LocalDateTime.now();
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_REVOKED);
        message.setRevokedBy(operatorUserId);
        message.setRevokedAt(now);
        message.setContent(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER);
        message.setPayloadJson(null);
        s.getMessageRepository().updateById(message);
        if (!chatPayloadHelper.isAttachmentMessageType(message.getMessageType())) {
            return;
        }
        fileChatFacadeService.releaseReferences(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE, message.getId());
    }

    private void markMessagesDelivered(Long userId, Long conversationId, List<ChatMessageHistoryItem> items) {
        List<Long> messageIds = items.stream()
                .filter(item -> !Objects.equals(item.getSenderId(), userId))
                .filter(item -> item.getDeliveryStatus() != null && item.getDeliveryStatus() < ChatConstants.DELIVERY_STATUS_DELIVERED)
                .map(ChatMessageHistoryItem::getId)
                .toList();
        if (messageIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        s.getMessageRecipientRepository().batchMarkDelivered(conversationId, userId, messageIds, now);
        Long maxMessageId = Collections.max(messageIds);
        s.advanceCursorDeliveredState(conversationId, userId, maxMessageId, now);
        ChatConversationMember member = s.findMember(conversationId, userId);
        s.advanceMemberDeliveredState(member, maxMessageId, now);
        for (ChatMessageHistoryItem item : items) {
            if (messageIds.contains(item.getId())) {
                item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_DELIVERED);
            }
        }
    }
}

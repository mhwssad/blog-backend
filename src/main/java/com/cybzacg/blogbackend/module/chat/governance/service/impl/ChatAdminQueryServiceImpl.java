package com.cybzacg.blogbackend.module.chat.governance.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.domain.chat.ChatMessageRecipient;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminQueryService;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.message.model.admin.*;
import com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRecipientRepository;
import com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatMemberHelper;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatPayloadHelper;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 后台聊天查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatAdminQueryServiceImpl implements ChatAdminQueryService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final ChatMessageRecipientRepository chatMessageRecipientRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelMapper chatModelMapper;
    private final ChatPayloadHelper chatPayloadHelper;
    private final ChatMemberHelper chatMemberHelper;

    @Override
    public PageResult<ChatAdminConversationVO> pageConversations(ChatAdminConversationPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 10L, 100L);
        long total = Objects.requireNonNullElse(chatConversationRepository.countAdminConversationPage(query), 0L);
        if (total == 0L) {
            return PageResult.<ChatAdminConversationVO>builder()
                    .total(0L).current(current).size(size).records(List.of()).build();
        }
        long offset = (current - 1) * size;
        List<ChatAdminConversationListItem> items = chatConversationRepository.selectAdminConversationPage(query, offset, size);
        return PageResult.<ChatAdminConversationVO>builder()
                .total(total).current(current).size(size)
                .records(buildConversationRecords(items)).build();
    }

    @Override
    public ChatAdminConversationVO getConversation(Long conversationId) {
        requireConversation(conversationId);
        ChatAdminConversationListItem item = chatConversationRepository.selectAdminConversationDetail(conversationId);
        ExceptionThrowerCore.throwBusinessIfNull(item, ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在");
        return buildConversationVO(item, listMembersByConversationIds(List.of(conversationId)).getOrDefault(conversationId, List.of()));
    }

    @Override
    public List<ChatMemberVO> listMembers(Long conversationId) {
        requireConversation(conversationId);
        return buildMemberRecords(listConversationMembers(conversationId));
    }

    @Override
    public PageResult<ChatAdminMessageVO> pageMessages(Long conversationId, ChatAdminMessagePageQuery query) {
        requireConversation(conversationId);
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        long total = Objects.requireNonNullElse(chatMessageRepository.countAdminMessagePage(conversationId, query), 0L);
        if (total == 0L) {
            return PageResult.<ChatAdminMessageVO>builder()
                    .total(0L).current(current).size(size).records(List.of()).build();
        }
        long offset = (current - 1) * size;
        List<ChatAdminMessageItem> items = chatMessageRepository.selectAdminMessagePage(conversationId, query, offset, size);
        Map<Long, SysUser> userMap = loadUsers(items.stream().map(ChatAdminMessageItem::getSenderId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        Map<Long, ChatReplyMessageVO> replySnapshots = loadAdminReplySnapshots(conversationId, collectReplyMessageIds(items));
        List<ChatAdminMessageVO> records = items.stream().map(item -> buildMessageVO(item, userMap, replySnapshots)).toList();
        return PageResult.<ChatAdminMessageVO>builder()
                .total(total).current(current).size(size).records(records).build();
    }

    @Override
    public ChatAdminMessageDetailVO getMessageDetail(Long conversationId, Long messageId) {
        requireConversation(conversationId);
        ChatMessage message = requireMessage(conversationId, messageId);
        List<ChatMessageRecipient> recipients = listMessageRecipients(messageId);
        Map<Long, SysUser> userMap = loadUsers(Set.of(message.getSenderId()));
        SysUser sender = userMap.get(message.getSenderId());
        ChatAdminMessageDetailVO vo = new ChatAdminMessageDetailVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(UserDisplayNameUtils.resolveDisplayName(sender, message.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setFile(chatPayloadHelper.extractFilePayload(message.getPayloadJson()));
        vo.setReplyMessageId(message.getReplyMessageId());
        vo.setReply(resolveReplySnapshot(message.getReplyMessageId(), chatPayloadHelper.extractReplyPayload(message.getPayloadJson()), loadAdminReplySnapshot(conversationId, message.getReplyMessageId())));
        vo.setClientMessageId(message.getClientMessageId());
        vo.setSendStatus(message.getSendStatus());
        vo.setRevokeStatus(message.getRevokeStatus());
        vo.setRevokedBy(message.getRevokedBy());
        vo.setRevokedAt(message.getRevokedAt());
        vo.setTotalRecipientCount((long) recipients.size());
        vo.setDeliveredRecipientCount(recipients.stream().filter(this::isDelivered).count());
        vo.setReadRecipientCount(recipients.stream().filter(this::isRead).count());
        vo.setEdited(chatPayloadHelper.isEdited(message.getMessageType(), message.getCreatedAt(), message.getUpdatedAt()));
        vo.setUpdatedAt(message.getUpdatedAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    @Override
    public PageResult<ChatAdminMessageReceiptVO> pageMessageReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query) {
        requireConversation(conversationId);
        requireMessage(conversationId, messageId);
        ChatAdminMessageReceiptPageQuery safeQuery = query == null ? new ChatAdminMessageReceiptPageQuery() : query;
        long current = PaginationUtils.normalizeCurrent(safeQuery.getCurrent());
        long size = PaginationUtils.normalizeSize(safeQuery.getSize(), 20L, 100L);
        Page<ChatMessageRecipient> page = chatMessageRecipientRepository.pageAdminReceipts(conversationId, messageId, safeQuery);
        Map<Long, SysUser> userMap = loadUsers(page.getRecords().stream()
                .map(ChatMessageRecipient::getRecipientUserId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatAdminMessageReceiptVO> records = page.getRecords().stream()
                .map(recipient -> buildReceiptVO(recipient, userMap))
                .toList();
        return PageResult.<ChatAdminMessageReceiptVO>builder()
                .total(page.getTotal()).current(current).size(size).records(records).build();
    }

    // ==================== 私有辅助方法 ====================

    private List<ChatAdminConversationVO> buildConversationRecords(List<ChatAdminConversationListItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        Map<Long, List<ChatConversationMember>> membersByConversation = listMembersByConversationIds(items.stream().map(ChatAdminConversationListItem::getId).toList());
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatAdminConversationListItem item : items) {
            if (item.getOwnerId() != null) userIds.add(item.getOwnerId());
            if (item.getLastMessageSenderId() != null) userIds.add(item.getLastMessageSenderId());
            for (ChatConversationMember member : membersByConversation.getOrDefault(item.getId(), List.of())) {
                userIds.add(member.getUserId());
            }
        }
        Map<Long, SysUser> userMap = loadUsers(userIds);
        List<ChatAdminConversationVO> records = new ArrayList<>();
        for (ChatAdminConversationListItem item : items) {
            records.add(buildConversationVO(item, membersByConversation.getOrDefault(item.getId(), List.of()), userMap));
        }
        return records;
    }

    private ChatAdminConversationVO buildConversationVO(ChatAdminConversationListItem item, List<ChatConversationMember> members) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (item.getOwnerId() != null) userIds.add(item.getOwnerId());
        if (item.getLastMessageSenderId() != null) userIds.add(item.getLastMessageSenderId());
        members.forEach(member -> userIds.add(member.getUserId()));
        return buildConversationVO(item, members, loadUsers(userIds));
    }

    private ChatAdminConversationVO buildConversationVO(ChatAdminConversationListItem item, List<ChatConversationMember> members, Map<Long, SysUser> userMap) {
        ChatAdminConversationVO vo = chatModelMapper.toAdminConversationVO(item);
        vo.setMemberCount(Objects.requireNonNullElse(item.getMemberCount(), 0L));
        SysUser owner = userMap.get(item.getOwnerId());
        vo.setOwnerUsername(owner != null ? owner.getUsername() : null);
        vo.setOwnerNickname(owner != null ? owner.getNickname() : null);
        if (item.getLastMessageId() != null) {
            ChatConversationLastMessageVO lastMessage = chatModelMapper.toConversationLastMessageVO(item);
            SysUser sender = userMap.get(item.getLastMessageSenderId());
            lastMessage.setSenderNickname(UserDisplayNameUtils.resolveDisplayName(sender, item.getLastMessageSenderId()));
            vo.setLastMessage(lastMessage);
        }
        if (Objects.equals(item.getConversationType(), ChatConstants.CONVERSATION_TYPE_SINGLE) && !StrUtils.hasText(vo.getName())) {
            vo.setName(buildSingleConversationName(members, userMap));
        }
        return vo;
    }

    private ChatAdminMessageVO buildMessageVO(ChatAdminMessageItem item, Map<Long, SysUser> userMap, Map<Long, ChatReplyMessageVO> replySnapshots) {
        ChatAdminMessageVO vo = chatModelMapper.toAdminMessageVO(item);
        SysUser sender = userMap.get(item.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(UserDisplayNameUtils.resolveDisplayName(sender, item.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setFile(chatPayloadHelper.extractFilePayload(item.getPayloadJson()));
        vo.setReplyMessageId(item.getReplyMessageId());
        vo.setReply(resolveReplySnapshot(item.getReplyMessageId(), chatPayloadHelper.extractReplyPayload(item.getPayloadJson()),
                item.getReplyMessageId() == null ? null : replySnapshots.get(item.getReplyMessageId())));
        vo.setEdited(chatPayloadHelper.isEdited(item.getMessageType(), item.getCreatedAt(), item.getUpdatedAt()));
        return vo;
    }

    private ChatAdminMessageReceiptVO buildReceiptVO(ChatMessageRecipient recipient, Map<Long, SysUser> userMap) {
        ChatAdminMessageReceiptVO vo = new ChatAdminMessageReceiptVO();
        vo.setId(recipient.getId());
        vo.setMessageId(recipient.getMessageId());
        vo.setConversationId(recipient.getConversationId());
        vo.setRecipientUserId(recipient.getRecipientUserId());
        SysUser recipientUser = userMap.get(recipient.getRecipientUserId());
        vo.setRecipientUsername(recipientUser != null ? recipientUser.getUsername() : null);
        vo.setRecipientNickname(UserDisplayNameUtils.resolveDisplayName(recipientUser, recipient.getRecipientUserId()));
        vo.setRecipientAvatar(recipientUser != null ? recipientUser.getAvatar() : null);
        vo.setReceiveType(recipient.getReceiveType());
        vo.setDeliveryStatus(recipient.getDeliveryStatus());
        vo.setDeliveredAt(recipient.getDeliveredAt());
        vo.setReadAt(recipient.getReadAt());
        vo.setVisibleStatus(recipient.getVisibleStatus());
        vo.setCreatedAt(recipient.getCreatedAt());
        return vo;
    }

    private List<ChatMemberVO> buildMemberRecords(List<ChatConversationMember> members) {
        if (members == null || members.isEmpty()) return List.of();
        Map<Long, SysUser> userMap = loadUsers(members.stream().map(ChatConversationMember::getUserId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatMemberVO> records = new ArrayList<>();
        members.stream()
                .sorted(Comparator.comparingInt(chatMemberHelper::memberRoleOrder)
                        .thenComparing(ChatConversationMember::getStatus)
                        .thenComparing(ChatConversationMember::getJoinedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ChatConversationMember::getUserId))
                .forEach(member -> {
                    ChatMemberVO vo = chatModelMapper.toMemberVO(member);
                    SysUser user = userMap.get(member.getUserId());
                    vo.setUserId(member.getUserId());
                    vo.setUsername(user != null ? user.getUsername() : null);
                    vo.setNickname(user != null ? user.getNickname() : null);
                    vo.setAvatar(user != null ? user.getAvatar() : null);
                    records.add(vo);
                });
        return records;
    }

    private String buildSingleConversationName(List<ChatConversationMember> members, Map<Long, SysUser> userMap) {
        List<String> names = members.stream()
                .map(ChatConversationMember::getUserId)
                .distinct()
                .map(userId -> UserDisplayNameUtils.resolveDisplayName(userMap.get(userId), userId))
                .filter(StrUtils::hasText)
                .limit(2)
                .toList();
        if (names.isEmpty()) return null;
        return String.join(" / ", names);
    }

    private Map<Long, List<ChatConversationMember>> listMembersByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) return Map.of();
        List<ChatConversationMember> members = chatConversationMemberRepository.listByConversationIds(conversationIds);
        Map<Long, List<ChatConversationMember>> result = new HashMap<>();
        for (ChatConversationMember member : members) {
            result.computeIfAbsent(member.getConversationId(), key -> new ArrayList<>()).add(member);
        }
        return result;
    }

    private List<ChatConversationMember> listConversationMembers(Long conversationId) {
        return chatConversationMemberRepository.listByConversationId(conversationId);
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        Map<Long, SysUser> userMap = new HashMap<>();
        sysUserRepository.listByIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        return userMap;
    }

    private ChatConversation requireConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在");
        return conversation;
    }

    private ChatMessage requireMessage(Long conversationId, Long messageId) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        ChatMessage message = chatMessageRepository.getById(messageId);
        ExceptionThrowerCore.throwBusinessIf(message == null || !Objects.equals(message.getConversationId(), conversationId), ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在");
        return message;
    }

    private List<ChatMessageRecipient> listMessageRecipients(Long messageId) {
        return chatMessageRecipientRepository.listByMessageId(messageId);
    }

    private boolean isDelivered(ChatMessageRecipient recipient) {
        return recipient.getDeliveryStatus() != null && recipient.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_DELIVERED;
    }

    private boolean isRead(ChatMessageRecipient recipient) {
        return recipient.getDeliveryStatus() != null && recipient.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_READ;
    }

    private Map<Long, ChatReplyMessageVO> loadAdminReplySnapshots(Long conversationId, Collection<Long> replyMessageIds) {
        List<Long> ids = replyMessageIds == null ? List.of() : replyMessageIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        List<ChatAdminMessageItem> replyItems = Objects.requireNonNullElse(chatMessageRepository.selectAdminMessagesByIds(conversationId, ids), List.of());
        Map<Long, SysUser> userMap = loadUsers(replyItems.stream().map(ChatAdminMessageItem::getSenderId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        Map<Long, ChatReplyMessageVO> result = new LinkedHashMap<>();
        for (ChatAdminMessageItem replyItem : replyItems) {
            result.put(replyItem.getId(), buildReplySnapshot(replyItem, userMap));
        }
        for (Long id : ids) {
            result.putIfAbsent(id, chatPayloadHelper.buildUnavailableReplySnapshot(id));
        }
        return result;
    }

    private ChatReplyMessageVO loadAdminReplySnapshot(Long conversationId, Long replyMessageId) {
        if (replyMessageId == null) return null;
        return loadAdminReplySnapshots(conversationId, List.of(replyMessageId)).get(replyMessageId);
    }

    private ChatReplyMessageVO buildReplySnapshot(ChatAdminMessageItem item, Map<Long, SysUser> userMap) {
        ChatReplyMessageVO reply = new ChatReplyMessageVO();
        reply.setId(item.getId());
        reply.setSenderId(item.getSenderId());
        SysUser sender = userMap.get(item.getSenderId());
        reply.setSenderUsername(sender != null ? sender.getUsername() : null);
        reply.setSenderNickname(UserDisplayNameUtils.resolveDisplayName(sender, item.getSenderId()));
        reply.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        reply.setMessageType(item.getMessageType());
        reply.setReplyToMessageId(item.getReplyMessageId());
        reply.setContent(item.getContent());
        reply.setFile(chatPayloadHelper.extractFilePayload(item.getPayloadJson()));
        boolean revoked = Objects.equals(item.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED);
        reply.setRevoked(revoked);
        reply.setDeleted(false);
        reply.setState(revoked ? ChatConstants.REPLY_STATE_REVOKED : ChatConstants.REPLY_STATE_NORMAL);
        reply.setCreatedAt(item.getCreatedAt());
        return reply;
    }

    private ChatReplyMessageVO resolveReplySnapshot(Long replyMessageId, ChatReplyMessageVO payloadReply, ChatReplyMessageVO fallbackReply) {
        if (replyMessageId == null) return null;
        if (fallbackReply != null && !Boolean.TRUE.equals(fallbackReply.getDeleted())) return fallbackReply;
        if (payloadReply != null) return payloadReply;
        return fallbackReply != null ? fallbackReply : chatPayloadHelper.buildUnavailableReplySnapshot(replyMessageId);
    }

    private List<Long> collectReplyMessageIds(Collection<ChatAdminMessageItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream().map(ChatAdminMessageItem::getReplyMessageId).filter(Objects::nonNull).distinct().toList();
    }
}

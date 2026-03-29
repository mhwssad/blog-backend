package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.mapper.ChatConversationMapper;
import com.cybzacg.blogbackend.mapper.ChatMessageMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptPageQuery;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminService;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationMemberService;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageRecipientService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 聊天后台管理服务实现。
 *
 * <p>负责后台会话查询、成员查看、消息追踪和会话状态维护。
 */
@Service
@RequiredArgsConstructor
public class ChatAdminServiceImpl implements ChatAdminService {
    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatConversationService chatConversationService;
    private final ChatConversationMemberService chatConversationMemberService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageRecipientService chatMessageRecipientService;
    private final SysUserService sysUserService;
    private final ChatModelMapper chatModelMapper;
    private final ChatPushService chatPushService;
    private final FileBusinessInfoService fileBusinessInfoService;
    private final FileLifecycleService fileLifecycleService;

    @Override
    public PageResult<ChatAdminConversationVO> pageConversations(ChatAdminConversationPageQuery query) {
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize(), 10L, 100L);
        long total = Objects.requireNonNullElse(chatConversationMapper.countAdminConversationPage(query), 0L);
        if (total == 0L) {
            return PageResult.<ChatAdminConversationVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatAdminConversationListItem> items = chatConversationMapper.selectAdminConversationPage(query, offset, size);
        return PageResult.<ChatAdminConversationVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(buildConversationRecords(items))
                .build();
    }

    @Override
    public ChatAdminConversationVO getConversation(Long conversationId) {
        requireConversation(conversationId);
        ChatAdminConversationListItem item = chatConversationMapper.selectAdminConversationDetail(conversationId);
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
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize(), 20L, 100L);
        long total = Objects.requireNonNullElse(chatMessageMapper.countAdminMessagePage(conversationId, query), 0L);
        if (total == 0L) {
            return PageResult.<ChatAdminMessageVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatAdminMessageItem> items = chatMessageMapper.selectAdminMessagePage(conversationId, query, offset, size);
        Map<Long, SysUser> userMap = loadUsers(items.stream().map(ChatAdminMessageItem::getSenderId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatAdminMessageVO> records = items.stream().map(item -> buildMessageVO(item, userMap)).toList();
        return PageResult.<ChatAdminMessageVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
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
        vo.setSenderNickname(displayName(sender, message.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setFile(parseFilePayload(message.getPayloadJson()));
        vo.setReplyMessageId(message.getReplyMessageId());
        vo.setClientMessageId(message.getClientMessageId());
        vo.setSendStatus(message.getSendStatus());
        vo.setRevokeStatus(message.getRevokeStatus());
        vo.setRevokedBy(message.getRevokedBy());
        vo.setRevokedAt(message.getRevokedAt());
        vo.setTotalRecipientCount((long) recipients.size());
        vo.setDeliveredRecipientCount(recipients.stream().filter(this::isDelivered).count());
        vo.setReadRecipientCount(recipients.stream().filter(this::isRead).count());
        vo.setEdited(isEdited(message.getMessageType(), message.getCreatedAt(), message.getUpdatedAt()));
        vo.setUpdatedAt(message.getUpdatedAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    @Override
    public PageResult<ChatAdminMessageReceiptVO> pageMessageReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query) {
        requireConversation(conversationId);
        requireMessage(conversationId, messageId);
        ChatAdminMessageReceiptPageQuery safeQuery = query == null ? new ChatAdminMessageReceiptPageQuery() : query;
        long current = normalizeCurrent(safeQuery.getCurrent());
        long size = normalizeSize(safeQuery.getSize(), 20L, 100L);
        LambdaQueryWrapper<ChatMessageRecipient> wrapper = new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(safeQuery.getRecipientUserId() != null, ChatMessageRecipient::getRecipientUserId, safeQuery.getRecipientUserId())
                .eq(safeQuery.getDeliveryStatus() != null, ChatMessageRecipient::getDeliveryStatus, safeQuery.getDeliveryStatus())
                .eq(safeQuery.getVisibleStatus() != null, ChatMessageRecipient::getVisibleStatus, safeQuery.getVisibleStatus())
                .orderByDesc(ChatMessageRecipient::getId);
        Page<ChatMessageRecipient> page = chatMessageRecipientService.page(new Page<>(current, size), wrapper);
        Map<Long, SysUser> userMap = loadUsers(page.getRecords().stream()
                .map(ChatMessageRecipient::getRecipientUserId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatAdminMessageReceiptVO> records = page.getRecords().stream()
                .map(recipient -> buildReceiptVO(recipient, userMap))
                .toList();
        return PageResult.<ChatAdminMessageReceiptVO>builder()
                .total(page.getTotal())
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

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
                chatConversationMemberService.updateById(currentOwner);
            }
            conversation.setOwnerId(memberUserId);
            chatConversationService.updateById(conversation);
        } else if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER) && conversation.getOwnerId() != null
                && !Objects.equals(conversation.getOwnerId(), memberUserId)) {
            conversation.setOwnerId(null);
            chatConversationService.updateById(conversation);
        } else if (!Objects.equals(role, ChatConstants.MEMBER_ROLE_OWNER) && Objects.equals(conversation.getOwnerId(), memberUserId)) {
            conversation.setOwnerId(null);
            chatConversationService.updateById(conversation);
        }
        member.setMemberRole(role);
        chatConversationMemberService.updateById(member);
        List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(activeMembers);
        List<Long> activeUserIds = activeUserIds(activeMembers);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("admin_member_role_updated", conversationId, memberUserId, records), activeUserIds);
        chatPushService.pushConversationUpdated(buildConversationUpdatedPayload("admin_member_role_updated", conversation, activeMembers), activeUserIds);
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
            member.setJoinedAt(new Date());
        }
        chatConversationMemberService.updateById(member);
        List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(activeMembers);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("admin_member_status_updated", conversationId, memberUserId, records), notifyUserIds);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> updateMemberMute(Long conversationId, Long memberUserId, ChatAdminMemberMuteUpdateRequest request) {
        requireManageableGroupConversation(conversationId);
        ChatConversationMember member = requireMember(conversationId, memberUserId);
        Date muteUntil = request == null ? null : request.getMuteUntil();
        member.setMuteUntil(muteUntil != null && muteUntil.after(new Date()) ? muteUntil : null);
        chatConversationMemberService.updateById(member);
        List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(activeMembers);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("admin_member_mute_updated", conversationId, memberUserId, records), activeUserIds(activeMembers));
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
        Date now = new Date();
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_REVOKED);
        message.setRevokedBy(0L);
        message.setRevokedAt(now);
        message.setContent(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER);
        message.setPayloadJson(null);
        chatMessageService.updateById(message);
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
        chatConversationService.updateById(conversation);
        chatPushService.pushConversationUpdated(buildConversationUpdatedPayload("admin_conversation_status_updated", conversation, listActiveMembers(conversationId)),
                activeUserIds(listActiveMembers(conversationId)));
    }

    /**
     * 统一装配后台会话分页记录，补齐单聊名称、群主信息和最后一条消息摘要。
     */
    private List<ChatAdminConversationVO> buildConversationRecords(List<ChatAdminConversationListItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ChatConversationMember>> membersByConversation = listMembersByConversationIds(items.stream().map(ChatAdminConversationListItem::getId).toList());
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatAdminConversationListItem item : items) {
            if (item.getOwnerId() != null) {
                userIds.add(item.getOwnerId());
            }
            if (item.getLastMessageSenderId() != null) {
                userIds.add(item.getLastMessageSenderId());
            }
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

    private ChatAdminConversationVO buildConversationVO(ChatAdminConversationListItem item,
                                                        List<ChatConversationMember> members) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (item.getOwnerId() != null) {
            userIds.add(item.getOwnerId());
        }
        if (item.getLastMessageSenderId() != null) {
            userIds.add(item.getLastMessageSenderId());
        }
        members.forEach(member -> userIds.add(member.getUserId()));
        return buildConversationVO(item, members, loadUsers(userIds));
    }

    private ChatAdminConversationVO buildConversationVO(ChatAdminConversationListItem item,
                                                        List<ChatConversationMember> members,
                                                        Map<Long, SysUser> userMap) {
        ChatAdminConversationVO vo = chatModelMapper.toAdminConversationVO(item);
        vo.setMemberCount(Objects.requireNonNullElse(item.getMemberCount(), 0L));
        SysUser owner = userMap.get(item.getOwnerId());
        vo.setOwnerUsername(owner != null ? owner.getUsername() : null);
        vo.setOwnerNickname(owner != null ? owner.getNickname() : null);
        if (item.getLastMessageId() != null) {
            ChatConversationLastMessageVO lastMessage = chatModelMapper.toConversationLastMessageVO(item);
            SysUser sender = userMap.get(item.getLastMessageSenderId());
            lastMessage.setSenderNickname(displayName(sender, item.getLastMessageSenderId()));
            vo.setLastMessage(lastMessage);
        }
        if (Objects.equals(item.getConversationType(), ChatConstants.CONVERSATION_TYPE_SINGLE) && !StrUtils.hasText(vo.getName())) {
            vo.setName(buildSingleConversationName(members, userMap));
        }
        return vo;
    }

    private ChatAdminMessageVO buildMessageVO(ChatAdminMessageItem item, Map<Long, SysUser> userMap) {
        ChatAdminMessageVO vo = chatModelMapper.toAdminMessageVO(item);
        SysUser sender = userMap.get(item.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(displayName(sender, item.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setFile(parseFilePayload(item.getPayloadJson()));
        vo.setReplyMessageId(item.getReplyMessageId());
        vo.setEdited(isEdited(item.getMessageType(), item.getCreatedAt(), item.getUpdatedAt()));
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
        vo.setRecipientNickname(displayName(recipientUser, recipient.getRecipientUserId()));
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
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = loadUsers(members.stream().map(ChatConversationMember::getUserId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatMemberVO> records = new ArrayList<>();
        members.stream()
                .sorted(Comparator.comparingInt(this::memberRoleOrder)
                        .thenComparing(ChatConversationMember::getStatus)
                        .thenComparing(ChatConversationMember::getJoinedAt, Comparator.nullsLast(java.util.Date::compareTo))
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

    /**
     * 单聊会话在主表通常没有固定名称，后台列表按成员展示名拼出可读标题。
     */
    private String buildSingleConversationName(List<ChatConversationMember> members, Map<Long, SysUser> userMap) {
        List<String> names = members.stream()
                .map(ChatConversationMember::getUserId)
                .distinct()
                .map(userId -> displayName(userMap.get(userId), userId))
                .filter(StrUtils::hasText)
                .limit(2)
                .toList();
        if (names.isEmpty()) {
            return null;
        }
        return String.join(" / ", names);
    }

    private Map<Long, List<ChatConversationMember>> listMembersByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        List<ChatConversationMember> members = chatConversationMemberService.lambdaQuery()
                .in(ChatConversationMember::getConversationId, conversationIds)
                .list();
        Map<Long, List<ChatConversationMember>> result = new HashMap<>();
        for (ChatConversationMember member : members) {
            result.computeIfAbsent(member.getConversationId(), key -> new ArrayList<>()).add(member);
        }
        return result;
    }

    private List<ChatConversationMember> listConversationMembers(Long conversationId) {
        return chatConversationMemberService.lambdaQuery()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .list();
    }

    private List<ChatConversationMember> listActiveMembers(Long conversationId) {
        return chatConversationMemberService.lambdaQuery()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL)
                .list();
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        sysUserService.listByIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        return userMap;
    }

    private ChatConversation requireConversation(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationService.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在");
        return conversation;
    }

    private ChatConversation requireManageableGroupConversation(Long conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(conversation.getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP)
                        || Objects.equals(conversation.getIsAllSite(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "后台成员治理仅支持普通群聊会话");
        return conversation;
    }

    private ChatMessage requireMessage(Long conversationId, Long messageId) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        ChatMessage message = chatMessageService.getById(messageId);
        ExceptionThrowerCore.throwBusinessIf(message == null || !Objects.equals(message.getConversationId(), conversationId), ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在");
        return message;
    }

    private ChatConversationMember requireMember(Long conversationId, Long memberUserId) {
        ExceptionThrowerCore.throwBusinessIfNull(memberUserId, ResultErrorCode.ILLEGAL_ARGUMENT, "成员用户ID不能为空");
        ChatConversationMember member = chatConversationMemberService.lambdaQuery()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getUserId, memberUserId)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1")
                .one();
        ExceptionThrowerCore.throwBusinessIfNull(member, ResultErrorCode.ILLEGAL_ARGUMENT, "成员不存在");
        return member;
    }

    private ChatConversationMember findOwnerMember(Long conversationId) {
        return chatConversationMemberService.lambdaQuery()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getMemberRole, ChatConstants.MEMBER_ROLE_OWNER)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1")
                .one();
    }

    private List<ChatMessageRecipient> listMessageRecipients(Long messageId) {
        return chatMessageRecipientService.lambdaQuery()
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .list();
    }

    private String normalizeRole(ChatAdminMemberRoleUpdateRequest request) {
        String role = request == null ? null : StrUtils.trimToNull(request.getRole());
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(role, ChatConstants.MEMBER_ROLE_OWNER)
                        && !Objects.equals(role, ChatConstants.MEMBER_ROLE_ADMIN)
                        && !Objects.equals(role, ChatConstants.MEMBER_ROLE_MEMBER),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "成员角色不合法");
        return role;
    }

    private void validateMemberStatus(Integer status) {
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(status, ChatConstants.MEMBER_STATUS_LEFT)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_NORMAL)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_REMOVED)
                        && !Objects.equals(status, ChatConstants.MEMBER_STATUS_DISABLED),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "成员状态不合法");
    }

    private void validateConversationStatus(Integer status) {
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(status, ChatConstants.CONVERSATION_STATUS_DISABLED)
                && !Objects.equals(status, ChatConstants.CONVERSATION_STATUS_NORMAL), ResultErrorCode.ILLEGAL_ARGUMENT, "后台只支持将会话状态更新为禁用或正常");
    }

    private void releaseFileReferencesForMessage(ChatMessage message) {
        if (message == null || !isAttachmentMessageType(message.getMessageType())) {
            return;
        }
        List<FileBusinessInfo> references = fileBusinessInfoService.lambdaQuery()
                .eq(FileBusinessInfo::getReferenceType, ChatConstants.FILE_MESSAGE_REFERENCE_TYPE)
                .eq(FileBusinessInfo::getReferenceId, message.getId())
                .list();
        if (references.isEmpty()) {
            return;
        }
        Map<Long, Long> fileIds = new LinkedHashMap<>();
        for (FileBusinessInfo reference : references) {
            if (reference.getFileId() != null) {
                fileIds.put(reference.getFileId(), reference.getFileId());
            }
        }
        fileBusinessInfoService.removeByIds(references.stream().map(FileBusinessInfo::getId).toList());
        fileIds.values().forEach(fileLifecycleService::syncFileAfterReferenceRemoval);
    }

    private ChatFilePayloadVO parseFilePayload(String payloadJson) {
        if (!StrUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            return JsonUtils.fromJson(payloadJson, ChatFilePayloadVO.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isDelivered(ChatMessageRecipient recipient) {
        return recipient.getDeliveryStatus() != null && recipient.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_DELIVERED;
    }

    private boolean isRead(ChatMessageRecipient recipient) {
        return recipient.getDeliveryStatus() != null && recipient.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_READ;
    }

    private boolean isEdited(String messageType, Date createdAt, Date updatedAt) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_TEXT)
                && createdAt != null
                && updatedAt != null
                && updatedAt.after(createdAt);
    }

    private boolean isAttachmentMessageType(String messageType) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_FILE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE);
    }

    private List<Long> activeUserIds(List<ChatConversationMember> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream().map(ChatConversationMember::getUserId).distinct().toList();
    }

    private ChatWsConversationUpdatedPayload buildConversationUpdatedPayload(String action,
                                                                             ChatConversation conversation,
                                                                             List<ChatConversationMember> activeMembers) {
        return ChatWsConversationUpdatedPayload.builder()
                .action(action)
                .conversationId(conversation.getId())
                .conversationType(conversation.getConversationType())
                .name(conversation.getName())
                .avatar(conversation.getAvatar())
                .ownerId(conversation.getOwnerId())
                .notice(conversation.getRemark())
                .status(conversation.getStatus())
                .memberCount((long) (activeMembers == null ? 0 : activeMembers.size()))
                .build();
    }

    private ChatWsMembersUpdatedPayload buildMembersUpdatedPayload(String action,
                                                                   Long conversationId,
                                                                   Long affectedUserId,
                                                                   List<ChatMemberVO> members) {
        return ChatWsMembersUpdatedPayload.builder()
                .action(action)
                .conversationId(conversationId)
                .affectedUserId(affectedUserId)
                .members(members)
                .build();
    }

    private ChatMessageVO buildMessagePushVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setFile(parseFilePayload(message.getPayloadJson()));
        vo.setReplyMessageId(message.getReplyMessageId());
        SysUser sender = loadUsers(Set.of(message.getSenderId())).get(message.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(displayName(sender, message.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setRevoked(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED));
        vo.setEdited(isEdited(message.getMessageType(), message.getCreatedAt(), message.getUpdatedAt()));
        vo.setUpdatedAt(message.getUpdatedAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private String displayName(SysUser user, Long fallbackUserId) {
        if (user == null) {
            return fallbackUserId == null ? null : "用户" + fallbackUserId;
        }
        if (StrUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StrUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return user.getId() == null ? null : "用户" + user.getId();
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size, long defaultValue, long maxValue) {
        long normalized = size == null || size < 1 ? defaultValue : size;
        return Math.min(normalized, maxValue);
    }

    private int memberRoleOrder(ChatConversationMember member) {
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)) {
            return 0;
        }
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN)) {
            return 1;
        }
        return 2;
    }
}

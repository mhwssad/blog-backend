package com.cybzacg.blogbackend.module.chat.shared.support;

import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.chat.*;
import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ChatConversationRepository;
import com.cybzacg.blogbackend.dto.repository.chat.member.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.dto.repository.chat.message.ChatMessageReadCursorRepository;
import com.cybzacg.blogbackend.dto.repository.chat.message.ChatMessageRecipientRepository;
import com.cybzacg.blogbackend.dto.repository.chat.message.ChatMessageRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelConvert;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 聊天子服务共享基础设施：成员查询、游标操作、消息 VO 构造、用户加载等。
 *
 * <p>各子 Service 通过组合注入本类，复用通用的数据访问和 VO 构造逻辑。
 */
@Component
@RequiredArgsConstructor
public class ChatServiceSupport {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageRecipientRepository chatMessageRecipientRepository;
    private final ChatMessageReadCursorRepository chatMessageReadCursorRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelConvert chatModelConvert;
    private final ChatPayloadHelper chatPayloadHelper;
    private final ChatMemberHelper chatMemberHelper;
    private final SysConfigService sysConfigService;

    // ========== Repositories (exposed for sub-services) ==========

    public ChatConversationRepository getConversationRepository() {
        return chatConversationRepository;
    }

    public ChatConversationMemberRepository getConversationMemberRepository() {
        return chatConversationMemberRepository;
    }

    public ChatMessageRepository getMessageRepository() {
        return chatMessageRepository;
    }

    public ChatMessageRecipientRepository getMessageRecipientRepository() {
        return chatMessageRecipientRepository;
    }

    public ChatMessageReadCursorRepository getMessageReadCursorRepository() {
        return chatMessageReadCursorRepository;
    }

    public ChatModelConvert getModelConvert() {
        return chatModelConvert;
    }

    // ========== Access Context ==========

    public ConversationAccessContext requireConversationAccess(
        Long userId,
        Long conversationId
    ) {
        ExceptionThrowerCore.throwBusinessIfNull(
            conversationId,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "会话ID不能为空"
        );
        ChatConversation conversation = chatConversationRepository.getById(
            conversationId
        );
        ExceptionThrowerCore.throwBusinessIf(
            conversation == null ||
                !Objects.equals(
                    conversation.getStatus(),
                    ChatConstants.CONVERSATION_STATUS_NORMAL
                ),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "会话不存在或不可用"
        );
        if (Objects.equals(conversation.getIsAllSite(), 1)) {
            ensureGlobalConversationMembership(userId);
        }
        ChatConversationMember selfMember = findMember(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(
            selfMember == null ||
                !Objects.equals(
                    selfMember.getStatus(),
                    ChatConstants.MEMBER_STATUS_NORMAL
                ),
            ResultErrorCode.FORBIDDEN,
            "当前用户不在该会话中"
        );
        return new ConversationAccessContext(
            conversation,
            selfMember,
            listActiveMembers(conversationId)
        );
    }

    public ConversationAccessContext requireGroupAccess(
        Long userId,
        Long conversationId
    ) {
        ConversationAccessContext context = requireConversationAccess(
            userId,
            conversationId
        );
        ExceptionThrowerCore.throwBusinessIf(
            !Objects.equals(
                context.conversation().getConversationType(),
                ChatConstants.CONVERSATION_TYPE_GROUP
            ),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "当前会话不是群聊"
        );
        return context;
    }

    public ConversationAccessContext requireGroupOwner(
        Long userId,
        Long conversationId
    ) {
        ConversationAccessContext context = requireGroupAccess(
            userId,
            conversationId
        );
        ExceptionThrowerCore.throwBusinessIf(
            !Objects.equals(
                context.selfMember().getMemberRole(),
                ChatConstants.MEMBER_ROLE_OWNER
            ),
            ResultErrorCode.FORBIDDEN,
            "只有群主可以执行该操作"
        );
        return context;
    }

    public ConversationAccessContext requireGroupManager(
        Long userId,
        Long conversationId
    ) {
        ConversationAccessContext context = requireGroupAccess(
            userId,
            conversationId
        );
        ExceptionThrowerCore.throwBusinessIf(
            !isGroupManager(context.selfMember()),
            ResultErrorCode.FORBIDDEN,
            "只有群主或管理员可以执行该操作"
        );
        return context;
    }

    // ========== Membership ==========

    public void ensureGlobalConversationMembership(Long userId) {
        ChatConversation conversation =
            chatConversationRepository.findGlobalConversation();
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setConversationType(
                ChatConstants.CONVERSATION_TYPE_GLOBAL
            );
            conversation.setName(ChatConstants.GLOBAL_CONVERSATION_NAME);
            conversation.setIsAllSite(1);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                chatConversationRepository.save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation =
                    chatConversationRepository.findGlobalConversation();
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(
            conversation,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "全站群初始化失败"
        );
        upsertConversationMembership(
            conversation,
            userId,
            ChatConstants.MEMBER_ROLE_MEMBER,
            ChatConstants.JOIN_SOURCE_SYSTEM,
            true
        );
    }

    public void upsertConversationMembership(
        ChatConversation conversation,
        Long memberUserId,
        String memberRole,
        String joinSource,
        boolean resetCursorToLatest
    ) {
        ChatConversationMember member = findMember(
            conversation.getId(),
            memberUserId
        );
        Long referenceMessageId = resetCursorToLatest
            ? conversation.getLastMessageId()
            : null;
        LocalDateTime referenceMessageTime = resetCursorToLatest
            ? conversation.getLastMessageTime()
            : null;
        boolean wasInactive =
            member == null ||
            !Objects.equals(
                member.getStatus(),
                ChatConstants.MEMBER_STATUS_NORMAL
            );
        if (member == null) {
            member = chatModelConvert.toConversationMember(
                conversation.getId(),
                memberUserId,
                memberRole,
                joinSource,
                referenceMessageId,
                referenceMessageTime
            );
            chatConversationMemberRepository.save(member);
        } else {
            member.setMemberRole(memberRole);
            member.setJoinSource(joinSource);
            member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
            member.setMuteUntil(null);
            if (wasInactive || member.getJoinedAt() == null) {
                member.setJoinedAt(LocalDateTime.now());
            }
            if (resetCursorToLatest) {
                member.setLastReadMessageId(referenceMessageId);
                member.setLastReadAt(referenceMessageTime);
                member.setLastDeliveredMessageId(referenceMessageId);
                member.setLastDeliveredAt(referenceMessageTime);
            }
            chatConversationMemberRepository.updateById(member);
        }
        ChatMessageReadCursor cursor = getOrCreateCursor(
            conversation.getId(),
            memberUserId,
            referenceMessageId,
            referenceMessageTime
        );
        if (resetCursorToLatest) {
            cursor.setReadMessageId(referenceMessageId);
            cursor.setReadAt(referenceMessageTime);
            cursor.setDeliveredMessageId(referenceMessageId);
            cursor.setDeliveredAt(referenceMessageTime);
            cursor.setUnreadCount(0);
            saveOrUpdateCursor(cursor);
        }
    }

    public List<ChatConversationMember> listActiveMembers(Long conversationId) {
        return chatConversationMemberRepository.listActiveByConversationId(
            conversationId
        );
    }

    public Map<
        Long,
        List<ChatConversationMember>
    > listActiveMembersByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        List<ChatConversationMember> members =
            chatConversationMemberRepository.listActiveByConversationIds(
                conversationIds
            );
        Map<Long, List<ChatConversationMember>> result = new LinkedHashMap<>();
        for (ChatConversationMember member : members) {
            result
                .computeIfAbsent(member.getConversationId(), key ->
                    new ArrayList<>()
                )
                .add(member);
        }
        return result;
    }

    public ChatConversationMember findMember(Long conversationId, Long userId) {
        return chatConversationMemberRepository.findByConversationAndUser(
            conversationId,
            userId
        );
    }

    public ChatConversationMember requireActiveGroupMember(
        Long conversationId,
        Long memberUserId
    ) {
        ChatConversationMember member = findMember(
            conversationId,
            memberUserId
        );
        ExceptionThrowerCore.throwBusinessIf(
            member == null ||
                !Objects.equals(
                    member.getStatus(),
                    ChatConstants.MEMBER_STATUS_NORMAL
                ),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "目标成员不存在或已失效"
        );
        return member;
    }

    public boolean isGroupManager(ChatConversationMember member) {
        if (member == null) {
            return false;
        }
        return (
            Objects.equals(
                member.getMemberRole(),
                ChatConstants.MEMBER_ROLE_OWNER
            ) ||
            Objects.equals(
                member.getMemberRole(),
                ChatConstants.MEMBER_ROLE_ADMIN
            )
        );
    }

    public void validateManagerCanOperateMember(
        ChatConversationMember manager,
        ChatConversationMember targetMember
    ) {
        ExceptionThrowerCore.throwBusinessIf(
            manager == null || !isGroupManager(manager),
            ResultErrorCode.FORBIDDEN,
            "当前角色不能管理成员"
        );
        ExceptionThrowerCore.throwBusinessIf(
            Objects.equals(
                targetMember.getMemberRole(),
                ChatConstants.MEMBER_ROLE_OWNER
            ),
            ResultErrorCode.FORBIDDEN,
            "不能操作群主"
        );
        if (
            Objects.equals(
                manager.getMemberRole(),
                ChatConstants.MEMBER_ROLE_ADMIN
            )
        ) {
            ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(
                    targetMember.getMemberRole(),
                    ChatConstants.MEMBER_ROLE_MEMBER
                ),
                ResultErrorCode.FORBIDDEN,
                "管理员只能操作普通成员"
            );
        }
    }

    public List<Long> normalizeMemberIds(
        Collection<Long> memberUserIds,
        Long excludeUserId
    ) {
        Set<Long> ids = new LinkedHashSet<>();
        if (memberUserIds != null) {
            for (Long memberUserId : memberUserIds) {
                if (
                    memberUserId != null &&
                    !Objects.equals(memberUserId, excludeUserId)
                ) {
                    ids.add(memberUserId);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    public int countAdditionalMembers(
        Long conversationId,
        Collection<Long> memberUserIds
    ) {
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long memberUserId : memberUserIds) {
            ChatConversationMember member = findMember(
                conversationId,
                memberUserId
            );
            if (
                member == null ||
                !Objects.equals(
                    member.getStatus(),
                    ChatConstants.MEMBER_STATUS_NORMAL
                )
            ) {
                count++;
            }
        }
        return count;
    }

    public void ensureConversationMemberLimitAllows(
        ChatConversation conversation,
        int additionalCount
    ) {
        if (conversation == null || additionalCount <= 0) {
            return;
        }
        Integer memberLimit = conversation.getMemberLimit();
        if (memberLimit == null || memberLimit <= 0) {
            return;
        }
        int activeCount = listActiveMembers(conversation.getId()).size();
        ensureMemberLimitAllows(
            memberLimit,
            activeCount + additionalCount,
            "群人数已达上限"
        );
    }

    public void ensureMemberLimitAllows(
        Integer memberLimit,
        int expectedCount,
        String message
    ) {
        if (memberLimit == null || memberLimit <= 0) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
            expectedCount > memberLimit,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            message
        );
    }

    // ========== User Loading ==========

    public Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser user : sysUserRepository.listByIds(userIds)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    public SysUser requireActiveUser(Long userId, boolean allowSelf) {
        ExceptionThrowerCore.throwBusinessIfNull(
            userId,
            ResultErrorCode.USER_NOT_FOUND,
            "用户不存在"
        );
        Long currentUserId =
            com.cybzacg.blogbackend.utils.SecurityUtils.getUserId();
        if (!allowSelf && Objects.equals(currentUserId, userId)) {
            ExceptionThrowerCore.throwBusinessEx(
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "不能操作自己"
            );
        }
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
            user == null || !Objects.equals(user.getDeletedFlag(), 0),
            ResultErrorCode.USER_NOT_FOUND,
            "用户不存在"
        );
        ExceptionThrowerCore.throwBusinessIf(
            !Objects.equals(user.getStatus(), 1),
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "目标用户不可用"
        );
        return user;
    }

    public void requireActiveUsers(
        Collection<Long> userIds,
        boolean allowSelf
    ) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            requireActiveUser(userId, allowSelf);
        }
    }

    // ========== Cursor ==========

    public ChatMessageReadCursor getOrCreateCursor(
        Long conversationId,
        Long userId,
        Long referenceMessageId,
        LocalDateTime referenceTime
    ) {
        ChatMessageReadCursor cursor = findCursor(conversationId, userId);
        if (cursor != null) {
            if (cursor.getUnreadCount() == null) {
                cursor.setUnreadCount(0);
            }
            return cursor;
        }
        cursor = new ChatMessageReadCursor();
        cursor.setConversationId(conversationId);
        cursor.setUserId(userId);
        cursor.setReadMessageId(referenceMessageId);
        cursor.setReadAt(referenceTime);
        cursor.setDeliveredMessageId(referenceMessageId);
        cursor.setDeliveredAt(referenceTime);
        cursor.setUnreadCount(0);
        try {
            chatMessageReadCursorRepository.save(cursor);
        } catch (DuplicateKeyException ex) {
            ChatMessageReadCursor existing = findCursor(conversationId, userId);
            if (existing != null) {
                if (existing.getUnreadCount() == null) {
                    existing.setUnreadCount(0);
                }
                return existing;
            }
            throw ex;
        }
        return cursor;
    }

    public void saveOrUpdateCursor(ChatMessageReadCursor cursor) {
        if (cursor.getId() == null) {
            chatMessageReadCursorRepository.save(cursor);
        } else {
            chatMessageReadCursorRepository.updateById(cursor);
        }
    }

    public ChatMessageReadCursor findCursor(Long conversationId, Long userId) {
        return chatMessageReadCursorRepository.findByConversationAndUser(
            conversationId,
            userId
        );
    }

    public void advanceCursorDeliveredState(
        Long conversationId,
        Long userId,
        Long messageId,
        LocalDateTime deliveredAt
    ) {
        if (messageId == null) {
            return;
        }
        ChatMessageReadCursor cursor = getOrCreateCursor(
            conversationId,
            userId,
            null,
            null
        );
        if (
            cursor.getId() == null ||
            (cursor.getDeliveredMessageId() != null &&
                cursor.getDeliveredMessageId() >= messageId)
        ) {
            return;
        }
        boolean updated = chatMessageReadCursorRepository.advanceDeliveredState(
            cursor.getId(),
            messageId,
            deliveredAt
        );
        if (updated) {
            cursor.setDeliveredMessageId(messageId);
            cursor.setDeliveredAt(deliveredAt);
        }
    }

    public void advanceMemberDeliveredState(
        ChatConversationMember member,
        Long messageId,
        LocalDateTime deliveredAt
    ) {
        if (member == null || member.getId() == null || messageId == null) {
            return;
        }
        if (
            member.getLastDeliveredMessageId() != null &&
            member.getLastDeliveredMessageId() >= messageId
        ) {
            return;
        }
        boolean updated =
            chatConversationMemberRepository.advanceDeliveredState(
                member.getId(),
                messageId,
                deliveredAt
            );
        if (updated) {
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(deliveredAt);
        }
    }

    public void updateMemberReadState(
        ChatConversationMember member,
        Long messageId,
        LocalDateTime readAt
    ) {
        member.setLastReadMessageId(messageId);
        member.setLastReadAt(readAt);
        if (
            member.getLastDeliveredMessageId() == null ||
            member.getLastDeliveredMessageId() < messageId
        ) {
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(readAt);
        }
        chatConversationMemberRepository.updateById(member);
    }

    public long countUnread(Long conversationId, Long userId) {
        return chatMessageRecipientRepository.countUnread(
            conversationId,
            userId
        );
    }

    // ========== Message Access ==========

    public ChatMessageHistoryItem requireVisibleMessage(
        Long userId,
        Long conversationId,
        Long messageId
    ) {
        ChatMessageHistoryItem item =
            chatMessageRepository.selectVisibleMessageById(
                conversationId,
                userId,
                messageId
            );
        ExceptionThrowerCore.throwBusinessIfNull(
            item,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "消息不存在或不可访问"
        );
        return item;
    }

    public ChatMessage requireVisibleMessageEntity(
        Long userId,
        Long messageId
    ) {
        ExceptionThrowerCore.throwBusinessIfNull(
            messageId,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "消息ID不能为空"
        );
        ChatMessageRecipient recipient =
            chatMessageRecipientRepository.findVisibleByUserAndMessage(
                userId,
                messageId
            );
        ExceptionThrowerCore.throwBusinessIfNull(
            recipient,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "消息不存在或不可访问"
        );
        ChatMessage message = chatMessageRepository.getById(messageId);
        ExceptionThrowerCore.throwBusinessIfNull(
            message,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "消息不存在或不可访问"
        );
        return message;
    }

    public ChatMessageHistoryItem findExistingMessage(
        Long userId,
        String clientMessageId,
        Long conversationId
    ) {
        clientMessageId = StrUtils.trimToNull(clientMessageId);
        if (!StrUtils.hasText(clientMessageId)) {
            return null;
        }
        ChatMessage message =
            chatMessageRepository.findBySenderAndClientMessageId(
                userId,
                clientMessageId
            );
        if (
            message == null ||
            !Objects.equals(message.getConversationId(), conversationId)
        ) {
            return null;
        }
        return chatMessageRepository.selectVisibleMessageById(
            conversationId,
            userId,
            message.getId()
        );
    }

    // ========== VO Builders ==========

    public ChatConversationVO getConversationVO(
        Long userId,
        Long conversationId
    ) {
        ChatConversationListItem item =
            chatConversationRepository.selectConversationDetail(
                conversationId,
                userId,
                null
            );
        ExceptionThrowerCore.throwBusinessIfNull(
            item,
            ResultErrorCode.ILLEGAL_ARGUMENT,
            "会话不存在或不可访问"
        );
        List<ChatConversationMember> members = listActiveMembers(
            conversationId
        );
        Map<Long, List<ChatConversationMember>> memberMap = Map.of(
            conversationId,
            members
        );
        Map<Long, SysUser> userMap = loadUsers(
            collectConversationUserIds(List.of(item), memberMap)
        );
        return buildConversationVO(userId, item, members, userMap);
    }

    public List<ChatConversationVO> buildConversationRecords(
        Long userId,
        List<ChatConversationListItem> items
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ChatConversationMember>> membersByConversation =
            listActiveMembersByConversationIds(
                items.stream().map(ChatConversationListItem::getId).toList()
            );
        Map<Long, SysUser> userMap = loadUsers(
            collectConversationUserIds(items, membersByConversation)
        );
        List<ChatConversationVO> records = new ArrayList<>();
        for (ChatConversationListItem item : items) {
            records.add(
                buildConversationVO(
                    userId,
                    item,
                    membersByConversation.getOrDefault(item.getId(), List.of()),
                    userMap
                )
            );
        }
        return records;
    }

    public ChatConversationVO buildConversationVO(
        Long userId,
        ChatConversationListItem item,
        List<ChatConversationMember> members,
        Map<Long, SysUser> userMap
    ) {
        ChatConversationVO vo = chatModelConvert.toConversationVO(item);
        vo.setMemberCount((long) members.size());
        vo.setUnreadCount(Objects.requireNonNullElse(item.getUnreadCount(), 0));
        if (item.getLastMessageId() != null) {
            var lastMessage = chatModelConvert.toConversationLastMessageVO(
                item
            );
            SysUser sender = userMap.get(item.getLastMessageSenderId());
            lastMessage.setSenderNickname(
                UserDisplayNameUtils.resolveDisplayName(
                    sender,
                    item.getLastMessageSenderId()
                )
            );
            vo.setLastMessage(lastMessage);
        }
        if (
            Objects.equals(
                item.getConversationType(),
                ChatConstants.CONVERSATION_TYPE_SINGLE
            )
        ) {
            ChatConversationMember targetMember = members
                .stream()
                .filter(member -> !Objects.equals(member.getUserId(), userId))
                .findFirst()
                .orElse(null);
            if (targetMember != null) {
                SysUser targetUser = userMap.get(targetMember.getUserId());
                vo.setTargetUserId(targetMember.getUserId());
                vo.setTargetUsername(
                    targetUser != null ? targetUser.getUsername() : null
                );
                vo.setTargetNickname(
                    targetUser != null ? targetUser.getNickname() : null
                );
                vo.setName(
                    UserDisplayNameUtils.resolveDisplayName(
                        targetUser,
                        targetMember.getUserId()
                    )
                );
                vo.setAvatar(
                    targetUser != null ? targetUser.getAvatar() : null
                );
            }
        }
        return vo;
    }

    public List<ChatMemberVO> buildMemberRecords(
        List<ChatConversationMember> members
    ) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = loadUsers(
            members
                .stream()
                .map(ChatConversationMember::getUserId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll)
        );
        List<ChatMemberVO> records = new ArrayList<>();
        members
            .stream()
            .sorted(
                Comparator.comparingInt(chatMemberHelper::memberRoleOrder)
                    .thenComparing(
                        ChatConversationMember::getJoinedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                    )
                    .thenComparing(ChatConversationMember::getUserId)
            )
            .forEach(member -> {
                ChatMemberVO vo = chatModelConvert.toMemberVO(member);
                SysUser user = userMap.get(member.getUserId());
                vo.setUserId(member.getUserId());
                vo.setUsername(user != null ? user.getUsername() : null);
                vo.setNickname(user != null ? user.getNickname() : null);
                vo.setAvatar(user != null ? user.getAvatar() : null);
                records.add(vo);
            });
        return records;
    }

    public ChatMessageVO buildMessageVO(
        Long currentUserId,
        ChatMessageHistoryItem item,
        Map<Long, SysUser> userMap
    ) {
        Map<Long, ChatReplyMessageVO> replySnapshots =
            item.getReplyMessageId() == null
                ? Map.of()
                : loadReplySnapshotsForVisibleMessages(
                      currentUserId,
                      item.getConversationId(),
                      List.of(item.getReplyMessageId())
                  );
        return buildMessageVO(currentUserId, item, userMap, replySnapshots);
    }

    public ChatMessageVO buildMessageVO(
        Long currentUserId,
        ChatMessageHistoryItem item,
        Map<Long, SysUser> userMap,
        Map<Long, ChatReplyMessageVO> replySnapshots
    ) {
        ChatMessageVO vo = chatModelConvert.toMessageVO(item);
        SysUser sender = userMap.get(item.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(
            UserDisplayNameUtils.resolveDisplayName(sender, item.getSenderId())
        );
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setFile(chatPayloadHelper.extractFilePayload(item.getPayloadJson()));
        vo.setReplyMessageId(item.getReplyMessageId());
        vo.setReply(
            resolveReplySnapshot(item, item.getPayloadJson(), replySnapshots)
        );
        vo.setSelf(Objects.equals(currentUserId, item.getSenderId()));
        vo.setReadByCurrentUser(
            item.getDeliveryStatus() != null &&
                item.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_READ
        );
        vo.setRevoked(
            Objects.equals(
                item.getRevokeStatus(),
                ChatConstants.REVOKE_STATUS_REVOKED
            )
        );
        vo.setEdited(
            chatPayloadHelper.isEdited(
                item.getMessageType(),
                item.getCreatedAt(),
                item.getUpdatedAt()
            )
        );
        return vo;
    }

    public ChatReadStateVO toReadStateVO(
        ChatMessageReadCursor cursor,
        Long userId
    ) {
        ChatReadStateVO vo = chatModelConvert.toReadStateVO(cursor);
        vo.setUserId(userId);
        return vo;
    }

    // ========== Reply Snapshot ==========

    public ChatReplyMessageVO buildReplySnapshot(ChatMessageHistoryItem item) {
        if (item == null) {
            return null;
        }
        return buildReplySnapshot(item, loadUsers(Set.of(item.getSenderId())));
    }

    public ChatReplyMessageVO buildReplySnapshot(
        ChatMessageHistoryItem item,
        Map<Long, SysUser> userMap
    ) {
        ChatReplyMessageVO reply = new ChatReplyMessageVO();
        reply.setId(item.getId());
        reply.setSenderId(item.getSenderId());
        SysUser sender = userMap.get(item.getSenderId());
        reply.setSenderUsername(sender != null ? sender.getUsername() : null);
        reply.setSenderNickname(
            UserDisplayNameUtils.resolveDisplayName(sender, item.getSenderId())
        );
        reply.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        reply.setMessageType(item.getMessageType());
        reply.setReplyToMessageId(item.getReplyMessageId());
        reply.setContent(item.getContent());
        reply.setFile(
            chatPayloadHelper.extractFilePayload(item.getPayloadJson())
        );
        boolean revoked = Objects.equals(
            item.getRevokeStatus(),
            ChatConstants.REVOKE_STATUS_REVOKED
        );
        reply.setRevoked(revoked);
        reply.setDeleted(false);
        reply.setState(
            revoked
                ? ChatConstants.REPLY_STATE_REVOKED
                : ChatConstants.REPLY_STATE_NORMAL
        );
        reply.setCreatedAt(item.getCreatedAt());
        return reply;
    }

    public ChatReplyMessageVO resolveReplySnapshot(
        ChatMessageHistoryItem item,
        String payloadJson,
        Map<Long, ChatReplyMessageVO> replySnapshots
    ) {
        if (item.getReplyMessageId() == null) {
            return null;
        }
        ChatReplyMessageVO liveReply = replySnapshots.get(
            item.getReplyMessageId()
        );
        if (liveReply != null && !Boolean.TRUE.equals(liveReply.getDeleted())) {
            return liveReply;
        }
        ChatReplyMessageVO payloadReply =
            chatPayloadHelper.normalizeReplySnapshot(
                chatPayloadHelper.extractReplyPayload(payloadJson)
            );
        if (payloadReply != null) {
            return payloadReply;
        }
        return liveReply != null
            ? liveReply
            : chatPayloadHelper.buildUnavailableReplySnapshot(
                  item.getReplyMessageId()
              );
    }

    public Map<Long, ChatReplyMessageVO> loadReplySnapshotsForVisibleMessages(
        Long userId,
        Long conversationId,
        Collection<Long> replyMessageIds
    ) {
        List<Long> ids =
            replyMessageIds == null
                ? List.of()
                : replyMessageIds
                      .stream()
                      .filter(Objects::nonNull)
                      .distinct()
                      .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ChatMessageHistoryItem> replyItems = Objects.requireNonNullElse(
            chatMessageRepository.selectVisibleMessagesByIds(
                conversationId,
                userId,
                ids
            ),
            List.of()
        );
        Map<Long, SysUser> userMap = loadUsers(collectSenderIds(replyItems));
        Map<Long, ChatReplyMessageVO> result = new LinkedHashMap<>();
        for (ChatMessageHistoryItem replyItem : replyItems) {
            result.put(
                replyItem.getId(),
                buildReplySnapshot(replyItem, userMap)
            );
        }
        for (Long id : ids) {
            result.putIfAbsent(
                id,
                chatPayloadHelper.buildUnavailableReplySnapshot(id)
            );
        }
        return result;
    }

    // ========== Collections ==========

    public Set<Long> collectConversationUserIds(
        List<ChatConversationListItem> items,
        Map<Long, List<ChatConversationMember>> membersByConversation
    ) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatConversationListItem item : items) {
            if (item.getLastMessageSenderId() != null) {
                userIds.add(item.getLastMessageSenderId());
            }
            for (ChatConversationMember member : membersByConversation.getOrDefault(
                item.getId(),
                List.of()
            )) {
                userIds.add(member.getUserId());
            }
        }
        return userIds;
    }

    public Set<Long> collectSenderIds(List<ChatMessageHistoryItem> items) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatMessageHistoryItem item : items) {
            userIds.add(item.getSenderId());
        }
        return userIds;
    }

    public List<Long> collectReplyMessageIds(
        Collection<ChatMessageHistoryItem> items
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items
            .stream()
            .map(ChatMessageHistoryItem::getReplyMessageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<Long> listActiveUserIds(Long conversationId) {
        return activeUserIds(listActiveMembers(conversationId));
    }

    public List<Long> activeUserIds(
        Collection<ChatConversationMember> members
    ) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members
            .stream()
            .map(ChatConversationMember::getUserId)
            .distinct()
            .toList();
    }

    // ========== Single Conversation ==========

    public String buildSinglePairKey(Long userId, Long targetUserId) {
        long left = Math.min(userId, targetUserId);
        long right = Math.max(userId, targetUserId);
        return left + ":" + right;
    }

    // ========== Payload Builders ==========

    public String buildMessagePayloadJson(
        ChatFilePayloadVO filePayload,
        ChatReplyMessageVO replySnapshot
    ) {
        if (filePayload == null && replySnapshot == null) {
            return null;
        }
        ChatMessagePayloadVO payload = chatModelConvert.toMessagePayloadVO(
            filePayload,
            replySnapshot
        );
        return JsonUtils.toJson(payload);
    }

    public ChatFilePayloadVO buildFilePayload(
        FileBusinessInfo chatReference,
        FileInfo fileInfo,
        String messageType
    ) {
        ChatFilePayloadVO payload = new ChatFilePayloadVO();
        payload.setBusinessId(chatReference.getId());
        payload.setFileId(fileInfo.getId());
        payload.setFileName(fileInfo.getFileName());
        payload.setOriginalName(fileInfo.getOriginalName());
        payload.setFileUrl(fileInfo.getFileUrl());
        payload.setFileSize(fileInfo.getFileSize());
        payload.setFileType(fileInfo.getFileType());
        payload.setMimeType(fileInfo.getMimeType());
        payload.setPreviewUrl(fileInfo.getFileUrl());
        if (
            Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE) &&
            !StrUtils.hasText(payload.getThumbnailUrl())
        ) {
            payload.setThumbnailUrl(fileInfo.getFileUrl());
        }
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE)) {
            payload.setTranscodeStatus(
                ChatConstants.ATTACHMENT_TRANSCODE_STATUS_PENDING
            );
        } else {
            payload.setTranscodeStatus(
                ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE
            );
        }
        return payload;
    }

    public String buildFileMessageSummary(
        String messageType,
        FileInfo fileInfo
    ) {
        String fileName = resolveFileDisplayName(fileInfo);
        String prefix = switch (messageType) {
            case ChatConstants.MESSAGE_TYPE_IMAGE -> "[图片]";
            case ChatConstants.MESSAGE_TYPE_VOICE -> "[语音]";
            default -> "[文件]";
        };
        return StrUtils.hasText(fileName) ? prefix + " " + fileName : prefix;
    }

    private String resolveFileDisplayName(FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        if (StrUtils.hasText(fileInfo.getOriginalName())) {
            return fileInfo.getOriginalName().trim();
        }
        if (StrUtils.hasText(fileInfo.getFileName())) {
            return fileInfo.getFileName().trim();
        }
        return null;
    }

    public String resolveAttachmentMessageType(FileInfo fileInfo) {
        String mimeType =
            fileInfo == null
                ? null
                : StrUtils.trimToNull(fileInfo.getMimeType());
        if (mimeType != null) {
            String lowerMimeType = mimeType.toLowerCase();
            if (lowerMimeType.startsWith("image/")) {
                return ChatConstants.MESSAGE_TYPE_IMAGE;
            }
            if (lowerMimeType.startsWith("audio/")) {
                return ChatConstants.MESSAGE_TYPE_VOICE;
            }
        }
        return ChatConstants.MESSAGE_TYPE_FILE;
    }

    // ========== Config ==========

    public int resolveConversationSpeakRequiredLevel(
        ChatConversation conversation
    ) {
        Integer speakLevelLimit = conversation.getSpeakLevelLimit();
        if (speakLevelLimit != null && speakLevelLimit > 1) {
            return speakLevelLimit;
        }
        if (
            Integer.valueOf(1).equals(conversation.getIsAllSite()) ||
            Objects.equals(
                conversation.getConversationType(),
                ChatConstants.CONVERSATION_TYPE_GLOBAL
            ) ||
            Objects.equals(
                conversation.getSceneType(),
                ChatConstants.SCENE_TYPE_HALL_CHANNEL
            )
        ) {
            return getConfigInt(
                ConfigConstants.CHAT_HALL_SPEAK_MIN_LEVEL_KEY,
                ConfigConstants.DEFAULT_CHAT_HALL_SPEAK_MIN_LEVEL
            );
        }
        return 1;
    }

    public int getConfigInt(String key, int defaultValue) {
        String value = sysConfigService.getValueOrDefault(
            key,
            String.valueOf(defaultValue)
        );
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public String trimKeyword(String keyword) {
        return StrUtils.trimToNull(keyword);
    }

    public String metricResultOf(RuntimeException ex) {
        return ex instanceof com.cybzacg.blogbackend.exception.BusinessException
            ? "business_error"
            : "system_error";
    }

    // ========== Inner Records ==========

    public record ConversationAccessContext(
        ChatConversation conversation,
        ChatConversationMember selfMember,
        List<ChatConversationMember> activeMembers
    ) {
        public List<Long> activeUserIds() {
            return activeMembers
                .stream()
                .map(ChatConversationMember::getUserId)
                .distinct()
                .toList();
        }
    }

    public record PreparedFileMessage(
        Long sourceBusinessId,
        FileInfo fileInfo
    ) {}

    public record RevocableMessageContext(ChatMessage message) {}
}

package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatEditMessageRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupNoticeUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMarkReadRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMuteMemberRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatOpenSingleConversationRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatTransferGroupOwnerRequest;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMessageDeletedPayload;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageReadCursorRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageRecipientRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentAsyncProcessingService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageGovernanceService;
import com.cybzacg.blogbackend.module.chat.service.ChatMetricsService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.service.UserChatService;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户侧聊天服务实现。
 *
 * <p>负责统一收口会话鉴权、单聊懒创建、群成员维护、消息持久化、未读统计和实时推送。
 */
@Service
@RequiredArgsConstructor
public class UserChatServiceImpl implements UserChatService {
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageRecipientRepository chatMessageRecipientRepository;
    private final ChatMessageReadCursorRepository chatMessageReadCursorRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelMapper chatModelMapper;
    private final ChatPushService chatPushService;
    private final ChatWebSocketSessionRegistry chatWebSocketSessionRegistry;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileLifecycleService fileLifecycleService;
    private final ChatAttachmentAsyncProcessingService chatAttachmentAsyncProcessingService;
    private final ChatMessageGovernanceService chatMessageGovernanceService;
    private final ChatMetricsService chatMetricsService;

    /**
     * 分页查询当前用户的会话列表。
     */
    @Override
    public PageResult<ChatConversationVO> pageMyConversations(ChatConversationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        ensureGlobalConversationMembership(userId);
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize(), 20L, 100L);
        String keyword = trimKeyword(query.getKeyword());
        long total = Objects.requireNonNullElse(chatConversationRepository.countConversationPage(userId, keyword), 0L);
        if (total == 0L) {
            return PageResult.<ChatConversationVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatConversationListItem> items = chatConversationRepository.selectConversationPage(userId, keyword, offset, size);
        return PageResult.<ChatConversationVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(buildConversationRecords(userId, items))
                .build();
    }

    /**
     * 查询当前用户指定会话的详情。
     */
    @Override
    public ChatConversationVO getMyConversation(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        ensureGlobalConversationMembership(userId);
        requireConversationAccess(userId, conversationId);
        return getConversationVO(userId, conversationId);
    }

    /**
     * 打开或创建与目标用户的单聊会话。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO openSingleConversation(ChatOpenSingleConversationRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysUser targetUser = requireActiveUser(request.getTargetUserId(), true);
        ChatConversation conversation = ensureSingleConversation(userId, targetUser.getId());
        return getConversationVO(userId, conversation.getId());
    }

    /**
     * 分页查询当前用户在指定会话中的消息历史。
     */
    @Override
    public PageResult<ChatMessageVO> pageMyMessages(Long conversationId, ChatMessagePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        return pageMessages(userId, conversationId, query);
    }

    /**
     * 以当前登录用户身份发送文本消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendTextMessage(ChatSendTextRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return sendTextMessage(userId, request);
    }

    /**
     * 以指定用户身份发送文本消息，含频控校验、幂等检查、回复快照、未读计数和实时推送。
     *
     * @param userId  发送者用户 ID
     * @param request 发送请求，含会话/目标用户、内容和客户端消息 ID
     * @return 发送成功的消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request) {
        try {
            validateSendRequest(request);
            chatMessageGovernanceService.validateTextMessage(userId, request.getContent());
            ChatConversation conversation = resolveSendConversation(userId, request);
            ConversationAccessContext context = requireConversationAccess(userId, conversation.getId());
            validateMemberCanSend(context.selfMember());
            List<ChatConversationMember> activeMembers = context.activeMembers();
            ChatMessageHistoryItem existing = findExistingMessage(userId, request.getClientMessageId(), conversation.getId());
            if (existing != null) {
                ChatMessageVO existingMessage = buildMessageVO(userId, existing, loadUsers(Set.of(existing.getSenderId())));
                chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
                return existingMessage;
            }
            ChatMessageHistoryItem replyMessage = resolveReplyMessage(userId, conversation.getId(), request.getReplyMessageId());

            ChatMessage message = chatModelMapper.toTextMessage(request);
            message.setConversationId(conversation.getId());
            message.setSenderId(userId);
            message.setReplyMessageId(replyMessage != null ? replyMessage.getId() : null);
            message.setPayloadJson(buildMessagePayloadJson(null, buildReplySnapshot(replyMessage)));
            try {
                chatMessageRepository.save(message);
            } catch (DuplicateKeyException ex) {
                ChatMessageVO existingMessage = resolveDuplicateClientMessage(userId, request.getClientMessageId(), conversation.getId(), ex);
                chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
                return existingMessage;
            }

            LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageTime(now);
            chatConversationRepository.updateById(conversation);

            List<Long> activeUserIds = activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
            persistRecipients(message, activeUserIds, userId, now);
            updateSenderCursorAfterSend(conversation, userId, message.getId(), now);
            incrementUnreadForRecipients(conversation.getId(), activeUserIds, userId);
            markDeliveredForOnlineRecipients(conversation.getId(), activeUserIds, userId, message.getId(), now);

            ChatMessageHistoryItem item = requireVisibleMessage(userId, conversation.getId(), message.getId());
            ChatMessageVO messageVO = buildMessageVO(userId, item, loadUsers(Set.of(userId)));
            chatPushService.pushMessageCreated(messageVO, activeUserIds);
            chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, "success");
            return messageVO;
        } catch (RuntimeException ex) {
            chatMetricsService.recordSend(ChatConstants.MESSAGE_TYPE_TEXT, metricResultOf(ex));
            throw ex;
        }
    }

    /**
     * 发送文件/图片/语音附件消息，发送后调度异步媒体处理（缩略图、转码等）。
     *
     * @param request 发送请求，含文件业务引用 ID、会话/目标用户和客户端消息 ID
     * @return 发送成功的消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendFileMessage(ChatSendFileRequest request) {
        Long userId = SecurityUtils.requireUserId();
        String metricMessageType = ChatConstants.MESSAGE_TYPE_FILE;
        try {
            validateSendFileRequest(request);
            chatMessageGovernanceService.validateAttachmentMessage(userId);
            ChatConversation conversation = resolveSendConversation(userId, request.getConversationId(), request.getTargetUserId());
            ConversationAccessContext context = requireConversationAccess(userId, conversation.getId());
            validateMemberCanSend(context.selfMember());
            List<ChatConversationMember> activeMembers = context.activeMembers();
            ChatMessageHistoryItem existing = findExistingMessage(userId, request.getClientMessageId(), conversation.getId());
            if (existing != null) {
                ChatMessageVO existingMessage = buildMessageVO(userId, existing, loadUsers(Set.of(existing.getSenderId())));
                chatMetricsService.recordSend(existing.getMessageType(), "success");
                return existingMessage;
            }

            PreparedFileMessage preparedFile = prepareFileMessage(userId, request.getBusinessId());
            metricMessageType = resolveAttachmentMessageType(preparedFile.fileInfo());
            ChatMessageHistoryItem replyMessage = resolveReplyMessage(userId, conversation.getId(), request.getReplyMessageId());
            Long replyMessageId = replyMessage != null ? replyMessage.getId() : null;
            ChatMessage message = buildFileMessage(conversation.getId(), userId, request.getClientMessageId(), replyMessageId, preparedFile.fileInfo());
            try {
                chatMessageRepository.save(message);
            } catch (DuplicateKeyException ex) {
                ChatMessageVO existingMessage = resolveDuplicateClientMessage(userId, request.getClientMessageId(), conversation.getId(), ex);
                chatMetricsService.recordSend(existingMessage.getMessageType(), "success");
                return existingMessage;
            }
            ChatFilePayloadVO filePayload = bindFileReferenceToMessage(preparedFile, message.getId(), message.getMessageType());
            message.setPayloadJson(buildMessagePayloadJson(filePayload, buildReplySnapshot(replyMessage)));
            chatMessageRepository.updateById(message);

            LocalDateTime now = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageTime(now);
            chatConversationRepository.updateById(conversation);

            List<Long> activeUserIds = activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
            persistRecipients(message, activeUserIds, userId, now);
            updateSenderCursorAfterSend(conversation, userId, message.getId(), now);
            incrementUnreadForRecipients(conversation.getId(), activeUserIds, userId);
            markDeliveredForOnlineRecipients(conversation.getId(), activeUserIds, userId, message.getId(), now);

            ChatMessageHistoryItem item = requireVisibleMessage(userId, conversation.getId(), message.getId());
            ChatMessageVO messageVO = buildMessageVO(userId, item, loadUsers(Set.of(userId)));
            chatPushService.pushMessageCreated(messageVO, activeUserIds);
            chatAttachmentAsyncProcessingService.scheduleAfterCommit(message.getId(), messageVO, activeUserIds);
            chatMetricsService.recordSend(metricMessageType, "success");
            return messageVO;
        } catch (RuntimeException ex) {
            chatMetricsService.recordSend(metricMessageType, metricResultOf(ex));
            throw ex;
        }
    }

    /**
     * 编辑当前用户自己发送的文本消息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO editMessage(Long messageId, ChatEditMessageRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "编辑参数不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(StrUtils.trimToNull(request.getContent()), ResultErrorCode.ILLEGAL_ARGUMENT, "消息内容不能为空");
        ChatMessage message = requireEditableOwnTextMessage(userId, messageId);
        message.setContent(StrUtils.trim(request.getContent()));
        chatMessageRepository.updateById(message);
        ChatMessageHistoryItem item = requireVisibleMessage(userId, message.getConversationId(), messageId);
        ChatMessageVO messageVO = buildMessageVO(userId, item, loadUsers(Set.of(message.getSenderId())));
        chatPushService.pushMessageUpdated(messageVO, listActiveUserIds(message.getConversationId()));
        return messageVO;
    }

    /**
     * 撤回当前用户自己发送的消息，同时释放关联的文件业务引用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeMessage(Long messageId) {
        Long userId = SecurityUtils.requireUserId();
        RevocableMessageContext revokeContext = requireRevocableMessage(userId, messageId);
        List<Long> activeUserIds = listActiveUserIds(revokeContext.message().getConversationId());
        revokeMessage(revokeContext.message(), userId);
        ChatMessageHistoryItem item = requireVisibleMessage(userId, revokeContext.message().getConversationId(), messageId);
        ChatMessageVO messageVO = buildMessageVO(userId, item, loadUsers(Set.of(revokeContext.message().getSenderId())));
        chatPushService.pushMessageRevoked(messageVO, activeUserIds);
    }

    /**
     * 对当前用户隐藏指定消息（仅影响自身可见性），并重新计算未读数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId) {
        Long userId = SecurityUtils.requireUserId();
        ChatMessage message = requireVisibleMessageEntity(userId, messageId);
        chatMessageRecipientRepository.hideMessage(message.getConversationId(), userId, messageId);
        ChatMessageReadCursor cursor = getOrCreateCursor(message.getConversationId(), userId, null, null);
        int unreadCount = (int) countUnread(message.getConversationId(), userId);
        cursor.setUnreadCount(unreadCount);
        saveOrUpdateCursor(cursor);
        chatPushService.pushMessageDeleted(ChatWsMessageDeletedPayload.builder()
                .conversationId(message.getConversationId())
                .messageId(messageId)
                .userId(userId)
                .unreadCount(unreadCount)
                .build(), List.of(userId));
    }

    /**
     * 以当前登录用户身份标记会话内消息为已读。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long conversationId, ChatMarkReadRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return markRead(userId, conversationId, request.getReadMessageId());
    }
    /**
     * 推进当前用户在会话内的已读高水位，并同步回写 recipient/cursor/member 三处读状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReadStateVO markRead(Long userId, Long conversationId, Long readMessageId) {
        ExceptionThrowerCore.throwBusinessIfNull(readMessageId, ResultErrorCode.ILLEGAL_ARGUMENT, "已读消息ID不能为空");
        ConversationAccessContext context = requireConversationAccess(userId, conversationId);
        ChatMessageHistoryItem message = requireVisibleMessage(userId, conversationId, readMessageId);
        ChatMessageReadCursor cursor = getOrCreateCursor(conversationId, userId, null, null);
        if (cursor.getReadMessageId() != null && cursor.getReadMessageId() >= message.getId()) {
            ChatReadStateVO state = toReadStateVO(cursor, userId);
            chatPushService.pushReadUpdated(state, context.activeUserIds());
            return state;
        }

        LocalDateTime now = LocalDateTime.now();
        chatMessageRecipientRepository.markReadUpTo(conversationId, userId, message.getId(), now);

        long unread = countUnread(conversationId, userId);
        cursor.setReadMessageId(message.getId());
        cursor.setReadAt(now);
        if (cursor.getDeliveredMessageId() == null || cursor.getDeliveredMessageId() < message.getId()) {
            cursor.setDeliveredMessageId(message.getId());
            cursor.setDeliveredAt(now);
        }
        cursor.setUnreadCount((int) unread);
        saveOrUpdateCursor(cursor);
        updateMemberReadState(context.selfMember(), message.getId(), now);

        ChatReadStateVO state = toReadStateVO(cursor, userId);
        chatPushService.pushReadUpdated(state, context.activeUserIds());
        return state;
    }

    /**
     * 创建群聊会话，并将创建者设为群主、指定成员设为普通成员。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO createGroup(ChatCreateGroupRequest request) {
        Long userId = SecurityUtils.requireUserId();
        List<Long> memberUserIds = normalizeMemberIds(request.getMemberUserIds(), userId);
        ExceptionThrowerCore.throwBusinessIf(memberUserIds.isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "群成员不能为空");
        requireActiveUsers(memberUserIds, true);
        ChatConversation conversation = chatModelMapper.toGroupConversation(request);
        conversation.setOwnerId(userId);
        chatConversationRepository.save(conversation);
        upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_OWNER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        for (Long memberUserId : memberUserIds) {
            upsertConversationMembership(conversation, memberUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        }
        return getConversationVO(userId, conversation.getId());
    }

    /**
     * 查询当前用户所在群聊的详情。
     */
    @Override
    public ChatConversationVO getGroupDetail(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupAccess(userId, conversationId);
        return getConversationVO(userId, context.conversation().getId());
    }

    /**
     * 查询指定群聊的活跃成员列表。
     */
    @Override
    public List<ChatMemberVO> listGroupMembers(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupAccess(userId, conversationId);
        return buildMemberRecords(listActiveMembers(conversationId));
    }

    /**
     * 邀请用户加入群聊（需要群主或管理员权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> inviteGroupMembers(Long conversationId, ChatGroupMemberOperateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupManager(userId, conversationId);
        List<Long> memberUserIds = normalizeMemberIds(request.getMemberUserIds(), userId);
        ExceptionThrowerCore.throwBusinessIf(memberUserIds.isEmpty(), ResultErrorCode.ILLEGAL_ARGUMENT, "成员用户ID不能为空");
        requireActiveUsers(memberUserIds, true);
        for (Long memberUserId : memberUserIds) {
            upsertConversationMembership(context.conversation(), memberUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        }
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(members);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("members_invited", conversationId, null, records), activeUserIds(members));
        return records;
    }

    /**
     * 将指定成员提升为群管理员（需要群主权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> appointGroupAdmin(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupOwner(userId, conversationId);
        ChatConversationMember member = requireActiveGroupMember(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.ILLEGAL_ARGUMENT, "群主无需重复设置为管理员");
        member.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        chatConversationMemberRepository.updateById(member);
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(members);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("admin_appointed", conversationId, memberUserId, records), activeUserIds(members));
        return records;
    }

    /**
     * 取消指定成员的群管理员角色，回退为普通成员（需要群主权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> removeGroupAdmin(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        requireGroupOwner(userId, conversationId);
        ChatConversationMember member = requireActiveGroupMember(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.ILLEGAL_ARGUMENT, "不能取消群主角色");
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        chatConversationMemberRepository.updateById(member);
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(members);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("admin_removed", conversationId, memberUserId, records), activeUserIds(members));
        return records;
    }

    /**
     * 将群主转让给指定成员，当前群主自动降级为管理员。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO transferGroupOwner(Long conversationId, ChatTransferGroupOwnerRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupOwner(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(request == null || request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "新群主用户ID不能为空");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(request.getTargetUserId(), userId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能把群主转让给自己");
        ChatConversationMember targetMember = requireActiveGroupMember(conversationId, request.getTargetUserId());
        context.selfMember().setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        context.conversation().setOwnerId(targetMember.getUserId());
        chatConversationMemberRepository.updateById(context.selfMember());
        chatConversationMemberRepository.updateById(targetMember);
        chatConversationRepository.updateById(context.conversation());
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        List<ChatMemberVO> memberRecords = buildMemberRecords(members);
        List<Long> activeUserIds = activeUserIds(members);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("owner_transferred", conversationId, targetMember.getUserId(), memberRecords), activeUserIds);
        chatPushService.pushConversationUpdated(buildConversationUpdatedPayload("owner_transferred", context.conversation(), members), activeUserIds);
        return getConversationVO(userId, conversationId);
    }

    /**
     * 设置或解除群成员禁言（需要群主或管理员权限）。
     *
     * @param conversationId 目标会话 ID
     * @param memberUserId   被操作成员的用户 ID
     * @param request        包含禁言截止时间，null 或已过去的时间视为解除禁言
     * @return 更新后的成员列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> muteGroupMember(Long conversationId, Long memberUserId, ChatMuteMemberRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupManager(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, memberUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能对自己执行禁言操作");
        ChatConversationMember targetMember = requireActiveGroupMember(conversationId, memberUserId);
        validateManagerCanOperateMember(context.selfMember(), targetMember);
        LocalDateTime muteUntil = request == null ? null : request.getMuteUntil();
        targetMember.setMuteUntil(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()) ? muteUntil : null);
        chatConversationMemberRepository.updateById(targetMember);
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        List<ChatMemberVO> records = buildMemberRecords(members);
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("member_mute_updated", conversationId, memberUserId, records), activeUserIds(members));
        return records;
    }

    /**
     * 更新群公告内容（需要群主或管理员权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO updateGroupNotice(Long conversationId, ChatGroupNoticeUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupManager(userId, conversationId);
        context.conversation().setRemark(request == null ? null : StrUtils.trimToNull(request.getNotice()));
        chatConversationRepository.updateById(context.conversation());
        chatPushService.pushConversationUpdated(buildConversationUpdatedPayload("notice_updated", context.conversation(), listActiveMembers(conversationId)), context.activeUserIds());
        return getConversationVO(userId, conversationId);
    }

    /**
     * 将指定成员从群聊中移除（需要群主或管理员权限，不能移除自己）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupMember(Long conversationId, Long memberUserId) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupManager(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, memberUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能移除自己，请使用退群接口");
        ChatConversationMember member = requireActiveGroupMember(conversationId, memberUserId);
        validateManagerCanOperateMember(context.selfMember(), member);
        member.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        chatConversationMemberRepository.updateById(member);
        List<ChatMemberVO> records = buildMemberRecords(listActiveMembers(conversationId));
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("member_removed", conversationId, memberUserId, records), notifyUserIds);
    }

    /**
     * 当前用户主动退出群聊（群主不能退出，需先解散或转让）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupAccess(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(context.selfMember().getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.UNSUPPORTED_OPERATION, "群主不能直接退群，请先解散群聊");
        context.selfMember().setStatus(ChatConstants.MEMBER_STATUS_LEFT);
        chatConversationMemberRepository.updateById(context.selfMember());
        List<ChatMemberVO> records = buildMemberRecords(listActiveMembers(conversationId));
        chatPushService.pushMembersUpdated(buildMembersUpdatedPayload("member_left", conversationId, userId, records), notifyUserIds);
    }

    /**
     * 解散群聊，移除所有活跃成员并将会话标记为已解散（需要群主权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        ConversationAccessContext context = requireGroupOwner(userId, conversationId);
        List<Long> notifyUserIds = context.activeUserIds();
        context.conversation().setStatus(ChatConstants.CONVERSATION_STATUS_DISSOLVED);
        chatConversationRepository.updateById(context.conversation());
        chatConversationMemberRepository.removeAllActiveMembers(conversationId);
        chatPushService.pushConversationUpdated(buildConversationUpdatedPayload("conversation_dissolved", context.conversation(), List.of()), notifyUserIds);
    }

    /**
     * 分页读取消息历史，并在用户拉取到消息时收口“已送达”状态。
     */
    private PageResult<ChatMessageVO> pageMessages(Long userId, Long conversationId, ChatMessagePageQuery query) {
        requireConversationAccess(userId, conversationId);
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize(), 20L, 100L);
        Long beforeMessageId = query.getBeforeMessageId();
        long total = Objects.requireNonNullElse(chatMessageRepository.countMessagePage(conversationId, userId, beforeMessageId), 0L);
        if (total == 0L) {
            return PageResult.<ChatMessageVO>builder()
                    .total(0L)
                    .current(current)
                    .size(size)
                    .records(List.of())
                    .build();
        }
        long offset = (current - 1) * size;
        List<ChatMessageHistoryItem> items = new ArrayList<>(chatMessageRepository.selectMessagePage(conversationId, userId, beforeMessageId, offset, size));
        markMessagesDelivered(userId, conversationId, items);
        Collections.reverse(items);
        Map<Long, SysUser> userMap = loadUsers(collectSenderIds(items));
        Map<Long, ChatReplyMessageVO> replySnapshots = loadReplySnapshotsForVisibleMessages(userId, conversationId, collectReplyMessageIds(items));
        List<ChatMessageVO> records = items.stream().map(item -> buildMessageVO(userId, item, userMap, replySnapshots)).toList();
        return PageResult.<ChatMessageVO>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    /**
     * 统一校验当前用户是否有权访问会话，并返回会话、本人成员关系和当前活跃成员快照。
     */
    private ConversationAccessContext requireConversationAccess(Long userId, Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID不能为空");
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL), ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在或不可用");
        if (Objects.equals(conversation.getIsAllSite(), 1)) {
            ensureGlobalConversationMembership(userId);
        }
        ChatConversationMember selfMember = findMember(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(selfMember == null || !Objects.equals(selfMember.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL), ResultErrorCode.FORBIDDEN, "当前用户不在该会话中");
        return new ConversationAccessContext(conversation, selfMember, listActiveMembers(conversationId));
    }

    private ConversationAccessContext requireGroupAccess(Long userId, Long conversationId) {
        ConversationAccessContext context = requireConversationAccess(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(context.conversation().getConversationType(), ChatConstants.CONVERSATION_TYPE_GROUP), ResultErrorCode.ILLEGAL_ARGUMENT, "当前会话不是群聊");
        return context;
    }

    private ConversationAccessContext requireGroupOwner(Long userId, Long conversationId) {
        ConversationAccessContext context = requireGroupAccess(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(context.selfMember().getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.FORBIDDEN, "只有群主可以执行该操作");
        return context;
    }

    private ConversationAccessContext requireGroupManager(Long userId, Long conversationId) {
        ConversationAccessContext context = requireGroupAccess(userId, conversationId);
        ExceptionThrowerCore.throwBusinessIf(!isGroupManager(context.selfMember()), ResultErrorCode.FORBIDDEN, "只有群主或管理员可以执行该操作");
        return context;
    }

    private ChatConversation resolveSendConversation(Long userId, ChatSendTextRequest request) {
        return resolveSendConversation(userId, request.getConversationId(), request.getTargetUserId());
    }

    private ChatConversation resolveSendConversation(Long userId, Long conversationId, Long targetUserId) {
        if (conversationId != null) {
            return requireConversationAccess(userId, conversationId).conversation();
        }
        ExceptionThrowerCore.throwBusinessIfNull(targetUserId, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
        SysUser targetUser = requireActiveUser(targetUserId, true);
        return ensureSingleConversation(userId, targetUser.getId());
    }

    private void validateSendRequest(ChatSendTextRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "发送参数不能为空");
        ExceptionThrowerCore.throwBusinessIfBlank(StrUtils.trimToNull(request.getContent()), ResultErrorCode.ILLEGAL_ARGUMENT, "消息内容不能为空");
        ExceptionThrowerCore.throwBusinessIf(request.getConversationId() == null && request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
    }

    private void validateSendFileRequest(ChatSendFileRequest request) {
        ExceptionThrowerCore.throwBusinessIf(request == null, ResultErrorCode.ILLEGAL_ARGUMENT, "发送参数不能为空");
        ExceptionThrowerCore.throwBusinessIfNull(request.getBusinessId(), ResultErrorCode.ILLEGAL_ARGUMENT, "文件业务引用ID不能为空");
        ExceptionThrowerCore.throwBusinessIf(request.getConversationId() == null && request.getTargetUserId() == null, ResultErrorCode.ILLEGAL_ARGUMENT, "会话ID和目标用户ID不能同时为空");
    }

    /**
     * 统一拦截成员自身被禁言时的发言请求，避免继续写入消息和推进状态。
     */
    private void validateMemberCanSend(ChatConversationMember selfMember) {
        LocalDateTime muteUntil = selfMember == null ? null : selfMember.getMuteUntil();
        ExceptionThrowerCore.throwBusinessIf(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()),
                ResultErrorCode.FORBIDDEN,
                "当前用户已被禁言，暂时不能发送消息");
    }

    /**
     * 确保单聊会话存在，并把双方成员与游标修正到可发送状态。
     */
    private ChatConversation ensureSingleConversation(Long userId, Long targetUserId) {
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(userId, targetUserId), ResultErrorCode.ILLEGAL_ARGUMENT, "不能给自己发送单聊消息");
        String pairKey = buildSinglePairKey(userId, targetUserId);
        ChatConversation conversation = chatConversationRepository.findBySinglePairKey(pairKey);
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
            conversation.setSinglePairKey(pairKey);
            conversation.setIsAllSite(0);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                chatConversationRepository.save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation = chatConversationRepository.findBySinglePairKey(pairKey);
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "单聊会话创建失败");
        if (!Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL)) {
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            chatConversationRepository.updateById(conversation);
        }
        upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        upsertConversationMembership(conversation, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_MANUAL, true);
        return conversation;
    }

    /**
     * 确保全站群存在，并为当前用户补建活跃成员和游标记录。
     */
    private ChatConversation ensureGlobalConversationMembership(Long userId) {
        ChatConversation conversation = chatConversationRepository.findGlobalConversation();
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
            conversation.setName(ChatConstants.GLOBAL_CONVERSATION_NAME);
            conversation.setIsAllSite(1);
            conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            try {
                chatConversationRepository.save(conversation);
            } catch (DuplicateKeyException ex) {
                conversation = chatConversationRepository.findGlobalConversation();
            }
        }
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "全站群初始化失败");
        upsertConversationMembership(conversation, userId, ChatConstants.MEMBER_ROLE_MEMBER, ChatConstants.JOIN_SOURCE_SYSTEM, true);
        return conversation;
    }

    /**
     * 在成员新增、恢复或补建时，统一初始化成员状态和已读游标，避免把历史未读错算给新成员。
     */
    private void upsertConversationMembership(ChatConversation conversation,
                                              Long memberUserId,
                                              String memberRole,
                                              String joinSource,
                                              boolean resetCursorToLatest) {
        ChatConversationMember member = findMember(conversation.getId(), memberUserId);
        Long referenceMessageId = resetCursorToLatest ? conversation.getLastMessageId() : null;
        LocalDateTime referenceMessageTime = resetCursorToLatest ? conversation.getLastMessageTime() : null;
        boolean wasInactive = member == null || !Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL);
        if (member == null) {
            member = chatModelMapper.toConversationMember(conversation.getId(), memberUserId, memberRole, joinSource, referenceMessageId, referenceMessageTime);
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
        ChatMessageReadCursor cursor = getOrCreateCursor(conversation.getId(), memberUserId, referenceMessageId, referenceMessageTime);
        if (resetCursorToLatest) {
            cursor.setReadMessageId(referenceMessageId);
            cursor.setReadAt(referenceMessageTime);
            cursor.setDeliveredMessageId(referenceMessageId);
            cursor.setDeliveredAt(referenceMessageTime);
            cursor.setUnreadCount(0);
            saveOrUpdateCursor(cursor);
        }
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
        chatMessageRecipientRepository.saveBatch(recipients);
    }

    private void updateSenderCursorAfterSend(ChatConversation conversation, Long senderId, Long messageId, LocalDateTime now) {
        ChatMessageReadCursor cursor = getOrCreateCursor(conversation.getId(), senderId, messageId, now);
        cursor.setReadMessageId(messageId);
        cursor.setReadAt(now);
        cursor.setDeliveredMessageId(messageId);
        cursor.setDeliveredAt(now);
        cursor.setUnreadCount(0);
        saveOrUpdateCursor(cursor);
        ChatConversationMember member = findMember(conversation.getId(), senderId);
        if (member != null) {
            member.setLastReadMessageId(messageId);
            member.setLastReadAt(now);
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(now);
            chatConversationMemberRepository.updateById(member);
        }
    }

    private void incrementUnreadForRecipients(Long conversationId, List<Long> userIds, Long senderId) {
        for (Long recipientUserId : userIds) {
            if (Objects.equals(recipientUserId, senderId)) {
                continue;
            }
            ChatMessageReadCursor cursor = getOrCreateCursor(conversationId, recipientUserId, null, null);
            cursor.setUnreadCount(Objects.requireNonNullElse(cursor.getUnreadCount(), 0) + 1);
            saveOrUpdateCursor(cursor);
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
            chatMessageRecipientRepository.markDelivered(conversationId, recipientUserId, messageId, now);
            advanceCursorDeliveredState(conversationId, recipientUserId, messageId, now);
            ChatConversationMember member = findMember(conversationId, recipientUserId);
            advanceMemberDeliveredState(member, messageId, now);
        }
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
        chatMessageRecipientRepository.batchMarkDelivered(conversationId, userId, messageIds, now);
        Long maxMessageId = Collections.max(messageIds);
        advanceCursorDeliveredState(conversationId, userId, maxMessageId, now);
        ChatConversationMember member = findMember(conversationId, userId);
        advanceMemberDeliveredState(member, maxMessageId, now);
        for (ChatMessageHistoryItem item : items) {
            if (messageIds.contains(item.getId())) {
                item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_DELIVERED);
            }
        }
    }

    private long countUnread(Long conversationId, Long userId) {
        return chatMessageRecipientRepository.countUnread(conversationId, userId);
    }

    private ChatMessageHistoryItem requireVisibleMessage(Long userId, Long conversationId, Long messageId) {
        ChatMessageHistoryItem item = chatMessageRepository.selectVisibleMessageById(conversationId, userId, messageId);
        ExceptionThrowerCore.throwBusinessIfNull(item, ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在或不可访问");
        return item;
    }

    /**
     * 校验文件引用是否可被聊天消息消费，并收口到文件模块统一的真实文件实体。
     */
    private PreparedFileMessage prepareFileMessage(Long userId, Long businessId) {
        FileBusinessInfo sourceReference = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIfNull(sourceReference, ResultErrorCode.ILLEGAL_ARGUMENT, "文件业务引用不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(sourceReference.getUserId(), userId), ResultErrorCode.FORBIDDEN, "不能发送他人的文件");
        String referenceType = StrUtils.trimToNull(sourceReference.getReferenceType());
        boolean tempReference = Objects.equals(referenceType, "temp");
        boolean chatReference = Objects.equals(referenceType, ChatConstants.FILE_MESSAGE_REFERENCE_TYPE);
        ExceptionThrowerCore.throwBusinessIf(!tempReference && !chatReference, ResultErrorCode.ILLEGAL_ARGUMENT, "当前文件引用不能直接用于聊天消息");
        ExceptionThrowerCore.throwBusinessIf(chatReference && sourceReference.getReferenceId() != null && sourceReference.getReferenceId() > 0L,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文件已经绑定到聊天消息");
        FileInfo fileInfo = fileInfoRepository.getById(sourceReference.getFileId());
        ExceptionThrowerCore.throwBusinessIf(fileInfo == null || !Objects.equals(fileInfo.getStatus(), FileStatusEnum.NORMAL.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件不存在或不可发送");
        return new PreparedFileMessage(sourceReference, fileInfo);
    }

    private ChatMessage buildFileMessage(Long conversationId,
                                         Long userId,
                                         String clientMessageId,
                                         Long replyMessageId,
                                         FileInfo fileInfo) {
        String messageType = resolveAttachmentMessageType(fileInfo);
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setMessageType(messageType);
        message.setContent(buildFileMessageSummary(messageType, fileInfo));
        message.setSendStatus(ChatConstants.SEND_STATUS_SENT);
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);
        message.setMentionAll(0);
        message.setReplyMessageId(replyMessageId);
        message.setClientMessageId(StrUtils.trimToNull(clientMessageId));
        return message;
    }

    /**
     * 把上传阶段的业务引用收口为聊天消息引用，避免聊天模块直接维护文件元数据。
     */
    private ChatFilePayloadVO bindFileReferenceToMessage(PreparedFileMessage preparedFile, Long messageId, String messageType) {
        FileBusinessInfo sourceReference = preparedFile.sourceReference();
        FileInfo fileInfo = preparedFile.fileInfo();
        FileBusinessInfo chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                fileInfo.getId(),
                sourceReference.getUserId(),
                ChatConstants.FILE_MESSAGE_REFERENCE_TYPE,
                messageId
        );
        if (chatReference == null) {
            chatReference = new FileBusinessInfo();
            chatReference.setFileId(fileInfo.getId());
            chatReference.setUserId(sourceReference.getUserId());
            chatReference.setReferenceType(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE);
            chatReference.setReferenceId(messageId);
            chatReference.setSourceIp(sourceReference.getSourceIp());
            chatReference.setIsPublic(sourceReference.getIsPublic());
            chatReference.setCategory(ChatConstants.FILE_MESSAGE_CATEGORY);
            chatReference.setRemark(sourceReference.getRemark());
            try {
                fileBusinessInfoRepository.save(chatReference);
            } catch (DuplicateKeyException ex) {
                chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                        fileInfo.getId(),
                        sourceReference.getUserId(),
                        ChatConstants.FILE_MESSAGE_REFERENCE_TYPE,
                        messageId
                );
            }
        }
        if (sourceReference.getId() != null && !Objects.equals(sourceReference.getId(), chatReference.getId())) {
            fileBusinessInfoRepository.removeById(sourceReference.getId());
        }
        fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), Integer.valueOf(1).equals(chatReference.getIsPublic()));
        return buildFilePayload(chatReference, fileInfo, messageType);
    }

    private ChatMessage requireEditableOwnTextMessage(Long userId, Long messageId) {
        ChatMessage message = requireVisibleMessageEntity(userId, messageId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getSenderId(), userId), ResultErrorCode.FORBIDDEN, "只能编辑自己发送的消息");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getMessageType(), ChatConstants.MESSAGE_TYPE_TEXT), ResultErrorCode.ILLEGAL_ARGUMENT, "只有文本消息支持编辑");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED), ResultErrorCode.ILLEGAL_ARGUMENT, "已撤回消息不能编辑");
        return message;
    }

    /**
     * 校验回复目标消息对当前用户仍可见，避免把 reply 指向越权、已删除或已隐藏的消息。
     */
    private ChatMessageHistoryItem resolveReplyMessage(Long userId, Long conversationId, Long replyMessageId) {
        if (replyMessageId == null) {
            return null;
        }
        return requireVisibleMessage(userId, conversationId, replyMessageId);
    }

    private RevocableMessageContext requireRevocableMessage(Long userId, Long messageId) {
        ChatMessage message = requireVisibleMessageEntity(userId, messageId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(message.getSenderId(), userId), ResultErrorCode.FORBIDDEN, "只能撤回自己发送的消息");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED), ResultErrorCode.ILLEGAL_ARGUMENT, "消息已撤回");
        return new RevocableMessageContext(message);
    }

    /**
     * 撤回时同步清空文件载荷，并释放聊天消息对文件模块的业务引用。
     */
    private void revokeMessage(ChatMessage message, Long operatorUserId) {
        LocalDateTime now = LocalDateTime.now();
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_REVOKED);
        message.setRevokedBy(operatorUserId);
        message.setRevokedAt(now);
        message.setContent(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER);
        message.setPayloadJson(null);
        chatMessageRepository.updateById(message);
        if (!isAttachmentMessageType(message.getMessageType())) {
            return;
        }
        List<FileBusinessInfo> references = fileBusinessInfoRepository.listByReferenceTypeAndReferenceId(
                ChatConstants.FILE_MESSAGE_REFERENCE_TYPE,
                message.getId()
        );
        if (references.isEmpty()) {
            return;
        }
        Set<Long> fileIds = references.stream()
                .map(FileBusinessInfo::getFileId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        fileBusinessInfoRepository.removeByIds(references.stream().map(FileBusinessInfo::getId).toList());
        fileIds.forEach(fileLifecycleService::syncFileAfterReferenceRemoval);
    }

    private ChatMessage requireVisibleMessageEntity(Long userId, Long messageId) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        ChatMessageRecipient recipient = chatMessageRecipientRepository.findVisibleByUserAndMessage(userId, messageId);
        ExceptionThrowerCore.throwBusinessIfNull(recipient, ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在或不可访问");
        ChatMessage message = chatMessageRepository.getById(messageId);
        ExceptionThrowerCore.throwBusinessIfNull(message, ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在或不可访问");
        return message;
    }

    private ChatConversationMember requireActiveGroupMember(Long conversationId, Long memberUserId) {
        ChatConversationMember member = findMember(conversationId, memberUserId);
        ExceptionThrowerCore.throwBusinessIf(member == null || !Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标成员不存在或已失效");
        return member;
    }

    /**
     * 群主可管理管理员和普通成员，管理员只能管理普通成员。
     */
    private void validateManagerCanOperateMember(ChatConversationMember manager, ChatConversationMember targetMember) {
        ExceptionThrowerCore.throwBusinessIf(manager == null || !isGroupManager(manager), ResultErrorCode.FORBIDDEN, "当前角色不能管理成员");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(targetMember.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER), ResultErrorCode.FORBIDDEN, "不能操作群主");
        if (Objects.equals(manager.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN)) {
            ExceptionThrowerCore.throwBusinessIf(!Objects.equals(targetMember.getMemberRole(), ChatConstants.MEMBER_ROLE_MEMBER), ResultErrorCode.FORBIDDEN, "管理员只能操作普通成员");
        }
    }

    private boolean isGroupManager(ChatConversationMember member) {
        if (member == null) {
            return false;
        }
        return Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)
                || Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN);
    }

    private ChatMessageHistoryItem findExistingMessage(Long userId, String clientMessageId, Long conversationId) {
        clientMessageId = StrUtils.trimToNull(clientMessageId);
        if (!StrUtils.hasText(clientMessageId)) {
            return null;
        }
        ChatMessage message = chatMessageRepository.findBySenderAndClientMessageId(userId, clientMessageId);
        if (message == null || !Objects.equals(message.getConversationId(), conversationId)) {
            return null;
        }
        return chatMessageRepository.selectVisibleMessageById(conversationId, userId, message.getId());
    }

    private ChatMessageVO resolveDuplicateClientMessage(Long userId,
                                                        String clientMessageId,
                                                        Long conversationId,
                                                        DuplicateKeyException ex) {
        ChatMessageHistoryItem existing = findExistingMessage(userId, clientMessageId, conversationId);
        if (existing == null) {
            throw ex;
        }
        return buildMessageVO(userId, existing, loadUsers(Set.of(existing.getSenderId())));
    }

    private ChatConversationVO getConversationVO(Long userId, Long conversationId) {
        ChatConversationListItem item = chatConversationRepository.selectConversationDetail(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIfNull(item, ResultErrorCode.ILLEGAL_ARGUMENT, "会话不存在或不可访问");
        List<ChatConversationMember> members = listActiveMembers(conversationId);
        Map<Long, List<ChatConversationMember>> memberMap = Map.of(conversationId, members);
        Map<Long, SysUser> userMap = loadUsers(collectConversationUserIds(List.of(item), memberMap));
        return buildConversationVO(userId, item, members, userMap);
    }

    private List<ChatConversationVO> buildConversationRecords(Long userId, List<ChatConversationListItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ChatConversationMember>> membersByConversation = listActiveMembersByConversationIds(items.stream().map(ChatConversationListItem::getId).toList());
        Map<Long, SysUser> userMap = loadUsers(collectConversationUserIds(items, membersByConversation));
        List<ChatConversationVO> records = new ArrayList<>();
        for (ChatConversationListItem item : items) {
            records.add(buildConversationVO(userId, item, membersByConversation.getOrDefault(item.getId(), List.of()), userMap));
        }
        return records;
    }

    private ChatConversationVO buildConversationVO(Long userId,
                                                   ChatConversationListItem item,
                                                   List<ChatConversationMember> members,
                                                   Map<Long, SysUser> userMap) {
        ChatConversationVO vo = chatModelMapper.toConversationVO(item);
        vo.setMemberCount((long) members.size());
        vo.setUnreadCount(Objects.requireNonNullElse(item.getUnreadCount(), 0));
        if (item.getLastMessageId() != null) {
            var lastMessage = chatModelMapper.toConversationLastMessageVO(item);
            SysUser sender = userMap.get(item.getLastMessageSenderId());
            lastMessage.setSenderNickname(displayName(sender, item.getLastMessageSenderId()));
            vo.setLastMessage(lastMessage);
        }
        if (Objects.equals(item.getConversationType(), ChatConstants.CONVERSATION_TYPE_SINGLE)) {
            ChatConversationMember targetMember = members.stream()
                    .filter(member -> !Objects.equals(member.getUserId(), userId))
                    .findFirst()
                    .orElse(null);
            if (targetMember != null) {
                SysUser targetUser = userMap.get(targetMember.getUserId());
                vo.setTargetUserId(targetMember.getUserId());
                vo.setTargetUsername(targetUser != null ? targetUser.getUsername() : null);
                vo.setTargetNickname(targetUser != null ? targetUser.getNickname() : null);
                vo.setName(displayName(targetUser, targetMember.getUserId()));
                vo.setAvatar(targetUser != null ? targetUser.getAvatar() : null);
            }
        }
        return vo;
    }

    private List<ChatMemberVO> buildMemberRecords(List<ChatConversationMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = loadUsers(members.stream().map(ChatConversationMember::getUserId).collect(LinkedHashSet::new, Set::add, Set::addAll));
        List<ChatMemberVO> records = new ArrayList<>();
        members.stream()
                .sorted(Comparator.comparingInt(this::memberRoleOrder)
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

    private ChatMessageVO buildMessageVO(Long currentUserId, ChatMessageHistoryItem item, Map<Long, SysUser> userMap) {
        Map<Long, ChatReplyMessageVO> replySnapshots = item.getReplyMessageId() == null
                ? Map.of()
                : loadReplySnapshotsForVisibleMessages(currentUserId, item.getConversationId(), List.of(item.getReplyMessageId()));
        return buildMessageVO(currentUserId, item, userMap, replySnapshots);
    }

    private ChatMessageVO buildMessageVO(Long currentUserId,
                                         ChatMessageHistoryItem item,
                                         Map<Long, SysUser> userMap,
                                         Map<Long, ChatReplyMessageVO> replySnapshots) {
        ChatMessageVO vo = chatModelMapper.toMessageVO(item);
        SysUser sender = userMap.get(item.getSenderId());
        vo.setSenderUsername(sender != null ? sender.getUsername() : null);
        vo.setSenderNickname(displayName(sender, item.getSenderId()));
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setFile(extractFilePayload(item.getPayloadJson()));
        vo.setReplyMessageId(item.getReplyMessageId());
        vo.setReply(resolveReplySnapshot(item, item.getPayloadJson(), replySnapshots));
        vo.setSelf(Objects.equals(currentUserId, item.getSenderId()));
        vo.setReadByCurrentUser(item.getDeliveryStatus() != null && item.getDeliveryStatus() >= ChatConstants.DELIVERY_STATUS_READ);
        vo.setRevoked(Objects.equals(item.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED));
        vo.setEdited(isEdited(item.getMessageType(), item.getCreatedAt(), item.getUpdatedAt()));
        return vo;
    }

    private ChatReadStateVO toReadStateVO(ChatMessageReadCursor cursor, Long userId) {
        ChatReadStateVO vo = chatModelMapper.toReadStateVO(cursor);
        vo.setUserId(userId);
        return vo;
    }

    private Map<Long, List<ChatConversationMember>> listActiveMembersByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        List<ChatConversationMember> members = chatConversationMemberRepository.listActiveByConversationIds(conversationIds);
        Map<Long, List<ChatConversationMember>> result = new LinkedHashMap<>();
        for (ChatConversationMember member : members) {
            result.computeIfAbsent(member.getConversationId(), key -> new ArrayList<>()).add(member);
        }
        return result;
    }

    private List<ChatConversationMember> listActiveMembers(Long conversationId) {
        return chatConversationMemberRepository.listActiveByConversationId(conversationId);
    }

    private Set<Long> collectConversationUserIds(List<ChatConversationListItem> items,
                                                 Map<Long, List<ChatConversationMember>> membersByConversation) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatConversationListItem item : items) {
            if (item.getLastMessageSenderId() != null) {
                userIds.add(item.getLastMessageSenderId());
            }
            for (ChatConversationMember member : membersByConversation.getOrDefault(item.getId(), List.of())) {
                userIds.add(member.getUserId());
            }
        }
        return userIds;
    }

    private Set<Long> collectSenderIds(List<ChatMessageHistoryItem> items) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (ChatMessageHistoryItem item : items) {
            userIds.add(item.getSenderId());
        }
        return userIds;
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser user : sysUserRepository.listByIds(userIds)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private SysUser requireActiveUser(Long userId, boolean allowSelf) {
        ExceptionThrowerCore.throwBusinessIfNull(userId, ResultErrorCode.USER_NOT_FOUND, "用户不存在");
        Long currentUserId = SecurityUtils.getUserId();
        if (!allowSelf && Objects.equals(currentUserId, userId)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "不能操作自己");
        }
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(user == null || !Objects.equals(user.getDeletedFlag(), 0), ResultErrorCode.USER_NOT_FOUND, "用户不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(user.getStatus(), 1), ResultErrorCode.ILLEGAL_ARGUMENT, "目标用户不可用");
        return user;
    }

    private Map<Long, SysUser> requireActiveUsers(Collection<Long> userIds, boolean allowSelf) {
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        if (userIds == null) {
            return userMap;
        }
        for (Long userId : userIds) {
            userMap.put(userId, requireActiveUser(userId, allowSelf));
        }
        return userMap;
    }

    private List<Long> normalizeMemberIds(Collection<Long> memberUserIds, Long excludeUserId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (memberUserIds != null) {
            for (Long memberUserId : memberUserIds) {
                if (memberUserId != null && !Objects.equals(memberUserId, excludeUserId)) {
                    ids.add(memberUserId);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private ChatConversationMember findMember(Long conversationId, Long userId) {
        return chatConversationMemberRepository.findByConversationAndUser(conversationId, userId);
    }

    private ChatMessageReadCursor getOrCreateCursor(Long conversationId, Long userId, Long referenceMessageId, LocalDateTime referenceTime) {
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

    private void saveOrUpdateCursor(ChatMessageReadCursor cursor) {
        if (cursor.getId() == null) {
            chatMessageReadCursorRepository.save(cursor);
        } else {
            chatMessageReadCursorRepository.updateById(cursor);
        }
    }

    private ChatMessageReadCursor findCursor(Long conversationId, Long userId) {
        return chatMessageReadCursorRepository.findByConversationAndUser(conversationId, userId);
    }

    /**
     * delivered 高水位只能前进不能回退，避免并发推送时旧事务覆盖新事务的游标。
     */
    private void advanceCursorDeliveredState(Long conversationId, Long userId, Long messageId, LocalDateTime deliveredAt) {
        if (messageId == null) {
            return;
        }
        ChatMessageReadCursor cursor = getOrCreateCursor(conversationId, userId, null, null);
        if (cursor.getId() == null || (cursor.getDeliveredMessageId() != null && cursor.getDeliveredMessageId() >= messageId)) {
            return;
        }
        boolean updated = chatMessageReadCursorRepository.advanceDeliveredState(cursor.getId(), messageId, deliveredAt);
        if (updated) {
            cursor.setDeliveredMessageId(messageId);
            cursor.setDeliveredAt(deliveredAt);
        }
    }

    private void advanceMemberDeliveredState(ChatConversationMember member, Long messageId, LocalDateTime deliveredAt) {
        if (member == null || member.getId() == null || messageId == null) {
            return;
        }
        if (member.getLastDeliveredMessageId() != null && member.getLastDeliveredMessageId() >= messageId) {
            return;
        }
        boolean updated = chatConversationMemberRepository.advanceDeliveredState(member.getId(), messageId, deliveredAt);
        if (updated) {
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(deliveredAt);
        }
    }

    private void updateMemberReadState(ChatConversationMember member, Long messageId, LocalDateTime readAt) {
        member.setLastReadMessageId(messageId);
        member.setLastReadAt(readAt);
        if (member.getLastDeliveredMessageId() == null || member.getLastDeliveredMessageId() < messageId) {
            member.setLastDeliveredMessageId(messageId);
            member.setLastDeliveredAt(readAt);
        }
        chatConversationMemberRepository.updateById(member);
    }

    private String buildSinglePairKey(Long userId, Long targetUserId) {
        long left = Math.min(userId, targetUserId);
        long right = Math.max(userId, targetUserId);
        return left + ":" + right;
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

    private String trimKeyword(String keyword) {
        return StrUtils.trimToNull(keyword);
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

    private String buildFileMessageSummary(String messageType, FileInfo fileInfo) {
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

    private ChatMessagePayloadVO parseMessagePayload(String payloadJson) {
        if (!StrUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(payloadJson, ChatMessagePayloadVO.class);
            if (payload != null && (payload.getFile() != null || payload.getReply() != null)) {
                return payload;
            }
            ChatFilePayloadVO legacyFilePayload = JsonUtils.fromJson(payloadJson, ChatFilePayloadVO.class);
            if (hasFilePayloadContent(legacyFilePayload)) {
                ChatMessagePayloadVO legacyPayload = new ChatMessagePayloadVO();
                legacyPayload.setFile(legacyFilePayload);
                return legacyPayload;
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }

    private ChatFilePayloadVO extractFilePayload(String payloadJson) {
        ChatMessagePayloadVO payload = parseMessagePayload(payloadJson);
        return payload == null ? null : payload.getFile();
    }

    private ChatReplyMessageVO extractReplyPayload(String payloadJson) {
        ChatMessagePayloadVO payload = parseMessagePayload(payloadJson);
        return payload == null ? null : payload.getReply();
    }

    private String buildMessagePayloadJson(ChatFilePayloadVO filePayload, ChatReplyMessageVO replySnapshot) {
        if (filePayload == null && replySnapshot == null) {
            return null;
        }
        ChatMessagePayloadVO payload = chatModelMapper.toMessagePayloadVO(filePayload, replySnapshot);
        return JsonUtils.toJson(payload);
    }

    private boolean isEdited(String messageType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_TEXT)
                && createdAt != null
                && updatedAt != null
                && updatedAt.isAfter(createdAt);
    }

    private boolean isAttachmentMessageType(String messageType) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_FILE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE);
    }

    private String resolveAttachmentMessageType(FileInfo fileInfo) {
        String mimeType = fileInfo == null ? null : StrUtils.trimToNull(fileInfo.getMimeType());
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

    private ChatFilePayloadVO buildFilePayload(FileBusinessInfo chatReference, FileInfo fileInfo, String messageType) {
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
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE) && !StrUtils.hasText(payload.getThumbnailUrl())) {
            payload.setThumbnailUrl(fileInfo.getFileUrl());
        }
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE)) {
            payload.setTranscodeStatus(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_PENDING);
        } else {
            payload.setTranscodeStatus(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE);
        }
        return payload;
    }

    private String metricResultOf(RuntimeException ex) {
        return ex instanceof BusinessException ? "business_error" : "system_error";
    }

    /**
     * 为新发消息持久化一份轻量 reply 快照，减少后续展示对原消息实时查询的强依赖。
     */
    private ChatReplyMessageVO buildReplySnapshot(ChatMessageHistoryItem item) {
        if (item == null) {
            return null;
        }
        return buildReplySnapshot(item, loadUsers(Set.of(item.getSenderId())));
    }

    private ChatReplyMessageVO buildReplySnapshot(ChatMessageHistoryItem item, Map<Long, SysUser> userMap) {
        ChatReplyMessageVO reply = new ChatReplyMessageVO();
        reply.setId(item.getId());
        reply.setSenderId(item.getSenderId());
        SysUser sender = userMap.get(item.getSenderId());
        reply.setSenderUsername(sender != null ? sender.getUsername() : null);
        reply.setSenderNickname(displayName(sender, item.getSenderId()));
        reply.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        reply.setMessageType(item.getMessageType());
        reply.setReplyToMessageId(item.getReplyMessageId());
        reply.setContent(item.getContent());
        reply.setFile(extractFilePayload(item.getPayloadJson()));
        boolean revoked = Objects.equals(item.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED);
        reply.setRevoked(revoked);
        reply.setDeleted(false);
        reply.setState(revoked ? ChatConstants.REPLY_STATE_REVOKED : ChatConstants.REPLY_STATE_NORMAL);
        reply.setCreatedAt(item.getCreatedAt());
        return reply;
    }

    private ChatReplyMessageVO resolveReplySnapshot(ChatMessageHistoryItem item,
                                                    String payloadJson,
                                                    Map<Long, ChatReplyMessageVO> replySnapshots) {
        if (item.getReplyMessageId() == null) {
            return null;
        }
        ChatReplyMessageVO liveReply = replySnapshots.get(item.getReplyMessageId());
        if (liveReply != null && !Boolean.TRUE.equals(liveReply.getDeleted())) {
            return liveReply;
        }
        ChatReplyMessageVO payloadReply = normalizeReplySnapshot(extractReplyPayload(payloadJson));
        if (payloadReply != null) {
            return payloadReply;
        }
        return liveReply != null ? liveReply : buildUnavailableReplySnapshot(item.getReplyMessageId());
    }
    /**
     * 批量加载当前用户仍可见的 reply 原消息；缺失记录会统一补成 unavailable 占位，保证历史消息渲染稳定。
     */
    private Map<Long, ChatReplyMessageVO> loadReplySnapshotsForVisibleMessages(Long userId,
                                                                               Long conversationId,
                                                                               Collection<Long> replyMessageIds) {
        List<Long> ids = replyMessageIds == null ? List.of() : replyMessageIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ChatMessageHistoryItem> replyItems = Objects.requireNonNullElse(
                chatMessageRepository.selectVisibleMessagesByIds(conversationId, userId, ids),
                List.of()
        );
        Map<Long, SysUser> userMap = loadUsers(collectSenderIds(replyItems));
        Map<Long, ChatReplyMessageVO> result = new LinkedHashMap<>();
        for (ChatMessageHistoryItem replyItem : replyItems) {
            result.put(replyItem.getId(), buildReplySnapshot(replyItem, userMap));
        }
        for (Long id : ids) {
            result.putIfAbsent(id, buildUnavailableReplySnapshot(id));
        }
        return result;
    }

    private List<Long> collectReplyMessageIds(Collection<ChatMessageHistoryItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(ChatMessageHistoryItem::getReplyMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private ChatReplyMessageVO buildUnavailableReplySnapshot(Long replyMessageId) {
        ChatReplyMessageVO reply = new ChatReplyMessageVO();
        reply.setId(replyMessageId);
        reply.setContent(ChatConstants.REPLY_MESSAGE_UNAVAILABLE_PLACEHOLDER);
        reply.setDeleted(true);
        reply.setRevoked(false);
        reply.setState(ChatConstants.REPLY_STATE_UNAVAILABLE);
        return reply;
    }

    private ChatReplyMessageVO normalizeReplySnapshot(ChatReplyMessageVO reply) {
        if (reply == null) {
            return null;
        }
        if (!StrUtils.hasText(reply.getState())) {
            if (Boolean.TRUE.equals(reply.getDeleted())) {
                reply.setState(ChatConstants.REPLY_STATE_UNAVAILABLE);
            } else if (Boolean.TRUE.equals(reply.getRevoked())) {
                reply.setState(ChatConstants.REPLY_STATE_REVOKED);
            } else {
                reply.setState(ChatConstants.REPLY_STATE_NORMAL);
            }
        }
        return reply;
    }

    private boolean hasFilePayloadContent(ChatFilePayloadVO payload) {
        return payload != null
                && (payload.getBusinessId() != null
                || payload.getFileId() != null
                || StrUtils.hasText(payload.getFileName())
                || StrUtils.hasText(payload.getFileUrl()));
    }

    private List<Long> listActiveUserIds(Long conversationId) {
        return activeUserIds(listActiveMembers(conversationId));
    }

    private List<Long> activeUserIds(Collection<ChatConversationMember> members) {
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

    private record ConversationAccessContext(ChatConversation conversation,
                                             ChatConversationMember selfMember,
                                             List<ChatConversationMember> activeMembers) {
        private List<Long> activeUserIds() {
            return activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
        }
    }

    private record PreparedFileMessage(FileBusinessInfo sourceReference, FileInfo fileInfo) {
    }

    private record RevocableMessageContext(ChatMessage message) {
    }
}




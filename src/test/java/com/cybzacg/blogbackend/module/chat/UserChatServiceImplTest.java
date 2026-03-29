package com.cybzacg.blogbackend.module.chat;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
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
import com.cybzacg.blogbackend.mapper.ChatConversationMapper;
import com.cybzacg.blogbackend.mapper.ChatMessageMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.user.ChatEditMessageRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMarkReadRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatOpenSingleConversationRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationMemberService;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageReadCursorService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageRecipientService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.service.impl.UserChatServiceImpl;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserChatServiceImplTest {
    @Mock
    private ChatConversationService chatConversationService;
    @Mock
    private ChatConversationMemberService chatConversationMemberService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatMessageRecipientService chatMessageRecipientService;
    @Mock
    private ChatMessageReadCursorService chatMessageReadCursorService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private ChatConversationMapper chatConversationMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatModelMapper chatModelMapper;
    @Mock
    private ChatPushService chatPushService;
    @Mock
    private ChatWebSocketSessionRegistry chatWebSocketSessionRegistry;
    @Mock
    private FileBusinessInfoService fileBusinessInfoService;
    @Mock
    private FileInfoService fileInfoService;
    @Mock
    private FileLifecycleService fileLifecycleService;

    @Mock
    private LambdaQueryChainWrapper<ChatConversation> conversationQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> memberFindSelfQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> memberFindTargetQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> groupOwnerQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatConversationMember> groupActiveMembersQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatMessageReadCursor> currentUserCursorQuery;
    @Mock
    private LambdaQueryChainWrapper<ChatMessageReadCursor> targetUserCursorQuery;

    private UserChatServiceImpl userChatService;

    @BeforeEach
    void setUp() {
        userChatService = new UserChatServiceImpl(
                chatConversationService,
                chatConversationMemberService,
                chatMessageService,
                chatMessageRecipientService,
                chatMessageReadCursorService,
                sysUserService,
                chatConversationMapper,
                chatMessageMapper,
                chatModelMapper,
                chatPushService,
                chatWebSocketSessionRegistry,
                fileBusinessInfoService,
                fileInfoService,
                fileLifecycleService
        );
    }

    @Test
    void openSingleConversationShouldReuseConversationAndCreateMissingMemberships() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setDeletedFlag(0);
        targetUser.setStatus(1);
        targetUser.setUsername("zhangsan");
        targetUser.setNickname("张三");

        SysUser currentUser = new SysUser();
        currentUser.setId(currentUserId);
        currentUser.setDeletedFlag(0);
        currentUser.setStatus(1);
        currentUser.setUsername("admin");
        currentUser.setNickname("管理员");

        ChatConversation conversation = new ChatConversation();
        conversation.setId(1001L);
        conversation.setSinglePairKey("1:2");
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setIsAllSite(0);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(1001L);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(1001L);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember mappedSelfMember = new ChatConversationMember();
        mappedSelfMember.setConversationId(1001L);
        mappedSelfMember.setUserId(currentUserId);

        ChatConversationMember mappedTargetMember = new ChatConversationMember();
        mappedTargetMember.setConversationId(1001L);
        mappedTargetMember.setUserId(targetUserId);

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(1001L);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(1001L);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        when(sysUserService.getById(targetUserId)).thenReturn(targetUser);
        when(chatConversationService.lambdaQuery()).thenReturn(conversationQuery);
        when(conversationQuery.eq(anySFunction(), any())).thenReturn(conversationQuery);
        when(conversationQuery.last(any())).thenReturn(conversationQuery);
        when(conversationQuery.one()).thenReturn(conversation);

        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberFindSelfQuery, memberFindTargetQuery, activeMembersQuery);
        mockMemberFindQuery(memberFindSelfQuery, null);
        mockMemberFindQuery(memberFindTargetQuery, null);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));

        when(chatModelMapper.toConversationMember(1001L, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedSelfMember);
        when(chatModelMapper.toConversationMember(1001L, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedTargetMember);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(currentUserCursorQuery, targetUserCursorQuery);
        mockCursorFindQuery(currentUserCursorQuery, null);
        mockCursorFindQuery(targetUserCursorQuery, null);
        when(chatMessageReadCursorService.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorService.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatConversationMapper.selectConversationDetail(1001L, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(currentUser, targetUser));
        when(chatConversationMemberService.save(any(ChatConversationMember.class))).thenReturn(true);

        ChatOpenSingleConversationRequest request = new ChatOpenSingleConversationRequest();
        request.setTargetUserId(targetUserId);

        ChatConversationVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.openSingleConversation(request);
        }

        assertEquals(1001L, result.getId());
        assertEquals(targetUserId, result.getTargetUserId());
        assertEquals("zhangsan", result.getTargetUsername());
        assertEquals("张三", result.getTargetNickname());
        assertEquals("张三", result.getName());
        assertEquals(2L, result.getMemberCount());
        verify(chatConversationService, never()).save(any(ChatConversation.class));
        verify(chatConversationMemberService).save(mappedSelfMember);
        verify(chatConversationMemberService).save(mappedTargetMember);
    }

    @Test
    void openSingleConversationShouldRejectDisabledTargetUser() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setDeletedFlag(0);
        targetUser.setStatus(0);

        when(sysUserService.getById(targetUserId)).thenReturn(targetUser);

        ChatOpenSingleConversationRequest request = new ChatOpenSingleConversationRequest();
        request.setTargetUserId(targetUserId);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.openSingleConversation(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("目标用户不可用", exception.getMessage());
        verify(chatConversationService, never()).lambdaQuery();
    }

    @Test
    void openSingleConversationShouldRejectSelfTarget() {
        Long currentUserId = 1L;

        SysUser selfUser = new SysUser();
        selfUser.setId(currentUserId);
        selfUser.setDeletedFlag(0);
        selfUser.setStatus(1);

        when(sysUserService.getById(currentUserId)).thenReturn(selfUser);

        ChatOpenSingleConversationRequest request = new ChatOpenSingleConversationRequest();
        request.setTargetUserId(currentUserId);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.openSingleConversation(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("不能给自己发送单聊消息", exception.getMessage());
    }

    @Test
    void leaveGroupShouldRejectOwnerLeaving() {
        Long currentUserId = 1L;
        Long conversationId = 2001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setJoinedAt(new Date());

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(groupOwnerQuery, groupActiveMembersQuery);
        mockMemberFindQuery(groupOwnerQuery, ownerMember);
        when(groupActiveMembersQuery.eq(anySFunction(), any())).thenReturn(groupActiveMembersQuery);
        when(groupActiveMembersQuery.list()).thenReturn(List.of(ownerMember));

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.leaveGroup(conversationId));
        }

        assertEquals(ResultErrorCode.UNSUPPORTED_OPERATION.getCode(), exception.getCode());
        assertEquals("群主不能直接退群，请先解散群聊", exception.getMessage());
        verify(chatConversationMemberService, never()).updateById(any(ChatConversationMember.class));
    }

    @Test
    void sendTextMessageShouldPersistRecipientsAndPushMessage() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 3001L;
        Long messageId = 9001L;
        Date now = new Date();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessage mappedMessage = new ChatMessage();
        mappedMessage.setContent("hello");

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(messageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(currentUserId);
        historyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        historyItem.setContent("hello");
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        historyItem.setCreatedAt(now);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);
        messageVO.setConversationId(conversationId);

        SysUser sender = new SysUser();
        sender.setId(currentUserId);
        sender.setUsername("admin");
        sender.setNickname("管理员");

        ChatMessageReadCursor senderCursor = new ChatMessageReadCursor();
        senderCursor.setId(11L);
        senderCursor.setConversationId(conversationId);
        senderCursor.setUserId(currentUserId);
        senderCursor.setUnreadCount(0);

        ChatMessageReadCursor targetCursor = new ChatMessageReadCursor();
        targetCursor.setId(12L);
        targetCursor.setConversationId(conversationId);
        targetCursor.setUserId(targetUserId);
        targetCursor.setUnreadCount(0);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> senderMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> targetMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> senderCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> targetCursorUnreadQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> targetCursorDeliveredQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> recipientDeliveredUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(
                selfQuery,
                activeMembersQuery,
                secondSelfQuery,
                secondActiveMembersQuery,
                senderMemberQuery,
                targetMemberQuery
        );
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(secondSelfQuery, selfMember);
        when(secondActiveMembersQuery.eq(anySFunction(), any())).thenReturn(secondActiveMembersQuery);
        when(secondActiveMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(senderMemberQuery, selfMember);
        mockMemberFindQuery(targetMemberQuery, targetMember);

        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageService.save(mappedMessage)).thenAnswer(invocation -> {
            mappedMessage.setId(messageId);
            mappedMessage.setCreatedAt(now);
            return true;
        });
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientService.saveBatch(anyCollection())).thenReturn(true);
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(recipientDeliveredUpdate);
        mockRecipientUpdateQuery(recipientDeliveredUpdate);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(senderCursorQuery, targetCursorUnreadQuery, targetCursorDeliveredQuery);
        mockCursorFindQuery(senderCursorQuery, senderCursor);
        mockCursorFindQuery(targetCursorUnreadQuery, targetCursor);
        mockCursorFindQuery(targetCursorDeliveredQuery, targetCursor);
        when(chatMessageReadCursorService.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of(mock(WebSocketSession.class)));
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatConversationMemberService.updateById(any(ChatConversationMember.class))).thenReturn(true);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        assertEquals(currentUserId, mappedMessage.getSenderId());
        assertEquals(conversationId, mappedMessage.getConversationId());
        assertEquals(messageId, conversation.getLastMessageId());
        assertEquals("管理员", result.getSenderNickname());

        verify(chatMessageRecipientService).saveBatch(anyCollection());
        verify(chatPushService).pushMessageCreated(messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void markReadShouldUpdateCursorAndPushReadState() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 4001L;
        Long readMessageId = 9002L;
        Date previousTime = new Date(System.currentTimeMillis() - 10_000L);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setLastReadMessageId(9000L);
        selfMember.setLastDeliveredMessageId(9000L);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(readMessageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(senderUserId);
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_DELIVERED);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(21L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setReadMessageId(9000L);
        cursor.setReadAt(previousTime);
        cursor.setDeliveredMessageId(9000L);
        cursor.setDeliveredAt(previousTime);
        cursor.setUnreadCount(3);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageRecipient> unreadCountQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> recipientReadUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));

        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(recipientReadUpdate);
        mockRecipientReadUpdateQuery(recipientReadUpdate);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(unreadCountQuery);
        when(unreadCountQuery.eq(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.lt(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.count()).thenReturn(0L);
        when(chatMessageReadCursorService.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberService.updateById(selfMember)).thenReturn(true);
        when(chatModelMapper.toReadStateVO(cursor)).thenAnswer(invocation -> {
            ChatMessageReadCursor source = invocation.getArgument(0);
            ChatReadStateVO stateVO = new ChatReadStateVO();
            stateVO.setConversationId(source.getConversationId());
            stateVO.setReadMessageId(source.getReadMessageId());
            stateVO.setDeliveredMessageId(source.getDeliveredMessageId());
            stateVO.setUnreadCount(source.getUnreadCount());
            return stateVO;
        });

        ChatMarkReadRequest request = new ChatMarkReadRequest();
        request.setReadMessageId(readMessageId);

        ChatReadStateVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.markRead(conversationId, request);
        }

        assertEquals(conversationId, result.getConversationId());
        assertEquals(currentUserId, result.getUserId());
        assertEquals(readMessageId, result.getReadMessageId());
        assertEquals(readMessageId, result.getDeliveredMessageId());
        assertEquals(Integer.valueOf(0), result.getUnreadCount());
        assertEquals(readMessageId, selfMember.getLastReadMessageId());
        assertEquals(readMessageId, selfMember.getLastDeliveredMessageId());
        assertNotNull(selfMember.getLastReadAt());
        verify(chatPushService).pushReadUpdated(result, List.of(currentUserId, senderUserId));
    }

    @Test
    void markReadShouldKeepUnreadCountWhenUnreadMessagesRemain() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 4002L;
        Long readMessageId = 9003L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(readMessageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(senderUserId);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(22L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setReadMessageId(9000L);
        cursor.setDeliveredMessageId(9000L);
        cursor.setUnreadCount(4);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageRecipient> unreadCountQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> recipientReadUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(recipientReadUpdate);
        mockRecipientReadUpdateQuery(recipientReadUpdate);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(unreadCountQuery);
        when(unreadCountQuery.eq(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.lt(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.count()).thenReturn(2L);
        when(chatMessageReadCursorService.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberService.updateById(selfMember)).thenReturn(true);
        when(chatModelMapper.toReadStateVO(cursor)).thenAnswer(invocation -> {
            ChatMessageReadCursor source = invocation.getArgument(0);
            ChatReadStateVO stateVO = new ChatReadStateVO();
            stateVO.setConversationId(source.getConversationId());
            stateVO.setReadMessageId(source.getReadMessageId());
            stateVO.setDeliveredMessageId(source.getDeliveredMessageId());
            stateVO.setUnreadCount(source.getUnreadCount());
            return stateVO;
        });

        ChatReadStateVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.markRead(currentUserId, conversationId, readMessageId);
        }

        assertEquals(Integer.valueOf(2), result.getUnreadCount());
        assertEquals(Integer.valueOf(2), cursor.getUnreadCount());
        assertEquals(readMessageId, cursor.getReadMessageId());
        verify(chatPushService).pushReadUpdated(result, List.of(currentUserId, senderUserId));
    }

    @Test
    void markReadShouldKeepDeliveredCursorWhenItAlreadyPointsToNewerMessage() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 4003L;
        Long readMessageId = 9004L;
        Long deliveredMessageId = 9010L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setLastReadMessageId(9000L);
        selfMember.setLastDeliveredMessageId(deliveredMessageId);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(readMessageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(senderUserId);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(23L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setReadMessageId(9000L);
        cursor.setDeliveredMessageId(deliveredMessageId);
        cursor.setUnreadCount(6);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageRecipient> unreadCountQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> recipientReadUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(recipientReadUpdate);
        mockRecipientReadUpdateQuery(recipientReadUpdate);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(unreadCountQuery);
        when(unreadCountQuery.eq(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.lt(anySFunction(), any())).thenReturn(unreadCountQuery);
        when(unreadCountQuery.count()).thenReturn(3L);
        when(chatMessageReadCursorService.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberService.updateById(selfMember)).thenReturn(true);
        when(chatModelMapper.toReadStateVO(cursor)).thenAnswer(invocation -> {
            ChatMessageReadCursor source = invocation.getArgument(0);
            ChatReadStateVO stateVO = new ChatReadStateVO();
            stateVO.setConversationId(source.getConversationId());
            stateVO.setReadMessageId(source.getReadMessageId());
            stateVO.setDeliveredMessageId(source.getDeliveredMessageId());
            stateVO.setUnreadCount(source.getUnreadCount());
            return stateVO;
        });

        ChatReadStateVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.markRead(currentUserId, conversationId, readMessageId);
        }

        assertEquals(readMessageId, result.getReadMessageId());
        assertEquals(deliveredMessageId, result.getDeliveredMessageId());
        assertEquals(Integer.valueOf(3), result.getUnreadCount());
        assertEquals(deliveredMessageId, cursor.getDeliveredMessageId());
        assertEquals(deliveredMessageId, selfMember.getLastDeliveredMessageId());
        verify(chatPushService).pushReadUpdated(result, List.of(currentUserId, senderUserId));
    }

    @Test
    void createGroupShouldSaveConversationAndInitializeMembers() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 5001L;

        SysUser memberUser = new SysUser();
        memberUser.setId(memberUserId);
        memberUser.setDeletedFlag(0);
        memberUser.setStatus(1);
        memberUser.setUsername("zhangsan");
        memberUser.setNickname("张三");

        SysUser ownerUser = new SysUser();
        ownerUser.setId(currentUserId);
        ownerUser.setDeletedFlag(0);
        ownerUser.setStatus(1);
        ownerUser.setUsername("admin");
        ownerUser.setNickname("管理员");

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setName("学习群");

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(memberUserId);
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(conversationId);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        detailItem.setName("学习群");

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversationVO.setName("学习群");

        LambdaQueryChainWrapper<ChatConversationMember> ownerFindQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> memberFindQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> ownerCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> memberCursorQuery = mock(LambdaQueryChainWrapper.class);

        when(sysUserService.getById(memberUserId)).thenReturn(memberUser);
        when(chatModelMapper.toGroupConversation(any(ChatCreateGroupRequest.class))).thenReturn(conversation);
        when(chatConversationService.save(conversation)).thenReturn(true);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(ownerFindQuery, memberFindQuery, activeMembersQuery);
        mockMemberFindQuery(ownerFindQuery, null);
        mockMemberFindQuery(memberFindQuery, null);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember, member));
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_OWNER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(ownerMember);
        when(chatModelMapper.toConversationMember(conversationId, memberUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(member);
        when(chatConversationMemberService.save(any(ChatConversationMember.class))).thenReturn(true);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(ownerCursorQuery, memberCursorQuery);
        mockCursorFindQuery(ownerCursorQuery, null);
        mockCursorFindQuery(memberCursorQuery, null);
        when(chatMessageReadCursorService.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorService.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatConversationMapper.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(ownerUser, memberUser));

        ChatCreateGroupRequest request = new ChatCreateGroupRequest();
        request.setName("学习群");
        request.setMemberUserIds(List.of(currentUserId, memberUserId, memberUserId));

        ChatConversationVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.createGroup(request);
        }

        assertEquals(conversationId, result.getId());
        assertEquals(ChatConstants.CONVERSATION_TYPE_GROUP, result.getConversationType());
        assertEquals(2L, result.getMemberCount());
        verify(chatConversationService).save(conversation);
        verify(chatConversationMemberService).save(ownerMember);
        verify(chatConversationMemberService).save(member);
    }

    @Test
    void createGroupShouldRejectWhenNormalizedMembersBecomeEmpty() {
        Long currentUserId = 1L;

        ChatCreateGroupRequest request = new ChatCreateGroupRequest();
        request.setMemberUserIds(java.util.Arrays.asList(null, currentUserId));

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.createGroup(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("群成员不能为空", exception.getMessage());
        verify(chatConversationService, never()).save(any(ChatConversation.class));
    }

    @Test
    void removeGroupMemberShouldMarkMemberRemoved() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 6001L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(memberUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        LambdaQueryChainWrapper<ChatConversationMember> ownerQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> targetQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(ownerQuery, activeMembersQuery, targetQuery);
        mockMemberFindQuery(ownerQuery, ownerMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember, targetMember));
        mockMemberFindQuery(targetQuery, targetMember);
        when(chatConversationMemberService.updateById(targetMember)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.removeGroupMember(conversationId, memberUserId);
        }

        assertEquals(ChatConstants.MEMBER_STATUS_REMOVED, targetMember.getStatus());
        verify(chatConversationMemberService).updateById(targetMember);
    }

    @Test
    void removeGroupMemberShouldRejectOwnerRemovingSelf() {
        Long currentUserId = 1L;
        Long conversationId = 6101L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        LambdaQueryChainWrapper<ChatConversationMember> ownerQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(ownerQuery, activeMembersQuery);
        mockMemberFindQuery(ownerQuery, ownerMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember));

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class,
                    () -> userChatService.removeGroupMember(conversationId, currentUserId));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("不能移除自己，请使用退群接口", exception.getMessage());
        verify(chatConversationMemberService, never()).updateById(any(ChatConversationMember.class));
    }

    @Test
    void sendTextMessageShouldRejectDisabledConversation() {
        Long currentUserId = 1L;
        Long conversationId = 6201L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_DISABLED);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话不存在或不可用", exception.getMessage());
        verify(chatMessageService, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendTextMessageShouldRejectBlankContent() {
        Long currentUserId = 1L;

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(6202L);
        request.setContent("   ");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("消息内容不能为空", exception.getMessage());
        verify(chatConversationService, never()).getById(any(Long.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenConversationAndTargetMissing() {
        Long currentUserId = 1L;

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话ID和目标用户ID不能同时为空", exception.getMessage());
        verify(chatConversationService, never()).getById(any(Long.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenCurrentMemberIsRemoved() {
        Long currentUserId = 1L;
        Long conversationId = 6203L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember removedMember = new ChatConversationMember();
        removedMember.setConversationId(conversationId);
        removedMember.setUserId(currentUserId);
        removedMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery);
        mockMemberFindQuery(selfQuery, removedMember);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
        verify(chatMessageService, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenCurrentMemberIsDisabled() {
        Long currentUserId = 1L;
        Long conversationId = 6204L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember disabledMember = new ChatConversationMember();
        disabledMember.setConversationId(conversationId);
        disabledMember.setUserId(currentUserId);
        disabledMember.setStatus(ChatConstants.MEMBER_STATUS_DISABLED);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery);
        mockMemberFindQuery(selfQuery, disabledMember);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
        verify(chatMessageService, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenCurrentMemberIsMuted() {
        Long currentUserId = 1L;
        Long conversationId = 6205L;
        Date muteUntil = new Date(System.currentTimeMillis() + 60_000L);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMuteUntil(muteUntil);

        LambdaQueryChainWrapper<ChatConversationMember> resolveSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> resolveActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> sendSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> sendActiveMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(
                resolveSelfQuery,
                resolveActiveMembersQuery,
                sendSelfQuery,
                sendActiveMembersQuery
        );
        mockMemberFindQuery(resolveSelfQuery, selfMember);
        when(resolveActiveMembersQuery.eq(anySFunction(), any())).thenReturn(resolveActiveMembersQuery);
        when(resolveActiveMembersQuery.list()).thenReturn(List.of(selfMember));
        mockMemberFindQuery(sendSelfQuery, selfMember);
        when(sendActiveMembersQuery.eq(anySFunction(), any())).thenReturn(sendActiveMembersQuery);
        when(sendActiveMembersQuery.list()).thenReturn(List.of(selfMember));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户已被禁言，暂时不能发送消息", exception.getMessage());
        verify(chatMessageService, never()).save(any(ChatMessage.class));
    }

    @Test
    void dissolveGroupShouldUpdateConversationAndRemoveActiveMembers() {
        Long currentUserId = 1L;
        Long conversationId = 6301L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        LambdaQueryChainWrapper<ChatConversationMember> ownerQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatConversationMember> memberRemoveUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(ownerQuery, activeMembersQuery);
        mockMemberFindQuery(ownerQuery, ownerMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember));
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatConversationMemberService.lambdaUpdate()).thenReturn(memberRemoveUpdate);
        when(memberRemoveUpdate.eq(anySFunction(), any())).thenReturn(memberRemoveUpdate);
        when(memberRemoveUpdate.set(anySFunction(), any())).thenReturn(memberRemoveUpdate);
        when(memberRemoveUpdate.update()).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.dissolveGroup(conversationId);
        }

        assertEquals(ChatConstants.CONVERSATION_STATUS_DISSOLVED, conversation.getStatus());
        verify(chatConversationService).updateById(conversation);
        verify(memberRemoveUpdate).update();
    }

    @Test
    void inviteGroupMembersShouldRestoreInactiveMemberAndReturnRecords() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7001L;
        Long lastMessageId = 888L;
        Date lastMessageTime = new Date();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setLastMessageId(lastMessageId);
        conversation.setLastMessageTime(lastMessageTime);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setJoinedAt(new Date(lastMessageTime.getTime() - 2_000L));

        ChatConversationMember inactiveMember = new ChatConversationMember();
        inactiveMember.setConversationId(conversationId);
        inactiveMember.setUserId(memberUserId);
        inactiveMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        inactiveMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        inactiveMember.setJoinedAt(new Date(lastMessageTime.getTime() - 1_000L));

        ChatMessageReadCursor memberCursor = new ChatMessageReadCursor();
        memberCursor.setId(31L);
        memberCursor.setConversationId(conversationId);
        memberCursor.setUserId(memberUserId);
        memberCursor.setUnreadCount(5);

        SysUser ownerUser = new SysUser();
        ownerUser.setId(currentUserId);
        ownerUser.setUsername("admin");
        ownerUser.setNickname("管理员");

        SysUser memberUser = new SysUser();
        memberUser.setId(memberUserId);
        memberUser.setDeletedFlag(0);
        memberUser.setStatus(1);
        memberUser.setUsername("zhangsan");
        memberUser.setNickname("张三");

        ChatMemberVO ownerVO = new ChatMemberVO();
        ownerVO.setRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerVO.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMemberVO memberVO = new ChatMemberVO();
        memberVO.setRole(ChatConstants.MEMBER_ROLE_MEMBER);
        memberVO.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        LambdaQueryChainWrapper<ChatConversationMember> ownerQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> memberFindQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> refreshedMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> memberCursorQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(ownerQuery, activeMembersQuery, memberFindQuery, refreshedMembersQuery);
        mockMemberFindQuery(ownerQuery, ownerMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember));
        mockMemberFindQuery(memberFindQuery, inactiveMember);
        when(refreshedMembersQuery.eq(anySFunction(), any())).thenReturn(refreshedMembersQuery);
        when(refreshedMembersQuery.list()).thenReturn(List.of(ownerMember, inactiveMember));
        when(chatConversationMemberService.updateById(inactiveMember)).thenReturn(true);

        when(sysUserService.getById(memberUserId)).thenReturn(memberUser);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(memberCursorQuery);
        mockCursorFindQuery(memberCursorQuery, memberCursor);
        when(chatMessageReadCursorService.updateById(memberCursor)).thenReturn(true);
        when(sysUserService.listByIds(any())).thenReturn(List.of(ownerUser, memberUser));
        when(chatModelMapper.toMemberVO(ownerMember)).thenReturn(ownerVO);
        when(chatModelMapper.toMemberVO(inactiveMember)).thenReturn(memberVO);

        ChatGroupMemberOperateRequest request = new ChatGroupMemberOperateRequest();
        request.setMemberUserIds(List.of(currentUserId, memberUserId, memberUserId));

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.inviteGroupMembers(conversationId, request);
        }

        assertEquals(2, result.size());
        assertEquals(List.of(currentUserId, memberUserId), result.stream().map(ChatMemberVO::getUserId).toList());
        assertEquals("张三", result.get(1).getNickname());
        assertEquals(ChatConstants.MEMBER_STATUS_NORMAL, inactiveMember.getStatus());
        assertEquals(ChatConstants.JOIN_SOURCE_MANUAL, inactiveMember.getJoinSource());
        assertEquals(lastMessageId, inactiveMember.getLastReadMessageId());
        assertEquals(lastMessageId, inactiveMember.getLastDeliveredMessageId());
        assertEquals(Integer.valueOf(0), memberCursor.getUnreadCount());
        assertEquals(lastMessageId, memberCursor.getReadMessageId());
        verify(chatConversationMemberService).updateById(inactiveMember);
        verify(chatConversationMemberService, never()).save(any(ChatConversationMember.class));
    }

    @Test
    void sendTextMessageShouldCreateSingleConversationWhenTargetUserProvided() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 8001L;
        Long messageId = 9003L;
        Date now = new Date();

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setDeletedFlag(0);
        targetUser.setStatus(1);
        targetUser.setUsername("zhangsan");
        targetUser.setNickname("张三");

        SysUser currentUser = new SysUser();
        currentUser.setId(currentUserId);
        currentUser.setUsername("admin");
        currentUser.setNickname("管理员");

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setSinglePairKey("1:2");
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setIsAllSite(0);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember mappedSelfMember = new ChatConversationMember();
        mappedSelfMember.setConversationId(conversationId);
        mappedSelfMember.setUserId(currentUserId);

        ChatConversationMember mappedTargetMember = new ChatConversationMember();
        mappedTargetMember.setConversationId(conversationId);
        mappedTargetMember.setUserId(targetUserId);

        ChatMessage mappedMessage = new ChatMessage();
        mappedMessage.setContent("hello");

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(messageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(currentUserId);
        historyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        historyItem.setContent("hello");
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        historyItem.setCreatedAt(now);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);
        messageVO.setConversationId(conversationId);

        LambdaQueryChainWrapper<ChatConversationMember> firstSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> firstTargetQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> senderMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> firstSelfCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> firstTargetCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> senderCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> recipientUnreadCursorQuery = mock(LambdaQueryChainWrapper.class);

        when(sysUserService.getById(targetUserId)).thenReturn(targetUser);
        when(chatConversationService.lambdaQuery()).thenReturn(conversationQuery);
        when(conversationQuery.eq(anySFunction(), any())).thenReturn(conversationQuery);
        when(conversationQuery.last(any())).thenReturn(conversationQuery);
        when(conversationQuery.one()).thenReturn(null);
        when(chatConversationService.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(conversationId);
            saved.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
            saved.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            saved.setSinglePairKey("1:2");
            return true;
        });

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(
                firstSelfQuery,
                firstTargetQuery,
                secondSelfQuery,
                secondActiveMembersQuery,
                senderMemberQuery
        );
        mockMemberFindQuery(firstSelfQuery, null);
        mockMemberFindQuery(firstTargetQuery, null);
        mockMemberFindQuery(secondSelfQuery, selfMember);
        when(secondActiveMembersQuery.eq(anySFunction(), any())).thenReturn(secondActiveMembersQuery);
        when(secondActiveMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(senderMemberQuery, selfMember);

        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedSelfMember);
        when(chatModelMapper.toConversationMember(conversationId, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedTargetMember);
        when(chatConversationMemberService.save(any(ChatConversationMember.class))).thenReturn(true);
        when(chatConversationMemberService.updateById(any(ChatConversationMember.class))).thenReturn(true);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(
                firstSelfCursorQuery,
                firstTargetCursorQuery,
                senderCursorQuery,
                recipientUnreadCursorQuery
        );
        mockCursorFindQuery(firstSelfCursorQuery, null);
        mockCursorFindQuery(firstTargetCursorQuery, null);
        mockCursorFindQuery(senderCursorQuery, null);
        mockCursorFindQuery(recipientUnreadCursorQuery, null);
        when(chatMessageReadCursorService.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorService.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageService.save(mappedMessage)).thenAnswer(invocation -> {
            mappedMessage.setId(messageId);
            mappedMessage.setCreatedAt(now);
            return true;
        });
        when(chatConversationService.updateById(any(ChatConversation.class))).thenReturn(true);
        when(chatMessageRecipientService.saveBatch(anyCollection())).thenReturn(true);
        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of());
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(currentUser));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setTargetUserId(targetUserId);
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        assertEquals(conversationId, mappedMessage.getConversationId());
        verify(chatConversationService).save(any(ChatConversation.class));
        verify(chatConversationMemberService).save(mappedSelfMember);
        verify(chatConversationMemberService).save(mappedTargetMember);
    }

    @Test
    void sendTextMessageShouldReturnExistingMessageForSameClientMessageId() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 9001L;
        Long messageId = 9901L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setId(messageId);
        existingMessage.setConversationId(conversationId);
        existingMessage.setSenderId(currentUserId);
        existingMessage.setClientMessageId("c-1");

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(messageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(currentUserId);
        historyItem.setClientMessageId("c-1");
        historyItem.setContent("hello");
        historyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser currentUser = new SysUser();
        currentUser.setId(currentUserId);
        currentUser.setUsername("admin");
        currentUser.setNickname("管理员");

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessage> existingMessageQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery, secondSelfQuery, secondActiveMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(secondSelfQuery, selfMember);
        when(secondActiveMembersQuery.eq(anySFunction(), any())).thenReturn(secondActiveMembersQuery);
        when(secondActiveMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        when(chatMessageService.lambdaQuery()).thenReturn(existingMessageQuery);
        when(existingMessageQuery.eq(anySFunction(), any())).thenReturn(existingMessageQuery);
        when(existingMessageQuery.last(any())).thenReturn(existingMessageQuery);
        when(existingMessageQuery.one()).thenReturn(existingMessage);
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(currentUser));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setClientMessageId("c-1");
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        verify(chatMessageService, never()).save(any(ChatMessage.class));
        verify(chatMessageRecipientService, never()).saveBatch(anyCollection());
        verify(chatPushService, never()).pushMessageCreated(any(ChatMessageVO.class), anyCollection());
    }

    @Test
    void markReadShouldReturnExistingStateWhenReadCursorAlreadyReachedMessage() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 9101L;
        Long readMessageId = 9902L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(readMessageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(senderUserId);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(41L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setReadMessageId(readMessageId);
        cursor.setDeliveredMessageId(readMessageId);
        cursor.setUnreadCount(0);

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatModelMapper.toReadStateVO(cursor)).thenAnswer(invocation -> {
            ChatMessageReadCursor source = invocation.getArgument(0);
            ChatReadStateVO stateVO = new ChatReadStateVO();
            stateVO.setConversationId(source.getConversationId());
            stateVO.setReadMessageId(source.getReadMessageId());
            stateVO.setDeliveredMessageId(source.getDeliveredMessageId());
            stateVO.setUnreadCount(source.getUnreadCount());
            return stateVO;
        });

        ChatReadStateVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.markRead(currentUserId, conversationId, readMessageId);
        }

        assertEquals(readMessageId, result.getReadMessageId());
        assertEquals(Integer.valueOf(0), result.getUnreadCount());
        verify(chatMessageRecipientService, never()).lambdaUpdate();
        verify(chatMessageReadCursorService, never()).updateById(any(ChatMessageReadCursor.class));
        verify(chatPushService).pushReadUpdated(result, List.of(currentUserId, senderUserId));
    }

    @Test
    void pageMyMessagesShouldMarkPendingMessagesDeliveredAndNormalizePage() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 9201L;
        Long messageId = 9903L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem item = new ChatMessageHistoryItem();
        item.setId(messageId);
        item.setConversationId(conversationId);
        item.setSenderId(senderUserId);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("hello");
        item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_PENDING);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(51L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setUnreadCount(2);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(senderUserId);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> deliveredMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> deliveredUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery, deliveredMemberQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));
        mockMemberFindQuery(deliveredMemberQuery, selfMember);
        when(chatMessageMapper.countMessagePage(conversationId, currentUserId, null)).thenReturn(1L);
        when(chatMessageMapper.selectMessagePage(conversationId, currentUserId, null, 0L, 100L)).thenReturn(List.of(item));
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(deliveredUpdate);
        mockRecipientDeliveredHistoryUpdate(deliveredUpdate);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatMessageReadCursorService.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberService.updateById(selfMember)).thenReturn(true);
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();
        query.setCurrent(0L);
        query.setSize(999L);

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertEquals(messageId, result.getRecords().get(0).getId());
        assertEquals(ChatConstants.DELIVERY_STATUS_DELIVERED, item.getDeliveryStatus());
        assertEquals(messageId, cursor.getDeliveredMessageId());
        assertEquals(messageId, selfMember.getLastDeliveredMessageId());
        verify(chatMessageRecipientService).lambdaUpdate();
        verify(chatConversationMemberService).updateById(selfMember);
    }

    @Test
    void pageMyConversationsShouldCreateGlobalConversationMembershipWhenMissing() {
        Long currentUserId = 1L;
        Long conversationId = 9301L;

        ChatConversationMember mappedMember = new ChatConversationMember();
        mappedMember.setConversationId(conversationId);
        mappedMember.setUserId(currentUserId);

        LambdaQueryChainWrapper<ChatConversation> globalConversationQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> memberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.lambdaQuery()).thenReturn(globalConversationQuery);
        when(globalConversationQuery.eq(anySFunction(), any())).thenReturn(globalConversationQuery);
        when(globalConversationQuery.last(any())).thenReturn(globalConversationQuery);
        when(globalConversationQuery.one()).thenReturn(null);
        when(chatConversationService.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(conversationId);
            return true;
        });

        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberQuery);
        mockMemberFindQuery(memberQuery, null);
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_SYSTEM, null, null)).thenReturn(mappedMember);
        when(chatConversationMemberService.save(mappedMember)).thenReturn(true);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, null);
        when(chatMessageReadCursorService.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(61L);
            return true;
        });

        ChatConversationPageQuery query = new ChatConversationPageQuery();
        query.setKeyword("  system  ");
        when(chatConversationMapper.countConversationPage(currentUserId, "system")).thenReturn(0L);

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(query);
        }

        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(20L, result.getSize());
        verify(chatConversationService).save(any(ChatConversation.class));
        verify(chatConversationMemberService).save(mappedMember);
        verify(chatMessageReadCursorService).save(any(ChatMessageReadCursor.class));
    }

    @Test
    void pageMyConversationsShouldRecoverGlobalConversationAfterDuplicateCreate() {
        Long currentUserId = 1L;
        Long conversationId = 9302L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        conversation.setIsAllSite(1);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember mappedMember = new ChatConversationMember();
        mappedMember.setConversationId(conversationId);
        mappedMember.setUserId(currentUserId);

        LambdaQueryChainWrapper<ChatConversation> firstGlobalConversationQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversation> duplicatedGlobalConversationQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> memberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.lambdaQuery()).thenReturn(firstGlobalConversationQuery, duplicatedGlobalConversationQuery);
        when(firstGlobalConversationQuery.eq(anySFunction(), any())).thenReturn(firstGlobalConversationQuery);
        when(firstGlobalConversationQuery.last(any())).thenReturn(firstGlobalConversationQuery);
        when(firstGlobalConversationQuery.one()).thenReturn(null);
        when(duplicatedGlobalConversationQuery.eq(anySFunction(), any())).thenReturn(duplicatedGlobalConversationQuery);
        when(duplicatedGlobalConversationQuery.last(any())).thenReturn(duplicatedGlobalConversationQuery);
        when(duplicatedGlobalConversationQuery.one()).thenReturn(conversation);
        when(chatConversationService.save(any(ChatConversation.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate global conversation"));

        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberQuery);
        mockMemberFindQuery(memberQuery, null);
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_SYSTEM, null, null)).thenReturn(mappedMember);
        when(chatConversationMemberService.save(mappedMember)).thenReturn(true);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, null);
        when(chatMessageReadCursorService.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(62L);
            return true;
        });

        when(chatConversationMapper.countConversationPage(currentUserId, null)).thenReturn(0L);

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(new ChatConversationPageQuery());
        }

        assertEquals(0L, result.getTotal());
        verify(chatConversationService).save(any(ChatConversation.class));
        verify(chatConversationMemberService).save(mappedMember);
        verify(chatMessageReadCursorService).save(any(ChatMessageReadCursor.class));
    }

    @Test
    void pageMyConversationsShouldRestoreInactiveGlobalMembershipAndResetCursor() {
        Long currentUserId = 1L;
        Long conversationId = 9303L;
        Long lastMessageId = 8001L;
        Date lastMessageTime = new Date();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        conversation.setIsAllSite(1);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setLastMessageId(lastMessageId);
        conversation.setLastMessageTime(lastMessageTime);

        ChatConversationMember inactiveMember = new ChatConversationMember();
        inactiveMember.setId(71L);
        inactiveMember.setConversationId(conversationId);
        inactiveMember.setUserId(currentUserId);
        inactiveMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        inactiveMember.setJoinSource(ChatConstants.JOIN_SOURCE_MANUAL);
        inactiveMember.setLastReadMessageId(1L);
        inactiveMember.setLastDeliveredMessageId(1L);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(72L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setUnreadCount(5);
        cursor.setReadMessageId(1L);
        cursor.setDeliveredMessageId(1L);

        LambdaQueryChainWrapper<ChatConversation> globalConversationQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> memberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> cursorQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.lambdaQuery()).thenReturn(globalConversationQuery);
        when(globalConversationQuery.eq(anySFunction(), any())).thenReturn(globalConversationQuery);
        when(globalConversationQuery.last(any())).thenReturn(globalConversationQuery);
        when(globalConversationQuery.one()).thenReturn(conversation);

        when(chatConversationMemberService.lambdaQuery()).thenReturn(memberQuery);
        mockMemberFindQuery(memberQuery, inactiveMember);
        when(chatConversationMemberService.updateById(inactiveMember)).thenReturn(true);

        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(cursorQuery);
        mockCursorFindQuery(cursorQuery, cursor);
        when(chatMessageReadCursorService.updateById(cursor)).thenReturn(true);

        when(chatConversationMapper.countConversationPage(currentUserId, null)).thenReturn(0L);

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(new ChatConversationPageQuery());
        }

        assertEquals(0L, result.getTotal());
        assertEquals(ChatConstants.MEMBER_STATUS_NORMAL, inactiveMember.getStatus());
        assertEquals(ChatConstants.JOIN_SOURCE_SYSTEM, inactiveMember.getJoinSource());
        assertEquals(lastMessageId, inactiveMember.getLastReadMessageId());
        assertEquals(lastMessageId, inactiveMember.getLastDeliveredMessageId());
        assertEquals(lastMessageId, cursor.getReadMessageId());
        assertEquals(lastMessageId, cursor.getDeliveredMessageId());
        assertEquals(Integer.valueOf(0), cursor.getUnreadCount());
        verify(chatConversationMemberService).updateById(inactiveMember);
        verify(chatMessageReadCursorService).updateById(cursor);
        verify(chatConversationMemberService, never()).save(any(ChatConversationMember.class));
    }

    @Test
    void pageMyMessagesShouldSkipDeliveredUpdateWhenMessagesAlreadyDelivered() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 9302L;
        Long deliveredMessageId = 9904L;
        Long selfMessageId = 9905L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageHistoryItem deliveredItem = new ChatMessageHistoryItem();
        deliveredItem.setId(deliveredMessageId);
        deliveredItem.setConversationId(conversationId);
        deliveredItem.setSenderId(senderUserId);
        deliveredItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_DELIVERED);

        ChatMessageHistoryItem selfItem = new ChatMessageHistoryItem();
        selfItem.setId(selfMessageId);
        selfItem.setConversationId(conversationId);
        selfItem.setSenderId(currentUserId);
        selfItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_PENDING);

        ChatMessageVO deliveredVO = new ChatMessageVO();
        deliveredVO.setId(deliveredMessageId);
        ChatMessageVO selfVO = new ChatMessageVO();
        selfVO.setId(selfMessageId);

        SysUser sender = new SysUser();
        sender.setId(senderUserId);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery);
        mockMemberFindQuery(selfQuery, selfMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageMapper.countMessagePage(conversationId, currentUserId, null)).thenReturn(2L);
        when(chatMessageMapper.selectMessagePage(conversationId, currentUserId, null, 0L, 20L))
                .thenReturn(List.of(deliveredItem, selfItem));
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(deliveredItem)).thenReturn(deliveredVO);
        when(chatModelMapper.toMessageVO(selfItem)).thenReturn(selfVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(selfMessageId, deliveredMessageId), result.getRecords().stream().map(ChatMessageVO::getId).toList());
        verify(chatMessageRecipientService, never()).lambdaUpdate();
        verify(chatMessageReadCursorService, never()).lambdaQuery();
        verify(chatConversationMemberService, never()).updateById(selfMember);
    }

    @Test
    void editMessageShouldRejectEditingFileMessage() {
        Long currentUserId = 1L;
        Long messageId = 9801L;

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setMessageId(messageId);
        recipient.setRecipientUserId(currentUserId);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(1L);
        message.setSenderId(currentUserId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_FILE);
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        LambdaQueryChainWrapper<ChatMessageRecipient> recipientQuery = mock(LambdaQueryChainWrapper.class);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(recipientQuery);
        mockRecipientFindQuery(recipientQuery, recipient);
        when(chatMessageService.getById(messageId)).thenReturn(message);

        ChatEditMessageRequest request = new ChatEditMessageRequest();
        request.setContent("new content");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.editMessage(messageId, request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("只有文本消息支持编辑", exception.getMessage());
        verify(chatMessageService, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void revokeMessageShouldReleaseChatFileReference() {
        Long currentUserId = 1L;
        Long messageId = 9802L;
        Long fileId = 7001L;

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setMessageId(messageId);
        recipient.setRecipientUserId(currentUserId);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setSenderId(currentUserId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_FILE);
        message.setPayloadJson("{\"fileId\":7001}");
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        FileBusinessInfo fileReference = new FileBusinessInfo();
        fileReference.setId(501L);
        fileReference.setFileId(fileId);
        fileReference.setReferenceType(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE);
        fileReference.setReferenceId(messageId);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(1L);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        LambdaQueryChainWrapper<ChatMessageRecipient> recipientQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<FileBusinessInfo> fileReferenceQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        when(chatMessageRecipientService.lambdaQuery()).thenReturn(recipientQuery);
        mockRecipientFindQuery(recipientQuery, recipient);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(chatMessageService.updateById(message)).thenReturn(true);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(fileReferenceQuery);
        when(fileReferenceQuery.eq(anySFunction(), any())).thenReturn(fileReferenceQuery);
        when(fileReferenceQuery.list()).thenReturn(List.of(fileReference));
        when(fileBusinessInfoService.removeByIds(List.of(501L))).thenReturn(true);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(activeMembersQuery);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember));
        when(chatMessageMapper.selectVisibleMessageById(any(Long.class), eq(currentUserId), eq(messageId))).thenReturn(new ChatMessageHistoryItem());
        when(chatModelMapper.toMessageVO(any(ChatMessageHistoryItem.class))).thenReturn(new ChatMessageVO());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.revokeMessage(messageId);
        }

        assertEquals(ChatConstants.REVOKE_STATUS_REVOKED, message.getRevokeStatus());
        assertEquals(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER, message.getContent());
        assertEquals(null, message.getPayloadJson());
        verify(fileBusinessInfoService).removeByIds(List.of(501L));
        verify(fileLifecycleService).syncFileAfterReferenceRemoval(fileId);
    }

    @Test
    void sendFileMessageShouldRejectForeignBusinessReference() {
        Long currentUserId = 1L;
        Long conversationId = 1001L;
        Long businessId = 801L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        FileBusinessInfo fileReference = new FileBusinessInfo();
        fileReference.setId(businessId);
        fileReference.setUserId(2L);
        fileReference.setReferenceType("temp");
        fileReference.setFileId(9001L);

        LambdaQueryChainWrapper<ChatConversationMember> firstSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> firstActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondActiveMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(
                firstSelfQuery,
                firstActiveMembersQuery,
                secondSelfQuery,
                secondActiveMembersQuery
        );
        mockMemberFindQuery(firstSelfQuery, selfMember);
        when(firstActiveMembersQuery.eq(anySFunction(), any())).thenReturn(firstActiveMembersQuery);
        when(firstActiveMembersQuery.list()).thenReturn(List.of(selfMember));
        mockMemberFindQuery(secondSelfQuery, selfMember);
        when(secondActiveMembersQuery.eq(anySFunction(), any())).thenReturn(secondActiveMembersQuery);
        when(secondActiveMembersQuery.list()).thenReturn(List.of(selfMember));
        when(fileBusinessInfoService.getById(businessId)).thenReturn(fileReference);

        ChatSendFileRequest request = new ChatSendFileRequest();
        request.setConversationId(conversationId);
        request.setBusinessId(businessId);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendFileMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("不能发送他人的文件", exception.getMessage());
    }

    @Test
    void editMessageShouldPushMessageUpdatedEvent() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 9901L;
        Long messageId = 8801L;
        Date now = new Date();

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setMessageId(messageId);
        recipient.setRecipientUserId(currentUserId);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        message.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        ChatMessageHistoryItem item = new ChatMessageHistoryItem();
        item.setId(messageId);
        item.setConversationId(conversationId);
        item.setSenderId(currentUserId);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("edited");
        item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        item.setCreatedAt(now);
        item.setUpdatedAt(new Date(now.getTime() + 1000L));

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(currentUserId);
        sender.setUsername("admin");
        sender.setNickname("管理员");

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        LambdaQueryChainWrapper<ChatMessageRecipient> recipientQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatMessageRecipientService.lambdaQuery()).thenReturn(recipientQuery);
        mockRecipientFindQuery(recipientQuery, recipient);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(chatMessageService.updateById(message)).thenReturn(true);
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(item);
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatConversationMemberService.lambdaQuery()).thenReturn(activeMembersQuery);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));

        ChatEditMessageRequest request = new ChatEditMessageRequest();
        request.setContent("edited");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.editMessage(messageId, request);
        }

        verify(chatPushService).pushMessageUpdated(messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void sendFileMessageShouldClassifyImageAndPersistReplyMessageId() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 9902L;
        Long messageId = 8802L;
        Long replyMessageId = 8701L;
        Long businessId = 801L;
        Date now = new Date();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        FileBusinessInfo tempReference = new FileBusinessInfo();
        tempReference.setId(businessId);
        tempReference.setUserId(currentUserId);
        tempReference.setReferenceType("temp");
        tempReference.setFileId(7001L);
        tempReference.setIsPublic(0);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7001L);
        fileInfo.setMimeType("image/png");
        fileInfo.setOriginalName("demo.png");
        fileInfo.setFileName("demo.png");
        fileInfo.setFileUrl("https://example.com/demo.png");
        fileInfo.setFileSize(123L);
        fileInfo.setFileType("png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());

        ChatMessageHistoryItem replyItem = new ChatMessageHistoryItem();
        replyItem.setId(replyMessageId);
        replyItem.setConversationId(conversationId);
        replyItem.setSenderId(targetUserId);
        replyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        replyItem.setContent("origin");

        ChatMessageHistoryItem createdItem = new ChatMessageHistoryItem();
        createdItem.setId(messageId);
        createdItem.setConversationId(conversationId);
        createdItem.setSenderId(currentUserId);
        createdItem.setMessageType(ChatConstants.MESSAGE_TYPE_IMAGE);
        createdItem.setContent("[图片] demo.png");
        createdItem.setReplyMessageId(replyMessageId);
        createdItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        createdItem.setCreatedAt(now);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(currentUserId);
        sender.setUsername("admin");
        sender.setNickname("管理员");

        ChatMessageReadCursor senderCursor = new ChatMessageReadCursor();
        senderCursor.setId(101L);
        senderCursor.setConversationId(conversationId);
        senderCursor.setUserId(currentUserId);
        senderCursor.setUnreadCount(0);

        ChatMessageReadCursor targetCursor = new ChatMessageReadCursor();
        targetCursor.setId(102L);
        targetCursor.setConversationId(conversationId);
        targetCursor.setUserId(targetUserId);
        targetCursor.setUnreadCount(0);

        LambdaQueryChainWrapper<ChatConversationMember> firstSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> firstActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondSelfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> secondActiveMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> senderMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> targetMemberQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> senderCursorQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatMessageReadCursor> targetCursorUnreadQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<FileBusinessInfo> existingChatRefQuery = mock(LambdaQueryChainWrapper.class);
        LambdaUpdateChainWrapper<ChatMessageRecipient> deliveredUpdate = mock(LambdaUpdateChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(
                firstSelfQuery,
                firstActiveMembersQuery,
                secondSelfQuery,
                secondActiveMembersQuery,
                senderMemberQuery,
                targetMemberQuery
        );
        mockMemberFindQuery(firstSelfQuery, selfMember);
        when(firstActiveMembersQuery.eq(anySFunction(), any())).thenReturn(firstActiveMembersQuery);
        when(firstActiveMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(secondSelfQuery, selfMember);
        when(secondActiveMembersQuery.eq(anySFunction(), any())).thenReturn(secondActiveMembersQuery);
        when(secondActiveMembersQuery.list()).thenReturn(List.of(selfMember, targetMember));
        mockMemberFindQuery(senderMemberQuery, selfMember);
        mockMemberFindQuery(targetMemberQuery, targetMember);

        when(fileBusinessInfoService.getById(businessId)).thenReturn(tempReference);
        when(fileInfoService.getById(7001L)).thenReturn(fileInfo);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(existingChatRefQuery);
        when(existingChatRefQuery.eq(anySFunction(), any())).thenReturn(existingChatRefQuery);
        when(existingChatRefQuery.orderByDesc(anySFunction())).thenReturn(existingChatRefQuery);
        when(existingChatRefQuery.last(any())).thenReturn(existingChatRefQuery);
        when(existingChatRefQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo reference = invocation.getArgument(0);
            reference.setId(901L);
            return true;
        });
        when(fileBusinessInfoService.removeById(businessId)).thenReturn(true);

        when(chatMessageService.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            saved.setCreatedAt(now);
            return true;
        });
        when(chatMessageService.updateById(any(ChatMessage.class))).thenReturn(true);
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientService.saveBatch(any())).thenReturn(true);
        when(chatMessageRecipientService.lambdaUpdate()).thenReturn(deliveredUpdate);
        mockRecipientUpdateQuery(deliveredUpdate);
        when(chatMessageReadCursorService.lambdaQuery()).thenReturn(senderCursorQuery, targetCursorUnreadQuery);
        mockCursorFindQuery(senderCursorQuery, senderCursor);
        mockCursorFindQuery(targetCursorUnreadQuery, targetCursor);
        when(chatMessageReadCursorService.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);
        when(chatConversationMemberService.updateById(any(ChatConversationMember.class))).thenReturn(true);
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, replyMessageId)).thenReturn(replyItem);
        when(chatMessageMapper.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(createdItem);
        when(chatModelMapper.toMessageVO(createdItem)).thenReturn(messageVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of(sender));
        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of());

        ChatSendFileRequest request = new ChatSendFileRequest();
        request.setConversationId(conversationId);
        request.setBusinessId(businessId);
        request.setReplyMessageId(replyMessageId);

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendFileMessage(request);
        }

        assertEquals(messageId, result.getId());
        verify(chatMessageService).save(argThat((ChatMessage saved) ->
                saved != null
                        && ChatConstants.MESSAGE_TYPE_IMAGE.equals(saved.getMessageType())
                        && "[图片] demo.png".equals(saved.getContent())
                        && Objects.equals(replyMessageId, saved.getReplyMessageId())
        ));
        verify(chatPushService).pushMessageCreated(messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void updateGroupNoticeShouldPushConversationUpdatedEvent() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 9903L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setRemark("old");

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(memberUserId);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(conversationId);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        detailItem.setNotice("new notice");

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setNotice("new notice");

        LambdaQueryChainWrapper<ChatConversationMember> selfQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> activeMembersQuery = mock(LambdaQueryChainWrapper.class);
        LambdaQueryChainWrapper<ChatConversationMember> detailMembersQuery = mock(LambdaQueryChainWrapper.class);

        when(chatConversationService.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberService.lambdaQuery()).thenReturn(selfQuery, activeMembersQuery, detailMembersQuery);
        mockMemberFindQuery(selfQuery, ownerMember);
        when(activeMembersQuery.eq(anySFunction(), any())).thenReturn(activeMembersQuery);
        when(activeMembersQuery.list()).thenReturn(List.of(ownerMember, member));
        when(detailMembersQuery.eq(anySFunction(), any())).thenReturn(detailMembersQuery);
        when(detailMembersQuery.list()).thenReturn(List.of(ownerMember, member));
        when(chatConversationService.updateById(conversation)).thenReturn(true);
        when(chatConversationMapper.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserService.listByIds(any())).thenReturn(List.of());

        var request = new com.cybzacg.blogbackend.module.chat.model.user.ChatGroupNoticeUpdateRequest();
        request.setNotice("new notice");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.updateGroupNotice(conversationId, request);
        }

        verify(chatPushService).pushConversationUpdated(any(), eq(List.of(currentUserId, memberUserId)));
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }

    private static void mockMemberFindQuery(LambdaQueryChainWrapper<ChatConversationMember> query,
                                            ChatConversationMember result) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.orderByDesc(anySFunction())).thenReturn(query);
        when(query.last(any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private static void mockCursorFindQuery(LambdaQueryChainWrapper<ChatMessageReadCursor> query,
                                            ChatMessageReadCursor result) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.orderByDesc(anySFunction())).thenReturn(query);
        when(query.last(any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private static void mockRecipientFindQuery(LambdaQueryChainWrapper<ChatMessageRecipient> query,
                                               ChatMessageRecipient result) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.orderByDesc(anySFunction())).thenReturn(query);
        when(query.last(any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private static void mockRecipientUpdateQuery(LambdaUpdateChainWrapper<ChatMessageRecipient> query) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.set(anySFunction(), any())).thenReturn(query);
        when(query.update()).thenReturn(true);
    }

    private static void mockRecipientReadUpdateQuery(LambdaUpdateChainWrapper<ChatMessageRecipient> query) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.le(anySFunction(), any())).thenReturn(query);
        when(query.set(anySFunction(), any())).thenReturn(query);
        when(query.update()).thenReturn(true);
    }

    private static void mockRecipientDeliveredHistoryUpdate(LambdaUpdateChainWrapper<ChatMessageRecipient> query) {
        when(query.eq(anySFunction(), any())).thenReturn(query);
        when(query.in(anySFunction(), anyCollection())).thenReturn(query);
        when(query.lt(anySFunction(), any())).thenReturn(query);
        when(query.set(anySFunction(), any())).thenReturn(query);
        when(query.update()).thenReturn(true);
    }
}

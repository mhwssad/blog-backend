package com.cybzacg.blogbackend.module.chat;

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
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;
import com.cybzacg.blogbackend.module.chat.model.user.ChatEditMessageRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationLastMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatCreateGroupRequest;
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
import com.cybzacg.blogbackend.module.chat.service.impl.UserChatServiceImpl;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private ChatConversationMemberRepository chatConversationMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatMessageRecipientRepository chatMessageRecipientRepository;
    @Mock
    private ChatMessageReadCursorRepository chatMessageReadCursorRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ChatModelMapper chatModelMapper;
    @Mock
    private ChatPushService chatPushService;
    @Mock
    private ChatWebSocketSessionRegistry chatWebSocketSessionRegistry;
    @Mock
    private FileBusinessInfoRepository fileBusinessInfoRepository;
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileLifecycleService fileLifecycleService;
    @Mock
    private ChatAttachmentAsyncProcessingService chatAttachmentAsyncProcessingService;
    @Mock
    private ChatMessageGovernanceService chatMessageGovernanceService;
    @Mock
    private ChatMetricsService chatMetricsService;

    private UserChatServiceImpl userChatService;

    @BeforeEach
    void setUp() {
        userChatService = new UserChatServiceImpl(
                chatConversationRepository,
                chatConversationMemberRepository,
                chatMessageRepository,
                chatMessageRecipientRepository,
                chatMessageReadCursorRepository,
                sysUserRepository,
                chatModelMapper,
                chatPushService,
                chatWebSocketSessionRegistry,
                fileBusinessInfoRepository,
                fileInfoRepository,
                fileLifecycleService,
                chatAttachmentAsyncProcessingService,
                chatMessageGovernanceService,
                chatMetricsService
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

        when(sysUserRepository.getById(targetUserId)).thenReturn(targetUser);
        when(chatConversationRepository.findBySinglePairKey("1:2")).thenReturn(conversation);

        when(chatConversationMemberRepository.findByConversationAndUser(1001L, currentUserId)).thenReturn(null);
        when(chatConversationMemberRepository.findByConversationAndUser(1001L, targetUserId)).thenReturn(null);
        when(chatConversationMemberRepository.listActiveByConversationId(1001L)).thenReturn(List.of(selfMember, targetMember));

        when(chatModelMapper.toConversationMember(1001L, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedSelfMember);
        when(chatModelMapper.toConversationMember(1001L, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedTargetMember);

        when(chatMessageReadCursorRepository.findByConversationAndUser(1001L, currentUserId)).thenReturn(null);
        when(chatMessageReadCursorRepository.findByConversationAndUser(1001L, targetUserId)).thenReturn(null);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatConversationRepository.selectConversationDetail(1001L, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(currentUser, targetUser));
        when(chatConversationMemberRepository.save(any(ChatConversationMember.class))).thenReturn(true);

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
        verify(chatConversationRepository, never()).save(any(ChatConversation.class));
        verify(chatConversationMemberRepository).save(mappedSelfMember);
        verify(chatConversationMemberRepository).save(mappedTargetMember);
    }

    @Test
    void openSingleConversationShouldRejectDisabledTargetUser() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setDeletedFlag(0);
        targetUser.setStatus(0);

        when(sysUserRepository.getById(targetUserId)).thenReturn(targetUser);

        ChatOpenSingleConversationRequest request = new ChatOpenSingleConversationRequest();
        request.setTargetUserId(targetUserId);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.openSingleConversation(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("目标用户不可用", exception.getMessage());
        verify(chatConversationRepository, never()).findBySinglePairKey(any());
    }

    @Test
    void openSingleConversationShouldRejectSelfTarget() {
        Long currentUserId = 1L;

        SysUser selfUser = new SysUser();
        selfUser.setId(currentUserId);
        selfUser.setDeletedFlag(0);
        selfUser.setStatus(1);

        when(sysUserRepository.getById(currentUserId)).thenReturn(selfUser);

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
    void getMyConversationShouldAssembleSingleConversationDetail() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long globalConversationId = 9001L;
        Long conversationId = 9101L;

        ChatConversation globalConversation = new ChatConversation();
        globalConversation.setId(globalConversationId);
        globalConversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        globalConversation.setIsAllSite(1);
        globalConversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember globalMember = new ChatConversationMember();
        globalMember.setId(101L);
        globalMember.setConversationId(globalConversationId);
        globalMember.setUserId(currentUserId);
        globalMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        globalMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMessageReadCursor globalCursor = new ChatMessageReadCursor();
        globalCursor.setId(102L);
        globalCursor.setConversationId(globalConversationId);
        globalCursor.setUserId(currentUserId);
        globalCursor.setUnreadCount(0);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setId(61L);
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(conversationId);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        detailItem.setUnreadCount(3);
        detailItem.setLastMessageId(9201L);
        detailItem.setLastMessageSenderId(targetUserId);
        detailItem.setLastMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        detailItem.setLastMessageContent("hello");

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        ChatConversationLastMessageVO lastMessageVO = new ChatConversationLastMessageVO();
        lastMessageVO.setId(9201L);
        lastMessageVO.setContent("hello");

        SysUser currentUser = new SysUser();
        currentUser.setId(currentUserId);
        currentUser.setUsername("admin");
        currentUser.setNickname("管理员");

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setUsername("zhangsan");
        targetUser.setNickname("张三");
        targetUser.setAvatar("https://example.com/u2.png");

        when(chatConversationRepository.findGlobalConversation()).thenReturn(globalConversation);
        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        when(chatConversationMemberRepository.findByConversationAndUser(globalConversationId, currentUserId)).thenReturn(globalMember);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember), List.of(selfMember, targetMember));
        when(chatConversationMemberRepository.updateById(globalMember)).thenReturn(true);

        when(chatMessageReadCursorRepository.findByConversationAndUser(globalConversationId, currentUserId)).thenReturn(globalCursor);
        when(chatMessageReadCursorRepository.updateById(globalCursor)).thenReturn(true);

        when(chatConversationRepository.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(chatModelMapper.toConversationLastMessageVO(detailItem)).thenReturn(lastMessageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(currentUser, targetUser));

        ChatConversationVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.getMyConversation(conversationId);
        }

        assertEquals(conversationId, result.getId());
        assertEquals(2L, result.getMemberCount());
        assertEquals(Integer.valueOf(3), result.getUnreadCount());
        assertEquals(targetUserId, result.getTargetUserId());
        assertEquals("zhangsan", result.getTargetUsername());
        assertEquals("张三", result.getTargetNickname());
        assertEquals("张三", result.getName());
        assertEquals("https://example.com/u2.png", result.getAvatar());
        assertNotNull(result.getLastMessage());
        assertEquals("张三", result.getLastMessage().getSenderNickname());
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
        ownerMember.setJoinedAt(LocalDateTime.now());

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(ownerMember));

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.leaveGroup(conversationId));
        }

        assertEquals(ResultErrorCode.UNSUPPORTED_OPERATION.getCode(), exception.getCode());
        assertEquals("群主不能直接退群，请先解散群聊", exception.getMessage());
        verify(chatConversationMemberRepository, never()).updateById(any(ChatConversationMember.class));
    }

    @Test
    void leaveGroupShouldRejectGlobalConversation() {
        Long currentUserId = 1L;
        Long conversationId = 2002L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        conversation.setIsAllSite(1);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setUnreadCount(0);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationRepository.findGlobalConversation()).thenReturn(conversation);

        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember));

        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.leaveGroup(conversationId));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前会话不是群聊", exception.getMessage());
        verify(chatPushService, never()).pushMembersUpdated(any(), any());
    }

    @Test
    void sendTextMessageShouldPersistRecipientsAndPushMessage() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 3001L;
        Long messageId = 9001L;
        LocalDateTime now = LocalDateTime.now();

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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember));

        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageRepository.save(mappedMessage)).thenAnswer(invocation -> {
            mappedMessage.setId(messageId);
            mappedMessage.setCreatedAt(now);
            return true;
        });
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientRepository.saveBatch(anyCollection())).thenReturn(true);
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of(mock(WebSocketSession.class)));
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatConversationMemberRepository.updateById(any(ChatConversationMember.class))).thenReturn(true);

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

        verify(chatMessageRecipientRepository).saveBatch(anyCollection());
        verify(chatPushService).pushMessageCreated(messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void sendTextMessageShouldPersistReplySnapshot() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 3002L;
        Long messageId = 9002L;
        Long replyMessageId = 8801L;
        LocalDateTime now = LocalDateTime.now();

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
        mappedMessage.setContent("reply text");

        ChatMessageHistoryItem replyItem = new ChatMessageHistoryItem();
        replyItem.setId(replyMessageId);
        replyItem.setConversationId(conversationId);
        replyItem.setSenderId(targetUserId);
        replyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        replyItem.setContent("origin text");
        replyItem.setReplyMessageId(7701L);
        replyItem.setCreatedAt(now.minusSeconds(3));

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(messageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(currentUserId);
        historyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        historyItem.setContent("reply text");
        historyItem.setReplyMessageId(replyMessageId);
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        historyItem.setCreatedAt(now);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(currentUserId);
        sender.setUsername("admin");
        sender.setNickname("管理员");

        SysUser replySender = new SysUser();
        replySender.setId(targetUserId);
        replySender.setUsername("zhangsan");
        replySender.setNickname("张三");

        ChatMessageReadCursor senderCursor = new ChatMessageReadCursor();
        senderCursor.setId(21L);
        senderCursor.setConversationId(conversationId);
        senderCursor.setUserId(currentUserId);
        senderCursor.setUnreadCount(0);

        ChatMessageReadCursor targetCursor = new ChatMessageReadCursor();
        targetCursor.setId(22L);
        targetCursor.setConversationId(conversationId);
        targetCursor.setUserId(targetUserId);
        targetCursor.setUnreadCount(0);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember));

        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageRepository.save(mappedMessage)).thenAnswer(invocation -> {
            mappedMessage.setId(messageId);
            mappedMessage.setCreatedAt(now);
            historyItem.setPayloadJson(mappedMessage.getPayloadJson());
            return true;
        });
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientRepository.saveBatch(anyCollection())).thenReturn(true);
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);
        when(chatConversationMemberRepository.updateById(any(ChatConversationMember.class))).thenReturn(true);
        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of());
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, replyMessageId)).thenReturn(replyItem);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender, replySender));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("reply text");
        request.setReplyMessageId(replyMessageId);

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        ChatMessagePayloadVO payload = JsonUtils.fromJson(mappedMessage.getPayloadJson(), ChatMessagePayloadVO.class);
        assertNotNull(payload);
        assertNotNull(payload.getReply());
        assertEquals(replyMessageId, payload.getReply().getId());
        assertEquals("origin text", payload.getReply().getContent());
        assertEquals("张三", payload.getReply().getSenderNickname());
        assertEquals(7701L, payload.getReply().getReplyToMessageId());
        assertEquals(ChatConstants.REPLY_STATE_NORMAL, payload.getReply().getState());
        assertNotNull(result.getReply());
        assertEquals(replyMessageId, result.getReply().getId());
        assertEquals("origin text", result.getReply().getContent());
        assertEquals(ChatConstants.REPLY_STATE_NORMAL, result.getReply().getState());
    }

    @Test
    void markReadShouldUpdateCursorAndPushReadState() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 4001L;
        Long readMessageId = 9002L;
        LocalDateTime previousTime = LocalDateTime.now().minusSeconds(10);

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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));

        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageRecipientRepository.countUnread(eq(conversationId), eq(currentUserId))).thenReturn(0L);
        when(chatMessageReadCursorRepository.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberRepository.updateById(selfMember)).thenReturn(true);
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageRecipientRepository.countUnread(eq(conversationId), eq(currentUserId))).thenReturn(2L);
        when(chatMessageReadCursorRepository.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberRepository.updateById(selfMember)).thenReturn(true);
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
        when(chatMessageRecipientRepository.countUnread(eq(conversationId), eq(currentUserId))).thenReturn(3L);
        when(chatMessageReadCursorRepository.updateById(cursor)).thenReturn(true);
        when(chatConversationMemberRepository.updateById(selfMember)).thenReturn(true);
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

        when(sysUserRepository.getById(memberUserId)).thenReturn(memberUser);
        when(chatModelMapper.toGroupConversation(any(ChatCreateGroupRequest.class))).thenReturn(conversation);
        when(chatConversationRepository.save(conversation)).thenReturn(true);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(ownerMember, member));
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_OWNER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(ownerMember);
        when(chatModelMapper.toConversationMember(conversationId, memberUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(member);
        when(chatConversationMemberRepository.save(any(ChatConversationMember.class))).thenReturn(true);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatConversationRepository.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(ownerUser, memberUser));

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
        assertEquals(currentUserId, conversation.getOwnerId());
        assertEquals(ChatConstants.MEMBER_ROLE_OWNER, ownerMember.getMemberRole());
        verify(chatConversationRepository).save(conversation);
        verify(chatConversationMemberRepository).save(ownerMember);
        verify(chatConversationMemberRepository).save(member);
    }

    @Test
    void getGroupDetailShouldReturnConversationDetailForGroupMember() {
        Long currentUserId = 1L;
        Long conversationId = 5002L;
        Long memberUserId = 2L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(memberUserId);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        member.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(conversationId);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        detailItem.setName("学习群");
        detailItem.setNotice("欢迎加入");

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversationVO.setName("学习群");
        conversationVO.setNotice("欢迎加入");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, member), List.of(selfMember, member));
        when(chatConversationRepository.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());

        ChatConversationVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.getGroupDetail(conversationId);
        }

        assertEquals(conversationId, result.getId());
        assertEquals("学习群", result.getName());
        assertEquals("欢迎加入", result.getNotice());
        assertEquals(2L, result.getMemberCount());
    }

    @Test
    void listGroupMembersShouldReturnSortedMemberRecords() {
        Long currentUserId = 1L;
        Long adminUserId = 2L;
        Long memberUserId = 3L;
        Long conversationId = 5003L;
        LocalDateTime now = LocalDateTime.now();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);
        ownerMember.setJoinedAt(now.minusSeconds(3));

        ChatConversationMember adminMember = new ChatConversationMember();
        adminMember.setConversationId(conversationId);
        adminMember.setUserId(adminUserId);
        adminMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminMember.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);
        adminMember.setJoinedAt(now.minusSeconds(2));

        ChatConversationMember normalMember = new ChatConversationMember();
        normalMember.setConversationId(conversationId);
        normalMember.setUserId(memberUserId);
        normalMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        normalMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        normalMember.setJoinedAt(now.minusSeconds(1));

        SysUser ownerUser = new SysUser();
        ownerUser.setId(currentUserId);
        ownerUser.setUsername("owner");
        ownerUser.setNickname("群主");

        SysUser adminUser = new SysUser();
        adminUser.setId(adminUserId);
        adminUser.setUsername("admin");
        adminUser.setNickname("管理员");

        SysUser memberUser = new SysUser();
        memberUser.setId(memberUserId);
        memberUser.setUsername("member");
        memberUser.setNickname("成员");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, adminMember, normalMember), List.of(ownerMember, adminMember, normalMember));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(ownerUser, adminUser, memberUser));
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.listGroupMembers(conversationId);
        }

        assertEquals(List.of(currentUserId, adminUserId, memberUserId), result.stream().map(ChatMemberVO::getUserId).toList());
        assertEquals(List.of("群主", "管理员", "成员"), result.stream().map(ChatMemberVO::getNickname).toList());
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
        verify(chatConversationRepository, never()).save(any(ChatConversation.class));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(ownerMember, targetMember));
        when(chatConversationMemberRepository.updateById(targetMember)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.removeGroupMember(conversationId, memberUserId);
        }

        assertEquals(ChatConstants.MEMBER_STATUS_REMOVED, targetMember.getStatus());
        verify(chatConversationMemberRepository).updateById(targetMember);
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(ownerMember));

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class,
                    () -> userChatService.removeGroupMember(conversationId, currentUserId));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("不能移除自己，请使用退群接口", exception.getMessage());
        verify(chatConversationMemberRepository, never()).updateById(any(ChatConversationMember.class));
    }

    @Test
    void sendTextMessageShouldRejectDisabledConversation() {
        Long currentUserId = 1L;
        Long conversationId = 6201L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_DISABLED);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话不存在或不可用", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
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
        verify(chatConversationRepository, never()).getById(any(Long.class));
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
        verify(chatConversationRepository, never()).getById(any(Long.class));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenCurrentMemberIsMuted() {
        Long currentUserId = 1L;
        Long conversationId = 6205L;
        LocalDateTime muteUntil = LocalDateTime.now().plusSeconds(60);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMuteUntil(muteUntil);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember), List.of(selfMember));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户已被禁言，暂时不能发送消息", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(ownerMember));
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatConversationMemberRepository.removeAllActiveMembers(conversationId)).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.dissolveGroup(conversationId);
        }

        assertEquals(ChatConstants.CONVERSATION_STATUS_DISSOLVED, conversation.getStatus());
        verify(chatConversationRepository).updateById(conversation);
        verify(chatConversationMemberRepository).removeAllActiveMembers(conversationId);
    }

    @Test
    void getMyConversationShouldRejectDissolvedConversation() {
        Long currentUserId = 1L;
        Long globalConversationId = 9002L;
        Long conversationId = 6302L;

        ChatConversation globalConversation = new ChatConversation();
        globalConversation.setId(globalConversationId);
        globalConversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        globalConversation.setIsAllSite(1);
        globalConversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversation dissolvedConversation = new ChatConversation();
        dissolvedConversation.setId(conversationId);
        dissolvedConversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        dissolvedConversation.setStatus(ChatConstants.CONVERSATION_STATUS_DISSOLVED);

        ChatConversationMember globalMember = new ChatConversationMember();
        globalMember.setId(201L);
        globalMember.setConversationId(globalConversationId);
        globalMember.setUserId(currentUserId);
        globalMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageReadCursor globalCursor = new ChatMessageReadCursor();
        globalCursor.setId(202L);
        globalCursor.setConversationId(globalConversationId);
        globalCursor.setUserId(currentUserId);
        when(chatConversationRepository.getById(conversationId)).thenReturn(dissolvedConversation);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.getMyConversation(conversationId));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话不存在或不可用", exception.getMessage());
        verify(chatConversationRepository, never()).selectConversationDetail(any(Long.class), any(Long.class));
    }

    @Test
    void inviteGroupMembersShouldRestoreInactiveMemberAndReturnRecords() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7001L;
        Long lastMessageId = 888L;
        LocalDateTime lastMessageTime = LocalDateTime.now();

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
        ownerMember.setJoinedAt(lastMessageTime.minusSeconds(2));

        ChatConversationMember inactiveMember = new ChatConversationMember();
        inactiveMember.setConversationId(conversationId);
        inactiveMember.setUserId(memberUserId);
        inactiveMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        inactiveMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        inactiveMember.setJoinedAt(lastMessageTime.minusSeconds(1));

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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, memberUserId)).thenReturn(inactiveMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember), List.of(ownerMember, inactiveMember));
        when(chatConversationMemberRepository.updateById(inactiveMember)).thenReturn(true);

        when(sysUserRepository.getById(memberUserId)).thenReturn(memberUser);
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, memberUserId)).thenReturn(memberCursor);
        when(chatMessageReadCursorRepository.updateById(memberCursor)).thenReturn(true);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(ownerUser, memberUser));
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
        verify(chatConversationMemberRepository).updateById(inactiveMember);
        verify(chatConversationMemberRepository, never()).save(any(ChatConversationMember.class));
    }

    @Test
    void inviteGroupMembersShouldCreateNewMemberAndPushMembersUpdated() {
        Long currentUserId = 1L;
        Long memberUserId = 3L;
        Long conversationId = 7002L;
        Long lastMessageId = 889L;
        LocalDateTime lastMessageTime = LocalDateTime.now();

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

        ChatConversationMember newMember = new ChatConversationMember();
        newMember.setConversationId(conversationId);
        newMember.setUserId(memberUserId);
        newMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        newMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMessageReadCursor memberCursor = new ChatMessageReadCursor();
        memberCursor.setConversationId(conversationId);
        memberCursor.setUserId(memberUserId);
        memberCursor.setUnreadCount(0);

        SysUser memberUser = new SysUser();
        memberUser.setId(memberUserId);
        memberUser.setDeletedFlag(0);
        memberUser.setStatus(1);
        memberUser.setUsername("lisi");
        memberUser.setNickname("李四");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember), List.of(ownerMember, newMember));
        when(chatModelMapper.toConversationMember(conversationId, memberUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, lastMessageId, lastMessageTime)).thenReturn(newMember);
        when(chatConversationMemberRepository.save(newMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(41L);
            return true;
        });

        when(sysUserRepository.getById(memberUserId)).thenReturn(memberUser);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(memberUser));
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        ChatGroupMemberOperateRequest request = new ChatGroupMemberOperateRequest();
        request.setMemberUserIds(List.of(memberUserId));

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.inviteGroupMembers(conversationId, request);
        }

        assertEquals(2, result.size());
        verify(chatConversationMemberRepository).save(newMember);
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "members_invited".equals(payload.getAction())
                                && conversationId.equals(payload.getConversationId())
                                && payload.getMembers() != null
                                && payload.getMembers().size() == 2),
                eq(List.of(currentUserId, memberUserId)));
    }

    @Test
    void appointGroupAdminShouldPromoteMemberAndPushMembersUpdated() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7003L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, member), List.of(ownerMember, member));
        when(chatConversationMemberRepository.updateById(member)).thenReturn(true);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.appointGroupAdmin(conversationId, memberUserId);
        }

        assertEquals(ChatConstants.MEMBER_ROLE_ADMIN, member.getMemberRole());
        assertEquals(2, result.size());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "admin_appointed".equals(payload.getAction())
                                && memberUserId.equals(payload.getAffectedUserId())),
                eq(List.of(currentUserId, memberUserId)));
    }

    @Test
    void removeGroupAdminShouldDemoteMemberAndPushMembersUpdated() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7004L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember ownerMember = new ChatConversationMember();
        ownerMember.setConversationId(conversationId);
        ownerMember.setUserId(currentUserId);
        ownerMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        ownerMember.setMemberRole(ChatConstants.MEMBER_ROLE_OWNER);

        ChatConversationMember adminMember = new ChatConversationMember();
        adminMember.setConversationId(conversationId);
        adminMember.setUserId(memberUserId);
        adminMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminMember.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, adminMember), List.of(ownerMember, adminMember));
        when(chatConversationMemberRepository.updateById(adminMember)).thenReturn(true);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.removeGroupAdmin(conversationId, memberUserId);
        }

        assertEquals(ChatConstants.MEMBER_ROLE_MEMBER, adminMember.getMemberRole());
        assertEquals(2, result.size());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "admin_removed".equals(payload.getAction())
                                && memberUserId.equals(payload.getAffectedUserId())),
                eq(List.of(currentUserId, memberUserId)));
    }

    @Test
    void transferGroupOwnerShouldPromoteTargetOwnerAndPushConversationUpdated() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7005L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setOwnerId(currentUserId);

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

        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(conversationId);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        detailItem.setOwnerId(memberUserId);
        detailItem.setName("Owner Changed");

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversationVO.setName("Owner Changed");
        conversationVO.setOwnerId(memberUserId);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, memberUserId)).thenReturn(targetMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, targetMember), List.of(ownerMember, targetMember), List.of(ownerMember, targetMember));
        when(chatConversationMemberRepository.updateById(ownerMember)).thenReturn(true);
        when(chatConversationMemberRepository.updateById(targetMember)).thenReturn(true);
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatConversationRepository.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            return vo;
        });

        ChatTransferGroupOwnerRequest request = new ChatTransferGroupOwnerRequest();
        request.setTargetUserId(memberUserId);

        ChatConversationVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.transferGroupOwner(conversationId, request);
        }

        assertEquals(memberUserId, conversation.getOwnerId());
        assertEquals(ChatConstants.MEMBER_ROLE_ADMIN, ownerMember.getMemberRole());
        assertEquals(ChatConstants.MEMBER_ROLE_OWNER, targetMember.getMemberRole());
        assertEquals(memberUserId, result.getOwnerId());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                payload != null && "owner_transferred".equals(payload.getAction())), eq(List.of(currentUserId, memberUserId)));
        verify(chatPushService).pushConversationUpdated(argThat(payload ->
                payload != null
                        && "owner_transferred".equals(payload.getAction())
                        && memberUserId.equals(payload.getOwnerId())), eq(List.of(currentUserId, memberUserId)));
    }

    @Test
    void muteGroupMemberShouldUpdateMuteUntilAndPushMembersUpdated() {
        Long currentUserId = 1L;
        Long memberUserId = 2L;
        Long conversationId = 7006L;
        LocalDateTime muteUntil = LocalDateTime.now().plusSeconds(60);

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember adminMember = new ChatConversationMember();
        adminMember.setConversationId(conversationId);
        adminMember.setUserId(currentUserId);
        adminMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        adminMember.setMemberRole(ChatConstants.MEMBER_ROLE_ADMIN);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(memberUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(adminMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(adminMember, targetMember), List.of(adminMember, targetMember));
        when(chatConversationMemberRepository.updateById(targetMember)).thenReturn(true);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenAnswer(invocation -> {
            ChatConversationMember source = invocation.getArgument(0);
            ChatMemberVO vo = new ChatMemberVO();
            vo.setRole(source.getMemberRole());
            vo.setStatus(source.getStatus());
            vo.setMuteUntil(source.getMuteUntil());
            return vo;
        });

        ChatMuteMemberRequest request = new ChatMuteMemberRequest();
        request.setMuteUntil(muteUntil);

        List<ChatMemberVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.muteGroupMember(conversationId, memberUserId, request);
        }

        assertEquals(muteUntil, targetMember.getMuteUntil());
        assertEquals(2, result.size());
        verify(chatPushService).pushMembersUpdated(argThat(payload ->
                        payload != null
                                && "member_mute_updated".equals(payload.getAction())
                                && memberUserId.equals(payload.getAffectedUserId())),
                eq(List.of(currentUserId, memberUserId)));
    }

    @Test
    void sendTextMessageShouldCreateSingleConversationWhenTargetUserProvided() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 8001L;
        Long messageId = 9003L;
        LocalDateTime now = LocalDateTime.now();

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

        when(sysUserRepository.getById(targetUserId)).thenReturn(targetUser);
        when(chatConversationRepository.findBySinglePairKey("1:2")).thenReturn(null);
        when(chatConversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(conversationId);
            saved.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
            saved.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            saved.setSinglePairKey("1:2");
            return true;
        });

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId))
                .thenReturn(null, null, selfMember);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, targetUserId))
                .thenReturn(null);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, targetMember));

        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedSelfMember);
        when(chatModelMapper.toConversationMember(conversationId, targetUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_MANUAL, null, null)).thenReturn(mappedTargetMember);
        when(chatConversationMemberRepository.save(any(ChatConversationMember.class))).thenReturn(true);
        when(chatConversationMemberRepository.updateById(any(ChatConversationMember.class))).thenReturn(true);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(System.nanoTime());
            return true;
        });
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageRepository.save(mappedMessage)).thenAnswer(invocation -> {
            mappedMessage.setId(messageId);
            mappedMessage.setCreatedAt(now);
            return true;
        });
        when(chatConversationRepository.updateById(any(ChatConversation.class))).thenReturn(true);
        when(chatMessageRecipientRepository.saveBatch(anyCollection())).thenReturn(true);
        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of());
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(currentUser));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setTargetUserId(targetUserId);
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        assertEquals(conversationId, mappedMessage.getConversationId());
        verify(chatConversationRepository).save(any(ChatConversation.class));
        verify(chatConversationMemberRepository).save(mappedSelfMember);
        verify(chatConversationMemberRepository).save(mappedTargetMember);
    }

    @Test
    void sendTextMessageShouldRejectWhenUserIsNotConversationMember() {
        Long currentUserId = 1L;
        Long conversationId = 8005L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setContent("hello");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.sendTextMessage(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember));
        when(chatMessageRepository.findBySenderAndClientMessageId(currentUserId, "c-1")).thenReturn(existingMessage);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(currentUser));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setClientMessageId("c-1");
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(chatMessageRecipientRepository, never()).saveBatch(anyCollection());
        verify(chatPushService, never()).pushMessageCreated(any(ChatMessageVO.class), anyCollection());
    }

    @Test
    void sendTextMessageShouldReturnExistingMessageWhenSaveHitsDuplicateClientMessageId() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 9002L;
        Long messageId = 9902L;

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

        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setId(messageId);
        existingMessage.setConversationId(conversationId);
        existingMessage.setSenderId(currentUserId);
        existingMessage.setClientMessageId("dup-1");

        ChatMessageHistoryItem historyItem = new ChatMessageHistoryItem();
        historyItem.setId(messageId);
        historyItem.setConversationId(conversationId);
        historyItem.setSenderId(currentUserId);
        historyItem.setClientMessageId("dup-1");
        historyItem.setContent("hello");
        historyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        historyItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser currentUser = new SysUser();
        currentUser.setId(currentUserId);
        currentUser.setUsername("admin");
        currentUser.setNickname("管理员");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, targetMember));
        when(chatModelMapper.toTextMessage(any(ChatSendTextRequest.class))).thenReturn(mappedMessage);
        when(chatMessageRepository.save(mappedMessage)).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate client message"));
        when(chatMessageRepository.findBySenderAndClientMessageId(currentUserId, "dup-1")).thenReturn(existingMessage);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(historyItem);
        when(chatModelMapper.toMessageVO(historyItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(currentUser));

        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(conversationId);
        request.setClientMessageId("dup-1");
        request.setContent("hello");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.sendTextMessage(request);
        }

        assertEquals(messageId, result.getId());
        verify(chatMessageRepository).save(mappedMessage);
        verify(chatMessageRecipientRepository, never()).saveBatch(anyCollection());
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
        selfMember.setId(61L);
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, readMessageId)).thenReturn(historyItem);
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
        verify(chatMessageReadCursorRepository, never()).updateById(any(ChatMessageReadCursor.class));
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
        selfMember.setId(61L);
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRepository.countMessagePage(conversationId, currentUserId, null)).thenReturn(1L);
        when(chatMessageRepository.selectMessagePage(conversationId, currentUserId, null, 0L, 100L)).thenReturn(List.of(item));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);
        when(chatConversationMemberRepository.updateById(selfMember)).thenReturn(true);
        when(chatMessageRecipientRepository.batchMarkDelivered(eq(conversationId), eq(currentUserId), any(), any())).thenReturn(true);
        when(chatMessageReadCursorRepository.advanceDeliveredState(eq(cursor.getId()), eq(messageId), any())).thenReturn(true);
        when(chatConversationMemberRepository.advanceDeliveredState(eq(selfMember.getId()), eq(messageId), any())).thenReturn(true);

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
    }

    @Test
    void pageMyMessagesShouldRejectDissolvedConversation() {
        Long currentUserId = 2L;
        Long conversationId = 9301L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_DISSOLVED);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatMessagePageQuery query = new ChatMessagePageQuery();
        query.setCurrent(1L);
        query.setSize(20L);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.pageMyMessages(conversationId, query));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("会话不存在或不可用", exception.getMessage());
        verify(chatMessageRepository, never()).countMessagePage(any(Long.class), any(Long.class), any());
        verify(chatMessageRepository, never()).selectMessagePage(any(Long.class), any(Long.class), any(), any(Long.class), any(Long.class));
    }

    @Test
    void pageMyMessagesShouldRejectRemovedMemberInNormalGroup() {
        Long currentUserId = 2L;
        Long conversationId = 9303L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember removedMember = new ChatConversationMember();
        removedMember.setConversationId(conversationId);
        removedMember.setUserId(currentUserId);
        removedMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);

        ChatMessagePageQuery query = new ChatMessagePageQuery();
        query.setCurrent(1L);
        query.setSize(20L);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.pageMyMessages(conversationId, query));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("当前用户不在该会话中", exception.getMessage());
        verify(chatMessageRepository, never()).countMessagePage(any(Long.class), any(Long.class), any());
    }

    @Test
    void pageMyMessagesShouldRestoreInactiveGlobalMembershipAndAllowHistory() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 9304L;
        Long messageId = 9906L;
        LocalDateTime lastMessageTime = LocalDateTime.now();

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        conversation.setIsAllSite(1);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        conversation.setLastMessageId(messageId);
        conversation.setLastMessageTime(lastMessageTime);

        ChatConversationMember inactiveMember = new ChatConversationMember();
        inactiveMember.setId(91L);
        inactiveMember.setConversationId(conversationId);
        inactiveMember.setUserId(currentUserId);
        inactiveMember.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        inactiveMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);
        inactiveMember.setLastReadMessageId(1L);
        inactiveMember.setLastDeliveredMessageId(1L);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        senderMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(92L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setUnreadCount(5);
        cursor.setReadMessageId(1L);
        cursor.setDeliveredMessageId(1L);

        ChatMessageHistoryItem item = new ChatMessageHistoryItem();
        item.setId(messageId);
        item.setConversationId(conversationId);
        item.setSenderId(senderUserId);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("global");
        item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_PENDING);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(senderUserId);
        sender.setUsername("sender");
        sender.setNickname("发送者");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationRepository.findGlobalConversation()).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(inactiveMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(inactiveMember, senderMember));
        when(chatConversationMemberRepository.updateById(inactiveMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageReadCursorRepository.updateById(cursor)).thenReturn(true);

        when(chatMessageRepository.countMessagePage(conversationId, currentUserId, null)).thenReturn(1L);
        when(chatMessageRepository.selectMessagePage(conversationId, currentUserId, null, 0L, 20L)).thenReturn(List.of(item));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();
        query.setCurrent(1L);
        query.setSize(20L);

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(messageId, result.getRecords().get(0).getId());
        assertEquals(ChatConstants.MEMBER_STATUS_NORMAL, inactiveMember.getStatus());
        assertEquals(messageId, cursor.getReadMessageId());
        assertEquals(Integer.valueOf(0), cursor.getUnreadCount());
        assertEquals(ChatConstants.DELIVERY_STATUS_DELIVERED, item.getDeliveryStatus());
    }

    @Test
    void pageMyConversationsShouldCreateGlobalConversationMembershipWhenMissing() {
        Long currentUserId = 1L;
        Long conversationId = 9301L;

        ChatConversationMember mappedMember = new ChatConversationMember();
        mappedMember.setConversationId(conversationId);
        mappedMember.setUserId(currentUserId);
        when(chatConversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(conversationId);
            return true;
        });
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_SYSTEM, null, null)).thenReturn(mappedMember);
        when(chatConversationMemberRepository.save(mappedMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(61L);
            return true;
        });

        ChatConversationPageQuery query = new ChatConversationPageQuery();
        query.setKeyword("  system  ");
        when(chatConversationRepository.countConversationPage(currentUserId, "system")).thenReturn(0L);

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(query);
        }

        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(20L, result.getSize());
        verify(chatConversationRepository).save(any(ChatConversation.class));
        verify(chatConversationMemberRepository).save(mappedMember);
        verify(chatMessageReadCursorRepository).save(any(ChatMessageReadCursor.class));
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
        when(chatConversationRepository.findGlobalConversation()).thenReturn(null, conversation);
        when(chatConversationRepository.save(any(ChatConversation.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate global conversation"));
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(null);
        when(chatModelMapper.toConversationMember(conversationId, currentUserId, ChatConstants.MEMBER_ROLE_MEMBER,
                ChatConstants.JOIN_SOURCE_SYSTEM, null, null)).thenReturn(mappedMember);
        when(chatConversationMemberRepository.save(mappedMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.save(any(ChatMessageReadCursor.class))).thenAnswer(invocation -> {
            ChatMessageReadCursor cursor = invocation.getArgument(0);
            cursor.setId(62L);
            return true;
        });

        when(chatConversationRepository.countConversationPage(currentUserId, null)).thenReturn(0L);

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(new ChatConversationPageQuery());
        }

        assertEquals(0L, result.getTotal());
        verify(chatConversationRepository).save(any(ChatConversation.class));
        verify(chatConversationMemberRepository).save(mappedMember);
        verify(chatMessageReadCursorRepository).save(any(ChatMessageReadCursor.class));
    }

    @Test
    void pageMyConversationsShouldRestoreInactiveGlobalMembershipAndResetCursor() {
        Long currentUserId = 1L;
        Long conversationId = 9303L;
        Long lastMessageId = 8001L;
        LocalDateTime lastMessageTime = LocalDateTime.now();

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
        when(chatConversationRepository.findGlobalConversation()).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(inactiveMember);
        when(chatConversationMemberRepository.updateById(inactiveMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageReadCursorRepository.updateById(cursor)).thenReturn(true);

        when(chatConversationRepository.countConversationPage(currentUserId, null)).thenReturn(0L);

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
        verify(chatConversationMemberRepository).updateById(inactiveMember);
        verify(chatMessageReadCursorRepository).updateById(cursor);
        verify(chatConversationMemberRepository, never()).save(any(ChatConversationMember.class));
    }

    @Test
    void pageMyConversationsShouldReturnPagedRecordsForNormalConversation() {
        Long currentUserId = 1L;
        Long globalConversationId = 9304L;
        Long conversationId = 9305L;
        Long targetUserId = 2L;

        ChatConversation globalConversation = new ChatConversation();
        globalConversation.setId(globalConversationId);
        globalConversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        globalConversation.setIsAllSite(1);
        globalConversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember globalMember = new ChatConversationMember();
        globalMember.setId(81L);
        globalMember.setConversationId(globalConversationId);
        globalMember.setUserId(currentUserId);
        globalMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        globalMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatMessageReadCursor globalCursor = new ChatMessageReadCursor();
        globalCursor.setId(82L);
        globalCursor.setConversationId(globalConversationId);
        globalCursor.setUserId(currentUserId);
        globalCursor.setUnreadCount(0);

        ChatConversationListItem item = new ChatConversationListItem();
        item.setId(conversationId);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        item.setUnreadCount(2);
        item.setLastMessageId(9001L);
        item.setLastMessageSenderId(targetUserId);
        item.setLastMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setLastMessageContent("hello");

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationMember targetMember = new ChatConversationMember();
        targetMember.setConversationId(conversationId);
        targetMember.setUserId(targetUserId);
        targetMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        targetMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(conversationId);
        conversationVO.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);

        ChatConversationLastMessageVO lastMessageVO = new ChatConversationLastMessageVO();
        lastMessageVO.setId(9001L);
        lastMessageVO.setContent("hello");

        SysUser targetUser = new SysUser();
        targetUser.setId(targetUserId);
        targetUser.setUsername("zhangsan");
        targetUser.setNickname("张三");
        when(chatConversationRepository.findGlobalConversation()).thenReturn(globalConversation);
        when(chatConversationMemberRepository.findByConversationAndUser(globalConversationId, currentUserId)).thenReturn(globalMember);
        when(chatMessageReadCursorRepository.findByConversationAndUser(globalConversationId, currentUserId)).thenReturn(globalCursor);
        when(chatConversationMemberRepository.listActiveByConversationIds(anyCollection())).thenReturn(List.of(selfMember, targetMember));
        when(chatConversationMemberRepository.updateById(globalMember)).thenReturn(true);
        when(chatMessageReadCursorRepository.updateById(globalCursor)).thenReturn(true);

        when(chatConversationRepository.countConversationPage(currentUserId, "hello")).thenReturn(1L);
        when(chatConversationRepository.selectConversationPage(currentUserId, "hello", 0L, 5L)).thenReturn(List.of(item));
        when(chatModelMapper.toConversationVO(item)).thenReturn(conversationVO);
        when(chatModelMapper.toConversationLastMessageVO(item)).thenReturn(lastMessageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(targetUser));

        ChatConversationPageQuery query = new ChatConversationPageQuery();
        query.setCurrent(1L);
        query.setSize(5L);
        query.setKeyword(" hello ");

        PageResult<ChatConversationVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyConversations(query);
        }

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(conversationId, result.getRecords().get(0).getId());
        assertEquals(targetUserId, result.getRecords().get(0).getTargetUserId());
        assertEquals("张三", result.getRecords().get(0).getName());
        assertEquals(Integer.valueOf(2), result.getRecords().get(0).getUnreadCount());
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageRepository.countMessagePage(conversationId, currentUserId, null)).thenReturn(2L);
        when(chatMessageRepository.selectMessagePage(conversationId, currentUserId, null, 0L, 20L))
                .thenReturn(List.of(deliveredItem, selfItem));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(deliveredItem)).thenReturn(deliveredVO);
        when(chatModelMapper.toMessageVO(selfItem)).thenReturn(selfVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(selfMessageId, deliveredMessageId), result.getRecords().stream().map(ChatMessageVO::getId).toList());
        verify(chatConversationMemberRepository, never()).updateById(selfMember);
    }

    @Test
    void pageMyMessagesShouldPreferLiveReplySnapshotOverPayloadSnapshot() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long replyMessageId = 9906L;
        Long conversationId = 9303L;

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

        com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO payloadReply =
                new com.cybzacg.blogbackend.module.chat.model.common.ChatReplyMessageVO();
        payloadReply.setId(replyMessageId);
        payloadReply.setContent("old snapshot");
        payloadReply.setState(ChatConstants.REPLY_STATE_NORMAL);
        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        payload.setReply(payloadReply);

        ChatMessageHistoryItem item = new ChatMessageHistoryItem();
        item.setId(9907L);
        item.setConversationId(conversationId);
        item.setSenderId(senderUserId);
        item.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        item.setContent("reply message");
        item.setReplyMessageId(replyMessageId);
        item.setPayloadJson(JsonUtils.toJson(payload));
        item.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);

        ChatMessageHistoryItem liveReplyItem = new ChatMessageHistoryItem();
        liveReplyItem.setId(replyMessageId);
        liveReplyItem.setConversationId(conversationId);
        liveReplyItem.setSenderId(senderUserId);
        liveReplyItem.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        liveReplyItem.setContent("new live content");
        liveReplyItem.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(item.getId());

        SysUser sender = new SysUser();
        sender.setId(senderUserId);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageRepository.countMessagePage(conversationId, currentUserId, null)).thenReturn(1L);
        when(chatMessageRepository.selectMessagePage(conversationId, currentUserId, null, 0L, 20L)).thenReturn(List.of(item));
        when(chatMessageRepository.selectVisibleMessagesByIds(conversationId, currentUserId, List.of(replyMessageId))).thenReturn(List.of(liveReplyItem));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(1, result.getRecords().size());
        assertNotNull(result.getRecords().get(0).getReply());
        assertEquals("new live content", result.getRecords().get(0).getReply().getContent());
        assertEquals(ChatConstants.REPLY_STATE_NORMAL, result.getRecords().get(0).getReply().getState());
    }

    @Test
    void pageMyMessagesShouldAdvanceDeliveredCursorWithMonotonicUpdate() {
        Long currentUserId = 2L;
        Long senderUserId = 1L;
        Long conversationId = 9304L;
        Long pendingMessageId = 9908L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_SINGLE);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setId(41L);
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setLastDeliveredMessageId(9900L);

        ChatConversationMember senderMember = new ChatConversationMember();
        senderMember.setConversationId(conversationId);
        senderMember.setUserId(senderUserId);
        senderMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(42L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setDeliveredMessageId(9900L);
        cursor.setUnreadCount(1);

        ChatMessageHistoryItem pendingItem = new ChatMessageHistoryItem();
        pendingItem.setId(pendingMessageId);
        pendingItem.setConversationId(conversationId);
        pendingItem.setSenderId(senderUserId);
        pendingItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_PENDING);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(pendingMessageId);

        SysUser sender = new SysUser();
        sender.setId(senderUserId);
        sender.setUsername("zhangsan");
        sender.setNickname("张三");

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, senderMember));
        when(chatMessageRepository.countMessagePage(conversationId, currentUserId, null)).thenReturn(1L);
        when(chatMessageRepository.selectMessagePage(conversationId, currentUserId, null, 0L, 20L)).thenReturn(List.of(pendingItem));
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatModelMapper.toMessageVO(pendingItem)).thenReturn(messageVO);

        ChatMessagePageQuery query = new ChatMessagePageQuery();

        PageResult<ChatMessageVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.pageMyMessages(conversationId, query);
        }

        assertEquals(1, result.getRecords().size());
        assertEquals(ChatConstants.DELIVERY_STATUS_DELIVERED, pendingItem.getDeliveryStatus());
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
        when(chatMessageRecipientRepository.findVisibleByUserAndMessage(currentUserId, messageId)).thenReturn(recipient);
        when(chatMessageRepository.getById(messageId)).thenReturn(message);

        ChatEditMessageRequest request = new ChatEditMessageRequest();
        request.setContent("new content");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.editMessage(messageId, request));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("只有文本消息支持编辑", exception.getMessage());
        verify(chatMessageRepository, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void revokeMessageShouldReleaseChatFileReference() {
        Long currentUserId = 1L;
        Long conversationId = 1001L;
        Long messageId = 9802L;
        Long fileId = 7001L;

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setMessageId(messageId);
        recipient.setRecipientUserId(currentUserId);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);
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
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        when(chatMessageRecipientRepository.findVisibleByUserAndMessage(currentUserId, messageId)).thenReturn(recipient);
        when(chatMessageRepository.getById(messageId)).thenReturn(message);
        when(chatMessageRepository.updateById(message)).thenReturn(true);
        when(fileBusinessInfoRepository.listByReferenceTypeAndReferenceId(ChatConstants.FILE_MESSAGE_REFERENCE_TYPE, messageId))
                .thenReturn(List.of(fileReference));
        when(fileBusinessInfoRepository.removeByIds(List.of(501L))).thenReturn(true);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember));
        ChatMessageHistoryItem revokedItem = new ChatMessageHistoryItem();
        revokedItem.setId(messageId);
        revokedItem.setConversationId(conversationId);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(revokedItem);
        when(chatModelMapper.toMessageVO(any(ChatMessageHistoryItem.class))).thenReturn(new ChatMessageVO());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.revokeMessage(messageId);
        }

        assertEquals(ChatConstants.REVOKE_STATUS_REVOKED, message.getRevokeStatus());
        assertEquals(ChatConstants.MESSAGE_REVOKED_PLACEHOLDER, message.getContent());
        assertEquals(null, message.getPayloadJson());
        verify(fileBusinessInfoRepository).removeByIds(List.of(501L));
        verify(fileLifecycleService).syncFileAfterReferenceRemoval(fileId);
    }

    @Test
    void deleteMessageShouldPushDeletedEventForCurrentUser() {
        Long currentUserId = 1L;
        Long conversationId = 9803L;
        Long messageId = 7701L;

        ChatMessageRecipient recipient = new ChatMessageRecipient();
        recipient.setMessageId(messageId);
        recipient.setRecipientUserId(currentUserId);
        recipient.setVisibleStatus(ChatConstants.VISIBLE_STATUS_VISIBLE);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversationId(conversationId);
        message.setSenderId(2L);

        ChatMessageReadCursor cursor = new ChatMessageReadCursor();
        cursor.setId(33L);
        cursor.setConversationId(conversationId);
        cursor.setUserId(currentUserId);
        cursor.setUnreadCount(5);
        when(chatMessageRecipientRepository.findVisibleByUserAndMessage(currentUserId, messageId)).thenReturn(recipient);
        when(chatMessageRepository.getById(messageId)).thenReturn(message);
        when(chatMessageRecipientRepository.hideMessage(eq(conversationId), eq(currentUserId), eq(messageId))).thenReturn(true);
        when(chatMessageReadCursorRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(cursor);
        when(chatMessageRecipientRepository.countUnread(eq(conversationId), eq(currentUserId))).thenReturn(2L);
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.deleteMessage(messageId);
        }

        assertEquals(2, cursor.getUnreadCount());
        verify(chatPushService).pushMessageDeleted(argThat(payload ->
                        payload != null
                                && conversationId.equals(payload.getConversationId())
                                && messageId.equals(payload.getMessageId())
                                && currentUserId.equals(payload.getUserId())
                                && Integer.valueOf(2).equals(payload.getUnreadCount())),
                eq(List.of(currentUserId)));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember), List.of(selfMember));
        when(fileBusinessInfoRepository.getById(businessId)).thenReturn(fileReference);

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
        LocalDateTime now = LocalDateTime.now();

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
        item.setUpdatedAt(now.plusSeconds(1));

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
        when(chatMessageRepository.getById(messageId)).thenReturn(message);
        when(chatMessageRecipientRepository.findVisibleByUserAndMessage(currentUserId, messageId)).thenReturn(recipient);
        when(chatMessageRepository.updateById(message)).thenReturn(true);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(item);
        when(chatModelMapper.toMessageVO(item)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember, targetMember));

        ChatEditMessageRequest request = new ChatEditMessageRequest();
        request.setContent("edited");

        ChatMessageVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            result = userChatService.editMessage(messageId, request);
        }

        assertEquals("edited", message.getContent());
        assertEquals(messageId, result.getId());
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
        LocalDateTime now = LocalDateTime.now();

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
        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember));

        when(fileBusinessInfoRepository.getById(businessId)).thenReturn(tempReference);
        when(fileInfoRepository.getById(7001L)).thenReturn(fileInfo);
        when(fileBusinessInfoRepository.findLatestByFileUserReference(7001L, currentUserId, ChatConstants.FILE_MESSAGE_REFERENCE_TYPE, messageId))
                .thenReturn(null);
        when(fileBusinessInfoRepository.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo reference = invocation.getArgument(0);
            reference.setId(901L);
            return true;
        });
        when(fileBusinessInfoRepository.removeById(businessId)).thenReturn(true);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            saved.setCreatedAt(now);
            return true;
        });
        when(chatMessageRepository.updateById(any(ChatMessage.class))).thenReturn(true);
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientRepository.saveBatch(any())).thenReturn(true);
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);
        when(chatConversationMemberRepository.updateById(any(ChatConversationMember.class))).thenReturn(true);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, replyMessageId)).thenReturn(replyItem);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(createdItem);
        when(chatModelMapper.toMessageVO(createdItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
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
        verify(chatMessageRepository).save(argThat((ChatMessage saved) ->
                saved != null
                        && ChatConstants.MESSAGE_TYPE_IMAGE.equals(saved.getMessageType())
                        && "[图片] demo.png".equals(saved.getContent())
                        && Objects.equals(replyMessageId, saved.getReplyMessageId())
        ));
        verify(chatMessageRepository).updateById(argThat((ChatMessage updated) -> {
            if (updated == null || !Objects.equals(messageId, updated.getId())) {
                return false;
            }
            ChatMessagePayloadVO payload = JsonUtils.fromJson(updated.getPayloadJson(), ChatMessagePayloadVO.class);
            return payload != null
                    && payload.getFile() != null
                    && "https://example.com/demo.png".equals(payload.getFile().getPreviewUrl())
                    && "https://example.com/demo.png".equals(payload.getFile().getThumbnailUrl())
                    && payload.getFile().getWidth() == null
                    && payload.getFile().getHeight() == null
                    && ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE.equals(payload.getFile().getTranscodeStatus())
                    && payload.getReply() != null
                    && Objects.equals(replyMessageId, payload.getReply().getId())
                    && "origin".equals(payload.getReply().getContent())
                    && ChatConstants.REPLY_STATE_NORMAL.equals(payload.getReply().getState());
        }));
        verify(chatPushService).pushMessageCreated(messageVO, List.of(currentUserId, targetUserId));
        verify(chatAttachmentAsyncProcessingService).scheduleAfterCommit(messageId, messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void sendFileMessageShouldClassifyVoiceAndScheduleAsyncProcessing() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;
        Long conversationId = 9904L;
        Long messageId = 8803L;
        Long businessId = 802L;
        LocalDateTime now = LocalDateTime.now();

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
        tempReference.setFileId(7002L);
        tempReference.setIsPublic(0);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7002L);
        fileInfo.setMimeType("audio/mpeg");
        fileInfo.setOriginalName("voice.mp3");
        fileInfo.setFileName("voice.mp3");
        fileInfo.setFileUrl("https://example.com/voice.mp3");
        fileInfo.setFileSize(456L);
        fileInfo.setFileType("mp3");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());

        ChatMessageHistoryItem createdItem = new ChatMessageHistoryItem();
        createdItem.setId(messageId);
        createdItem.setConversationId(conversationId);
        createdItem.setSenderId(currentUserId);
        createdItem.setMessageType(ChatConstants.MESSAGE_TYPE_VOICE);
        createdItem.setContent("[语音] voice.mp3");
        createdItem.setDeliveryStatus(ChatConstants.DELIVERY_STATUS_READ);
        createdItem.setCreatedAt(now);

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);

        SysUser sender = new SysUser();
        sender.setId(currentUserId);
        sender.setUsername("admin");
        sender.setNickname("管理员");

        ChatMessageReadCursor senderCursor = new ChatMessageReadCursor();
        senderCursor.setId(103L);
        senderCursor.setConversationId(conversationId);
        senderCursor.setUserId(currentUserId);
        senderCursor.setUnreadCount(0);

        ChatMessageReadCursor targetCursor = new ChatMessageReadCursor();
        targetCursor.setId(104L);
        targetCursor.setConversationId(conversationId);
        targetCursor.setUserId(targetUserId);
        targetCursor.setUnreadCount(0);
        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(selfMember, targetMember));

        when(fileBusinessInfoRepository.getById(businessId)).thenReturn(tempReference);
        when(fileInfoRepository.getById(7002L)).thenReturn(fileInfo);
        when(fileBusinessInfoRepository.findLatestByFileUserReference(7002L, currentUserId, ChatConstants.FILE_MESSAGE_REFERENCE_TYPE, messageId))
                .thenReturn(null);
        when(fileBusinessInfoRepository.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo reference = invocation.getArgument(0);
            reference.setId(902L);
            return true;
        });
        when(fileBusinessInfoRepository.removeById(businessId)).thenReturn(true);

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            saved.setCreatedAt(now);
            return true;
        });
        when(chatMessageRepository.updateById(any(ChatMessage.class))).thenReturn(true);
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatMessageRecipientRepository.saveBatch(any())).thenReturn(true);
        when(chatMessageReadCursorRepository.updateById(any(ChatMessageReadCursor.class))).thenReturn(true);
        when(chatConversationMemberRepository.updateById(any(ChatConversationMember.class))).thenReturn(true);
        when(chatMessageRepository.selectVisibleMessageById(conversationId, currentUserId, messageId)).thenReturn(createdItem);
        when(chatModelMapper.toMessageVO(createdItem)).thenReturn(messageVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of(sender));
        when(chatWebSocketSessionRegistry.getSessions(targetUserId)).thenReturn(List.of());

        ChatSendFileRequest request = new ChatSendFileRequest();
        request.setConversationId(conversationId);
        request.setBusinessId(businessId);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.sendFileMessage(request);
        }

        verify(chatMessageRepository).save(argThat((ChatMessage saved) ->
                saved != null
                        && ChatConstants.MESSAGE_TYPE_VOICE.equals(saved.getMessageType())
                        && "[语音] voice.mp3".equals(saved.getContent())
        ));
        verify(chatMessageRepository).updateById(argThat((ChatMessage updated) -> {
            if (updated == null || !Objects.equals(messageId, updated.getId())) {
                return false;
            }
            ChatMessagePayloadVO payload = JsonUtils.fromJson(updated.getPayloadJson(), ChatMessagePayloadVO.class);
            return payload != null
                    && payload.getFile() != null
                    && "https://example.com/voice.mp3".equals(payload.getFile().getPreviewUrl())
                    && payload.getFile().getDurationSeconds() == null
                    && payload.getFile().getWaveform() == null
                    && ChatConstants.ATTACHMENT_TRANSCODE_STATUS_PENDING.equals(payload.getFile().getTranscodeStatus());
        }));
        verify(chatAttachmentAsyncProcessingService).scheduleAfterCommit(messageId, messageVO, List.of(currentUserId, targetUserId));
    }

    @Test
    void updateGroupNoticeShouldRejectNormalMember() {
        Long currentUserId = 1L;
        Long conversationId = 9905L;

        ChatConversation conversation = new ChatConversation();
        conversation.setId(conversationId);
        conversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);

        ChatConversationMember selfMember = new ChatConversationMember();
        selfMember.setConversationId(conversationId);
        selfMember.setUserId(currentUserId);
        selfMember.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        selfMember.setMemberRole(ChatConstants.MEMBER_ROLE_MEMBER);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(selfMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId)).thenReturn(List.of(selfMember));

        var request = new com.cybzacg.blogbackend.module.chat.model.user.ChatGroupNoticeUpdateRequest();
        request.setNotice("new notice");

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            exception = assertThrows(BusinessException.class, () -> userChatService.updateGroupNotice(conversationId, request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("只有群主或管理员可以执行该操作", exception.getMessage());
        verify(chatConversationRepository, never()).updateById(any(ChatConversation.class));
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

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, currentUserId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, member), List.of(ownerMember, member));
        when(chatConversationRepository.updateById(conversation)).thenReturn(true);
        when(chatConversationRepository.selectConversationDetail(conversationId, currentUserId)).thenReturn(detailItem);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(conversationVO);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());

        var request = new com.cybzacg.blogbackend.module.chat.model.user.ChatGroupNoticeUpdateRequest();
        request.setNotice("new notice");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(currentUserId)) {
            userChatService.updateGroupNotice(conversationId, request);
        }

        verify(chatPushService).pushConversationUpdated(any(), eq(List.of(currentUserId, memberUserId)));
    }
}

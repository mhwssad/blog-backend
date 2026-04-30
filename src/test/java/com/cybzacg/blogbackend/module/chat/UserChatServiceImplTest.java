package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.user.*;
import com.cybzacg.blogbackend.module.chat.service.ChatConversationQueryService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageSendService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageLifecycleService;
import com.cybzacg.blogbackend.module.chat.service.ChatGroupManageService;
import com.cybzacg.blogbackend.module.chat.service.ChatChannelJoinService;
import com.cybzacg.blogbackend.module.chat.service.impl.UserChatServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserChatServiceImplTest {
    @Mock
    private ChatConversationQueryService conversationQueryService;
    @Mock
    private ChatMessageSendService messageSendService;
    @Mock
    private ChatMessageLifecycleService messageLifecycleService;
    @Mock
    private ChatGroupManageService groupManageService;
    @Mock
    private ChatChannelJoinService channelJoinService;

    private UserChatServiceImpl userChatService;

    @BeforeEach
    void setUp() {
        userChatService = new UserChatServiceImpl(
                conversationQueryService,
                messageSendService,
                messageLifecycleService,
                groupManageService,
                channelJoinService
        );
    }

    @Test
    void pageMyConversationsShouldDelegateToConversationQueryService() {
        ChatConversationPageQuery query = new ChatConversationPageQuery();
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            userChatService.pageMyConversations(query);
            verify(conversationQueryService).pageMyConversations(eq(1L), eq(query));
        }
    }

    @Test
    void getMyConversationShouldDelegateToConversationQueryService() {
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            userChatService.getMyConversation(1L);
            verify(conversationQueryService).getMyConversation(eq(1L), eq(1L));
        }
    }

    @Test
    void openSingleConversationShouldDelegateToChannelJoinService() {
        ChatOpenSingleConversationRequest request = new ChatOpenSingleConversationRequest();
        request.setTargetUserId(2L);
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            userChatService.openSingleConversation(request);
            verify(channelJoinService).openSingleConversation(eq(1L), eq(2L));
        }
    }

    @Test
    void pageMyMessagesShouldDelegateToMessageLifecycleService() {
        ChatMessagePageQuery query = new ChatMessagePageQuery();
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            userChatService.pageMyMessages(1L, query);
            verify(messageLifecycleService).pageMyMessages(eq(1L), eq(1L), eq(query));
        }
    }

    @Test
    void sendTextMessageShouldDelegateToMessageSendService() {
        ChatSendTextRequest request = new ChatSendTextRequest();
        request.setConversationId(1L);
        request.setContent("hello");
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            userChatService.sendTextMessage(request);
            verify(messageSendService).sendTextMessage(eq(1L), eq(request));
        }
    }
}
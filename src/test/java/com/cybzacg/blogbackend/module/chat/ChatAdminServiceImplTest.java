package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminGovernanceService;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminQueryService;
import com.cybzacg.blogbackend.module.chat.governance.service.impl.ChatAdminServiceImpl;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberRoleUpdateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberStatusUpdateRequest;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessageReceiptPageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatAdminServiceImplTest {
    @Mock
    private ChatAdminQueryService chatAdminQueryService;
    @Mock
    private ChatAdminGovernanceService chatAdminGovernanceService;

    private ChatAdminServiceImpl chatAdminService;

    @BeforeEach
    void setUp() {
        chatAdminService = new ChatAdminServiceImpl(
                chatAdminQueryService,
                chatAdminGovernanceService
        );
    }

    @Test
    void pageConversationsShouldDelegateToQueryService() {
        ChatAdminConversationPageQuery query = new ChatAdminConversationPageQuery();
        chatAdminService.pageConversations(query);
        verify(chatAdminQueryService).pageConversations(query);
    }

    @Test
    void getConversationShouldDelegateToQueryService() {
        chatAdminService.getConversation(1L);
        verify(chatAdminQueryService).getConversation(1L);
    }

    @Test
    void listMembersShouldDelegateToQueryService() {
        chatAdminService.listMembers(1L);
        verify(chatAdminQueryService).listMembers(1L);
    }

    @Test
    void pageMessagesShouldDelegateToQueryService() {
        ChatAdminMessagePageQuery query = new ChatAdminMessagePageQuery();
        chatAdminService.pageMessages(1L, query);
        verify(chatAdminQueryService).pageMessages(1L, query);
    }

    @Test
    void getMessageDetailShouldDelegateToQueryService() {
        chatAdminService.getMessageDetail(1L, 2L);
        verify(chatAdminQueryService).getMessageDetail(1L, 2L);
    }

    @Test
    void pageMessageReceiptsShouldDelegateToQueryService() {
        ChatAdminMessageReceiptPageQuery query = new ChatAdminMessageReceiptPageQuery();
        chatAdminService.pageMessageReceipts(1L, 2L, query);
        verify(chatAdminQueryService).pageMessageReceipts(1L, 2L, query);
    }

    @Test
    void updateMemberRoleShouldDelegateToGovernanceService() {
        ChatAdminMemberRoleUpdateRequest request = new ChatAdminMemberRoleUpdateRequest();
        chatAdminService.updateMemberRole(1L, 2L, request);
        verify(chatAdminGovernanceService).updateMemberRole(1L, 2L, request);
    }

    @Test
    void updateMemberStatusShouldDelegateToGovernanceService() {
        ChatAdminMemberStatusUpdateRequest request = new ChatAdminMemberStatusUpdateRequest();
        chatAdminService.updateMemberStatus(1L, 2L, request);
        verify(chatAdminGovernanceService).updateMemberStatus(1L, 2L, request);
    }

    @Test
    void updateMemberMuteShouldDelegateToGovernanceService() {
        ChatAdminMemberMuteUpdateRequest request = new ChatAdminMemberMuteUpdateRequest();
        chatAdminService.updateMemberMute(1L, 2L, request);
        verify(chatAdminGovernanceService).updateMemberMute(1L, 2L, request);
    }

    @Test
    void revokeMessageShouldDelegateToGovernanceService() {
        chatAdminService.revokeMessage(1L, 2L);
        verify(chatAdminGovernanceService).revokeMessage(1L, 2L);
    }

    @Test
    void updateConversationStatusShouldDelegateToGovernanceService() {
        chatAdminService.updateConversationStatus(1L, 1);
        verify(chatAdminGovernanceService).updateConversationStatus(1L, 1);
    }
}

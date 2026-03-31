package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.controller.ChatAdminController;
import com.cybzacg.blogbackend.module.chat.controller.UserChatController;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageDetailVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptVO;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAdminService;
import com.cybzacg.blogbackend.module.chat.service.UserChatService;
import com.cybzacg.blogbackend.utils.HttpServletResponseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ChatControllerSecurityTest.TestConfig.class)
class ChatControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserChatService userChatService;
    @Autowired
    private ChatAdminService chatAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(userChatService, chatAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void pageMyConversationsShouldRequireLogin() throws Exception {
        mockMvc.perform(get("/api/user/chat/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userChatService, never()).pageMyConversations(any());
    }

    @Test
    @WithMockUser
    void pageMyConversationsShouldAllowAuthenticatedUser() throws Exception {
        when(userChatService.pageMyConversations(any()))
                .thenReturn(PageResult.<ChatConversationVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(20L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/user/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(userChatService).pageMyConversations(any());
    }

    @Test
    void sendTextMessageShouldRequireLogin() throws Exception {
        mockMvc.perform(post("/api/user/chat/messages/text")
                        .contentType(APPLICATION_JSON)
                        .content("{\"conversationId\":1001,\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userChatService, never()).sendTextMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest.class));
    }

    @Test
    @WithMockUser
    void sendTextMessageShouldAllowAuthenticatedUser() throws Exception {
        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(9001L);
        messageVO.setContent("hello");
        when(userChatService.sendTextMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest.class)))
                .thenReturn(messageVO);

        mockMvc.perform(post("/api/user/chat/messages/text")
                        .contentType(APPLICATION_JSON)
                        .content("{\"conversationId\":1001,\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(9001));

        verify(userChatService).sendTextMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest.class));
    }

    @Test
    void sendFileMessageShouldRequireLogin() throws Exception {
        mockMvc.perform(post("/api/user/chat/messages/file")
                        .contentType(APPLICATION_JSON)
                        .content("{\"conversationId\":1001,\"businessId\":88}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userChatService, never()).sendFileMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest.class));
    }

    @Test
    @WithMockUser
    void sendFileMessageShouldAllowAuthenticatedUser() throws Exception {
        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(9002L);
        when(userChatService.sendFileMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest.class)))
                .thenReturn(messageVO);

        mockMvc.perform(post("/api/user/chat/messages/file")
                        .contentType(APPLICATION_JSON)
                        .content("{\"conversationId\":1001,\"businessId\":88}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(9002));

        verify(userChatService).sendFileMessage(any(com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest.class));
    }

    @Test
    @WithMockUser
    void revokeMessageShouldAllowAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/user/chat/messages/9001/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(userChatService).revokeMessage(9001L);
    }

    @Test
    @WithMockUser
    void deleteMessageShouldAllowAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/user/chat/messages/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(userChatService).deleteMessage(9001L);
    }

    @Test
    @WithMockUser
    void updateGroupNoticeShouldAllowAuthenticatedUser() throws Exception {
        ChatConversationVO conversationVO = new ChatConversationVO();
        conversationVO.setId(1001L);
        when(userChatService.updateGroupNotice(any(), any())).thenReturn(conversationVO);

        mockMvc.perform(put("/api/user/chat/groups/1001/notice")
                        .contentType(APPLICATION_JSON)
                        .content("{\"notice\":\"new notice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(1001));

        verify(userChatService).updateGroupNotice(org.mockito.ArgumentMatchers.eq(1001L), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void adminPageConversationsShouldAllowAuthorizedUser() throws Exception {
        when(chatAdminService.pageConversations(any()))
                .thenReturn(PageResult.<ChatAdminConversationVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/sys/chats/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(chatAdminService).pageConversations(any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:update")
    void adminPageConversationsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/chats/conversations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(chatAdminService, never()).pageConversations(any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void adminPageMessagesShouldAllowAuthorizedUser() throws Exception {
        when(chatAdminService.pageMessages(any(), any()))
                .thenReturn(PageResult.<ChatAdminMessageVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(20L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/sys/chats/conversations/1001/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(chatAdminService).pageMessages(org.mockito.ArgumentMatchers.eq(1001L), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void adminGetMessageDetailShouldAllowAuthorizedUser() throws Exception {
        ChatAdminMessageDetailVO detailVO = new ChatAdminMessageDetailVO();
        detailVO.setId(9001L);
        when(chatAdminService.getMessageDetail(1001L, 9001L)).thenReturn(detailVO);

        mockMvc.perform(get("/api/sys/chats/conversations/1001/messages/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.id").value(9001));

        verify(chatAdminService).getMessageDetail(1001L, 9001L);
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void adminPageMessageReceiptsShouldAllowAuthorizedUser() throws Exception {
        when(chatAdminService.pageMessageReceipts(any(), any(), any()))
                .thenReturn(PageResult.<ChatAdminMessageReceiptVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(20L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/sys/chats/conversations/1001/messages/9001/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(chatAdminService).pageMessageReceipts(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(9001L), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void updateConversationStatusShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/chats/conversations/1001/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(chatAdminService, never()).updateConversationStatus(any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:update")
    void adminRevokeMessageShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/chats/conversations/1001/messages/9001/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(chatAdminService).revokeMessage(1001L, 9001L);
    }

    @Test
    @WithMockUser(authorities = "content:chat:update")
    void adminUpdateMemberStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/chats/conversations/1001/members/2/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(chatAdminService).updateMemberStatus(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(2L), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:update")
    void adminUpdateMemberMuteShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/chats/conversations/1001/members/2/mute")
                        .contentType(APPLICATION_JSON)
                        .content("{\"muteUntil\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(chatAdminService).updateMemberMute(org.mockito.ArgumentMatchers.eq(1001L), org.mockito.ArgumentMatchers.eq(2L), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:query")
    void adminUpdateMemberRoleShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/chats/conversations/1001/members/2/role")
                        .contentType(APPLICATION_JSON)
                        .content("{\"role\":\"admin\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(chatAdminService, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:chat:update")
    void updateConversationStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/chats/conversations/1001/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(chatAdminService).updateConversationStatus(1001L, 0);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        UserChatService userChatService() {
            return mock(UserChatService.class);
        }

        @Bean
        ChatAdminService chatAdminService() {
            return mock(ChatAdminService.class);
        }

        @Bean
        UserChatController userChatController(UserChatService userChatService) {
            return new UserChatController(userChatService);
        }

        @Bean
        ChatAdminController chatAdminController(ChatAdminService chatAdminService) {
            return new ChatAdminController(chatAdminService);
        }

        @Bean("permission")
        SecurityPermissionChecker securityPermissionChecker() {
            return new SecurityPermissionChecker();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint((request, response, exception) ->
                                    HttpServletResponseUtils.writeJson(response,
                                            org.springframework.http.HttpStatus.UNAUTHORIZED.value(),
                                            ResultErrorCode.LOGIN_REQUIRED))
                            .accessDeniedHandler((request, response, exception) ->
                                    HttpServletResponseUtils.writeJson(response,
                                            org.springframework.http.HttpStatus.FORBIDDEN.value(),
                                            ResultErrorCode.FORBIDDEN)));
            return http.build();
        }
    }
}

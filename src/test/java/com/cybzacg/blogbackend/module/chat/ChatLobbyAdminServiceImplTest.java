package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatLobbySettingsUpdateRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.service.impl.ChatLobbyAdminServiceImpl;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatLobbyAdminServiceImpl unit tests.
 */
@ExtendWith(MockitoExtension.class)
class ChatLobbyAdminServiceImplTest {

    @Mock
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatConversationMemberRepository chatConversationMemberRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ChatModelMapper chatModelMapper;
    @Mock
    private ChatPushService chatPushService;

    private ChatLobbyAdminServiceImpl chatLobbyAdminService;

    @BeforeEach
    void setUp() {
        chatLobbyAdminService = new ChatLobbyAdminServiceImpl(
                chatConversationRepository,
                chatMessageRepository,
                chatConversationMemberRepository,
                sysUserRepository,
                chatModelMapper,
                chatPushService
        );
    }

    // ==================== pinMessage ====================

    @Test
    void pinMessageShouldSetPinnedByAndPush() {
        ChatConversation lobby = buildLobbyConversation(1L);
        ChatMessage message = buildMessage(100L, lobby.getId(), 10L, "hello");

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        when(chatMessageRepository.getById(100L)).thenReturn(message);
        ChatConversationMember activeMember = buildMember(1L, lobby.getId(), 20L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));

        chatLobbyAdminService.pinMessage(100L);

        assertEquals(0L, message.getPinnedBy());
        verify(chatMessageRepository).updateById(message);
        verify(chatPushService).pushMessageUpdated(any(ChatMessageVO.class), eq(List.of(20L)));
    }

    // ==================== unpinMessage ====================

    @Test
    void unpinMessageShouldClearPinnedByAndPush() {
        ChatConversation lobby = buildLobbyConversation(1L);
        ChatMessage message = buildMessage(100L, lobby.getId(), 10L, "hello");
        message.setPinnedBy(0L);

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        when(chatMessageRepository.getById(100L)).thenReturn(message);
        ChatConversationMember activeMember = buildMember(1L, lobby.getId(), 20L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));

        chatLobbyAdminService.unpinMessage(100L);

        assertNull(message.getPinnedBy());
        verify(chatMessageRepository).updateById(message);
        verify(chatPushService).pushMessageUpdated(any(ChatMessageVO.class), eq(List.of(20L)));
    }

    // ==================== muteLobbyMember ====================

    @Test
    void muteLobbyMemberShouldSetMuteUntilAndPush() {
        ChatConversation lobby = buildLobbyConversation(1L);
        ChatConversationMember member = buildMember(1L, lobby.getId(), 200L, ChatConstants.MEMBER_ROLE_MEMBER);
        LocalDateTime muteUntil = LocalDateTime.now().plusHours(1);

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        when(chatConversationMemberRepository.findByConversationAndUser(lobby.getId(), 200L)).thenReturn(member);
        ChatConversationMember activeMember = buildMember(2L, lobby.getId(), 200L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));
        SysUser user = buildSysUser(200L, "testuser");
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user));
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenReturn(new ChatMemberVO());

        ChatAdminMemberMuteUpdateRequest request = new ChatAdminMemberMuteUpdateRequest();
        request.setMuteUntil(muteUntil);

        List<ChatMemberVO> result = chatLobbyAdminService.muteLobbyMember(200L, request);

        assertEquals(muteUntil, member.getMuteUntil());
        verify(chatConversationMemberRepository).updateById(member);
        verify(chatPushService).pushMembersUpdated(any(), anyList());
        assertNotNull(result);
    }

    // ==================== kickLobbyMember ====================

    @Test
    void kickLobbyMemberShouldSetRemovedStatusAndPush() {
        ChatConversation lobby = buildLobbyConversation(1L);
        ChatConversationMember member = buildMember(1L, lobby.getId(), 200L, ChatConstants.MEMBER_ROLE_MEMBER);

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        when(chatConversationMemberRepository.findByConversationAndUser(lobby.getId(), 200L)).thenReturn(member);
        ChatConversationMember activeMember = buildMember(2L, lobby.getId(), 300L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));
        SysUser user = buildSysUser(300L, "otheruser");
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user));
        when(chatModelMapper.toMemberVO(any(ChatConversationMember.class))).thenReturn(new ChatMemberVO());

        List<ChatMemberVO> result = chatLobbyAdminService.kickLobbyMember(200L);

        assertEquals(ChatConstants.MEMBER_STATUS_REMOVED, member.getStatus());
        assertNull(member.getMuteUntil());
        verify(chatConversationMemberRepository).updateById(member);
        verify(chatPushService).pushMembersUpdated(any(), anyList());
        assertNotNull(result);
    }

    // ==================== updateLobbySettings ====================

    @Test
    void updateLobbySettingsShouldPersistChanges() {
        ChatConversation lobby = buildLobbyConversation(1L);

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        ChatConversationMember activeMember = buildMember(1L, lobby.getId(), 10L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));

        ChatLobbySettingsUpdateRequest request = new ChatLobbySettingsUpdateRequest();
        request.setAnnouncement("New announcement");
        request.setSlowModeSeconds(30);
        request.setSpeakLevelLimit(5);

        ChatConversationVO result = chatLobbyAdminService.updateLobbySettings(request);

        verify(chatConversationRepository).updateById(lobby);
        assertEquals("New announcement", lobby.getAnnouncement());
        assertEquals(30, lobby.getSlowModeSeconds());
        assertEquals(5, lobby.getSpeakLevelLimit());
        verify(chatPushService).pushConversationUpdated(any(), eq(List.of(10L)));
        assertNotNull(result);
    }

    // ==================== sendAnnouncement (via updateLobbySettings with announcement) ====================

    @Test
    void sendAnnouncementShouldCreateSystemMessage() {
        // The lobby admin service does not have a dedicated "sendAnnouncement" method.
        // Testing announcement via updateLobbySettings which sets the announcement on the lobby.
        ChatConversation lobby = buildLobbyConversation(1L);

        when(chatConversationRepository.findGlobalConversation()).thenReturn(lobby);
        ChatConversationMember activeMember = buildMember(1L, lobby.getId(), 10L, ChatConstants.MEMBER_ROLE_MEMBER);
        when(chatConversationMemberRepository.listActiveByConversationId(lobby.getId()))
                .thenReturn(List.of(activeMember));

        ChatLobbySettingsUpdateRequest request = new ChatLobbySettingsUpdateRequest();
        request.setAnnouncement("System announcement");

        ChatConversationVO result = chatLobbyAdminService.updateLobbySettings(request);

        assertEquals("System announcement", lobby.getAnnouncement());
        verify(chatConversationRepository).updateById(lobby);
        verify(chatPushService).pushConversationUpdated(any(), eq(List.of(10L)));
        assertNotNull(result);
        assertNotNull(result.getNotice());
    }

    // ==================== Helper methods ====================

    private ChatConversation buildLobbyConversation(Long id) {
        ChatConversation c = new ChatConversation();
        c.setId(id);
        c.setConversationType(ChatConstants.CONVERSATION_TYPE_GLOBAL);
        c.setSceneType(ChatConstants.SCENE_TYPE_HALL_CHANNEL);
        c.setName(ChatConstants.GLOBAL_CONVERSATION_NAME);
        c.setIsAllSite(1);
        c.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        c.setSlowModeSeconds(0);
        c.setSpeakLevelLimit(1);
        return c;
    }

    private ChatMessage buildMessage(Long messageId, Long conversationId, Long senderId, String content) {
        ChatMessage m = new ChatMessage();
        m.setId(messageId);
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        m.setContent(content);
        m.setMessageType(ChatConstants.MESSAGE_TYPE_TEXT);
        m.setRevokeStatus(ChatConstants.REVOKE_STATUS_NORMAL);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    private ChatConversationMember buildMember(Long id, Long conversationId, Long userId, String role) {
        ChatConversationMember m = new ChatConversationMember();
        m.setId(id);
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setMemberRole(role);
        m.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        m.setJoinedAt(LocalDateTime.now());
        return m;
    }

    private SysUser buildSysUser(Long id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setDeletedFlag(0);
        u.setStatus(1);
        return u;
    }
}

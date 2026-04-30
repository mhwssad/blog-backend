package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatMemberHelper;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatPushPayloadBuilder;
import com.cybzacg.blogbackend.module.chat.member.service.impl.ChatGroupManageServiceImpl;
import com.cybzacg.blogbackend.module.chat.shared.support.ChatServiceSupport;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatGroupManageServiceImpl unit tests.
 */
@ExtendWith(MockitoExtension.class)
class ChatGroupManageServiceImplTest {

    @Mock
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private ChatConversationMemberRepository chatConversationMemberRepository;
    @Mock
    private com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRepository chatMessageRepository;
    @Mock
    private com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRecipientRepository chatMessageRecipientRepository;
    @Mock
    private com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageReadCursorRepository chatMessageReadCursorRepository;
    @Mock
    private com.cybzacg.blogbackend.module.auth.repository.SysUserRepository sysUserRepository;
    @Mock
    private ChatModelMapper chatModelMapper;
    @Mock
    private com.cybzacg.blogbackend.module.chat.shared.support.ChatPayloadHelper chatPayloadHelper;
    @Mock
    private ChatMemberHelper chatMemberHelper;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private ChatPushService chatPushService;
    @Mock
    private ChatPushPayloadBuilder chatPushPayloadBuilder;
    @Mock
    private ChatNotificationService chatNotificationService;
    @Mock
    private UserExperienceService userExperienceService;

    private ChatGroupManageServiceImpl chatGroupManageService;

    @BeforeEach
    void setUp() {
        ChatServiceSupport support = new ChatServiceSupport(
                chatConversationRepository,
                chatConversationMemberRepository,
                chatMessageRepository,
                chatMessageRecipientRepository,
                chatMessageReadCursorRepository,
                sysUserRepository,
                chatModelMapper,
                chatPayloadHelper,
                chatMemberHelper,
                sysConfigService
        );
        chatGroupManageService = new ChatGroupManageServiceImpl(
                support,
                chatPushService,
                chatPushPayloadBuilder,
                chatNotificationService,
                userExperienceService
        );
    }

    // ==================== createGroupShouldSucceedWithValidMembers ====================

    @Test
    void createGroupShouldSucceedWithValidMembers() {
        Long ownerId = 1L;
        ChatCreateGroupRequest request = buildValidCreateGroupRequest(List.of(2L, 3L));

        // Permission check: min level 1 means no level check needed
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.CHAT_GROUP_CREATE_MIN_LEVEL_KEY,
                String.valueOf(ConfigConstants.DEFAULT_CHAT_GROUP_CREATE_MIN_LEVEL)
        )).thenReturn("1");

        // Count limit check: allow up to 20 groups
        when(sysConfigService.getValueOrDefault(
                ConfigConstants.CHAT_GROUP_CREATE_MAX_COUNT_KEY,
                String.valueOf(ConfigConstants.DEFAULT_CHAT_GROUP_CREATE_MAX_COUNT)
        )).thenReturn("20");
        when(chatConversationRepository.countNormalGroupsByOwner(ownerId)).thenReturn(0L);

        // Active user check for memberUserIds (owner excluded by normalizeMemberIds)
        SysUser member2 = buildSysUser(2L, "user2");
        SysUser member3 = buildSysUser(3L, "user3");
        when(sysUserRepository.getById(2L)).thenReturn(member2);
        when(sysUserRepository.getById(3L)).thenReturn(member3);

        // Mapper
        ChatConversation mappedConversation = new ChatConversation();
        mappedConversation.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        mappedConversation.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_PRIVATE);
        mappedConversation.setJoinRule(ChatConstants.JOIN_RULE_FREE);
        mappedConversation.setAllowGuestView(0);
        mappedConversation.setRequireJoinToSpeak(1);
        mappedConversation.setSpeakLevelLimit(1);
        mappedConversation.setMemberLimit(0);
        mappedConversation.setSlowModeSeconds(0);
        mappedConversation.setDisplaySort(0);
        when(chatModelMapper.toGroupConversation(request)).thenReturn(mappedConversation);

        // Membership upsert - conversation.getId() is null after save in test context
        when(chatConversationMemberRepository.findByConversationAndUser(any(), any())).thenReturn(null);

        // Mapper: toConversationMember returns a real member since chatModelMapper is a mock
        // (default methods on mock interfaces return null unless stubbed)
        when(chatModelMapper.toConversationMember(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ChatConversationMember member = new ChatConversationMember();
                    member.setConversationId(invocation.getArgument(0));
                    member.setUserId(invocation.getArgument(1));
                    member.setMemberRole(invocation.getArgument(2));
                    member.setJoinSource(invocation.getArgument(3));
                    member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
                    member.setJoinedAt(LocalDateTime.now());
                    return member;
                });

        // Simulate auto-increment ID assignment on save
        doAnswer(invocation -> {
            ChatConversation c = invocation.getArgument(0);
            c.setId(1L);
            return true;
        }).when(chatConversationRepository).save(any(ChatConversation.class));

        // Conversation VO
        ChatConversationListItem detailItem = new ChatConversationListItem();
        detailItem.setId(1L);
        detailItem.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        when(chatConversationRepository.selectConversationDetail(any(), eq(ownerId))).thenReturn(detailItem);
        when(chatConversationMemberRepository.listActiveByConversationId(any())).thenReturn(List.of());
        ChatConversationVO vo = new ChatConversationVO();
        vo.setId(1L);
        when(chatModelMapper.toConversationVO(detailItem)).thenReturn(vo);

        try (var ignored = SecurityTestUtils.mockUserId(ownerId)) {
            ChatConversationVO result = chatGroupManageService.createGroup(ownerId, request);
            assertNotNull(result);
        }

        verify(chatConversationRepository).save(any(ChatConversation.class));
        // Owner + 2 members = 3 saves
        verify(chatConversationMemberRepository, times(3)).save(any(ChatConversationMember.class));
    }

    // ==================== createGroupShouldRejectExceedMemberLimit ====================

    @Test
    void createGroupShouldRejectExceedMemberLimit() {
        Long ownerId = 1L;
        // Request with memberLimit=2 but trying to add 3 members (owner + 3 = 4)
        ChatCreateGroupRequest request = buildValidCreateGroupRequest(List.of(2L, 3L, 4L));
        request.setMemberLimit(2);

        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.CHAT_GROUP_CREATE_MIN_LEVEL_KEY),
                anyString()
        )).thenReturn("1");
        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.CHAT_GROUP_CREATE_MAX_COUNT_KEY),
                anyString()
        )).thenReturn("20");
        when(chatConversationRepository.countNormalGroupsByOwner(ownerId)).thenReturn(0L);

        assertThrows(BusinessException.class, () ->
                chatGroupManageService.createGroup(ownerId, request)
        );

        verify(chatConversationRepository, never()).save(any());
    }

    // ==================== createGroupShouldRejectExceedGroupLimit ====================

    @Test
    void createGroupShouldRejectExceedGroupLimit() {
        Long ownerId = 1L;
        ChatCreateGroupRequest request = buildValidCreateGroupRequest(List.of(2L));

        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.CHAT_GROUP_CREATE_MIN_LEVEL_KEY),
                anyString()
        )).thenReturn("1");
        when(sysConfigService.getValueOrDefault(
                eq(ConfigConstants.CHAT_GROUP_CREATE_MAX_COUNT_KEY),
                anyString()
        )).thenReturn("3");
        // Owner already has 3 groups
        when(chatConversationRepository.countNormalGroupsByOwner(ownerId)).thenReturn(3L);

        assertThrows(BusinessException.class, () ->
                chatGroupManageService.createGroup(ownerId, request)
        );

        verify(chatConversationRepository, never()).save(any());
    }

    // ==================== quitGroupShouldRejectForOwner ====================

    @Test
    void quitGroupShouldRejectForOwner() {
        Long ownerId = 1L;
        Long conversationId = 10L;

        ChatConversation conversation = buildGroupConversation(conversationId, ownerId);
        ChatConversationMember ownerMember = buildMember(1L, conversationId, ownerId, ChatConstants.MEMBER_ROLE_OWNER);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, ownerId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember));

        assertThrows(BusinessException.class, () ->
                chatGroupManageService.leaveGroup(ownerId, conversationId)
        );

        verify(chatConversationMemberRepository, never()).updateById(any());
    }

    // ==================== dismissGroupShouldSetDisabledStatus ====================

    @Test
    void dismissGroupShouldSetDisabledStatus() {
        Long ownerId = 1L;
        Long conversationId = 10L;

        ChatConversation conversation = buildGroupConversation(conversationId, ownerId);
        ChatConversationMember ownerMember = buildMember(1L, conversationId, ownerId, ChatConstants.MEMBER_ROLE_OWNER);
        ChatConversationMember otherMember = buildMember(2L, conversationId, 2L, ChatConstants.MEMBER_ROLE_MEMBER);

        when(chatConversationRepository.getById(conversationId)).thenReturn(conversation);
        when(chatConversationMemberRepository.findByConversationAndUser(conversationId, ownerId)).thenReturn(ownerMember);
        when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                .thenReturn(List.of(ownerMember, otherMember));

        try (var ignored = SecurityTestUtils.mockUserId(ownerId)) {
            chatGroupManageService.dissolveGroup(ownerId, conversationId);
        }

        assertEquals(ChatConstants.CONVERSATION_STATUS_DISSOLVED, conversation.getStatus());
        verify(chatConversationRepository).updateById(conversation);
        verify(chatConversationMemberRepository).removeAllActiveMembers(conversationId);
        verify(chatPushService).pushConversationUpdated(any(), anyList());
    }

    // ==================== Helper methods ====================

    private ChatCreateGroupRequest buildValidCreateGroupRequest(List<Long> memberUserIds) {
        ChatCreateGroupRequest request = new ChatCreateGroupRequest();
        request.setName("Test Group");
        request.setMemberUserIds(memberUserIds);
        request.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_PRIVATE);
        request.setJoinRule(ChatConstants.JOIN_RULE_FREE);
        request.setSpeakLevelLimit(1);
        request.setMemberLimit(0);
        return request;
    }

    private ChatConversation buildGroupConversation(Long id, Long ownerId) {
        ChatConversation c = new ChatConversation();
        c.setId(id);
        c.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        c.setSceneType(ChatConstants.SCENE_TYPE_USER_GROUP);
        c.setIsAllSite(0);
        c.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        c.setOwnerId(ownerId);
        c.setName("Test Group");
        c.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_PRIVATE);
        c.setAllowGuestView(0);
        c.setRequireJoinToSpeak(1);
        c.setJoinRule(ChatConstants.JOIN_RULE_FREE);
        c.setSpeakLevelLimit(1);
        c.setMemberLimit(0);
        c.setSlowModeSeconds(0);
        c.setDisplaySort(0);
        return c;
    }

    private ChatConversationMember buildMember(Long id, Long conversationId, Long userId, String role) {
        ChatConversationMember m = new ChatConversationMember();
        m.setId(id);
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setMemberRole(role);
        m.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        m.setJoinSource(ChatConstants.JOIN_SOURCE_MANUAL);
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

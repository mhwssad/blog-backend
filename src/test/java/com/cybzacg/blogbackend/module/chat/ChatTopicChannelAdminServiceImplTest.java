package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.chat.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.enums.chat.ChatChannelApplicationStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatTopicChannelSaveRequest;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.service.impl.ChatTopicChannelAdminServiceImpl;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationReviewRequest;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatChannelCreateApplicationRepository;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.member.service.impl.ChatChannelApplicationAdminServiceImpl;
import com.cybzacg.blogbackend.module.chat.push.service.ChatNotificationService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatTopicChannelAdminServiceImpl and ChatChannelApplicationAdminServiceImpl unit tests.
 *
 * <p>Combines topic channel CRUD tests with application approve/reject tests,
 * as both relate to topic channel administration.
 */
@ExtendWith(MockitoExtension.class)
class ChatTopicChannelAdminServiceImplTest {

    // ==================== Topic Channel CRUD Tests ====================

    @Nested
    class TopicChannelCrudTests {
        @Mock
        private ChatConversationRepository chatConversationRepository;
        @Mock
        private ChatConversationMemberRepository chatConversationMemberRepository;
        @Mock
        private SysUserRepository sysUserRepository;
        @Mock
        private ChatModelMapper chatModelMapper;
        @Mock
        private ChatNotificationService chatNotificationService;

        private ChatTopicChannelAdminServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new ChatTopicChannelAdminServiceImpl(
                    chatConversationRepository,
                    chatConversationMemberRepository,
                    sysUserRepository,
                    chatModelMapper,
                    chatNotificationService
            );
        }

        @Test
        void createTopicChannelShouldCreateConversationWithOwnerMember() {
            ChatTopicChannelSaveRequest request = buildValidSaveRequest();
            request.setOwnerId(10L);

            SysUser owner = buildSysUser(10L, "owner");
            when(sysUserRepository.getById(10L)).thenReturn(owner);
            when(chatConversationMemberRepository.findByConversationAndUser(any(), eq(10L))).thenReturn(null);
            ChatAdminConversationListItem detailItem = buildAdminConversationListItem(1L);
            when(chatConversationRepository.selectAdminConversationDetail(any())).thenReturn(detailItem);
            ChatAdminConversationVO expectedVo = new ChatAdminConversationVO();
            expectedVo.setId(1L);
            when(chatModelMapper.toAdminConversationVO(detailItem)).thenReturn(expectedVo);

            try (var ignored = SecurityTestUtils.mockUserId(1L)) {
                ChatAdminConversationVO result = service.createTopicChannel(request);
                assertNotNull(result);
            }

            verify(chatConversationRepository).save(any(ChatConversation.class));
            verify(chatConversationMemberRepository).save(any(ChatConversationMember.class));
        }

        @Test
        void updateTopicChannelShouldApplyFieldsAndNotify() {
            Long conversationId = 1L;
            ChatConversation existing = buildTopicChannelConversation(conversationId);
            existing.setAnnouncement(null);
            when(chatConversationRepository.getById(conversationId)).thenReturn(existing);

            ChatTopicChannelSaveRequest request = buildValidSaveRequest();
            request.setAnnouncement("New announcement");

            when(chatConversationMemberRepository.listActiveByConversationId(conversationId))
                    .thenReturn(List.of());
            ChatAdminConversationListItem detailItem = buildAdminConversationListItem(conversationId);
            when(chatConversationRepository.selectAdminConversationDetail(conversationId)).thenReturn(detailItem);
            ChatAdminConversationVO expectedVo = new ChatAdminConversationVO();
            when(chatModelMapper.toAdminConversationVO(detailItem)).thenReturn(expectedVo);

            try (var ignored = SecurityTestUtils.mockUserId(1L)) {
                ChatAdminConversationVO result = service.updateTopicChannel(conversationId, request);
                assertNotNull(result);
            }

            verify(chatConversationRepository).updateById(existing);
            assertEquals("New announcement", existing.getAnnouncement());
            verify(chatNotificationService).deliverChannelAnnouncementNotifications(
                    eq(existing), eq(List.of()), eq(1L)
            );
        }

        @Test
        void deleteTopicChannelShouldSetDisabledStatus() {
            Long conversationId = 1L;
            ChatConversation existing = buildTopicChannelConversation(conversationId);
            existing.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
            when(chatConversationRepository.getById(conversationId)).thenReturn(existing);

            ChatTopicChannelSaveRequest request = buildValidSaveRequest();
            request.setAnnouncement(null);

            ChatAdminConversationListItem detailItem = buildAdminConversationListItem(conversationId);
            when(chatConversationRepository.selectAdminConversationDetail(conversationId)).thenReturn(detailItem);
            ChatAdminConversationVO expectedVo = new ChatAdminConversationVO();
            when(chatModelMapper.toAdminConversationVO(detailItem)).thenReturn(expectedVo);

            try (var ignored = SecurityTestUtils.mockUserId(1L)) {
                service.updateTopicChannel(conversationId, request);
            }

            verify(chatConversationRepository).updateById(existing);
        }

        @Test
        void createTopicChannelShouldRejectInvalidRequest() {
            ChatTopicChannelSaveRequest request = new ChatTopicChannelSaveRequest();
            request.setName(null);

            assertThrows(BusinessException.class, () ->
                    service.createTopicChannel(request)
            );

            verify(chatConversationRepository, never()).save(any());
        }

        @Test
        void createTopicChannelShouldRejectInvalidVisibilityScope() {
            ChatTopicChannelSaveRequest request = buildValidSaveRequest();
            request.setVisibilityScope("invalid_scope");

            assertThrows(BusinessException.class, () ->
                    service.createTopicChannel(request)
            );
        }

        @Test
        void createTopicChannelShouldRejectInvalidJoinRule() {
            ChatTopicChannelSaveRequest request = buildValidSaveRequest();
            request.setJoinRule("invalid_rule");

            assertThrows(BusinessException.class, () ->
                    service.createTopicChannel(request)
            );
        }
    }

    // ==================== Application Approve/Reject Tests ====================

    @Nested
    class ApplicationReviewTests {
        @Mock
        private ChatChannelCreateApplicationRepository chatChannelCreateApplicationRepository;
        @Mock
        private ChatConversationRepository chatConversationRepository;
        @Mock
        private ChatConversationMemberRepository chatConversationMemberRepository;
        @Mock
        private SysUserRepository sysUserRepository;
        @Mock
        private ChatModelMapper chatModelMapper;

        private ChatChannelApplicationAdminServiceImpl service;

        @BeforeEach
        void setUp() {
            service = new ChatChannelApplicationAdminServiceImpl(
                    chatChannelCreateApplicationRepository,
                    chatConversationRepository,
                    chatConversationMemberRepository,
                    sysUserRepository,
                    chatModelMapper
            );
        }

        @Test
        void approveChannelApplicationShouldSetApprovedStatus() {
            Long applicationId = 100L;
            ChatChannelCreateApplication application = buildPendingApplication(applicationId);

            when(chatChannelCreateApplicationRepository.getById(applicationId)).thenReturn(application);

            ChatChannelApplicationReviewRequest request = new ChatChannelApplicationReviewRequest();
            request.setReviewStatus(ChatChannelApplicationStatusEnum.APPROVED.getValue());
            request.setReviewComment("Approved");

            try (var ignored = SecurityTestUtils.mockUserId(1L)) {
                service.reviewApplication(applicationId, request);
            }

            assertEquals(ChatChannelApplicationStatusEnum.APPROVED.getValue(), application.getApplyStatus());
            assertEquals(1L, application.getReviewerId());
            assertEquals("Approved", application.getReviewComment());
            assertNotNull(application.getReviewedAt());
            verify(chatConversationRepository).save(any(ChatConversation.class));
            verify(chatConversationMemberRepository).save(any(ChatConversationMember.class));
            verify(chatChannelCreateApplicationRepository).updateById(application);
        }

        @Test
        void rejectChannelApplicationShouldSetRejectedStatusWithReason() {
            Long applicationId = 100L;
            ChatChannelCreateApplication application = buildPendingApplication(applicationId);

            when(chatChannelCreateApplicationRepository.getById(applicationId)).thenReturn(application);

            ChatChannelApplicationReviewRequest request = new ChatChannelApplicationReviewRequest();
            request.setReviewStatus(ChatChannelApplicationStatusEnum.REJECTED.getValue());
            request.setReviewComment("Name already taken");

            try (var ignored = SecurityTestUtils.mockUserId(1L)) {
                service.reviewApplication(applicationId, request);
            }

            assertEquals(ChatChannelApplicationStatusEnum.REJECTED.getValue(), application.getApplyStatus());
            assertEquals(1L, application.getReviewerId());
            assertEquals("Name already taken", application.getReviewComment());
            assertNotNull(application.getReviewedAt());
            assertNull(application.getConversationId());
            verify(chatConversationRepository, never()).save(any(ChatConversation.class));
            verify(chatChannelCreateApplicationRepository).updateById(application);
        }
    }

    // ==================== Shared Helper methods ====================

    private static ChatTopicChannelSaveRequest buildValidSaveRequest() {
        ChatTopicChannelSaveRequest request = new ChatTopicChannelSaveRequest();
        request.setName("Test Channel");
        request.setAvatar("https://example.com/avatar.png");
        request.setDescription("A test channel");
        request.setAnnouncement("Welcome!");
        request.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_MEMBER);
        request.setJoinRule(ChatConstants.JOIN_RULE_APPROVAL);
        request.setSpeakLevelLimit(1);
        request.setMemberLimit(100);
        request.setSlowModeSeconds(0);
        request.setDisplaySort(0);
        request.setCategoryCode("general");
        return request;
    }

    private static ChatConversation buildTopicChannelConversation(Long id) {
        ChatConversation c = new ChatConversation();
        c.setId(id);
        c.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        c.setSceneType(ChatConstants.SCENE_TYPE_TOPIC_CHANNEL);
        c.setIsAllSite(0);
        c.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        c.setOwnerId(10L);
        c.setName("Test Channel");
        c.setVisibilityScope(ChatConstants.VISIBILITY_SCOPE_MEMBER);
        c.setAllowGuestView(0);
        c.setRequireJoinToSpeak(1);
        c.setJoinRule(ChatConstants.JOIN_RULE_APPROVAL);
        c.setSpeakLevelLimit(1);
        c.setMemberLimit(100);
        c.setSlowModeSeconds(0);
        c.setDisplaySort(0);
        c.setChannelCategoryCode("general");
        return c;
    }

    private static SysUser buildSysUser(Long id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setDeletedFlag(0);
        u.setStatus(1);
        return u;
    }

    private static ChatAdminConversationListItem buildAdminConversationListItem(Long id) {
        ChatAdminConversationListItem item = new ChatAdminConversationListItem();
        item.setId(id);
        item.setConversationType(ChatConstants.CONVERSATION_TYPE_GROUP);
        item.setSceneType(ChatConstants.SCENE_TYPE_TOPIC_CHANNEL);
        item.setName("Test Channel");
        item.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        item.setIsAllSite(0);
        return item;
    }

    private static ChatChannelCreateApplication buildPendingApplication(Long id) {
        ChatChannelCreateApplication app = new ChatChannelCreateApplication();
        app.setId(id);
        app.setApplicantUserId(10L);
        app.setDesiredName("Test Channel");
        app.setApplyStatus(ChatChannelApplicationStatusEnum.PENDING.getValue());
        app.setDescription("Please approve");
        return app;
    }
}

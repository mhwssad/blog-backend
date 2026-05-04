package com.cybzacg.blogbackend.module.auth.notice;

import com.cybzacg.blogbackend.domain.notice.SysNotice;
import com.cybzacg.blogbackend.domain.notice.SysUserNotice;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.notice.service.impl.NotificationDeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NotificationDeliveryServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceImplTest {

    @Mock
    private SysNoticeRepository sysNoticeRepository;
    @Mock
    private SysUserNoticeRepository sysUserNoticeRepository;
    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;

    private NotificationDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new NotificationDeliveryServiceImpl(
                sysNoticeRepository,
                sysUserNoticeRepository,
                userNotificationPreferenceService
        );
    }

    @Test
    void deliverBusinessNoticeShouldSaveMetadataWhenEnabled() {
        Long targetUserId = 1L;
        when(userNotificationPreferenceService.isNotificationEnabled(targetUserId, NotificationTypeEnum.AI_TASK_DONE))
                .thenReturn(true);
        when(sysNoticeRepository.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(100L);
            return true;
        });

        service.deliverAfterCommit(
                targetUserId,
                NotificationTypeEnum.AI_TASK_DONE,
                "Agent 任务完成",
                "writer-agent 任务已完成",
                null,
                "ai_agent_task", 42L,
                "/ai/agents/tasks/42");

        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeRepository).save(noticeCaptor.capture());
        SysNotice saved = noticeCaptor.getValue();
        assertEquals("ai_agent_task", saved.getBusinessType());
        assertEquals(42L, saved.getBusinessId());
        assertEquals("/ai/agents/tasks/42", saved.getActionPath());

        ArgumentCaptor<SysUserNotice> userNoticeCaptor = ArgumentCaptor.forClass(SysUserNotice.class);
        verify(sysUserNoticeRepository).save(userNoticeCaptor.capture());
        assertEquals(targetUserId, userNoticeCaptor.getValue().getUserId());
    }

    @Test
    void deliverBusinessNoticeShouldSkipWhenDisabled() {
        Long targetUserId = 1L;
        when(userNotificationPreferenceService.isNotificationEnabled(targetUserId, NotificationTypeEnum.AI_TASK_DONE))
                .thenReturn(false);

        service.deliverAfterCommit(
                targetUserId,
                NotificationTypeEnum.AI_TASK_DONE,
                "Agent 任务完成",
                "任务已完成",
                null,
                "ai_agent_task", 42L,
                "/ai/agents/tasks/42");

        verify(sysNoticeRepository, never()).save(any());
        verify(sysUserNoticeRepository, never()).save(any());
    }

    @Test
    void deliverShouldSkipWhenTargetUserIdIsNull() {
        service.deliverAfterCommit(
                null,
                NotificationTypeEnum.AI_TASK_DONE,
                "标题",
                "内容",
                null,
                "ai_agent_task", 42L,
                "/ai/agents/tasks/42");

        verifyNoInteractions(sysNoticeRepository, sysUserNoticeRepository);
    }

    @Test
    void deliverShouldSkipWhenNotificationTypeIsNull() {
        service.deliverAfterCommit(
                1L,
                null,
                "标题",
                "内容",
                null,
                "ai_agent_task", 42L,
                "/ai/agents/tasks/42");

        verifyNoInteractions(sysNoticeRepository, sysUserNoticeRepository);
    }

    @Test
    void deliverShouldSkipWhenTitleIsBlank() {
        service.deliverAfterCommit(
                1L,
                NotificationTypeEnum.AI_TASK_DONE,
                "  ",
                "内容",
                null,
                "ai_agent_task", 42L,
                "/ai/agents/tasks/42");

        verifyNoInteractions(sysNoticeRepository, sysUserNoticeRepository);
    }

    @Test
    void legacyMethodShouldDelegateWithoutMetadata() {
        Long targetUserId = 1L;
        when(userNotificationPreferenceService.isNotificationEnabled(targetUserId, NotificationTypeEnum.COMMENT_ME))
                .thenReturn(true);
        when(sysNoticeRepository.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(200L);
            return true;
        });

        service.deliverAfterCommit(targetUserId, NotificationTypeEnum.COMMENT_ME, "评论通知", "有人评论了你", 2L);

        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        verify(sysNoticeRepository).save(noticeCaptor.capture());
        SysNotice saved = noticeCaptor.getValue();
        assertNull(saved.getBusinessType());
        assertNull(saved.getBusinessId());
        assertNull(saved.getActionPath());
    }
}

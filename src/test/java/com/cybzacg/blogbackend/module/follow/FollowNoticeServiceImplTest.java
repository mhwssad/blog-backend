package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.follow.service.impl.FollowNoticeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowNoticeServiceImplTest {
    @Mock
    private SysNoticeRepository sysNoticeRepository;
    @Mock
    private SysUserNoticeRepository sysUserNoticeRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;

    private FollowNoticeServiceImpl followNoticeService;

    @BeforeEach
    void setUp() {
        followNoticeService = new FollowNoticeServiceImpl(sysNoticeRepository, sysUserNoticeRepository, sysUserRepository, userNotificationPreferenceService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void notifyNewFollowerAfterCommitShouldRegisterCallbackInsideTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        when(userNotificationPreferenceService.isNotificationEnabled(any(), any())).thenReturn(true);
        when(sysUserRepository.getById(7L)).thenReturn(activeFollower(7L));
        when(sysNoticeRepository.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(100L);
            return true;
        });
        when(sysUserNoticeRepository.save(any(SysUserNotice.class))).thenReturn(true);

        followNoticeService.notifyNewFollowerAfterCommit(12L, 7L);

        verify(sysNoticeRepository, never()).save(any(SysNotice.class));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.get(0).afterCommit();

        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        ArgumentCaptor<SysUserNotice> relationCaptor = ArgumentCaptor.forClass(SysUserNotice.class);
        verify(sysNoticeRepository).save(noticeCaptor.capture());
        verify(sysUserNoticeRepository).save(relationCaptor.capture());
        assertEquals("你收到了一位新粉丝", noticeCaptor.getValue().getTitle());
        assertEquals(NoticeConstants.TARGET_SPECIFIED, noticeCaptor.getValue().getTargetType());
        assertEquals("12", noticeCaptor.getValue().getTargetUserIds());
        assertEquals(100L, relationCaptor.getValue().getNoticeId());
        assertEquals(12L, relationCaptor.getValue().getUserId());
        assertEquals(NoticeConstants.READ_UNREAD, relationCaptor.getValue().getIsRead());
    }

    @Test
    void notifyNewFollowerAfterCommitShouldRunImmediatelyWithoutTransaction() {
        when(userNotificationPreferenceService.isNotificationEnabled(any(), any())).thenReturn(true);
        when(sysUserRepository.getById(7L)).thenReturn(activeFollower(7L));
        when(sysNoticeRepository.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(101L);
            return true;
        });
        when(sysUserNoticeRepository.save(any(SysUserNotice.class))).thenReturn(true);

        followNoticeService.notifyNewFollowerAfterCommit(12L, 7L);

        verify(sysNoticeRepository).save(any(SysNotice.class));
        verify(sysUserNoticeRepository).save(any(SysUserNotice.class));
    }

    private SysUser activeFollower(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("follower");
        user.setNickname("新粉丝");
        user.setDeletedFlag(0);
        return user;
    }
}
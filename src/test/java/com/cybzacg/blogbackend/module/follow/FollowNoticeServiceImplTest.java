package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowNoticeServiceImplTest {
    @Mock
    private SysNoticeService sysNoticeService;
    @Mock
    private SysUserNoticeService sysUserNoticeService;
    @Mock
    private SysUserService sysUserService;

    private FollowNoticeServiceImpl followNoticeService;

    @BeforeEach
    void setUp() {
        followNoticeService = new FollowNoticeServiceImpl(sysNoticeService, sysUserNoticeService, sysUserService);
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
        when(sysUserService.getById(7L)).thenReturn(activeFollower(7L));
        when(sysNoticeService.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(100L);
            return true;
        });
        when(sysUserNoticeService.save(any(SysUserNotice.class))).thenReturn(true);

        followNoticeService.notifyNewFollowerAfterCommit(12L, 7L);

        verify(sysNoticeService, never()).save(any(SysNotice.class));
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.get(0).afterCommit();

        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        ArgumentCaptor<SysUserNotice> relationCaptor = ArgumentCaptor.forClass(SysUserNotice.class);
        verify(sysNoticeService).save(noticeCaptor.capture());
        verify(sysUserNoticeService).save(relationCaptor.capture());
        assertEquals("你收到了一位新粉丝", noticeCaptor.getValue().getTitle());
        assertEquals(NoticeConstants.TARGET_SPECIFIED, noticeCaptor.getValue().getTargetType());
        assertEquals("12", noticeCaptor.getValue().getTargetUserIds());
        assertEquals(100L, relationCaptor.getValue().getNoticeId());
        assertEquals(12L, relationCaptor.getValue().getUserId());
        assertEquals(NoticeConstants.READ_UNREAD, relationCaptor.getValue().getIsRead());
    }

    @Test
    void notifyNewFollowerAfterCommitShouldRunImmediatelyWithoutTransaction() {
        when(sysUserService.getById(7L)).thenReturn(activeFollower(7L));
        when(sysNoticeService.save(any(SysNotice.class))).thenAnswer(invocation -> {
            SysNotice notice = invocation.getArgument(0);
            notice.setId(101L);
            return true;
        });
        when(sysUserNoticeService.save(any(SysUserNotice.class))).thenReturn(true);

        followNoticeService.notifyNewFollowerAfterCommit(12L, 7L);

        verify(sysNoticeService).save(any(SysNotice.class));
        verify(sysUserNoticeService).save(any(SysUserNotice.class));
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

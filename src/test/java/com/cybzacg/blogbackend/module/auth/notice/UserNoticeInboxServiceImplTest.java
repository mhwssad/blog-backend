package com.cybzacg.blogbackend.module.auth.notice;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.notice.service.impl.SysNoticeFactory;
import com.cybzacg.blogbackend.module.auth.notice.service.impl.UserNoticeInboxServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;

@ExtendWith(MockitoExtension.class)
class UserNoticeInboxServiceImplTest {
    @Mock
    private SysNoticeRepository sysNoticeRepository;
    @Mock
    private SysUserNoticeRepository sysUserNoticeRepository;
    @Mock
    private SysNoticeModelMapper sysNoticeModelMapper;
    @Mock
    private SysNoticeFactory sysNoticeFactory;
    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;

    private UserNoticeInboxServiceImpl userNoticeInboxService;

    @BeforeEach
    void setUp() {
        userNoticeInboxService = new UserNoticeInboxServiceImpl(
                sysNoticeRepository,
                sysUserNoticeRepository,
                sysNoticeModelMapper,
                sysNoticeFactory,
                userNotificationPreferenceService
        );
    }

    @Test
    void markAllReadShouldUpdateUnreadRelationsAndCreateGlobalReadRecords() {
        SysUserNotice unreadTargetRelation = new SysUserNotice();
        unreadTargetRelation.setId(1L);
        unreadTargetRelation.setNoticeId(200L);
        unreadTargetRelation.setUserId(7L);
        unreadTargetRelation.setIsRead(NoticeConstants.READ_UNREAD);
        unreadTargetRelation.setIsDeleted(0);

        SysUserNotice readGlobalRelation = new SysUserNotice();
        readGlobalRelation.setId(2L);
        readGlobalRelation.setNoticeId(100L);
        readGlobalRelation.setUserId(7L);
        readGlobalRelation.setIsRead(NoticeConstants.READ_READ);
        readGlobalRelation.setIsDeleted(0);

        SysNotice unreadGlobalNotice = new SysNotice();
        unreadGlobalNotice.setId(300L);

        when(userNotificationPreferenceService.isNotificationEnabled(any(), eq(NotificationTypeEnum.SYSTEM_ANNOUNCEMENT))).thenReturn(true);
        when(sysUserNoticeRepository.findByUserId(7L)).thenReturn(List.of(unreadTargetRelation, readGlobalRelation));
        when(sysNoticeRepository.findGlobalUnread(anyCollection())).thenReturn(List.of(unreadGlobalNotice));
        when(sysUserNoticeRepository.findLatestByNoticeIdAndUserId(300L, 7L)).thenReturn(Optional.empty());
        when(sysNoticeFactory.createReadRecord(any(), any(), any())).thenAnswer(inv -> {
            SysUserNotice record = new SysUserNotice();
            record.setNoticeId(inv.getArgument(0));
            record.setUserId(inv.getArgument(1));
            record.setIsRead(NoticeConstants.READ_READ);
            record.setReadTime(inv.getArgument(2));
            record.setCreateTime(inv.getArgument(2));
            record.setUpdateTime(inv.getArgument(2));
            record.setIsDeleted(0);
            return record;
        });
        when(sysUserNoticeRepository.save(any(SysUserNotice.class))).thenAnswer(invocation -> {
            SysUserNotice relation = invocation.getArgument(0);
            relation.setId(3L);
            return true;
        });

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userNoticeInboxService.markAllRead();
        }

        assertEquals(NoticeConstants.READ_READ, unreadTargetRelation.getIsRead());
        assertNotNull(unreadTargetRelation.getReadTime());
        assertNotNull(unreadTargetRelation.getUpdateTime());
        verify(sysUserNoticeRepository).updateById(unreadTargetRelation);

        ArgumentCaptor<SysUserNotice> relationCaptor = ArgumentCaptor.forClass(SysUserNotice.class);
        verify(sysUserNoticeRepository).save(relationCaptor.capture());
        SysUserNotice createdRelation = relationCaptor.getValue();
        assertEquals(Long.valueOf(300L), createdRelation.getNoticeId());
        assertEquals(Long.valueOf(7L), createdRelation.getUserId());
        assertEquals(NoticeConstants.READ_READ, createdRelation.getIsRead());
        assertEquals(Integer.valueOf(0), createdRelation.getIsDeleted());
        assertNotNull(createdRelation.getReadTime());
        assertNotNull(createdRelation.getCreateTime());
        assertNotNull(createdRelation.getUpdateTime());
    }

    @Test
    void getMyNoticeShouldRejectSpecifiedNoticeOutsideInbox() {
        SysNotice notice = new SysNotice();
        notice.setId(99L);
        notice.setTargetType(NoticeConstants.TARGET_SPECIFIED);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setIsDeleted(0);

        when(sysNoticeRepository.getById(99L)).thenReturn(notice);
        when(sysUserNoticeRepository.existsByNoticeIdAndUserId(99L, 7L)).thenReturn(false);

        BusinessException exception;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userNoticeInboxService.getMyNotice(99L));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void countUnreadNoticesShouldMergeGlobalAndTargetedUnreadResults() {
        SysUserNotice readGlobalRelation = new SysUserNotice();
        readGlobalRelation.setNoticeId(100L);
        readGlobalRelation.setUserId(7L);
        readGlobalRelation.setIsRead(NoticeConstants.READ_READ);
        readGlobalRelation.setIsDeleted(0);

        SysUserNotice unreadTargetRelation = new SysUserNotice();
        unreadTargetRelation.setNoticeId(200L);
        unreadTargetRelation.setUserId(7L);
        unreadTargetRelation.setIsRead(NoticeConstants.READ_UNREAD);
        unreadTargetRelation.setIsDeleted(0);

        when(sysUserNoticeRepository.findByUserId(7L)).thenReturn(List.of(readGlobalRelation, unreadTargetRelation));
        when(userNotificationPreferenceService.isNotificationEnabled(any(), eq(NotificationTypeEnum.SYSTEM_ANNOUNCEMENT))).thenReturn(true);
        when(sysNoticeRepository.countGlobalUnread(List.of(100L))).thenReturn(2L);
        when(sysNoticeRepository.countTargetedUnread(List.of(200L))).thenReturn(1L);

        long unreadCount;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            unreadCount = userNoticeInboxService.countUnreadNotices();
        }

        assertEquals(3L, unreadCount);
    }

    @Test
    void markAllReadShouldRecoverWhenGlobalNoticeRelationInsertedConcurrently() {
        SysNotice unreadGlobalNotice = new SysNotice();
        unreadGlobalNotice.setId(300L);

        SysUserNotice existingReadRelation = new SysUserNotice();
        existingReadRelation.setId(5L);
        existingReadRelation.setNoticeId(300L);
        existingReadRelation.setUserId(7L);
        existingReadRelation.setIsRead(NoticeConstants.READ_READ);
        existingReadRelation.setIsDeleted(0);

        when(sysUserNoticeRepository.findByUserId(7L)).thenReturn(List.of());
        when(userNotificationPreferenceService.isNotificationEnabled(any(), eq(NotificationTypeEnum.SYSTEM_ANNOUNCEMENT))).thenReturn(true);
        when(sysNoticeRepository.findGlobalUnread(anyCollection())).thenReturn(List.of(unreadGlobalNotice));
        when(sysUserNoticeRepository.findLatestByNoticeIdAndUserId(300L, 7L)).thenReturn(Optional.empty(), Optional.of(existingReadRelation));
        when(sysNoticeFactory.createReadRecord(any(), any(), any())).thenAnswer(inv -> {
            SysUserNotice record = new SysUserNotice();
            record.setNoticeId(inv.getArgument(0));
            record.setUserId(inv.getArgument(1));
            record.setIsRead(NoticeConstants.READ_READ);
            record.setIsDeleted(0);
            return record;
        });
        when(sysUserNoticeRepository.save(any(SysUserNotice.class)))
                .thenThrow(new DuplicateKeyException("uk_notice_user"));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userNoticeInboxService.markAllRead();
        }

        verify(sysUserNoticeRepository).save(any(SysUserNotice.class));
        verify(sysUserNoticeRepository, never()).updateById(existingReadRelation);
    }
}

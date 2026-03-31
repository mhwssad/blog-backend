package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserNoticeService;
import com.cybzacg.blogbackend.module.auth.service.impl.UserNoticeInboxServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserNoticeInboxServiceImplTest {
    @Mock
    private SysNoticeService sysNoticeService;
    @Mock
    private SysUserNoticeService sysUserNoticeService;
    @Mock
    private SysNoticeModelMapper sysNoticeModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysUserNotice> userNoticeQuery;
    @Mock
    private LambdaQueryChainWrapper<SysNotice> noticeQuery;
    @Mock
    private LambdaQueryChainWrapper<SysUserNotice> accessQuery;
    @Mock
    private LambdaQueryChainWrapper<SysUserNotice> activeRelationQuery;
    @Mock
    private LambdaQueryChainWrapper<SysNotice> globalUnreadQuery;
    @Mock
    private LambdaQueryChainWrapper<SysNotice> targetedUnreadQuery;

    private UserNoticeInboxServiceImpl userNoticeInboxService;

    @BeforeEach
    void setUp() {
        userNoticeInboxService = new UserNoticeInboxServiceImpl(
                sysNoticeService,
                sysUserNoticeService,
                sysNoticeModelMapper
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

        when(sysUserNoticeService.lambdaQuery()).thenReturn(userNoticeQuery, activeRelationQuery);
        when(userNoticeQuery.eq(anySFunction(), any())).thenReturn(userNoticeQuery);
        when(userNoticeQuery.list()).thenReturn(List.of(unreadTargetRelation, readGlobalRelation));
        when(sysUserNoticeService.updateById(unreadTargetRelation)).thenReturn(true);

        when(activeRelationQuery.eq(anySFunction(), any())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.orderByDesc(anySFunction())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.last(any())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.one()).thenReturn(null);
        when(sysUserNoticeService.save(any(SysUserNotice.class))).thenAnswer(invocation -> {
            SysUserNotice relation = invocation.getArgument(0);
            relation.setId(3L);
            return true;
        });

        when(sysNoticeService.lambdaQuery()).thenReturn(noticeQuery);
        when(noticeQuery.eq(anySFunction(), any())).thenReturn(noticeQuery);
        when(noticeQuery.notIn(anyBoolean(), anySFunction(), anyCollection())).thenReturn(noticeQuery);
        when(noticeQuery.list()).thenReturn(List.of(unreadGlobalNotice));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userNoticeInboxService.markAllRead();
        }

        assertEquals(NoticeConstants.READ_READ, unreadTargetRelation.getIsRead());
        assertNotNull(unreadTargetRelation.getReadTime());
        assertNotNull(unreadTargetRelation.getUpdateTime());
        verify(sysUserNoticeService).updateById(unreadTargetRelation);

        ArgumentCaptor<SysUserNotice> relationCaptor = ArgumentCaptor.forClass(SysUserNotice.class);
        verify(sysUserNoticeService).save(relationCaptor.capture());
        SysUserNotice createdRelation = relationCaptor.getValue();
        assertEquals(Long.valueOf(300L), createdRelation.getNoticeId());
        assertEquals(Long.valueOf(7L), createdRelation.getUserId());
        assertEquals(NoticeConstants.READ_READ, createdRelation.getIsRead());
        assertEquals(Integer.valueOf(0), createdRelation.getIsDeleted());
        assertNotNull(createdRelation.getReadTime());
        assertNotNull(createdRelation.getCreateTime());
        assertNotNull(createdRelation.getUpdateTime());
        verify(sysUserNoticeService, never()).saveBatch(anyCollection());
    }

    @Test
    void getMyNoticeShouldRejectSpecifiedNoticeOutsideInbox() {
        SysNotice notice = new SysNotice();
        notice.setId(99L);
        notice.setTargetType(NoticeConstants.TARGET_SPECIFIED);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setIsDeleted(0);

        when(sysNoticeService.getById(99L)).thenReturn(notice);
        when(sysUserNoticeService.lambdaQuery()).thenReturn(accessQuery);
        when(accessQuery.eq(anySFunction(), any())).thenReturn(accessQuery);
        when(accessQuery.exists()).thenReturn(false);

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

        when(sysUserNoticeService.lambdaQuery()).thenReturn(userNoticeQuery);
        when(userNoticeQuery.eq(anySFunction(), any())).thenReturn(userNoticeQuery);
        when(userNoticeQuery.list()).thenReturn(List.of(readGlobalRelation, unreadTargetRelation));

        when(sysNoticeService.lambdaQuery()).thenReturn(globalUnreadQuery, targetedUnreadQuery);
        when(globalUnreadQuery.eq(anySFunction(), any())).thenReturn(globalUnreadQuery);
        when(globalUnreadQuery.notIn(anyBoolean(), anySFunction(), anyCollection())).thenReturn(globalUnreadQuery);
        when(globalUnreadQuery.count()).thenReturn(2L);

        when(targetedUnreadQuery.eq(anySFunction(), any())).thenReturn(targetedUnreadQuery);
        when(targetedUnreadQuery.in(anySFunction(), anyCollection())).thenReturn(targetedUnreadQuery);
        when(targetedUnreadQuery.count()).thenReturn(1L);

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

        when(sysUserNoticeService.lambdaQuery()).thenReturn(userNoticeQuery, activeRelationQuery, activeRelationQuery);
        when(userNoticeQuery.eq(anySFunction(), any())).thenReturn(userNoticeQuery);
        when(userNoticeQuery.list()).thenReturn(List.of());

        when(sysNoticeService.lambdaQuery()).thenReturn(noticeQuery);
        when(noticeQuery.eq(anySFunction(), any())).thenReturn(noticeQuery);
        when(noticeQuery.notIn(anyBoolean(), anySFunction(), anyCollection())).thenReturn(noticeQuery);
        when(noticeQuery.list()).thenReturn(List.of(unreadGlobalNotice));

        when(activeRelationQuery.eq(anySFunction(), any())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.orderByDesc(anySFunction())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.last(any())).thenReturn(activeRelationQuery);
        when(activeRelationQuery.one()).thenReturn(null, existingReadRelation);
        when(sysUserNoticeService.save(any(SysUserNotice.class))).thenThrow(new DuplicateKeyException("uk_notice_user"));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userNoticeInboxService.markAllRead();
        }

        verify(sysUserNoticeService).save(any(SysUserNotice.class));
        verify(sysUserNoticeService, never()).updateById(existingReadRelation);
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}

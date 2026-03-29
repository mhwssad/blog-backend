package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserNoticeService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysNoticeAdminServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysNoticeAdminServiceImplTest {
    @Mock
    private SysNoticeService sysNoticeService;
    @Mock
    private SysUserNoticeService sysUserNoticeService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysNoticeModelMapper sysNoticeModelMapper;
    @Mock
    private LambdaUpdateChainWrapper<SysUserNotice> userNoticeUpdate;

    private SysNoticeAdminServiceImpl sysNoticeAdminService;

    @BeforeEach
    void setUp() {
        sysNoticeAdminService = new SysNoticeAdminServiceImpl(
                sysNoticeService,
                sysUserNoticeService,
                sysUserService,
                sysNoticeModelMapper
        );
    }

    @Test
    void publishNoticeShouldCreateUnreadDeliveryRecordsForSpecifiedUsers() {
        SysNotice notice = new SysNotice();
        notice.setId(10L);
        notice.setTargetType(NoticeConstants.TARGET_SPECIFIED);
        notice.setTargetUserIds("7,9");
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_DRAFT);
        notice.setIsDeleted(0);

        when(sysNoticeService.getById(10L)).thenReturn(notice);
        when(sysNoticeService.updateById(notice)).thenReturn(true);
        when(sysUserNoticeService.lambdaUpdate()).thenReturn(userNoticeUpdate);
        when(userNoticeUpdate.eq(anySFunction(), any())).thenReturn(userNoticeUpdate);
        when(userNoticeUpdate.remove()).thenReturn(true);
        when(sysNoticeModelMapper.toIdList("7,9")).thenReturn(List.of(7L, 9L));
        when(sysUserNoticeService.saveBatch(anyCollection())).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(99L)) {
            sysNoticeAdminService.publishNotice(10L);
        }

        assertEquals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus());
        assertEquals(Long.valueOf(99L), notice.getPublisherId());
        assertNotNull(notice.getPublishTime());
        assertNull(notice.getRevokeTime());

        ArgumentCaptor<Collection<SysUserNotice>> relationCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(sysNoticeService).updateById(notice);
        verify(userNoticeUpdate).remove();
        verify(sysUserNoticeService).saveBatch(relationCaptor.capture());

        List<SysUserNotice> relations = relationCaptor.getValue().stream().toList();
        assertEquals(2, relations.size());
        assertEquals(List.of(7L, 9L), relations.stream().map(SysUserNotice::getUserId).toList());
        relations.forEach(relation -> {
            assertEquals(notice.getId(), relation.getNoticeId());
            assertEquals(NoticeConstants.READ_UNREAD, relation.getIsRead());
            assertNull(relation.getReadTime());
            assertEquals(Integer.valueOf(0), relation.getIsDeleted());
            assertNotNull(relation.getCreateTime());
            assertNotNull(relation.getUpdateTime());
        });
    }

    @Test
    void publishNoticeShouldSkipBatchDeliveryForGlobalNotice() {
        SysNotice notice = new SysNotice();
        notice.setId(11L);
        notice.setTargetType(NoticeConstants.TARGET_ALL);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_DRAFT);
        notice.setIsDeleted(0);

        when(sysNoticeService.getById(11L)).thenReturn(notice);
        when(sysNoticeService.updateById(notice)).thenReturn(true);
        when(sysUserNoticeService.lambdaUpdate()).thenReturn(userNoticeUpdate);
        when(userNoticeUpdate.eq(anySFunction(), any())).thenReturn(userNoticeUpdate);
        when(userNoticeUpdate.remove()).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(77L)) {
            sysNoticeAdminService.publishNotice(11L);
        }

        assertEquals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus());
        assertEquals(Long.valueOf(77L), notice.getPublisherId());
        verify(userNoticeUpdate).remove();
        verify(sysUserNoticeService, never()).saveBatch(anyCollection());
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}

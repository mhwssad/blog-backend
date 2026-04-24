package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.module.auth.convert.SysNoticeModelMapper;
import com.cybzacg.blogbackend.module.auth.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysNoticeAdminServiceImplTest {
    @Mock
    private SysNoticeRepository sysNoticeRepository;
    @Mock
    private SysUserNoticeRepository sysUserNoticeRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysNoticeModelMapper sysNoticeModelMapper;

    private SysNoticeAdminServiceImpl sysNoticeAdminService;

    @BeforeEach
    void setUp() {
        sysNoticeAdminService = new SysNoticeAdminServiceImpl(
                sysNoticeRepository,
                sysUserNoticeRepository,
                sysUserRepository,
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

        when(sysNoticeRepository.getById(10L)).thenReturn(notice);
        when(sysNoticeModelMapper.toIdList("7,9")).thenReturn(List.of(7L, 9L));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(99L)) {
            sysNoticeAdminService.publishNotice(10L);
        }

        assertEquals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus());
        assertEquals(Long.valueOf(99L), notice.getPublisherId());
        assertNotNull(notice.getPublishTime());
        assertNull(notice.getRevokeTime());

        ArgumentCaptor<Collection<SysUserNotice>> relationCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(sysNoticeRepository).updateById(notice);
        verify(sysUserNoticeRepository).deleteByNoticeId(10L);
        verify(sysUserNoticeRepository).saveBatch(relationCaptor.capture());

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

        when(sysNoticeRepository.getById(11L)).thenReturn(notice);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(77L)) {
            sysNoticeAdminService.publishNotice(11L);
        }

        assertEquals(NoticeConstants.PUBLISH_STATUS_PUBLISHED, notice.getPublishStatus());
        assertEquals(Long.valueOf(77L), notice.getPublisherId());
        verify(sysNoticeRepository).updateById(notice);
        verify(sysUserNoticeRepository).deleteByNoticeId(11L);
        verify(sysUserNoticeRepository, never()).saveBatch(anyCollection());
    }
}

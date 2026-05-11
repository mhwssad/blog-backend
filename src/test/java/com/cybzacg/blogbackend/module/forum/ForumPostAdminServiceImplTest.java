package com.cybzacg.blogbackend.module.forum;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminDetailVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminVO;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.service.impl.ForumPostAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumPostAdminServiceImplTest {
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private ForumSectionRepository forumSectionRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ForumModelConvert forumModelConvert;
    @Mock
    private SysAuditLogService sysAuditLogService;
    @Mock
    private NotificationDeliveryService notificationDeliveryService;
    @Mock
    private ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ForumPostAdminServiceImpl forumPostAdminService;

    @BeforeEach
    void setUp() {
        forumPostAdminService = new ForumPostAdminServiceImpl(
                forumPostRepository,
                forumSectionRepository,
                sysUserRepository,
                forumModelConvert,
                sysAuditLogService,
                notificationDeliveryService,
                forumPostChannelLinkRepository,
                eventPublisher
        );
    }

    @Test
    void pagePostsShouldNormalizeQueryAndEnrichRecords() {
        ForumPostAdminPageQuery query = new ForumPostAdminPageQuery();
        query.setCurrent(0L);
        query.setSize(200L);
        query.setKeyword("  java  ");
        ForumPost post = post(20L, ForumPostStatusEnum.PUBLISHED.getValue());
        Page<ForumPost> page = new Page<>(1, 100);
        page.setTotal(1);
        page.setRecords(List.of(post));
        ForumPostAdminVO vo = postVO(20L);
        when(forumPostRepository.pageAdminPosts(query)).thenReturn(page);
        when(forumModelConvert.toPostAdminVO(post)).thenReturn(vo);
        when(forumSectionRepository.listByIds(anyCollection())).thenReturn(List.of(section(10L)));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(7L)));

        PageResult<ForumPostAdminVO> result = forumPostAdminService.pagePosts(query);

        assertEquals(1L, result.getTotal());
        assertEquals(100L, result.getSize());
        assertEquals("java", query.getKeyword());
        assertEquals("论坛版块", result.getRecords().get(0).getSectionName());
        assertEquals("作者", result.getRecords().get(0).getAuthorName());
        assertEquals("已发布", result.getRecords().get(0).getStatusName());
    }

    @Test
    void getPostShouldReturnEnrichedDetail() {
        ForumPost post = post(20L, ForumPostStatusEnum.PUBLISHED.getValue());
        ForumPostAdminDetailVO vo = new ForumPostAdminDetailVO();
        vo.setId(20L);
        vo.setSectionId(10L);
        vo.setAuthorId(7L);
        vo.setStatus(ForumPostStatusEnum.PUBLISHED.getValue());
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumModelConvert.toPostAdminDetailVO(post)).thenReturn(vo);
        when(forumSectionRepository.listByIds(anyCollection())).thenReturn(List.of(section(10L)));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(7L)));

        ForumPostAdminDetailVO result = forumPostAdminService.getPost(20L);

        assertEquals("论坛版块", result.getSectionName());
        assertEquals("作者", result.getAuthorName());
        assertEquals("已发布", result.getStatusName());
    }

    @Test
    void hidePostShouldUpdateStatusInvalidateChannelLinkAuditAndPublishEvent() {
        ForumPost post = post(20L, ForumPostStatusEnum.PUBLISHED.getValue());
        when(forumPostRepository.getById(20L)).thenReturn(post);

        forumPostAdminService.hidePost(20L, 99L, "127.0.0.1", "ua");

        verify(forumPostRepository).updateStatusById(20L, ForumPostStatusEnum.HIDDEN.getValue());
        verify(forumPostChannelLinkRepository).updateStatusByForumPostId(20L, 0);
        verifyAudit(SysAuditOperationType.HIDE_FORUM_POST, "forum_post", 20L);
        verify(eventPublisher).publishEvent(any(ContentChangeEvent.class));
    }

    @Test
    void hidePostShouldRejectDeletedPost() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.DELETED.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostAdminService.hidePost(20L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumPostRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void restorePostShouldOnlyAllowHiddenPost() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.PUBLISHED.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostAdminService.restorePost(20L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumPostRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void restorePostShouldPublishAndReactivateChannelLink() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.HIDDEN.getValue()));

        forumPostAdminService.restorePost(20L, 99L, "127.0.0.1", "ua");

        verify(forumPostRepository).updateStatusById(20L, ForumPostStatusEnum.PUBLISHED.getValue());
        verify(forumPostChannelLinkRepository).updateStatusByForumPostId(20L, 1);
        verifyAudit(SysAuditOperationType.RESTORE_FORUM_POST, "forum_post", 20L);
        verify(eventPublisher).publishEvent(any(ContentChangeEvent.class));
    }

    @Test
    void deletePostShouldRejectAlreadyDeletedPost() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.DELETED.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostAdminService.deletePost(20L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumPostRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void deletePostShouldUpdateStatusInvalidateChannelLinkAuditAndPublishEvent() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.PUBLISHED.getValue()));

        forumPostAdminService.deletePost(20L, 99L, "127.0.0.1", "ua");

        verify(forumPostRepository).updateStatusById(20L, ForumPostStatusEnum.DELETED.getValue());
        verify(forumPostChannelLinkRepository).updateStatusByForumPostId(20L, 0);
        verifyAudit(SysAuditOperationType.DELETE_FORUM_POST, "forum_post", 20L);
        verify(eventPublisher).publishEvent(any(ContentChangeEvent.class));
    }

    @Test
    void toggleTopShouldUpdateFlagAndAudit() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.PUBLISHED.getValue()));

        forumPostAdminService.toggleTop(20L, true, 99L, "127.0.0.1", "ua");

        verify(forumPostRepository).updateTopById(20L, 1);
        verifyAudit(SysAuditOperationType.TOGGLE_FORUM_POST_PIN, "forum_post", 20L);
    }

    @Test
    void toggleEssenceShouldNotifyAuthorWhenEnabled() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L, ForumPostStatusEnum.PUBLISHED.getValue()));

        forumPostAdminService.toggleEssence(20L, true, 99L, "127.0.0.1", "ua");

        verify(forumPostRepository).updateEssenceById(20L, 1);
        verifyAudit(SysAuditOperationType.TOGGLE_FORUM_POST_ESSENCE, "forum_post", 20L);
        verify(notificationDeliveryService).deliverAfterCommit(
                7L,
                NotificationTypeEnum.FORUM_POST_ESSENCE,
                "帖子被设为精华",
                "您的帖子「标题」已被管理员设为精华",
                99L,
                "forum_post", 20L, "/forum/posts/20"
        );
    }

    private void verifyAudit(SysAuditOperationType operationType, String targetType, Long targetId) {
        ArgumentCaptor<SysAuditLogCreateRequest> captor = ArgumentCaptor.forClass(SysAuditLogCreateRequest.class);
        verify(sysAuditLogService).record(captor.capture());
        assertEquals(operationType.getCode(), captor.getValue().getOperationType());
        assertEquals(targetType, captor.getValue().getTargetTypeName());
        assertEquals(targetId, captor.getValue().getTargetId());
        assertEquals(99L, captor.getValue().getOperatorUserId());
        assertEquals(7L, captor.getValue().getTargetUserId());
    }

    private ForumPost post(Long id, Integer status) {
        ForumPost post = new ForumPost();
        post.setId(id);
        post.setSectionId(10L);
        post.setAuthorId(7L);
        post.setTitle("标题");
        post.setStatus(status);
        post.setIsTop(0);
        post.setIsEssence(0);
        return post;
    }

    private ForumPostAdminVO postVO(Long id) {
        ForumPostAdminVO vo = new ForumPostAdminVO();
        vo.setId(id);
        vo.setSectionId(10L);
        vo.setAuthorId(7L);
        vo.setStatus(ForumPostStatusEnum.PUBLISHED.getValue());
        return vo;
    }

    private ForumSection section(Long id) {
        ForumSection section = new ForumSection();
        section.setId(id);
        section.setName("论坛版块");
        return section;
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname("作者");
        return user;
    }
}

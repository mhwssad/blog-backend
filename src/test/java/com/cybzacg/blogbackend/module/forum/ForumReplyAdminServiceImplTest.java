package com.cybzacg.blogbackend.module.forum;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminVO;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumReplyRepository;
import com.cybzacg.blogbackend.module.forum.service.impl.ForumReplyAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumReplyAdminServiceImplTest {
    @Mock
    private ForumReplyRepository forumReplyRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ForumModelConvert forumModelConvert;
    @Mock
    private SysAuditLogService sysAuditLogService;

    private ForumReplyAdminServiceImpl forumReplyAdminService;

    @BeforeEach
    void setUp() {
        forumReplyAdminService = new ForumReplyAdminServiceImpl(
                forumReplyRepository,
                forumPostRepository,
                sysUserRepository,
                forumModelConvert,
                sysAuditLogService
        );
    }

    @Test
    void pageRepliesShouldNormalizeQueryAndEnrichRecords() {
        ForumReplyAdminPageQuery query = new ForumReplyAdminPageQuery();
        query.setCurrent(0L);
        query.setSize(200L);
        query.setKeyword("  reply  ");
        ForumReply reply = reply(100L, ForumReplyStatusEnum.NORMAL.getValue());
        Page<ForumReply> page = new Page<>(1, 100);
        page.setTotal(1);
        page.setRecords(List.of(reply));
        ForumReplyAdminVO vo = replyVO(100L);
        when(forumReplyRepository.pageAdminReplies(query)).thenReturn(page);
        when(forumModelConvert.toReplyAdminVO(reply)).thenReturn(vo);
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(7L)));
        when(forumPostRepository.listByIds(anyCollection())).thenReturn(List.of(post(20L)));

        PageResult<ForumReplyAdminVO> result = forumReplyAdminService.pageReplies(query);

        assertEquals(1L, result.getTotal());
        assertEquals(100L, result.getSize());
        assertEquals("reply", query.getKeyword());
        assertEquals("作者", result.getRecords().get(0).getUserName());
        assertEquals("帖子标题", result.getRecords().get(0).getPostTitle());
        assertEquals("正常", result.getRecords().get(0).getStatusName());
    }

    @Test
    void hideReplyShouldRejectDeletedReply() {
        when(forumReplyRepository.getById(100L)).thenReturn(reply(100L, ForumReplyStatusEnum.DELETED.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumReplyAdminService.hideReply(100L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumReplyRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void hideReplyShouldUpdateStatusAndAudit() {
        when(forumReplyRepository.getById(100L)).thenReturn(reply(100L, ForumReplyStatusEnum.NORMAL.getValue()));

        forumReplyAdminService.hideReply(100L, 99L, "127.0.0.1", "ua");

        verify(forumReplyRepository).updateStatusById(100L, ForumReplyStatusEnum.HIDDEN.getValue());
        verifyAudit(SysAuditOperationType.HIDE_FORUM_REPLY, 100L);
    }

    @Test
    void restoreReplyShouldOnlyAllowHiddenReply() {
        when(forumReplyRepository.getById(100L)).thenReturn(reply(100L, ForumReplyStatusEnum.NORMAL.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumReplyAdminService.restoreReply(100L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumReplyRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void restoreReplyShouldUpdateStatusAndAudit() {
        when(forumReplyRepository.getById(100L)).thenReturn(reply(100L, ForumReplyStatusEnum.HIDDEN.getValue()));

        forumReplyAdminService.restoreReply(100L, 99L, "127.0.0.1", "ua");

        verify(forumReplyRepository).updateStatusById(100L, ForumReplyStatusEnum.NORMAL.getValue());
        verifyAudit(SysAuditOperationType.RESTORE_FORUM_REPLY, 100L);
    }

    @Test
    void deleteReplyShouldRejectAlreadyDeletedReply() {
        when(forumReplyRepository.getById(100L)).thenReturn(reply(100L, ForumReplyStatusEnum.DELETED.getValue()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumReplyAdminService.deleteReply(100L, 99L, "127.0.0.1", "ua"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumReplyRepository, never()).updateStatusById(any(), any());
    }

    @Test
    void deleteReplyShouldUpdateStatusRollbackCountsAndAudit() {
        ForumReply reply = reply(100L, ForumReplyStatusEnum.NORMAL.getValue());
        reply.setParentId(50L);
        when(forumReplyRepository.getById(100L)).thenReturn(reply);

        forumReplyAdminService.deleteReply(100L, 99L, "127.0.0.1", "ua");

        verify(forumReplyRepository).updateStatusById(100L, ForumReplyStatusEnum.DELETED.getValue());
        verify(forumPostRepository).incrementReplyCount(20L, -1);
        verify(forumReplyRepository).incrementReplyCount(50L, -1);
        verifyAudit(SysAuditOperationType.DELETE_FORUM_REPLY, 100L);
    }

    private void verifyAudit(SysAuditOperationType operationType, Long targetId) {
        ArgumentCaptor<SysAuditLogCreateRequest> captor = ArgumentCaptor.forClass(SysAuditLogCreateRequest.class);
        verify(sysAuditLogService).record(captor.capture());
        assertEquals(operationType.getCode(), captor.getValue().getOperationType());
        assertEquals("forum_reply", captor.getValue().getTargetTypeName());
        assertEquals(targetId, captor.getValue().getTargetId());
        assertEquals(99L, captor.getValue().getOperatorUserId());
        assertEquals(7L, captor.getValue().getTargetUserId());
    }

    private ForumReply reply(Long id, Integer status) {
        ForumReply reply = new ForumReply();
        reply.setId(id);
        reply.setPostId(20L);
        reply.setParentId(0L);
        reply.setUserId(7L);
        reply.setStatus(status);
        return reply;
    }

    private ForumReplyAdminVO replyVO(Long id) {
        ForumReplyAdminVO vo = new ForumReplyAdminVO();
        vo.setId(id);
        vo.setPostId(20L);
        vo.setUserId(7L);
        vo.setStatus(ForumReplyStatusEnum.NORMAL.getValue());
        return vo;
    }

    private ForumPost post(Long id) {
        ForumPost post = new ForumPost();
        post.setId(id);
        post.setTitle("帖子标题");
        return post;
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname("作者");
        return user;
    }
}

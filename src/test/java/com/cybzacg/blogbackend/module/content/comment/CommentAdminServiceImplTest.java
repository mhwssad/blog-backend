package com.cybzacg.blogbackend.module.content.comment;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.content.SysComment;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.comment.service.impl.CommentAdminServiceImpl;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAdminServiceImplTest {
    @Mock
    private SysCommentRepository sysCommentRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private ArticleContentFacadeService articleContentFacadeService;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ContentModelMapper contentModelMapper;

    private CommentAdminServiceImpl commentAdminService;

    @BeforeEach
    void setUp() {
        commentAdminService = new CommentAdminServiceImpl(
                sysCommentRepository,
                sysInteractionRepository,
                articleContentFacadeService,
                sysUserRepository,
                contentModelMapper
        );
    }

    @Test
    void pageCommentsShouldReturnMappedRecords() {
        CommentPageQuery query = new CommentPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        Page<SysComment> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(comment));

        CommentVO vo = new CommentVO();
        vo.setId(10L);

        when(sysCommentRepository.pageByAdminConditions(query)).thenReturn(page);
        when(contentModelMapper.toCommentVO(comment)).thenReturn(vo);
        when(sysUserRepository.listByIds(any())).thenReturn(List.of());

        PageResult<CommentVO> result = commentAdminService.pageComments(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getCommentShouldReturnCommentWithUserInfo() {
        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        CommentVO vo = new CommentVO();
        vo.setId(10L);

        when(sysCommentRepository.getById(10L)).thenReturn(comment);
        when(contentModelMapper.toCommentVO(comment)).thenReturn(vo);

        CommentVO result = commentAdminService.getComment(10L);

        assertEquals(10L, result.getId());
    }

    @Test
    void updateStatusShouldUpdateCommentWhenStatusValid() {
        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        when(sysCommentRepository.getById(10L)).thenReturn(comment);

        commentAdminService.updateStatus(10L, 2);

        assertEquals(Integer.valueOf(2), comment.getStatus());
        verify(sysCommentRepository).updateById(comment);
    }

    @Test
    void updateStatusShouldThrowWhenStatusInvalid() {
        assertThrows(com.cybzacg.blogbackend.exception.BusinessException.class,
            () -> commentAdminService.updateStatus(10L, 9));
    }

    @Test
    void deleteCommentShouldRemoveSubtreeAndAdjustCounts() {
        SysComment parent = comment(50L, 100L, 6L, 0L, 0L, 1);
        parent.setReplyCount(2);

        SysComment comment = comment(100L, 100L, 7L, 50L, 50L, 1);
        SysComment child = comment(101L, 100L, 8L, 100L, 50L, 1);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysCommentRepository.getById(50L)).thenReturn(parent);
        when(sysCommentRepository.findByTargetTypeAndTargetId("article", 100L)).thenReturn(List.of(parent, comment, child));

        commentAdminService.deleteComment(100L);

        verify(articleContentFacadeService).adjustCommentCount(100L, -2);
        verify(sysCommentRepository).removeByIds(any());
        verify(sysCommentRepository).updateById(parent);
    }

    private SysComment comment(Long id, Long targetId, Long userId, Long parentId, Long rootId, Integer status) {
        SysComment comment = new SysComment();
        comment.setId(id);
        comment.setTargetId(targetId);
        comment.setTargetType("article");
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setStatus(status);
        return comment;
    }

    private SysUser user(Long id, String nickname, String avatar) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    private CommentVO commentVO(Long id, Long userId) {
        CommentVO vo = new CommentVO();
        vo.setId(id);
        vo.setUserId(userId);
        return vo;
    }
}

package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.impl.CommentAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAdminServiceImplTest {
    @Mock
    private SysCommentService sysCommentService;
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysComment> commentTreeQuery;

    private CommentAdminServiceImpl commentAdminService;

    @BeforeEach
    void setUp() {
        commentAdminService = new CommentAdminServiceImpl(
                sysCommentService,
                blogArticleService,
                sysUserService,
                contentModelMapper
        );
    }

    @Test
    void pageCommentsShouldReturnMappedRecordsWithUserInfo() {
        CommentPageQuery query = new CommentPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setTargetType("article");
        query.setStatus(1);

        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        Page<SysComment> page = new Page<>(2, 5, 1);
        page.setRecords(List.of(comment));

        SysUser user = user(7L, "Tom", "/avatar.png");
        CommentVO vo = commentVO(10L, 7L);

        when(sysCommentService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sysUserService.listByIds(any())).thenReturn(List.of(user));
        when(contentModelMapper.toCommentVO(comment)).thenReturn(vo);

        PageResult<CommentVO> result = commentAdminService.pageComments(query);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(1, result.getRecords().size());
        CommentVO resultVo = result.getRecords().get(0);
        assertSame(vo, resultVo);
        assertEquals("Tom", resultVo.getUserNickname());
        assertEquals("/avatar.png", resultVo.getUserAvatar());
        assertFalse(resultVo.getLiked());
    }

    @Test
    void getCommentShouldReturnMappedCommentWithUserInfo() {
        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        SysUser user = user(7L, "Tom", "/avatar.png");
        CommentVO vo = commentVO(10L, 7L);

        when(sysCommentService.getById(10L)).thenReturn(comment);
        when(sysUserService.getById(7L)).thenReturn(user);
        when(contentModelMapper.toCommentVO(comment)).thenReturn(vo);

        CommentVO result = commentAdminService.getComment(10L);

        assertSame(vo, result);
        assertEquals("Tom", result.getUserNickname());
        assertEquals("/avatar.png", result.getUserAvatar());
        assertFalse(result.getLiked());
    }

    @Test
    void updateStatusShouldUpdateCommentWhenStatusValid() {
        SysComment comment = comment(10L, 100L, 7L, 0L, 0L, 1);
        when(sysCommentService.getById(10L)).thenReturn(comment);

        commentAdminService.updateStatus(10L, 2);

        assertEquals(Integer.valueOf(2), comment.getStatus());
        verify(sysCommentService).updateById(comment);
    }

    @Test
    void updateStatusShouldThrowWhenStatusInvalid() {
        BusinessException exception = assertThrows(BusinessException.class, () -> commentAdminService.updateStatus(10L, 9));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("评论状态非法", exception.getMessage());
        verify(sysCommentService, never()).getById(10L);
    }

    @Test
    void deleteCommentShouldRemoveSubtreeAndRollbackArticleAndParentCounts() {
        SysComment parent = comment(50L, 100L, 6L, 0L, 0L, 1);
        parent.setReplyCount(2);

        SysComment comment = comment(100L, 100L, 7L, 50L, 50L, 1);
        SysComment child = comment(101L, 100L, 8L, 100L, 50L, 1);

        BlogArticle article = new BlogArticle();
        article.setId(100L);
        article.setCommentCount(4);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysCommentService.getById(50L)).thenReturn(parent);
        when(sysCommentService.lambdaQuery()).thenReturn(commentTreeQuery);
        when(commentTreeQuery.eq(anySFunction(), any())).thenReturn(commentTreeQuery);
        when(commentTreeQuery.list()).thenReturn(List.of(parent, comment, child));
        when(blogArticleService.getById(100L)).thenReturn(article);

        commentAdminService.deleteComment(100L);

        assertEquals(Integer.valueOf(2), article.getCommentCount());
        assertEquals(Integer.valueOf(1), parent.getReplyCount());
        verify(sysCommentService).removeByIds(any());
        verify(blogArticleService).updateById(article);
        verify(sysCommentService).updateById(parent);
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

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}


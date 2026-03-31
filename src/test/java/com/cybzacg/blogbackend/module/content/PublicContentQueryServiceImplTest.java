package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.mapper.SysCommentMapper;
import com.cybzacg.blogbackend.mapper.SysTagMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentQuery;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.impl.PublicContentQueryServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicContentQueryServiceImplTest {
    @Mock
    private SysCategoryService sysCategoryService;
    @Mock
    private SysTagMapper sysTagMapper;
    @Mock
    private SysCommentMapper sysCommentMapper;
    @Mock
    private SysInteractionService sysInteractionService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysCategory> categoryQuery;
    @Mock
    private LambdaQueryChainWrapper<SysInteraction> interactionQuery;

    private PublicContentQueryServiceImpl publicContentQueryService;

    @BeforeEach
    void setUp() {
        publicContentQueryService = new PublicContentQueryServiceImpl(
                sysCategoryService,
                sysTagMapper,
                sysCommentMapper,
                sysInteractionService,
                sysUserService,
                contentModelMapper
        );
    }

    @Test
    void listCategoryTreeShouldBuildParentChildHierarchy() {
        SysCategory root = category(1L, 0L, "Backend");
        SysCategory child = category(2L, 1L, "Java");

        PublicCategoryTreeVO rootVo = categoryVO(1L, 0L, "Backend");
        PublicCategoryTreeVO childVo = categoryVO(2L, 1L, "Java");

        when(sysCategoryService.lambdaQuery()).thenReturn(categoryQuery);
        when(categoryQuery.eq(anySFunction(), any())).thenReturn(categoryQuery);
        when(categoryQuery.orderByAsc(anySFunction())).thenReturn(categoryQuery);
        when(categoryQuery.list()).thenReturn(List.of(root, child));
        when(contentModelMapper.toPublicCategoryTreeVO(root)).thenReturn(rootVo);
        when(contentModelMapper.toPublicCategoryTreeVO(child)).thenReturn(childVo);

        List<PublicCategoryTreeVO> result = publicContentQueryService.listCategoryTree();

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1L), result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(Long.valueOf(2L), result.get(0).getChildren().get(0).getId());
    }

    @Test
    void listTagsShouldReturnEmptyWhenTargetTypeUnsupported() {
        List<PublicTagVO> result = publicContentQueryService.listTags("comment");

        assertTrue(result.isEmpty());
        verifyNoInteractions(sysTagMapper, contentModelMapper);
    }

    @Test
    void listTagsShouldMapArticleTags() {
        SysTag tag = new SysTag();
        tag.setId(10L);
        tag.setName("Spring");
        tag.setColor("#0f0");

        PublicTagVO vo = new PublicTagVO();
        vo.setId(10L);
        vo.setName("Spring");

        when(sysTagMapper.selectByTargetType("article")).thenReturn(List.of(tag));
        when(contentModelMapper.toPublicTagVO(tag)).thenReturn(vo);

        List<PublicTagVO> result = publicContentQueryService.listTags(null);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(10L), result.get(0).getId());
    }

    @Test
    void listCommentsShouldBuildTreeAndFillUserInfoAndLikedState() {
        PublicCommentQuery query = new PublicCommentQuery();
        query.setTargetType("article");
        query.setTargetId(100L);

        SysComment root = comment(20L, 100L, 8L, 0L, 0L);
        SysComment reply = comment(21L, 100L, 9L, 20L, 20L);
        SysUser rootUser = user(8L, "Tom", "/a.png");
        SysUser replyUser = user(9L, "Jerry", "/b.png");

        PublicCommentVO rootVo = commentVO(20L, 8L, 0L, 0L);
        PublicCommentVO replyVo = commentVO(21L, 9L, 20L, 20L);

        SysInteraction like = new SysInteraction();
        like.setTargetId(20L);

        when(sysCommentMapper.selectRootCommentsByTarget(100L, "article")).thenReturn(List.of(root));
        when(sysCommentMapper.selectRepliesByRootIds(List.of(20L))).thenReturn(List.of(reply));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(rootUser, replyUser));
        when(contentModelMapper.toPublicCommentVO(root)).thenReturn(rootVo);
        when(contentModelMapper.toPublicCommentVO(reply)).thenReturn(replyVo);
        when(sysInteractionService.lambdaQuery()).thenReturn(interactionQuery);
        when(interactionQuery.eq(anySFunction(), any())).thenReturn(interactionQuery);
        when(interactionQuery.in(anySFunction(), anyCollection())).thenReturn(interactionQuery);
        when(interactionQuery.list()).thenReturn(List.of(like));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            List<PublicCommentVO> result = publicContentQueryService.listComments(query);

            assertEquals(1, result.size());
            assertEquals(Long.valueOf(20L), result.get(0).getId());
            assertEquals("Tom", result.get(0).getUserNickname());
            assertTrue(result.get(0).getLiked());
            assertEquals(1, result.get(0).getChildren().size());
            assertEquals("Jerry", result.get(0).getChildren().get(0).getUserNickname());
            assertFalse(result.get(0).getChildren().get(0).getLiked());
        }
    }

    @Test
    void listCommentsShouldReturnEmptyWhenNoRootComments() {
        PublicCommentQuery query = new PublicCommentQuery();
        query.setTargetType("article");
        query.setTargetId(100L);

        when(sysCommentMapper.selectRootCommentsByTarget(100L, "article")).thenReturn(List.of());

        List<PublicCommentVO> result = publicContentQueryService.listComments(query);

        assertTrue(result.isEmpty());
        verify(sysCommentMapper, never()).selectRepliesByRootIds(any());
        verify(sysUserService, never()).listByIds(anyCollection());
        verify(sysInteractionService, never()).lambdaQuery();
    }

    private SysCategory category(Long id, Long parentId, String name) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setType("article");
        category.setStatus(1);
        return category;
    }

    private PublicCategoryTreeVO categoryVO(Long id, Long parentId, String name) {
        PublicCategoryTreeVO vo = new PublicCategoryTreeVO();
        vo.setId(id);
        vo.setParentId(parentId);
        vo.setName(name);
        return vo;
    }

    private SysComment comment(Long id, Long targetId, Long userId, Long parentId, Long rootId) {
        SysComment comment = new SysComment();
        comment.setId(id);
        comment.setTargetId(targetId);
        comment.setTargetType("article");
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setStatus(1);
        return comment;
    }

    private PublicCommentVO commentVO(Long id, Long userId, Long parentId, Long rootId) {
        PublicCommentVO vo = new PublicCommentVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setParentId(parentId);
        vo.setRootId(rootId);
        return vo;
    }

    private SysUser user(Long id, String nickname, String avatar) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}


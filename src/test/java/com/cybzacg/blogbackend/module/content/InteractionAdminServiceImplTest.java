package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.service.impl.InteractionAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionAdminServiceImplTest {
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private BlogArticleRepository blogArticleService;
    @Mock
    private SysCommentRepository sysCommentRepository;
    @Mock
    private ContentModelMapper contentModelMapper;

    private InteractionAdminServiceImpl interactionAdminService;

    @BeforeEach
    void setUp() {
        interactionAdminService = new InteractionAdminServiceImpl(
                sysInteractionRepository,
                blogArticleService,
                sysCommentRepository,
                contentModelMapper
        );
    }

    @Test
    void pageInteractionsShouldReturnMappedRecords() {
        InteractionPageQuery query = new InteractionPageQuery();
        query.setCurrent(3L);
        query.setSize(4L);
        query.setUserId(9L);
        query.setTargetId(100L);
        query.setTargetType("article");
        query.setActionType("like");

        SysInteraction interaction = interaction(18L, 9L, 100L, "article", "like");
        Page<SysInteraction> page = new Page<>(3, 4, 1);
        page.setRecords(List.of(interaction));

        InteractionVO vo = new InteractionVO();
        vo.setId(18L);
        vo.setTargetId(100L);

        when(sysInteractionRepository.pageByAdminConditions(query)).thenReturn(page);
        when(contentModelMapper.toInteractionVO(interaction)).thenReturn(vo);

        PageResult<InteractionVO> result = interactionAdminService.pageInteractions(query);

        assertEquals(1L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(4L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertSame(vo, result.getRecords().get(0));
    }

    @Test
    void deleteInteractionShouldRollbackArticleLikeCount() {
        SysInteraction interaction = interaction(21L, 9L, 100L, "article", "like");
        BlogArticle article = new BlogArticle();
        article.setId(100L);
        article.setLikeCount(5);

        when(sysInteractionRepository.getById(21L)).thenReturn(interaction);
        when(blogArticleService.getById(100L)).thenReturn(article);

        interactionAdminService.deleteInteraction(21L);

        assertEquals(Integer.valueOf(4), article.getLikeCount());
        verify(blogArticleService).updateById(article);
        verify(sysInteractionRepository).removeById(21L);
    }

    @Test
    void deleteInteractionShouldRollbackCommentLikeCount() {
        SysInteraction interaction = interaction(22L, 9L, 200L, "comment", "like");
        SysComment comment = new SysComment();
        comment.setId(200L);
        comment.setLikeCount(3);

        when(sysInteractionRepository.getById(22L)).thenReturn(interaction);
        when(sysCommentRepository.getById(200L)).thenReturn(comment);

        interactionAdminService.deleteInteraction(22L);

        assertEquals(Integer.valueOf(2), comment.getLikeCount());
        verify(sysCommentRepository).updateById(comment);
        verify(sysInteractionRepository).removeById(22L);
    }

    @Test
    void deleteInteractionShouldThrowWhenRecordMissing() {
        when(sysInteractionRepository.getById(22L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> interactionAdminService.deleteInteraction(22L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("互动记录不存在", exception.getMessage());
        verify(blogArticleService, never()).updateById(org.mockito.ArgumentMatchers.any(BlogArticle.class));
        verify(sysCommentRepository, never()).updateById(org.mockito.ArgumentMatchers.any(SysComment.class));
        verify(sysInteractionRepository, never()).removeById(22L);
    }

    private SysInteraction interaction(Long id, Long userId, Long targetId, String targetType, String actionType) {
        SysInteraction interaction = new SysInteraction();
        interaction.setId(id);
        interaction.setUserId(userId);
        interaction.setTargetId(targetId);
        interaction.setTargetType(targetType);
        interaction.setActionType(actionType);
        return interaction;
    }
}


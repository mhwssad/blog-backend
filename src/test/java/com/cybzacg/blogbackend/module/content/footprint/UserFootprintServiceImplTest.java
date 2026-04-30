package com.cybzacg.blogbackend.module.content.footprint;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.content.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.module.content.footprint.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.footprint.service.impl.UserFootprintServiceImpl;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFootprintServiceImplTest {
    @Mock
    private SysUserFootprintRepository sysUserFootprintRepository;
    @Mock
    private ArticleContentFacadeService articleContentFacadeService;
    @Mock
    private ContentModelMapper contentModelMapper;

    private UserFootprintServiceImpl userFootprintService;

    @BeforeEach
    void setUp() {
        userFootprintService = new UserFootprintServiceImpl(
                sysUserFootprintRepository,
                articleContentFacadeService,
                contentModelMapper
        );
    }

    @Test
    void pageFootprintsShouldReturnMappedRecordsForCurrentUser() {
        UserFootprintPageQuery query = new UserFootprintPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setTargetType("article");

        SysUserFootprint footprint = footprint(11L, 7L, 100L, "article");
        Page<SysUserFootprint> page = new Page<>(2, 5, 1);
        page.setRecords(List.of(footprint));

        UserFootprintVO vo = new UserFootprintVO();
        vo.setId(11L);
        vo.setTargetId(100L);

        when(sysUserFootprintRepository.pageByUserIdAndTargetType(7L, "article", 2L, 5L)).thenReturn(page);
        when(contentModelMapper.toUserFootprintVO(footprint)).thenReturn(vo);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            PageResult<UserFootprintVO> result = userFootprintService.pageFootprints(query);

            assertEquals(1L, result.getTotal());
            assertEquals(2L, result.getCurrent());
            assertEquals(5L, result.getSize());
            assertSame(vo, result.getRecords().get(0));
        }
    }

    @Test
    void deleteFootprintShouldRemoveOwnedRecord() {
        SysUserFootprint footprint = footprint(12L, 7L, 100L, "article");
        when(sysUserFootprintRepository.getById(12L)).thenReturn(footprint);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userFootprintService.deleteFootprint(12L);
        }

        verify(sysUserFootprintRepository).removeById(12L);
    }

    @Test
    void deleteFootprintShouldThrowWhenRecordNotOwned() {
        SysUserFootprint footprint = footprint(12L, 8L, 100L, "article");
        when(sysUserFootprintRepository.getById(12L)).thenReturn(footprint);

        BusinessException exception;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userFootprintService.deleteFootprint(12L));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("足迹不存在", exception.getMessage());
        verify(sysUserFootprintRepository, never()).removeById(12L);
    }

    @Test
    void clearFootprintsShouldRemoveCurrentUserRecords() {
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userFootprintService.clearFootprints();
        }

        verify(sysUserFootprintRepository).removeByUserId(7L);
    }

    @Test
    void recordArticleFootprintShouldUpsertWhenArticleVisible() {
        BlogArticle article = new BlogArticle();
        article.setId(100L);
        article.setStatus(1);
        article.setTitle("Public Article");

        SysUserFootprint footprint = footprint(null, 7L, 100L, "article");

        when(articleContentFacadeService.findAccessiblePublishedArticle(100L, 7L)).thenReturn(article);
        when(contentModelMapper.toArticleFootprint(eq(7L), eq(article), eq("127.0.0.1"), eq("JUnit"), any(LocalDateTime.class)))
                .thenReturn(footprint);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L);
             MockedStatic<RequestContextUtils> requestContextUtils = mockStatic(RequestContextUtils.class)) {
            requestContextUtils.when(RequestContextUtils::getClientIp).thenReturn("127.0.0.1");
            requestContextUtils.when(RequestContextUtils::getUserAgent).thenReturn("JUnit");

            userFootprintService.recordArticleFootprint(100L);
        }

        verify(contentModelMapper).toArticleFootprint(eq(7L), eq(article), eq("127.0.0.1"), eq("JUnit"), any(LocalDateTime.class));
        verify(sysUserFootprintRepository).upsertFootprint(footprint);
    }

    private SysUserFootprint footprint(Long id, Long userId, Long targetId, String targetType) {
        SysUserFootprint footprint = new SysUserFootprint();
        footprint.setId(id);
        footprint.setUserId(userId);
        footprint.setTargetId(targetId);
        footprint.setTargetType(targetType);
        return footprint;
    }
}

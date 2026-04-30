package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleAccessControlServiceImpl;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleAccessControlServiceTest {
    @Mock
    private ArticleStatusMachine articleStatusMachine;
    @Mock
    private BlogArticleAccessRepository blogArticleAccessRepository;

    private ArticleAccessControlServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(articleStatusMachine.isPublishedForNormalUsers(any(), any(LocalDateTime.class))).thenReturn(true);
        lenient().when(articleStatusMachine.normalizeVisibilityScope(any())).thenReturn(0);
        service = spy(new ArticleAccessControlServiceImpl(blogArticleAccessRepository, articleStatusMachine));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canAccessArticleShouldRespectBasicAccessLevels() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setAuthorId(10L);

        article.setAccessLevel(0);
        assertTrue(service.canAccessArticle(article, null));

        article.setAccessLevel(1);
        assertFalse(service.canAccessArticle(article, null));
        assertTrue(service.canAccessArticle(article, 99L));

        article.setAccessLevel(2);
        assertFalse(service.canAccessArticle(article, 99L));

        article.setAccessLevel(3);
        assertFalse(service.canAccessArticle(article, 99L));

        assertTrue(service.canAccessArticle(article, 10L));
    }

    @Test
    void canAccessArticleShouldAllowBackendPermissionBypass() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L,
                "N/A",
                List.of(new SimpleGrantedAuthority("content:article:query"))));

        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setAccessLevel(4);

        assertTrue(service.canAccessArticle(article, 99L));
    }

    @Test
    void canAccessArticleShouldRequireValidWhitelistForAccessLevelFour() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setAccessLevel(4);

        BlogArticleAccess whitelist = new BlogArticleAccess();
        whitelist.setArticleId(1L);
        whitelist.setUserId(9L);
        whitelist.setAccessType(1);
        whitelist.setExpireTime(null);

        doReturn(List.of(whitelist)).when(service).listArticleAccesses(1L);
        assertTrue(service.canAccessArticle(article, 9L));
        assertTrue(service.hasArticleAccess(1L, 9L));
    }

    @Test
    void canAccessArticleShouldRejectExpiredWhitelistAndBlacklist() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setAccessLevel(4);

        BlogArticleAccess expired = new BlogArticleAccess();
        expired.setArticleId(1L);
        expired.setUserId(9L);
        expired.setAccessType(1);
        expired.setExpireTime(Instant.now().minusSeconds(60).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());

        BlogArticleAccess blacklist = new BlogArticleAccess();
        blacklist.setArticleId(1L);
        blacklist.setUserId(9L);
        blacklist.setAccessType(2);
        blacklist.setExpireTime(null);

        doReturn(List.of(expired, blacklist)).when(service).listArticleAccesses(1L);
        assertFalse(service.canAccessArticle(article, 9L));
        assertFalse(service.hasArticleAccess(1L, 9L));
    }

    /**
     * 定时发布时间在未来的文章，isPublishedForNormalUsers 返回 false，应拒绝访问。
     */
    @Test
    void canAccessArticleShouldRejectScheduledFutureArticle() {
        BlogArticle article = new BlogArticle();
        article.setId(5L);
        article.setAuthorId(99L);
        article.setStatus(1);
        article.setAccessLevel(0);
        article.setScheduledPublishTime(LocalDateTime.now().plusHours(1));

        // 使用 any() 匹配器覆盖 setUp 中的 lenient 默认值
        when(articleStatusMachine.isPublishedForNormalUsers(eq(article), any(LocalDateTime.class))).thenReturn(false);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            assertFalse(service.canAccessArticle(article, null));
        }
    }

    /**
     * 匿名用户(null userId)访问登录可见文章，validateArticleAccess 应抛出 FORBIDDEN。
     */
    @Test
    void validateArticleAccessShouldThrowForAnonymousOnLoginRequired() {
        BlogArticle article = new BlogArticle();
        article.setId(6L);
        article.setAuthorId(99L);
        article.setStatus(1);
        article.setAccessLevel(1);
        article.setVisibilityScope(0);

        // isPublishedForNormalUsers 和 normalizeVisibilityScope 的 lenient 默认值已满足此场景

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.validateArticleAccess(article, null));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        }
    }
}

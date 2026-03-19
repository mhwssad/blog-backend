package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.module.article.service.BlogArticleAccessService;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleAccessControlServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ArticleAccessControlServiceTest {
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canAccessArticleShouldRespectBasicAccessLevels() {
        ArticleAccessControlServiceImpl service = spy(new ArticleAccessControlServiceImpl(mock(BlogArticleAccessService.class)));

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
        ArticleAccessControlServiceImpl service = spy(new ArticleAccessControlServiceImpl(mock(BlogArticleAccessService.class)));
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
        ArticleAccessControlServiceImpl service = spy(new ArticleAccessControlServiceImpl(mock(BlogArticleAccessService.class)));
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
        ArticleAccessControlServiceImpl service = spy(new ArticleAccessControlServiceImpl(mock(BlogArticleAccessService.class)));
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setAccessLevel(4);

        BlogArticleAccess expired = new BlogArticleAccess();
        expired.setArticleId(1L);
        expired.setUserId(9L);
        expired.setAccessType(1);
        expired.setExpireTime(java.util.Date.from(Instant.now().minusSeconds(60)));

        BlogArticleAccess blacklist = new BlogArticleAccess();
        blacklist.setArticleId(1L);
        blacklist.setUserId(9L);
        blacklist.setAccessType(2);
        blacklist.setExpireTime(null);

        doReturn(List.of(expired, blacklist)).when(service).listArticleAccesses(1L);
        assertFalse(service.canAccessArticle(article, 9L));
        assertFalse(service.hasArticleAccess(1L, 9L));
    }
}

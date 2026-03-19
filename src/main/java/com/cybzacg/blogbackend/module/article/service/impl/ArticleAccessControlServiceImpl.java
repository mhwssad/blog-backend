package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.BlogArticleAccessService;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 文章访问控制服务实现。
 *
 * <p>负责根据文章访问级别、作者身份、后台权限和定向授权记录判断文章是否可访问。
 */
@Service
@RequiredArgsConstructor
public class ArticleAccessControlServiceImpl implements ArticleAccessControlService {
    private final BlogArticleAccessService blogArticleAccessService;

    @Override
    public boolean canAccessArticle(BlogArticle article, Long userId) {
        if (article == null) {
            return false;
        }
        if (userId != null && userId.equals(article.getAuthorId())) {
            return true;
        }
        if (SecurityUtils.hasAuthority("content:article:query")) {
            return true;
        }
        Integer accessLevel = article.getAccessLevel();
        if (accessLevel == null || accessLevel == 0) {
            return true;
        }
        if (accessLevel == 1) {
            return userId != null;
        }
        if (accessLevel == 2 || accessLevel == 3) {
            return false;
        }
        if (accessLevel != 4 || userId == null) {
            return false;
        }

        List<BlogArticleAccess> accessList = listArticleAccesses(article.getId());
        Date now = new Date();
        boolean hitWhitelist = false;
        for (BlogArticleAccess access : accessList) {
            if (!userId.equals(access.getUserId())) {
                continue;
            }
            if (access.getExpireTime() != null && access.getExpireTime().before(now)) {
                continue;
            }
            if (Integer.valueOf(2).equals(access.getAccessType())) {
                return false;
            }
            if (Integer.valueOf(1).equals(access.getAccessType())) {
                hitWhitelist = true;
            }
        }
        return hitWhitelist;
    }

    @Override
    public void validateArticleAccess(BlogArticle article, Long userId) {
        if (!canAccessArticle(article, userId)) {
            throw new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "当前用户无权访问该文章");
        }
    }

    @Override
    public boolean hasArticleAccess(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            return false;
        }
        Date now = new Date();
        return listArticleAccesses(articleId).stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().before(now))
                .noneMatch(access -> Integer.valueOf(2).equals(access.getAccessType()))
                && listArticleAccesses(articleId).stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().before(now))
                .anyMatch(access -> Integer.valueOf(1).equals(access.getAccessType()));
    }

    /**
     * 按访问类型和用户 ID 排序读取授权记录，方便后续判断逻辑稳定执行。
     */
    @Override
    public List<BlogArticleAccess> listArticleAccesses(Long articleId) {
        return blogArticleAccessService.lambdaQuery()
                .eq(BlogArticleAccess::getArticleId, articleId)
                .orderByAsc(BlogArticleAccess::getAccessType)
                .orderByAsc(BlogArticleAccess::getUserId)
                .list();
    }
}

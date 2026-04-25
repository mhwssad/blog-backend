package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章访问控制服务实现。
 *
 * <p>负责根据文章访问级别、作者身份、后台权限和定向授权记录判断文章是否可访问。
 */
@Service
@RequiredArgsConstructor
public class ArticleAccessControlServiceImpl implements ArticleAccessControlService {
    private final BlogArticleAccessRepository blogArticleAccessRepository;

    /**
     * 综合判断用户是否可访问指定文章。
     *
     * <p>依次检查：空文章拦截、作者自身放行、后台管理权限放行、
     * 访问级别(公开/登录/密码/付费/指定用户白名单)逐级判定。
     *
     * @param article 目标文章
     * @param userId  当前用户 ID，匿名时为 null
     * @return 是否允许访问
     */
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
        LocalDateTime now = LocalDateTime.now();
        boolean hitWhitelist = false;
        for (BlogArticleAccess access : accessList) {
            if (!userId.equals(access.getUserId())) {
                continue;
            }
            if (access.getExpireTime() != null && access.getExpireTime().isBefore(now)) {
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

    /**
     * 校验文章访问权限，无权访问时抛出业务异常。
     *
     * @param article 目标文章
     * @param userId  当前用户 ID
     */
    @Override
    public void validateArticleAccess(BlogArticle article, Long userId) {
        ExceptionThrowerCore.throwBusinessIfNot(canAccessArticle(article, userId), ResultErrorCode.FORBIDDEN, "当前用户无权访问该文章");
    }

    /**
     * 判断用户是否在文章的定向授权名单中且未被拉黑。
     *
     * <p>要求同时满足：存在未过期的白名单记录(accessType=1)且不存在黑名单记录(accessType=2)。
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否拥有定向授权
     */
    @Override
    public boolean hasArticleAccess(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return listArticleAccesses(articleId).stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().isBefore(now))
                .noneMatch(access -> Integer.valueOf(2).equals(access.getAccessType()))
                && listArticleAccesses(articleId).stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().isBefore(now))
                .anyMatch(access -> Integer.valueOf(1).equals(access.getAccessType()));
    }

    /**
     * 按访问类型和用户 ID 排序读取授权记录，方便后续判断逻辑稳定执行。
     */
    @Override
    public List<BlogArticleAccess> listArticleAccesses(Long articleId) {
        return blogArticleAccessRepository.listByArticleIdOrdered(articleId);
    }
}



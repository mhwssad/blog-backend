package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleAccess;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
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
    private final ArticleStatusMachine articleStatusMachine;

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
        if (!articleStatusMachine.isPublishedForNormalUsers(article, LocalDateTime.now())) {
            return false;
        }

        Integer visibilityScope = articleStatusMachine.normalizeVisibilityScope(article.getVisibilityScope());
        if (ArticleVisibilityScopeEnum.SELF_ONLY.getValue().equals(visibilityScope)) {
            return false;
        }
        if (ArticleVisibilityScopeEnum.LOGIN_REQUIRED.getValue().equals(visibilityScope) && userId == null) {
            return false;
        }
        if (ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(visibilityScope)
                && !hasWhitelistAccess(article.getId(), userId)) {
            return false;
        }
        return canPassLegacyAccessLevel(article, userId);
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
        return hasWhitelistAccess(articleId, userId);
    }

    /**
     * 按访问类型和用户 ID 排序读取授权记录，方便后续判断逻辑稳定执行。
     */
    @Override
    public List<BlogArticleAccess> listArticleAccesses(Long articleId) {
        return blogArticleAccessRepository.listByArticleIdOrdered(articleId);
    }

    private boolean canPassLegacyAccessLevel(BlogArticle article, Long userId) {
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
        return hasWhitelistAccess(article.getId(), userId);
    }

    private boolean hasWhitelistAccess(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            return false;
        }
        List<BlogArticleAccess> accessList = listArticleAccesses(articleId);
        LocalDateTime now = LocalDateTime.now();
        boolean hitBlacklist = accessList.stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().isBefore(now))
                .anyMatch(access -> Integer.valueOf(2).equals(access.getAccessType()));
        if (hitBlacklist) {
            return false;
        }
        return accessList.stream()
                .filter(access -> userId.equals(access.getUserId()))
                .filter(access -> access.getExpireTime() == null || !access.getExpireTime().isBefore(now))
                .anyMatch(access -> Integer.valueOf(1).equals(access.getAccessType()));
    }
}


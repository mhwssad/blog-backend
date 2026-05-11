package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleCategory;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRelationRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelConvert;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticlePageQuery;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleVO;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessManageService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.UserArticleManageService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户侧文章管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserArticleManageServiceImpl implements UserArticleManageService {
    private static final String TARGET_TYPE_ARTICLE = "article";
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleCategoryRepository blogArticleCategoryRepository;
    private final SysTagRelationRepository sysTagRelationRepository;
    private final SysUserRepository sysUserRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleAccessManageService articleAccessManageService;
    private final ArticleSeriesService articleSeriesService;
    private final ArticleModelConvert articleModelConvert;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<UserArticleVO> pageMyArticles(UserArticlePageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        UserArticlePageQuery safeQuery = normalizeQuery(query);
        Set<Long> filteredArticleIds = resolveArticleIdsByRelations(safeQuery);
        if (filteredArticleIds != null && filteredArticleIds.isEmpty()) {
            return PageResult.empty(safeQuery.getCurrent(), safeQuery.getSize());
        }

        ArticleAdminPageQuery articleQuery = new ArticleAdminPageQuery();
        articleQuery.setCurrent(safeQuery.getCurrent());
        articleQuery.setSize(safeQuery.getSize());
        articleQuery.setKeyword(safeQuery.getKeyword());
        articleQuery.setAuthorId(userId);
        articleQuery.setStatus(safeQuery.getStatus());
        articleQuery.setReviewStatus(safeQuery.getReviewStatus());
        articleQuery.setVisibilityScope(safeQuery.getVisibilityScope());

        Page<BlogArticle> page = blogArticleRepository.pageAdminArticles(articleQuery, filteredArticleIds);
        List<UserArticleVO> records = page.getRecords().stream()
                .map(articleModelConvert::toUserVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserArticleDetailVO getMyArticle(Long id) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = requireOwnedArticle(id, userId);
        UserArticleDetailVO detailVO = articleModelConvert.toUserDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategoryIds(listCategoryIds(article.getId()));
        detailVO.setTagIds(sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, article.getId()));
        detailVO.setAccessList(articleAccessControlService.listArticleAccesses(article.getId()).stream()
                .map(articleModelConvert::toAccessItem)
                .toList());
        detailVO.setSeriesList(articleSeriesService.listVisibleSeriesSummariesByArticleId(article.getId(), userId));
        return detailVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMyArticleAccess(Long id, List<ArticleAccessItem> accessList) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = requireOwnedArticle(id, userId);
        ExceptionThrowerCore.throwBusinessIfNot(articleAccessManageService.supportsAccessList(article),
                ResultErrorCode.ILLEGAL_ARGUMENT, "当前文章不支持访问授权配置");
        articleAccessManageService.validateAccessItems(accessList);
        articleAccessManageService.rebuildArticleAccessBindings(id, accessList);
    }

    /**
     * 标准化分页查询参数，避免用户侧分页被异常值放大。
     */
    private UserArticlePageQuery normalizeQuery(UserArticlePageQuery query) {
        UserArticlePageQuery safeQuery = query == null ? new UserArticlePageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        return safeQuery;
    }

    /**
     * 根据分类和标签条件反查当前用户的文章 ID 集合。
     */
    private Set<Long> resolveArticleIdsByRelations(UserArticlePageQuery query) {
        Set<Long> ids = null;
        if (query.getCategoryId() != null) {
            ids = blogArticleCategoryRepository.listArticleIdsByCategoryId(query.getCategoryId()).stream()
                    .map(BlogArticleCategory::getArticleId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (query.getTagId() != null) {
            Set<Long> tagIds = new LinkedHashSet<>(sysTagRelationRepository.listTargetIdsByTargetTypeAndTagId(TARGET_TYPE_ARTICLE, query.getTagId()));
            if (ids == null) {
                ids = tagIds;
            } else {
                ids.retainAll(tagIds);
            }
        }
        return ids;
    }

    /**
     * 校验文章属于当前登录用户。
     */
    private BlogArticle requireOwnedArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        ExceptionThrowerCore.throwBusinessIfNot(Objects.equals(article.getAuthorId(), userId),
                ResultErrorCode.FORBIDDEN, "只能查看自己的文章");
        return article;
    }

    /**
     * 按绑定顺序读取文章分类 ID。
     */
    private List<Long> listCategoryIds(Long articleId) {
        return blogArticleCategoryRepository.listByArticleIdOrdered(articleId).stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
    }

    /**
     * 读取作者展示名。
     */
    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(authorId);
        if (user == null) {
            return null;
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }
}

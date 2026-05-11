package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleCategory;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.content.*;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelConvert;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.PublicArticleService;
import com.cybzacg.blogbackend.module.content.footprint.service.UserFootprintService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 前台文章查询服务实现。
 *
 * <p>负责公开文章分页、详情聚合、访问控制校验，以及分类标签等展示信息的组装。
 */
@Service
@RequiredArgsConstructor
public class PublicArticleServiceImpl implements PublicArticleService {
    private static final String TARGET_TYPE_ARTICLE = "article";

    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleCategoryRepository blogArticleCategoryRepository;
    private final SysTagRelationRepository sysTagRelationRepository;
    private final SysCategoryRepository sysCategoryRepository;
    private final SysTagRepository sysTagRepository;
    private final SysUserRepository sysUserRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleSeriesService articleSeriesService;
    private final ArticleStatusMachine articleStatusMachine;
    private final ArticleModelConvert articleModelConvert;
    private final ContentModelConvert contentModelConvert;
    private final UserFootprintService userFootprintService;

    /**
     * 分页读取当前用户可见的已发布文章，并在内存中统一完成条件过滤、排序和分页切片。
     */
    @Override
    public PageResult<PublicArticleCardVO> pageArticles(PublicArticlePageQuery query) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BlogArticle> page =
                blogArticleRepository.pagePublishedArticles(query, null);
        Map<Long, String> authorNameMap = loadAuthorNames(page.getRecords().stream()
                .map(BlogArticle::getAuthorId)
                .collect(Collectors.toSet()));

        List<PublicArticleCardVO> records = page.getRecords().stream()
                .map(articleModelConvert::toPublicCardVO)
                .peek(vo -> vo.setAuthorName(authorNameMap.get(vo.getAuthorId())))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 查询单篇文章详情，并在访问校验通过后补齐分类、标签和当前用户状态。
     */
    @Override
    public PublicArticleDetailVO getArticle(Long id) {
        BlogArticle article = blogArticleRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.NO_HANDLER_FOUND, "文章不存在");

        Long userId = SecurityUtils.getUserId();
        articleAccessControlService.validateArticleAccess(article, userId);

        PublicArticleDetailVO detailVO = articleModelConvert.toPublicDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategories(loadArticleCategories(article.getId()));
        detailVO.setTags(loadArticleTags(article.getId()));
        detailVO.setLiked(isArticleLiked(article.getId(), userId));
        detailVO.setCollected(isArticleCollected(article.getId(), userId));
        detailVO.setCanComment(userId != null && articleStatusMachine.canInteract(article));
        detailVO.setSeriesList(articleSeriesService.listVisibleSeriesSummariesByArticleId(article.getId(), userId));
        userFootprintService.recordArticleFootprint(article.getId());
        return detailVO;
    }

    /**
     * 按文章分类绑定顺序组装前台展示分类列表。
     */
    private List<PublicCategoryTreeVO> loadArticleCategories(Long articleId) {
        List<Long> categoryIds = blogArticleCategoryRepository.listByArticleIdOrdered(articleId)
                .stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysCategory> categoryMap = sysCategoryRepository.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(SysCategory::getId, category -> category));
        List<PublicCategoryTreeVO> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            SysCategory category = categoryMap.get(categoryId);
            if (category != null) {
                categories.add(contentModelConvert.toPublicCategoryTreeVO(category));
            }
        }
        return categories;
    }

    /**
     * 按标签关联顺序读取文章标签，保证前台展示顺序稳定。
     */
    private List<PublicTagVO> loadArticleTags(Long articleId) {
        List<Long> tagIds = sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, articleId);
        if (tagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysTag> tagMap = sysTagRepository.listByIds(tagIds).stream()
                .collect(Collectors.toMap(SysTag::getId, tag -> tag));
        List<PublicTagVO> tags = new ArrayList<>();
        for (Long tagId : tagIds) {
            SysTag tag = tagMap.get(tagId);
            if (tag != null) {
                tags.add(contentModelConvert.toPublicTagVO(tag));
            }
        }
        return tags;
    }

    /**
     * 查询当前用户是否已点赞文章，用于详情页状态回填。
     */
    private boolean isArticleLiked(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(userId, articleId, TARGET_TYPE_ARTICLE, "like");
    }

    /**
     * 查询当前用户是否已收藏文章，用于详情页按钮状态展示。
     */
    private boolean isArticleCollected(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return sysCollectionRepository.existsByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_ARTICLE, articleId);
    }

    /**
     * 批量读取作者名称，供分页列表回填作者展示名复用。
     */
    private Map<Long, String> loadAuthorNames(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> authorNameMap = new HashMap<>();
        sysUserRepository.listByIds(authorIds).forEach(user -> authorNameMap.put(user.getId(), buildAuthorName(user)));
        return authorNameMap;
    }

    /**
     * 读取单个作者展示名，优先返回昵称，缺失时回退用户名。
     */
    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(authorId);
        return user == null ? null : buildAuthorName(user);
    }

    private String buildAuthorName(SysUser user) {
        return StrUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

}



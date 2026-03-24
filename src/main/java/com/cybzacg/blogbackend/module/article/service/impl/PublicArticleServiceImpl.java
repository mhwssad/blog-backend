package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.article.service.PublicArticleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.module.content.service.UserFootprintService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final BlogArticleService blogArticleService;
    private final BlogArticleCategoryService blogArticleCategoryService;
    private final SysTagRelationService sysTagRelationService;
    private final SysCategoryService sysCategoryService;
    private final SysTagService sysTagService;
    private final SysUserService sysUserService;
    private final SysInteractionService sysInteractionService;
    private final SysCollectionService sysCollectionService;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleModelMapper articleModelMapper;
    private final ContentModelMapper contentModelMapper;
    private final UserFootprintService userFootprintService;

    @Override
    public PageResult<PublicArticleCardVO> pageArticles(PublicArticlePageQuery query) {
        Long currentUserId = SecurityUtils.getUserId();
        Set<Long> filteredIds = resolveArticleIdsByRelations(query);
        List<BlogArticle> publishedArticles = blogArticleService.lambdaQuery()
                .eq(BlogArticle::getStatus, 1)
                .list();

        List<BlogArticle> matched = publishedArticles.stream()
                .filter(article -> filteredIds == null || filteredIds.contains(article.getId()))
                .filter(article -> !StringUtils.hasText(query.getKeyword())
                        || contains(article.getTitle(), query.getKeyword())
                        || contains(article.getSummary(), query.getKeyword()))
                .filter(article -> articleAccessControlService.canAccessArticle(article, currentUserId))
                .sorted(resolveComparator(query.getSort()))
                .toList();

        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getSize() == null ? 10L : query.getSize();
        int fromIndex = (int) Math.min(Math.max((current - 1) * size, 0), matched.size());
        int toIndex = (int) Math.min(fromIndex + size, matched.size());
        Map<Long, String> authorNameMap = loadAuthorNames(matched.stream().map(BlogArticle::getAuthorId).collect(Collectors.toSet()));

        List<PublicArticleCardVO> records = matched.subList(fromIndex, toIndex).stream()
                .map(articleModelMapper::toPublicCardVO)
                .peek(vo -> vo.setAuthorName(authorNameMap.get(vo.getAuthorId())))
                .toList();

        return PageResult.<PublicArticleCardVO>builder()
                .total((long) matched.size())
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    @Override
    public PublicArticleDetailVO getArticle(Long id, HttpServletRequest request) {
        BlogArticle article = blogArticleService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(article == null || !Integer.valueOf(1).equals(article.getStatus()), ResultErrorCode.NO_HANDLER_FOUND, "文章不存在");

        Long userId = SecurityUtils.getUserId();
        Integer accessLevel = article.getAccessLevel() == null ? 0 : article.getAccessLevel();
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(accessLevel) && userId == null, ResultErrorCode.LOGIN_REQUIRED);
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(2).equals(accessLevel) || Integer.valueOf(3).equals(accessLevel), ResultErrorCode.FORBIDDEN, "当前版本未开放该访问级别");
        articleAccessControlService.validateArticleAccess(article, userId);

        PublicArticleDetailVO detailVO = articleModelMapper.toPublicDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategories(loadArticleCategories(article.getId()));
        detailVO.setTags(loadArticleTags(article.getId()));
        detailVO.setLiked(isArticleLiked(article.getId(), userId));
        detailVO.setCollected(isArticleCollected(article.getId(), userId));
        detailVO.setCanComment(userId != null);
        userFootprintService.recordArticleFootprint(article.getId(), request);
        return detailVO;
    }

    /**
     * 根据分类和标签条件反查文章 ID，供前台分页过滤复用。
     */
    private Set<Long> resolveArticleIdsByRelations(PublicArticlePageQuery query) {
        Set<Long> ids = null;
        if (query.getCategoryId() != null) {
            ids = blogArticleCategoryService.lambdaQuery()
                    .eq(BlogArticleCategory::getCategoryId, query.getCategoryId())
                    .list()
                    .stream()
                    .map(BlogArticleCategory::getArticleId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (query.getTagId() != null) {
            Set<Long> tagIds = sysTagRelationService.lambdaQuery()
                    .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                    .eq(SysTagRelation::getTagId, query.getTagId())
                    .list()
                    .stream()
                    .map(SysTagRelation::getTargetId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (ids == null) {
                ids = tagIds;
            } else {
                ids.retainAll(tagIds);
            }
        }
        return ids;
    }

    /**
     * 根据排序参数选择对应比较器，统一处理最新、置顶和热门排序逻辑。
     */
    private Comparator<BlogArticle> resolveComparator(String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            return Comparator.comparingInt((BlogArticle article) -> defaultInt(article.getViewCount()) + defaultInt(article.getLikeCount()) + defaultInt(article.getCommentCount()))
                    .reversed()
                    .thenComparing(BlogArticle::getId, Comparator.reverseOrder());
        }
        if ("top".equalsIgnoreCase(sort)) {
            return Comparator.comparing((BlogArticle article) -> defaultInt(article.getIsTop()), Comparator.reverseOrder())
                    .thenComparing(BlogArticle::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(BlogArticle::getId, Comparator.reverseOrder());
        }
        return Comparator.comparing(BlogArticle::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BlogArticle::getId, Comparator.reverseOrder());
    }

    /**
     * 按文章分类绑定顺序组装前台展示分类列表。
     */
    private List<PublicCategoryTreeVO> loadArticleCategories(Long articleId) {
        List<Long> categoryIds = blogArticleCategoryService.lambdaQuery()
                .eq(BlogArticleCategory::getArticleId, articleId)
                .orderByAsc(BlogArticleCategory::getSortOrder)
                .orderByAsc(BlogArticleCategory::getId)
                .list()
                .stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysCategory> categoryMap = sysCategoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(SysCategory::getId, category -> category));
        List<PublicCategoryTreeVO> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            SysCategory category = categoryMap.get(categoryId);
            if (category != null) {
                PublicCategoryTreeVO vo = contentModelMapper.toPublicCategoryTreeVO(category);
                vo.setChildren(new ArrayList<>());
                categories.add(vo);
            }
        }
        return categories;
    }

    /**
     * 按标签关联顺序读取文章标签，保证前台展示顺序稳定。
     */
    private List<PublicTagVO> loadArticleTags(Long articleId) {
        List<Long> tagIds = sysTagRelationService.lambdaQuery()
                .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(SysTagRelation::getTargetId, articleId)
                .orderByAsc(SysTagRelation::getId)
                .list()
                .stream()
                .map(SysTagRelation::getTagId)
                .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysTag> tagMap = sysTagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(SysTag::getId, tag -> tag));
        List<PublicTagVO> tags = new ArrayList<>();
        for (Long tagId : tagIds) {
            SysTag tag = tagMap.get(tagId);
            if (tag != null) {
                tags.add(contentModelMapper.toPublicTagVO(tag));
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
        return sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, articleId)
                .eq(SysInteraction::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(SysInteraction::getActionType, "like")
                .exists();
    }

    /**
     * 查询当前用户是否已收藏文章，用于详情页按钮状态展示。
     */
    private boolean isArticleCollected(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return sysCollectionService.lambdaQuery()
                .eq(SysCollection::getUserId, userId)
                .eq(SysCollection::getTargetId, articleId)
                .eq(SysCollection::getTargetType, TARGET_TYPE_ARTICLE)
                .exists();
    }

    private Map<Long, String> loadAuthorNames(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> authorNameMap = new HashMap<>();
        sysUserService.listByIds(authorIds).forEach(user -> authorNameMap.put(user.getId(), buildAuthorName(user)));
        return authorNameMap;
    }

    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(authorId);
        return user == null ? null : buildAuthorName(user);
    }

    private String buildAuthorName(SysUser user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}





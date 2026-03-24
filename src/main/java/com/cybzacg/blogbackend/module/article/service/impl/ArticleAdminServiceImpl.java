package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleAccessService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.module.content.service.SysUserFootprintService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章后台管理服务实现。
 *
 * <p>负责文章后台场景下的分页查询、详情组装、保存校验、关联关系维护和访问授权配置。
 */
@Service
@RequiredArgsConstructor
public class ArticleAdminServiceImpl implements ArticleAdminService {
    private static final String TARGET_TYPE_ARTICLE = "article";

    private final BlogArticleService blogArticleService;
    private final BlogArticleCategoryService blogArticleCategoryService;
    private final BlogArticleAccessService blogArticleAccessService;
    private final SysTagRelationService sysTagRelationService;
    private final SysCategoryService sysCategoryService;
    private final SysTagService sysTagService;
    private final SysCommentService sysCommentService;
    private final SysCollectionService sysCollectionService;
    private final SysInteractionService sysInteractionService;
    private final SysUserFootprintService sysUserFootprintService;
    private final SysUserService sysUserService;
    private final ArticleModelMapper articleModelMapper;
    private final ArticleAccessControlService articleAccessControlService;

    /**
     * 按作者、状态、授权级别及分类标签等条件分页查询后台文章。
     */
    @Override
    public PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query) {
        Set<Long> filteredArticleIds = resolveArticleIdsByRelations(query);
        if (filteredArticleIds != null && filteredArticleIds.isEmpty()) {
            return PageResult.<ArticleAdminVO>builder()
                    .total(0L)
                    .current(query.getCurrent())
                    .size(query.getSize())
                    .records(List.of())
                    .build();
        }

        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(BlogArticle::getTitle, query.getKeyword())
                    .or()
                    .like(BlogArticle::getSummary, query.getKeyword()));
        }
        wrapper.eq(query.getAuthorId() != null, BlogArticle::getAuthorId, query.getAuthorId())
                .eq(query.getStatus() != null, BlogArticle::getStatus, query.getStatus())
                .eq(query.getAccessLevel() != null, BlogArticle::getAccessLevel, query.getAccessLevel())
                .eq(query.getIsTop() != null, BlogArticle::getIsTop, query.getIsTop())
                .ge(query.getPublishTimeStart() != null, BlogArticle::getPublishTime, query.getPublishTimeStart())
                .le(query.getPublishTimeEnd() != null, BlogArticle::getPublishTime, query.getPublishTimeEnd())
                .in(filteredArticleIds != null, BlogArticle::getId, filteredArticleIds)
                .orderByDesc(BlogArticle::getUpdatedAt)
                .orderByDesc(BlogArticle::getId);

        Page<BlogArticle> page = blogArticleService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        Map<Long, String> authorNameMap = loadAuthorNames(page.getRecords().stream()
                .map(BlogArticle::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<ArticleAdminVO> records = page.getRecords().stream()
                .map(articleModelMapper::toAdminVO)
                .peek(vo -> vo.setAuthorName(authorNameMap.get(vo.getAuthorId())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public ArticleDetailVO getArticle(Long id) {
        BlogArticle article = getArticleOrThrow(id);
        return buildArticleDetail(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO createArticle(ArticleSaveRequest request) {
        validateSaveRequest(request);
        BlogArticle article = articleModelMapper.toArticle(request);
        applyArticleFields(article, request, true);
        initializeCounters(article);
        blogArticleService.save(article);
        syncCategoryBindings(article.getId(), request.getCategoryIds());
        syncTagBindings(article.getId(), request.getTagIds());
        syncAccessBindings(article.getId(), article.getAccessLevel(), request.getAccessList());
        return buildArticleDetail(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request) {
        validateSaveRequest(request);
        BlogArticle article = getArticleOrThrow(id);
        applyArticleFields(article, request, false);
        blogArticleService.updateById(article);
        syncCategoryBindings(id, request.getCategoryIds());
        syncTagBindings(id, request.getTagIds());
        syncAccessBindings(id, article.getAccessLevel(), request.getAccessList());
        return buildArticleDetail(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        BlogArticle article = getArticleOrThrow(id);
        article.setStatus(status);
        if (Integer.valueOf(1).equals(status) && article.getPublishTime() == null) {
            article.setPublishTime(new Date());
        }
        blogArticleService.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignAccess(Long id, List<ArticleAccessItem> accessList) {
        BlogArticle article = getArticleOrThrow(id);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(4).equals(article.getAccessLevel()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "当前文章访问级别不是指定用户可见");
        validateAccessItems(accessList);
        rebuildAccessBindings(id, accessList);
    }

    /**
     * 删除文章及其分类、标签、评论、收藏、交互和足迹等全部关联数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        getArticleOrThrow(id);
        blogArticleAccessService.remove(new LambdaQueryWrapper<BlogArticleAccess>()
                .eq(BlogArticleAccess::getArticleId, id));
        blogArticleCategoryService.remove(new LambdaQueryWrapper<BlogArticleCategory>()
                .eq(BlogArticleCategory::getArticleId, id));
        sysTagRelationService.remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(SysTagRelation::getTargetId, id));
        sysCommentService.remove(new LambdaQueryWrapper<com.cybzacg.blogbackend.domain.SysComment>()
                .eq(com.cybzacg.blogbackend.domain.SysComment::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(com.cybzacg.blogbackend.domain.SysComment::getTargetId, id));
        sysCollectionService.remove(new LambdaQueryWrapper<com.cybzacg.blogbackend.domain.SysCollection>()
                .eq(com.cybzacg.blogbackend.domain.SysCollection::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(com.cybzacg.blogbackend.domain.SysCollection::getTargetId, id));
        sysInteractionService.remove(new LambdaQueryWrapper<com.cybzacg.blogbackend.domain.SysInteraction>()
                .eq(com.cybzacg.blogbackend.domain.SysInteraction::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(com.cybzacg.blogbackend.domain.SysInteraction::getTargetId, id));
        sysUserFootprintService.remove(new LambdaQueryWrapper<com.cybzacg.blogbackend.domain.SysUserFootprint>()
                .eq(com.cybzacg.blogbackend.domain.SysUserFootprint::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(com.cybzacg.blogbackend.domain.SysUserFootprint::getTargetId, id));
        blogArticleService.removeById(id);
    }

    /**
     * 将文章实体扩展为详情对象，补齐作者名、分类、标签和授权列表。
     */
    private ArticleDetailVO buildArticleDetail(BlogArticle article) {
        ArticleDetailVO detailVO = articleModelMapper.toDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategoryIds(listCategoryIds(article.getId()));
        detailVO.setTagIds(listTagIds(article.getId()));
        detailVO.setAccessList(articleAccessControlService.listArticleAccesses(article.getId()).stream()
                .map(articleModelMapper::toAccessItem)
                .toList());
        return detailVO;
    }

    /**
     * 将请求对象中的可编辑字段统一回填到文章实体，确保创建和更新流程复用同一套规则。
     */
    private void applyArticleFields(BlogArticle article, ArticleSaveRequest request, boolean creating) {
        Date existingPublishTime = article.getPublishTime();
        articleModelMapper.updateArticle(request, article);
        article.setIsTop(defaultIfNull(article.getIsTop(), 0));
        article.setIsOriginal(defaultIfNull(article.getIsOriginal(), 1));
        article.setStatus(defaultIfNull(article.getStatus(), 0));
        article.setPublishTime(resolvePublishTime(article.getStatus(), request.getPublishTime(), existingPublishTime));
        article.setAccessLevel(defaultIfNull(article.getAccessLevel(), 0));
        if (creating && article.getPublishTime() == null && Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(new Date());
        }
    }

    private Date resolvePublishTime(Integer status, Date requestPublishTime, Date existingPublishTime) {
        if (Integer.valueOf(1).equals(defaultIfNull(status, 0))) {
            return requestPublishTime != null ? requestPublishTime : (existingPublishTime != null ? existingPublishTime : new Date());
        }
        return requestPublishTime;
    }

    private void initializeCounters(BlogArticle article) {
        article.setViewCount(defaultIfNull(article.getViewCount(), 0));
        article.setLikeCount(defaultIfNull(article.getLikeCount(), 0));
        article.setCommentCount(defaultIfNull(article.getCommentCount(), 0));
        article.setCollectCount(defaultIfNull(article.getCollectCount(), 0));
        article.setShareCount(defaultIfNull(article.getShareCount(), 0));
    }

    /**
     * 校验文章保存请求，包括作者、状态、访问级别、分类标签和授权项合法性。
     */
    private void validateSaveRequest(ArticleSaveRequest request) {
        validateStatus(defaultIfNull(request.getStatus(), 0));
        validateAccessLevel(defaultIfNull(request.getAccessLevel(), 0));
        validateAuthor(request.getAuthorId());

        ExceptionThrowerCore.throwBusinessIf(
                Integer.valueOf(0).equals(defaultIfNull(request.getIsOriginal(), 1))
                        && !StringUtils.hasText(request.getSourceUrl()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "转载文章必须提供原文链接");

        List<Long> categoryIds = uniqueIds(request.getCategoryIds(), "分类ID");
        List<Long> tagIds = uniqueIds(request.getTagIds(), "标签ID");
        request.setCategoryIds(categoryIds);
        request.setTagIds(tagIds);
        validateCategories(categoryIds);
        validateTags(tagIds);
        validateAccessItems(request.getAccessList());
    }

    private void validateAuthor(Long authorId) {
        ExceptionThrowerCore.throwBusinessIfNull(authorId, ResultErrorCode.ILLEGAL_ARGUMENT, "作者不能为空");
        SysUser author = sysUserService.getById(authorId);
        ExceptionThrowerCore.throwBusinessIf(author == null || Integer.valueOf(1).equals(author.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND, "作者不存在");
    }

    private void validateCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<SysCategory> categories = sysCategoryService.lambdaQuery()
                .in(SysCategory::getId, categoryIds)
                .eq(SysCategory::getType, TARGET_TYPE_ARTICLE)
                .list();
        ExceptionThrowerCore.throwBusinessIf(categories.size() != categoryIds.size(),
                ResultErrorCode.ILLEGAL_ARGUMENT, "分类不存在或不属于文章分类");
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTag> tags = sysTagService.listByIds(tagIds);
        ExceptionThrowerCore.throwBusinessIf(tags.size() != tagIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "标签不存在");
    }

    /**
     * 校验指定用户授权项，确保用户存在且授权组合不重复。
     */
    private void validateAccessItems(List<ArticleAccessItem> accessList) {
        if (accessList == null || accessList.isEmpty()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (ArticleAccessItem item : accessList) {
            ExceptionThrowerCore.throwBusinessIfNull(item.getUserId(), ResultErrorCode.ILLEGAL_ARGUMENT, "授权用户不能为空");
            Integer accessType = defaultIfNull(item.getAccessType(), 1);
            ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(accessType) && !Integer.valueOf(2).equals(accessType),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "访问类型非法");
            item.setAccessType(accessType);
            String key = item.getUserId() + ":" + accessType;
            ExceptionThrowerCore.throwBusinessIf(!keys.add(key), ResultErrorCode.ILLEGAL_ARGUMENT, "存在重复的访问授权记录");
            userIds.add(item.getUserId());
        }
        List<SysUser> users = sysUserService.listByIds(userIds);
        long availableUsers = users.stream()
                .filter(user -> !Integer.valueOf(1).equals(user.getDeletedFlag()))
                .count();
        ExceptionThrowerCore.throwBusinessIf(availableUsers != userIds.size(), ResultErrorCode.USER_NOT_FOUND, "授权用户不存在");
    }

    private void validateStatus(Integer status) {
        ExceptionThrowerCore.throwBusinessIf(
                !Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status) && !Integer.valueOf(2).equals(status),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文章状态非法");
    }

    private void validateAccessLevel(Integer accessLevel) {
        ExceptionThrowerCore.throwBusinessIf(accessLevel < 0 || accessLevel > 4,
                ResultErrorCode.ILLEGAL_ARGUMENT, "文章访问级别非法");
    }

    /**
     * 以“先删后建”的方式同步文章分类关系，保证顺序和最终状态一致。
     */
    private void syncCategoryBindings(Long articleId, List<Long> categoryIds) {
        blogArticleCategoryService.remove(new LambdaQueryWrapper<BlogArticleCategory>()
                .eq(BlogArticleCategory::getArticleId, articleId));
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<BlogArticleCategory> relations = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            relations.add(articleModelMapper.toArticleCategory(articleId, categoryIds.get(i), i + 1));
        }
        blogArticleCategoryService.saveBatch(relations);
    }

    /**
     * 重建文章标签关系，保持标签绑定结果与请求参数完全一致。
     */
    private void syncTagBindings(Long articleId, List<Long> tagIds) {
        sysTagRelationService.remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(SysTagRelation::getTargetId, articleId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTagRelation> relations = new ArrayList<>();
        for (Long tagId : tagIds) {
            relations.add(articleModelMapper.toTagRelation(tagId, articleId, TARGET_TYPE_ARTICLE));
        }
        sysTagRelationService.saveBatch(relations);
    }

    /**
     * 根据访问级别决定是否保留指定用户授权数据。
     */
    private void syncAccessBindings(Long articleId, Integer accessLevel, List<ArticleAccessItem> accessList) {
        if (!Integer.valueOf(4).equals(accessLevel)) {
            blogArticleAccessService.remove(new LambdaQueryWrapper<BlogArticleAccess>()
                    .eq(BlogArticleAccess::getArticleId, articleId));
            return;
        }
        rebuildAccessBindings(articleId, accessList);
    }

    /**
     * 先清空后重建文章的指定用户授权记录，保持授权结果与请求完全一致。
     */
    private void rebuildAccessBindings(Long articleId, List<ArticleAccessItem> accessList) {
        blogArticleAccessService.remove(new LambdaQueryWrapper<BlogArticleAccess>()
                .eq(BlogArticleAccess::getArticleId, articleId));
        if (accessList == null || accessList.isEmpty()) {
            return;
        }
        Date now = new Date();
        List<BlogArticleAccess> records = accessList.stream()
                .map(item -> articleModelMapper.toArticleAccess(articleId, item, now))
                .toList();
        blogArticleAccessService.saveBatch(records);
    }

    private List<Long> listCategoryIds(Long articleId) {
        return blogArticleCategoryService.lambdaQuery()
                .eq(BlogArticleCategory::getArticleId, articleId)
                .orderByAsc(BlogArticleCategory::getSortOrder)
                .orderByAsc(BlogArticleCategory::getId)
                .list()
                .stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
    }

    private List<Long> listTagIds(Long articleId) {
        return sysTagRelationService.lambdaQuery()
                .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                .eq(SysTagRelation::getTargetId, articleId)
                .orderByAsc(SysTagRelation::getId)
                .list()
                .stream()
                .map(SysTagRelation::getTagId)
                .toList();
    }

    /**
     * 先通过分类和标签反查文章 ID 集合，再交给主查询拼装最终分页条件。
     */
    private Set<Long> resolveArticleIdsByRelations(ArticleAdminPageQuery query) {
        Set<Long> ids = null;
        if (query.getCategoryId() != null) {
            Set<Long> categoryIds = blogArticleCategoryService.lambdaQuery()
                    .eq(BlogArticleCategory::getCategoryId, query.getCategoryId())
                    .list()
                    .stream()
                    .map(BlogArticleCategory::getArticleId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ids = categoryIds;
        }
        if (query.getTagId() != null) {
            Set<Long> tagIds = sysTagRelationService.lambdaQuery()
                    .eq(SysTagRelation::getTargetType, TARGET_TYPE_ARTICLE)
                    .eq(SysTagRelation::getTagId, query.getTagId())
                    .list()
                    .stream()
                    .map(SysTagRelation::getTargetId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ids = intersect(ids, tagIds);
        }
        return ids;
    }

    private Set<Long> intersect(Set<Long> current, Set<Long> incoming) {
        if (current == null) {
            return incoming;
        }
        current.retainAll(incoming);
        return current;
    }

    /**
     * 读取文章，不存在时统一抛出业务异常。
     */
    private BlogArticle getArticleOrThrow(Long id) {
        BlogArticle article = blogArticleService.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(article, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        return article;
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
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private List<Long> uniqueIds(List<Long> ids, String label) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            ExceptionThrowerCore.throwBusinessIfNull(id, ResultErrorCode.ILLEGAL_ARGUMENT, label + "不能为空");
            ExceptionThrowerCore.throwBusinessIf(!uniqueIds.add(id), ResultErrorCode.ILLEGAL_ARGUMENT, label + "存在重复值");
        }
        return new ArrayList<>(uniqueIds);
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

}



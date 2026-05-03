package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.content.*;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelConvert;
import com.cybzacg.blogbackend.module.article.model.admin.*;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.*;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.footprint.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysCategoryRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRelationRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRepository;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章后台 CRUD 服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleAdminCrudServiceImpl implements ArticleAdminCrudService {
    private static final String TARGET_TYPE_ARTICLE = "article";

    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleCategoryRepository blogArticleCategoryRepository;
    private final BlogArticleAccessRepository blogArticleAccessRepository;
    private final SysTagRelationRepository sysTagRelationRepository;
    private final SysCategoryRepository sysCategoryRepository;
    private final SysTagRepository sysTagRepository;
    private final SysCommentRepository sysCommentRepository;
    private final SysCollectionFolderRepository sysCollectionFolderRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final SysUserFootprintRepository sysUserFootprintRepository;
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileLifecycleService fileLifecycleService;
    private final SysUserRepository sysUserRepository;
    private final SysConfigService sysConfigService;
    private final AuthorPermissionService authorPermissionService;
    private final ArticleModelConvert articleModelConvert;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleAccessManageService articleAccessManageService;
    private final ArticleSeriesService articleSeriesService;
    private final ArticleStatusMachine articleStatusMachine;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query) {
        Set<Long> filteredArticleIds = resolveArticleIdsByRelations(query);
        if (filteredArticleIds != null && filteredArticleIds.isEmpty()) {
            return PageResult.empty(query.getCurrent(), query.getSize());
        }

        Page<BlogArticle> page = blogArticleRepository.pageAdminArticles(query, filteredArticleIds);
        Map<Long, String> authorNameMap = loadAuthorNames(page.getRecords().stream()
                .map(BlogArticle::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<ArticleAdminVO> records = page.getRecords().stream()
                .map(articleModelConvert::toAdminVO)
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
        validateAuthorArticleQuota(request.getAuthorId());
        BlogArticle article = articleModelConvert.toArticle(request);
        applyArticleFields(article, request, true);
        initializeCounters(article);
        blogArticleRepository.save(article);
        syncCategoryBindings(article.getId(), request.getCategoryIds());
        syncTagBindings(article.getId(), request.getTagIds());
        syncAccessBindings(article.getId(), article.getAccessLevel(), request.getAccessList());
        syncArticleAttachments(article.getId(), article.getContent(), article.getCoverImage());
        eventPublisher.publishEvent(new XpAwardEvent(
                article.getAuthorId(), ExperienceSourceTypeEnum.ARTICLE_PUBLISH.getValue(),
                String.valueOf(article.getId()),
                "article_publish:" + article.getAuthorId() + ":" + article.getId()));
        return buildArticleDetail(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request) {
        validateSaveRequest(request);
        BlogArticle article = getArticleOrThrow(id);
        validateUpdateAllowed(article);
        Long previousAuthorId = article.getAuthorId();
        applyArticleFields(article, request, false);
        blogArticleRepository.updateById(article);
        syncCategoryBindings(id, request.getCategoryIds());
        syncTagBindings(id, request.getTagIds());
        syncAccessBindings(id, article.getAccessLevel(), request.getAccessList());
        syncArticleAttachments(id, article.getContent(), article.getCoverImage());
        if (!Objects.equals(previousAuthorId, article.getAuthorId())) {
            articleSeriesService.cleanupArticleSeriesRelations(id);
        }
        return buildArticleDetail(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BlogArticle article = getArticleOrThrow(id);
        Integer actualStatus = articleStatusMachine.normalizeStatus(status);
        articleStatusMachine.validateSaveState(
                actualStatus,
                article.getReviewStatus(),
                article.getVisibilityScope(),
                article.getAccessLevel(),
                null);
        article.setStatus(actualStatus);
        article.setScheduledPublishTime(null);
        if (Integer.valueOf(1).equals(actualStatus)) {
            article.setPublishTime(LocalDateTime.now());
        }
        blogArticleRepository.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        getArticleOrThrow(id);
        List<SysComment> comments = sysCommentRepository.findByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        List<SysCollection> collections = sysCollectionRepository.listByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);

        articleSeriesService.cleanupArticleSeriesRelations(id);
        blogArticleAccessRepository.removeByArticleId(id);
        blogArticleCategoryRepository.removeByArticleId(id);
        sysTagRelationRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        cleanupArticleAttachments(id);
        cleanupCommentRelations(comments);
        sysCommentRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        sysCollectionRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        refreshCollectionFolderCounts(collections);
        sysInteractionRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        sysUserFootprintRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        blogArticleRepository.removeById(id);
    }

    // ==================== private helpers ====================

    ArticleDetailVO buildArticleDetail(BlogArticle article) {
        ArticleDetailVO detailVO = articleModelConvert.toDetailVO(article);
        detailVO.setAuthorName(loadAuthorName(article.getAuthorId()));
        detailVO.setCategoryIds(listCategoryIds(article.getId()));
        detailVO.setTagIds(listTagIds(article.getId()));
        detailVO.setAccessList(articleAccessControlService.listArticleAccesses(article.getId()).stream()
                .map(articleModelConvert::toAccessItem)
                .toList());
        detailVO.setSeriesList(articleSeriesService.listVisibleSeriesSummariesByArticleId(article.getId(), article.getAuthorId()));
        return detailVO;
    }

    private void applyArticleFields(BlogArticle article, ArticleSaveRequest request, boolean creating) {
        LocalDateTime existingPublishTime = article.getPublishTime();
        Integer existingReviewStatus = article.getReviewStatus();
        articleModelConvert.updateArticle(request, article);
        article.setIsTop(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getIsTop(), 0));
        article.setIsRecommend(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getIsRecommend(), 0));
        article.setIsOriginal(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getIsOriginal(), 1));
        article.setAccessLevel(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getAccessLevel(), 0));
        article.setReviewStatus(creating
                ? com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(existingReviewStatus, 0)
                : com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(existingReviewStatus, 0));
        article.setVisibilityScope(articleStatusMachine.normalizeVisibilityScope(article.getVisibilityScope()));
        articleStatusMachine.validateSaveState(
                article.getStatus(),
                article.getReviewStatus(),
                article.getVisibilityScope(),
                article.getAccessLevel(),
                article.getScheduledPublishTime());
        article.setStatus(articleStatusMachine.resolveStatusForSave(article.getStatus(), article.getScheduledPublishTime()));
        article.setPublishTime(articleStatusMachine.resolvePublishTime(
                article.getStatus(),
                request.getPublishTime(),
                existingPublishTime,
                article.getScheduledPublishTime()));
        if (creating && article.getPublishTime() == null && Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(LocalDateTime.now());
        }
    }

    private void validateUpdateAllowed(BlogArticle article) {
        if (article == null) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                Integer.valueOf(1).equals(articleStatusMachine.normalizeReviewStatus(article.getReviewStatus()))
                        && !SecurityUtils.hasAuthority("content:article-review:review"),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.FORBIDDEN,
                "当前文章正在审核中，暂不允许修改");
    }

    private void initializeCounters(BlogArticle article) {
        article.setViewCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getViewCount(), 0));
        article.setLikeCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getLikeCount(), 0));
        article.setCommentCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getCommentCount(), 0));
        article.setCollectCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getCollectCount(), 0));
        article.setShareCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getShareCount(), 0));
    }

    private void validateSaveRequest(ArticleSaveRequest request) {
        validateAuthor(request.getAuthorId());
        articleStatusMachine.validateSaveState(
                com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getStatus(), 0),
                0,
                request.getVisibilityScope(),
                com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getAccessLevel(), 0),
                request.getScheduledPublishTime());

        ExceptionThrowerCore.throwBusinessIf(
                Integer.valueOf(0).equals(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getIsOriginal(), 1))
                        && !StringUtils.hasText(request.getSourceUrl()),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT,
                "转载文章必须提供原文链接");

        List<Long> categoryIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getCategoryIds(),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT,
                "分类ID不能为空",
                "分类ID存在重复值");
        List<Long> tagIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getTagIds(),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT,
                "标签ID不能为空",
                "标签ID存在重复值");
        request.setCategoryIds(categoryIds);
        request.setTagIds(tagIds);
        validateCategories(categoryIds);
        validateTags(tagIds);
        boolean requireAccessList = ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(articleStatusMachine.normalizeVisibilityScope(request.getVisibilityScope()))
                || Integer.valueOf(4).equals(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getAccessLevel(), 0));
        ExceptionThrowerCore.throwBusinessIf(
                requireAccessList && CollectionUtils.isEmpty(request.getAccessList()),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文章必须配置访问授权列表");
        articleAccessManageService.validateAccessItems(request.getAccessList());
    }

    private void validateAuthor(Long authorId) {
        ExceptionThrowerCore.throwBusinessIfNull(authorId, com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT, "作者不能为空");
        SysUser author = sysUserRepository.getById(authorId);
        ExceptionThrowerCore.throwBusinessIf(author == null || Integer.valueOf(1).equals(author.getDeletedFlag()),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.USER_NOT_FOUND, "作者不存在");
    }

    private void validateCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<SysCategory> categories = sysCategoryRepository.listByTypeAndIds(TARGET_TYPE_ARTICLE, categoryIds);
        ExceptionThrowerCore.throwBusinessIf(categories.size() != categoryIds.size(),
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT, "分类不存在或不属于文章分类");
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTag> tags = sysTagRepository.listByIds(tagIds);
        ExceptionThrowerCore.throwBusinessIf(tags.size() != tagIds.size(), com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT, "标签不存在");
    }

    private void validateAuthorArticleQuota(Long authorId) {
        if (authorId == null) {
            return;
        }
        boolean authorRole = authorPermissionService.hasAuthorRole(authorId);
        int maxArticleCount = resolveArticleQuota(authorRole);
        if (maxArticleCount <= 0) {
            return;
        }
        long currentArticleCount = blogArticleRepository.countByAuthorId(authorId);
        ExceptionThrowerCore.throwBusinessIf(
                currentArticleCount >= maxArticleCount,
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT,
                authorRole
                        ? "当前作者文章数量已达上限，请先整理现有内容后再创建"
                        : "当前用户文章数量已达普通用户上限，申请作者后可获得更高配额"
        );
    }

    private int resolveArticleQuota(boolean authorRole) {
        String configKey = authorRole
                ? ConfigConstants.ARTICLE_MAX_COUNT_AUTHOR_KEY
                : ConfigConstants.ARTICLE_MAX_COUNT_NORMAL_USER_KEY;
        int defaultValue = authorRole
                ? ConfigConstants.DEFAULT_ARTICLE_MAX_COUNT_AUTHOR
                : ConfigConstants.DEFAULT_ARTICLE_MAX_COUNT_NORMAL_USER;
        String configuredValue = sysConfigService.getValueOrDefault(configKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(configuredValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void syncCategoryBindings(Long articleId, List<Long> categoryIds) {
        blogArticleCategoryRepository.removeByArticleId(articleId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<BlogArticleCategory> relations = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            relations.add(articleModelConvert.toArticleCategory(articleId, categoryIds.get(i), i + 1));
        }
        blogArticleCategoryRepository.saveBatch(relations);
    }

    private void syncTagBindings(Long articleId, List<Long> tagIds) {
        sysTagRelationRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, articleId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTagRelation> relations = new ArrayList<>();
        for (Long tagId : tagIds) {
            relations.add(articleModelConvert.toTagRelation(tagId, articleId, TARGET_TYPE_ARTICLE));
        }
        sysTagRelationRepository.saveBatch(relations);
    }

    private void syncAccessBindings(Long articleId, Integer accessLevel, List<ArticleAccessItem> accessList) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        boolean useAccessList = Integer.valueOf(4).equals(accessLevel)
                || articleAccessManageService.supportsAccessList(article);
        if (!useAccessList) {
            blogArticleAccessRepository.removeByArticleId(articleId);
            return;
        }
        articleAccessManageService.rebuildArticleAccessBindings(articleId, accessList);
    }

    private List<Long> listCategoryIds(Long articleId) {
        return blogArticleCategoryRepository.listByArticleIdOrdered(articleId)
                .stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
    }

    private List<Long> listTagIds(Long articleId) {
        return sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, articleId);
    }

    private Set<Long> resolveArticleIdsByRelations(ArticleAdminPageQuery query) {
        Set<Long> ids = null;
        if (query.getCategoryId() != null) {
            Set<Long> categoryIds = blogArticleCategoryRepository.listArticleIdsByCategoryId(query.getCategoryId())
                    .stream()
                    .map(BlogArticleCategory::getArticleId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ids = categoryIds;
        }
        if (query.getTagId() != null) {
            Set<Long> tagIds = sysTagRelationRepository.listTargetIdsByTargetTypeAndTagId(TARGET_TYPE_ARTICLE, query.getTagId())
                    .stream()
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

    private void cleanupCommentRelations(List<SysComment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return;
        }
        List<Long> commentIds = comments.stream()
                .map(SysComment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (commentIds.isEmpty()) {
            return;
        }
        sysInteractionRepository.removeByTargetTypeAndTargetIds("comment", commentIds);
    }

    private void refreshCollectionFolderCounts(List<SysCollection> collections) {
        if (CollectionUtils.isEmpty(collections)) {
            return;
        }
        Set<Long> folderIds = collections.stream()
                .map(SysCollection::getFolderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long folderId : folderIds) {
            SysCollectionFolder folder = sysCollectionFolderRepository.getById(folderId);
            if (folder == null) {
                continue;
            }
            long count = sysCollectionRepository.countByFolderId(folderId);
            folder.setCollectionCount((int) count);
            sysCollectionFolderRepository.updateById(folder);
        }
    }

    private void cleanupArticleAttachments(Long articleId) {
        List<FileBusinessInfo> references = fileBusinessInfoRepository.listByReferenceTypeAndReferenceId("article_attachment", articleId);
        if (CollectionUtils.isEmpty(references)) {
            return;
        }
        Set<Long> fileIds = references.stream()
                .map(FileBusinessInfo::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        fileBusinessInfoRepository.removeByIds(references.stream().map(FileBusinessInfo::getId).toList());
        for (Long fileId : fileIds) {
            fileLifecycleService.syncFileAfterReferenceRemoval(fileId);
        }
    }

    private void syncArticleAttachments(Long articleId, String content, String coverImage) {
        Set<String> imageUrls = extractImageUrls(content);
        if (StringUtils.hasText(coverImage)) {
            imageUrls.add(coverImage.trim());
        }
        Set<Long> newFileIds = imageUrls.isEmpty()
                ? Set.of()
                : fileInfoRepository.listByFileUrls(imageUrls).stream()
                        .map(FileInfo::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<FileBusinessInfo> existingRefs = fileBusinessInfoRepository
                .listByReferenceTypeAndReferenceId("article_attachment", articleId);
        Set<Long> existingFileIds = existingRefs.stream()
                .map(FileBusinessInfo::getFileId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> toAdd = new LinkedHashSet<>(newFileIds);
        toAdd.removeAll(existingFileIds);
        Set<Long> toRemove = new LinkedHashSet<>(existingFileIds);
        toRemove.removeAll(newFileIds);

        if (!toAdd.isEmpty()) {
            Long userId = SecurityUtils.getUserId();
            List<FileBusinessInfo> addRefs = toAdd.stream()
                    .map(fileId -> {
                        FileBusinessInfo ref = new FileBusinessInfo();
                        ref.setFileId(fileId);
                        ref.setUserId(userId);
                        ref.setReferenceType("article_attachment");
                        ref.setReferenceId(articleId);
                        return ref;
                    })
                    .toList();
            fileBusinessInfoRepository.saveBatch(addRefs);
        }

        if (!toRemove.isEmpty()) {
            List<FileBusinessInfo> removeRefs = existingRefs.stream()
                    .filter(ref -> toRemove.contains(ref.getFileId()))
                    .toList();
            fileBusinessInfoRepository.removeByIds(removeRefs.stream().map(FileBusinessInfo::getId).toList());
            for (Long fileId : toRemove) {
                fileLifecycleService.syncFileAfterReferenceRemoval(fileId);
            }
        }
    }

    private Set<String> extractImageUrls(String content) {
        if (!StringUtils.hasText(content)) {
            return Set.of();
        }
        Set<String> urls = new LinkedHashSet<>();
        var mdPattern = java.util.regex.Pattern.compile("!\\[.*?\\]\\(([^)]+)\\)");
        var mdMatcher = mdPattern.matcher(content);
        while (mdMatcher.find()) {
            urls.add(mdMatcher.group(1).trim());
        }
        var htmlPattern = java.util.regex.Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");
        var htmlMatcher = htmlPattern.matcher(content);
        while (htmlMatcher.find()) {
            urls.add(htmlMatcher.group(1).trim());
        }
        return urls;
    }

    BlogArticle getArticleOrThrow(Long id) {
        BlogArticle article = blogArticleRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(article, com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        return article;
    }

    private Map<Long, String> loadAuthorNames(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> authorNameMap = new HashMap<>();
        sysUserRepository.listByIds(authorIds).forEach(user -> authorNameMap.put(user.getId(), buildAuthorName(user)));
        return authorNameMap;
    }

    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(authorId);
        return user == null ? null : buildAuthorName(user);
    }

    private String buildAuthorName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}

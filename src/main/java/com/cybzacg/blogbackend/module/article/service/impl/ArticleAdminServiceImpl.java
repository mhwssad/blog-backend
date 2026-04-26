package com.cybzacg.blogbackend.module.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.*;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.*;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.repository.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
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
    private final FileLifecycleService fileLifecycleService;
    private final SysUserRepository sysUserRepository;
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

        Page<BlogArticle> page = blogArticleRepository.pageAdminArticles(query, filteredArticleIds);
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

    /**
     * 查询后台文章详情，并补齐分类、标签和访问授权列表。
     */
    @Override
    public ArticleDetailVO getArticle(Long id) {
        BlogArticle article = getArticleOrThrow(id);
        return buildArticleDetail(article);
    }

    /**
     * 创建文章主体并同步落库分类、标签和访问授权关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO createArticle(ArticleSaveRequest request) {
        validateSaveRequest(request);
        BlogArticle article = articleModelMapper.toArticle(request);
        applyArticleFields(article, request, true);
        initializeCounters(article);
        blogArticleRepository.save(article);
        syncCategoryBindings(article.getId(), request.getCategoryIds());
        syncTagBindings(article.getId(), request.getTagIds());
        syncAccessBindings(article.getId(), article.getAccessLevel(), request.getAccessList());
        return buildArticleDetail(article);
    }

    /**
     * 更新文章主体信息，并按最新请求重建分类、标签和访问授权关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request) {
        validateSaveRequest(request);
        BlogArticle article = getArticleOrThrow(id);
        applyArticleFields(article, request, false);
        blogArticleRepository.updateById(article);
        syncCategoryBindings(id, request.getCategoryIds());
        syncTagBindings(id, request.getTagIds());
        syncAccessBindings(id, article.getAccessLevel(), request.getAccessList());
        return buildArticleDetail(article);
    }

    /**
     * 调整文章发布状态，并在首次发布时补齐发布时间。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        BlogArticle article = getArticleOrThrow(id);
        article.setStatus(status);
        if (Integer.valueOf(1).equals(status) && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        blogArticleRepository.updateById(article);
    }

    /**
     * 为"指定用户可见"的文章重建授权名单。
     */
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
        List<com.cybzacg.blogbackend.domain.SysComment> comments = sysCommentRepository.findByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);
        List<com.cybzacg.blogbackend.domain.SysCollection> collections = sysCollectionRepository.listByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, id);

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
        LocalDateTime existingPublishTime = article.getPublishTime();
        articleModelMapper.updateArticle(request, article);
        article.setIsTop(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getIsTop(), 0));
        article.setIsOriginal(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getIsOriginal(), 1));
        article.setStatus(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getStatus(), 0));
        article.setPublishTime(resolvePublishTime(article.getStatus(), request.getPublishTime(), existingPublishTime));
        article.setAccessLevel(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getAccessLevel(), 0));
        if (creating && article.getPublishTime() == null && Integer.valueOf(1).equals(article.getStatus())) {
            article.setPublishTime(LocalDateTime.now());
        }
    }

    /**
     * 根据目标状态和请求发布时间推导最终发布时间，保证已发布文章始终有稳定的发布时间。
     */
    private LocalDateTime resolvePublishTime(Integer status, LocalDateTime requestPublishTime, LocalDateTime existingPublishTime) {
        if (Integer.valueOf(1).equals(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(status, 0))) {
            return requestPublishTime != null ? requestPublishTime : (existingPublishTime != null ? existingPublishTime : LocalDateTime.now());
        }
        return requestPublishTime;
    }

    /**
     * 初始化文章的统计字段，避免新建文章后计数字段为空。
     */
    private void initializeCounters(BlogArticle article) {
        article.setViewCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getViewCount(), 0));
        article.setLikeCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getLikeCount(), 0));
        article.setCommentCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getCommentCount(), 0));
        article.setCollectCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getCollectCount(), 0));
        article.setShareCount(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(article.getShareCount(), 0));
    }

    /**
     * 校验文章保存请求，包括作者、状态、访问级别、分类标签和授权项合法性。
     */
    private void validateSaveRequest(ArticleSaveRequest request) {
        validateStatus(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getStatus(), 0));
        validateAccessLevel(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getAccessLevel(), 0));
        validateAuthor(request.getAuthorId());

        ExceptionThrowerCore.throwBusinessIf(
                Integer.valueOf(0).equals(com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(request.getIsOriginal(), 1))
                        && !StringUtils.hasText(request.getSourceUrl()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "转载文章必须提供原文链接");

        List<Long> categoryIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getCategoryIds(),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "分类ID不能为空",
                "分类ID存在重复值");
        List<Long> tagIds = IdCollectionUtils.requireUniqueNonNullIds(
                request.getTagIds(),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "标签ID不能为空",
                "标签ID存在重复值");
        request.setCategoryIds(categoryIds);
        request.setTagIds(tagIds);
        validateCategories(categoryIds);
        validateTags(tagIds);
        validateAccessItems(request.getAccessList());
    }

    private void validateAuthor(Long authorId) {
        ExceptionThrowerCore.throwBusinessIfNull(authorId, ResultErrorCode.ILLEGAL_ARGUMENT, "作者不能为空");
        SysUser author = sysUserRepository.getById(authorId);
        ExceptionThrowerCore.throwBusinessIf(author == null || Integer.valueOf(1).equals(author.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND, "作者不存在");
    }

    private void validateCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<SysCategory> categories = sysCategoryRepository.listByTypeAndIds(TARGET_TYPE_ARTICLE, categoryIds);
        ExceptionThrowerCore.throwBusinessIf(categories.size() != categoryIds.size(),
                ResultErrorCode.ILLEGAL_ARGUMENT, "分类不存在或不属于文章分类");
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTag> tags = sysTagRepository.listByIds(tagIds);
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
            Integer accessType = com.cybzacg.blogbackend.utils.CollectionUtils.defaultIfNull(item.getAccessType(), 1);
            ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(accessType) && !Integer.valueOf(2).equals(accessType),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "访问类型非法");
            item.setAccessType(accessType);
            String key = item.getUserId() + ":" + accessType;
            ExceptionThrowerCore.throwBusinessIf(!keys.add(key), ResultErrorCode.ILLEGAL_ARGUMENT, "存在重复的访问授权记录");
            userIds.add(item.getUserId());
        }
        List<SysUser> users = sysUserRepository.listByIds(userIds);
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
     * 以"先删后建"的方式同步文章分类关系，保证顺序和最终状态一致。
     */
    private void syncCategoryBindings(Long articleId, List<Long> categoryIds) {
        blogArticleCategoryRepository.removeByArticleId(articleId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<BlogArticleCategory> relations = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            relations.add(articleModelMapper.toArticleCategory(articleId, categoryIds.get(i), i + 1));
        }
        blogArticleCategoryRepository.saveBatch(relations);
    }

    /**
     * 重建文章标签关系，保持标签绑定结果与请求参数完全一致。
     */
    private void syncTagBindings(Long articleId, List<Long> tagIds) {
        sysTagRelationRepository.removeByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, articleId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<SysTagRelation> relations = new ArrayList<>();
        for (Long tagId : tagIds) {
            relations.add(articleModelMapper.toTagRelation(tagId, articleId, TARGET_TYPE_ARTICLE));
        }
        sysTagRelationRepository.saveBatch(relations);
    }

    /**
     * 根据访问级别决定是否保留指定用户授权数据。
     */
    private void syncAccessBindings(Long articleId, Integer accessLevel, List<ArticleAccessItem> accessList) {
        if (!Integer.valueOf(4).equals(accessLevel)) {
            blogArticleAccessRepository.removeByArticleId(articleId);
            return;
        }
        rebuildAccessBindings(articleId, accessList);
    }

    /**
     * 先清空后重建文章的指定用户授权记录，保持授权结果与请求完全一致。
     */
    private void rebuildAccessBindings(Long articleId, List<ArticleAccessItem> accessList) {
        blogArticleAccessRepository.removeByArticleId(articleId);
        if (accessList == null || accessList.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<BlogArticleAccess> records = accessList.stream()
                .map(item -> articleModelMapper.toArticleAccess(articleId, item, now))
                .toList();
        blogArticleAccessRepository.saveBatch(records);
    }

    /**
     * 读取文章分类 ID 列表，并保持与绑定顺序一致，供后台详情回显复用。
     */
    private List<Long> listCategoryIds(Long articleId) {
        return blogArticleCategoryRepository.listByArticleIdOrdered(articleId)
                .stream()
                .map(BlogArticleCategory::getCategoryId)
                .toList();
    }

    /**
     * 读取文章标签 ID 列表，并保持与绑定顺序一致，供后台详情回显复用。
     */
    private List<Long> listTagIds(Long articleId) {
        return sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId(TARGET_TYPE_ARTICLE, articleId);
    }

    /**
     * 先通过分类和标签反查文章 ID 集合，再交给主查询拼装最终分页条件。
     */
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

    /**
     * 删除文章评论对应的互动记录，避免评论被删后仍残留点赞等关联数据。
     */
    private void cleanupCommentRelations(List<com.cybzacg.blogbackend.domain.SysComment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return;
        }
        List<Long> commentIds = comments.stream()
                .map(com.cybzacg.blogbackend.domain.SysComment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (commentIds.isEmpty()) {
            return;
        }
        sysInteractionRepository.removeByTargetTypeAndTargetIds("comment", commentIds);
    }

    /**
     * 根据本次删除涉及的收藏记录重算收藏夹数量，防止文章移除后收藏夹计数失真。
     */
    private void refreshCollectionFolderCounts(List<com.cybzacg.blogbackend.domain.SysCollection> collections) {
        if (CollectionUtils.isEmpty(collections)) {
            return;
        }
        Set<Long> folderIds = collections.stream()
                .map(com.cybzacg.blogbackend.domain.SysCollection::getFolderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long folderId : folderIds) {
            com.cybzacg.blogbackend.domain.SysCollectionFolder folder = sysCollectionFolderRepository.getById(folderId);
            if (folder == null) {
                continue;
            }
            long count = sysCollectionRepository.countByFolderId(folderId);
            folder.setCollectionCount((int) count);
            sysCollectionFolderRepository.updateById(folder);
        }
    }

    /**
     * 清理文章附件引用，并在引用归零时同步回收物理文件状态。
     */
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


    /**
     * 读取文章，不存在时统一抛出业务异常。
     */
    private BlogArticle getArticleOrThrow(Long id) {
        BlogArticle article = blogArticleRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(article, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        return article;
    }

    /**
     * 批量读取作者展示名，供后台列表回填作者信息。
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
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}

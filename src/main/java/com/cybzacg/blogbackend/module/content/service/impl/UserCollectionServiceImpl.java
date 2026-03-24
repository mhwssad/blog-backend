package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.UserCollectionService;
import com.cybzacg.blogbackend.utils.BeanConverterUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户收藏服务实现。
 *
 * <p>负责用户收藏夹维护、文章收藏创建与删除，以及收藏数量的联动更新。
 */
@Service
@RequiredArgsConstructor
public class UserCollectionServiceImpl implements UserCollectionService {
    private static final String ARTICLE_TYPE = "article";

    private final SysCollectionFolderService sysCollectionFolderService;
    private final SysCollectionService sysCollectionService;
    private final BlogArticleService blogArticleService;
    private final ArticleAccessControlService articleAccessControlService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<CollectionFolderVO> pageFolders() {
        Long userId = SecurityUtils.requireUserId();
        Page<SysCollectionFolder> page = sysCollectionFolderService.page(new Page<>(1, 100),
                new LambdaQueryWrapper<SysCollectionFolder>()
                        .eq(SysCollectionFolder::getUserId, userId)
                        .orderByDesc(SysCollectionFolder::getIsDefault)
                        .orderByAsc(SysCollectionFolder::getSortOrder)
                        .orderByDesc(SysCollectionFolder::getId));
        List<CollectionFolderVO> records = page.getRecords().stream().map(contentModelMapper::toCollectionFolderVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 创建收藏夹，并在需要时调整同类型默认收藏夹的唯一性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionFolderVO createFolder(CollectionFolderSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysCollectionFolder folder = contentModelMapper.toCollectionFolder(request);
        folder.setUserId(userId);
        folder.setFolderType(resolveFolderType(request.getFolderType()));
        folder.setIsPublic(defaultInt(request.getIsPublic(), 0));
        folder.setIsDefault(defaultInt(request.getIsDefault(), 0));
        folder.setSortOrder(defaultInt(request.getSortOrder(), 0));
        folder.setCollectionCount(0);
        if (Integer.valueOf(1).equals(folder.getIsDefault())) {
            unsetDefaultFolder(userId, folder.getFolderType(), null);
        }
        sysCollectionFolderService.save(folder);
        return contentModelMapper.toCollectionFolderVO(folder);
    }

    /**
     * 更新收藏夹基本信息，并维护默认收藏夹标记的一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionFolderVO updateFolder(Long id, CollectionFolderSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysCollectionFolder folder = getFolderOrThrow(id, userId);
        contentModelMapper.updateCollectionFolder(request, folder);
        folder.setFolderType(resolveFolderType(request.getFolderType()));
        folder.setIsPublic(defaultInt(request.getIsPublic(), 0));
        folder.setIsDefault(defaultInt(request.getIsDefault(), 0));
        folder.setSortOrder(defaultInt(request.getSortOrder(), 0));
        if (Integer.valueOf(1).equals(folder.getIsDefault())) {
            unsetDefaultFolder(userId, folder.getFolderType(), folder.getId());
        }
        sysCollectionFolderService.updateById(folder);
        return contentModelMapper.toCollectionFolderVO(folder);
    }

    /**
     * 删除收藏夹及其下收藏记录，同时回退文章收藏计数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysCollectionFolder folder = getFolderOrThrow(id, userId);
        ExceptionThrowerCore.throwBusinessIf(Integer.valueOf(1).equals(folder.getIsDefault()), ResultErrorCode.ILLEGAL_ARGUMENT, "默认收藏夹不可删除");
        List<SysCollection> collections = sysCollectionService.lambdaQuery().eq(SysCollection::getFolderId, id).list();
        if (!collections.isEmpty()) {
            for (SysCollection collection : collections) {
                rollbackArticleCollectCount(collection);
            }
            sysCollectionService.remove(new LambdaQueryWrapper<SysCollection>().eq(SysCollection::getFolderId, id));
        }
        sysCollectionFolderService.removeById(id);
    }

    @Override
    public PageResult<CollectionVO> pageCollections() {
        Long userId = SecurityUtils.requireUserId();
        Page<SysCollection> page = sysCollectionService.page(new Page<>(1, 100),
                new LambdaQueryWrapper<SysCollection>()
                        .eq(SysCollection::getUserId, userId)
                        .orderByDesc(SysCollection::getCreatedAt)
                        .orderByDesc(SysCollection::getId));
        List<CollectionVO> records = page.getRecords().stream().map(contentModelMapper::toUserCollectionVO).toList();
        return PageResult.of(page, records);
    }

    /**
     * 创建文章收藏记录，并同步更新收藏夹数量与文章收藏数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCollection(CollectionSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIf(!ARTICLE_TYPE.equals(request.getTargetType()), ResultErrorCode.ILLEGAL_ARGUMENT, "当前仅支持文章收藏");
        BlogArticle article = blogArticleService.getById(request.getTargetId());
        ExceptionThrowerCore.throwBusinessIf(article == null || !Integer.valueOf(1).equals(article.getStatus()), ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        articleAccessControlService.validateArticleAccess(article, userId);
        Long folderId = request.getFolderId();
        SysCollectionFolder folder = folderId == null ? getOrCreateDefaultFolder(userId, ARTICLE_TYPE) : getFolderOrThrow(folderId, userId);
        boolean exists = sysCollectionService.lambdaQuery()
                .eq(SysCollection::getUserId, userId)
                .eq(SysCollection::getFolderId, folder.getId())
                .eq(SysCollection::getTargetId, request.getTargetId())
                .eq(SysCollection::getTargetType, ARTICLE_TYPE)
                .exists();
        if (exists) {
            return;
        }
        SysCollection collection = BeanConverterUtils.convert(request, SysCollection::new);
        collection.setUserId(userId);
        collection.setFolderId(folder.getId());
        collection.setTargetId(article.getId());
        collection.setTargetType(ARTICLE_TYPE);
        collection.setRemark(StrUtils.normalize(request.getRemark()));
        collection.setTargetTitle(article.getTitle());
        collection.setTargetUrl("/article/" + article.getId());
        sysCollectionService.save(collection);
        folder.setCollectionCount((folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) + 1);
        sysCollectionFolderService.updateById(folder);
        article.setCollectCount((article.getCollectCount() == null ? 0 : article.getCollectCount()) + 1);
        blogArticleService.updateById(article);
    }

    /**
     * 删除单条收藏记录，并回退收藏夹与文章维度的统计值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysCollection collection = sysCollectionService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(collection == null || !userId.equals(collection.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "收藏记录不存在");
        SysCollectionFolder folder = sysCollectionFolderService.getById(collection.getFolderId());
        if (folder != null) {
            folder.setCollectionCount(Math.max(0, (folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) - 1));
            sysCollectionFolderService.updateById(folder);
        }
        rollbackArticleCollectCount(collection);
        sysCollectionService.removeById(id);
    }

    /**
     * 获取默认收藏夹，不存在时自动创建一份系统默认夹。
     */
    private SysCollectionFolder getOrCreateDefaultFolder(Long userId, String folderType) {
        SysCollectionFolder folder = sysCollectionFolderService.lambdaQuery()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1)
                .one();
        if (folder != null) {
            return folder;
        }
        SysCollectionFolder created = new SysCollectionFolder();
        created.setUserId(userId);
        created.setFolderName("默认收藏夹");
        created.setFolderType(folderType);
        created.setDescription("系统自动创建的默认收藏夹");
        created.setIsPublic(0);
        created.setIsDefault(1);
        created.setSortOrder(0);
        created.setCollectionCount(0);
        unsetDefaultFolder(userId, folderType, null);
        sysCollectionFolderService.save(created);
        return created;
    }

    /**
     * 将同类型的其他默认收藏夹取消默认标记，确保默认夹唯一。
     */
    private void unsetDefaultFolder(Long userId, String folderType, Long keepId) {
        List<SysCollectionFolder> folders = sysCollectionFolderService.lambdaQuery()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1)
                .list();
        for (SysCollectionFolder existing : folders) {
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            existing.setIsDefault(0);
            sysCollectionFolderService.updateById(existing);
        }
    }

    private SysCollectionFolder getFolderOrThrow(Long id, Long userId) {
        SysCollectionFolder folder = sysCollectionFolderService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(folder == null || !userId.equals(folder.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "收藏夹不存在");
        return folder;
    }

    /**
     * 删除收藏记录时回退文章收藏数，避免计数不一致。
     */
    private void rollbackArticleCollectCount(SysCollection collection) {
        if (!ARTICLE_TYPE.equals(collection.getTargetType())) {
            return;
        }
        BlogArticle article = blogArticleService.getById(collection.getTargetId());
        if (article != null) {
            article.setCollectCount(Math.max(0, (article.getCollectCount() == null ? 0 : article.getCollectCount()) - 1));
            blogArticleService.updateById(article);
        }
    }

    private String resolveFolderType(String folderType) {
        return StrUtils.trimToDefault(folderType, ARTICLE_TYPE);
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

}









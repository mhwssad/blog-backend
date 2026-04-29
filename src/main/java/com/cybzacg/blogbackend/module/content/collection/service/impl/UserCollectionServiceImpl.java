package com.cybzacg.blogbackend.module.content.collection.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.collection.service.UserCollectionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
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

    private final SysCollectionFolderRepository sysCollectionFolderRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final ArticleContentFacadeService articleContentFacadeService;
    private final ContentModelMapper contentModelMapper;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 分页查询当前用户的收藏夹列表，按默认夹与排序字段排列。
     */
    @Override
    public PageResult<CollectionFolderVO> pageFolders() {
        Long userId = SecurityUtils.requireUserId();
        Page<SysCollectionFolder> page = sysCollectionFolderRepository.pageByUserIdOrderByDefaultAndSort(userId, 1, 100);
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
        sysCollectionFolderRepository.save(folder);
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
        sysCollectionFolderRepository.updateById(folder);
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
        List<SysCollection> collections = sysCollectionRepository.findByFolderId(id);
        if (!collections.isEmpty()) {
            for (SysCollection collection : collections) {
                rollbackArticleCollectCount(collection);
            }
            sysCollectionRepository.removeByFolderId(id);
        }
        sysCollectionFolderRepository.removeById(id);
    }

    /**
     * 分页查询当前用户的收藏记录列表。
     */
    @Override
    public PageResult<CollectionVO> pageCollections() {
        Long userId = SecurityUtils.requireUserId();
        Page<SysCollection> page = sysCollectionRepository.pageByUserId(userId, 1, 100);
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
        BlogArticle article = articleContentFacadeService.requireInteractableArticle(request.getTargetId(), userId, "收藏");
        Long folderId = request.getFolderId();
        SysCollectionFolder folder = folderId == null ? getOrCreateDefaultFolder(userId, ARTICLE_TYPE) : getFolderOrThrow(folderId, userId);
        boolean exists = sysCollectionRepository.existsByUserIdAndFolderIdAndTargetIdAndTargetType(
                userId,
                folder.getId(),
                request.getTargetId(),
                ARTICLE_TYPE);
        if (exists) {
            return;
        }
        SysCollection collection = contentModelMapper.toCollection(request, userId, folder.getId(), article);
        sysCollectionRepository.save(collection);
        folder.setCollectionCount((folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) + 1);
        sysCollectionFolderRepository.updateById(folder);
        articleContentFacadeService.adjustCollectCount(article.getId(), 1);
        if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            notificationDeliveryService.deliverAfterCommit(
                    article.getAuthorId(),
                    NotificationTypeEnum.COLLECT_ARTICLE,
                    "你的文章被收藏了",
                    "《" + article.getTitle() + "》被新的用户收藏",
                    userId);
        }
    }

    /**
     * 删除单条收藏记录，并回退收藏夹与文章维度的统计值。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long id) {
        Long userId = SecurityUtils.requireUserId();
        SysCollection collection = sysCollectionRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(collection == null || !userId.equals(collection.getUserId()), ResultErrorCode.ILLEGAL_ARGUMENT, "收藏记录不存在");
        SysCollectionFolder folder = sysCollectionFolderRepository.getById(collection.getFolderId());
        if (folder != null) {
            folder.setCollectionCount(Math.max(0, (folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) - 1));
            sysCollectionFolderRepository.updateById(folder);
        }
        rollbackArticleCollectCount(collection);
        sysCollectionRepository.removeById(id);
    }

    /**
     * 获取默认收藏夹，不存在时自动创建一份系统默认夹。
     */
    private SysCollectionFolder getOrCreateDefaultFolder(Long userId, String folderType) {
        SysCollectionFolder folder = sysCollectionFolderRepository.findDefaultByUserIdAndFolderType(userId, folderType);
        if (folder != null) {
            return folder;
        }
        SysCollectionFolder created = contentModelMapper.toDefaultCollectionFolder(userId, folderType);
        unsetDefaultFolder(userId, folderType, null);
        sysCollectionFolderRepository.save(created);
        return created;
    }

    /**
     * 将同类型的其他默认收藏夹取消默认标记，确保默认夹唯一。
     */
    private void unsetDefaultFolder(Long userId, String folderType, Long keepId) {
        List<SysCollectionFolder> folders = sysCollectionFolderRepository.findDefaultsByUserIdAndFolderType(userId, folderType);
        for (SysCollectionFolder existing : folders) {
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            existing.setIsDefault(0);
            sysCollectionFolderRepository.updateById(existing);
        }
    }

    private SysCollectionFolder getFolderOrThrow(Long id, Long userId) {
        SysCollectionFolder folder = sysCollectionFolderRepository.getById(id);
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
        articleContentFacadeService.adjustCollectCount(collection.getTargetId(), -1);
    }

    private String resolveFolderType(String folderType) {
        return StrUtils.trimToDefault(folderType, ARTICLE_TYPE);
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

}






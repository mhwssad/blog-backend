package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.service.CollectionAdminService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionAdminServiceImpl implements CollectionAdminService {
    private final SysCollectionFolderService sysCollectionFolderService;
    private final SysCollectionService sysCollectionService;
    private final BlogArticleService blogArticleService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<CollectionFolderVO> pageFolders(CollectionPageQuery query) {
        LambdaQueryWrapper<SysCollectionFolder> wrapper = new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(query.getUserId() != null, SysCollectionFolder::getUserId, query.getUserId())
                .eq(query.getTargetType() != null, SysCollectionFolder::getFolderType, query.getTargetType())
                .orderByDesc(SysCollectionFolder::getUpdatedAt)
                .orderByDesc(SysCollectionFolder::getId);
        Page<SysCollectionFolder> page = sysCollectionFolderService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<CollectionFolderVO> records = page.getRecords().stream()
                .map(contentModelMapper::toCollectionFolderVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public PageResult<CollectionVO> pageCollections(CollectionPageQuery query) {
        LambdaQueryWrapper<SysCollection> wrapper = new LambdaQueryWrapper<SysCollection>()
                .eq(query.getUserId() != null, SysCollection::getUserId, query.getUserId())
                .eq(query.getFolderId() != null, SysCollection::getFolderId, query.getFolderId())
                .eq(query.getTargetId() != null, SysCollection::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysCollection::getTargetType, query.getTargetType())
                .orderByDesc(SysCollection::getCreatedAt)
                .orderByDesc(SysCollection::getId);
        Page<SysCollection> page = sysCollectionService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<CollectionVO> records = page.getRecords().stream()
                .map(contentModelMapper::toAdminCollectionVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long id) {
        SysCollection collection = sysCollectionService.getById(id);
        if (collection == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "收藏记录不存在");
        }
        SysCollectionFolder folder = sysCollectionFolderService.getById(collection.getFolderId());
        if (folder != null) {
            folder.setCollectionCount(Math.max(0, (folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) - 1));
            sysCollectionFolderService.updateById(folder);
        }
        if ("article".equals(collection.getTargetType())) {
            BlogArticle article = blogArticleService.getById(collection.getTargetId());
            if (article != null) {
                article.setCollectCount(Math.max(0, (article.getCollectCount() == null ? 0 : article.getCollectCount()) - 1));
                blogArticleService.updateById(article);
            }
        }
        sysCollectionService.removeById(id);
    }
}

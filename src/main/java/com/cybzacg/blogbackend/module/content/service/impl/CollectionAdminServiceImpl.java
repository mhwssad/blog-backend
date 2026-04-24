package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.service.CollectionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionAdminServiceImpl implements CollectionAdminService {
    private final SysCollectionFolderRepository sysCollectionFolderRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final BlogArticleRepository blogArticleService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<CollectionFolderVO> pageFolders(CollectionPageQuery query) {
        Page<SysCollectionFolder> page = sysCollectionFolderRepository.pageByAdminConditions(query);
        List<CollectionFolderVO> records = page.getRecords().stream()
                .map(contentModelMapper::toCollectionFolderVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public PageResult<CollectionVO> pageCollections(CollectionPageQuery query) {
        Page<SysCollection> page = sysCollectionRepository.pageByAdminConditions(query);
        List<CollectionVO> records = page.getRecords().stream()
                .map(contentModelMapper::toAdminCollectionVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long id) {
        SysCollection collection = sysCollectionRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(collection, ResultErrorCode.ILLEGAL_ARGUMENT, "收藏记录不存在");
        SysCollectionFolder folder = sysCollectionFolderRepository.getById(collection.getFolderId());
        if (folder != null) {
            folder.setCollectionCount(Math.max(0, (folder.getCollectionCount() == null ? 0 : folder.getCollectionCount()) - 1));
            sysCollectionFolderRepository.updateById(folder);
        }
        if ("article".equals(collection.getTargetType())) {
            BlogArticle article = blogArticleService.getById(collection.getTargetId());
            if (article != null) {
                article.setCollectCount(Math.max(0, (article.getCollectCount() == null ? 0 : article.getCollectCount()) - 1));
                blogArticleService.updateById(article);
            }
        }
        sysCollectionRepository.removeById(id);
    }
}

package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.mapper.SysCollectionFolderMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionFolderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收藏夹 Repository 实现。
 */
@Repository
public class SysCollectionFolderRepositoryImpl extends ServiceImpl<SysCollectionFolderMapper, SysCollectionFolder>
        implements SysCollectionFolderRepository {

    @Override
    public Page<SysCollectionFolder> pageByAdminConditions(CollectionPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(query.getUserId() != null, SysCollectionFolder::getUserId, query.getUserId())
                .eq(query.getTargetType() != null, SysCollectionFolder::getFolderType, query.getTargetType())
                .orderByDesc(SysCollectionFolder::getUpdatedAt)
                .orderByDesc(SysCollectionFolder::getId));
    }

    @Override
    public Page<SysCollectionFolder> pageByUserIdOrderByDefaultAndSort(Long userId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .orderByDesc(SysCollectionFolder::getIsDefault)
                .orderByAsc(SysCollectionFolder::getSortOrder)
                .orderByDesc(SysCollectionFolder::getId));
    }

    @Override
    public SysCollectionFolder findDefaultByUserIdAndFolderType(Long userId, String folderType) {
        return getOne(new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1));
    }

    @Override
    public List<SysCollectionFolder> findDefaultsByUserIdAndFolderType(Long userId, String folderType) {
        return list(new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1));
    }
}

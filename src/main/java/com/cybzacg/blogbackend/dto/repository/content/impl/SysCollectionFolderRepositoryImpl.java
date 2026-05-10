package com.cybzacg.blogbackend.module.content.collection.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.content.SysCollectionFolder;
import com.cybzacg.blogbackend.dto.mapper.content.SysCollectionFolderMapper;
import com.cybzacg.blogbackend.module.content.collection.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收藏夹 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供收藏夹数据的增删改查。
 */
@Repository
public class SysCollectionFolderRepositoryImpl extends ServiceImpl<SysCollectionFolderMapper, SysCollectionFolder>
        implements SysCollectionFolderRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysCollectionFolder> pageByAdminConditions(CollectionPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(query.getUserId() != null, SysCollectionFolder::getUserId, query.getUserId())
                .eq(query.getTargetType() != null, SysCollectionFolder::getFolderType, query.getTargetType())
                .orderByDesc(SysCollectionFolder::getUpdatedAt)
                .orderByDesc(SysCollectionFolder::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysCollectionFolder> pageByUserIdOrderByDefaultAndSort(Long userId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .orderByDesc(SysCollectionFolder::getIsDefault)
                .orderByAsc(SysCollectionFolder::getSortOrder)
                .orderByDesc(SysCollectionFolder::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysCollectionFolder findDefaultByUserIdAndFolderType(Long userId, String folderType) {
        return getOne(new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCollectionFolder> findDefaultsByUserIdAndFolderType(Long userId, String folderType) {
        return list(new LambdaQueryWrapper<SysCollectionFolder>()
                .eq(SysCollectionFolder::getUserId, userId)
                .eq(SysCollectionFolder::getFolderType, folderType)
                .eq(SysCollectionFolder::getIsDefault, 1));
    }

    @Override
    public void incrementCollectionCount(Long id, int delta) {
        baseMapper.incrementCollectionCount(id, delta);
    }
}

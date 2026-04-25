package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.mapper.SysCollectionMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收藏 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供用户收藏记录的增删改查。
 */
@Repository
public class SysCollectionRepositoryImpl extends ServiceImpl<SysCollectionMapper, SysCollection>
        implements SysCollectionRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysCollection> pageByAdminConditions(CollectionPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysCollection>()
                .eq(query.getUserId() != null, SysCollection::getUserId, query.getUserId())
                .eq(query.getFolderId() != null, SysCollection::getFolderId, query.getFolderId())
                .eq(query.getTargetId() != null, SysCollection::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysCollection::getTargetType, query.getTargetType())
                .orderByDesc(SysCollection::getCreatedAt)
                .orderByDesc(SysCollection::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysCollection> pageByUserId(Long userId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getUserId, userId)
                .orderByDesc(SysCollection::getCreatedAt)
                .orderByDesc(SysCollection::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCollection> findByFolderId(Long folderId) {
        return list(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getFolderId, folderId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByFolderId(Long folderId) {
        return remove(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getFolderId, folderId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndFolderIdAndTargetIdAndTargetType(Long userId,
                                                                     Long folderId,
                                                                     Long targetId,
                                                                     String targetType) {
        return exists(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getUserId, userId)
                .eq(SysCollection::getFolderId, folderId)
                .eq(SysCollection::getTargetId, targetId)
                .eq(SysCollection::getTargetType, targetType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCollection> listByTargetTypeAndTargetId(String targetType, Long targetId) {
        return list(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getTargetType, targetType)
                .eq(SysCollection::getTargetId, targetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getTargetType, targetType)
                .eq(SysCollection::getTargetId, targetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByFolderId(Long folderId) {
        return count(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getFolderId, folderId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId) {
        return exists(new LambdaQueryWrapper<SysCollection>()
                .eq(SysCollection::getUserId, userId)
                .eq(SysCollection::getTargetType, targetType)
                .eq(SysCollection::getTargetId, targetId));
    }
}

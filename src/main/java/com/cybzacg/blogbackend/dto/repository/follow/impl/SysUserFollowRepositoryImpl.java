package com.cybzacg.blogbackend.module.follow.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.follow.SysUserFollow;
import com.cybzacg.blogbackend.dto.mapper.follow.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.data.FollowAdminRelationItem;
import com.cybzacg.blogbackend.module.follow.model.data.FollowRelationUserItem;
import com.cybzacg.blogbackend.module.follow.model.data.PublicFollowUserItem;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户关注关系 Repository 实现。
 */
@Repository
public class SysUserFollowRepositoryImpl extends ServiceImpl<SysUserFollowMapper, SysUserFollow>
        implements SysUserFollowRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public SysUserFollow findByFollowerAndFollowing(Long followerId, Long followingId) {
        return getOne(Wrappers.lambdaQuery(SysUserFollow.class)
                .eq(SysUserFollow::getFollowerId, followerId)
                .eq(SysUserFollow::getFollowingId, followingId)
                .last("limit 1"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countFollowPage(Long userId, Boolean specialOnly) {
        return baseMapper.countFollowPage(userId, specialOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FollowRelationUserItem> selectFollowPage(Long userId, Boolean specialOnly, Long offset, Long size) {
        return baseMapper.selectFollowPage(userId, specialOnly, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countFanPage(Long userId) {
        return baseMapper.countFanPage(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FollowRelationUserItem> selectFanPage(Long userId, Long offset, Long size) {
        return baseMapper.selectFanPage(userId, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countActiveRelation(Long followerId, Long followingId) {
        return baseMapper.countActiveRelation(followerId, followingId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countActiveFollowing(Long userId) {
        return baseMapper.countActiveFollowing(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countActiveFans(Long userId) {
        return baseMapper.countActiveFans(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countPublicFollowPage(Long userId) {
        return baseMapper.countPublicFollowPage(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PublicFollowUserItem> selectPublicFollowPage(Long userId, Long offset, Long size) {
        return baseMapper.selectPublicFollowPage(userId, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countPublicFanPage(Long userId) {
        return baseMapper.countPublicFanPage(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PublicFollowUserItem> selectPublicFanPage(Long userId, Long offset, Long size) {
        return baseMapper.selectPublicFanPage(userId, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countAdminRelationPage(FollowAdminPageQuery query) {
        return baseMapper.countAdminRelationPage(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FollowAdminRelationItem> selectAdminRelationPage(FollowAdminPageQuery query, Long offset, Long size) {
        return baseMapper.selectAdminRelationPage(query, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countCleanableRelations(boolean cleanInactive, boolean cleanDeletedUsers, boolean cleanDisabledUsers) {
        return baseMapper.countCleanableRelations(cleanInactive, cleanDeletedUsers, cleanDisabledUsers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteCleanableRelations(boolean cleanInactive, boolean cleanDeletedUsers, boolean cleanDisabledUsers) {
        return baseMapper.deleteCleanableRelations(cleanInactive, cleanDeletedUsers, cleanDisabledUsers);
    }
}

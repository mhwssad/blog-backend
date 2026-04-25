package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.mapper.SysUserFootprintMapper;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.repository.SysUserFootprintRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户足迹 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供用户浏览足迹数据的增删改查。
 */
@Repository
public class SysUserFootprintRepositoryImpl extends ServiceImpl<SysUserFootprintMapper, SysUserFootprint>
        implements SysUserFootprintRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysUserFootprint> pageByAdminConditions(FootprintPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), buildAdminWrapper(query)
                .orderByDesc(SysUserFootprint::getVisitedAt)
                .orderByDesc(SysUserFootprint::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysUserFootprint> pageByUserIdAndTargetType(Long userId, String targetType, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysUserFootprint>()
                .eq(SysUserFootprint::getUserId, userId)
                .eq(targetType != null, SysUserFootprint::getTargetType, targetType)
                .orderByDesc(SysUserFootprint::getVisitedAt)
                .orderByDesc(SysUserFootprint::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByUserId(Long userId) {
        return remove(new LambdaQueryWrapper<SysUserFootprint>()
                .eq(SysUserFootprint::getUserId, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByAdminConditions(FootprintPageQuery query) {
        return remove(buildAdminWrapper(query));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int upsertFootprint(SysUserFootprint footprint) {
        return baseMapper.upsertFootprint(footprint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysUserFootprint>()
                .eq(SysUserFootprint::getTargetType, targetType)
                .eq(SysUserFootprint::getTargetId, targetId));
    }

    /**
     * 构建后台足迹筛选条件，统一收口管理端分页与批量删除的查询口径。
     *
     * @param query 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<SysUserFootprint> buildAdminWrapper(FootprintPageQuery query) {
        return new LambdaQueryWrapper<SysUserFootprint>()
                .eq(query.getUserId() != null, SysUserFootprint::getUserId, query.getUserId())
                .eq(query.getTargetId() != null, SysUserFootprint::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysUserFootprint::getTargetType, query.getTargetType())
                .ge(query.getVisitedAtStart() != null, SysUserFootprint::getVisitedAt, query.getVisitedAtStart())
                .le(query.getVisitedAtEnd() != null, SysUserFootprint::getVisitedAt, query.getVisitedAtEnd());
    }
}

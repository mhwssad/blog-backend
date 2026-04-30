package com.cybzacg.blogbackend.module.content.taxonomy.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.content.SysTagRelation;
import com.cybzacg.blogbackend.mapper.content.SysTagRelationMapper;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRelationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签关联 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供标签与目标实体关联关系的增删改查。
 */
@Repository
public class SysTagRelationRepositoryImpl extends ServiceImpl<SysTagRelationMapper, SysTagRelation>
        implements SysTagRelationRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByTagId(Long tagId) {
        return exists(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTagId, tagId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTagId(Long tagId) {
        return remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTagId, tagId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, targetType)
                .eq(SysTagRelation::getTargetId, targetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> listTargetIdsByTargetTypeAndTagId(String targetType, Long tagId) {
        return list(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, targetType)
                .eq(SysTagRelation::getTagId, tagId)
                .orderByAsc(SysTagRelation::getId)).stream()
                .map(SysTagRelation::getTargetId)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> listTagIdsByTargetTypeAndTargetId(String targetType, Long targetId) {
        return list(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, targetType)
                .eq(SysTagRelation::getTargetId, targetId)
                .orderByAsc(SysTagRelation::getId)).stream()
                .map(SysTagRelation::getTagId)
                .collect(Collectors.toList());
    }
}

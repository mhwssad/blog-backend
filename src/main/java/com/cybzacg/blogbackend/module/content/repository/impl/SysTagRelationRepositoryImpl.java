package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.mapper.SysTagRelationMapper;
import com.cybzacg.blogbackend.module.content.repository.SysTagRelationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签关联 Repository 实现。
 */
@Repository
public class SysTagRelationRepositoryImpl extends ServiceImpl<SysTagRelationMapper, SysTagRelation>
        implements SysTagRelationRepository {

    @Override
    public boolean existsByTagId(Long tagId) {
        return exists(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTagId, tagId));
    }

    @Override
    public boolean removeByTagId(Long tagId) {
        return remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTagId, tagId));
    }

    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, targetType)
                .eq(SysTagRelation::getTargetId, targetId));
    }

    @Override
    public List<Long> listTargetIdsByTargetTypeAndTagId(String targetType, Long tagId) {
        return list(new LambdaQueryWrapper<SysTagRelation>()
                .eq(SysTagRelation::getTargetType, targetType)
                .eq(SysTagRelation::getTagId, tagId)
                .orderByAsc(SysTagRelation::getId)).stream()
                .map(SysTagRelation::getTargetId)
                .collect(Collectors.toList());
    }

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

package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.mapper.SysTagRelationMapper;
import com.cybzacg.blogbackend.module.content.repository.SysTagRelationRepository;
import org.springframework.stereotype.Repository;

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
}

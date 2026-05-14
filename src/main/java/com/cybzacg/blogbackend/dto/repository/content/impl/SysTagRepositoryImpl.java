package com.cybzacg.blogbackend.dto.repository.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.dto.mapper.content.SysTagMapper;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标签 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供标签数据的增删改查。
 */
@Repository
public class SysTagRepositoryImpl extends ServiceImpl<SysTagMapper, SysTag> implements SysTagRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysTag> findAllOrderByIdDesc() {
        return list(new LambdaQueryWrapper<SysTag>()
                .orderByDesc(SysTag::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByNameExcludingId(String name, Long excludeId) {
        return exists(new LambdaQueryWrapper<SysTag>()
                .eq(SysTag::getName, name)
                .ne(excludeId != null, SysTag::getId, excludeId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysTag> findByTargetType(String targetType) {
        return baseMapper.selectByTargetType(targetType);
    }

    @Override
    public List<SysTag> listByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<SysTag>()
                .in(SysTag::getName, names));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysTag findByName(String name) {
        return getOne(new LambdaQueryWrapper<SysTag>()
                .eq(SysTag::getName, name));
    }
}

package com.cybzacg.blogbackend.module.content.taxonomy.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.mapper.SysCategoryMapper;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 分类 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供分类数据的增删改查。
 */
@Repository
public class SysCategoryRepositoryImpl extends ServiceImpl<SysCategoryMapper, SysCategory>
        implements SysCategoryRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCategory> findByTypeOrderBySortOrderAndId(String type) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getType, type)
                .orderByAsc(SysCategory::getSortOrder)
                .orderByAsc(SysCategory::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCategory> findByTypeAndStatusOrderBySortOrderAndId(String type, Integer status) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getType, type)
                .eq(SysCategory::getStatus, status)
                .orderByAsc(SysCategory::getSortOrder)
                .orderByAsc(SysCategory::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCategory> findByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getParentId, parentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByParentId(Long parentId) {
        return exists(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getParentId, parentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByTypeAndCodeExcludingId(String type, String code, Long excludeId) {
        return exists(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getType, type)
                .eq(SysCategory::getCode, code)
                .ne(excludeId != null, SysCategory::getId, excludeId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysCategory> listByTypeAndIds(String type, Collection<Long> ids) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .in(SysCategory::getId, ids)
                .eq(SysCategory::getType, type));
    }
}

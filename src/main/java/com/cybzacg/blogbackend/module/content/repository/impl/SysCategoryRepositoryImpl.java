package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.mapper.SysCategoryMapper;
import com.cybzacg.blogbackend.module.content.repository.SysCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分类 Repository 实现。
 */
@Repository
public class SysCategoryRepositoryImpl extends ServiceImpl<SysCategoryMapper, SysCategory>
        implements SysCategoryRepository {

    @Override
    public List<SysCategory> findByTypeOrderBySortOrderAndId(String type) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getType, type)
                .orderByAsc(SysCategory::getSortOrder)
                .orderByAsc(SysCategory::getId));
    }

    @Override
    public List<SysCategory> findByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getParentId, parentId));
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return exists(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getParentId, parentId));
    }

    @Override
    public boolean existsByTypeAndCodeExcludingId(String type, String code, Long excludeId) {
        return exists(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getType, type)
                .eq(SysCategory::getCode, code)
                .ne(excludeId != null, SysCategory::getId, excludeId));
    }
}

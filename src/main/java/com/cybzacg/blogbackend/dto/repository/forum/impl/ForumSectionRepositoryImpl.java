package com.cybzacg.blogbackend.dto.repository.forum.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.dto.mapper.forum.ForumSectionMapper;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 论坛版块 Repository 实现。
 */
@Repository
public class ForumSectionRepositoryImpl extends ServiceImpl<ForumSectionMapper, ForumSection>
        implements ForumSectionRepository {
    @Override
    public List<ForumSection> listPublicVisibleSections(Integer visibilityScope) {
        return list(new LambdaQueryWrapper<ForumSection>()
                .eq(ForumSection::getStatus, 1)
                .le(ForumSection::getVisibilityScope, visibilityScope)
                .orderByAsc(ForumSection::getSortOrder)
                .orderByAsc(ForumSection::getId));
    }

    @Override
    public Page<ForumSection> pageAdminSections(ForumSectionPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<ForumSection>()
                .eq(query.getStatus() != null, ForumSection::getStatus, query.getStatus())
                .eq(query.getVisibilityScope() != null, ForumSection::getVisibilityScope, query.getVisibilityScope())
                .and(StrUtils.hasText(query.getKeyword()), wrapper -> wrapper
                        .like(ForumSection::getName, query.getKeyword())
                        .or()
                        .like(ForumSection::getDescription, query.getKeyword()))
                .orderByAsc(ForumSection::getSortOrder)
                .orderByAsc(ForumSection::getId));
    }

    @Override
    public boolean existsByNameExcludingId(String name, Long excludedId) {
        return count(new LambdaQueryWrapper<ForumSection>()
                .eq(ForumSection::getName, name)
                .ne(excludedId != null, ForumSection::getId, excludedId)) > 0;
    }
}

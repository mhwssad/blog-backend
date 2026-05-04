package com.cybzacg.blogbackend.module.forum.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.forum.ForumSection;
import com.cybzacg.blogbackend.mapper.forum.ForumSectionMapper;
import com.cybzacg.blogbackend.module.forum.repository.ForumSectionRepository;
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
}

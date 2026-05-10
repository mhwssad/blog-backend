package com.cybzacg.blogbackend.module.forum.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;

import java.util.List;

/**
 * 论坛版块 Repository。
 */
public interface ForumSectionRepository extends IService<ForumSection> {
    List<ForumSection> listPublicVisibleSections(Integer visibilityScope);

    Page<ForumSection> pageAdminSections(ForumSectionPageQuery query);

    boolean existsByNameExcludingId(String name, Long excludedId);
}

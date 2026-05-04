package com.cybzacg.blogbackend.module.forum.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.forum.ForumSection;

import java.util.List;

/**
 * 论坛版块 Repository。
 */
public interface ForumSectionRepository extends IService<ForumSection> {
    List<ForumSection> listPublicVisibleSections(Integer visibilityScope);
}

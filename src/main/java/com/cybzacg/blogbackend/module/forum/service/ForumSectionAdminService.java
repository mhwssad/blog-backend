package com.cybzacg.blogbackend.module.forum.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionSaveRequest;

/**
 * 论坛版块后台管理服务。
 */
public interface ForumSectionAdminService {
    PageResult<ForumSectionAdminVO> pageSections(ForumSectionPageQuery query);

    ForumSectionAdminVO getSection(Long id);

    ForumSectionAdminVO createSection(ForumSectionSaveRequest request);

    ForumSectionAdminVO updateSection(Long id, ForumSectionSaveRequest request);

    void updateStatus(Long id, Integer status);

    void deleteSection(Long id);
}

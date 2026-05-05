package com.cybzacg.blogbackend.module.forum.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminDetailVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminVO;

/**
 * 论坛帖子后台治理服务。
 */
public interface ForumPostAdminService {
    PageResult<ForumPostAdminVO> pagePosts(ForumPostAdminPageQuery query);

    ForumPostAdminDetailVO getPost(Long id);

    void hidePost(Long id, Long operatorId, String ip, String ua);

    void restorePost(Long id, Long operatorId, String ip, String ua);

    void deletePost(Long id, Long operatorId, String ip, String ua);

    void toggleTop(Long id, boolean enabled, Long operatorId, String ip, String ua);

    void toggleEssence(Long id, boolean enabled, Long operatorId, String ip, String ua);
}

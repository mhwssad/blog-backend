package com.cybzacg.blogbackend.module.forum.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumPostAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumPostPageQuery;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostPageQuery;

import java.util.Collection;

/**
 * 论坛帖子 Repository。
 */
public interface ForumPostRepository extends IService<ForumPost> {
    Page<ForumPost> pagePublicPosts(ForumPostPageQuery query, boolean loginUser, Collection<Long> visibleSectionIds);

    Page<ForumPost> pageUserPosts(Long authorId, UserForumPostPageQuery query);

    Page<ForumPost> pageAdminPosts(ForumPostAdminPageQuery query);

    boolean existsBySectionId(Long sectionId);

    void incrementLikeCount(Long id, int delta);

    void incrementReplyCount(Long id, int delta);

    void incrementCollectCount(Long id, int delta);

    void incrementShareCount(Long id, int delta);

    void incrementViewCount(Long id, int delta);

    void softDeleteById(Long id);

    void updateStatusById(Long id, Integer status);

    void updateTopById(Long id, Integer isTop);

    void updateEssenceById(Long id, Integer isEssence);
}

package com.cybzacg.blogbackend.module.chat.conversation.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.forum.ForumPostChannelLink;

/**
 * 论坛帖子频道关联 Repository。
 */
public interface ForumPostChannelLinkRepository extends IService<ForumPostChannelLink> {
    void updateStatusByForumPostId(Long forumPostId, Integer status);
}

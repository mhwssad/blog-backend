package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.user.ForumPostChannelLinkVO;

/**
 * 论坛帖子与频道关联服务。
 */
public interface ForumPostChannelLinkService {
    /**
     * 分享帖子到频道（幂等）。
     */
    ForumPostChannelLinkVO sharePostToChannel(Long userId, Long forumPostId, Long conversationId);

    /**
     * 查询帖子关联的频道。
     */
    ForumPostChannelLinkVO getPostLinkedChannel(Long forumPostId);

    /**
     * 分页查询频道关联的帖子列表。
     */
    PageResult<ForumPostChannelLinkVO> pageChannelLinks(Long conversationId, Long current, Long size);

    /**
     * 取消帖子与频道的关联。
     */
    void unlinkPost(Long userId, Long forumPostId);
}

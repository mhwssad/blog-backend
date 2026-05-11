package com.cybzacg.blogbackend.dto.repository.chat.conversation.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPostChannelLink;
import com.cybzacg.blogbackend.dto.mapper.forum.ForumPostChannelLinkMapper;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ForumPostChannelLinkRepository;
import org.springframework.stereotype.Repository;

/**
 * 论坛帖子频道关联 Repository 实现。
 */
@Repository
public class ForumPostChannelLinkRepositoryImpl
        extends ServiceImpl<ForumPostChannelLinkMapper, ForumPostChannelLink>
        implements ForumPostChannelLinkRepository {

    @Override
    public void updateStatusByForumPostId(Long forumPostId, Integer status) {
        lambdaUpdate()
                .eq(ForumPostChannelLink::getForumPostId, forumPostId)
                .set(ForumPostChannelLink::getStatus, status)
                .update();
    }
}

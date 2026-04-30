package com.cybzacg.blogbackend.module.chat.conversation.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.forum.ForumPostChannelLink;
import com.cybzacg.blogbackend.mapper.forum.ForumPostChannelLinkMapper;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ForumPostChannelLinkRepository;
import org.springframework.stereotype.Repository;

/**
 * 论坛帖子频道关联 Repository 实现。
 */
@Repository
public class ForumPostChannelLinkRepositoryImpl
        extends ServiceImpl<ForumPostChannelLinkMapper, ForumPostChannelLink>
        implements ForumPostChannelLinkRepository {
}

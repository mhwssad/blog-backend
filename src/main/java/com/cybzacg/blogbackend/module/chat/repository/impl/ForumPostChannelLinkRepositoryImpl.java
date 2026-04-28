package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ForumPostChannelLink;
import com.cybzacg.blogbackend.mapper.ForumPostChannelLinkMapper;
import com.cybzacg.blogbackend.module.chat.repository.ForumPostChannelLinkRepository;
import org.springframework.stereotype.Repository;

/**
 * 论坛帖子频道关联 Repository 实现。
 */
@Repository
public class ForumPostChannelLinkRepositoryImpl
        extends ServiceImpl<ForumPostChannelLinkMapper, ForumPostChannelLink>
        implements ForumPostChannelLinkRepository {
}

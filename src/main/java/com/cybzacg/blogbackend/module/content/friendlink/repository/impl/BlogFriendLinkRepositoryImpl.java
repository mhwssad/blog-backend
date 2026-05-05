package com.cybzacg.blogbackend.module.content.friendlink.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.content.BlogFriendLink;
import com.cybzacg.blogbackend.mapper.content.BlogFriendLinkMapper;
import com.cybzacg.blogbackend.module.content.friendlink.repository.BlogFriendLinkRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BlogFriendLinkRepositoryImpl extends ServiceImpl<BlogFriendLinkMapper, BlogFriendLink>
        implements BlogFriendLinkRepository {

    @Override
    public List<BlogFriendLink> listEnabled() {
        return list(new LambdaQueryWrapper<BlogFriendLink>()
                .eq(BlogFriendLink::getStatus, 1)
                .orderByAsc(BlogFriendLink::getSortOrder)
                .orderByDesc(BlogFriendLink::getId));
    }
}

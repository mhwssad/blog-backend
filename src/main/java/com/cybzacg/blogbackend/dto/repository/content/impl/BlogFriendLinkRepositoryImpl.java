package com.cybzacg.blogbackend.dto.repository.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.content.BlogFriendLink;
import com.cybzacg.blogbackend.dto.mapper.content.BlogFriendLinkMapper;
import com.cybzacg.blogbackend.dto.repository.content.BlogFriendLinkRepository;
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

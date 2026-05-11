package com.cybzacg.blogbackend.dto.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPostChannelLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛帖子频道关联 Mapper。
 */
@Mapper
public interface ForumPostChannelLinkMapper
    extends BaseMapper<ForumPostChannelLink> {}

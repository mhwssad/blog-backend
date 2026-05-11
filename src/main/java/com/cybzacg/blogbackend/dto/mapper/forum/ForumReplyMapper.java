package com.cybzacg.blogbackend.dto.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ForumReplyMapper extends BaseMapper<ForumReply> {
    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementReplyCount(@Param("id") Long id, @Param("delta") int delta);
}

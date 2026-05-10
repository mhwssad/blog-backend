package com.cybzacg.blogbackend.dto.mapper.forum;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import org.apache.ibatis.annotations.Param;

public interface ForumPostMapper extends BaseMapper<ForumPost> {
    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementReplyCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementCollectCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementShareCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementViewCount(@Param("id") Long id, @Param("delta") int delta);
}

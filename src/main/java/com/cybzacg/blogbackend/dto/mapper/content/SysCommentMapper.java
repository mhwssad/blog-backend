package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysComment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysCommentMapper extends BaseMapper<SysComment> {
    List<SysComment> selectRepliesByRootIds(
        @Param("rootIds") List<Long> rootIds
    );

    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementReplyCount(@Param("id") Long id, @Param("delta") int delta);
}

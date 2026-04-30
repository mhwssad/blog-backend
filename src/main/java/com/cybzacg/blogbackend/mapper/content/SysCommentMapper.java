package com.cybzacg.blogbackend.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.content.SysComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysCommentMapper extends BaseMapper<SysComment> {
    List<SysComment> selectRootCommentsByTarget(@Param("targetId") Long targetId,
                                                @Param("targetType") String targetType);

    List<SysComment> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);
}

package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysInteractionMapper extends BaseMapper<SysInteraction> {
    boolean existsUserAction(
        @Param("userId") Long userId,
        @Param("targetId") Long targetId,
        @Param("targetType") String targetType,
        @Param("actionType") String actionType
    );
}

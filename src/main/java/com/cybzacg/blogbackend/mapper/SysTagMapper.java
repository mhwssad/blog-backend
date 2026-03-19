package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysTagMapper extends BaseMapper<SysTag> {
    List<SysTag> selectByTargetType(@Param("targetType") String targetType);
}

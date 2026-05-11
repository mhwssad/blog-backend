package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysUserFootprint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserFootprintMapper extends BaseMapper<SysUserFootprint> {
    int upsertFootprint(SysUserFootprint footprint);
}

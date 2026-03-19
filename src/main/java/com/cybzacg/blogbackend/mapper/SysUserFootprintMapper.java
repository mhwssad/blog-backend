package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysUserFootprint;

public interface SysUserFootprintMapper extends BaseMapper<SysUserFootprint> {
    int upsertFootprint(SysUserFootprint footprint);
}

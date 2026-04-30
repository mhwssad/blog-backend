package com.cybzacg.blogbackend.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.content.SysUserFootprint;

public interface SysUserFootprintMapper extends BaseMapper<SysUserFootprint> {
    int upsertFootprint(SysUserFootprint footprint);
}

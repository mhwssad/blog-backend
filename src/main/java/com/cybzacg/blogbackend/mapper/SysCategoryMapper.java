package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysCategoryMapper extends BaseMapper<SysCategory> {
    List<SysCategory> selectTreeByType(@Param("type") String type);
}

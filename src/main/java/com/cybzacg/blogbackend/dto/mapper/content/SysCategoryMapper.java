package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysCategoryMapper extends BaseMapper<SysCategory> {
    List<SysCategory> selectTreeByType(@Param("type") String type);
}

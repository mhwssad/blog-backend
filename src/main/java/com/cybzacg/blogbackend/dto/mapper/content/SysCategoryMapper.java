package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {
    List<SysCategory> selectTreeByType(@Param("type") String type);
}

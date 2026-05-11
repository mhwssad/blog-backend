package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysCollectionFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysCollectionFolderMapper
    extends BaseMapper<SysCollectionFolder>
{
    int incrementCollectionCount(
        @Param("id") Long id,
        @Param("delta") int delta
    );
}

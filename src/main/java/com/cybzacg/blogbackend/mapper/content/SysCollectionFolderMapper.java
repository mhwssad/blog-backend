package com.cybzacg.blogbackend.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.content.SysCollectionFolder;
import org.apache.ibatis.annotations.Param;

public interface SysCollectionFolderMapper extends BaseMapper<SysCollectionFolder> {
    int incrementCollectionCount(@Param("id") Long id, @Param("delta") int delta);
}

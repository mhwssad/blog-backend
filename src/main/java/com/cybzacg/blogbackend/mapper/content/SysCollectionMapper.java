package com.cybzacg.blogbackend.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.content.SysCollection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysCollectionMapper extends BaseMapper<SysCollection> {
    List<SysCollection> selectUserCollectionPage(@Param("userId") Long userId,
                                                 @Param("folderId") Long folderId,
                                                 @Param("targetType") String targetType);
}

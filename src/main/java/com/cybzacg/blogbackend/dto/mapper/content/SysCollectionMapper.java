package com.cybzacg.blogbackend.dto.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.content.SysCollection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysCollectionMapper extends BaseMapper<SysCollection> {
    List<SysCollection> selectUserCollectionPage(
        @Param("userId") Long userId,
        @Param("folderId") Long folderId,
        @Param("targetType") String targetType
    );
}

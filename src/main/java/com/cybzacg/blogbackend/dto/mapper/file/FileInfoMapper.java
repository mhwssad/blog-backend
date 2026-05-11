package com.cybzacg.blogbackend.dto.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件物理信息 Mapper。
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {}

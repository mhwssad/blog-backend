package com.cybzacg.blogbackend.dto.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.file.FileChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片 Mapper。
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {}

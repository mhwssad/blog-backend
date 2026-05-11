package com.cybzacg.blogbackend.dto.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.file.FileUploadTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传任务 Mapper。
 */
@Mapper
public interface FileUploadTaskMapper extends BaseMapper<FileUploadTask> {}

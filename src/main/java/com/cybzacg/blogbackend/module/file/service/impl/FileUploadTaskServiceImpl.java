package com.cybzacg.blogbackend.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.mapper.FileUploadTaskMapper;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import org.springframework.stereotype.Service;

/**
 * 文件上传任务基础服务实现。
 */
@Service
public class FileUploadTaskServiceImpl extends ServiceImpl<FileUploadTaskMapper, FileUploadTask>
        implements FileUploadTaskService {
}

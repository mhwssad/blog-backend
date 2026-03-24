package com.cybzacg.blogbackend.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.mapper.FileBusinessInfoMapper;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import org.springframework.stereotype.Service;

/**
 * 文件业务引用基础服务实现。
 */
@Service
public class FileBusinessInfoServiceImpl extends ServiceImpl<FileBusinessInfoMapper, FileBusinessInfo>
        implements FileBusinessInfoService {
}

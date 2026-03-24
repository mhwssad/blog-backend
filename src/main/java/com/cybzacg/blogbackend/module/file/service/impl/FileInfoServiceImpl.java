package com.cybzacg.blogbackend.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.mapper.FileInfoMapper;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import org.springframework.stereotype.Service;

/**
 * 文件物理信息基础服务实现。
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {
}

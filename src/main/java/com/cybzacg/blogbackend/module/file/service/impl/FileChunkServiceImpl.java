package com.cybzacg.blogbackend.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.mapper.FileChunkMapper;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import org.springframework.stereotype.Service;

/**
 * 文件分片基础服务实现。
 */
@Service
public class FileChunkServiceImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements FileChunkService {
}

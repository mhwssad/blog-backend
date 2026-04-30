package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.file.FileChunk;
import com.cybzacg.blogbackend.mapper.file.FileChunkMapper;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import org.springframework.stereotype.Repository;

/**
 * 文件分片 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文件分片记录的增删改查。
 */
@Repository
public class FileChunkRepositoryImpl extends ServiceImpl<FileChunkMapper, FileChunk> implements FileChunkRepository {
    /**
     * {@inheritDoc}
     */
    @Override
    public FileChunk findByTaskIdAndChunkNumber(Long uploadTaskId, Integer chunkNumber) {
        return getOne(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId)
                .eq(FileChunk::getChunkNumber, chunkNumber)
                .last("limit 1"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByTaskIdAndStatus(Long uploadTaskId, Integer completedStatus) {
        Long count = count(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId)
                .eq(FileChunk::getUploadStatus, completedStatus));
        return count == null ? 0L : count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteByUploadTaskId(Long uploadTaskId) {
        return remove(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId));
    }
}

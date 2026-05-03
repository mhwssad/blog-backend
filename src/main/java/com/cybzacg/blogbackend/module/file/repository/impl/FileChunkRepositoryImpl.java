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
     * 根据上传任务ID和分片序号查询单条分片记录。
     *
     * @param uploadTaskId 上传任务ID
     * @param chunkNumber  分片序号
     * @return 匹配的分片记录，若不存在则返回 null
     */
    @Override
    public FileChunk findByTaskIdAndChunkNumber(Long uploadTaskId, Integer chunkNumber) {
        return getOne(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId)
                .eq(FileChunk::getChunkNumber, chunkNumber)
                .last("limit 1"));
    }

    /**
     * 统计指定上传任务ID下已完成（状态为 completedStatus）的分片数量。
     *
     * @param uploadTaskId    上传任务ID
     * @param completedStatus 已完成状态值
     * @return 已完成分片数量
     */
    @Override
    public long countByTaskIdAndStatus(Long uploadTaskId, Integer completedStatus) {
        Long count = count(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId)
                .eq(FileChunk::getUploadStatus, completedStatus));
        return count == null ? 0L : count;
    }

    /**
     * 删除指定上传任务ID对应的所有分片记录。
     *
     * @param uploadTaskId 上传任务ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByUploadTaskId(Long uploadTaskId) {
        return remove(new LambdaQueryWrapper<FileChunk>()
                .eq(FileChunk::getUploadTaskId, uploadTaskId));
    }
}

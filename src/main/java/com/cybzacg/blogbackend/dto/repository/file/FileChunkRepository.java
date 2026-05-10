package com.cybzacg.blogbackend.dto.repository.file;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.file.FileChunk;

/**
 * 文件分片 Repository。<p>封装文件分片记录的持久化操作，提供按任务查询分片、统计完成数及批量删除。
 */
public interface FileChunkRepository extends IService<FileChunk> {
    /**
     * 按任务和分片序号查询分片。
     *
     * @param uploadTaskId 上传任务 ID
     * @param chunkNumber  分片序号
     * @return 分片记录，不存在则返回 null
     */
    FileChunk findByTaskIdAndChunkNumber(Long uploadTaskId, Integer chunkNumber);

    /**
     * 统计任务已完成分片数。
     *
     * @param uploadTaskId    上传任务 ID
     * @param completedStatus 已完成状态值
     * @return 已完成分片数
     */
    long countByTaskIdAndStatus(Long uploadTaskId, Integer completedStatus);

    /**
     * 删除任务下的所有分片。
     *
     * @param uploadTaskId 上传任务 ID
     * @return 是否删除成功
     */
    boolean deleteByUploadTaskId(Long uploadTaskId);
}

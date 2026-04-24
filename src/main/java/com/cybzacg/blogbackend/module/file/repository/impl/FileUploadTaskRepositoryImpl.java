package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.mapper.FileUploadTaskMapper;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 文件上传任务 Repository 实现。
 */
@Repository
public class FileUploadTaskRepositoryImpl extends ServiceImpl<FileUploadTaskMapper, FileUploadTask>
        implements FileUploadTaskRepository {
    @Override
    public FileUploadTask findByUploadIdAndUserId(String uploadId, Long userId) {
        return getOne(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getUploadId, uploadId)
                .eq(FileUploadTask::getUploadUserId, userId)
                .last("limit 1"));
    }

    @Override
    public Page<FileUploadTask> pageByUserAndStatus(Long userId, UserFileTaskPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getUploadUserId, userId)
                .eq(query.getTaskStatus() != null, FileUploadTask::getTaskStatus, query.getTaskStatus())
                .eq(query.getIsQuickUpload() != null, FileUploadTask::getIsQuickUpload, query.getIsQuickUpload())
                .eq(query.getIsChunked() != null, FileUploadTask::getIsChunked, query.getIsChunked())
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId));
    }

    @Override
    public Page<FileUploadTask> pageAdminTasks(FileTaskPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<FileUploadTask>()
                .eq(query.getUploadUserId() != null, FileUploadTask::getUploadUserId, query.getUploadUserId())
                .eq(query.getTaskStatus() != null, FileUploadTask::getTaskStatus, query.getTaskStatus())
                .eq(query.getIsQuickUpload() != null, FileUploadTask::getIsQuickUpload, query.getIsQuickUpload())
                .eq(query.getIsChunked() != null, FileUploadTask::getIsChunked, query.getIsChunked())
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId));
    }

    @Override
    public List<FileUploadTask> findExpiredTasks(Date expireTime, List<Integer> statuses, int limit) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .le(FileUploadTask::getExpireTime, expireTime)
                .in(FileUploadTask::getTaskStatus, statuses)
                .orderByAsc(FileUploadTask::getExpireTime)
                .orderByAsc(FileUploadTask::getId)
                .last("limit " + limit));
    }

    @Override
    public List<FileUploadTask> listRecentByFileId(Long fileId, int limit) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getFileId, fileId)
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId)
                .last("limit " + limit));
    }

    @Override
    public List<FileUploadTask> listByFileId(Long fileId) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getFileId, fileId));
    }
}

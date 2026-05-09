package com.cybzacg.blogbackend.module.file.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.mapper.file.FileUploadTaskMapper;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件上传任务 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文件上传任务的增删改查。
 */
@Repository
public class FileUploadTaskRepositoryImpl extends ServiceImpl<FileUploadTaskMapper, FileUploadTask>
        implements FileUploadTaskRepository {
    /**
     * 根据上传ID和用户ID查询单条上传任务记录。
     *
     * @param uploadId 上传任务唯一标识
     * @param userId   上传用户ID
     * @return 匹配的上传任务记录，若不存在则返回 null
     */
    @Override
    public FileUploadTask findByUploadIdAndUserId(String uploadId, Long userId) {
        return getOne(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getUploadId, uploadId)
                .eq(FileUploadTask::getUploadUserId, userId)
                .last("limit 1"));
    }

    /**
     * 用户分页查询上传任务，支持按任务状态、秒传标识和分片标识过滤。
     *
     * @param userId 用户 ID
     * @param query  分页查询条件
     * @return 上传任务分页结果，按创建时间和 ID 倒序排列
     */
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

    /**
     * 管理员分页查询上传任务，支持按上传用户、任务状态、秒传标识和分片标识过滤。
     *
     * @param query 分页查询条件
     * @return 上传任务分页结果，按创建时间和 ID 倒序排列
     */
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

    /**
     * 查询过期上传任务，用于定时清理。结果按过期时间和 ID 升序排列，优先清理最早过期的任务。
     *
     * @param expireTime 过期时间阈值
     * @param statuses   目标状态集合
     * @param limit      最大返回条数
     * @return 符合条件的过期任务列表
     */
    @Override
    public List<FileUploadTask> findExpiredTasks(LocalDateTime expireTime, List<Integer> statuses, int limit) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .le(FileUploadTask::getExpireTime, expireTime)
                .in(FileUploadTask::getTaskStatus, statuses)
                .orderByAsc(FileUploadTask::getExpireTime)
                .orderByAsc(FileUploadTask::getId)
                .last("limit " + limit));
    }

    /**
     * 查询文件最近关联的上传任务。
     *
     * @param fileId 文件 ID
     * @param limit  最大返回条数
     * @return 最近的上传任务列表，按创建时间和 ID 倒序排列
     */
    @Override
    public List<FileUploadTask> listRecentByFileId(Long fileId, int limit) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getFileId, fileId)
                .orderByDesc(FileUploadTask::getCreatedAt)
                .orderByDesc(FileUploadTask::getId)
                .last("limit " + limit));
    }

    /**
     * 查询文件关联的所有上传任务。
     *
     * @param fileId 文件 ID
     * @return 该文件的所有上传任务列表
     */
    @Override
    public List<FileUploadTask> listByFileId(Long fileId) {
        return list(new LambdaQueryWrapper<FileUploadTask>()
                .eq(FileUploadTask::getFileId, fileId));
    }
}

package com.cybzacg.blogbackend.module.file.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件上传任务 Repository。<p>封装文件上传任务表的持久化操作，提供按用户、状态分页查询及过期任务清理。
 */
public interface FileUploadTaskRepository extends IService<FileUploadTask> {
    /**
     * 按上传 ID 和用户 ID 查询任务。
     *
     * @param uploadId 上传 ID
     * @param userId   用户 ID
     * @return 上传任务
     */
    FileUploadTask findByUploadIdAndUserId(String uploadId, Long userId);

    /**
     * 按用户和状态分页查询上传任务。
     *
     * @param userId     用户 ID
     * @param taskStatus 任务状态
     * @param current    页码
     * @param size       页大小
     * @return 上传任务分页
     */
    Page<FileUploadTask> pageByUserAndStatus(Long userId, UserFileTaskPageQuery query);

    /**
     * 按管理端条件分页查询上传任务。
     *
     * @param query 查询条件
     * @return 上传任务分页
     */
    Page<FileUploadTask> pageAdminTasks(FileTaskPageQuery query);

    /**
     * 查询过期上传任务。
     *
     * @param expireTime 过期时间
     * @param statuses   可清理状态
     * @param limit      限制数量
     * @return 过期任务列表
     */
    List<FileUploadTask> findExpiredTasks(LocalDateTime expireTime, List<Integer> statuses, int limit);

    /**
     * 查询文件最近关联的上传任务。
     *
     * @param fileId 文件 ID
     * @param limit  限制数量
     * @return 上传任务列表
     */
    List<FileUploadTask> listRecentByFileId(Long fileId, int limit);

    /**
     * 查询文件关联的所有上传任务。
     *
     * @param fileId 文件 ID
     * @return 上传任务列表
     */
    List<FileUploadTask> listByFileId(Long fileId);
}

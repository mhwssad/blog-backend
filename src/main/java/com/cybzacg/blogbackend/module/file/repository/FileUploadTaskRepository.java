package com.cybzacg.blogbackend.module.file.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
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
     * @param userId 用户 ID
     * @param query  分页查询条件
     * @return 上传任务分页结果，按创建时间和 ID 倒序排列
     */
    Page<FileUploadTask> pageByUserAndStatus(Long userId, UserFileTaskPageQuery query);

    /**
     * 按管理端条件分页查询上传任务。
     *
     * @param query 分页查询条件
     * @return 上传任务分页结果，按创建时间和 ID 倒序排列
     */
    Page<FileUploadTask> pageAdminTasks(FileTaskPageQuery query);

    /**
     * 查询过期上传任务，用于定时清理。
     *
     * @param expireTime 过期时间阈值
     * @param statuses   目标状态集合
     * @param limit      最大返回条数
     * @return 符合条件的过期任务列表，按过期时间和 ID 升序排列
     */
    List<FileUploadTask> findExpiredTasks(LocalDateTime expireTime, List<Integer> statuses, int limit);

    /**
     * 查询文件最近关联的上传任务。
     *
     * @param fileId 文件 ID
     * @param limit  最大返回条数
     * @return 最近的上传任务列表，按创建时间和 ID 倒序排列
     */
    List<FileUploadTask> listRecentByFileId(Long fileId, int limit);

    /**
     * 查询文件关联的所有上传任务。
     *
     * @param fileId 文件 ID
     * @return 该文件的所有上传任务列表
     */
    List<FileUploadTask> listByFileId(Long fileId);
}

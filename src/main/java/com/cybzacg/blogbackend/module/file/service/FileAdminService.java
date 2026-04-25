package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.file.model.admin.*;

/**
 * 后台文件管理服务。
 * 提供文件与上传任务的查询、状态调整和清理能力。
 */
public interface FileAdminService {
    /**
     * 分页查询文件列表。
     */
    PageResult<FileAdminVO> pageFiles(FileAdminPageQuery query);

    /**
     * 查询单个文件的详情、引用和关联任务。
     */
    FileDetailVO getFile(Long id);

    /**
     * 分页查询上传任务列表。
     */
    PageResult<FileTaskAdminVO> pageTasks(FileTaskPageQuery query);

    /**
     * 更新文件状态。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除文件及其关联引用、任务和临时分片。
     */
    void deleteFile(Long id);
}

package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;

/**
 * 文件上传任务服务接口。
 */
public interface FileUploadService {

    UserTaskVO initUploadTask(Long userId, UserUploadInitRequest request);

    UserTaskVO quickCheck(Long userId, String md5);

    UserTaskVO uploadFile(Long userId, String md5, String taskId, FileInfo fileInfo);

    UserTaskVO uploadChunk(Long userId, String taskId, Integer chunkIndex, FileInfo chunkFileInfo);

    UserTaskVO completeUpload(Long userId, String taskId);
}
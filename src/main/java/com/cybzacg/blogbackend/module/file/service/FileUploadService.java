package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;

import java.io.InputStream;

/**
 * 文件上传任务服务接口。
 */
public interface FileUploadService {

    UserTaskVO initUploadTask(Long userId, UserUploadInitRequest request);

    UserTaskVO quickCheck(Long userId, String md5);

    UserTaskVO uploadFile(Long userId, String md5, String taskId, FileInfo fileInfo, InputStream inputStream);

    UserTaskVO uploadChunk(Long userId, String taskId, Integer chunkIndex, FileInfo chunkFileInfo, InputStream inputStream);

    UserTaskVO completeUpload(Long userId, String taskId);
}

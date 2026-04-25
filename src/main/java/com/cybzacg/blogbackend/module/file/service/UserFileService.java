package com.cybzacg.blogbackend.module.file.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.file.model.user.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户文件服务。
 * 封装上传任务流转、个人文件查询和用户侧引用删除能力。
 */
public interface UserFileService {
    /**
     * 初始化上传任务，并在可复用已有文件时直接返回秒传结果。
     */
    FileUploadInitVO initUploadTask(FileUploadInitRequest request, String sourceIp);

    /**
     * 基于任务上下文执行秒传检测。
     */
    FileUploadResultVO quickCheck(String uploadId, String sourceIp);

    /**
     * 执行整文件上传。
     */
    FileUploadResultVO uploadFile(String uploadId, MultipartFile file, String sourceIp);

    /**
     * 上传单个分片并刷新任务进度。
     */
    ChunkUploadVO uploadChunk(String uploadId, Integer chunkNumber, MultipartFile file, String chunkMd5, String sourceIp);

    /**
     * 完成分片上传并触发合并。
     */
    FileUploadResultVO completeUpload(String uploadId, String sourceIp);

    /**
     * 分页查询当前用户的文件引用列表。
     */
    PageResult<UserFileVO> pageMyFiles(UserFilePageQuery query);

    /**
     * 分页查询当前用户的上传任务列表。
     */
    PageResult<UserFileTaskVO> pageMyUploadTasks(UserFileTaskPageQuery query);

    /**
     * 删除当前用户拥有的文件引用。
     */
    void deleteMyFile(Long businessId);
}

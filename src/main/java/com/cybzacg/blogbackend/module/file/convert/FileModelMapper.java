package com.cybzacg.blogbackend.module.file.convert;

import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileDetailVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskAdminVO;
import com.cybzacg.blogbackend.module.file.model.user.*;
import org.mapstruct.*;

/**
 * 文件模块对象转换器，处理实体与 VO 之间的映射。
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FileModelMapper {

    // ==================== 用户侧 VO ====================

    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "quickUploadAvailable", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    FileUploadInitVO toFileUploadInitVO(FileUploadTask task);

    @Mapping(target = "uploadId", source = "uploadId")
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "quickUpload", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    @Mapping(target = "referenceCount", ignore = true)
    FileUploadResultVO toFileUploadResultVO(FileUploadTask task);

    @Mapping(target = "uploadId", source = "uploadId")
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "errorCode", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    UserFileTaskVO toUserFileTaskVO(FileUploadTask task);

    @Mapping(target = "uploadId", ignore = true)
    @Mapping(target = "uploadedChunks", ignore = true)
    @Mapping(target = "totalChunks", ignore = true)
    @Mapping(target = "taskStatus", ignore = true)
    ChunkUploadVO toChunkUploadVO(FileChunk chunk);

    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "fileId", source = "id")
    @Mapping(target = "referenceId", ignore = true)
    @Mapping(target = "referenceType", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    UserFileVO toUserFileVO(FileInfo fileInfo);

    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "originalName", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "fileType", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserFileVO toUserFileVO(FileBusinessInfo businessInfo);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isPublic", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserFileVO toUserFileVOFromBoth(FileBusinessInfo businessInfo, FileInfo fileInfo);

    // ==================== 管理侧 VO ====================

    @Mapping(target = "id", source = "id")
    FileAdminVO toFileAdminVO(FileInfo fileInfo);

    @Mapping(target = "references", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    FileDetailVO toFileDetailVO(FileInfo fileInfo);

    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "uploadUserId", ignore = true)
    @Mapping(target = "storageKey", ignore = true)
    @Mapping(target = "isQuickUpload", ignore = true)
    @Mapping(target = "isChunked", ignore = true)
    @Mapping(target = "uploadedChunks", ignore = true)
    @Mapping(target = "totalChunks", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "completeTime", ignore = true)
    FileTaskAdminVO toFileTaskAdminVO(FileUploadTask task);

    // ==================== AfterMapping 填充运行时字段 ====================

    @AfterMapping
    default void fillFileUploadInitVO(@MappingTarget FileUploadInitVO vo, FileUploadTask task) {
        vo.setTaskId(task.getId());
        if (task.getIsQuickUpload() != null && task.getIsQuickUpload() == 1) {
            vo.setCompleted(true);
            vo.setQuickUploadAvailable(true);
        } else {
            vo.setCompleted(false);
            vo.setQuickUploadAvailable(false);
        }
        if (task.getIsChunked() != null && task.getIsChunked() == 1) {
            vo.setUploadMode(1);
        } else {
            vo.setUploadMode(0);
        }
    }

    @AfterMapping
    default void fillFileUploadResultVO(@MappingTarget FileUploadResultVO vo, FileUploadTask task) {
        vo.setTaskId(task.getId());
        vo.setQuickUpload(task.getIsQuickUpload() != null && task.getIsQuickUpload() == 1);
    }

    @AfterMapping
    default void fillUserFileTaskVO(@MappingTarget UserFileTaskVO vo, FileUploadTask task) {
        vo.setId(task.getId());
        if (task.getIsQuickUpload() != null) {
            vo.setIsQuickUpload(task.getIsQuickUpload());
        }
        if (task.getIsChunked() != null) {
            vo.setIsChunked(task.getIsChunked());
        }
    }

    @AfterMapping
    default void fillUserFileVOFromBusinessInfo(@MappingTarget UserFileVO vo, FileBusinessInfo businessInfo) {
        if (businessInfo != null) {
            vo.setBusinessId(businessInfo.getId());
            vo.setReferenceId(businessInfo.getReferenceId());
            vo.setReferenceType(businessInfo.getReferenceType());
            vo.setCategory(businessInfo.getCategory());
            vo.setIsPublic(businessInfo.getIsPublic());
        }
    }

    @AfterMapping
    default void fillUserFileVOFromBoth(@MappingTarget UserFileVO vo, FileBusinessInfo businessInfo, FileInfo fileInfo) {
        if (fileInfo != null) {
            vo.setFileId(fileInfo.getId());
            vo.setFileName(fileInfo.getFileName());
            vo.setOriginalName(fileInfo.getOriginalName());
            vo.setFileUrl(fileInfo.getFileUrl());
            vo.setFileSize(fileInfo.getFileSize());
            vo.setFileType(fileInfo.getFileType());
            vo.setMimeType(fileInfo.getMimeType());
            vo.setStatus(fileInfo.getStatus());
            vo.setCreatedAt(fileInfo.getCreatedAt());
        }
        if (businessInfo != null) {
            vo.setBusinessId(businessInfo.getId());
            vo.setReferenceId(businessInfo.getReferenceId());
            vo.setReferenceType(businessInfo.getReferenceType());
            vo.setCategory(businessInfo.getCategory());
            vo.setIsPublic(businessInfo.getIsPublic());
        }
    }
}

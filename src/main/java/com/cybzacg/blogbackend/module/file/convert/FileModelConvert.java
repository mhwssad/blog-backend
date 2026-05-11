package com.cybzacg.blogbackend.module.file.convert;

import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileChunk;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.module.file.model.admin.*;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
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
public interface FileModelConvert {

    // ==================== 用户侧 VO ====================

    @Mapping(target = "taskId", source = "id")
    FileUploadInitVO toFileUploadInitVO(FileUploadTask task);

    @Mapping(target = "taskId", source = "id")
    FileUploadResultVO toFileUploadResultVO(FileUploadTask task);

    UserFileTaskVO toUserFileTaskVO(FileUploadTask task);

    ChunkUploadVO toChunkUploadVO(FileChunk chunk);

    @Mapping(target = "fileId", source = "id")
    UserFileVO toUserFileVO(FileInfo fileInfo);

    UserFileVO toUserFileVO(FileBusinessInfo businessInfo);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isPublic", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserFileVO toUserFileVOFromBoth(FileBusinessInfo businessInfo, FileInfo fileInfo);

    // ==================== 管理侧用户文件 VO ====================

    @Mapping(target = "businessId", source = "id")
    @Mapping(target = "fileId", source = "id")
    UserFileVO toAdminUserFileVO(FileInfo fileInfo);

    // ==================== 管理侧 VO ====================

    FileAdminVO toFileAdminVO(FileInfo fileInfo);

    FileReferenceVO toFileReferenceVO(FileBusinessInfo businessInfo);

    FileDetailVO toFileDetailVO(FileInfo fileInfo);

    FileTaskAdminVO toFileTaskAdminVO(FileUploadTask task);

    @Mapping(target = "taskId", source = "id")
    UserTaskVO toUserTaskVO(FileUploadTask task);

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

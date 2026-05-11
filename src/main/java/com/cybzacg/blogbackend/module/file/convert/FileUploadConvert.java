package com.cybzacg.blogbackend.module.file.convert;

import com.cybzacg.blogbackend.dto.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileChunk;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.dto.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;
import com.cybzacg.blogbackend.utils.CollectionUtils;
import org.mapstruct.*;

import java.time.LocalDateTime;

/**
 * 文件上传模块实体转换器。
 * 处理上传任务、文件信息、业务引用和分片的对象创建与更新。
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FileUploadConvert {

    // ==================== FileUploadTask 创建 ====================

    @Mapping(target = "uploadId", expression = "java(java.util.UUID.randomUUID().toString().replace(\"-\", \"\"))")
    @Mapping(target = "uploadUserId", source = "userId")
    @Mapping(target = "fileMd5", source = "normalizedMd5")
    @Mapping(target = "taskStatus", expression = "java(com.cybzacg.blogbackend.enums.storage.TaskStatusEnum.INIT.getValue())")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "expireTime", source = "expireTime")
    @Mapping(target = "uploadedChunks", constant = "0")
    @Mapping(target = "retryCount", constant = "0")
    FileUploadTask toFileUploadTask(UserUploadInitRequest request, Long userId, String storageKey,
                                    String normalizedMd5, Integer isChunked, Long chunkSize,
                                    Integer totalChunks, LocalDateTime startTime, LocalDateTime expireTime);

    @AfterMapping
    default void normalizeTaskFields(@MappingTarget FileUploadTask task, UserUploadInitRequest request) {
        task.setReferenceType(FileReferenceTypeEnum.normalize(request.getReferenceType()));
        task.setReferenceId(request.getReferenceId() == null ? 0L : request.getReferenceId());
        task.setCategory(FileCategoryEnum.normalize(request.getCategory()));
        task.setIsPublic(CollectionUtils.defaultInt(request.getIsPublic(), 0));
        task.setRemark(request.getRemark() != null && !request.getRemark().isBlank() ? request.getRemark().trim() : null);
        task.setMimeType(request.getMimeType() != null && !request.getMimeType().isBlank() ? request.getMimeType() : null);
        if (Integer.valueOf(0).equals(task.getIsChunked())) {
            task.setChunkSize(null);
            task.setTotalChunks(null);
        }
    }

    // ==================== FileInfo 创建 ====================

    @Mapping(target = "uploadTaskId", source = "task.id")
    @Mapping(target = "fileName", qualifiedByName = "extractFileName", source = "objectName")
    @Mapping(target = "filePath", source = "objectName")
    @Mapping(target = "fileUrl", source = "url")
    @Mapping(target = "fileType", qualifiedByName = "resolveFileType", source = "task.mimeType")
    @Mapping(target = "fileExtension", expression = "java(com.cybzacg.blogbackend.utils.FileUtils.normalizeExt(com.cybzacg.blogbackend.utils.FileUtils.getExtension(task.getOriginalName())))")
    @Mapping(target = "isPublic", expression = "java(com.cybzacg.blogbackend.utils.CollectionUtils.defaultInt(task.getIsPublic(), 0))")
    @Mapping(target = "category", expression = "java(com.cybzacg.blogbackend.enums.file.FileCategoryEnum.normalize(task.getCategory()))")
    @Mapping(target = "downloadCount", constant = "0")
    @Mapping(target = "referenceCount", constant = "0")
    @Mapping(target = "status", expression = "java(com.cybzacg.blogbackend.enums.file.FileStatusEnum.NORMAL.getValue())")
    FileInfo toFileInfo(FileUploadTask task, String objectName, String url, String md5);

    // ==================== FileInfo 更新（冲突复用） ====================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "md5", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFileInfoFromCandidate(@MappingTarget FileInfo target, FileInfo candidate);

    // ==================== FileBusinessInfo 创建 ====================

    FileBusinessInfo toFileBusinessInfo(Long fileId, Long userId, String referenceType,
                                        Long referenceId, String sourceIp,
                                        Integer isPublic, String category, String remark);

    // ==================== FileChunk 创建 ====================

    @Mapping(target = "uploadTaskId", source = "taskId")
    @Mapping(target = "chunkSize", source = "size")
    @Mapping(target = "chunkMd5", source = "md5")
    @Mapping(target = "uploadStatus", constant = "2")
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "uploadTime", expression = "java(java.time.LocalDateTime.now())")
    FileChunk toFileChunk(Long taskId, Integer chunkNumber, Long size, String md5);

    // ==================== 辅助方法（带 @Named 避免歧义） ====================

    @Named("extractFileName")
    default String extractFileName(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return objectName;
        }
        int idx = objectName.lastIndexOf('/');
        return idx < 0 ? objectName : objectName.substring(idx + 1);
    }

    @Named("resolveFileType")
    default String resolveFileType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "other";
        }
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("excel") || mimeType.contains("text")) {
            return "document";
        }
        return "other";
    }
}

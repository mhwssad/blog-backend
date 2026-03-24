package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件上传任务表。
 */
@Data
@TableName("file_upload_task")
public class FileUploadTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadId;
    private Long fileId;
    private Long uploadUserId;
    private String storageKey;
    private String sourceIp;

    private Integer isQuickUpload;
    private Long referencedFileId;
    private String fileMd5;
    private Long fileSize;
    private String originalName;
    private String mimeType;

    private String referenceType;
    private Long referenceId;
    private String category;
    private Integer isPublic;
    private String remark;

    private Integer isChunked;
    private Long chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunks;

    private Integer taskStatus;
    private Integer retryCount;
    private Date startTime;
    private Date completeTime;
    private Date quickUploadTime;
    private Date expireTime;

    private String errorCode;
    private String errorMessage;

    private Date createdAt;
    private Date updatedAt;
}

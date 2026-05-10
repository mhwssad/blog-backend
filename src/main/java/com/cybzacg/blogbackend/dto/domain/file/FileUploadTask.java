package com.cybzacg.blogbackend.dto.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件上传任务表。
 */
@Data
@TableName("file_upload_task")
public class FileUploadTask {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传唯一标识（UUID）
     */
    private String uploadId;
    /**
     * 关联文件ID（上传完成后回填）
     */
    private Long fileId;
    /**
     * 上传用户ID
     */
    private Long uploadUserId;
    /**
     * 存储唯一键
     */
    private String storageKey;
    /**
     * 来源IP地址
     */
    private String sourceIp;
    /**
     * 是否秒传：0-否，1-是
     */
    private Integer isQuickUpload;
    /**
     * 秒传引用的已有文件ID
     */
    private Long referencedFileId;
    /**
     * 文件MD5哈希值
     */
    private String fileMd5;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 原始文件名
     */
    private String originalName;
    /**
     * MIME类型
     */
    private String mimeType;
    /**
     * 业务引用类型（avatar-头像，chat_message-聊天消息文件，article_attachment-文章附件，temp-临时文件）
     */
    private String referenceType;
    /**
     * 业务引用ID
     */
    private Long referenceId;
    /**
     * 文件分类
     */
    private String category;
    /**
     * 是否公开：0-私密，1-公开
     */
    private Integer isPublic;
    /**
     * 备注
     */
    private String remark;
    /**
     * 是否分片上传：0-否，1-是
     */
    private Integer isChunked;
    /**
     * 分片大小（字节）
     */
    private Long chunkSize;
    /**
     * 总分片数
     */
    private Integer totalChunks;
    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;
    /**
     * 任务状态：0-待上传，1-上传中，2-已完成，3-失败，4-已过期
     */
    private Integer taskStatus;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 开始上传时间
     */
    private LocalDateTime startTime;
    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
    /**
     * 秒传完成时间
     */
    private LocalDateTime quickUploadTime;
    /**
     * 任务过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 错误码
     */
    private String errorCode;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

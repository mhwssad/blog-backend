package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分片表。
 */
@Data
@TableName("file_chunk")
public class FileChunk {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联上传任务ID */
    private Long uploadTaskId;
    /** 分片序号（从1开始） */
    private Integer chunkNumber;
    /** 分片大小（字节） */
    private Long chunkSize;
    /** 分片MD5哈希值 */
    private String chunkMd5;
    /** 上传状态：0-待上传，1-已上传 */
    private Integer uploadStatus;
    /** 重试次数 */
    private Integer retryCount;
    /** 上传完成时间 */
    private LocalDateTime uploadTime;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}

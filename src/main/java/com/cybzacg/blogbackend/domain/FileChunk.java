package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件分片表。
 */
@Data
@TableName("file_chunk")
public class FileChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploadTaskId;
    private Integer chunkNumber;
    private Long chunkSize;
    private String chunkMd5;
    private Integer uploadStatus;
    private Integer retryCount;
    private Date uploadTime;
    private Date createdAt;
    private Date updatedAt;
}

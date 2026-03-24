package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件物理信息表。
 */
@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploadTaskId;
    private String fileName;
    private String originalName;
    private String filePath;
    private String storageKey;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String fileExtension;
    private String md5;
    private Integer referenceCount;
    private Integer isPublic;
    private String category;
    private Integer downloadCount;
    private Long uploadUserId;
    private String remark;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}

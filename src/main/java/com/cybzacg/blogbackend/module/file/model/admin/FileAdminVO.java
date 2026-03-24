package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "后台文件列表项")
public class FileAdminVO {
    @Schema(description = "文件ID")
    private Long id;
    @Schema(description = "文件名称")
    private String fileName;
    @Schema(description = "原始文件名")
    private String originalName;
    @Schema(description = "文件路径")
    private String filePath;
    @Schema(description = "文件地址")
    private String fileUrl;
    @Schema(description = "存储节点")
    private String storageKey;
    @Schema(description = "文件大小")
    private Long fileSize;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "MIME类型")
    private String mimeType;
    @Schema(description = "扩展名")
    private String fileExtension;
    @Schema(description = "上传用户ID")
    private Long uploadUserId;
    @Schema(description = "是否公开")
    private Integer isPublic;
    @Schema(description = "业务分类")
    private String category;
    @Schema(description = "文件状态")
    private Integer status;
    @Schema(description = "引用数")
    private Integer referenceCount;
    @Schema(description = "创建时间")
    private Date createdAt;
}

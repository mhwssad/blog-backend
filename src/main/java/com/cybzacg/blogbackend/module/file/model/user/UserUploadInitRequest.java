package com.cybzacg.blogbackend.module.file.model.user;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.validation.SafeFileName;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户上传初始化请求。
 */
@Data
@Schema(description = "用户文件上传初始化请求")
public class UserUploadInitRequest {
    @NotBlank(message = "原始文件名不能为空")
    @SafeFileName(message = "文件名包含非法字符或不允许双重扩展名")
    @Schema(description = "原始文件名")
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "MIME类型")
    private String mimeType;

    @EnumValue(enumClass = FileReferenceTypeEnum.class, message = "文件引用类型非法")
    @Schema(description = "引用类型")
    private String referenceType;

    @Schema(description = "引用对象ID")
    private Long referenceId;

    @EnumValue(enumClass = FileCategoryEnum.class, message = "文件分类非法")
    @Schema(description = "业务分类")
    private String category;

    @Min(value = 0, message = "文件可见性非法")
    @Max(value = 1, message = "文件可见性非法")
    @Schema(description = "是否公开：0-私有，1-公开")
    private Integer isPublic;

    @Schema(description = "总分片数")
    private Integer totalChunks;

    @Schema(description = "分片大小")
    private Long chunkSize;

    @Schema(description = "备注")
    private String remark;
}

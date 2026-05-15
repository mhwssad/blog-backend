package com.cybzacg.blogbackend.module.file.model.user;

import com.cybzacg.blogbackend.core.validation.AllowedFileExtension;
import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.validation.MaxUploadFileSize;
import com.cybzacg.blogbackend.core.validation.Md5Required;
import com.cybzacg.blogbackend.core.validation.SafeFileName;
import com.cybzacg.blogbackend.core.validation.ValidChunkParams;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@ValidChunkParams
@Schema(description = "初始化文件上传任务请求")
public class FileUploadInitRequest {

    @NotBlank(message = "原始文件名不能为空")
    @SafeFileName(message = "文件名包含非法字符或不允许双重扩展名")
    @AllowedFileExtension(message = "不支持的文件类型")
    @Schema(
        description = "原始文件名",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    @MaxUploadFileSize(message = "文件大小超出限制")
    @Schema(
        description = "文件大小（字节）",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long fileSize;

    @Md5Required(message = "文件MD5不能为空")
    @Schema(description = "文件MD5")
    private String fileMd5;

    @Schema(description = "MIME类型")
    private String mimeType;

    @EnumValue(enumClass = FileReferenceTypeEnum.class, message = "文件引用类型非法")
    @Schema(
        description = "引用类型：avatar/chat_message/article_attachment/temp"
    )
    private String referenceType;

    @Schema(description = "引用对象ID")
    private Long referenceId;

    @EnumValue(enumClass = FileCategoryEnum.class, message = "文件分类非法")
    @Schema(description = "业务分类，如 avatar/attachment/comment/temp")
    private String category;

    @Min(value = 0, message = "文件可见性非法")
    @Max(value = 1, message = "文件可见性非法")
    @Schema(description = "是否公开：0-私有，1-公开")
    private Integer isPublic;

    @Schema(description = "总分片数，普通上传可为空")
    private Integer totalChunks;

    @Schema(description = "分片大小，普通上传可为空")
    private Long chunkSize;

    @Schema(description = "备注")
    private String remark;
}

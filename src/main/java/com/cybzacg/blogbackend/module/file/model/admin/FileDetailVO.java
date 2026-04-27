package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Schema(description = "后台文件详情")
@EqualsAndHashCode(callSuper = true)
public class FileDetailVO extends FileAdminVO {
    @Schema(description = "引用列表")
    private List<FileReferenceVO> references;
    @Schema(description = "相关上传任务")
    private List<FileTaskAdminVO> tasks;
}

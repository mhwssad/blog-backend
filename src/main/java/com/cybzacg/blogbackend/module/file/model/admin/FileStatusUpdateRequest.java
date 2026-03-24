package com.cybzacg.blogbackend.module.file.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "文件状态更新请求")
public class FileStatusUpdateRequest {
    @NotNull(message = "文件状态不能为空")
    @Schema(description = "文件状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}

package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "状态更新请求")
public class StatusUpdateRequest {
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态值")
    private Integer status;
}

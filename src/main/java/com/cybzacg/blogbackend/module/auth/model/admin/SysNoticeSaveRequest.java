package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "通知新增/修改请求")
public class SysNoticeSaveRequest {
    @NotBlank(message = "通知标题不能为空")
    @Schema(description = "通知标题")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Schema(description = "通知内容")
    private String content;

    @NotNull(message = "通知类型不能为空")
    @Schema(description = "通知类型")
    private Integer type;

    @NotBlank(message = "通知等级不能为空")
    @Schema(description = "通知等级")
    private String level;

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型 1-全体 2-指定")
    private Integer targetType;

    @Schema(description = "目标用户ID列表")
    private List<Long> targetUserIds;
}

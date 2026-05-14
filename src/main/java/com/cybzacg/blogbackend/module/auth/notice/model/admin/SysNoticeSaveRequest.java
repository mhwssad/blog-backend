package com.cybzacg.blogbackend.module.auth.notice.model.admin;

import com.cybzacg.blogbackend.core.validation.ConditionalNotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@ConditionalNotEmpty(field = "targetUserIds", dependsOn = "targetType", values = {"2"}, message = "指定用户通知至少选择一个目标用户")
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
    @Min(value = 1, message = "通知目标类型非法")
    @Max(value = 2, message = "通知目标类型非法")
    @Schema(description = "目标类型 1-全体 2-指定")
    private Integer targetType;

    @Schema(description = "目标用户ID列表")
    private List<Long> targetUserIds;
}

package com.cybzacg.blogbackend.module.auth.notice.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户通知偏好批量更新单项请求。
 */
@Data
@Schema(description = "用户通知偏好批量更新单项请求")
public class UserNotificationSettingBatchUpdateItemRequest {
    @NotBlank(message = "通知类型不能为空")
    @Schema(description = "通知类型编码")
    private String type;

    @NotNull(message = "通知开关状态不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled;
}

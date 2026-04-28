package com.cybzacg.blogbackend.module.auth.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户通知偏好开关更新请求。
 */
@Data
@Schema(description = "用户通知偏好开关更新请求")
public class UserNotificationSettingStatusUpdateRequest {
    @NotNull(message = "通知开关状态不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled;
}

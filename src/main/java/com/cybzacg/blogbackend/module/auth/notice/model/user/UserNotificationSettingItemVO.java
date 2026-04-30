package com.cybzacg.blogbackend.module.auth.notice.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户通知偏好单项视图。
 */
@Data
@Builder
@Schema(description = "用户通知偏好单项视图")
public class UserNotificationSettingItemVO {
    @Schema(description = "通知类型编码")
    private String type;

    @Schema(description = "通知类型名称")
    private String label;

    @Schema(description = "是否启用")
    private Boolean enabled;
}

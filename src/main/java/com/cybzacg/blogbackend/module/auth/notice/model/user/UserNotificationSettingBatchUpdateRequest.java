package com.cybzacg.blogbackend.module.auth.notice.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户通知偏好批量更新请求。
 */
@Data
@Schema(description = "用户通知偏好批量更新请求")
public class UserNotificationSettingBatchUpdateRequest {
    @Valid
    @NotEmpty(message = "通知设置不能为空")
    @Size(max = 9, message = "单次更新的通知类型数量不能超过9个")
    @Schema(description = "通知设置列表")
    private List<UserNotificationSettingBatchUpdateItemRequest> settings;
}

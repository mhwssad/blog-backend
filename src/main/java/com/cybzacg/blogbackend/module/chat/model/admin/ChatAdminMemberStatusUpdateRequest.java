package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台更新成员状态请求。
 */
@Data
@Schema(description = "后台更新成员状态请求")
public class ChatAdminMemberStatusUpdateRequest {
    @NotNull(message = "成员状态不能为空")
    @Schema(description = "成员状态：0-已退出，1-正常，2-已移除，3-已禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}

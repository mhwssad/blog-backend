package com.cybzacg.blogbackend.module.chat.member.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台更新成员状态请求。
 */
@Data
@Schema(description = "后台更新成员状态请求")
public class ChatAdminMemberStatusUpdateRequest {
    @NotNull(message = "成员状态不能为空")
    @Min(value = 0, message = "成员状态必须在 0-3 之间")
    @Max(value = 3, message = "成员状态必须在 0-3 之间")
    @Schema(description = "成员状态：0-已退出，1-正常，2-已移除，3-已禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}

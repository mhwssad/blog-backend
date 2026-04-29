package com.cybzacg.blogbackend.module.report.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员处理举报请求。
 */
@Data
@Schema(description = "管理员处理举报请求")
public class ReportHandleRequest {
    @NotBlank(message = "处理结果类型不能为空")
    @Schema(description = "处理结果类型：delete_content/revoke_message/mute_user/ban_user/record_only",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String resultType;

    @Schema(description = "处罚类型：content_delete/message_revoke/mute/ban/none")
    private String punishmentType;

    @Size(max = 512, message = "备注不能超过512字符")
    @Schema(description = "处理备注")
    private String remark;

    @Schema(description = "会话ID（举报聊天消息时必填）")
    private Long conversationId;
}

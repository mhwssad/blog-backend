package com.cybzacg.blogbackend.module.report.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员驳回举报请求。
 */
@Data
@Schema(description = "驳回举报请求")
public class ReportRejectRequest {
    @Size(max = 512, message = "备注不能超过512字符")
    @Schema(description = "驳回原因")
    private String remark;
}

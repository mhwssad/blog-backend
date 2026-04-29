package com.cybzacg.blogbackend.module.report.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台举报详情 VO。
 */
@Data
@Schema(description = "后台举报详情")
public class ReportAdminVO {
    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报对象类型")
    private String reportTargetType;

    @Schema(description = "举报对象ID")
    private Long reportTargetId;

    @Schema(description = "举报人ID")
    private Long reporterUserId;

    @Schema(description = "举报人用户名")
    private String reporterUsername;

    @Schema(description = "举报原因编码")
    private String reasonCode;

    @Schema(description = "补充说明")
    private String reasonDetail;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "处理人ID")
    private Long handlerUserId;

    @Schema(description = "处理人用户名")
    private String handlerUsername;

    @Schema(description = "处理结果类型")
    private String resultType;

    @Schema(description = "处罚类型")
    private String punishmentType;

    @Schema(description = "举报时间")
    private LocalDateTime reportedAt;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

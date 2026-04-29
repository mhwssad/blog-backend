package com.cybzacg.blogbackend.module.report.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报处理日志 VO。
 */
@Data
@Schema(description = "举报处理日志")
public class ReportHandleLogVO {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "变更前状态")
    private Integer fromStatus;

    @Schema(description = "变更后状态")
    private Integer toStatus;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "处理结果")
    private String actionResult;

    @Schema(description = "操作人ID")
    private Long operatorUserId;

    @Schema(description = "操作人用户名")
    private String operatorUsername;

    @Schema(description = "操作备注")
    private String actionRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

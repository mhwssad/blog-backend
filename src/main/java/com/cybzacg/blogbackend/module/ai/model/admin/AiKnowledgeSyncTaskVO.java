package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识同步任务响应 VO。
 */
@Data
@Schema(description = "知识同步任务信息")
public class AiKnowledgeSyncTaskVO {
    @Schema(description = "任务ID")
    private Long id;
    @Schema(description = "任务类型")
    private String taskType;
    @Schema(description = "知识源类型")
    private String sourceType;
    @Schema(description = "状态：0-待执行，1-执行中，2-已完成，3-失败")
    private Integer status;
    @Schema(description = "总条目数")
    private Integer totalCount;
    @Schema(description = "成功条目数")
    private Integer successCount;
    @Schema(description = "失败条目数")
    private Integer failCount;
    @Schema(description = "跳过条目数")
    private Integer skipCount;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "已重试次数")
    private Integer retryCount;
    @Schema(description = "最大重试次数")
    private Integer maxRetry;
    @Schema(description = "开始执行时间")
    private LocalDateTime startedAt;
    @Schema(description = "执行完成时间")
    private LocalDateTime completedAt;
    @Schema(description = "触发方式")
    private String triggeredBy;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

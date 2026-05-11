package com.cybzacg.blogbackend.module.report.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台举报分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台举报分页查询条件")
public class ReportAdminPageQuery extends PageQuery {
    @Min(value = 0, message = "举报状态不合法")
    @Max(value = 3, message = "举报状态不合法")
    @Schema(description = "状态：0-待处理，1-处理中，2-已处理，3-已驳回")
    private Integer status;

    @Schema(description = "举报对象类型：article/comment/chat_message")
    private String reportTargetType;

    @Schema(description = "举报人ID")
    private Long reporterUserId;

    @Schema(description = "举报开始时间")
    private LocalDateTime reportedStart;

    @Schema(description = "举报结束时间")
    private LocalDateTime reportedEnd;

    @Schema(description = "每页条数")
    private Long size = 20L;
}

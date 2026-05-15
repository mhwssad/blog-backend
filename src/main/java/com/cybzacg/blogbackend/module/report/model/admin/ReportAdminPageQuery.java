package com.cybzacg.blogbackend.module.report.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.web.PageQuery;
import com.cybzacg.blogbackend.enums.report.ReportRecordStatusEnum;
import com.cybzacg.blogbackend.enums.report.ReportTargetTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台举报分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台举报分页查询条件")
public class ReportAdminPageQuery extends PageQuery {
    @EnumValue(enumClass = ReportRecordStatusEnum.class, message = "举报状态不合法")
    @Schema(description = "状态：0-待处理，1-处理中，2-已处理，3-已驳回")
    private Integer status;

    @EnumValue(enumClass = ReportTargetTypeEnum.class, method = "getCode", message = "举报对象类型不合法")
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

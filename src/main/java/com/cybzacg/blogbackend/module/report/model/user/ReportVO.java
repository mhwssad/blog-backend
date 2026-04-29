package com.cybzacg.blogbackend.module.report.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户举报记录 VO。
 */
@Data
@Schema(description = "用户举报记录")
public class ReportVO {
    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报对象类型")
    private String targetType;

    @Schema(description = "举报对象ID")
    private Long targetId;

    @Schema(description = "举报原因编码")
    private String reasonCode;

    @Schema(description = "补充说明")
    private String reasonDetail;

    @Schema(description = "状态：0-待处理/1-处理中/2-已处理/3-已驳回")
    private Integer status;

    @Schema(description = "举报时间")
    private LocalDateTime reportedAt;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @Schema(description = "处理结果类型")
    private String resultType;

    @Schema(description = "备注")
    private String remark;
}

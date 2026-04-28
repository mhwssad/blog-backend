package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台看板时间范围查询。
 */
@Data
@Schema(description = "后台看板时间范围查询")
public class DashboardRangeQuery {
    @Schema(description = "时间范围：today/week/month/all/custom，默认 today")
    private String rangeType;

    @Schema(description = "自定义开始时间，rangeType=custom 时必填")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "自定义结束时间，rangeType=custom 时必填")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}

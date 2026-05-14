package com.cybzacg.blogbackend.module.dashboard.model.admin;

import com.cybzacg.blogbackend.core.validation.ValidTimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台看板时间范围查询。
 */
@Data
@Schema(description = "后台看板时间范围查询")
@ValidTimeRange(conditionField = "rangeType", conditionValue = "custom", maxDays = 366)
public class DashboardRangeQuery {
    @Pattern(regexp = "today|week|month|all|custom", message = "时间范围类型仅支持 today/week/month/all/custom")
    @Schema(description = "时间范围：today/week/month/all/custom，默认 today")
    private String rangeType;

    @Schema(description = "自定义开始时间，rangeType=custom 时必填")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "自定义结束时间，rangeType=custom 时必填")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}

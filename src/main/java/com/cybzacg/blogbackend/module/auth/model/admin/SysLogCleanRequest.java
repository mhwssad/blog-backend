package com.cybzacg.blogbackend.module.auth.model.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@Schema(description = "日志清理请求")
public class SysLogCleanRequest {
    @Schema(description = "日志模块")
    private String module;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "创建人ID")
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建开始时间", example = "2026-03-18 00:00:00")
    private Date createTimeStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建结束时间", example = "2026-03-18 23:59:59")
    private Date createTimeEnd;
}

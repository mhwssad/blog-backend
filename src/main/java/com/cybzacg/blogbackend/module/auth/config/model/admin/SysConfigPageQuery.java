package com.cybzacg.blogbackend.module.auth.config.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置分页查询条件")
public class SysConfigPageQuery extends PageQuery {

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置键")
    private String configKey;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建开始时间", example = "2026-03-18 00:00:00")
    private LocalDateTime createTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建结束时间", example = "2026-03-18 23:59:59")
    private LocalDateTime createTimeEnd;
}

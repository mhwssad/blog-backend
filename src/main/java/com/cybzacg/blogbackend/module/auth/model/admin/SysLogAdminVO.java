package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "系统日志信息")
public class SysLogAdminVO {
    @Schema(description = "日志ID")
    private Long id;
    @Schema(description = "日志模块")
    private String module;
    @Schema(description = "请求方式")
    private String requestMethod;
    @Schema(description = "请求参数")
    private String requestParams;
    @Schema(description = "响应内容")
    private String responseContent;
    @Schema(description = "日志内容")
    private String content;
    @Schema(description = "请求路径")
    private String requestUri;
    @Schema(description = "处理方法")
    private String method;
    @Schema(description = "IP地址")
    private String ip;
    @Schema(description = "省份")
    private String province;
    @Schema(description = "城市")
    private String city;
    @Schema(description = "执行耗时")
    private Long executionTime;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "浏览器版本")
    private String browserVersion;
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "创建人ID")
    private Long createBy;
    @Schema(description = "创建时间")
    private Date createTime;
}

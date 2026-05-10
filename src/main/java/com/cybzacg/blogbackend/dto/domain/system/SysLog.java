package com.cybzacg.blogbackend.dto.domain.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志表。
 */
@Data
@TableName("sys_log")
public class SysLog {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 操作模块
     */
    private String module;
    /**
     * HTTP请求方法（GET/POST/PUT/DELETE）
     */
    private String requestMethod;
    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;
    /**
     * 响应内容（截断）
     */
    private String responseContent;
    /**
     * 操作描述
     */
    private String content;
    /**
     * 请求URI
     */
    private String requestUri;
    /**
     * 处理方法（类名.方法名）
     */
    private String method;
    /**
     * 请求IP
     */
    private String ip;
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 执行时长（毫秒）
     */
    private Long executionTime;
    /**
     * 浏览器名称
     */
    private String browser;
    /**
     * 浏览器版本
     */
    private String browserVersion;
    /**
     * 操作系统
     */
    private String os;
    /**
     * 操作人ID
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

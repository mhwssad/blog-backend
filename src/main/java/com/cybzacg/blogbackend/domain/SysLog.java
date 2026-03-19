package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 系统操作日志表
 * @TableName sys_log
 */
@TableName(value ="sys_log")
@Data
public class SysLog {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志模块
     */
    private String module;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求参数(批量请求参数可能会超过text)
     */
    private String requestParams;

    /**
     * 返回参数
     */
    private String responseContent;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * 方法名
     */
    private String method;

    /**
     * IP地址
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
     * 执行时间(ms)
     */
    private Long executionTime;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 浏览器版本
     */
    private String browserVersion;

    /**
     * 终端系统
     */
    private String os;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        SysLog other = (SysLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getModule() == null ? other.getModule() == null : this.getModule().equals(other.getModule()))
            && (this.getRequestMethod() == null ? other.getRequestMethod() == null : this.getRequestMethod().equals(other.getRequestMethod()))
            && (this.getRequestParams() == null ? other.getRequestParams() == null : this.getRequestParams().equals(other.getRequestParams()))
            && (this.getResponseContent() == null ? other.getResponseContent() == null : this.getResponseContent().equals(other.getResponseContent()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getRequestUri() == null ? other.getRequestUri() == null : this.getRequestUri().equals(other.getRequestUri()))
            && (this.getMethod() == null ? other.getMethod() == null : this.getMethod().equals(other.getMethod()))
            && (this.getIp() == null ? other.getIp() == null : this.getIp().equals(other.getIp()))
            && (this.getProvince() == null ? other.getProvince() == null : this.getProvince().equals(other.getProvince()))
            && (this.getCity() == null ? other.getCity() == null : this.getCity().equals(other.getCity()))
            && (this.getExecutionTime() == null ? other.getExecutionTime() == null : this.getExecutionTime().equals(other.getExecutionTime()))
            && (this.getBrowser() == null ? other.getBrowser() == null : this.getBrowser().equals(other.getBrowser()))
            && (this.getBrowserVersion() == null ? other.getBrowserVersion() == null : this.getBrowserVersion().equals(other.getBrowserVersion()))
            && (this.getOs() == null ? other.getOs() == null : this.getOs().equals(other.getOs()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getModule() == null) ? 0 : getModule().hashCode());
        result = prime * result + ((getRequestMethod() == null) ? 0 : getRequestMethod().hashCode());
        result = prime * result + ((getRequestParams() == null) ? 0 : getRequestParams().hashCode());
        result = prime * result + ((getResponseContent() == null) ? 0 : getResponseContent().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getRequestUri() == null) ? 0 : getRequestUri().hashCode());
        result = prime * result + ((getMethod() == null) ? 0 : getMethod().hashCode());
        result = prime * result + ((getIp() == null) ? 0 : getIp().hashCode());
        result = prime * result + ((getProvince() == null) ? 0 : getProvince().hashCode());
        result = prime * result + ((getCity() == null) ? 0 : getCity().hashCode());
        result = prime * result + ((getExecutionTime() == null) ? 0 : getExecutionTime().hashCode());
        result = prime * result + ((getBrowser() == null) ? 0 : getBrowser().hashCode());
        result = prime * result + ((getBrowserVersion() == null) ? 0 : getBrowserVersion().hashCode());
        result = prime * result + ((getOs() == null) ? 0 : getOs().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", module=").append(module);
        sb.append(", requestMethod=").append(requestMethod);
        sb.append(", requestParams=").append(requestParams);
        sb.append(", responseContent=").append(responseContent);
        sb.append(", content=").append(content);
        sb.append(", requestUri=").append(requestUri);
        sb.append(", method=").append(method);
        sb.append(", ip=").append(ip);
        sb.append(", province=").append(province);
        sb.append(", city=").append(city);
        sb.append(", executionTime=").append(executionTime);
        sb.append(", browser=").append(browser);
        sb.append(", browserVersion=").append(browserVersion);
        sb.append(", os=").append(os);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}

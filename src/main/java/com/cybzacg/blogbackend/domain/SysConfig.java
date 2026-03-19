package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 系统配置表
 * @TableName sys_config
 */
@TableName(value ="sys_config")
@Data
public class SysConfig {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置key
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 逻辑删除标识(0-未删除 1-已删除)
     */
    private Integer isDeleted;

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
        SysConfig other = (SysConfig) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getConfigName() == null ? other.getConfigName() == null : this.getConfigName().equals(other.getConfigName()))
            && (this.getConfigKey() == null ? other.getConfigKey() == null : this.getConfigKey().equals(other.getConfigKey()))
            && (this.getConfigValue() == null ? other.getConfigValue() == null : this.getConfigValue().equals(other.getConfigValue()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getConfigName() == null) ? 0 : getConfigName().hashCode());
        result = prime * result + ((getConfigKey() == null) ? 0 : getConfigKey().hashCode());
        result = prime * result + ((getConfigValue() == null) ? 0 : getConfigValue().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", configName=").append(configName);
        sb.append(", configKey=").append(configKey);
        sb.append(", configValue=").append(configValue);
        sb.append(", remark=").append(remark);
        sb.append(", createTime=").append(createTime);
        sb.append(", createBy=").append(createBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", isDeleted=").append(isDeleted);
        sb.append("]");
        return sb.toString();
    }
}

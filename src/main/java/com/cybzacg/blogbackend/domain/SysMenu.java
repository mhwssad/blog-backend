package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 系统菜单表
 * @TableName sys_menu
 */
@TableName(value ="sys_menu")
@Data
public class SysMenu {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 父节点ID路径
     */
    private String treePath;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型（C-目录 M-菜单 B-按钮）
     */
    private String type;

    /**
     * 路由名称（Vue Router 中用于命名路由）
     */
    private String routeName;

    /**
     * 路由路径（Vue Router 中定义的 URL 路径）
     */
    private String routePath;

    /**
     * 组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）
     */
    private String component;

    /**
     * 【按钮】权限标识
     */
    private String perm;

    /**
     * 【目录】只有一个子路由是否始终显示（1-是 0-否）
     */
    private Integer alwaysShow;

    /**
     * 【菜单】是否开启页面缓存（1-是 0-否）
     */
    private Integer keepAlive;

    /**
     * 显示状态（1-显示 0-隐藏）
     */
    private Integer visible;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 跳转路径
     */
    private String redirect;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 路由参数
     */
    private Object params;

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
        SysMenu other = (SysMenu) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getParentId() == null ? other.getParentId() == null : this.getParentId().equals(other.getParentId()))
            && (this.getTreePath() == null ? other.getTreePath() == null : this.getTreePath().equals(other.getTreePath()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getRouteName() == null ? other.getRouteName() == null : this.getRouteName().equals(other.getRouteName()))
            && (this.getRoutePath() == null ? other.getRoutePath() == null : this.getRoutePath().equals(other.getRoutePath()))
            && (this.getComponent() == null ? other.getComponent() == null : this.getComponent().equals(other.getComponent()))
            && (this.getPerm() == null ? other.getPerm() == null : this.getPerm().equals(other.getPerm()))
            && (this.getAlwaysShow() == null ? other.getAlwaysShow() == null : this.getAlwaysShow().equals(other.getAlwaysShow()))
            && (this.getKeepAlive() == null ? other.getKeepAlive() == null : this.getKeepAlive().equals(other.getKeepAlive()))
            && (this.getVisible() == null ? other.getVisible() == null : this.getVisible().equals(other.getVisible()))
            && (this.getSort() == null ? other.getSort() == null : this.getSort().equals(other.getSort()))
            && (this.getIcon() == null ? other.getIcon() == null : this.getIcon().equals(other.getIcon()))
            && (this.getRedirect() == null ? other.getRedirect() == null : this.getRedirect().equals(other.getRedirect()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getParams() == null ? other.getParams() == null : this.getParams().equals(other.getParams()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getParentId() == null) ? 0 : getParentId().hashCode());
        result = prime * result + ((getTreePath() == null) ? 0 : getTreePath().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getRouteName() == null) ? 0 : getRouteName().hashCode());
        result = prime * result + ((getRoutePath() == null) ? 0 : getRoutePath().hashCode());
        result = prime * result + ((getComponent() == null) ? 0 : getComponent().hashCode());
        result = prime * result + ((getPerm() == null) ? 0 : getPerm().hashCode());
        result = prime * result + ((getAlwaysShow() == null) ? 0 : getAlwaysShow().hashCode());
        result = prime * result + ((getKeepAlive() == null) ? 0 : getKeepAlive().hashCode());
        result = prime * result + ((getVisible() == null) ? 0 : getVisible().hashCode());
        result = prime * result + ((getSort() == null) ? 0 : getSort().hashCode());
        result = prime * result + ((getIcon() == null) ? 0 : getIcon().hashCode());
        result = prime * result + ((getRedirect() == null) ? 0 : getRedirect().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getParams() == null) ? 0 : getParams().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", parentId=").append(parentId);
        sb.append(", treePath=").append(treePath);
        sb.append(", name=").append(name);
        sb.append(", type=").append(type);
        sb.append(", routeName=").append(routeName);
        sb.append(", routePath=").append(routePath);
        sb.append(", component=").append(component);
        sb.append(", perm=").append(perm);
        sb.append(", alwaysShow=").append(alwaysShow);
        sb.append(", keepAlive=").append(keepAlive);
        sb.append(", visible=").append(visible);
        sb.append(", sort=").append(sort);
        sb.append(", icon=").append(icon);
        sb.append(", redirect=").append(redirect);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", params=").append(params);
        sb.append("]");
        return sb.toString();
    }
}

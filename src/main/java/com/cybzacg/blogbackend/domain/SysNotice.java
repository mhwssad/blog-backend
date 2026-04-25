package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知公告表
 *
 * @TableName sys_notice
 */
@TableName(value = "sys_notice")
@Data
public class SysNotice {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型（关联字典编码：notice_type）
     */
    private Integer type;

    /**
     * 通知等级（字典code：notice_level）
     */
    private String level;

    /**
     * 目标类型（1: 全体, 2: 指定）
     */
    private Integer targetType;

    /**
     * 目标人ID集合（多个使用英文逗号,分割）
     */
    private String targetUserIds;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布状态（0: 未发布, 1: 已发布, -1: 已撤回）
     */
    private Integer publishStatus;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 撤回时间
     */
    private LocalDateTime revokeTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除（0: 未删除, 1: 已删除）
     */
    private Integer isDeleted;

}

package com.cybzacg.blogbackend.dto.domain.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知公告关联表
 *
 * @TableName sys_user_notice
 */
@TableName(value = "sys_user_notice")
@Data
public class SysUserNotice {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公共通知id
     */
    private Long noticeId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 读取状态（0: 未读, 1: 已读）
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除(0: 未删除, 1: 已删除)
     */
    private Integer isDeleted;

}

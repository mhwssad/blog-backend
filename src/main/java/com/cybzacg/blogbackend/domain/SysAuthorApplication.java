package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者申请表。
 *
 * @TableName sys_author_application
 */
@TableName(value = "sys_author_application")
@Data
public class SysAuthorApplication {
    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请用户ID
     */
    private Long userId;

    /**
     * 申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充
     */
    private Integer applyStatus;

    /**
     * 申请说明
     */
    private String applyReason;

    /**
     * 擅长内容方向
     */
    private String contentDirection;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 示例链接JSON数组
     */
    private String sampleLinksJson;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核备注
     */
    private String reviewComment;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

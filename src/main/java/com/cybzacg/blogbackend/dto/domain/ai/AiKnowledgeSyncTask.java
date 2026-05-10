package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识同步任务表。
 */
@Data
@TableName("ai_knowledge_sync_task")
public class AiKnowledgeSyncTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务类型：full_sync/incremental_sync/single_entry */
    private String taskType;
    /** 知识源类型 */
    private String sourceType;
    /** 状态：0-待执行，1-执行中，2-已完成，3-失败 */
    private Integer status;
    /** 总条目数 */
    private Integer totalCount;
    /** 成功条目数 */
    private Integer successCount;
    /** 失败条目数 */
    private Integer failCount;
    /** 跳过条目数 */
    private Integer skipCount;
    /** 错误信息 */
    private String errorMessage;
    /** 已重试次数 */
    private Integer retryCount;
    /** 最大重试次数 */
    private Integer maxRetry;
    /** 开始执行时间 */
    private LocalDateTime startedAt;
    /** 执行完成时间 */
    private LocalDateTime completedAt;
    /** 触发方式：system/admin/manual */
    private String triggeredBy;
    /** 操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

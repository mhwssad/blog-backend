package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI Agent 任务表。
 */
@Data
@TableName("ai_agent_task")
public class AiAgentTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 发起用户 */
    private Long userId;
    /** 关联 agent 定义 ID */
    private Long agentId;
    /** 0-待执行 1-执行中 2-已完成 3-失败 4-已取消 */
    private Integer status;
    /** 用户输入 */
    private String inputContent;
    /** agent 输出 */
    private String outputContent;
    /** 错误信息 */
    private String errorMessage;
    /** 消耗 token 数 */
    private Integer tokenCount;
    /** 开始时间 */
    private LocalDateTime startedAt;
    /** 完成时间 */
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

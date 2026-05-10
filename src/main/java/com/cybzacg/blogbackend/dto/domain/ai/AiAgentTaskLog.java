package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI Agent 任务日志表。
 */
@Data
@TableName("ai_agent_task_log")
public class AiAgentTaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联任务 ID */
    private Long taskId;
    /** 轮次序号 */
    private Integer turnIndex;
    /** user / assistant / system */
    private String roleType;
    /** 消息内容 */
    private String content;
    /** token 数 */
    private Integer tokenCount;
    private LocalDateTime createdAt;
}

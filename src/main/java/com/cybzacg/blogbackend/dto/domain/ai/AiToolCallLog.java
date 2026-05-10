package com.cybzacg.blogbackend.dto.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具调用日志表。
 */
@Data
@TableName("ai_tool_call_log")
public class AiToolCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long agentId;
    private Long sessionId;
    private Long taskId;
    private Long toolId;
    private String toolCode;
    private String toolName;
    private String requestSceneType;
    private String requestSummary;
    private String responseSummary;
    private Integer successStatus;
    private Long elapsedMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}

package com.cybzacg.blogbackend.module.ai.model.internal;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * AI 工具执行上下文。
 */
@Data
@Builder
public class AiToolExecutionContext {
    private Long userId;
    private Long agentId;
    private Long sessionId;
    private Long taskId;
    private String sceneType;
    private String dataScope;
    private String toolCode;
    private Long toolId;
    private String toolName;
    private Set<String> authorities;
}

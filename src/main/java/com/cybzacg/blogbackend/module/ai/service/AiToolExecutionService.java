package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;

/**
 * AI 工具统一执行服务。
 */
public interface AiToolExecutionService {
    void validateAuthorized(String toolCode, AiToolExecutionContext context);

    AiToolExecuteVO execute(String toolCode, String arguments, AiToolExecutionContext context);
}

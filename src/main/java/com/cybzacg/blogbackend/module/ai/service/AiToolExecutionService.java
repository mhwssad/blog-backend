package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;

/**
 * AI 工具统一执行服务。
 *
 * <p>提供工具调用的权限校验与统一执行入口，Agent 在对话过程中需要调用外部工具时由此服务调度。
 */
public interface AiToolExecutionService {

    /**
     * 校验当前上下文是否有权执行指定工具。
     *
     * @param toolCode 工具编码
     * @param context  工具执行上下文（含用户ID、会话ID等）
     */
    void validateAuthorized(String toolCode, AiToolExecutionContext context);

    /**
     * 执行指定工具并返回结果。
     *
     * @param toolCode  工具编码
     * @param arguments 工具入参 JSON 字符串
     * @param context   工具执行上下文（含用户ID、会话ID等）
     * @return 工具执行结果视图对象
     */
    AiToolExecuteVO execute(String toolCode, String arguments, AiToolExecutionContext context);
}

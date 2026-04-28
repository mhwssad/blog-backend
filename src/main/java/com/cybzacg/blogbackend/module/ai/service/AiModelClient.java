package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.domain.AiChannelConfig;
import com.cybzacg.blogbackend.domain.AiChatMessage;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;

import java.util.List;

/**
 * AI 模型调用客户端。
 *
 * <p>封装模型构建、消息组装、上下文裁剪与异常兜底，供上层业务统一调用。
 */
public interface AiModelClient {

    /**
     * 发起一次 AI 对话调用。
     *
     * @param config          渠道配置（含 apiBaseUrl、apiKeyEncrypted、modelName、maxContextTokens 等）
     * @param systemPrompt    系统提示词
     * @param contextMessages 历史上下文消息（按时间正序）
     * @param userQuestion    当前用户提问
     * @return 调用结果，包含响应内容、token 用量或错误信息
     */
    AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                           List<AiChatMessage> contextMessages, String userQuestion);
}

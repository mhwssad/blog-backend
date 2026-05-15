package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.dto.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.data.AiStreamEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 模型调用客户端。
 *
 * <p>封装模型构建、消息组装、上下文裁剪与异常兜底，供上层业务统一调用。
 */
public interface AiModelClient {

    /**
     * 发起一次 AI 对话调用。
     *
     * @param config          渠道配置（含模型名、API 地址等）
     * @param systemPrompt    系统提示词
     * @param contextMessages 上下文历史消息列表
     * @param userQuestion    用户当前提问
     * @return 模型调用结果（含响应文本、token 用量等）
     */
    AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                           List<AiChatMessage> contextMessages, String userQuestion);

    /**
     * 发起一次多模态 AI 对话调用（支持图片附件）。
     *
     * @param config            渠道配置（含模型名、API 地址等）
     * @param systemPrompt      系统提示词
     * @param contextMessages   上下文历史消息列表
     * @param userQuestion      用户当前提问
     * @param imageAttachments  图片附件列表
     * @return 模型调用结果（含响应文本、token 用量等）
     */
    AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                           List<AiChatMessage> contextMessages,
                           String userQuestion, List<FileInfo> imageAttachments);

    /**
     * 流式聊天。通过 consumer 回调推送 AiStreamEvent，返回完整响应文本。
     *
     * @param config          渠道配置（含模型名、API 地址等）
     * @param systemPrompt    系统提示词
     * @param contextMessages 上下文历史消息列表
     * @param userQuestion    用户当前提问
     * @param imageAttachments 图片附件列表
     * @param eventConsumer   流式事件回调消费者
     * @return 完整的模型响应文本
     */
    String streamChat(AiChannelConfig config, String systemPrompt,
                      List<AiChatMessage> contextMessages,
                      String userQuestion, List<FileInfo> imageAttachments,
                      Consumer<AiStreamEvent> eventConsumer);
}

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
     */
    AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                           List<AiChatMessage> contextMessages, String userQuestion);

    /**
     * 发起一次多模态 AI 对话调用（支持图片附件）。
     */
    AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                           List<AiChatMessage> contextMessages,
                           String userQuestion, List<FileInfo> imageAttachments);

    /**
     * 流式聊天。通过 consumer 回调推送 AiStreamEvent，返回完整响应文本。
     */
    String streamChat(AiChannelConfig config, String systemPrompt,
                      List<AiChatMessage> contextMessages,
                      String userQuestion, List<FileInfo> imageAttachments,
                      Consumer<AiStreamEvent> eventConsumer);
}

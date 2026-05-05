package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.config.LangChain4jConfig;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模型调用客户端实现。
 *
 * <p>职责：根据渠道配置动态构建模型实例，组装消息列表（含上下文裁剪），执行调用并封装结果。
 */
@Slf4j
@Service
public class AiModelClientImpl implements AiModelClient {

    @Override
    public AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                                  List<AiChatMessage> contextMessages, String userQuestion) {
        return chat(config, systemPrompt, contextMessages, userQuestion, null);
    }

    @Override
    public AiModelCallResult chat(AiChannelConfig config, String systemPrompt,
                                  List<AiChatMessage> contextMessages,
                                  String userQuestion, List<FileInfo> imageAttachments) {
        try {
            ChatModel model = LangChain4jConfig.buildModel(config);
            List<ChatMessage> messages = buildMessages(config, systemPrompt, contextMessages,
                    userQuestion, imageAttachments);

            ChatResponse response = model.chat(messages);

            return mapResponse(response);
        } catch (Exception e) {
            log.warn("AI 模型调用失败，渠道: {}, 错误: {}", config.getChannelCode(), e.getMessage());
            AiModelCallResult failure = new AiModelCallResult();
            failure.setSuccess(false);
            failure.setErrorMessage(truncateErrorMessage(e.getMessage()));
            return failure;
        }
    }

    /**
     * 组装最终消息列表，包含系统提示词、裁剪后的历史上下文和当前用户提问。
     *
     * <p>上下文裁剪策略：当渠道配置了 maxContextTokens > 0 时，按字符数 / 2 估算 token 数，
     * 从最老的消息开始裁剪，直到满足限制。系统提示词和当前用户提问始终保留。
     */
    private List<ChatMessage> buildMessages(AiChannelConfig config, String systemPrompt,
                                            List<AiChatMessage> contextMessages,
                                            String userQuestion, List<FileInfo> imageAttachments) {
        List<ChatMessage> history = convertHistory(contextMessages);
        int maxContextTokens = config.getMaxContextTokens() != null ? config.getMaxContextTokens() : 0;

        if (maxContextTokens > 0) {
            history = trimContext(history, maxContextTokens);
        }

        List<ChatMessage> messages = new ArrayList<>(history.size() + 2);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.addAll(history);

        if (imageAttachments != null && !imageAttachments.isEmpty()) {
            messages.add(buildMultimodalUserMessage(userQuestion, imageAttachments));
        } else {
            messages.add(UserMessage.from(userQuestion));
        }
        return messages;
    }

    /**
     * 构造包含文本和图片的多模态用户消息。
     */
    private UserMessage buildMultimodalUserMessage(String text, List<FileInfo> images) {
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>(images.size() + 1);
        contents.add(TextContent.from(text));
        for (FileInfo image : images) {
            String url = image.getFileUrl() != null ? image.getFileUrl() : image.getFilePath();
            contents.add(ImageContent.from(url));
        }
        return UserMessage.from(contents);
    }

    /**
     * 将历史消息实体转换为 LangChain4j ChatMessage 列表，过滤掉失败消息。
     */
    private List<ChatMessage> convertHistory(List<AiChatMessage> contextMessages) {
        List<ChatMessage> result = new ArrayList<>(contextMessages.size());
        for (AiChatMessage msg : contextMessages) {
            if (msg.getResponseStatus() == null || msg.getResponseStatus() != 1) {
                continue;
            }
            String content = msg.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            String roleType = msg.getRoleType();
            if (AiConstants.ROLE_TYPE_SYSTEM.equals(roleType)) {
                result.add(SystemMessage.from(content));
            } else if (AiConstants.ROLE_TYPE_USER.equals(roleType)) {
                result.add(UserMessage.from(content));
            } else if (AiConstants.ROLE_TYPE_ASSISTANT.equals(roleType)) {
                result.add(AiMessage.from(content));
            } else {
                log.debug("跳过未知角色类型的历史消息: roleType={}", roleType);
            }
        }
        return result;
    }

    /**
     * 按估算 token 数裁剪历史消息（从最老的开始移除），确保不超过上限。
     */
    private List<ChatMessage> trimContext(List<ChatMessage> history, int maxContextTokens) {
        int totalTokens = estimateTokens(history);
        if (totalTokens <= maxContextTokens) {
            return history;
        }

        List<ChatMessage> trimmed = new ArrayList<>(history);
        while (!trimmed.isEmpty() && estimateTokens(trimmed) > maxContextTokens) {
            trimmed.remove(0);
        }
        return trimmed;
    }

    private int estimateTokens(List<ChatMessage> messages) {
        int totalChars = 0;
        for (ChatMessage message : messages) {
            totalChars += extractText(message).length();
        }
        return totalChars / 2;
    }

    private String extractText(ChatMessage message) {
        if (message instanceof UserMessage userMsg) {
            return userMsg.hasSingleText() ? userMsg.singleText() : "";
        } else if (message instanceof AiMessage aiMsg) {
            return aiMsg.text() != null ? aiMsg.text() : "";
        } else if (message instanceof SystemMessage sysMsg) {
            return sysMsg.text() != null ? sysMsg.text() : "";
        }
        return "";
    }

    private AiModelCallResult mapResponse(ChatResponse response) {
        AiModelCallResult result = new AiModelCallResult();
        result.setSuccess(true);

        String text = response.aiMessage() != null ? response.aiMessage().text() : null;
        result.setContent(text);

        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            result.setRequestTokens(tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0);
            result.setResponseTokens(tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0);
            result.setTotalTokens(tokenUsage.totalTokenCount() != null ? tokenUsage.totalTokenCount() : 0);
        } else {
            result.setRequestTokens(0);
            result.setResponseTokens(0);
            result.setTotalTokens(0);
        }
        return result;
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() > 500 ? message.substring(0, 500) + "..." : message;
    }
}

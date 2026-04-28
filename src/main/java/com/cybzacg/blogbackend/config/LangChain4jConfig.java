package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.LangChain4jProperties;
import com.cybzacg.blogbackend.utils.StrUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 默认模型配置。
 *
 * <p>用于提供默认 OpenAI-compatible 模型 Bean。业务主链路仍应通过项目自有 AI Provider 抽象访问，避免绑定具体框架。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.langchain4j", name = "enabled", havingValue = "true")
public class LangChain4jConfig {
    private final LangChain4jProperties properties;

    /**
     * 默认同步聊天模型。
     */
    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel defaultChatModel() {
        LangChain4jProperties.Chat chat = properties.getChat();
        return OpenAiChatModel.builder()
                .baseUrl(requireApiBaseUrl())
                .apiKey(requireApiKey())
                .modelName(properties.getModelName())
                .temperature(chat.getTemperature())
                .topP(chat.getTopP())
                .maxTokens(chat.getMaxTokens())
                .timeout(chat.getTimeout())
                .maxRetries(chat.getMaxRetries())
                .logRequests(chat.getLogRequests())
                .logResponses(chat.getLogResponses())
                .build();
    }

    /**
     * 默认流式聊天模型。
     */
    @Bean
    @ConditionalOnMissingBean(StreamingChatModel.class)
    public StreamingChatModel defaultStreamingChatModel() {
        LangChain4jProperties.Chat chat = properties.getChat();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(requireApiBaseUrl())
                .apiKey(requireApiKey())
                .modelName(properties.getModelName())
                .temperature(chat.getTemperature())
                .topP(chat.getTopP())
                .maxTokens(chat.getMaxTokens())
                .timeout(chat.getTimeout())
                .logRequests(chat.getLogRequests())
                .logResponses(chat.getLogResponses())
                .build();
    }

    private String requireApiBaseUrl() {
        String baseUrl = StrUtils.trimToNull(properties.getBaseUrl());
        if (baseUrl == null) {
            throw new IllegalStateException("ai.langchain4j.base-url 不能为空");
        }
        return baseUrl;
    }

    private String requireApiKey() {
        String apiKey = StrUtils.trimToNull(properties.getApiKey());
        if (apiKey == null) {
            throw new IllegalStateException("启用 ai.langchain4j.enabled 时必须配置 ai.langchain4j.api-key");
        }
        return apiKey;
    }
}

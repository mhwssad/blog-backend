package com.cybzacg.blogbackend.config.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * LangChain4j 默认模型配置。
 *
 * <p>当前作为平台 AI 模块的默认 OpenAI-compatible 接入配置；后台 AI 配置中心落地后，业务侧应优先读取数据库渠道配置。
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.langchain4j")
public class LangChain4jProperties {
    /**
     * 是否启用默认模型 Bean。默认关闭，避免未配置 API Key 时启动失败。
     */
    private Boolean enabled = false;

    /**
     * 默认供应商标识：deepseek / minimax / openai 等。
     */
    @NotBlank
    private String provider = "deepseek";

    /**
     * OpenAI-compatible Chat Completions API 基础地址。
     */
    @NotBlank
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * API Key。建议通过环境变量注入，不要写入仓库。
     */
    private String apiKey;

    /**
     * 默认模型名称。
     */
    @NotBlank
    private String modelName = "deepseek-chat";

    /**
     * 默认聊天参数。
     */
    @Valid
    private Chat chat = new Chat();

    @Data
    public static class Chat {
        /**
         * 采样温度。
         */
        private Double temperature = 0.7D;

        /**
         * nucleus sampling 参数。
         */
        private Double topP;

        /**
         * 单次回答最大 token 数。
         */
        @Min(1)
        private Integer maxTokens = 2048;

        /**
         * 请求超时时间。
         */
        private Duration timeout = Duration.ofSeconds(60);

        /**
         * 同步调用最大重试次数。
         */
        @Min(0)
        private Integer maxRetries = 1;

        /**
         * 是否打印请求日志。生产环境不建议开启。
         */
        private Boolean logRequests = false;

        /**
         * 是否打印响应日志。生产环境不建议开启。
         */
        private Boolean logResponses = false;
    }
}

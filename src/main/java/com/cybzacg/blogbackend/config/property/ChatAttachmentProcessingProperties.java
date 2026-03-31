package com.cybzacg.blogbackend.config.property;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 聊天附件异步处理任务配置。
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "chat.attachment-processing")
public class ChatAttachmentProcessingProperties {
    /**
     * 调度扫描批次大小。
     */
    @Min(1)
    private Integer batchSize = 16;

    /**
     * 单任务最大重试次数。
     */
    @Min(1)
    private Integer maxRetryCount = 3;

    /**
     * 基础重试间隔秒数。
     */
    @Min(1)
    private Integer retryDelaySeconds = 30;

    /**
     * 单次处理租约秒数。
     */
    @Min(5)
    private Integer leaseSeconds = 300;

    /**
     * 调度固定延迟毫秒数。
     */
    @Min(1000)
    private Integer dispatchFixedDelayMs = 15000;
}

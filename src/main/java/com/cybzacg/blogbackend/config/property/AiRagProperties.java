package com.cybzacg.blogbackend.config.property;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * AI RAG 检索增强配置。
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.rag")
public class AiRagProperties {
    /** 是否启用 RAG 主链路。 */
    private Boolean enabled = true;
    /** 单个知识分块最大字符数。 */
    @Min(100)
    private Integer chunkSize = 800;
    /** 相邻分块重叠字符数。 */
    @Min(0)
    private Integer chunkOverlap = 120;
    /** 检索返回数量。 */
    @Min(1)
    private Integer topK = 5;
    /** 最低相似度。 */
    private Double minScore = 0.35D;
    /** Redis 缓存 TTL。 */
    private Duration cacheTtl = Duration.ofMinutes(30);
    /** 当前向量存储提供者，v1 固定 mysql-redis，后续可切换向量数据库实现。 */
    private String embeddingProvider = "mysql-redis";
}

package com.cybzacg.blogbackend.module.ai.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * RAG 引用来源信息。
 */
@Data
@Schema(description = "RAG 引用来源信息")
public class AiRagReferenceVO {
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "来源对象ID")
    private Long sourceId;
    @Schema(description = "知识条目ID")
    private Long entryId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "来源URL")
    private String sourceUrl;
    @Schema(description = "分块序号")
    private Integer chunkIndex;
    @Schema(description = "相似度")
    private Double score;
}

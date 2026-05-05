package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识分块表。
 */
@Data
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 知识条目ID */
    private Long entryId;
    /** 来源类型 */
    private String sourceType;
    /** 来源对象ID */
    private Long sourceId;
    /** 标题快照 */
    private String title;
    /** 来源页面URL */
    private String sourceUrl;
    /** 分块序号 */
    private Integer chunkIndex;
    /** 分块文本 */
    private String chunkText;
    /** 分块内容 SHA-256 */
    private String contentHash;
    /** embedding 向量 JSON 数组 */
    private String embeddingJson;
    /** 向量维度 */
    private Integer embeddingDim;
    /** embedding 模型名称 */
    private String embeddingModel;
    /** 状态：0-失效，1-有效 */
    private Integer status;
    /** 知识条目版本号 */
    private Integer entryVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

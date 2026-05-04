package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识条目表。
 */
@Data
@TableName("ai_knowledge_entry")
public class AiKnowledgeEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 来源类型 */
    private String sourceType;
    /** 来源对象ID */
    private Long sourceId;
    /** 标题 */
    private String title;
    /** 摘要 */
    private String summary;
    /** 内容快照 */
    private String contentSnapshot;
    /** 来源页面URL（预留） */
    private String sourceUrl;
    /** 原始作者ID */
    private Long authorId;
    /** 状态：0-禁用，1-正常，2-过期，3-已删除 */
    private Integer status;
    /** 版本号 */
    private Integer version;
    /** 分块数量 */
    private Integer chunkCount;
    /** 源内容最后更新时间 */
    private LocalDateTime sourceUpdatedAt;
    /** 最近一次同步时间 */
    private LocalDateTime syncedAt;
    /** 标签JSON数组（预留） */
    private String tagJson;
    /** 扩展字段JSON（预留） */
    private String extraJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

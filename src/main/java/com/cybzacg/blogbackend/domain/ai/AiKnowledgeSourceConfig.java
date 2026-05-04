package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识源配置表。
 */
@Data
@TableName("ai_knowledge_source_config")
public class AiKnowledgeSourceConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 知识源类型编码 */
    private String sourceType;
    /** 是否启用：0-禁用，1-启用 */
    private Integer enabled;
    /** 同步间隔（秒） */
    private Integer syncInterval;
    /** 最近一次同步完成时间 */
    private LocalDateTime lastSyncedAt;
    /** 最近同步状态：success/failed */
    private String lastSyncStatus;
    /** 扩展配置JSON（预留） */
    private String configJson;
    /** 更新人ID */
    private Long updatedBy;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

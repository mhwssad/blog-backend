package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识源配置响应 VO。
 */
@Data
@Schema(description = "知识源配置信息")
public class AiKnowledgeSourceConfigVO {
    @Schema(description = "配置ID")
    private Long id;
    @Schema(description = "知识源类型编码")
    private String sourceType;
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer enabled;
    @Schema(description = "同步间隔（秒）")
    private Integer syncInterval;
    @Schema(description = "最近一次同步完成时间")
    private LocalDateTime lastSyncedAt;
    @Schema(description = "最近同步状态")
    private String lastSyncStatus;
    @Schema(description = "扩展配置JSON")
    private String configJson;
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识条目响应 VO。
 */
@Data
@Schema(description = "知识条目信息")
public class AiKnowledgeEntryVO {
    @Schema(description = "条目ID")
    private Long id;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "来源对象ID")
    private Long sourceId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "来源页面URL")
    private String sourceUrl;
    @Schema(description = "原始作者ID")
    private Long authorId;
    @Schema(description = "状态：0-禁用，1-正常，2-过期，3-已删除")
    private Integer status;
    @Schema(description = "版本号")
    private Integer version;
    @Schema(description = "分块数量")
    private Integer chunkCount;
    @Schema(description = "源内容最后更新时间")
    private LocalDateTime sourceUpdatedAt;
    @Schema(description = "最近同步时间")
    private LocalDateTime syncedAt;
    @Schema(description = "标签JSON")
    private String tagJson;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

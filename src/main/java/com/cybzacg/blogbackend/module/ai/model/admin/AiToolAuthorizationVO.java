package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具授权 VO。
 */
@Data
@Schema(description = "AI 工具授权信息")
public class AiToolAuthorizationVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "工具 ID")
    private Long toolId;
    @Schema(description = "授权类型")
    private String authorizationType;
    @Schema(description = "授权键")
    private String authorizationKey;
    @Schema(description = "数据范围")
    private String dataScope;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "创建人ID")
    private Long createdBy;
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

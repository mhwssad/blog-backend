package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 工具授权分页查询。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "AI 工具授权分页查询")
public class AiToolAuthorizationPageQuery extends PageQuery {
    @Schema(description = "工具 ID")
    private Long toolId;

    @Schema(description = "授权类型")
    private String authorizationType;

    @Schema(description = "授权键")
    private String authorizationKey;

    @Schema(description = "启用状态：0-否/1-是")
    private Integer enabled;
}

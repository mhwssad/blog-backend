package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识条目分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识条目分页查询")
public class AiKnowledgeEntryPageQuery extends PageQuery {
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "状态：0-禁用，1-正常，2-过期，3-已删除")
    private Integer status;
    @Schema(description = "标题关键词")
    private String keyword;
}

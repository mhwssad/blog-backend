package com.cybzacg.blogbackend.module.content.comment.model.publics;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "前台评论查询条件")
public class PublicCommentQuery extends PageQuery {
    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;
}

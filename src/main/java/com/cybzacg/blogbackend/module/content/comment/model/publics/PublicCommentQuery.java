package com.cybzacg.blogbackend.module.content.comment.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "前台评论查询条件")
public class PublicCommentQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;
}

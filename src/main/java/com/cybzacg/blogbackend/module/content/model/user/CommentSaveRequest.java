package com.cybzacg.blogbackend.module.content.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户评论请求")
public class CommentSaveRequest {
    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论图片列表")
    private List<String> images;
    @Schema(description = "根评论ID")
    private Long rootId = 0L;
    @Schema(description = "父评论ID")
    private Long parentId = 0L;
}

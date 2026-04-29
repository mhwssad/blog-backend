package com.cybzacg.blogbackend.module.content.comment.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评论状态更新请求")
public class CommentStatusRequest {
    @NotNull(message = "状态不能为空")
    @Schema(description = "评论状态")
    private Integer status;
}

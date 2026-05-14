package com.cybzacg.blogbackend.module.content.comment.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评论状态更新请求")
public class CommentStatusRequest {
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "评论状态必须在 0-2 之间")
    @Max(value = 2, message = "评论状态必须在 0-2 之间")
    @Schema(description = "评论状态")
    private Integer status;
}

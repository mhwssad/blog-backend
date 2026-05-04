package com.cybzacg.blogbackend.module.forum.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "论坛回复保存请求")
public class ForumReplySaveRequest {
    @Schema(description = "父回复ID，顶级回复为空")
    private Long parentId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 5000, message = "回复内容不能超过5000个字符")
    @Schema(description = "回复内容")
    private String content;
}

package com.cybzacg.blogbackend.module.content.taxonomy.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "标签新增/修改请求")
public class TagSaveRequest {
    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签颜色")
    private String color;
}

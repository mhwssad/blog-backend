package com.cybzacg.blogbackend.module.content.taxonomy.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "标签新增/修改请求")
public class TagSaveRequest {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称最长64字符")
    @Schema(description = "标签名称")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色格式不合法")
    @Schema(description = "标签颜色")
    private String color = "#409EFF";
}

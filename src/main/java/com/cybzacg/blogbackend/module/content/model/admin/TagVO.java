package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台标签信息")
public class TagVO {
    @Schema(description = "标签ID")
    private Long id;
    @Schema(description = "标签名称")
    private String name;
    @Schema(description = "标签颜色")
    private String color;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

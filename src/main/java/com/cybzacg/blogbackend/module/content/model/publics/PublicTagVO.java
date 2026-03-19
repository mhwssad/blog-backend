package com.cybzacg.blogbackend.module.content.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "前台标签信息")
public class PublicTagVO {
    @Schema(description = "标签ID")
    private Long id;
    @Schema(description = "标签名称")
    private String name;
    @Schema(description = "标签颜色")
    private String color;
}

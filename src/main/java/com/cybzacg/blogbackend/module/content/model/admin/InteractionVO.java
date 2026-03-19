package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "后台互动信息")
public class InteractionVO {
    @Schema(description = "互动ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "互动类型")
    private String actionType;
    @Schema(description = "创建时间")
    private Date createdAt;
}

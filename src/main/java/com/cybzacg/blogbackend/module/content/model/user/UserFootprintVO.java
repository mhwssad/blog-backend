package com.cybzacg.blogbackend.module.content.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户足迹信息")
public class UserFootprintVO {
    @Schema(description = "足迹ID")
    private Long id;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "目标标题")
    private String targetTitle;
    @Schema(description = "目标地址")
    private String targetUrl;
    @Schema(description = "访问时间")
    private LocalDateTime visitedAt;
}

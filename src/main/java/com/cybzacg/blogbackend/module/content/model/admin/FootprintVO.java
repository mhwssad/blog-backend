package com.cybzacg.blogbackend.module.content.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "后台足迹信息")
public class FootprintVO {
    @Schema(description = "足迹ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "目标标题")
    private String targetTitle;
    @Schema(description = "目标地址")
    private String targetUrl;
    @Schema(description = "IP地址")
    private String ipAddress;
    @Schema(description = "用户代理")
    private String userAgent;
    @Schema(description = "访问时间")
    private LocalDateTime visitedAt;
}

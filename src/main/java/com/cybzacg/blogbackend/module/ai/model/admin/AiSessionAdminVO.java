package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台 AI 会话管理 VO。
 */
@Data
@Schema(description = "后台AI会话信息")
public class AiSessionAdminVO {
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "场景类型")
    private String sceneType;

    @Schema(description = "状态：0-关闭，1-正常")
    private Integer status;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}

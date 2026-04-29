package com.cybzacg.blogbackend.module.content.interaction.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "互动切换命令")
public class InteractionToggleRequest {
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "互动类型")
    private String actionType;
}

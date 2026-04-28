package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI消息信息。
 */
@Data
@Schema(description = "AI消息信息")
public class AiMessageVO {
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "角色类型：user/assistant/system")
    private String roleType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息token数")
    private Integer tokenCount;

    @Schema(description = "响应状态：0-失败，1-成功")
    private Integer responseStatus;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}

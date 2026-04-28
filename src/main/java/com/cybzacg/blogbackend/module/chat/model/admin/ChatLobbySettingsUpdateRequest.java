package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 大厅频道设置更新请求。
 */
@Data
@Schema(description = "大厅频道设置更新请求")
public class ChatLobbySettingsUpdateRequest {
    @Schema(description = "大厅公告")
    private String announcement;

    @Schema(description = "慢速模式秒数（0=关闭）")
    @Min(0)
    @Max(3600)
    private Integer slowModeSeconds;

    @Schema(description = "发言最低等级限制")
    @Min(1)
    @Max(100)
    private Integer speakLevelLimit;
}

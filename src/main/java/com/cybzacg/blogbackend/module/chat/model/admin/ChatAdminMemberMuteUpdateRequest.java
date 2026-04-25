package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台更新成员禁言请求。
 */
@Data
@Schema(description = "后台更新成员禁言请求")
public class ChatAdminMemberMuteUpdateRequest {
    @Schema(description = "禁言截止时间；为空表示取消禁言")
    private LocalDateTime muteUntil;
}

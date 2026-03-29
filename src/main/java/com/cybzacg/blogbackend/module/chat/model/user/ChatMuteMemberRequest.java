package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 群成员禁言请求。
 */
@Data
@Schema(description = "群成员禁言请求")
public class ChatMuteMemberRequest {
    @Schema(description = "禁言截止时间；为空表示取消禁言")
    private Date muteUntil;
}

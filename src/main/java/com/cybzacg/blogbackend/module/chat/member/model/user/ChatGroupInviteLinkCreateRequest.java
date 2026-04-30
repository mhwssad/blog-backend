package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建群邀请链接请求。
 */
@Data
@Schema(description = "创建群邀请链接请求")
public class ChatGroupInviteLinkCreateRequest {
    @Schema(description = "过期时间，空表示不过期")
    private LocalDateTime expireAt;

    @Min(value = 0, message = "最大使用次数不能小于0")
    @Schema(description = "最大使用次数，0表示不限制")
    private Integer maxUseCount;
}

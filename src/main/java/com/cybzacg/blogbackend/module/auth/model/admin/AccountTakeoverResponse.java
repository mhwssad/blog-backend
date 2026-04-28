package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "账号接管响应")
public class AccountTakeoverResponse {
    @Schema(description = "接管令牌")
    private String takeoverToken;
    @Schema(description = "目标用户ID")
    private Long targetUserId;
    @Schema(description = "目标用户名")
    private String targetUsername;
    @Schema(description = "过期时间（秒）")
    private Long expiresIn;
}

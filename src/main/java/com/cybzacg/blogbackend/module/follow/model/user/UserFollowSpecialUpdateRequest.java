package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 特别关注状态更新请求。
 */
@Data
@Schema(description = "特别关注状态更新请求")
public class UserFollowSpecialUpdateRequest {
    @NotNull(message = "特别关注状态不能为空")
    @Schema(description = "是否特别关注：0-否，1-是")
    private Integer specialFollow;
}

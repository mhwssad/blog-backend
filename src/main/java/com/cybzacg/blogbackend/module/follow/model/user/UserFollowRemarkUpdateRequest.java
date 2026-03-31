package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 关注备注更新请求。
 */
@Data
@Schema(description = "关注备注更新请求")
public class UserFollowRemarkUpdateRequest {
    @Size(max = 256, message = "备注长度不能超过256个字符")
    @Schema(description = "关注备注")
    private String remark;
}

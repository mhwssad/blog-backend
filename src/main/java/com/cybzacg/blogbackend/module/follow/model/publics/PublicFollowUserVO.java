package com.cybzacg.blogbackend.module.follow.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 公开关注用户列表视图。
 */
@Data
@Schema(description = "公开关注用户列表项")
public class PublicFollowUserVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "最近关注时间")
    private Date followTime;
}

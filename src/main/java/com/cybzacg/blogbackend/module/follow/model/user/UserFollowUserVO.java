package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 关注用户列表视图。
 */
@Data
@Schema(description = "关注用户列表项")
public class UserFollowUserVO {
    @Schema(description = "关系ID")
    private Long relationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "是否特别关注：0-否，1-是")
    private Integer isSpecialFollow;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否互相关注：0-否，1-是")
    private Integer mutualFollow;

    @Schema(description = "最近关注时间")
    private Date followTime;
}

package com.cybzacg.blogbackend.module.auth.account.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开用户搜索结果视图。
 */
@Data
@Schema(description = "用户搜索结果")
public class PublicUserSearchVO {
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像URL")
    private String avatar;
    @Schema(description = "个人简介")
    private String bio;
}

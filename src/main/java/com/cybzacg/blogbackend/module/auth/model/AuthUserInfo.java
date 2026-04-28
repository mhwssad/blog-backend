package com.cybzacg.blogbackend.module.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "当前登录用户信息")
public class AuthUserInfo {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "用户等级")
    private Integer userLevel;

    @Schema(description = "经验值")
    private Integer experiencePoints;

    @Schema(description = "角色编码")
    private List<String> roles;

    @Schema(description = "权限标识")
    private List<String> permissions;
}

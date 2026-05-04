package com.cybzacg.blogbackend.module.auth.account.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户个人资料视图。
 */
@Data
@Schema(description = "用户个人资料")
public class UserProfileVO {
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
    @Schema(description = "个人站点")
    private String website;
    @Schema(description = "性别：0-未知，1-男，2-女，3-保密")
    private Integer gender;
    @Schema(description = "生日")
    private LocalDate birthday;
    @Schema(description = "邮箱（脱敏）")
    private String email;
    @Schema(description = "手机号（脱敏）")
    private String phone;
    @Schema(description = "用户等级")
    private Integer userLevel;
    @Schema(description = "经验值")
    private Integer experiencePoints;
    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}

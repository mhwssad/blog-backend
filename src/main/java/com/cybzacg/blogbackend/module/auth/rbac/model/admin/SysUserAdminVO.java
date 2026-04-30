package com.cybzacg.blogbackend.module.auth.rbac.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "后台用户信息")
public class SysUserAdminVO {
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "性别")
    private Integer gender;
    @Schema(description = "生日")
    private LocalDate birthday;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "用户等级")
    private Integer userLevel;
    @Schema(description = "经验值")
    private Integer experiencePoints;
    @Schema(description = "最近一次等级变更时间")
    private LocalDateTime levelUpdatedAt;
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;
    @Schema(description = "最后登录IP")
    private String lastLoginIp;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "MFA是否启用")
    private Integer mfaEnabled;
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}

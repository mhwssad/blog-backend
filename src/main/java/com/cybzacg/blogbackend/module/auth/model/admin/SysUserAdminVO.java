package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
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
    private Date birthday;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;
    @Schema(description = "最后登录IP")
    private String lastLoginIp;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}

package com.cybzacg.blogbackend.domain.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息表
 *
 * @TableName sys_user
 */
@TableName(value = "sys_user")
@Data
public class SysUser {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 个人站点
     */
    private String website;

    /**
     * 性别：0-未知，1-男，2-女, 3-保密
     */
    private Integer gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 用户等级：1-10
     */
    private Integer userLevel;

    /**
     * 经验值
     */
    private Integer experiencePoints;

    /**
     * 最近一次等级变更时间
     */
    private LocalDateTime levelUpdatedAt;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除标记：0-未删除，1-已删除
     */
    private Integer deletedFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * MFA是否启用：0-否，1-是
     */
    private Integer mfaEnabled;

}

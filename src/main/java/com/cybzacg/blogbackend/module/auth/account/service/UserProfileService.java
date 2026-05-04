package com.cybzacg.blogbackend.module.auth.account.service;

import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileVO;

/**
 * 用户自服务接口。
 */
public interface UserProfileService {

    /**
     * 查询当前用户个人资料。
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新当前用户公开资料。
     */
    UserProfileVO updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 修改密码（需验证旧密码）。
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}

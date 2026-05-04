package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.convert.UserProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileVO;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.UserProfileService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户自服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final SysUserRepository sysUserRepository;
    private final UserProfileModelConvert userProfileModelConvert;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getProfile(Long userId) {
        SysUser user = ExceptionThrowerCore.requireNonNull(
                sysUserRepository.getById(userId), ResultErrorCode.USER_NOT_FOUND);
        UserProfileVO vo = userProfileModelConvert.toUserProfileVO(user);
        vo.setEmail(desensitizeEmail(user.getEmail()));
        vo.setPhone(desensitizePhone(user.getPhone()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        SysUser user = ExceptionThrowerCore.requireNonNull(
                sysUserRepository.getById(userId), ResultErrorCode.USER_NOT_FOUND);

        userProfileModelConvert.updateProfile(request, user);
        sysUserRepository.updateById(user);

        log.info("用户更新资料: userId={}", userId);
        return getProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = ExceptionThrowerCore.requireNonNull(
                sysUserRepository.getById(userId), ResultErrorCode.USER_NOT_FOUND);

        ExceptionThrowerCore.throwBusinessIf(
                !passwordEncoder.matches(oldPassword, user.getPassword()),
                ResultErrorCode.OLD_PASSWORD_MISMATCH);

        PasswordUtils.validate(newPassword);

        ExceptionThrowerCore.throwBusinessIf(
                passwordEncoder.matches(newPassword, user.getPassword()),
                ResultErrorCode.PASSWORD_SAME_AS_OLD);

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserRepository.updateById(user);

        log.info("用户修改密码: userId={}", userId);
    }

    private String desensitizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        if (phone.length() >= 7) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
        return phone;
    }
}

package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超级管理员身份校验器。
 *
 * <p>负责判断当前用户是否为超级管理员，并提供校验断言方法。
 */
@Component
@RequiredArgsConstructor
public class SuperAdminVerifier {
    private static final String ADMIN_ROLE_CODE = "admin";

    private final SysRoleRepository sysRoleRepository;

    /**
     * 判断指定用户是否为超级管理员。
     */
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleCodes = sysRoleRepository.findRoleCodesByUserId(userId);
        return roleCodes != null && roleCodes.contains(ADMIN_ROLE_CODE);
    }

    /**
     * 校验当前登录用户是否为超级管理员，不是则抛出异常。
     */
    public void requireSuperAdmin() {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIfNot(isSuperAdmin(userId), ResultErrorCode.NOT_SUPER_ADMIN);
    }

    /**
     * 校验指定用户是否为超级管理员，不是则抛出异常。
     */
    public void requireSuperAdmin(Long userId) {
        ExceptionThrowerCore.throwBusinessIfNot(isSuperAdmin(userId), ResultErrorCode.NOT_SUPER_ADMIN);
    }
}

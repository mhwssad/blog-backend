package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.service.AuthorPermissionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 作者权限服务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthorPermissionServiceImpl implements AuthorPermissionService {
    private static final String AUTHOR_ROLE_CODE = "author";

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final RbacAssociationFactory rbacAssociationFactory;

    /**
     * 判断用户是否已具备作者角色。
     */
    @Override
    public boolean hasAuthorRole(Long userId) {
        if (userId == null) {
            return false;
        }
        return sysRoleRepository.findRoleCodesByUserId(userId).contains(AUTHOR_ROLE_CODE);
    }

    /**
     * 授予作者角色；如果用户已有该角色则不重复写入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantAuthorRole(Long userId) {
        requireExistingUser(userId);
        SysRole authorRole = sysRoleRepository.findByCode(AUTHOR_ROLE_CODE);
        ExceptionThrowerCore.throwBusinessIf(
                authorRole == null || !Objects.equals(authorRole.getStatus(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "作者角色未初始化"
        );
        List<Long> roleIds = sysUserRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.contains(authorRole.getId())) {
            return;
        }
        sysUserRoleRepository.save(rbacAssociationFactory.createUserRole(userId, authorRole.getId()));
    }

    /**
     * 撤销作者角色；如果用户未持有该角色则不重复删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAuthorRole(Long userId) {
        requireExistingUser(userId);
        SysRole authorRole = sysRoleRepository.findByCode(AUTHOR_ROLE_CODE);
        ExceptionThrowerCore.throwBusinessIf(
                authorRole == null || !Objects.equals(authorRole.getStatus(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "作者角色未初始化"
        );
        sysUserRoleRepository.deleteByUserIdAndRoleId(userId, authorRole.getId());
    }

    private void requireExistingUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND
        );
    }
}

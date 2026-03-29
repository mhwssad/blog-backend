package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
/**
 * 用户后台管理服务实现。
 *
 * <p>负责后台用户分页查询、资料维护、状态更新、密码重置以及角色分配。
 */
@Service
@RequiredArgsConstructor
public class SysUserAdminServiceImpl implements SysUserAdminService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;
    private final RbacAdminModelMapper rbacAdminModelMapper;

    @Override
    public PageResult<SysUserAdminVO> pageUsers(SysUserPageQuery query) {
        Page<SysUser> page = sysUserService.lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), SysUser::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getEmail()), SysUser::getEmail, query.getEmail())
                .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(SysUser::getDeletedFlag, 0)
                .orderByDesc(SysUser::getCreatedAt)
                .page(new Page<>(query.getCurrent(), query.getSize()));

        List<SysUserAdminVO> records = page.getRecords().stream()
                .map(user -> rbacAdminModelMapper.toUserVO(user, sysUserRoleService.listRoleIdsByUserId(user.getId())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysUserAdminVO getUser(Long id) {
        SysUser user = getAvailableUser(id);
        return rbacAdminModelMapper.toUserVO(user, sysUserRoleService.listRoleIdsByUserId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserAdminVO createUser(SysUserSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIfBlank(request.getPassword(), ResultErrorCode.ILLEGAL_ARGUMENT, "新增用户时密码不能为空");
        validateUserUniqueness(null, request);

        SysUser user = rbacAdminModelMapper.toUser(request);
        applyUserFields(user, request, true);
        sysUserService.save(user);
        return rbacAdminModelMapper.toUserVO(user, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserAdminVO updateUser(Long id, SysUserSaveRequest request) {
        SysUser user = getAvailableUser(id);
        validateUserUniqueness(id, request);
        applyUserFields(user, request, false);
        sysUserService.updateById(user);
        return rbacAdminModelMapper.toUserVO(user, sysUserRoleService.listRoleIdsByUserId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysUser user = getAvailableUser(id);
        user.setStatus(status);
        sysUserService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        ExceptionThrowerCore.throwBusinessIfBlank(password, ResultErrorCode.ILLEGAL_ARGUMENT, "新密码不能为空");
        SysUser user = getAvailableUser(id);
        user.setPassword(passwordEncoder.encode(password));
        sysUserService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = getAvailableUser(id);
        user.setDeletedFlag(1);
        sysUserService.updateById(user);
        sysUserRoleService.removeByUserId(id);
    }

    @Override
    public List<Long> listRoleIds(Long userId) {
        getAvailableUser(userId);
        return sysUserRoleService.listRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        getAvailableUser(userId);
        validateRolesExist(roleIds);
        sysUserRoleService.replaceUserRoles(userId, roleIds);
    }

    /**
     * 将请求中的可编辑字段统一回填到用户实体，复用新增与更新流程。
     */
    private void applyUserFields(SysUser user, SysUserSaveRequest request, boolean includePassword) {
        rbacAdminModelMapper.updateUser(request, user);
        if (includePassword) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    /**
     * 校验用户名、邮箱和手机号在未删除用户中是否唯一。
     */
    private void validateUserUniqueness(Long currentUserId, SysUserSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(sysUserService.lambdaQuery().eq(SysUser::getDeletedFlag, 0).eq(SysUser::getUsername, StrUtils.normalize(request.getUsername())).ne(currentUserId != null, SysUser::getId, currentUserId).exists(), ResultErrorCode.ILLEGAL_ARGUMENT, "用户名已存在");
        ExceptionThrowerCore.throwBusinessIf(StringUtils.hasText(request.getEmail()) && sysUserService.lambdaQuery().eq(SysUser::getDeletedFlag, 0).eq(SysUser::getEmail, StrUtils.normalize(request.getEmail())).ne(currentUserId != null, SysUser::getId, currentUserId).exists(), ResultErrorCode.ILLEGAL_ARGUMENT, "邮箱已存在");
        ExceptionThrowerCore.throwBusinessIf(StringUtils.hasText(request.getPhone()) && sysUserService.lambdaQuery().eq(SysUser::getDeletedFlag, 0).eq(SysUser::getPhone, StrUtils.normalize(request.getPhone())).ne(currentUserId != null, SysUser::getId, currentUserId).exists(), ResultErrorCode.ILLEGAL_ARGUMENT, "手机号已存在");
    }

    /**
     * 校验角色列表是否合法，避免绑定不存在或重复处理后的无效角色。
     */
    private void validateRolesExist(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctRoleIds = IdCollectionUtils.distinctNonNullIds(
                roleIds,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "角色ID不能为空");
        long count = sysRoleService.lambdaQuery()
                .in(SysRole::getId, distinctRoleIds)
                .eq(SysRole::getIsDeleted, 0)
                .count();
        ExceptionThrowerCore.throwBusinessIf(count != distinctRoleIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "存在无效角色");
    }

    /**
     * 获取未被逻辑删除的用户，不存在时抛出统一异常。
     */
    private SysUser getAvailableUser(Long id) {
        SysUser user = sysUserService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(user == null || Integer.valueOf(1).equals(user.getDeletedFlag()), ResultErrorCode.USER_NOT_FOUND);
        return user;
    }
}











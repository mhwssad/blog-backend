package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

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
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "新增用户时密码不能为空");
        }
        validateUserUniqueness(null, request);

        SysUser user = new SysUser();
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
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "新密码不能为空");
        }
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
        user.setUsername(StrUtils.normalize(request.getUsername()));
        user.setNickname(request.getNickname());
        user.setEmail(StrUtils.normalize(request.getEmail()));
        user.setPhone(StrUtils.normalize(request.getPhone()));
        user.setAvatar(request.getAvatar());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        user.setRemark(request.getRemark());
        if (includePassword) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    /**
     * 校验用户名、邮箱和手机号在未删除用户中是否唯一。
     */
    private void validateUserUniqueness(Long currentUserId, SysUserSaveRequest request) {
        if (sysUserService.lambdaQuery()
                .eq(SysUser::getDeletedFlag, 0)
                .eq(SysUser::getUsername, StrUtils.normalize(request.getUsername()))
                .ne(currentUserId != null, SysUser::getId, currentUserId)
                .exists()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "用户名已存在");
        }
        if (StringUtils.hasText(request.getEmail()) && sysUserService.lambdaQuery()
                .eq(SysUser::getDeletedFlag, 0)
                .eq(SysUser::getEmail, StrUtils.normalize(request.getEmail()))
                .ne(currentUserId != null, SysUser::getId, currentUserId)
                .exists()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "邮箱已存在");
        }
        if (StringUtils.hasText(request.getPhone()) && sysUserService.lambdaQuery()
                .eq(SysUser::getDeletedFlag, 0)
                .eq(SysUser::getPhone, StrUtils.normalize(request.getPhone()))
                .ne(currentUserId != null, SysUser::getId, currentUserId)
                .exists()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "手机号已存在");
        }
    }

    /**
     * 校验角色列表是否合法，避免绑定不存在或重复处理后的无效角色。
     */
    private void validateRolesExist(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        if (roleIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "角色ID不能为空");
        }
        List<Long> distinctRoleIds = roleIds.stream()
                .distinct()
                .toList();
        long count = sysRoleService.lambdaQuery()
                .in(SysRole::getId, distinctRoleIds)
                .eq(SysRole::getIsDeleted, 0)
                .count();
        if (count != distinctRoleIds.size()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "存在无效角色");
        }
    }

    /**
     * 获取未被逻辑删除的用户，不存在时抛出统一异常。
     */
    private SysUser getAvailableUser(Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeletedFlag())) {
            throw new BusinessException(ResultErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}

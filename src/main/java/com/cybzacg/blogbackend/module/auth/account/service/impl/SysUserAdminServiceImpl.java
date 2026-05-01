package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.SysUserAdminService;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.RbacAssociationFactory;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.PasswordUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 用户后台管理服务实现。
 *
 * <p>负责后台用户分页查询、资料维护、状态更新、密码重置以及角色分配。
 */
@Service
@RequiredArgsConstructor
public class SysUserAdminServiceImpl implements SysUserAdminService {
    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RbacAdminModelMapper rbacAdminModelMapper;
    private final RbacAssociationFactory rbacAssociationFactory;
    private final UserNotificationPreferenceService userNotificationPreferenceService;
    private final SuperAdminVerifier superAdminVerifier;
    private final TwoFactorService twoFactorService;
    private final SysAuditLogService sysAuditLogService;
    private final TokenManager tokenManager;

    /**
     * 分页查询用户列表，附带每个用户的角色 ID。
     */
    @Override
    public PageResult<SysUserAdminVO> pageUsers(SysUserPageQuery query) {
        Page<SysUser> page = sysUserRepository.pageByAdminConditions(query);
        List<SysUserAdminVO> records = page.getRecords().stream()
                .map(user -> rbacAdminModelMapper.toUserVO(user, sysUserRoleRepository.findRoleIdsByUserId(user.getId())))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 根据 ID 获取用户详情及其关联的角色 ID。
     */
    @Override
    public SysUserAdminVO getUser(Long id) {
        SysUser user = getAvailableUser(id);
        return rbacAdminModelMapper.toUserVO(user, sysUserRoleRepository.findRoleIdsByUserId(id));
    }

    /**
     * 创建用户，校验用户名、邮箱、手机号唯一性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserAdminVO createUser(SysUserSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIfBlank(request.getPassword(), ResultErrorCode.ILLEGAL_ARGUMENT, "新增用户时密码不能为空");
        validateUserUniqueness(null, request);

        SysUser user = rbacAdminModelMapper.toUser(request);
        applyUserFields(user, request, true);
        sysUserRepository.save(user);
        userNotificationPreferenceService.initializeDefaultSettings(user.getId());
        return rbacAdminModelMapper.toUserVO(user, List.of());
    }

    /**
     * 更新用户资料，校验唯一性约束。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserAdminVO updateUser(Long id, SysUserSaveRequest request) {
        SysUser user = getAvailableUser(id);
        validateUserUniqueness(id, request);
        applyUserFields(user, request, false);
        sysUserRepository.updateById(user);
        return rbacAdminModelMapper.toUserVO(user, sysUserRoleRepository.findRoleIdsByUserId(id));
    }

    /**
     * 更新用户状态（启用/禁用）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysUser user = getAvailableUser(id);
        user.setStatus(status);
        sysUserRepository.updateById(user);
        if (status == 0) {
            tokenManager.invalidateUserSessions(id);
        }
    }

    /**
     * 重置用户密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        ExceptionThrowerCore.throwBusinessIfBlank(password, ResultErrorCode.ILLEGAL_ARGUMENT, "新密码不能为空");
        PasswordUtils.validate(password);
        SysUser user = getAvailableUser(id);
        user.setPassword(passwordEncoder.encode(password));
        sysUserRepository.updateById(user);
    }

    /**
     * 软删除用户并清除用户-角色关联。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = getAvailableUser(id);
        user.setDeletedFlag(1);
        sysUserRepository.updateById(user);
        sysUserRoleRepository.deleteByUserId(id);
        tokenManager.invalidateUserSessions(id);
    }

    /**
     * 查询用户已分配的角色 ID 列表。
     */
    @Override
    public List<Long> listRoleIds(Long userId) {
        getAvailableUser(userId);
        return sysUserRoleRepository.findRoleIdsByUserId(userId);
    }

    /**
     * 为用户分配角色，先清除旧关联再批量写入。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表（为空时仅清除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        getAvailableUser(userId);
        List<Long> distinctRoleIds = validateRolesExist(roleIds);
        sysUserRoleRepository.deleteByUserId(userId);
        if (distinctRoleIds.isEmpty()) {
            tokenManager.invalidateUserSessions(userId);
            return;
        }
        sysUserRoleRepository.saveBatch(distinctRoleIds.stream()
                .map(roleId -> rbacAssociationFactory.createUserRole(userId, roleId))
                .toList());
        tokenManager.invalidateUserSessions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long operatorId, Long targetId, String mfaTicket, String ip, String ua) {
        validateHighRiskOperation(operatorId, targetId, mfaTicket);
        SysUser user = getAvailableUser(targetId);
        String beforeState = JsonUtils.toJson(user);
        user.setStatus(0);
        sysUserRepository.updateById(user);
        tokenManager.invalidateUserSessions(targetId);
        recordAudit(operatorId, targetId, SysAuditOperationType.BAN_USER.getCode(), beforeState,
                JsonUtils.toJson(user), ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUserByReport(Long operatorId, Long targetId, String reason, String ip, String ua) {
        superAdminVerifier.requireSuperAdmin(operatorId);
        ExceptionThrowerCore.throwBusinessIf(operatorId.equals(targetId), ResultErrorCode.CANNOT_MODIFY_SELF);
        SysUser user = getAvailableUser(targetId);
        String beforeState = JsonUtils.toJson(user);
        user.setStatus(0);
        sysUserRepository.updateById(user);
        tokenManager.invalidateUserSessions(targetId);
        SysAuditLogCreateRequest request = new SysAuditLogCreateRequest();
        request.setOperatorUserId(operatorId);
        request.setTargetUserId(targetId);
        request.setOperationType(SysAuditOperationType.BAN_USER.getCode());
        request.setBeforeState(beforeState);
        request.setAfterState(JsonUtils.toJson(user));
        request.setMfaPassed(0);
        request.setRequestIp(ip);
        request.setUserAgent(ua);
        request.setRemark(reason);
        sysAuditLogService.record(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long operatorId, Long targetId, String mfaTicket, String ip, String ua) {
        validateHighRiskOperation(operatorId, targetId, mfaTicket);
        SysUser user = getAvailableUser(targetId);
        String beforeState = JsonUtils.toJson(user);
        user.setStatus(1);
        sysUserRepository.updateById(user);
        recordAudit(operatorId, targetId, SysAuditOperationType.UNBAN_USER.getCode(), beforeState,
                JsonUtils.toJson(user), ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustLevel(Long operatorId, Long targetId, Integer newLevel, String mfaTicket, String ip, String ua) {
        validateHighRiskOperation(operatorId, targetId, mfaTicket);
        SysUser user = getAvailableUser(targetId);
        String beforeState = JsonUtils.toJson(user);
        user.setUserLevel(newLevel);
        sysUserRepository.updateById(user);
        recordAudit(operatorId, targetId, SysAuditOperationType.ADJUST_LEVEL.getCode(), beforeState,
                JsonUtils.toJson(user), ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustExperience(Long operatorId, Long targetId, Integer newExperience, String mfaTicket, String ip, String ua) {
        validateHighRiskOperation(operatorId, targetId, mfaTicket);
        SysUser user = getAvailableUser(targetId);
        String beforeState = JsonUtils.toJson(user);
        user.setExperiencePoints(newExperience);
        sysUserRepository.updateById(user);
        recordAudit(operatorId, targetId, SysAuditOperationType.ADJUST_EXPERIENCE.getCode(), beforeState,
                JsonUtils.toJson(user), ip, ua);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesWithAudit(Long operatorId, Long targetId, List<Long> roleIds, String mfaTicket, String ip, String ua) {
        validateHighRiskOperation(operatorId, targetId, mfaTicket);
        SysUser user = getAvailableUser(targetId);
        List<Long> oldRoleIds = sysUserRoleRepository.findRoleIdsByUserId(targetId);
        String beforeState = JsonUtils.toJson(Map.of("roleIds", oldRoleIds));
        List<Long> distinctRoleIds = validateRolesExist(roleIds);
        sysUserRoleRepository.deleteByUserId(targetId);
        if (!distinctRoleIds.isEmpty()) {
            sysUserRoleRepository.saveBatch(distinctRoleIds.stream()
                    .map(roleId -> rbacAssociationFactory.createUserRole(targetId, roleId))
                    .toList());
        }
        String afterState = JsonUtils.toJson(Map.of("roleIds", distinctRoleIds));
        recordAudit(operatorId, targetId, SysAuditOperationType.ASSIGN_ADMIN_ROLE.getCode(), beforeState,
                afterState, ip, ua);
    }

    private void validateHighRiskOperation(Long operatorId, Long targetId, String mfaTicket) {
        ExceptionThrowerCore.throwBusinessIfNot(twoFactorService.validateTicket(mfaTicket, operatorId),
                ResultErrorCode.MFA_TICKET_INVALID);
        superAdminVerifier.requireSuperAdmin(operatorId);
        ExceptionThrowerCore.throwBusinessIf(operatorId.equals(targetId), ResultErrorCode.CANNOT_MODIFY_SELF);
    }

    private void recordAudit(Long operatorId, Long targetId, String operationType,
                             String beforeState, String afterState, String ip, String ua) {
        SysAuditLogCreateRequest request = new SysAuditLogCreateRequest();
        request.setOperatorUserId(operatorId);
        request.setTargetUserId(targetId);
        request.setOperationType(operationType);
        request.setBeforeState(beforeState);
        request.setAfterState(afterState);
        request.setMfaPassed(1);
        request.setRequestIp(ip);
        request.setUserAgent(ua);
        sysAuditLogService.record(request);
    }

    private void applyUserFields(SysUser user, SysUserSaveRequest request, boolean includePassword) {
        rbacAdminModelMapper.updateUser(request, user);
        if (includePassword) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setUserLevel(1);
            user.setExperiencePoints(0);
            user.setLevelUpdatedAt(null);
        }
    }

    private void validateUserUniqueness(Long currentUserId, SysUserSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                sysUserRepository.existsActiveByUsername(StrUtils.normalize(request.getUsername()), currentUserId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "用户名已存在");
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(request.getEmail())
                        && sysUserRepository.existsActiveByEmail(StrUtils.normalize(request.getEmail()), currentUserId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "邮箱已存在");
        ExceptionThrowerCore.throwBusinessIf(
                StringUtils.hasText(request.getPhone())
                        && sysUserRepository.existsActiveByPhone(StrUtils.normalize(request.getPhone()), currentUserId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "手机号已存在");
    }

    private List<Long> validateRolesExist(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctRoleIds = IdCollectionUtils.distinctNonNullIds(
                roleIds,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "角色ID不能为空");
        long count = sysRoleRepository.countActiveByIds(distinctRoleIds);
        ExceptionThrowerCore.throwBusinessIf(count != distinctRoleIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "存在无效角色");
        return distinctRoleIds;
    }

    private SysUser getAvailableUser(Long id) {
        SysUser user = sysUserRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(user == null || Integer.valueOf(1).equals(user.getDeletedFlag()), ResultErrorCode.USER_NOT_FOUND);
        return user;
    }
}

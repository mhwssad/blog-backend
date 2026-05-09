package com.cybzacg.blogbackend.module.auth.rbac.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysRole;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelConvert;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.rbac.service.SysRoleAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.IdCollectionUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色后台管理服务实现。
 *
 * <p>负责角色分页查询、资料维护、状态更新、删除以及菜单权限分配。
 */
@Service
@RequiredArgsConstructor
public class SysRoleAdminServiceImpl implements SysRoleAdminService {
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysMenuRepository sysMenuRepository;
    private final RbacAdminModelConvert rbacAdminModelConvert;
    private final RbacAssociationFactory rbacAssociationFactory;

    /**
     * 分页查询角色列表，附带每个角色已分配的菜单 ID。
     */
    @Override
    public PageResult<SysRoleAdminVO> pageRoles(SysRolePageQuery query) {
        Page<SysRole> page = sysRoleRepository.pageByAdminConditions(query);
        List<SysRoleAdminVO> records = page.getRecords().stream()
                .map(role -> rbacAdminModelConvert.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(role.getId())))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 根据 ID 获取角色详情及其关联的菜单 ID。
     */
    @Override
    public SysRoleAdminVO getRole(Long id) {
        SysRole role = getAvailableRole(id);
        return rbacAdminModelConvert.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(id));
    }

    /**
     * 创建角色，校验名称与编码唯一性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO createRole(SysRoleSaveRequest request) {
        validateRoleUniqueness(null, request);
        SysRole role = rbacAdminModelConvert.toRole(request);
        applyRoleFields(role, request);
        role.setIsDeleted(0);
        sysRoleRepository.save(role);
        return rbacAdminModelConvert.toRoleVO(role, List.of());
    }

    /**
     * 更新角色资料，校验名称与编码唯一性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO updateRole(Long id, SysRoleSaveRequest request) {
        SysRole role = getAvailableRole(id);
        requireMutableRole(role);
        validateRoleUniqueness(id, request);
        applyRoleFields(role, request);
        sysRoleRepository.updateById(role);
        return rbacAdminModelConvert.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(id));
    }

    /**
     * 更新角色状态（启用/禁用）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysRole role = getAvailableRole(id);
        requireMutableRole(role);
        role.setStatus(status);
        sysRoleRepository.updateById(role);
    }

    /**
     * 删除角色，同步清理角色-菜单和用户-角色关联数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = getAvailableRole(id);
        requireMutableRole(role);
        sysRoleMenuRepository.deleteByRoleId(id);
        sysUserRoleRepository.deleteByRoleId(id);
        sysRoleRepository.removeById(id);
    }

    /**
     * 查询角色已分配的菜单 ID 列表。
     */
    @Override
    public List<Long> listMenuIds(Long roleId) {
        getAvailableRole(roleId);
        return sysRoleMenuRepository.findMenuIdsByRoleId(roleId);
    }

    /**
     * 为角色分配菜单权限，先清除旧关联再批量写入。
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表（为空时仅清除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getAvailableRole(roleId);
        requireMutableRole(role);
        List<Long> distinctMenuIds = validateMenusExist(menuIds);
        sysRoleMenuRepository.deleteByRoleId(roleId);
        if (distinctMenuIds.isEmpty()) {
            return;
        }
        sysRoleMenuRepository.saveBatch(distinctMenuIds.stream()
                .map(menuId -> rbacAssociationFactory.createRoleMenu(roleId, menuId))
                .toList());
    }

    private void applyRoleFields(SysRole role, SysRoleSaveRequest request) {
        rbacAdminModelConvert.updateRole(request, role);
    }

    private void validateRoleUniqueness(Long currentRoleId, SysRoleSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(
                sysRoleRepository.existsActiveByName(StrUtils.normalize(request.getName()), currentRoleId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "角色名称已存在");
        ExceptionThrowerCore.throwBusinessIf(
                sysRoleRepository.existsActiveByCode(StrUtils.normalize(request.getCode()), currentRoleId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "角色编码已存在");
    }

    private List<Long> validateMenusExist(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctMenuIds = IdCollectionUtils.distinctNonNullIds(
                menuIds,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "菜单ID不能为空");
        long count = sysMenuRepository.countByIds(distinctMenuIds);
        ExceptionThrowerCore.throwBusinessIf(count != distinctMenuIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "存在无效菜单");
        return distinctMenuIds;
    }

    private SysRole getAvailableRole(Long id) {
        SysRole role = sysRoleRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(role == null || Integer.valueOf(1).equals(role.getIsDeleted()), ResultErrorCode.ILLEGAL_ARGUMENT, "角色不存在");
        return role;
    }

    /**
     * 校验角色是否为系统内置超级管理员角色。
     *
     * <p>系统内置超级管理员角色不允许进行资料、状态、删除和菜单授权修改。
     */
    private void requireMutableRole(SysRole role) {
        ExceptionThrowerCore.throwBusinessIf(isSuperAdminRole(role), ResultErrorCode.FORBIDDEN, "超级管理员角色不可修改");
    }

    private boolean isSuperAdminRole(SysRole role) {
        return role != null && (Long.valueOf(1L).equals(role.getId())
                || AuthConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode()));
    }
}

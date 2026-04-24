package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleMenuRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.service.SysRoleAdminService;
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
    private final RbacAdminModelMapper rbacAdminModelMapper;

    @Override
    public PageResult<SysRoleAdminVO> pageRoles(SysRolePageQuery query) {
        Page<SysRole> page = sysRoleRepository.pageByAdminConditions(query);
        List<SysRoleAdminVO> records = page.getRecords().stream()
                .map(role -> rbacAdminModelMapper.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(role.getId())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysRoleAdminVO getRole(Long id) {
        SysRole role = getAvailableRole(id);
        return rbacAdminModelMapper.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO createRole(SysRoleSaveRequest request) {
        validateRoleUniqueness(null, request);
        SysRole role = rbacAdminModelMapper.toRole(request);
        applyRoleFields(role, request);
        role.setIsDeleted(0);
        sysRoleRepository.save(role);
        return rbacAdminModelMapper.toRoleVO(role, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO updateRole(Long id, SysRoleSaveRequest request) {
        SysRole role = getAvailableRole(id);
        validateRoleUniqueness(id, request);
        applyRoleFields(role, request);
        sysRoleRepository.updateById(role);
        return rbacAdminModelMapper.toRoleVO(role, sysRoleMenuRepository.findMenuIdsByRoleId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysRole role = getAvailableRole(id);
        role.setStatus(status);
        sysRoleRepository.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        getAvailableRole(id);
        sysRoleMenuRepository.deleteByRoleId(id);
        sysUserRoleRepository.deleteByRoleId(id);
        sysRoleRepository.removeById(id);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        getAvailableRole(roleId);
        return sysRoleMenuRepository.findMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        getAvailableRole(roleId);
        List<Long> distinctMenuIds = validateMenusExist(menuIds);
        sysRoleMenuRepository.deleteByRoleId(roleId);
        if (distinctMenuIds.isEmpty()) {
            return;
        }
        sysRoleMenuRepository.saveBatch(distinctMenuIds.stream()
                .map(menuId -> {
                    SysRoleMenu roleMenu = new SysRoleMenu();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuId(menuId);
                    return roleMenu;
                })
                .toList());
    }

    private void applyRoleFields(SysRole role, SysRoleSaveRequest request) {
        rbacAdminModelMapper.updateRole(request, role);
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
}

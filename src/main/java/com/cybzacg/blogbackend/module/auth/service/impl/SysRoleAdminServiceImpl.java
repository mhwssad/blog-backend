package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysRoleAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 角色后台管理服务实现。
 *
 * <p>负责角色分页查询、资料维护、状态更新、删除以及菜单权限分配。
 */
@Service
@RequiredArgsConstructor
public class SysRoleAdminServiceImpl implements SysRoleAdminService {
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;
    private final RbacAdminModelMapper rbacAdminModelMapper;

    @Override
    public PageResult<SysRoleAdminVO> pageRoles(SysRolePageQuery query) {
        Page<SysRole> page = sysRoleService.lambdaQuery()
                .like(StringUtils.hasText(query.getName()), SysRole::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), SysRole::getCode, query.getCode())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .eq(SysRole::getIsDeleted, 0)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId)
                .page(new Page<>(query.getCurrent(), query.getSize()));

        List<SysRoleAdminVO> records = page.getRecords().stream()
                .map(role -> rbacAdminModelMapper.toRoleVO(role, sysRoleMenuService.listMenuIdsByRoleId(role.getId())))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysRoleAdminVO getRole(Long id) {
        SysRole role = getAvailableRole(id);
        return rbacAdminModelMapper.toRoleVO(role, sysRoleMenuService.listMenuIdsByRoleId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO createRole(SysRoleSaveRequest request) {
        validateRoleUniqueness(null, request);
        SysRole role = rbacAdminModelMapper.toRole(request);
        applyRoleFields(role, request);
        role.setIsDeleted(0);
        sysRoleService.save(role);
        return rbacAdminModelMapper.toRoleVO(role, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleAdminVO updateRole(Long id, SysRoleSaveRequest request) {
        SysRole role = getAvailableRole(id);
        validateRoleUniqueness(id, request);
        applyRoleFields(role, request);
        sysRoleService.updateById(role);
        return rbacAdminModelMapper.toRoleVO(role, sysRoleMenuService.listMenuIdsByRoleId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysRole role = getAvailableRole(id);
        role.setStatus(status);
        sysRoleService.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        getAvailableRole(id);
        sysRoleMenuService.removeByRoleId(id);
        sysUserRoleService.removeByRoleId(id);
        sysRoleService.removeById(id);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        getAvailableRole(roleId);
        return sysRoleMenuService.listMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        getAvailableRole(roleId);
        validateMenusExist(menuIds);
        sysRoleMenuService.replaceRoleMenus(roleId, menuIds);
    }

    /**
     * 将角色请求字段统一回填到实体，复用新增和更新流程。
     */
    private void applyRoleFields(SysRole role, SysRoleSaveRequest request) {
        rbacAdminModelMapper.updateRole(request, role);
    }

    /**
     * 校验角色名称和编码在未删除角色中保持唯一。
     */
    private void validateRoleUniqueness(Long currentRoleId, SysRoleSaveRequest request) {
        ExceptionThrowerCore.throwBusinessIf(sysRoleService.lambdaQuery().eq(SysRole::getIsDeleted, 0).eq(SysRole::getName, StrUtils.normalize(request.getName())).ne(currentRoleId != null, SysRole::getId, currentRoleId).exists(), ResultErrorCode.ILLEGAL_ARGUMENT, "角色名称已存在");
        ExceptionThrowerCore.throwBusinessIf(sysRoleService.lambdaQuery().eq(SysRole::getIsDeleted, 0).eq(SysRole::getCode, StrUtils.normalize(request.getCode())).ne(currentRoleId != null, SysRole::getId, currentRoleId).exists(), ResultErrorCode.ILLEGAL_ARGUMENT, "角色编码已存在");
    }

    /**
     * 校验待绑定菜单是否都存在，避免角色挂载无效菜单。
     */
    private void validateMenusExist(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(menuIds.stream().anyMatch(Objects::isNull), ResultErrorCode.ILLEGAL_ARGUMENT, "菜单ID不能为空");
        List<Long> distinctMenuIds = menuIds.stream()
                .distinct()
                .toList();
        long count = sysMenuService.lambdaQuery()
                .in(SysMenu::getId, distinctMenuIds)
                .count();
        ExceptionThrowerCore.throwBusinessIf(count != distinctMenuIds.size(), ResultErrorCode.ILLEGAL_ARGUMENT, "存在无效菜单");
    }

    /**
     * 获取未删除角色，不存在时抛出统一业务异常。
     */
    private SysRole getAvailableRole(Long id) {
        SysRole role = sysRoleService.getById(id);
        ExceptionThrowerCore.throwBusinessIf(role == null || Integer.valueOf(1).equals(role.getIsDeleted()), ResultErrorCode.ILLEGAL_ARGUMENT, "角色不存在");
        return role;
    }
}








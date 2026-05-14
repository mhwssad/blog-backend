package com.cybzacg.blogbackend.module.auth.rbac.service.impl;

import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.dto.domain.auth.SysMenu;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysMenuRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysRoleMenuRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelConvert;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.service.SysMenuAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单后台管理服务实现。
 *
 * <p>负责菜单树查询、菜单新增修改删除，以及树路径和层级结构维护。
 */
@Service
@RequiredArgsConstructor
public class SysMenuAdminServiceImpl implements SysMenuAdminService {
    private final SysMenuRepository sysMenuRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final RbacAdminModelConvert rbacAdminModelConvert;

    /**
     * 查询全部菜单并组装为树形结构。
     */
    @Override
    public List<SysMenuAdminVO> listMenuTree() {
        return buildMenuTree(sysMenuRepository.findAllOrdered());
    }

    /**
     * 根据 ID 获取菜单详情。
     */
    @Override
    public SysMenuAdminVO getMenu(Long id) {
        rejectWildcardMenu(id);
        return rbacAdminModelConvert.toMenuVO(getMenuOrThrow(id));
    }

    /**
     * 新建菜单，校验父菜单和类型后自动维护树路径。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuAdminVO createMenu(SysMenuSaveRequest request) {
        SysMenu parent = validateParent(request.getParentId(), null);

        SysMenu menu = rbacAdminModelConvert.toMenu(request);
        applyMenuFields(menu, request);
        menu.setTreePath(buildTreePath(parent));
        sysMenuRepository.save(menu);
        return rbacAdminModelConvert.toMenuVO(menu);
    }

    /**
     * 更新菜单，重新计算树路径并级联刷新子节点。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuAdminVO updateMenu(Long id, SysMenuSaveRequest request) {
        rejectWildcardMenu(id);
        SysMenu menu = getMenuOrThrow(id);
        SysMenu parent = validateParent(request.getParentId(), id);

        applyMenuFields(menu, request);
        menu.setTreePath(buildTreePath(parent));
        sysMenuRepository.updateById(menu);
        refreshChildrenTreePath(menu);
        return rbacAdminModelConvert.toMenuVO(menu);
    }

    /**
     * 删除菜单，存在子菜单时拒绝删除并同步清理角色关联。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        rejectWildcardMenu(id);
        getMenuOrThrow(id);
        boolean hasChildren = sysMenuRepository.existsByParentId(id);
        ExceptionThrowerCore.throwBusinessIf(hasChildren, ResultErrorCode.ILLEGAL_ARGUMENT, "当前菜单存在子菜单，无法删除");
        sysRoleMenuRepository.deleteByMenuId(id);
        sysMenuRepository.removeById(id);
    }

    private void applyMenuFields(SysMenu menu, SysMenuSaveRequest request) {
        rbacAdminModelConvert.updateMenu(request, menu);
    }

    private SysMenu validateParent(Long parentId, Long currentMenuId) {
        ExceptionThrowerCore.throwBusinessIfNull(parentId, ResultErrorCode.ILLEGAL_ARGUMENT, "父菜单ID不能为空");
        if (MenuConstants.ROOT_PARENT_ID.equals(parentId)) {
            return null;
        }
        ExceptionThrowerCore.throwBusinessIf(currentMenuId != null && currentMenuId.equals(parentId), ResultErrorCode.ILLEGAL_ARGUMENT, "父菜单不能为自身");

        SysMenu parent = getMenuOrThrow(parentId);
        ExceptionThrowerCore.throwBusinessIf(currentMenuId != null && isDescendant(parent, currentMenuId), ResultErrorCode.ILLEGAL_ARGUMENT, "父菜单不能选择当前菜单的子节点");
        return parent;
    }

    private boolean isDescendant(SysMenu menu, Long currentMenuId) {
        if (!StrUtils.hasText(menu.getTreePath())) {
            return false;
        }
        String[] segments = menu.getTreePath().split(",");
        for (String segment : segments) {
            if (String.valueOf(currentMenuId).equals(StrUtils.trim(segment))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通配符菜单禁止查看、修改和删除。
     */
    private void rejectWildcardMenu(Long id) {
        if (MenuConstants.WILDCARD_MENU_ID.equals(id)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.PERMISSION_WILDCARD_NOT_ALLOWED);
        }
    }

    private SysMenu getMenuOrThrow(Long id) {
        SysMenu menu = sysMenuRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(menu, ResultErrorCode.ILLEGAL_ARGUMENT, "菜单不存在");
        return menu;
    }

    private String buildTreePath(SysMenu parent) {
        if (parent == null) {
            return String.valueOf(MenuConstants.ROOT_PARENT_ID);
        }
        if (!StrUtils.hasText(parent.getTreePath())) {
            return String.valueOf(parent.getId());
        }
        return parent.getTreePath() + "," + parent.getId();
    }

    private void refreshChildrenTreePath(SysMenu menu) {
        List<SysMenu> children = sysMenuRepository.findByParentId(menu.getId());
        if (children == null || children.isEmpty()) {
            return;
        }
        for (SysMenu child : children) {
            child.setTreePath(buildTreePath(menu));
            sysMenuRepository.updateById(child);
            refreshChildrenTreePath(child);
        }
    }

    private List<SysMenuAdminVO> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        Map<Long, SysMenuAdminVO> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (MenuConstants.WILDCARD_MENU_ID.equals(menu.getId())) {
                continue;
            }
            menuMap.put(menu.getId(), rbacAdminModelConvert.toMenuVO(menu));
        }

        List<SysMenuAdminVO> roots = new ArrayList<>();
        for (SysMenuAdminVO menu : menuMap.values()) {
            SysMenuAdminVO parent = menuMap.get(menu.getParentId());
            if (parent == null || MenuConstants.ROOT_PARENT_ID.equals(menu.getParentId())) {
                roots.add(menu);
                continue;
            }
            parent.getChildren().add(menu);
        }
        return roots;
    }
}

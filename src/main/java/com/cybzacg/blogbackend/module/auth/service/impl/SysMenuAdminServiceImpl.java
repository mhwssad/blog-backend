package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysMenuAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final SysMenuService sysMenuService;
    private final SysRoleMenuService sysRoleMenuService;
    private final RbacAdminModelMapper rbacAdminModelMapper;

    @Override
    public List<SysMenuAdminVO> listMenuTree() {
        List<SysMenu> menus = sysMenuService.lambdaQuery()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId)
                .list();
        return buildMenuTree(menus);
    }

    @Override
    public SysMenuAdminVO getMenu(Long id) {
        return rbacAdminModelMapper.toMenuVO(getMenuOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuAdminVO createMenu(SysMenuSaveRequest request) {
        SysMenu parent = validateParent(request.getParentId(), null);
        validateMenuType(request.getType());

        SysMenu menu = new SysMenu();
        applyMenuFields(menu, request);
        menu.setTreePath(buildTreePath(parent));
        sysMenuService.save(menu);
        return rbacAdminModelMapper.toMenuVO(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuAdminVO updateMenu(Long id, SysMenuSaveRequest request) {
        SysMenu menu = getMenuOrThrow(id);
        SysMenu parent = validateParent(request.getParentId(), id);
        validateMenuType(request.getType());

        applyMenuFields(menu, request);
        menu.setTreePath(buildTreePath(parent));
        sysMenuService.updateById(menu);
        refreshChildrenTreePath(menu);
        return rbacAdminModelMapper.toMenuVO(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        getMenuOrThrow(id);
        boolean hasChildren = sysMenuService.lambdaQuery()
                .eq(SysMenu::getParentId, id)
                .exists();
        if (hasChildren) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前菜单存在子菜单，无法删除");
        }
        sysRoleMenuService.removeByMenuId(id);
        sysMenuService.removeById(id);
    }

    /**
     * 将请求中的菜单字段统一写回实体，保持新增和更新逻辑一致。
     */
    private void applyMenuFields(SysMenu menu, SysMenuSaveRequest request) {
        menu.setParentId(request.getParentId());
        menu.setName(normalize(request.getName()));
        menu.setType(normalize(request.getType()));
        menu.setRouteName(normalize(request.getRouteName()));
        menu.setRoutePath(normalize(request.getRoutePath()));
        menu.setComponent(normalize(request.getComponent()));
        menu.setPerm(normalize(request.getPerm()));
        menu.setAlwaysShow(request.getAlwaysShow() != null ? request.getAlwaysShow() : 0);
        menu.setKeepAlive(request.getKeepAlive() != null ? request.getKeepAlive() : 0);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
        menu.setSort(request.getSort() != null ? request.getSort() : 0);
        menu.setIcon(normalize(request.getIcon()));
        menu.setRedirect(normalize(request.getRedirect()));
        menu.setParams(request.getParams());
    }

    /**
     * 校验父菜单是否合法，避免出现自关联或挂载到子孙节点的情况。
     */
    private SysMenu validateParent(Long parentId, Long currentMenuId) {
        if (parentId == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父菜单ID不能为空");
        }
        if (MenuConstants.ROOT_PARENT_ID.equals(parentId)) {
            return null;
        }
        if (currentMenuId != null && currentMenuId.equals(parentId)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父菜单不能为自身");
        }

        SysMenu parent = getMenuOrThrow(parentId);
        if (currentMenuId != null && isDescendant(parent, currentMenuId)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父菜单不能选择当前菜单的子节点");
        }
        return parent;
    }

    /**
     * 判断候选父节点是否为当前菜单的后代节点，防止树结构成环。
     */
    private boolean isDescendant(SysMenu menu, Long currentMenuId) {
        if (!StringUtils.hasText(menu.getTreePath())) {
            return false;
        }
        String[] segments = menu.getTreePath().split(",");
        for (String segment : segments) {
            if (String.valueOf(currentMenuId).equals(segment.trim())) {
                return true;
            }
        }
        return false;
    }

    private void validateMenuType(String type) {
        String normalizedType = normalize(type);
        if (!MenuConstants.TYPE_CATALOG.equalsIgnoreCase(normalizedType)
                && !MenuConstants.TYPE_MENU.equalsIgnoreCase(normalizedType)
                && !MenuConstants.TYPE_BUTTON.equalsIgnoreCase(normalizedType)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "菜单类型非法");
        }
    }

    private SysMenu getMenuOrThrow(Long id) {
        SysMenu menu = sysMenuService.getById(id);
        if (menu == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "菜单不存在");
        }
        return menu;
    }

    private String buildTreePath(SysMenu parent) {
        if (parent == null) {
            return String.valueOf(MenuConstants.ROOT_PARENT_ID);
        }
        if (!StringUtils.hasText(parent.getTreePath())) {
            return String.valueOf(parent.getId());
        }
        return parent.getTreePath() + "," + parent.getId();
    }

    /**
     * 递归刷新当前菜单所有子节点的树路径，保证迁移父节点后层级链路正确。
     */
    private void refreshChildrenTreePath(SysMenu menu) {
        List<SysMenu> children = sysMenuService.lambdaQuery()
                .eq(SysMenu::getParentId, menu.getId())
                .list();
        if (children == null || children.isEmpty()) {
            return;
        }
        for (SysMenu child : children) {
            child.setTreePath(buildTreePath(menu));
            sysMenuService.updateById(child);
            refreshChildrenTreePath(child);
        }
    }

    /**
     * 将菜单列表组装成树结构，供后台菜单管理界面直接展示。
     */
    private List<SysMenuAdminVO> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        Map<Long, SysMenuAdminVO> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menuMap.put(menu.getId(), rbacAdminModelMapper.toMenuVO(menu));
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

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}

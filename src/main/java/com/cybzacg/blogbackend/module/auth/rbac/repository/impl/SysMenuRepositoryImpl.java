package com.cybzacg.blogbackend.module.auth.rbac.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.auth.SysMenu;
import com.cybzacg.blogbackend.mapper.auth.SysMenuMapper;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysMenuRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 系统菜单 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysMenuRepositoryImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuRepository {

    /**
     * 根据用户 ID 查询权限标识列表，无结果时返回空列表。
     */
    @Override
    public List<String> findPermissionsByUserId(Long userId) {
        List<String> permissions = baseMapper.selectPermissionsByUserId(userId);
        return permissions != null ? permissions : List.of();
    }

    /**
     * 根据用户 ID 查询菜单列表，无结果时返回空列表。
     */
    @Override
    public List<SysMenu> findMenusByUserId(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return menus != null ? menus : List.of();
    }

    /**
     * 查询全部菜单，按父节点、排序权重和 ID 升序排列以构建菜单树。
     */
    @Override
    public List<SysMenu> findAllOrdered() {
        return list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
    }

    /**
     * 根据父节点 ID 查询直属子菜单。
     */
    @Override
    public List<SysMenu> findByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId));
    }

    /**
     * 判断指定父节点下是否还存在子菜单。
     */
    @Override
    public boolean existsByParentId(Long parentId) {
        return exists(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId));
    }

    /**
     * 统计给定 ID 集合中菜单的数量。
     */
    @Override
    public long countByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, ids));
    }
}

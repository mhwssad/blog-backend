package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.mapper.SysMenuMapper;
import com.cybzacg.blogbackend.module.auth.repository.SysMenuRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 系统菜单 Repository 实现。
 */
@Repository
public class SysMenuRepositoryImpl extends ServiceImpl<SysMenuMapper, SysMenu>
        implements SysMenuRepository {

    @Override
    public List<String> findPermissionsByUserId(Long userId) {
        List<String> permissions = baseMapper.selectPermissionsByUserId(userId);
        return permissions != null ? permissions : List.of();
    }

    @Override
    public List<SysMenu> findMenusByUserId(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return menus != null ? menus : List.of();
    }

    @Override
    public List<SysMenu> findAllOrdered() {
        return list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
    }

    @Override
    public List<SysMenu> findByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId));
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return exists(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId));
    }

    @Override
    public long countByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, ids));
    }
}

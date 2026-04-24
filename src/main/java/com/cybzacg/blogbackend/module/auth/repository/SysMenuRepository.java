package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysMenu;

import java.util.Collection;
import java.util.List;

/**
 * 系统菜单 Repository。
 */
public interface SysMenuRepository extends IService<SysMenu> {
    List<String> findPermissionsByUserId(Long userId);

    List<SysMenu> findMenusByUserId(Long userId);

    List<SysMenu> findAllOrdered();

    List<SysMenu> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);

    long countByIds(Collection<Long> ids);
}

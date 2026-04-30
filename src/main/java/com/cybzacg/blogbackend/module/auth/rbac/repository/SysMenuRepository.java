package com.cybzacg.blogbackend.module.auth.rbac.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.auth.SysMenu;

import java.util.Collection;
import java.util.List;

/**
 * 系统菜单 Repository。
 * <p>封装菜单实体的持久化操作，提供权限查询、树形排序查询及父子关系判断等能力。
 */
public interface SysMenuRepository extends IService<SysMenu> {
    /**
     * 根据用户 ID 查询其拥有的权限标识列表。
     */
    List<String> findPermissionsByUserId(Long userId);

    /**
     * 根据用户 ID 查询其可访问的菜单列表。
     */
    List<SysMenu> findMenusByUserId(Long userId);

    /**
     * 查询全部菜单并按父节点、排序和 ID 升序排列。
     */
    List<SysMenu> findAllOrdered();

    /**
     * 根据父节点 ID 查询直属子菜单。
     */
    List<SysMenu> findByParentId(Long parentId);

    /**
     * 判断指定父节点下是否存在子菜单。
     */
    boolean existsByParentId(Long parentId);

    /**
     * 统计给定 ID 集合中菜单的数量。
     */
    long countByIds(Collection<Long> ids);
}

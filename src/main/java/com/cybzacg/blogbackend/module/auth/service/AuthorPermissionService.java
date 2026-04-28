package com.cybzacg.blogbackend.module.auth.service;

/**
 * 作者权限服务。
 *
 * <p>统一收口作者身份判断和作者角色授予，避免业务层散落角色编码判断。
 */
public interface AuthorPermissionService {

    /**
     * 判断指定用户是否已具备作者角色。
     */
    boolean hasAuthorRole(Long userId);

    /**
     * 为指定用户授予作者角色；已具备角色时保持幂等。
     */
    void grantAuthorRole(Long userId);

    /**
     * 撤销指定用户的作者角色；未持有作者角色时保持幂等。
     */
    void revokeAuthorRole(Long userId);
}

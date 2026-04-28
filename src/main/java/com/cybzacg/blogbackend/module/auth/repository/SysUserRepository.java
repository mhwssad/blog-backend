package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;

import java.util.Collection;

/**
 * 系统用户 Repository。
 * <p>封装用户实体的持久化操作，提供按身份、字段查询及管理端分页等能力。
 */
public interface SysUserRepository extends IService<SysUser> {
    /**
     * 根据用户名查找未删除的用户。
     */
    SysUser findByUsername(String username);

    /**
     * 根据邮箱查找未删除的用户。
     */
    SysUser findByEmail(String email);

    /**
     * 更新指定用户的登录信息（IP、登录时间）。
     */
    boolean updateLoginInfo(Long userId, String ip);

    /**
     * 判断指定身份标识（用户名/邮箱/手机号）是否已被未删除用户占用。
     */
    boolean existsActiveByIdentity(String identity);

    /**
     * 判断指定字段的值是否已被未删除用户占用。
     */
    boolean existsActiveByField(String fieldName, String value);

    /**
     * 判断用户名是否已被其他未删除用户占用（排除指定 ID）。
     */
    boolean existsActiveByUsername(String username, Long excludeId);

    /**
     * 判断邮箱是否已被其他未删除用户占用（排除指定 ID）。
     */
    boolean existsActiveByEmail(String email, Long excludeId);

    /**
     * 判断手机号是否已被其他未删除用户占用（排除指定 ID）。
     */
    boolean existsActiveByPhone(String phone, Long excludeId);

    /**
     * 统计给定 ID 集合中未删除用户的数量。
     */
    long countActiveByIds(Collection<Long> ids);

    /**
     * 根据管理端查询条件对未删除用户进行分页。
     */
    Page<SysUser> pageByAdminConditions(SysUserPageQuery query);

    /**
     * 原子递增用户经验值。
     *
     * @return 影响行数
     */
    int incrementExperiencePoints(Long userId, int delta);

    /**
     * 更新用户等级和等级变更时间。
     *
     * @return 影响行数
     */
    int updateLevel(Long userId, int level);
}

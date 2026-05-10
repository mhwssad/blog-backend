package com.cybzacg.blogbackend.module.auth.account.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.auth.SysUserRole;
import com.cybzacg.blogbackend.dto.mapper.auth.SysUserRoleMapper;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关系 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysUserRoleRepositoryImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
        implements SysUserRoleRepository {

    /**
     * 根据用户 ID 查询关联的角色 ID 列表并去重。
     */
    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    /**
     * 根据用户 ID 删除所有关联的角色关系。
     */
    @Override
    public void deleteByUserId(Long userId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    /**
     * 根据角色 ID 删除所有关联的用户关系。
     */
    @Override
    public void deleteByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }

    /**
     * 删除指定用户与角色的关联关系。
     */
    @Override
    public void deleteByUserIdAndRoleId(Long userId, Long roleId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
    }
}

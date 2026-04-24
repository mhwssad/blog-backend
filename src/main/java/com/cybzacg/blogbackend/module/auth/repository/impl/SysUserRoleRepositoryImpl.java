package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserRole;
import com.cybzacg.blogbackend.mapper.SysUserRoleMapper;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关系 Repository 实现。
 */
@Repository
public class SysUserRoleRepositoryImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
        implements SysUserRoleRepository {

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    @Override
    public void deleteByUserId(Long userId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }
}

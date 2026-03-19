package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserRole;
import com.cybzacg.blogbackend.mapper.SysUserRoleMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
* @author liujian
* @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
    implements SysUserRoleService{

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        removeByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        saveBatch(roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .toList());
    }

    @Override
    public void removeByUserId(Long userId) {
        lambdaUpdate().eq(SysUserRole::getUserId, userId).remove();
    }

    @Override
    public void removeByRoleId(Long roleId) {
        lambdaUpdate().eq(SysUserRole::getRoleId, roleId).remove();
    }
}





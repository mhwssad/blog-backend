package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.mapper.SysRoleMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 系统角色 Repository 实现。
 */
@Repository
public class SysRoleRepositoryImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleRepository {

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        List<String> roleCodes = baseMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null ? roleCodes : List.of();
    }

    @Override
    public boolean existsActiveByName(String name, Long excludeId) {
        return existsActiveByUniqueField(SysRole::getName, name, excludeId);
    }

    @Override
    public boolean existsActiveByCode(String code, Long excludeId) {
        return existsActiveByUniqueField(SysRole::getCode, code, excludeId);
    }

    @Override
    public long countActiveByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, ids)
                .eq(SysRole::getIsDeleted, 0));
    }

    @Override
    public Page<SysRole> pageByAdminConditions(SysRolePageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(query.getName()), SysRole::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), SysRole::getCode, query.getCode())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .eq(SysRole::getIsDeleted, 0)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
    }

    private <T> boolean existsActiveByUniqueField(SFunction<SysRole, T> field, T value, Long excludeId) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getIsDeleted, 0)
                .eq(field, value)
                .ne(excludeId != null, SysRole::getId, excludeId));
    }
}

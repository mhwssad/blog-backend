package com.cybzacg.blogbackend.module.auth.rbac.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.auth.SysRole;
import com.cybzacg.blogbackend.mapper.auth.SysRoleMapper;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 系统角色 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysRoleRepositoryImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleRepository {

    /**
     * 根据用户 ID 查询关联的角色编码列表，无结果时返回空列表。
     */
    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        List<String> roleCodes = baseMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null ? roleCodes : List.of();
    }

    /**
     * 判断角色名称是否已被其他未删除角色占用。
     */
    @Override
    public boolean existsActiveByName(String name, Long excludeId) {
        return existsActiveByUniqueField(SysRole::getName, name, excludeId);
    }

    /**
     * 判断角色编码是否已被其他未删除角色占用。
     */
    @Override
    public boolean existsActiveByCode(String code, Long excludeId) {
        return existsActiveByUniqueField(SysRole::getCode, code, excludeId);
    }

    /**
     * 根据角色编码查询未删除角色。
     */
    @Override
    public SysRole findByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code)
                .eq(SysRole::getIsDeleted, 0)
                .last("limit 1"), false);
    }

    /**
     * 统计给定 ID 集合中未删除角色的数量。
     */
    @Override
    public long countActiveByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, ids)
                .eq(SysRole::getIsDeleted, 0));
    }

    /**
     * 根据管理端查询条件进行分页，按排序字段和 ID 升序排列。
     */
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

    /**
     * 通用的唯一字段存在性检查，排除指定 ID 后判断是否已有未删除记录占用该值。
     */
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

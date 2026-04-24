package com.cybzacg.blogbackend.module.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.mapper.SysUserMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 系统用户 Repository 实现，基于 MyBatis-Plus。
 */
@Repository
public class SysUserRepositoryImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserRepository {
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PHONE = "phone";

    /** 根据用户名查找用户。 */
    @Override
    public SysUser findByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    /** 根据邮箱查找用户。 */
    @Override
    public SysUser findByEmail(String email) {
        return baseMapper.selectByEmail(email);
    }

    /** 更新用户登录信息，返回是否成功。 */
    @Override
    public boolean updateLoginInfo(Long userId, String ip) {
        return baseMapper.updateLoginInfo(userId, ip) > 0;
    }

    /** 判断身份标识是否已被未删除用户占用，同时匹配用户名、邮箱和手机号。 */
    @Override
    public boolean existsActiveByIdentity(String identity) {
        if (!StringUtils.hasText(identity)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeletedFlag, 0)
                .and(wrapper -> wrapper.eq(SysUser::getUsername, identity)
                        .or()
                        .eq(SysUser::getEmail, identity)
                        .or()
                        .eq(SysUser::getPhone, identity)));
    }

    /** 根据字段名称动态判断该字段的值是否已被未删除用户占用。 */
    @Override
    public boolean existsActiveByField(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeletedFlag, 0);
        switch (fieldName) {
            case FIELD_USERNAME -> queryWrapper.eq(SysUser::getUsername, value);
            case FIELD_EMAIL -> queryWrapper.eq(SysUser::getEmail, value);
            case FIELD_PHONE -> queryWrapper.eq(SysUser::getPhone, value);
            default -> throw new IllegalArgumentException("Unsupported user field: " + fieldName);
        }
        return exists(queryWrapper);
    }

    /** 判断用户名是否已被其他未删除用户占用。 */
    @Override
    public boolean existsActiveByUsername(String username, Long excludeId) {
        return existsActiveByUniqueField(SysUser::getUsername, username, excludeId);
    }

    /** 判断邮箱是否已被其他未删除用户占用。 */
    @Override
    public boolean existsActiveByEmail(String email, Long excludeId) {
        return existsActiveByUniqueField(SysUser::getEmail, email, excludeId);
    }

    /** 判断手机号是否已被其他未删除用户占用。 */
    @Override
    public boolean existsActiveByPhone(String phone, Long excludeId) {
        return existsActiveByUniqueField(SysUser::getPhone, phone, excludeId);
    }

    /** 统计给定 ID 集合中未删除用户的数量。 */
    @Override
    public long countActiveByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, ids)
                .eq(SysUser::getDeletedFlag, 0));
    }

    /** 根据管理端查询条件进行分页，按创建时间降序排列。 */
    @Override
    public Page<SysUser> pageByAdminConditions(SysUserPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), SysUser::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getEmail()), SysUser::getEmail, query.getEmail())
                .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(SysUser::getDeletedFlag, 0)
                .orderByDesc(SysUser::getCreatedAt));
    }

    /**
     * 通用的唯一字段存在性检查，排除指定 ID 后判断是否已有未删除记录占用该值。
     */
    private <T> boolean existsActiveByUniqueField(SFunction<SysUser, T> field, T value, Long excludeId) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeletedFlag, 0)
                .eq(field, value)
                .ne(excludeId != null, SysUser::getId, excludeId));
    }
}

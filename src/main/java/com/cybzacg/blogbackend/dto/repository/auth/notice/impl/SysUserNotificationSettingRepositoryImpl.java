package com.cybzacg.blogbackend.dto.repository.auth.notice.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotificationSetting;
import com.cybzacg.blogbackend.dto.mapper.notice.SysUserNotificationSettingMapper;
import com.cybzacg.blogbackend.dto.repository.auth.notice.SysUserNotificationSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户通知偏好设置 Repository 实现。
 */
@Repository
public class SysUserNotificationSettingRepositoryImpl
        extends ServiceImpl<SysUserNotificationSettingMapper, SysUserNotificationSetting>
        implements SysUserNotificationSettingRepository {

    /**
     * 查询单个用户的通知偏好配置。
     */
    @Override
    public SysUserNotificationSetting findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUserNotificationSetting>()
                .eq(SysUserNotificationSetting::getUserId, userId)
                .last("limit 1"), false);
    }

    /**
     * 批量读取用户通知偏好并按用户ID映射。
     */
    @Override
    public Map<Long, SysUserNotificationSetting> findMapByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return list(new LambdaQueryWrapper<SysUserNotificationSetting>()
                .in(SysUserNotificationSetting::getUserId, userIds))
                .stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(SysUserNotificationSetting::getUserId,
                        Function.identity(),
                        (left, right) -> Objects.requireNonNullElse(left, right)));
    }
}

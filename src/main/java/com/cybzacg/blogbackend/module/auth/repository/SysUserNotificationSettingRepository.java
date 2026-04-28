package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserNotificationSetting;

import java.util.Collection;
import java.util.Map;

/**
 * 用户通知偏好设置 Repository。
 */
public interface SysUserNotificationSettingRepository extends IService<SysUserNotificationSetting> {
    /**
     * 根据用户ID查询通知偏好。
     */
    SysUserNotificationSetting findByUserId(Long userId);

    /**
     * 批量按用户ID查询通知偏好。
     */
    Map<Long, SysUserNotificationSetting> findMapByUserIds(Collection<Long> userIds);
}

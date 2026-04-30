package com.cybzacg.blogbackend.module.auth.notice.service;

import com.cybzacg.blogbackend.domain.notice.SysUserNotificationSetting;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;

import java.util.Map;

/**
 * 用户通知偏好服务。
 */
public interface UserNotificationPreferenceService {
    /**
     * 为新用户初始化默认通知偏好；已存在配置时保持幂等。
     */
    void initializeDefaultSettings(Long userId);

    /**
     * 判断指定用户是否开启某类通知，缺失配置时按默认开启处理。
     */
    boolean isNotificationEnabled(Long userId, NotificationTypeEnum type);

    /**
     * 查询并确保用户拥有通知偏好配置。
     */
    SysUserNotificationSetting getOrCreateSettings(Long userId);

    /**
     * 批量更新通知偏好。
     */
    void updateSettings(Long userId, Map<NotificationTypeEnum, Boolean> updates);
}

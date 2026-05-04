package com.cybzacg.blogbackend.module.auth.notice.service.impl;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.notice.SysUserNotificationSetting;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.notice.repository.SysUserNotificationSettingRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 用户通知偏好服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceServiceImpl implements UserNotificationPreferenceService {
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysUserRepository sysUserRepository;
    private final SysUserNotificationSettingRepository sysUserNotificationSettingRepository;

    /**
     * 初始化用户默认通知偏好，全部按“开启”落库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeDefaultSettings(Long userId) {
        requireAvailableUser(userId);
        if (sysUserNotificationSettingRepository.findByUserId(userId) != null) {
            return;
        }
        try {
            sysUserNotificationSettingRepository.save(buildDefaultSetting(userId));
        } catch (DuplicateKeyException ignored) {
            // 并发初始化时以唯一索引兜底，已有记录即可。
        }
    }

    /**
     * 查询通知开关；缺失配置时自动补一份默认配置并按默认开启处理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean isNotificationEnabled(Long userId, NotificationTypeEnum type) {
        if (userId == null || type == null) {
            return false;
        }
        SysUserNotificationSetting setting = getOrCreateSettings(userId);
        if (setting == null) {
            return true;
        }
        return type.isEnabled(setting);
    }

    /**
     * 查询或补齐用户默认通知偏好。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserNotificationSetting getOrCreateSettings(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserNotificationSetting setting = sysUserNotificationSettingRepository.findByUserId(userId);
        if (setting != null) {
            return setting;
        }
        initializeDefaultSettings(userId);
        return sysUserNotificationSettingRepository.findByUserId(userId);
    }

    /**
     * 批量更新用户通知偏好，只修改请求中出现的类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(Long userId, Map<NotificationTypeEnum, Boolean> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        SysUserNotificationSetting setting = getOrCreateSettings(userId);
        ExceptionThrowerCore.throwBusinessIfNull(setting, ResultErrorCode.ILLEGAL_ARGUMENT, "通知偏好配置不存在");
        updates.forEach((type, enabled) -> {
            if (type != null && enabled != null) {
                type.apply(setting, enabled);
            }
        });
        sysUserNotificationSettingRepository.updateById(setting);
    }

    private SysUserNotificationSetting buildDefaultSetting(Long userId) {
        SysUserNotificationSetting setting = new SysUserNotificationSetting();
        setting.setUserId(userId);
        setting.setCommentNoticeEnabled(ENABLED);
        setting.setLikeNoticeEnabled(ENABLED);
        setting.setCollectNoticeEnabled(ENABLED);
        setting.setFollowNoticeEnabled(ENABLED);
        setting.setPrivateChatNoticeEnabled(ENABLED);
        setting.setMentionNoticeEnabled(ENABLED);
        setting.setChannelAnnouncementEnabled(ENABLED);
        setting.setSystemNoticeEnabled(ENABLED);
        setting.setAiTaskNoticeEnabled(ENABLED);
        setting.setReportResultNoticeEnabled(ENABLED);
        return setting;
    }

    private void requireAvailableUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND
        );
    }
}

package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.domain.SysUserNotificationSetting;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingBatchUpdateRequest;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingItemVO;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingStatusUpdateRequest;
import com.cybzacg.blogbackend.module.auth.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.service.UserNotificationSettingService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户通知设置服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserNotificationSettingServiceImpl implements UserNotificationSettingService {
    private final UserNotificationPreferenceService userNotificationPreferenceService;

    /**
     * 查询当前用户的全部通知偏好。
     */
    @Override
    public List<UserNotificationSettingItemVO> listMySettings() {
        Long userId = SecurityUtils.requireUserId();
        SysUserNotificationSetting setting = userNotificationPreferenceService.getOrCreateSettings(userId);
        return List.of(NotificationTypeEnum.values()).stream()
                .map(type -> UserNotificationSettingItemVO.builder()
                        .type(type.getCode())
                        .label(type.getLabel())
                        .enabled(type.isEnabled(setting))
                        .build())
                .toList();
    }

    /**
     * 批量更新当前用户的通知偏好。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMySettings(UserNotificationSettingBatchUpdateRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "通知设置参数不能为空");
        Long userId = SecurityUtils.requireUserId();
        Map<NotificationTypeEnum, Boolean> updates = new LinkedHashMap<>();
        request.getSettings().forEach(item -> updates.put(resolveType(item.getType()), item.getEnabled()));
        userNotificationPreferenceService.updateSettings(userId, updates);
    }

    /**
     * 单独更新某一类通知偏好。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMySetting(String type, UserNotificationSettingStatusUpdateRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "通知设置参数不能为空");
        Long userId = SecurityUtils.requireUserId();
        userNotificationPreferenceService.updateSettings(userId, Map.of(resolveType(type), request.getEnabled()));
    }

    private NotificationTypeEnum resolveType(String type) {
        NotificationTypeEnum notificationType = NotificationTypeEnum.fromCode(type);
        ExceptionThrowerCore.throwBusinessIf(notificationType == null, ResultErrorCode.ILLEGAL_ARGUMENT, "通知类型不合法");
        return notificationType;
    }
}

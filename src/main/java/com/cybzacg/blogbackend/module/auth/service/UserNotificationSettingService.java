package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingBatchUpdateRequest;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingItemVO;
import com.cybzacg.blogbackend.module.auth.model.user.UserNotificationSettingStatusUpdateRequest;

import java.util.List;

/**
 * 用户通知设置服务。
 */
public interface UserNotificationSettingService {
    List<UserNotificationSettingItemVO> listMySettings();

    void updateMySettings(UserNotificationSettingBatchUpdateRequest request);

    void updateMySetting(String type, UserNotificationSettingStatusUpdateRequest request);
}

package com.cybzacg.blogbackend.module.auth.notice.service;

import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingBatchUpdateRequest;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingItemVO;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingStatusUpdateRequest;

import java.util.List;

/**
 * 用户通知设置服务。
 */
public interface UserNotificationSettingService {
    List<UserNotificationSettingItemVO> listMySettings();

    void updateMySettings(UserNotificationSettingBatchUpdateRequest request);

    void updateMySetting(String type, UserNotificationSettingStatusUpdateRequest request);
}

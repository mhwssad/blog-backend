package com.cybzacg.blogbackend.module.auth.notice;

import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotificationSetting;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingBatchUpdateItemRequest;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingBatchUpdateRequest;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingItemVO;
import com.cybzacg.blogbackend.module.auth.notice.model.user.UserNotificationSettingStatusUpdateRequest;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.notice.service.impl.UserNotificationSettingServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserNotificationSettingServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationSettingServiceImplTest {

    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;

    private UserNotificationSettingServiceImpl settingService;

    @BeforeEach
    void setUp() {
        settingService = new UserNotificationSettingServiceImpl(
                userNotificationPreferenceService
        );
    }

    // ========== listMySettings ==========

    @Test
    void listMySettingsShouldReturnAllNotificationTypes() {
        Long userId = 1L;
        SysUserNotificationSetting setting = buildDefaultSetting(userId);

        when(userNotificationPreferenceService.getOrCreateSettings(userId)).thenReturn(setting);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            List<UserNotificationSettingItemVO> result = settingService.listMySettings();

            assertNotNull(result);
            assertEquals(NotificationTypeEnum.values().length, result.size());
            // All should be enabled by default
            assertTrue(result.stream().allMatch(UserNotificationSettingItemVO::getEnabled));
        }
    }

    // ========== updateMySettings ==========

    @Test
    void updateMySettingsShouldDelegateToPreferenceService() {
        Long userId = 1L;

        UserNotificationSettingBatchUpdateRequest request = new UserNotificationSettingBatchUpdateRequest();
        UserNotificationSettingBatchUpdateItemRequest item = new UserNotificationSettingBatchUpdateItemRequest();
        item.setType("comment_me");
        item.setEnabled(false);
        request.setSettings(List.of(item));

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            settingService.updateMySettings(request);

            verify(userNotificationPreferenceService).updateSettings(eq(userId), any(Map.class));
        }
    }

    // ========== updateMySetting invalid type ==========

    @Test
    void updateMySettingShouldRejectInvalidType() {
        Long userId = 1L;
        UserNotificationSettingStatusUpdateRequest request = new UserNotificationSettingStatusUpdateRequest();
        request.setEnabled(true);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> settingService.updateMySetting("invalid_type", request));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            verify(userNotificationPreferenceService, never()).updateSettings(anyLong(), any());
        }
    }

    // ========== updateMySetting single type ==========

    @Test
    void updateMySettingShouldUpdateSingleType() {
        Long userId = 1L;
        UserNotificationSettingStatusUpdateRequest request = new UserNotificationSettingStatusUpdateRequest();
        request.setEnabled(false);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            settingService.updateMySetting("comment_me", request);

            verify(userNotificationPreferenceService).updateSettings(eq(userId), any(Map.class));
        }
    }

    // ========== updateMySettings null request ==========

    @Test
    void updateMySettingsShouldRejectNullRequest() {
        Long userId = 1L;

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> settingService.updateMySettings(null));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            verify(userNotificationPreferenceService, never()).updateSettings(anyLong(), any());
        }
    }

    // ========== Helper methods ==========

    private SysUserNotificationSetting buildDefaultSetting(Long userId) {
        SysUserNotificationSetting setting = new SysUserNotificationSetting();
        setting.setId(1L);
        setting.setUserId(userId);
        setting.setCommentNoticeEnabled(1);
        setting.setLikeNoticeEnabled(1);
        setting.setCollectNoticeEnabled(1);
        setting.setFollowNoticeEnabled(1);
        setting.setPrivateChatNoticeEnabled(1);
        setting.setMentionNoticeEnabled(1);
        setting.setChannelAnnouncementEnabled(1);
        setting.setSystemNoticeEnabled(1);
        setting.setAiTaskNoticeEnabled(1);
        return setting;
    }
}

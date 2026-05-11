package com.cybzacg.blogbackend.module.auth.notice;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotificationSetting;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.auth.notice.SysUserNotificationSettingRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.impl.UserNotificationPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserNotificationPreferenceServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationPreferenceServiceImplTest {

    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysUserNotificationSettingRepository sysUserNotificationSettingRepository;

    private UserNotificationPreferenceServiceImpl preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new UserNotificationPreferenceServiceImpl(
                sysUserRepository,
                sysUserNotificationSettingRepository
        );
    }

    // ========== initializeDefaultSettings ==========

    @Test
    void initializeDefaultSettingsShouldCreateAllEnabledRecord() {
        Long userId = 1L;
        SysUser user = buildActiveUser(userId);

        when(sysUserRepository.getById(userId)).thenReturn(user);
        when(sysUserNotificationSettingRepository.findByUserId(userId)).thenReturn(null);
        when(sysUserNotificationSettingRepository.save(any(SysUserNotificationSetting.class))).thenReturn(true);

        preferenceService.initializeDefaultSettings(userId);

        ArgumentCaptor<SysUserNotificationSetting> captor = ArgumentCaptor.forClass(SysUserNotificationSetting.class);
        verify(sysUserNotificationSettingRepository).save(captor.capture());
        SysUserNotificationSetting saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(1, saved.getCommentNoticeEnabled());
        assertEquals(1, saved.getLikeNoticeEnabled());
        assertEquals(1, saved.getCollectNoticeEnabled());
        assertEquals(1, saved.getFollowNoticeEnabled());
        assertEquals(1, saved.getPrivateChatNoticeEnabled());
        assertEquals(1, saved.getMentionNoticeEnabled());
        assertEquals(1, saved.getChannelAnnouncementEnabled());
        assertEquals(1, saved.getSystemNoticeEnabled());
        assertEquals(1, saved.getAiTaskNoticeEnabled());
    }

    @Test
    void initializeDefaultSettingsShouldSkipWhenExists() {
        Long userId = 1L;
        SysUser user = buildActiveUser(userId);

        when(sysUserRepository.getById(userId)).thenReturn(user);
        when(sysUserNotificationSettingRepository.findByUserId(userId))
                .thenReturn(buildDefaultSetting(userId));

        preferenceService.initializeDefaultSettings(userId);

        verify(sysUserNotificationSettingRepository, never()).save(any());
    }

    // ========== isNotificationEnabled ==========

    @Test
    void isNotificationEnabledShouldReturnTrueByDefault() {
        Long userId = 1L;

        // getOrCreateSettings calls findByUserId 3 times:
        // 1st: getOrCreateSettings initial check -> null
        // 2nd: initializeDefaultSettings check -> null (not yet saved)
        // 3rd: getOrCreateSettings re-fetch after init -> default setting
        when(sysUserNotificationSettingRepository.findByUserId(userId))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(buildDefaultSetting(userId));
        when(sysUserRepository.getById(userId)).thenReturn(buildActiveUser(userId));
        when(sysUserNotificationSettingRepository.save(any(SysUserNotificationSetting.class))).thenReturn(true);

        boolean enabled = preferenceService.isNotificationEnabled(userId, NotificationTypeEnum.COMMENT_ME);

        assertTrue(enabled);
    }

    // ========== updateSettings ==========

    @Test
    void updateSettingsShouldPersistChanges() {
        Long userId = 1L;
        SysUserNotificationSetting existingSetting = buildDefaultSetting(userId);

        when(sysUserNotificationSettingRepository.findByUserId(userId)).thenReturn(existingSetting);
        when(sysUserNotificationSettingRepository.updateById(any(SysUserNotificationSetting.class))).thenReturn(true);

        preferenceService.updateSettings(userId,
                Map.of(NotificationTypeEnum.COMMENT_ME, false, NotificationTypeEnum.LIKE_ME, false));

        ArgumentCaptor<SysUserNotificationSetting> captor = ArgumentCaptor.forClass(SysUserNotificationSetting.class);
        verify(sysUserNotificationSettingRepository).updateById(captor.capture());
        SysUserNotificationSetting updated = captor.getValue();
        assertEquals(0, updated.getCommentNoticeEnabled());
        assertEquals(0, updated.getLikeNoticeEnabled());
        // Other fields should remain enabled
        assertEquals(1, updated.getCollectNoticeEnabled());
    }

    // ========== getOrCreateSettings ==========

    @Test
    void getOrCreateSettingsShouldAutoInitialize() {
        Long userId = 1L;
        SysUser activeUser = buildActiveUser(userId);

        // getOrCreateSettings calls findByUserId 3 times:
        // 1st: initial check -> null (triggers initializeDefaultSettings)
        // 2nd: initializeDefaultSettings guard check -> null
        // 3rd: re-fetch after save -> default setting
        when(sysUserNotificationSettingRepository.findByUserId(userId))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(buildDefaultSetting(userId));
        when(sysUserRepository.getById(userId)).thenReturn(activeUser);
        when(sysUserNotificationSettingRepository.save(any(SysUserNotificationSetting.class))).thenReturn(true);

        SysUserNotificationSetting result = preferenceService.getOrCreateSettings(userId);

        assertNotNull(result);
        verify(sysUserNotificationSettingRepository).save(any(SysUserNotificationSetting.class));
    }

    // ========== Helper methods ==========

    private SysUser buildActiveUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        user.setDeletedFlag(0);
        return user;
    }

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

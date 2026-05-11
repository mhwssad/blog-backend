package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.convert.UserProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileUpdateRequest;
import com.cybzacg.blogbackend.module.auth.account.model.user.UserProfileVO;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserProfileServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private UserProfileModelConvert userProfileModelConvert;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(sysUserRepository, userProfileModelConvert, passwordEncoder);
    }

    @Test
    void getProfileShouldReturnDesensitizedContactInfo() {
        SysUser user = user();
        UserProfileVO converted = new UserProfileVO();
        converted.setId(user.getId());
        converted.setUsername(user.getUsername());
        converted.setEmail(user.getEmail());
        converted.setPhone(user.getPhone());

        when(sysUserRepository.getById(1L)).thenReturn(user);
        when(userProfileModelConvert.toUserProfileVO(user)).thenReturn(converted);

        UserProfileVO result = service.getProfile(1L);

        assertEquals("t***@example.com", result.getEmail());
        assertEquals("138****5678", result.getPhone());
        verify(userProfileModelConvert).toUserProfileVO(user);
    }

    @Test
    void updateProfileShouldUseConvertUpdateAndReturnFreshProfile() {
        SysUser existing = user();
        SysUser updated = user();
        updated.setNickname("新昵称");
        updated.setBio("新的简介");

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setNickname("新昵称");
        request.setBio("新的简介");
        request.setWebsite("https://example.com");

        UserProfileVO converted = new UserProfileVO();
        converted.setId(existing.getId());
        converted.setNickname("新昵称");
        converted.setEmail(updated.getEmail());
        converted.setPhone(updated.getPhone());

        when(sysUserRepository.getById(1L)).thenReturn(existing, updated);
        doAnswer(invocation -> {
            UserProfileUpdateRequest source = invocation.getArgument(0);
            SysUser target = invocation.getArgument(1);
            target.setNickname(source.getNickname());
            target.setBio(source.getBio());
            target.setWebsite(source.getWebsite());
            return null;
        }).when(userProfileModelConvert).updateProfile(request, existing);
        when(userProfileModelConvert.toUserProfileVO(updated)).thenReturn(converted);

        UserProfileVO result = service.updateProfile(1L, request);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserRepository).updateById(userCaptor.capture());
        assertEquals("新昵称", userCaptor.getValue().getNickname());
        assertEquals("新的简介", userCaptor.getValue().getBio());
        assertEquals("https://example.com", userCaptor.getValue().getWebsite());
        assertEquals("t***@example.com", result.getEmail());
        assertEquals("138****5678", result.getPhone());
    }

    @Test
    void updateProfileShouldRejectMissingUser() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        when(sysUserRepository.getById(404L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateProfile(404L, request));

        verify(userProfileModelConvert, never()).updateProfile(any(), any());
        verify(sysUserRepository, never()).updateById(any());
    }

    @Test
    void changePasswordShouldSucceedWithCorrectOldPassword() {
        SysUser user = user();
        user.setPassword("oldEncoded");
        when(sysUserRepository.getById(1L)).thenReturn(user);
        when(passwordEncoder.matches("OldPass123", "oldEncoded")).thenReturn(true);
        when(passwordEncoder.matches("NewPass456", "oldEncoded")).thenReturn(false);
        when(passwordEncoder.encode("NewPass456")).thenReturn("newEncoded");

        service.changePassword(1L, "OldPass123", "NewPass456");

        assertEquals("newEncoded", user.getPassword());
        verify(sysUserRepository).updateById(user);
    }

    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        SysUser user = user();
        user.setPassword("oldEncoded");
        when(sysUserRepository.getById(1L)).thenReturn(user);
        when(passwordEncoder.matches("WrongPass", "oldEncoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changePassword(1L, "WrongPass", "NewPass456"));
        assertEquals(ResultErrorCode.OLD_PASSWORD_MISMATCH.getCode(), ex.getCode());
        verify(sysUserRepository, never()).updateById(any());
    }

    @Test
    void changePasswordShouldRejectNonexistentUser() {
        when(sysUserRepository.getById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changePassword(999L, "OldPass123", "NewPass456"));
        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("tester");
        user.setNickname("Tester");
        user.setEmail("tester@example.com");
        user.setPhone("13812345678");
        user.setBio("hello");
        user.setWebsite("https://old.example.com");
        return user;
    }
}

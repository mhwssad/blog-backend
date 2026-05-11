package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysRoleRepository;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminVerifierTest {
    @Mock
    private SysRoleRepository sysRoleRepository;

    private SuperAdminVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new SuperAdminVerifier(sysRoleRepository);
    }

    @Test
    void isSuperAdminShouldReturnTrueForSuperAdminRole() {
        when(sysRoleRepository.findRoleCodesByUserId(1L)).thenReturn(List.of("admin", "author"));

        assertTrue(verifier.isSuperAdmin(1L));
    }

    @Test
    void isSuperAdminShouldReturnFalseForNonSuperAdmin() {
        when(sysRoleRepository.findRoleCodesByUserId(2L)).thenReturn(List.of("author", "user"));

        assertFalse(verifier.isSuperAdmin(2L));
    }

    @Test
    void requireSuperAdminShouldThrowWhenNotSuperAdmin() {
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(2L)) {
            when(sysRoleRepository.findRoleCodesByUserId(2L)).thenReturn(List.of("author"));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> verifier.requireSuperAdmin());

            assertEquals(ResultErrorCode.NOT_SUPER_ADMIN.getCode(), exception.getCode());
        }
    }

    @Test
    void requireSuperAdminShouldPassWhenIsSuperAdmin() {
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysRoleRepository.findRoleCodesByUserId(1L)).thenReturn(List.of("admin"));

            assertDoesNotThrow(() -> verifier.requireSuperAdmin());
        }
    }
}

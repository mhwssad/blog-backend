package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.dto.domain.ai.AiChannelAccount;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountVO;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelAccountRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiChannelAccountAdminServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChannelAccountAdminServiceImplTest {

    @Mock private AiChannelAccountRepository aiChannelAccountRepository;
    @Mock private AiChannelConfigRepository aiChannelConfigRepository;
    @Mock private AiModelConvert aiModelConvert;
    @Mock private SysAuditLogService sysAuditLogService;
    @Mock private TwoFactorService twoFactorService;
    @Mock private SuperAdminVerifier superAdminVerifier;

    private AiChannelAccountAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiChannelAccountAdminServiceImpl(
                aiChannelAccountRepository, aiChannelConfigRepository, aiModelConvert,
                sysAuditLogService, twoFactorService, superAdminVerifier);
    }

    @Test
    void createAccountShouldSaveAndReturnVO() {
        when(aiChannelConfigRepository.getById(1L)).thenReturn(new AiChannelConfig());

        AiChannelAccountSaveRequest request = new AiChannelAccountSaveRequest();
        request.setAccountName("test-account");
        request.setProvider("deepseek");
        request.setModelName("deepseek-chat");
        request.setApiBaseUrl("https://api.deepseek.com");
        request.setApiKeyEncrypted("sk-test-key-12345678");

        AiChannelAccount entity = new AiChannelAccount();
        entity.setId(1L);
        entity.setAccountName("test-account");
        entity.setApiKeyEncrypted("sk-test-key-12345678");
        when(aiModelConvert.toChannelAccount(request)).thenReturn(entity);

        AiChannelAccountVO vo = new AiChannelAccountVO();
        vo.setId(1L);
        vo.setApiKeyEncrypted("sk-test-key-12345678");
        when(aiModelConvert.toChannelAccountVO(any())).thenReturn(vo);

        when(aiChannelAccountRepository.save(any())).thenReturn(true);

        AiChannelAccountVO result = service.createAccount(1L, request, 1L);

        assertNotNull(result);
        verify(aiChannelAccountRepository).save(any());
        verify(sysAuditLogService).record(any());
    }

    @Test
    void createAccountShouldRejectMissingChannel() {
        when(aiChannelConfigRepository.getById(999L)).thenReturn(null);

        AiChannelAccountSaveRequest request = new AiChannelAccountSaveRequest();
        assertThrows(BusinessException.class, () -> service.createAccount(999L, request, 1L));
    }

    @Test
    void updateAccountShouldPreserveMaskedApiKey() {
        when(aiChannelConfigRepository.getById(1L)).thenReturn(new AiChannelConfig());

        AiChannelAccount existing = new AiChannelAccount();
        existing.setId(1L);
        existing.setChannelConfigId(1L);
        existing.setApiKeyEncrypted("sk-real-key-12345678");
        when(aiChannelAccountRepository.getById(1L)).thenReturn(existing);

        AiChannelAccountVO vo = new AiChannelAccountVO();
        vo.setApiKeyEncrypted("sk-****5678");
        when(aiModelConvert.toChannelAccountVO(any())).thenReturn(vo);

        AiChannelAccountSaveRequest request = new AiChannelAccountSaveRequest();
        request.setAccountName("updated");
        request.setApiKeyEncrypted("sk-****5678");

        service.updateAccount(1L, 1L, request, 1L);

        // masked key should not trigger MFA
        verify(superAdminVerifier, never()).requireSuperAdmin(any());
    }

    @Test
    void updateAccountShouldRequireMfaForApiKeyChange() {
        when(aiChannelConfigRepository.getById(1L)).thenReturn(new AiChannelConfig());

        AiChannelAccount existing = new AiChannelAccount();
        existing.setId(1L);
        existing.setChannelConfigId(1L);
        existing.setApiKeyEncrypted("sk-old-key-12345678");
        when(aiChannelAccountRepository.getById(1L)).thenReturn(existing);

        AiChannelAccountVO vo = new AiChannelAccountVO();
        when(aiModelConvert.toChannelAccountVO(any())).thenReturn(vo);

        AiChannelAccountSaveRequest request = new AiChannelAccountSaveRequest();
        request.setApiKeyEncrypted("sk-new-key-12345678");
        request.setMfaTicket("valid-ticket");

        when(twoFactorService.validateTicket("valid-ticket", 1L)).thenReturn(true);

        service.updateAccount(1L, 1L, request, 1L);

        verify(superAdminVerifier).requireSuperAdmin(1L);
        verify(twoFactorService).validateTicket("valid-ticket", 1L);
    }

    @Test
    void deleteAccountShouldRemove() {
        when(aiChannelConfigRepository.getById(1L)).thenReturn(new AiChannelConfig());

        AiChannelAccount existing = new AiChannelAccount();
        existing.setId(1L);
        existing.setChannelConfigId(1L);
        when(aiChannelAccountRepository.getById(1L)).thenReturn(existing);

        service.deleteAccount(1L, 1L, 1L);

        verify(aiChannelAccountRepository).removeById(1L);
    }

    @Test
    void getAccountShouldRejectWrongChannel() {
        when(aiChannelConfigRepository.getById(1L)).thenReturn(new AiChannelConfig());

        AiChannelAccount account = new AiChannelAccount();
        account.setId(1L);
        account.setChannelConfigId(2L);
        when(aiChannelAccountRepository.getById(1L)).thenReturn(account);

        assertThrows(BusinessException.class, () -> service.getAccount(1L, 1L));
    }
}

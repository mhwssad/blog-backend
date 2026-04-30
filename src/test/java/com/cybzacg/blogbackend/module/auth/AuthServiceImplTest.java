package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.authentication.EmailCodeAuthenticationToken;
import com.cybzacg.blogbackend.module.auth.account.convert.AuthModelMapper;
import com.cybzacg.blogbackend.module.auth.account.model.*;
import com.cybzacg.blogbackend.module.auth.config.repository.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.service.impl.AuthServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private TokenManager tokenManager;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private SysConfigRepository sysConfigRepository;
    @Mock
    private AuthModelMapper authModelMapper;
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private MailProperties mailProperties;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authenticationManager,
                tokenManager,
                sysUserRepository,
                sysRoleRepository,
                sysMenuRepository,
                sysConfigRepository,
                authModelMapper,
                redisOperator,
                javaMailSender,
                mailProperties,
                passwordEncoder,
                userNotificationPreferenceService,
                eventPublisher
        );
    }

    @Test
    void loginShouldTrimAccountAndUpdateLoginInfo() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("  demo@example.com  ");
        request.setPassword("secret");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);
        Authentication authentication = mock(Authentication.class);
        AuthenticationToken token = AuthenticationToken.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(sysUserRepository.findByUsername("demo@example.com")).thenReturn(user);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenManager.generateToken(authentication)).thenReturn(token);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.getUserId(authentication)).thenReturn(7L);

            AuthenticationToken result = authService.login(request, "127.0.0.1");

            ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(authenticationCaptor.capture());
            assertEquals("demo@example.com", authenticationCaptor.getValue().getPrincipal());
            assertEquals("secret", authenticationCaptor.getValue().getCredentials());
            assertEquals(token, result);
            verify(redisOperator).delete(RedisKeyUtils.build(
                    AuthConstants.LOGIN_FAIL_COUNT_PREFIX,
                    RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_SCOPE_USER, 7L)));
            verify(redisOperator).delete(RedisKeyUtils.build(
                    AuthConstants.LOGIN_FAIL_LOCK_PREFIX,
                    RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_SCOPE_USER, 7L)));
            verify(sysUserRepository).updateLoginInfo(7L, "127.0.0.1");
        }
    }

    @Test
    void loginShouldRejectLockedAccountBeforeAuthenticating() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("demo");
        request.setPassword("secret");

        SysUser user = new SysUser();
        user.setId(9L);
        user.setStatus(1);
        String lockKey = RedisKeyUtils.build(
                AuthConstants.LOGIN_FAIL_LOCK_PREFIX,
                RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_SCOPE_USER, 9L));

        when(sysUserRepository.findByUsername("demo")).thenReturn(user);
        when(redisOperator.hasKey(lockKey)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ACCOUNT_LOCKED.getCode(), exception.getCode());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void loginShouldLockAccountAfterConfiguredMaxFailures() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("demo");
        request.setPassword("bad-secret");

        SysUser user = new SysUser();
        user.setId(9L);
        user.setStatus(1);
        String countKey = RedisKeyUtils.build(
                AuthConstants.LOGIN_FAIL_COUNT_PREFIX,
                RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_SCOPE_USER, 9L));
        String lockKey = RedisKeyUtils.build(
                AuthConstants.LOGIN_FAIL_LOCK_PREFIX,
                RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_SCOPE_USER, 9L));

        when(sysUserRepository.findByUsername("demo")).thenReturn(user);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));
        when(sysConfigRepository.findByConfigKey(ConfigConstants.AUTH_LOGIN_FAIL_MAX_ATTEMPTS_KEY))
                .thenReturn(config(ConfigConstants.AUTH_LOGIN_FAIL_MAX_ATTEMPTS_KEY, "3"));
        when(sysConfigRepository.findByConfigKey(ConfigConstants.AUTH_LOGIN_FAIL_LOCK_MINUTES_KEY))
                .thenReturn(config(ConfigConstants.AUTH_LOGIN_FAIL_LOCK_MINUTES_KEY, "15"));
        when(redisOperator.increment(countKey)).thenReturn(3L);

        LockedException exception = assertThrows(LockedException.class,
                () -> authService.login(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ACCOUNT_LOCKED.getMessage(), exception.getMessage());
        verify(redisOperator).set(lockKey, "1", Duration.ofMinutes(15));
        verify(redisOperator).delete(countKey);
    }

    @Test
    void registerShouldRejectDuplicateUsernameBeforeSaving() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("  demo  ");
        request.setPassword("secret");
        request.setEmail("demo@example.com");
        request.setPhone("13800138000");

        when(sysUserRepository.existsActiveByIdentity("demo")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(sysUserRepository, never()).save(any(SysUser.class));
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void registerShouldNormalizeIdentityAndPersistDefaultFlags() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("  demo  ");
        request.setPassword("secret");
        request.setNickname("Demo User");
        request.setEmail("  Demo@Example.com  ");
        request.setPhone(" 13800138000 ");

        SysUser mappedUser = new SysUser();
        mappedUser.setNickname("Demo User");

        Authentication authentication = mock(Authentication.class);
        AuthenticationToken token = AuthenticationToken.builder()
                .accessToken("register-access-token")
                .refreshToken("register-refresh-token")
                .build();

        when(sysUserRepository.existsActiveByIdentity("demo")).thenReturn(false);
        when(sysUserRepository.existsActiveByIdentity("demo@example.com")).thenReturn(false);
        when(sysUserRepository.existsActiveByIdentity("13800138000")).thenReturn(false);
        when(authModelMapper.toRegisterUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenManager.generateToken(authentication)).thenReturn(token);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.getUserId(authentication)).thenReturn(18L);

            AuthenticationToken result = authService.register(request, "127.0.0.1");

            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(sysUserRepository).save(userCaptor.capture());
            SysUser savedUser = userCaptor.getValue();
            assertEquals("demo", savedUser.getUsername());
            assertEquals("encoded-secret", savedUser.getPassword());
            assertEquals("Demo User", savedUser.getNickname());
            assertEquals("demo@example.com", savedUser.getEmail());
            assertEquals("13800138000", savedUser.getPhone());
            assertEquals(Integer.valueOf(1), savedUser.getStatus());
            assertEquals(Integer.valueOf(0), savedUser.getDeletedFlag());
            verify(sysUserRepository).updateLoginInfo(18L, "127.0.0.1");
            assertEquals(token, result);
        }
    }

    @Test
    void registerShouldTranslateDuplicateKeyConflictToUsernameExistsMessage() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("demo");
        request.setPassword("secret");
        request.setEmail("demo@example.com");
        request.setPhone("13800138000");

        SysUser mappedUser = new SysUser();

        when(sysUserRepository.existsActiveByIdentity("demo")).thenReturn(false);
        when(sysUserRepository.existsActiveByIdentity("demo@example.com")).thenReturn(false);
        when(sysUserRepository.existsActiveByIdentity("13800138000")).thenReturn(false);
        when(authModelMapper.toRegisterUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        doThrow(new DuplicateKeyException("uk_sys_user_active_username")).when(sysUserRepository).save(mappedUser);
        when(sysUserRepository.existsActiveByField("username", "demo")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void emailLoginShouldNormalizeEmailAndTrimCode() {
        AuthEmailLoginRequest request = new AuthEmailLoginRequest();
        request.setEmail("  Demo@Example.com  ");
        request.setCode(" 9527 ");

        Authentication authentication = mock(Authentication.class);
        AuthenticationToken token = AuthenticationToken.builder()
                .accessToken("email-access-token")
                .refreshToken("email-refresh-token")
                .build();

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenManager.generateToken(authentication)).thenReturn(token);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.getUserId(authentication)).thenReturn(9L);

            AuthenticationToken result = authService.emailLogin(request, "192.168.1.9");

            ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(authenticationCaptor.capture());
            EmailCodeAuthenticationToken authRequest =
                    (EmailCodeAuthenticationToken) authenticationCaptor.getValue();
            assertEquals("demo@example.com", authRequest.getPrincipal());
            assertEquals("9527", authRequest.getCredentials());
            assertEquals(token, result);
            verify(sysUserRepository).updateLoginInfo(9L, "192.168.1.9");
        }
    }

    @Test
    void sendEmailLoginCodeShouldSendMailAndCacheCodeForNormalizedEmail() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("  Demo@Example.com  ");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);

        when(sysUserRepository.findByEmail("demo@example.com")).thenReturn(user);
        when(redisOperator.setIfAbsent(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, "demo@example.com")),
                eq("1"),
                eq(AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL))).thenReturn(true);
        when(mailProperties.getUsername()).thenReturn("noreply@example.com");

        authService.sendEmailLoginCode(request);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage message = mailCaptor.getValue();
        assertEquals("noreply@example.com", message.getFrom());
        assertEquals(AuthConstants.EMAIL_LOGIN_SUBJECT, message.getSubject());
        assertEquals("demo@example.com", message.getTo()[0]);
        assertNotNull(message.getText());
        assertTrue(message.getText().startsWith("您的登录验证码为："));
        String code = message.getText().replace("您的登录验证码为：", "").replace("，5分钟内有效。", "");
        assertTrue(code.matches("\\d{6}"));

        verify(redisOperator).set(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, "demo@example.com")),
                eq(code),
                eq(AuthConstants.EMAIL_LOGIN_CODE_TTL)
        );
    }

    @Test
    void refreshShouldRejectInvalidRefreshToken() {
        AuthRefreshRequest request = new AuthRefreshRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(tokenManager.validateRefreshToken("invalid-refresh-token")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refresh(request));

        assertEquals(ResultErrorCode.INVALID_TOKEN.getCode(), exception.getCode());
        verify(tokenManager, never()).refreshToken(any());
    }

    @Test
    void getCurrentUserShouldLoadRolesAndPermissions() {
        Authentication authentication = mock(Authentication.class);
        SysUser user = new SysUser();
        user.setId(12L);
        user.setDeletedFlag(0);

        List<String> roleCodes = List.of("admin");
        List<String> permissions = List.of("sys:user:query", "sys:role:assign-menu");
        AuthUserInfo expected = AuthUserInfo.builder()
                .id(12L)
                .username("demo")
                .roles(roleCodes)
                .permissions(permissions)
                .build();

        when(sysUserRepository.getById(12L)).thenReturn(user);
        when(sysRoleRepository.findRoleCodesByUserId(12L)).thenReturn(roleCodes);
        when(sysMenuRepository.findPermissionsByUserId(12L)).thenReturn(permissions);
        when(authModelMapper.toAuthUserInfo(user, roleCodes, permissions)).thenReturn(expected);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockAuthentication(authentication, 12L, null)) {
            AuthUserInfo result = authService.getCurrentUser();
            assertEquals(expected, result);
        }
    }

    @Test
    void getCurrentUserMenusShouldFallbackToUsernameAndBuildTree() {
        Authentication authentication = mock(Authentication.class);
        SysUser user = new SysUser();
        user.setId(21L);
        user.setDeletedFlag(0);

        SysMenu root = new SysMenu();
        root.setId(1L);
        root.setParentId(MenuConstants.ROOT_PARENT_ID);
        root.setType(MenuConstants.TYPE_CATALOG);

        SysMenu child = new SysMenu();
        child.setId(2L);
        child.setParentId(1L);
        child.setType(MenuConstants.TYPE_MENU);

        SysMenu button = new SysMenu();
        button.setId(3L);
        button.setParentId(2L);
        button.setType(MenuConstants.TYPE_BUTTON);

        AuthMenuInfo rootInfo = AuthMenuInfo.builder()
                .id(1L)
                .parentId(MenuConstants.ROOT_PARENT_ID)
                .children(new ArrayList<>())
                .build();
        AuthMenuInfo childInfo = AuthMenuInfo.builder()
                .id(2L)
                .parentId(1L)
                .children(new ArrayList<>())
                .build();

        when(sysUserRepository.findByUsername("demo")).thenReturn(user);
        when(sysMenuRepository.findMenusByUserId(21L)).thenReturn(List.of(root, child, button));
        when(authModelMapper.toAuthMenuInfo(root)).thenReturn(rootInfo);
        when(authModelMapper.toAuthMenuInfo(child)).thenReturn(childInfo);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockAuthentication(authentication, null, "demo")) {
            List<AuthMenuInfo> result = authService.getCurrentUserMenus();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(1, result.get(0).getChildren().size());
            assertEquals(2L, result.get(0).getChildren().get(0).getId());
        }
    }

    private SysConfig config(String key, String value) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}

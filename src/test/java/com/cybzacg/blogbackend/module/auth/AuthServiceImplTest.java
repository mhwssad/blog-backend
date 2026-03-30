package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.authentication.EmailCodeAuthenticationToken;
import com.cybzacg.blogbackend.module.auth.convert.AuthModelMapper;
import com.cybzacg.blogbackend.module.auth.model.AuthEmailCodeRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthEmailLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthRefreshRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthRegisterRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthUserInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.auth.service.impl.AuthServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private TokenManager tokenManager;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysMenuService sysMenuService;
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
    private LambdaQueryChainWrapper<SysUser> userQuery;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authenticationManager,
                tokenManager,
                sysUserService,
                sysRoleService,
                sysMenuService,
                authModelMapper,
                redisOperator,
                javaMailSender,
                mailProperties,
                passwordEncoder
        );
    }

    @Test
    void loginShouldTrimAccountAndUpdateLoginInfo() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("  demo@example.com  ");
        request.setPassword("secret");

        Authentication authentication = mock(Authentication.class);
        AuthenticationToken token = AuthenticationToken.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

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
            verify(sysUserService).updateLoginInfo(7L, "127.0.0.1");
        }
    }

    @Test
    void registerShouldRejectDuplicateUsernameBeforeSaving() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("  demo  ");
        request.setPassword("secret");
        request.setEmail("demo@example.com");
        request.setPhone("13800138000");

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.eq(any(), any())).thenReturn(userQuery);
        when(userQuery.and(any())).thenReturn(userQuery);
        when(userQuery.exists()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(sysUserService, never()).save(any(SysUser.class));
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

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.eq(any(), any())).thenReturn(userQuery);
        when(userQuery.and(any())).thenReturn(userQuery);
        when(userQuery.exists()).thenReturn(false, false, false);
        when(authModelMapper.toRegisterUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenManager.generateToken(authentication)).thenReturn(token);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.getUserId(authentication)).thenReturn(18L);

            AuthenticationToken result = authService.register(request, "127.0.0.1");

            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(sysUserService).save(userCaptor.capture());
            SysUser savedUser = userCaptor.getValue();
            assertEquals("demo", savedUser.getUsername());
            assertEquals("encoded-secret", savedUser.getPassword());
            assertEquals("Demo User", savedUser.getNickname());
            assertEquals("demo@example.com", savedUser.getEmail());
            assertEquals("13800138000", savedUser.getPhone());
            assertEquals(Integer.valueOf(1), savedUser.getStatus());
            assertEquals(Integer.valueOf(0), savedUser.getDeletedFlag());

            ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(authenticationCaptor.capture());
            assertEquals("demo", authenticationCaptor.getValue().getPrincipal());
            assertEquals("secret", authenticationCaptor.getValue().getCredentials());
            verify(sysUserService).updateLoginInfo(18L, "127.0.0.1");
            assertEquals(token, result);
        }
    }

    @Test
    void registerShouldRejectDuplicateEmailBeforeSaving() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("demo");
        request.setPassword("secret");
        request.setEmail("  Demo@Example.com ");

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.eq(any(), any())).thenReturn(userQuery);
        when(userQuery.and(any())).thenReturn(userQuery);
        when(userQuery.exists()).thenReturn(false, true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("邮箱已存在", exception.getMessage());
        verify(sysUserService, never()).save(any(SysUser.class));
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void registerShouldRejectDuplicatePhoneBeforeSaving() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("demo");
        request.setPassword("secret");
        request.setEmail("demo@example.com");
        request.setPhone(" 13800138000 ");

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.eq(any(), any())).thenReturn(userQuery);
        when(userQuery.and(any())).thenReturn(userQuery);
        when(userQuery.exists()).thenReturn(false, false, true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, "127.0.0.1"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("手机号已存在", exception.getMessage());
        verify(sysUserService, never()).save(any(SysUser.class));
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
            verify(sysUserService).updateLoginInfo(9L, "192.168.1.9");
        }
    }

    @Test
    void sendEmailLoginCodeShouldSendMailAndCacheCodeForNormalizedEmail() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("  Demo@Example.com  ");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);

        when(sysUserService.getByEmail("demo@example.com")).thenReturn(user);
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
    void sendEmailLoginCodeShouldRejectDisabledUser() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("demo@example.com");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(0);

        when(sysUserService.getByEmail("demo@example.com")).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.sendEmailLoginCode(request));

        assertEquals(ResultErrorCode.ACCOUNT_DISABLED.getCode(), exception.getCode());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(redisOperator, never()).set(any(), any(), any());
    }

    @Test
    void sendEmailLoginCodeShouldNotCacheCodeWhenMailSendFails() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("demo@example.com");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);

        when(sysUserService.getByEmail("demo@example.com")).thenReturn(user);
        when(redisOperator.setIfAbsent(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, "demo@example.com")),
                eq("1"),
                eq(AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL))).thenReturn(true);
        when(mailProperties.getUsername()).thenReturn("noreply@example.com");
        doThrow(new RuntimeException("mail server down")).when(javaMailSender).send(any(SimpleMailMessage.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.sendEmailLoginCode(request));

        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED.getCode(), exception.getCode());
        verify(redisOperator, never()).set(any(), any(), any());
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
    void logoutShouldIgnoreBlankToken() {
        authService.logout("   ");

        verify(tokenManager, never()).invalidateToken(any());
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

        when(sysUserService.getById(12L)).thenReturn(user);
        when(sysRoleService.listRoleCodesByUserId(12L)).thenReturn(roleCodes);
        when(sysMenuService.listPermissionsByUserId(12L)).thenReturn(permissions);
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

        when(sysUserService.getByUsername("demo")).thenReturn(user);
        when(sysMenuService.listMenusByUserId(21L)).thenReturn(List.of(root, child, button));
        when(authModelMapper.toAuthMenuInfo(root)).thenReturn(rootInfo);
        when(authModelMapper.toAuthMenuInfo(child)).thenReturn(childInfo);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockAuthentication(authentication, null, "demo")) {
            List<AuthMenuInfo> result = authService.getCurrentUserMenus();

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(1, result.get(0).getChildren().size());
            assertEquals(2L, result.get(0).getChildren().get(0).getId());
        }

        verify(authModelMapper, never()).toAuthMenuInfo(button);
    }

    @Test
    void sendEmailLoginCodeShouldRejectWhenRateLimited() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("demo@example.com");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);

        when(sysUserService.getByEmail("demo@example.com")).thenReturn(user);
        when(redisOperator.setIfAbsent(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, "demo@example.com")),
                eq("1"),
                eq(AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.sendEmailLoginCode(request));

        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_RATE_LIMITED.getCode(), exception.getCode());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
        verify(redisOperator, never()).set(any(), any(), any());
    }

    @Test
    void sendEmailLoginCodeShouldSetRateLimitKeyOnFirstRequest() {
        AuthEmailCodeRequest request = new AuthEmailCodeRequest();
        request.setEmail("demo@example.com");

        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);

        when(sysUserService.getByEmail("demo@example.com")).thenReturn(user);
        when(redisOperator.setIfAbsent(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, "demo@example.com")),
                eq("1"),
                eq(AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL))).thenReturn(true);
        when(mailProperties.getUsername()).thenReturn("noreply@example.com");

        authService.sendEmailLoginCode(request);

        verify(redisOperator).setIfAbsent(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, "demo@example.com")),
                eq("1"),
                eq(AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL));
        verify(javaMailSender).send(any(SimpleMailMessage.class));
        verify(redisOperator).set(
                eq(RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, "demo@example.com")),
                any(),
                eq(AuthConstants.EMAIL_LOGIN_CODE_TTL));
    }
}

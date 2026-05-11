package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.common.email.EmailService;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.auth.SysMenu;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.config.SysConfig;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.auth.config.SysConfigRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysMenuRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysRoleRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.auth.account.authentication.EmailCodeAuthenticationToken;
import com.cybzacg.blogbackend.module.auth.account.convert.AuthModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.*;
import com.cybzacg.blogbackend.module.auth.account.service.AuthService;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PasswordUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

/**
 * 认证服务实现。
 *
 * <p>负责账号登录、注册、邮箱验证码登录、令牌刷新以及当前登录态信息装配。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String USER_FIELD_USERNAME = "username";
    private static final String USER_FIELD_EMAIL = "email";
    private static final String USER_FIELD_PHONE = "phone";

    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;
    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysConfigRepository sysConfigRepository;
    private final AuthModelConvert authModelConvert;
    private final RedisOperator redisOperator;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserNotificationPreferenceService userNotificationPreferenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 账号密码登录，含失败锁定与登录信息记录。
     *
     * @param request 登录请求（用户名 + 密码）
     * @param loginIp 登录来源 IP
     * @return 认证令牌（accessToken + refreshToken）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken login(AuthLoginRequest request, String loginIp) {
        String account = StrUtils.trim(request.getUsername());
        SysUser loginUser = sysUserRepository.findByUsername(account);
        ensureLoginNotLocked(account, loginUser);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(account, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw handleLoginFailure(account, loginUser, ex);
        }

        Long userId = SecurityUtils.getUserId(authentication);
        clearLoginFailureState(account, userId != null ? userId : loginUser != null ? loginUser.getId() : null);
        if (userId != null) {
            sysUserRepository.updateLoginInfo(userId, loginIp);
        }
        AuthenticationToken token = tokenManager.generateToken(authentication);
        if (userId != null) {
            eventPublisher.publishEvent(new XpAwardEvent(
                    userId, ExperienceSourceTypeEnum.DAILY_LOGIN.getValue(),
                    null, "daily_login:" + userId + ":" + java.time.LocalDate.now()));
        }
        return token;
    }

    /**
     * 用户注册，校验唯一性后创建账号并自动登录。
     *
     * @param request 注册请求（用户名、邮箱、手机号、密码）
     * @param loginIp 注册来源 IP
     * @return 认证令牌
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken register(AuthRegisterRequest request, String loginIp) {
        String username = StrUtils.trimToNull(request.getUsername());
        String email = StrUtils.trimToLowerCase(request.getEmail());
        String phone = StrUtils.trimToNull(request.getPhone());
        validateRegisterIdentity(username, "用户名已存在");
        validateRegisterIdentity(email, "邮箱已存在");
        validateRegisterIdentity(phone, "手机号已存在");
        PasswordUtils.validate(request.getPassword());

        SysUser user = authModelConvert.toRegisterUser(request);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(user.getNickname());
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setUserLevel(1);
        user.setExperiencePoints(0);
        user.setLevelUpdatedAt(null);
        user.setDeletedFlag(0);
        try {
            sysUserRepository.save(user);
        } catch (DuplicateKeyException ex) {
            throwRegisterDuplicateException(username, email, phone, ex);
        }
        userNotificationPreferenceService.initializeDefaultSettings(user.getId());

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.getPassword())
        );
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId != null) {
            sysUserRepository.updateLoginInfo(userId, loginIp);
        }
        return tokenManager.generateToken(authentication);
    }

    /**
     * 发送邮箱登录验证码，含频率限制。
     *
     * @param request 邮箱验证码请求（目标邮箱地址）
     */
    @Override
    public void sendEmailLoginCode(AuthEmailCodeRequest request) {
        String email = StrUtils.trimToLowerCase(request.getEmail());
        SysUser user = sysUserRepository.findByEmail(email);
        ExceptionThrowerCore.throwBusinessIfNull(user, ResultErrorCode.USER_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIfNot(Integer.valueOf(1).equals(user.getStatus()), ResultErrorCode.ACCOUNT_DISABLED);

        String rateLimitKey = RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_RATE_PREFIX, email);
        ExceptionThrowerCore.throwBusinessIfNot(
                redisOperator.setIfAbsent(rateLimitKey, "1", AuthConstants.EMAIL_LOGIN_CODE_RATE_TTL),
                ResultErrorCode.EMAIL_CAPTCHA_RATE_LIMITED);

        String code = generateEmailCode();
        emailService.sendTextEmail(email, AuthConstants.EMAIL_LOGIN_SUBJECT, buildEmailCodeContent(code));

        redisOperator.set(emailLoginCodeKey(email), code, AuthConstants.EMAIL_LOGIN_CODE_TTL);
    }

    /**
     * 邮箱验证码登录。
     *
     * @param request 邮箱登录请求（邮箱 + 验证码）
     * @param loginIp 登录来源 IP
     * @return 认证令牌
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken emailLogin(AuthEmailLoginRequest request, String loginIp) {
        String email = StrUtils.trimToLowerCase(request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                EmailCodeAuthenticationToken.unauthenticated(email, StrUtils.trim(request.getCode()))
        );
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId != null) {
            sysUserRepository.updateLoginInfo(userId, loginIp);
        }
        AuthenticationToken token = tokenManager.generateToken(authentication);
        if (userId != null) {
            eventPublisher.publishEvent(new XpAwardEvent(
                    userId, ExperienceSourceTypeEnum.DAILY_LOGIN.getValue(),
                    null, "daily_login:" + userId + ":" + java.time.LocalDate.now()));
        }
        return token;
    }

    /**
     * 使用 refreshToken 换发新的认证令牌。
     *
     * @param request 刷新令牌请求（refreshToken）
     * @return 新的认证令牌
     */
    @Override
    public AuthenticationToken refresh(AuthRefreshRequest request) {
        ExceptionThrowerCore.throwBusinessIfNot(tokenManager.validateRefreshToken(request.getRefreshToken()),
                ResultErrorCode.INVALID_TOKEN);
        return tokenManager.refreshToken(request.getRefreshToken());
    }

    /**
     * 注销当前会话，立即使令牌失效。
     */
    @Override
    public void logout(String token) {
        if (!StrUtils.hasText(token)) {
            return;
        }
        tokenManager.invalidateToken(token);
    }

    /**
     * 获取当前登录用户的基本信息、角色编码与权限列表。
     */
    @Override
    public AuthUserInfo getCurrentUser() {
        Authentication authentication = SecurityUtils.requireAuthentication();
        SysUser user = getCurrentSysUser(authentication);
        List<String> roleCodes = sysRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = buildCurrentPermissions(roleCodes, user.getId());
        return authModelConvert.toAuthUserInfo(user, roleCodes, permissions);
    }

    /**
     * 获取当前登录用户可见的菜单树（过滤按钮类型，仅保留目录和菜单）。
     */
    @Override
    public List<AuthMenuInfo> getCurrentUserMenus() {
        Authentication authentication = SecurityUtils.requireAuthentication();
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId == null) {
            SysUser user = getCurrentSysUser(authentication);
            userId = user.getId();
        }
        List<String> roleCodes = sysRoleRepository.findRoleCodesByUserId(userId);
        List<SysMenu> menus = isSuperAdmin(roleCodes)
                ? sysMenuRepository.findAllOrdered()
                : sysMenuRepository.findMenusByUserId(userId);
        return buildMenuTree(menus);
    }

    private SysUser getCurrentSysUser(Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        SysUser user = userId != null ? sysUserRepository.getById(userId) : null;
        if (user == null) {
            user = sysUserRepository.findByUsername(SecurityUtils.getUsername(authentication));
        }
        ExceptionThrowerCore.throwBusinessIf(user == null
                        || (!Integer.valueOf(0).equals(user.getDeletedFlag()) && user.getDeletedFlag() != null),
                ResultErrorCode.USER_NOT_FOUND);
        return user;
    }

    private List<AuthMenuInfo> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        Map<Long, AuthMenuInfo> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (MenuConstants.TYPE_BUTTON.equalsIgnoreCase(menu.getType())
                    && !AuthConstants.ALL_PERMISSION.equals(menu.getPerm())) {
                continue;
            }
            menuMap.put(menu.getId(), authModelConvert.toAuthMenuInfo(menu));
        }

        List<AuthMenuInfo> roots = new ArrayList<>();
        for (AuthMenuInfo menu : menuMap.values()) {
            AuthMenuInfo parent = menuMap.get(menu.getParentId());
            if (parent == null || MenuConstants.ROOT_PARENT_ID.equals(menu.getParentId())) {
                roots.add(menu);
                continue;
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(menu);
        }
        return roots;
    }

    /**
     * 超级管理员仅持有通配符 {@code *:*:*}，通过段匹配覆盖所有权限。
     */
    private List<String> buildCurrentPermissions(List<String> roleCodes, Long userId) {
        Set<String> dedup = new LinkedHashSet<>();
        if (isSuperAdmin(roleCodes)) {
            dedup.add(AuthConstants.ALL_PERMISSION);
        } else {
            List<String> permissions = sysMenuRepository.findPermissionsByUserId(userId);
            if (permissions != null) {
                permissions.stream()
                        .filter(StrUtils::hasText)
                        .forEach(dedup::add);
            }
        }
        return dedup.stream().toList();
    }

    private boolean isSuperAdmin(List<String> roleCodes) {
        if (roleCodes == null) {
            return false;
        }
        return roleCodes.stream()
                .filter(StrUtils::hasText)
                .map(this::normalizeRoleCode)
                .anyMatch(AuthConstants.SUPER_ADMIN_ROLE_CODE::equals);
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode.startsWith("ROLE_") ? roleCode.substring("ROLE_".length()) : roleCode;
    }

    private String generateEmailCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private String buildEmailCodeContent(String code) {
        return "您的登录验证码为：" + code + "，5分钟内有效。";
    }

    private String emailLoginCodeKey(String email) {
        return RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, email);
    }

    private void validateRegisterIdentity(String identity, String message) {
        if (!StrUtils.hasText(identity)) {
            return;
        }
        boolean exists = sysUserRepository.existsActiveByIdentity(identity);
        ExceptionThrowerCore.throwBusinessIf(exists, ResultErrorCode.ILLEGAL_ARGUMENT, message);
    }

    private void ensureLoginNotLocked(String account, SysUser loginUser) {
        if (loginUser != null && !Integer.valueOf(1).equals(loginUser.getStatus())) {
            return;
        }
        String lockKey = loginFailLockKey(account, loginUser != null ? loginUser.getId() : null);
        ExceptionThrowerCore.throwBusinessIf(redisOperator.hasKey(lockKey), ResultErrorCode.ACCOUNT_LOCKED);
    }

    private AuthenticationException handleLoginFailure(String account,
                                                       SysUser loginUser,
                                                       BadCredentialsException cause) {
        int maxAttempts = resolveIntConfig(
                ConfigConstants.AUTH_LOGIN_FAIL_MAX_ATTEMPTS_KEY,
                ConfigConstants.DEFAULT_AUTH_LOGIN_FAIL_MAX_ATTEMPTS,
                0);
        if (maxAttempts <= 0) {
            return cause;
        }

        Long userId = loginUser != null ? loginUser.getId() : null;
        String countKey = loginFailCountKey(account, userId);
        long failures = redisOperator.increment(countKey);
        Duration lockDuration = Duration.ofMinutes(resolveIntConfig(
                ConfigConstants.AUTH_LOGIN_FAIL_LOCK_MINUTES_KEY,
                ConfigConstants.DEFAULT_AUTH_LOGIN_FAIL_LOCK_MINUTES,
                1));
        if (failures == 1) {
            redisOperator.expire(countKey, lockDuration);
        }
        if (failures < maxAttempts) {
            return cause;
        }

        redisOperator.set(loginFailLockKey(account, userId), "1", lockDuration);
        redisOperator.delete(countKey);
        return new LockedException(ResultErrorCode.ACCOUNT_LOCKED.getMessage(), cause);
    }

    private void clearLoginFailureState(String account, Long userId) {
        redisOperator.delete(loginFailCountKey(account, userId));
        redisOperator.delete(loginFailLockKey(account, userId));
        if (StrUtils.hasText(account)) {
            redisOperator.delete(loginFailCountKey(account, null));
            redisOperator.delete(loginFailLockKey(account, null));
        }
    }

    private void throwRegisterDuplicateException(String username,
                                                 String email,
                                                 String phone,
                                                 DuplicateKeyException cause) {
        if (existsActiveUser(USER_FIELD_USERNAME, username)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "用户名已存在", cause);
        }
        if (existsActiveUser(USER_FIELD_EMAIL, email)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "邮箱已存在", cause);
        }
        if (existsActiveUser(USER_FIELD_PHONE, phone)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "手机号已存在", cause);
        }
        ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.DATA_ALREADY_EXISTS, "注册信息已存在", cause);
    }

    private boolean existsActiveUser(String fieldName, String identity) {
        return sysUserRepository.existsActiveByField(fieldName, identity);
    }

    private int resolveIntConfig(String configKey, int defaultValue, int minValue) {
        SysConfig config = sysConfigRepository.findByConfigKey(configKey);
        String configuredValue = config != null ? config.getConfigValue() : String.valueOf(defaultValue);
        if (!StrUtils.hasText(configuredValue)) {
            return defaultValue;
        }
        try {
            return Math.max(Integer.parseInt(configuredValue.trim()), minValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String loginFailCountKey(String account, Long userId) {
        return RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_COUNT_PREFIX, loginFailScope(account, userId));
    }

    private String loginFailLockKey(String account, Long userId) {
        return RedisKeyUtils.build(AuthConstants.LOGIN_FAIL_LOCK_PREFIX, loginFailScope(account, userId));
    }

    private String loginFailScope(String account, Long userId) {
        if (userId != null) {
            return RedisKeyUtils.build(AuthConstants.TOKEN_SCOPE_USER, userId);
        }
        return RedisKeyUtils.build(AuthConstants.TOKEN_SCOPE_ACCOUNT, StrUtils.trim(account));
    }
}


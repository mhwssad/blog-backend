package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
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
import com.cybzacg.blogbackend.module.auth.service.AuthService;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现。
 *
 * <p>负责账号登录、注册、邮箱验证码登录、令牌刷新以及当前登录态信息装配。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenManager tokenManager;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;
    private final AuthModelMapper authModelMapper;
    private final RedisOperator redisOperator;
    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken login(AuthLoginRequest request, String loginIp) {
        String account = request.getUsername() == null ? null : request.getUsername().trim();
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(account, request.getPassword())
        );

        Long userId = SecurityUtils.getUserId(authentication);
        if (userId != null) {
            sysUserService.updateLoginInfo(userId, loginIp);
        }
        return tokenManager.generateToken(authentication);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken register(AuthRegisterRequest request, String loginIp) {
        String username = normalizeValue(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String phone = normalizeValue(request.getPhone());
        validateRegisterIdentity(username, "用户名已存在");
        validateRegisterIdentity(email, "邮箱已存在");
        validateRegisterIdentity(phone, "手机号已存在");

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setDeletedFlag(0);
        sysUserService.save(user);

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.getPassword())
        );
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId != null) {
            sysUserService.updateLoginInfo(userId, loginIp);
        }
        return tokenManager.generateToken(authentication);
    }

    @Override
    public void sendEmailLoginCode(AuthEmailCodeRequest request) {
        String email = normalizeEmail(request.getEmail());
        SysUser user = sysUserService.getByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultErrorCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultErrorCode.ACCOUNT_DISABLED);
        }

        String code = generateEmailCode();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveMailFrom());
            message.setTo(email);
            message.setSubject(AuthConstants.EMAIL_LOGIN_SUBJECT);
            message.setText(buildEmailCodeContent(code));
            javaMailSender.send(message);
        } catch (Exception ex) {
            throw new BusinessException(ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED.getCode(),
                    ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED.getMessage(), ex);
        }

        redisOperator.set(emailLoginCodeKey(email), code, AuthConstants.EMAIL_LOGIN_CODE_TTL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthenticationToken emailLogin(AuthEmailLoginRequest request, String loginIp) {
        String email = normalizeEmail(request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                EmailCodeAuthenticationToken.unauthenticated(email, request.getCode().trim())
        );
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId != null) {
            sysUserService.updateLoginInfo(userId, loginIp);
        }
        return tokenManager.generateToken(authentication);
    }

    @Override
    public AuthenticationToken refresh(AuthRefreshRequest request) {
        if (!tokenManager.validateRefreshToken(request.getRefreshToken())) {
            throw new BusinessException(ResultErrorCode.INVALID_TOKEN);
        }
        return tokenManager.refreshToken(request.getRefreshToken());
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        tokenManager.invalidateToken(token);
    }

    @Override
    public AuthUserInfo getCurrentUser() {
        Authentication authentication = SecurityUtils.requireAuthentication();
        SysUser user = getCurrentSysUser(authentication);
        List<String> roleCodes = sysRoleService.listRoleCodesByUserId(user.getId());
        List<String> permissions = sysMenuService.listPermissionsByUserId(user.getId());
        return authModelMapper.toAuthUserInfo(user, roleCodes, permissions);
    }

    @Override
    public List<AuthMenuInfo> getCurrentUserMenus() {
        Authentication authentication = SecurityUtils.requireAuthentication();
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId == null) {
            SysUser user = getCurrentSysUser(authentication);
            userId = user.getId();
        }
        List<SysMenu> menus = sysMenuService.listMenusByUserId(userId);
        return buildMenuTree(menus);
    }

    /**
     * 优先根据用户 ID 获取当前用户，必要时回退到用户名查询，保证上下文兼容性。
     */
    private SysUser getCurrentSysUser(Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        SysUser user = userId != null ? sysUserService.getById(userId) : null;
        if (user == null) {
            user = sysUserService.getByUsername(SecurityUtils.getUsername(authentication));
        }
        if (user == null || !Integer.valueOf(0).equals(user.getDeletedFlag()) && user.getDeletedFlag() != null) {
            throw new BusinessException(ResultErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 将平铺菜单列表转换为树结构，并过滤按钮类型节点。
     */
    private List<AuthMenuInfo> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }

        Map<Long, AuthMenuInfo> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (MenuConstants.TYPE_BUTTON.equalsIgnoreCase(menu.getType())) {
                continue;
            }
            menuMap.put(menu.getId(), authModelMapper.toAuthMenuInfo(menu));
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

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateEmailCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private String resolveMailFrom() {
        String from = mailProperties.getUsername();
        if (!StringUtils.hasText(from)) {
            throw new BusinessException(ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED);
        }
        return from;
    }

    private String buildEmailCodeContent(String code) {
        return "您的登录验证码为：" + code + "，5分钟内有效。";
    }

    private String emailLoginCodeKey(String email) {
        return RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, email);
    }

    /**
     * 统一校验用户名、邮箱或手机号是否已被占用。
     */
    private void validateRegisterIdentity(String identity, String message) {
        if (!StringUtils.hasText(identity)) {
            return;
        }
        boolean exists = sysUserService.lambdaQuery()
                .eq(SysUser::getDeletedFlag, 0)
                .and(wrapper -> wrapper.eq(SysUser::getUsername, identity)
                        .or()
                        .eq(SysUser::getEmail, identity)
                        .or()
                        .eq(SysUser::getPhone, identity))
                .exists();
        if (exists) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), message);
        }
    }
}

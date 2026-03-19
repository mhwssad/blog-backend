package com.cybzacg.blogbackend.module.auth.authentication;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮箱验证码认证 Provider
 */
@Component
@RequiredArgsConstructor
public class EmailCodeAuthenticationProvider implements AuthenticationProvider {
    private final RedisOperator redisOperator;
    private final AuthUserDetailsService authUserDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = normalizeEmail(authentication.getName());
        String code = authentication.getCredentials() == null ? null : authentication.getCredentials().toString().trim();

        String cachedCode = redisOperator.get(emailLoginCodeKey(email), String.class);
        if (!StringUtils.hasText(cachedCode)) {
            throw new BusinessException(ResultErrorCode.EMAIL_CAPTCHA_EXPIRED);
        }
        if (!cachedCode.equals(code)) {
            throw new BusinessException(ResultErrorCode.EMAIL_CAPTCHA_INVALID);
        }

        AuthUserDetails userDetails = authUserDetailsService.loadAuthUserByUsername(email);
        if (!userDetails.isEnabled()) {
            throw new DisabledException(ResultErrorCode.ACCOUNT_DISABLED.getMessage());
        }

        redisOperator.delete(emailLoginCodeKey(email));
        EmailCodeAuthenticationToken authenticated = EmailCodeAuthenticationToken.authenticated(
                userDetails, userDetails.getAuthorities());
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return EmailCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String emailLoginCodeKey(String email) {
        return RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, email);
    }
}

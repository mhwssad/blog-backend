package com.cybzacg.blogbackend.module.auth.authentication;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

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
        String email = StrUtils.trimToLowerCase(authentication.getName());
        String code = authentication.getCredentials() == null ? null : StrUtils.trim(authentication.getCredentials().toString());

        String cachedCode = redisOperator.get(emailLoginCodeKey(email), String.class);
        ExceptionThrowerCore.throwBusinessIfBlank(cachedCode, ResultErrorCode.EMAIL_CAPTCHA_EXPIRED);
        ExceptionThrowerCore.throwBusinessIf(!cachedCode.equals(code), ResultErrorCode.EMAIL_CAPTCHA_INVALID);

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


    private String emailLoginCodeKey(String email) {
        return RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, email);
    }
}

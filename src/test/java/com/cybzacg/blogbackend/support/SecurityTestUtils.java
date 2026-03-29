package com.cybzacg.blogbackend.support;

import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mockStatic;

/**
 * 测试期安全上下文工具。
 *
 * <p>统一收口 `SecurityUtils` 的静态 mock，减少各测试类中重复的样板代码。
 */
public final class SecurityTestUtils {

    private SecurityTestUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 模拟当前登录用户 ID，兼容直接读取和强制要求登录两种调用路径。
     */
    public static MockedStatic<SecurityUtils> mockUserId(Long userId) {
        MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(userId);
        securityUtils.when(SecurityUtils::requireUserId).thenReturn(userId);
        return securityUtils;
    }

    /**
     * 模拟当前认证对象，并按需补充用户 ID 与用户名读取结果。
     */
    public static MockedStatic<SecurityUtils> mockAuthentication(Authentication authentication,
                                                                 Long userId,
                                                                 String username) {
        MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireAuthentication).thenReturn(authentication);
        securityUtils.when(() -> SecurityUtils.getUserId(authentication)).thenReturn(userId);
        if (username != null) {
            securityUtils.when(() -> SecurityUtils.getUsername(authentication)).thenReturn(username);
        }
        return securityUtils;
    }
}

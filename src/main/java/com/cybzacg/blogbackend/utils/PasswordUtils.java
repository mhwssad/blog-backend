package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 密码强度校验工具类。
 */
public final class PasswordUtils {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$");

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "123456789", "1234567890", "password", "password1",
            "qwerty123", "abc12345", "admin123", "letmein1", "welcome1"
    );

    private PasswordUtils() {
    }

    /**
     * 校验密码强度。不满足时抛出 {@link com.cybzacg.blogbackend.exception.BusinessException}。
     */
    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.PARAM_VALIDATION_FAILED, "密码不能为空");
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.PARAM_VALIDATION_FAILED,
                    "密码长度需在" + MIN_LENGTH + "-" + MAX_LENGTH + "位之间");
        }
        if (!STRENGTH_PATTERN.matcher(password).matches()) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.PARAM_VALIDATION_FAILED, "密码需包含大小写字母和数字");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.PARAM_VALIDATION_FAILED, "密码过于简单，请更换");
        }
    }
}

package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 文件名安全性校验器。
 * 禁止：路径分隔符（/ \\）、控制字符（\\0 \\n \\r）、双重扩展名。
 */
public class SafeFileNameValidator implements ConstraintValidator<SafeFileName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StrUtils.hasText(value)) {
            return true;
        }
        // 禁止路径分隔符和控制字符
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.indexOf('\0') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return false;
        }
        // 禁止双重扩展名（如 .tar.gz 中 name 部分含 . 即视为双重扩展名）
        int lastDot = value.lastIndexOf('.');
        if (lastDot > 0) {
            String nameWithoutExt = value.substring(0, lastDot);
            if (nameWithoutExt.lastIndexOf('.') >= 0) {
                return false;
            }
        }
        return true;
    }
}

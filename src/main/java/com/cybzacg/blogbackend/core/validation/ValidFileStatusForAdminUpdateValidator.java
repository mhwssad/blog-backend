package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidFileStatusForAdminUpdate} 校验器。
 * 确保状态值在枚举范围内，且不是 {@code DELETED} 或 {@code PHYSICAL_DELETE_PENDING}。
 */
public class ValidFileStatusForAdminUpdateValidator implements ConstraintValidator<ValidFileStatusForAdminUpdate, Integer> {

    @Override
    public boolean isValid(Integer status, ConstraintValidatorContext context) {
        if (status == null) {
            return true;
        }
        if (!FileStatusEnum.contains(status)) {
            return false;
        }
        if (FileStatusEnum.DELETED.getValue().equals(status)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("文件删除请使用删除接口，状态更新接口不支持设置为已删除")
                    .addConstraintViolation();
            return false;
        }
        if (FileStatusEnum.PHYSICAL_DELETE_PENDING.getValue().equals(status)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("待物理删除状态由系统自动管理，不支持手动设置")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}

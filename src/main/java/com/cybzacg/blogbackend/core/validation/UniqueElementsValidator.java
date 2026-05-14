package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link UniqueElements} 校验器。
 */
public class UniqueElementsValidator implements ConstraintValidator<UniqueElements, Collection<?>> {

    @Override
    public boolean isValid(Collection<?> collection, ConstraintValidatorContext context) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        Set<Object> seen = new LinkedHashSet<>();
        for (Object element : collection) {
            if (element == null) {
                return false;
            }
            if (!seen.add(element)) {
                return false;
            }
        }
        return true;
    }
}

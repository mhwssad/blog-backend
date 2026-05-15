package com.cybzacg.blogbackend.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Java 反射工具类。
 *
 * <p>统一收口对象实例化、字段读取/写入、方法调用和属性访问逻辑，避免业务代码反复处理
 * `setAccessible(true)`、父类向上查找和异常吞并等样板代码。
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 通过无参构造器创建实例，兼容私有构造器。
     */
    public static <T> T newInstance(Class<T> targetClass) {
        Objects.requireNonNull(targetClass, "目标类型不能为空");
        try {
            Constructor<T> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("创建实例失败: " + targetClass.getName(), ex);
        }
    }

    /**
     * 在当前类及其父类层级中查找字段。
     */
    public static Optional<Field> findField(Class<?> targetClass, String fieldName) {
        Objects.requireNonNull(targetClass, "目标类型不能为空");
        if (StrUtils.isBlank(fieldName)) {
            return Optional.empty();
        }

        for (Class<?> current = targetClass; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return Optional.of(field);
            } catch (NoSuchFieldException ignored) {
                // 继续向父类查找。
            }
        }
        return Optional.empty();
    }

    /**
     * 在当前类及其父类层级中查找方法。
     */
    public static Optional<Method> findMethod(Class<?> targetClass, String methodName, Class<?>... parameterTypes) {
        Objects.requireNonNull(targetClass, "目标类型不能为空");
        if (StrUtils.isBlank(methodName)) {
            return Optional.empty();
        }

        Class<?>[] resolvedParameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        for (Class<?> current = targetClass; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(methodName, resolvedParameterTypes);
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
                // 继续向父类查找。
            }
        }
        return Optional.empty();
    }

    /**
     * 读取字段值，字段不存在或读取失败时返回空。
     */
    public static Optional<Object> readField(Object target, String fieldName) {
        if (target == null) {
            return Optional.empty();
        }
        return findField(target.getClass(), fieldName).flatMap(field -> {
            try {
                return Optional.ofNullable(field.get(target));
            } catch (IllegalAccessException ignored) {
                return Optional.empty();
            }
        });
    }

    /**
     * 写入字段值，字段不存在或写入失败时返回 false。
     */
    public static boolean writeField(Object target, String fieldName, Object value) {
        if (target == null) {
            return false;
        }
        Optional<Field> field = findField(target.getClass(), fieldName);
        if (field.isEmpty()) {
            return false;
        }
        try {
            field.get().set(target, value);
            return true;
        } catch (IllegalAccessException ex) {
            return false;
        }
    }

    /**
     * 调用无参方法，方法不存在或调用失败时返回空。
     */
    public static Optional<Object> invokeNoArgMethod(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }
        return findMethod(target.getClass(), methodName).flatMap(method -> {
            try {
                return Optional.ofNullable(method.invoke(target));
            } catch (ReflectiveOperationException ignored) {
                return Optional.empty();
            }
        });
    }

    /**
     * 按 Java Bean getter 与字段回退顺序读取属性值。
     */
    public static Optional<Object> readProperty(Object target, String propertyName) {
        if (target == null || StrUtils.isBlank(propertyName)) {
            return Optional.empty();
        }

        String normalizedName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        Optional<Object> getterValue = invokeNoArgMethod(target, "get" + normalizedName);
        if (getterValue.isPresent()) {
            return getterValue;
        }

        getterValue = invokeNoArgMethod(target, "is" + normalizedName);
        if (getterValue.isPresent()) {
            return getterValue;
        }

        return readField(target, propertyName);
    }
}

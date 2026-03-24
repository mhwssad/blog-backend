package com.cybzacg.blogbackend.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bean 对象转换工具类
 * <p>
 * 基于 Spring {@link BeanUtils} 实现的对象转换工具，提供对象属性拷贝、列表转换等功能。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 单个对象转换
 * UserDTO userDTO = new UserDTO();
 * userDTO.setName("张三");
 * userDTO.setAge(25);
 *
 * User user = BeanConverterUtils.convert(userDTO, User::new);
 *
 * // 列表转换
 * List<UserDTO> dtoList = Arrays.asList(userDTO1, userDTO2);
 * List<User> userList = BeanConverterUtils.convertList(dtoList, User::new);
 *
 * // 带忽略字段的转换
 * User user = BeanConverterUtils.convert(userDTO, User::new, "password", "createTime");
 * }</pre>
 * </p>
 *
 * @author demo
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
public class BeanConverterUtils {

    private BeanConverterUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 对象属性拷贝
     * <p>
     * 将源对象的属性拷贝到目标对象。
     * </p>
     *
     * @param source      源对象
     * @param target      目标对象
     * @param ignoreNames 忽略的属性名称
     */
    public static void copyProperties(Object source, Object target, String... ignoreNames) {
        if (source == null || target == null) {
            log.warn("源对象或目标对象为 null，跳过属性拷贝");
            return;
        }
        BeanUtils.copyProperties(source, target, ignoreNames);
    }

    /**
     * 对象转换
     * <p>
     * 将源对象转换为目标类型的对象。
     * </p>
     *
     * @param source         源对象
     * @param targetSupplier 目标对象构造函数
     * @param <S>            源对象类型
     * @param <T>            目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 对象转换（带忽略字段）
     * <p>
     * 将源对象转换为目标类型的对象，忽略指定字段。
     * </p>
     *
     * @param source         源对象
     * @param targetSupplier 目标对象构造函数
     * @param ignoreNames    忽略的属性名称
     * @param <S>            源对象类型
     * @param <T>            目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier, String... ignoreNames) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target, ignoreNames);
        return target;
    }

    /**
     * 对象转换（指定目标类型）
     * <p>
     * 将源对象转换为目标类型的对象，通过反射创建目标对象实例。
     * </p>
     *
     * @param source      源对象
     * @param targetClass 目标对象类型
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, @NonNull Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            log.error("对象转换失败，源类型: {}, 目标类型: {}",
                    source.getClass().getName(), targetClass.getName(), e);
            throw new IllegalArgumentException("对象转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对象转换（指定目标类型，带忽略字段）
     * <p>
     * 将源对象转换为目标类型的对象，忽略指定字段。
     * </p>
     *
     * @param source      源对象
     * @param targetClass 目标对象类型
     * @param ignoreNames 忽略的属性名称
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, @NonNull Class<T> targetClass, String... ignoreNames) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target, ignoreNames);
            return target;
        } catch (Exception e) {
            log.error("对象转换失败，源类型: {}, 目标类型: {}",
                    source.getClass().getName(), targetClass.getName(), e);
            throw new IllegalArgumentException("对象转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列表转换
     * <p>
     * 将源对象列表转换为目标对象列表。
     * </p>
     *
     * @param sourceList     源对象列表
     * @param targetSupplier 目标对象构造函数
     * @param <S>            源对象类型
     * @param <T>            目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = convert(source, targetSupplier);
            if (target != null) {
                targetList.add(target);
            }
        }
        return targetList;
    }

    /**
     * 列表转换（带忽略字段）
     * <p>
     * 将源对象列表转换为目标对象列表，忽略指定字段。
     * </p>
     *
     * @param sourceList     源对象列表
     * @param targetSupplier 目标对象构造函数
     * @param ignoreNames    忽略的属性名称
     * @param <S>            源对象类型
     * @param <T>            目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier, String... ignoreNames) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = convert(source, targetSupplier, ignoreNames);
            if (target != null) {
                targetList.add(target);
            }
        }
        return targetList;
    }

    /**
     * 列表转换（指定目标类型）
     * <p>
     * 将源对象列表转换为目标对象列表。
     * </p>
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标对象类型
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, @NonNull Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = convert(source, targetClass);
            if (target != null) {
                targetList.add(target);
            }
        }
        return targetList;
    }

    /**
     * 列表转换（指定目标类型，带忽略字段）
     * <p>
     * 将源对象列表转换为目标对象列表，忽略指定字段。
     * </p>
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标对象类型
     * @param ignoreNames 忽略的属性名称
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, @NonNull Class<T> targetClass, String... ignoreNames) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            T target = convert(source, targetClass, ignoreNames);
            if (target != null) {
                targetList.add(target);
            }
        }
        return targetList;
    }

    /**
     * 更新对象
     * <p>
     * 将源对象的非 null 属性更新到目标对象。
     * </p>
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void update(Object source, Object target) {
        if (source == null || target == null) {
            log.warn("源对象或目标对象为 null，跳过更新");
            return;
        }
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 获取对象中属性值为 null 的属性名数组
     *
     * @param source 源对象
     * @return 属性值为 null 的属性名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        final java.beans.BeanInfo beanInfo;
        try {
            beanInfo = java.beans.Introspector.getBeanInfo(source.getClass());
        } catch (java.beans.IntrospectionException e) {
            return new String[0];
        }

        final java.beans.PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        final java.util.Set<String> emptyNames = new java.util.HashSet<>();

        for (java.beans.PropertyDescriptor pd : propertyDescriptors) {
            Object srcValue = null;
            try {
                srcValue = pd.getReadMethod().invoke(source);
            } catch (Exception e) {
                // 忽略异常
            }
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}


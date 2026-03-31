package com.cybzacg.blogbackend.utils;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Bean 处理工具类。
 *
 * <p>统一收口按类型/注解扫描 Bean、解析代理后的真实类型和顺序元数据等常见操作，减少
 * 对 `ApplicationContext`、`AopUtils` 和 `@Order` 细节的重复处理。
 */
public final class SpringBeanUtils {

    private SpringBeanUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取指定类型的全部 Bean，并保持 Spring 返回的注册顺序。
     */
    public static <T> Map<String, T> getBeansOfType(ListableBeanFactory beanFactory, Class<T> beanType) {
        Objects.requireNonNull(beanFactory, "BeanFactory 不能为空");
        Objects.requireNonNull(beanType, "Bean 类型不能为空");
        return Collections.unmodifiableMap(new LinkedHashMap<>(beanFactory.getBeansOfType(beanType)));
    }

    /**
     * 获取带指定注解的全部 Bean，并保持 Spring 返回的注册顺序。
     */
    public static Map<String, Object> getBeansWithAnnotation(ListableBeanFactory beanFactory,
                                                             Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(beanFactory, "BeanFactory 不能为空");
        Objects.requireNonNull(annotationType, "注解类型不能为空");
        return Collections.unmodifiableMap(new LinkedHashMap<>(beanFactory.getBeansWithAnnotation(annotationType)));
    }

    /**
     * 解析 Bean 的执行顺序，优先读取 `Ordered`，其次读取 `@Order`。
     */
    public static int resolveOrder(Object bean) {
        Objects.requireNonNull(bean, "Bean 不能为空");
        if (bean instanceof Ordered ordered) {
            return ordered.getOrder();
        }
        Class<?> targetClass = resolveTargetClass(bean);
        Order orderAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 解析代理后的真实 Bean 类型，无法解析时回退为运行时类型。
     */
    public static Class<?> resolveTargetClass(Object bean) {
        Objects.requireNonNull(bean, "Bean 不能为空");
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return targetClass != null ? targetClass : bean.getClass();
    }
}

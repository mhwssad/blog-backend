package com.cybzacg.blogbackend.exception.handler;

import jakarta.annotation.PostConstruct;
import com.cybzacg.blogbackend.utils.SpringBeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常处理器注册中心。<p>在启动时自动扫描所有带 {@code @RestControllerAdvice} 注解的异常处理器，按异常类型建立索引并按优先级排序，提供运行时查询能力。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionHandlerRegistry {


    private final ApplicationContext applicationContext;

    private final Map<Class<? extends Exception>, List<ExceptionHandlerInfo>> exceptionHandlerMap = new ConcurrentHashMap<>();
    private final List<Object> allHandlers = new ArrayList<>();

    /**
     * 启动时自动扫描并注册所有异常处理器。
     */
    @PostConstruct
    public void init() {
        Map<String, Object> handlers = SpringBeanUtils.getBeansWithAnnotation(applicationContext, RestControllerAdvice.class);

        for (Object handler : handlers.values()) {
            registerHandler(handler);
        }

        log.info("异常处理器注册中心初始化完成，共注册 {} 个异常处理器", allHandlers.size());
    }

    private void registerHandler(Object handler) {
        allHandlers.add(handler);
        int order = SpringBeanUtils.resolveOrder(handler);

        java.lang.reflect.Method[] methods = handler.getClass().getMethods();
        for (java.lang.reflect.Method method : methods) {
            if (method.isAnnotationPresent(org.springframework.web.bind.annotation.ExceptionHandler.class)) {
                org.springframework.web.bind.annotation.ExceptionHandler annotation = method
                        .getAnnotation(org.springframework.web.bind.annotation.ExceptionHandler.class);

                Class<? extends Throwable>[] exceptionTypes = annotation.value();
                for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                    if (Exception.class.isAssignableFrom(exceptionType)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends Exception> typedExceptionType = (Class<? extends Exception>) exceptionType;
                        ExceptionHandlerInfo info = new ExceptionHandlerInfo(handler, order, typedExceptionType);
                        exceptionHandlerMap.computeIfAbsent(typedExceptionType, k -> new ArrayList<>()).add(info);
                        exceptionHandlerMap.get(typedExceptionType).sort(Comparator.comparingInt(
                                ExceptionHandlerInfo::order));
                    }
                }
            }
        }

        log.debug("注册异常处理器: {} (优先级: {})", SpringBeanUtils.resolveTargetClass(handler).getSimpleName(), order);
    }

    /**
     * 根据异常类型查找匹配的处理器列表，按优先级排序。
     *
     * @param exception 需要处理的异常
     * @return 匹配的异常处理器列表
     */
    public List<Object> getHandlers(Exception exception) {
        List<Object> handlers = new ArrayList<>();
        Class<?> exceptionClass = exception.getClass();

        List<ExceptionHandlerInfo> directHandlers = exceptionHandlerMap.get(exceptionClass);
        if (directHandlers != null) {
            directHandlers.forEach(info -> handlers.add(info.handler()));
        }

        for (Map.Entry<Class<? extends Exception>, List<ExceptionHandlerInfo>> entry : exceptionHandlerMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionClass) && !entry.getKey().equals(exceptionClass)) {
                entry.getValue().forEach(info -> handlers.add(info.handler()));
            }
        }

        return handlers.stream()
                .distinct()
                .sorted(Comparator.comparingInt(SpringBeanUtils::resolveOrder))
                .toList();
    }

    /**
     * 获取所有已注册的异常处理器。
     *
     * @return 异常处理器列表
     */
    public List<Object> getAllHandlers() {
        return new ArrayList<>(allHandlers);
    }

    private record ExceptionHandlerInfo(Object handler, int order, Class<? extends Exception> exceptionType) {

    }
}


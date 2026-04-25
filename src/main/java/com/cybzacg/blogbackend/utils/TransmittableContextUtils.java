package com.cybzacg.blogbackend.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * 基于 TransmittableThreadLocal 的通用上下文管理工具。
 *
 * <p>统一收口线程内上下文的写入、读取、批量替换、快照恢复和异步任务包装逻辑，
 * 供 traceId、租户、操作人等横切字段在同步/异步链路中复用。
 */
public final class TransmittableContextUtils {

    private static final TransmittableThreadLocal<Map<String, Object>> CONTEXT_HOLDER =
            TransmittableThreadLocal.withInitial(LinkedHashMap::new);

    private TransmittableContextUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 写入单个上下文字段，值为 null 时等价于移除。
     */
    public static void put(String key, Object value) {
        Objects.requireNonNull(key, "上下文键不能为空");
        if (value == null) {
            remove(key);
            return;
        }
        Map<String, Object> context = new LinkedHashMap<>(CONTEXT_HOLDER.get());
        context.put(key, value);
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 批量覆盖当前线程上下文，入参为空时清空。
     */
    public static void replace(Map<String, ?> context) {
        if (context == null || context.isEmpty()) {
            clear();
            return;
        }
        LinkedHashMap<String, Object> copiedContext = new LinkedHashMap<>();
        context.forEach((key, value) -> {
            if (key != null && value != null) {
                copiedContext.put(key, value);
            }
        });
        CONTEXT_HOLDER.set(copiedContext);
    }

    /**
     * 返回当前线程上下文的只读快照。
     */
    public static Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(CONTEXT_HOLDER.get()));
    }

    /**
     * 读取上下文字段。
     */
    public static Optional<Object> get(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CONTEXT_HOLDER.get().get(key));
    }

    /**
     * 读取字符串上下文字段。
     */
    public static String getString(String key) {
        Object value = get(key).orElse(null);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 读取 Long 上下文字段，非数值类型时返回 null。
     */
    public static Long getLong(String key) {
        Object value = get(key).orElse(null);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 移除单个上下文字段。
     */
    public static void remove(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Map<String, Object> currentContext = CONTEXT_HOLDER.get();
        if (!currentContext.containsKey(key)) {
            return;
        }
        Map<String, Object> newContext = new LinkedHashMap<>(currentContext);
        newContext.remove(key);
        if (newContext.isEmpty()) {
            CONTEXT_HOLDER.remove();
            return;
        }
        CONTEXT_HOLDER.set(newContext);
    }

    /**
     * 清空当前线程上下文。
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 在指定上下文快照下执行任务，并在结束后恢复原上下文。
     */
    public static void runWithContext(Map<String, ?> context, Runnable action) {
        Objects.requireNonNull(action, "执行任务不能为空");
        Map<String, Object> previousContext = new LinkedHashMap<>(CONTEXT_HOLDER.get());
        try {
            replace(context);
            action.run();
        } finally {
            restore(previousContext);
        }
    }

    /**
     * 在指定上下文快照下执行任务并返回结果，并在结束后恢复原上下文。
     */
    public static <T> T callWithContext(Map<String, ?> context, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "执行任务不能为空");
        Map<String, Object> previousContext = new LinkedHashMap<>(CONTEXT_HOLDER.get());
        try {
            replace(context);
            return action.call();
        } finally {
            restore(previousContext);
        }
    }

    /**
     * 包装 Runnable，供线程池异步执行时透传当前上下文。
     */
    public static Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "执行任务不能为空");
        return TtlRunnable.get(runnable);
    }

    /**
     * 包装 Callable，供线程池异步执行时透传当前上下文。
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        Objects.requireNonNull(callable, "执行任务不能为空");
        return TtlCallable.get(callable);
    }

    private static void restore(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            CONTEXT_HOLDER.remove();
            return;
        }
        CONTEXT_HOLDER.set(new LinkedHashMap<>(context));
    }
}

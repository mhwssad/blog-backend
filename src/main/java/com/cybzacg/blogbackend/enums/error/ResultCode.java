package com.cybzacg.blogbackend.enums.error;


import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误码枚举接口
 */
public interface ResultCode {
    /**
     * 获取错误码
     *
     * <p>返回系统中定义的错误码，用于程序识别不同的错误类型。
     * 错误码在整个系统中应该是唯一的。
     *
     * @return 错误码整数值
     */
    Integer getCode();

    /**
     * 获取默认信息
     *
     * <p>返回错误码对应的默认描述信息，用于在无法获取国际化信息时显示。
     *
     * @return 默认错误描述信息
     */
    String getMessage();

    /**
     * 静态辅助类，提供优雅的查询方法
     */
    class Helper {
        // 线程安全的缓存：Class -> (code -> enum instance)
        private static final Map<Class<? extends ResultCode>, Map<Integer, ? extends ResultCode>>
                CODE_CACHE = new ConcurrentHashMap<>();
        private static final Map<Class<? extends ResultCode>, Map<String, ? extends ResultCode>>
                MESSAGE_CACHE = new ConcurrentHashMap<>();

        private Helper() {
        }

        /**
         * 根据code查询（推荐使用方式）
         */
        @SuppressWarnings("unchecked")
        public static <T extends ResultCode> T getByCode(Class<T> enumClass, Integer code) {
            if (code == null || enumClass == null) return null;

            Map<Integer, T> cache = (Map<Integer, T>) CODE_CACHE.computeIfAbsent(enumClass, k -> {
                Map<Integer, T> map = new ConcurrentHashMap<>();
                if (enumClass.isEnum()) {
                    Arrays.stream((enumClass).getEnumConstants())
                            .forEach(item -> map.put(item.getCode(), item));
                }
                return map;
            });

            return cache.get(code);
        }

        /**
         * 根据message查询
         */
        @SuppressWarnings("unchecked")
        public static <T extends ResultCode> T getByMessage(Class<T> enumClass, String message) {
            if (message == null || enumClass == null) return null;

            Map<String, T> cache = (Map<String, T>) MESSAGE_CACHE.computeIfAbsent(enumClass, k -> {
                Map<String, T> map = new ConcurrentHashMap<>();
                if (enumClass.isEnum()) {
                    Arrays.stream((enumClass).getEnumConstants())
                            .forEach(item -> map.put(item.getMessage(), item));
                }
                return map;
            });

            return cache.get(message);
        }

        /**
         * 通用查询方法
         */
        public static <T extends ResultCode> T valueOf(Class<T> enumClass, Object value) {
            if (value == null || enumClass == null) return null;

            if (value instanceof Integer integer) {
                return getByCode(enumClass, integer);
            } else if (value instanceof String string) {
                return getByMessage(enumClass, string);
            }
            return null;
        }
    }
}



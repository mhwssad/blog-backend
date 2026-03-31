package com.cybzacg.blogbackend.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream 快捷工具类。
 *
 * <p>统一收口空安全流创建、顺序保持收集和常见映射逻辑，减少业务代码里重复手写
 * `null` 判空、`Collectors` 组装和顺序控制。
 */
public final class StreamUtils {

    private StreamUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 基于集合创建空安全串行流，集合为空时返回空流。
     */
    public static <T> Stream<T> stream(Collection<T> values) {
        return values == null || values.isEmpty() ? Stream.empty() : values.stream();
    }

    /**
     * 基于集合创建空安全并行流，集合为空时返回空流。
     */
    public static <T> Stream<T> parallelStream(Collection<T> values) {
        return values == null || values.isEmpty() ? Stream.empty() : values.parallelStream();
    }

    /**
     * 基于集合创建过滤空元素后的流，适合做 ID、对象引用等空安全处理。
     */
    public static <T> Stream<T> nonNullStream(Collection<T> values) {
        return stream(values).filter(Objects::nonNull);
    }

    /**
     * 将集合按指定函数映射为不可变列表。
     */
    public static <T, R> List<R> mapToList(Collection<T> values, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "映射函数不能为空");
        List<R> result = stream(values)
                .<R>map(mapper)
                .collect(Collectors.toList());
        return result.isEmpty() ? List.of() : Collections.unmodifiableList(result);
    }

    /**
     * 将集合按指定函数映射为保持原始遍历顺序的不可变集合。
     */
    public static <T, R> Set<R> mapToSet(Collection<T> values, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "映射函数不能为空");
        LinkedHashSet<R> result = stream(values)
                .map(mapper)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? Set.of() : Collections.unmodifiableSet(result);
    }

    /**
     * 对集合做顺序保持去重，并返回不可变列表。
     */
    public static <T> List<T> distinctToList(Collection<T> values) {
        return stream(values).distinct().toList();
    }

    /**
     * 按键提取函数将集合收集为顺序保持的不可变映射，重复键时以后出现的值为准。
     */
    public static <T, K> Map<K, T> toLinkedMap(Collection<T> values, Function<? super T, ? extends K> keyMapper) {
        return toLinkedMap(values, keyMapper, Function.identity());
    }

    /**
     * 按键和值提取函数将集合收集为顺序保持的不可变映射，重复键时以后出现的值为准。
     */
    public static <T, K, V> Map<K, V> toLinkedMap(Collection<T> values,
                                                  Function<? super T, ? extends K> keyMapper,
                                                  Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "键映射函数不能为空");
        Objects.requireNonNull(valueMapper, "值映射函数不能为空");
        LinkedHashMap<K, V> result = stream(values)
                .collect(Collectors.toMap(
                        keyMapper,
                        valueMapper,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
        return result.isEmpty() ? Map.of() : Collections.unmodifiableMap(result);
    }

    /**
     * 按分组函数将集合收集为顺序保持的不可变映射，组内元素顺序与原集合遍历顺序一致。
     */
    public static <T, K> Map<K, List<T>> groupByToList(Collection<T> values,
                                                       Function<? super T, ? extends K> classifier) {
        Objects.requireNonNull(classifier, "分组函数不能为空");
        LinkedHashMap<K, List<T>> grouped = stream(values)
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
        if (grouped.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<K, List<T>> readonlyResult = new LinkedHashMap<>();
        grouped.forEach((key, group) -> readonlyResult.put(key, List.copyOf(group)));
        return Collections.unmodifiableMap(readonlyResult);
    }
}

package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ID 集合处理工具。
 *
 * <p>统一收口 ID 列表去重、空值校验和字符串持久化等横向能力，避免各业务模块重复手写同类逻辑。
 */
public final class IdCollectionUtils {

    private IdCollectionUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 校验 ID 列表中的元素不能为空，并按原始顺序返回去重后的结果。
     */
    public static List<Long> distinctNonNullIds(List<Long> ids, ResultCode resultCode, String nullMessage) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>();
        for (Long id : ids) {
            ExceptionThrowerCore.throwBusinessIfNull(id, resultCode, nullMessage);
            distinctIds.add(id);
        }
        return List.copyOf(distinctIds);
    }

    /**
     * 校验 ID 列表中的元素不能为空且不能重复，并按原始顺序返回唯一结果。
     */
    public static List<Long> requireUniqueNonNullIds(List<Long> ids,
                                                     ResultCode resultCode,
                                                     String nullMessage,
                                                     String duplicateMessage) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>();
        for (Long id : ids) {
            ExceptionThrowerCore.throwBusinessIfNull(id, resultCode, nullMessage);
            ExceptionThrowerCore.throwBusinessIf(!distinctIds.add(id), resultCode, duplicateMessage);
        }
        return List.copyOf(distinctIds);
    }

    /**
     * 将 ID 列表转换为逗号分隔字符串，便于兼容旧表的字符串持久化口径。
     */
    public static String toCommaSeparatedIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}

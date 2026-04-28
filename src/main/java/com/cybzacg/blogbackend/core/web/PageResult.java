package com.cybzacg.blogbackend.core.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页响应
 */
@Data
@Builder
public class PageResult<T> {
    private Long total;
    private Long current;
    private Long size;
    private List<T> records;

    /**
     * 从 MyBatis Plus IPage 和转换后的 records 创建分页结果
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        return PageResult.<T>builder()
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .records(records)
                .build();
    }

    /**
     * 从 IPage 直接创建分页结果（当 records 已是目标类型时）
     */
    public static <T> PageResult<T> fromPage(IPage<T> page) {
        return PageResult.<T>builder()
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .records(page.getRecords())
                .build();
    }

    /**
     * 直接指定分页参数创建分页结果
     */
    public static <T> PageResult<T> of(long total, long current, long size, List<T> records) {
        return PageResult.<T>builder()
                .total(total)
                .current(current)
                .size(size)
                .records(records)
                .build();
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .total(0L)
                .current(1L)
                .size(10L)
                .records(List.of())
                .build();
    }

    /**
     * 创建指定页码和大小的空分页结果
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return PageResult.<T>builder()
                .total(0L)
                .current(current)
                .size(size)
                .records(List.of())
                .build();
    }
}

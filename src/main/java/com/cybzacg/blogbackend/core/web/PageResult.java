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

    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        return PageResult.<T>builder()
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .records(records)
                .build();
    }
}

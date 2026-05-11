package com.cybzacg.blogbackend.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import lombok.Getter;

import java.util.List;

/**
 * 分页工具类 - 简化分页相关常见操作。
 *
 * <p>提供分页参数提取、offset 计算、MyBatis-Plus Page 转换、分页结果回填等能力。
 * 所有方法均为线程安全的静态工具方法，不涉及任何业务逻辑或数据库操作。
 *
 * <p><strong>使用示例：</strong>
 * <pre>
 * // 1. 从 PageQuery 提取规范化参数并转换为 MyBatis-Plus Page
 * Page&lt;User&gt; page = PaginationUtils.toPage(query, 20L, 100L);
 *
 * // 2. 计算数据库查询所需的 offset
 * long offset = PaginationUtils.calculateOffset(current, size);
 *
 * // 3. 将分页结果回填到 Query 对象
 * PaginationUtils.fillBackPagination(query, pageResult);
 *
 * // 4. 构建统一的分页响应
 * PageResult&lt;UserVO&gt; result = PaginationUtils.buildPageResult(page, voList);
 * </pre>
 *
 * @author blog-backend
 */
public final class PaginationUtils {

    private PaginationUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 1. 分页参数提取与默认值处理 ====================

    /**
     * 标准化分页页码。
     *
     * <p>痛点：前端传入的页码可能是 null、0 或负数，直接用于查询会导致异常。
     * 本方法确保页码始终为有效值，最小为 1。
     *
     * @param current 原始页码，null 或小于 1 时返回 1
     * @return 标准化后的页码
     */
    public static long normalizeCurrent(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    /**
     * 标准化分页大小。
     *
     * <p>痛点：前端传入的分页大小可能超出合理范围（如 0、负数或超大值）。
     * 本方法在 [defaultValue, maxValue] 范围内约束分页大小。
     *
     * @param size          原始分页大小
     * @param defaultValue  默认值，分页大小为 null 或小于 1 时启用
     * @param maxValue      上限值，分页大小不能超过此值
     * @return 标准化后的分页大小，范围 [defaultValue, maxValue]
     */
    public static long normalizeSize(
        Long size,
        long defaultValue,
        long maxValue
    ) {
        long normalized = size == null || size < 1L ? defaultValue : size;
        return Math.min(normalized, maxValue);
    }

    /**
     * 从分页查询对象中安全提取并标准化分页参数。
     *
     * <p>痛点：每个 Service 方法都需要重复编写参数提取和默认值逻辑。
     * 本方法一站式完成参数提取、null 检查和范围约束。
     *
     * <p>示例：
     * <pre>
     * // 在 Service 中替代原有的手写逻辑：
     * // long current = query.getCurrent() == null ? 1L : query.getCurrent();
     * // long size = query.getSize() == null ? 20L : Math.min(query.getSize(), 100L);
     *
     * PaginationResult params = PaginationUtils.extractPaginationParams(query, 20L, 100L);
     * long current = params.getCurrent();
     * long size = params.getSize();
     * </pre>
     *
     * @param query         分页查询对象（通常为 PageQuery 或其子类），不能为 null
     * @param defaultSize   默认每页条数
     * @param maxSize       最大每页条数上限
     * @return 包含标准化后 current 和 size 的结果对象
     * @throws IllegalArgumentException 当 query 为 null 时抛出
     */
    public static PaginationResult extractPaginationParams(
        Object query,
        long defaultSize,
        long maxSize
    ) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询对象不能为 null");
        }
        long current = normalizeCurrent(
            getFieldValue(query, "getCurrent", Long.class)
        );
        long size = normalizeSize(
            getFieldValue(query, "getSize", Long.class),
            defaultSize,
            maxSize
        );
        return new PaginationResult(current, size);
    }

    /**
     * 从分页查询对象中提取页码，不做标准化处理。
     *
     * <p>适用于需要自行处理默认值和边界的场景。
     *
     * @param query 分页查询对象，不能为 null
     * @return 原始页码值，可能为 null
     * @throws IllegalArgumentException 当 query 为 null 时抛出
     */
    public static Long getCurrent(Object query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询对象不能为 null");
        }
        return getFieldValue(query, "getCurrent", Long.class);
    }

    /**
     * 从分页查询对象中提取每页条数，不做标准化处理。
     *
     * <p>适用于需要自行处理默认值和边界的场景。
     *
     * @param query 分页查询对象，不能为 null
     * @return 原始分页大小，可能为 null
     * @throws IllegalArgumentException 当 query 为 null 时抛出
     */
    public static Long getSize(Object query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询对象不能为 null");
        }
        return getFieldValue(query, "getSize", Long.class);
    }

    // ==================== 2. Offset 自动计算 ====================

    /**
     * 根据页码和每页条数计算数据库查询所需的 offset（跳过记录数）。
     *
     * <p>痛点：手写 offset 计算容易出错，特别是第一页和边界条件的处理。
     * 数据库查询通常使用 LIMIT size OFFSET offset 或 LIMIT offset, size。
     *
     * <p>计算公式：offset = (current - 1) * size
     * <ul>
     *   <li>第 1 页，每页 10 条：offset = (1-1) * 10 = 0</li>
     *   <li>第 2 页，每页 10 条：offset = (2-1) * 10 = 10</li>
     *   <li>第 5 页，每页 20 条：offset = (5-1) * 20 = 80</li>
     * </ul>
     *
     * @param current 页码（从 1 开始），会自动标准化
     * @param size    每页条数，会自动标准化
     * @return offset 跳过记录数，始终 >= 0
     */
    public static long calculateOffset(long current, long size) {
        long normalizedCurrent = normalizeCurrent(current);
        long normalizedSize = normalizeSize(size, 1L, Integer.MAX_VALUE);
        return (normalizedCurrent - 1) * normalizedSize;
    }

    /**
     * 根据页码和每页条数计算数据库查询所需的 limit（返回记录数）。
     *
     * <p>痛点：某些数据库或 ORM 框架需要明确指定 limit 而非依赖 Page 的内置分页。
     *
     * @param size 每页条数，会自动标准化
     * @return limit 返回记录数，始终 >= 0
     */
    public static long calculateLimit(long size) {
        return Math.max(0L, normalizeSize(size, 1L, Integer.MAX_VALUE));
    }

    /**
     * 计算分页查询的起始和结束位置。
     *
     * <p>适用于手写 SQL 分页或需要明确知道查询范围的场景。
     *
     * @param current 页码（从 1 开始）
     * @param size    每页条数
     * @return 长度为 2 的数组，[0] 为 start（offset），[1] 为 end（limit）
     */
    public static long[] calculatePageBounds(long current, long size) {
        long normalizedCurrent = normalizeCurrent(current);
        long normalizedSize = normalizeSize(size, 1L, Integer.MAX_VALUE);
        return new long[] {
            (normalizedCurrent - 1) * normalizedSize,
            normalizedSize,
        };
    }

    // ==================== 3. 转换为 MyBatis-Plus Page ====================

    /**
     * 将分页查询对象转换为 MyBatis-Plus 的 Page 对象。
     *
     * <p>痛点：每个分页 Service 方法都需要重复编写
     * {@code new Page<>(current, size)} 的构造逻辑。
     * 本方法一站式完成参数提取、标准化和 Page 对象创建。
     *
     * <p>示例：
     * <pre>
     * // 原有代码：
     * long current = PaginationUtils.normalizeCurrent(query.getCurrent());
     * long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
     * Page&lt;User&gt; page = new Page&lt;&gt;(current, size);
     *
     * // 使用工具方法后：
     * Page&lt;User&gt; page = PaginationUtils.toPage(query, 20L, 100L);
     * </pre>
     *
     * @param query        分页查询对象，不能为 null
     * @param defaultSize  默认每页条数
     * @param maxSize      最大每页条数上限
     * @param <T>          实体类型
     * @return 配置好的 MyBatis-Plus Page 对象，可直接用于 Repository 查询
     * @throws IllegalArgumentException 当 query 为 null 时抛出
     */
    public static <T> Page<T> toPage(
        Object query,
        long defaultSize,
        long maxSize
    ) {
        PaginationResult params = extractPaginationParams(
            query,
            defaultSize,
            maxSize
        );
        return new Page<>(params.getCurrent(), params.getSize());
    }

    /**
     * 将已标准化的分页参数转换为 MyBatis-Plus 的 Page 对象。
     *
     * <p>适用于已经完成参数标准化处理的场景，避免重复计算。
     *
     * @param current 已标准化的页码（>= 1）
     * @param size    已标准化的每页条数（>= 1）
     * @param <T>     实体类型
     * @return 配置好的 MyBatis-Plus Page 对象
     */
    public static <T> Page<T> toPage(long current, long size) {
        return new Page<>(
            normalizeCurrent(current),
            normalizeSize(size, 1L, Integer.MAX_VALUE)
        );
    }

    // ==================== 4. 分页结果回填到 Query 对象 ====================

    /**
     * 将分页查询结果回填到原始 Query 对象中（如果 Query 支持 set 操作）。
     *
     * <p>痛点：有时需要在查询后更新 Query 对象中的分页参数（如规范化后的值），
     * 以便后续逻辑或日志记录使用标准化后的值。
     *
     * <p>示例：
     * <pre>
     * // 查询前
     * PaginationUtils.fillBackPagination(query, current, size);
     * // 查询后，query.getCurrent() 和 query.getSize() 已更新为标准化值
     * </pre>
     *
     * @param query   分页查询对象，必须提供无参数的 setCurrent 和 setSize 方法
     * @param current 标准化后的页码
     * @param size    标准化后的每页条数
     * @return 始终返回 true，仅作为操作成功标识
     * @throws IllegalArgumentException 当 query 为 null 时抛出
     */
    public static boolean fillBackPagination(
        Object query,
        long current,
        long size
    ) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询对象不能为 null");
        }
        setFieldValue(
            query,
            "setCurrent",
            Long.class,
            normalizeCurrent(current)
        );
        setFieldValue(
            query,
            "setSize",
            Long.class,
            normalizeSize(size, 1L, Integer.MAX_VALUE)
        );
        return true;
    }

    /**
     * 将分页查询结果（从 MyBatis-Plus Page）回填到原始 Query 对象中。
     *
     * <p>使用 MyBatis-Plus Page 的实际查询参数（可能因边界条件而与原始输入不同）。
     *
     * @param query 分页查询对象
     * @param page  MyBatis-Plus 分页结果
     * @return 始终返回 true
     */
    public static boolean fillBackPaginationFromPage(
        Object query,
        IPage<?> page
    ) {
        if (query == null || page == null) {
            return false;
        }
        return fillBackPagination(query, page.getCurrent(), page.getSize());
    }

    // ==================== 5. 分页结果组装 ====================

    /**
     * 从 MyBatis-Plus IPage 和转换后的记录列表构建统一的分页响应。
     *
     * <p>痛点：每个分页 Service 方法都需要重复编写
     * {@code PageResult.<T>builder().total(page.getTotal()).current(page.getCurrent())...}
     * 的构建逻辑。本方法一站式完成分页响应组装。
     *
     * <p>示例：
     * <pre>
     * // 原有代码：
     * List&lt;UserVO&gt; voList = page.getRecords().stream().map(this::toVO).toList();
     * return PageResult.&lt;UserVO&gt;builder()
     *         .total(page.getTotal())
     *         .current(page.getCurrent())
     *         .size(page.getSize())
     *         .records(voList)
     *         .build();
     *
     * // 使用工具方法后：
     * List&lt;UserVO&gt; voList = page.getRecords().stream().map(this::toVO).toList();
     * return PaginationUtils.buildPageResult(page, voList);
     * </pre>
     *
     * @param page    MyBatis-Plus 分页结果对象，不能为 null
     * @param records 当前页的数据记录列表（已完成 VO 转换），不能为 null
     * @param <T>      记录类型
     * @return 组装好的 PageResult 对象
     * @throws IllegalArgumentException 当 page 或 records 为 null 时抛出
     */
    public static <T> PageResult<T> buildPageResult(
        IPage<?> page,
        List<T> records
    ) {
        if (page == null) {
            throw new IllegalArgumentException("分页结果对象不能为 null");
        }
        if (records == null) {
            throw new IllegalArgumentException("记录列表不能为 null");
        }
        return PageResult.<T>builder()
            .total(page.getTotal())
            .current(page.getCurrent())
            .size(page.getSize())
            .records(records)
            .build();
    }

    /**
     * 从原始分页参数和记录列表构建分页响应。
     *
     * <p>适用于非 MyBatis-Plus 分页场景（如手写 SQL 或其他 ORM）。
     *
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页条数
     * @param records 当前页的数据记录列表
     * @param <T>     记录类型
     * @return 组装好的 PageResult 对象
     */
    public static <T> PageResult<T> buildPageResult(
        long total,
        long current,
        long size,
        List<T> records
    ) {
        if (records == null) {
            throw new IllegalArgumentException("记录列表不能为 null");
        }
        return PageResult.<T>builder()
            .total(total)
            .current(normalizeCurrent(current))
            .size(Math.max(0L, size))
            .records(records)
            .build();
    }

    /**
     * 根据总记录数和分页参数构建分页响应（无记录列表时使用）。
     *
     * <p>适用于只需要分页元数据而不需要具体记录的场景。
     *
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页条数
     * @param <T>     泛型类型（结果集为空列表时使用）
     * @return 组装好的 PageResult 对象
     */
    public static <T> PageResult<T> buildPageResult(
        long total,
        long current,
        long size
    ) {
        return buildPageResult(total, current, size, List.of());
    }

    /**
     * 计算总页数。
     *
     * <p>痛点：手写总页数计算容易出错，特别是 total 为 0 或不能整除的场景。
     *
     * @param total 总记录数
     * @param size  每页条数
     * @return 总页数，最小为 0
     */
    public static long calculateTotalPages(long total, long size) {
        if (total <= 0 || size <= 0) {
            return 0L;
        }
        long normalizedSize = Math.max(1L, size);
        return (total + normalizedSize - 1) / normalizedSize;
    }

    /**
     * 判断是否为最后一页。
     *
     * @param current       当前页码
     * @param size          每页条数
     * @param totalRecords  总记录数
     * @return true 表示当前页是最后一页
     */
    public static boolean isLastPage(
        long current,
        long size,
        long totalRecords
    ) {
        if (current <= 0 || size <= 0 || totalRecords <= 0) {
            return false;
        }
        return current >= calculateTotalPages(totalRecords, size);
    }

    /**
     * 判断是否为空分页结果（无任何记录）。
     *
     * @param page MyBatis-Plus 分页结果
     * @return true 表示没有记录
     */
    public static boolean isEmptyPage(IPage<?> page) {
        return (
            page == null ||
            page.getTotal() == 0 ||
            page.getRecords() == null ||
            page.getRecords().isEmpty()
        );
    }

    // ==================== 内部工具方法 ====================

    /**
     * 通过反射获取对象字段值。
     * 仅用于访问分页查询对象的 getCurrent/getSize 方法。
     */
    private static <R> R getFieldValue(
        Object obj,
        String methodName,
        Class<R> returnType
    ) {
        try {
            java.lang.reflect.Method method = obj
                .getClass()
                .getMethod(methodName);
            Object result = method.invoke(obj);
            return returnType.cast(result);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过反射设置对象字段值。
     * 仅用于设置分页查询对象的 setCurrent/setSize 方法。
     */
    private static void setFieldValue(
        Object obj,
        String methodName,
        Class<?> paramType,
        Object value
    ) {
        try {
            java.lang.reflect.Method method = obj
                .getClass()
                .getMethod(methodName, paramType);
            method.invoke(obj, value);
        } catch (Exception e) {
            // 静默处理反射异常
        }
    }

    /**
     * 分页参数提取结果包装类。
     * 避免多次调用返回多个值，保持代码简洁。
     */
    @Getter
    public static final class PaginationResult {

        private final long current;
        private final long size;

        PaginationResult(long current, long size) {
            this.current = current;
            this.size = size;
        }

    }
}

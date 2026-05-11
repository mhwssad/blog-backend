package com.cybzacg.blogbackend.dto.repository.content;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.content.SysCategory;

import java.util.Collection;
import java.util.List;

/**
 * 分类 Repository。<p>封装分类表的持久化操作，提供按类型、父级、编码等维度的查询与校验。
 */
public interface SysCategoryRepository extends IService<SysCategory> {
    /**
     * 按类型查询分类列表，并按排序值和 ID 升序返回。
     *
     * @param type 分类类型
     * @return 分类列表
     */
    List<SysCategory> findByTypeOrderBySortOrderAndId(String type);

    /**
     * 按类型和状态查询分类列表，并按排序值和 ID 升序返回。
     *
     * @param type   分类类型
     * @param status 分类状态
     * @return 分类列表
     */
    List<SysCategory> findByTypeAndStatusOrderBySortOrderAndId(String type, Integer status);

    /**
     * 查询指定父分类下的直接子分类。
     *
     * @param parentId 父分类 ID
     * @return 子分类列表
     */
    List<SysCategory> findByParentId(Long parentId);

    /**
     * 检查指定父分类下是否存在子分类。
     *
     * @param parentId 父分类 ID
     * @return 是否存在子分类
     */
    boolean existsByParentId(Long parentId);

    /**
     * 校验同类型分类编码在排除指定 ID 后是否仍然存在。
     *
     * @param type      分类类型
     * @param code      分类编码
     * @param excludeId 排除的分类 ID
     * @return 是否存在重复编码
     */
    boolean existsByTypeAndCodeExcludingId(String type, String code, Long excludeId);

    /**
     * 按类型和 ID 集合查询分类，用于校验指定 ID 是否属于该类型。
     *
     * @param type 分类类型
     * @param ids  分类 ID 集合
     * @return 匹配的分类列表
     */
    List<SysCategory> listByTypeAndIds(String type, Collection<Long> ids);

    /**
     * 按类型、状态和编码集合查询分类。
     */
    List<SysCategory> listByTypeStatusAndCodes(String type, Integer status, Collection<String> codes);
}

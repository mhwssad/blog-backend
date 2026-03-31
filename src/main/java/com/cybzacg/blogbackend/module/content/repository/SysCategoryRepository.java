package com.cybzacg.blogbackend.module.content.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysCategory;

import java.util.List;

/**
 * 分类 Repository。
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
     * @param type 分类类型
     * @param code 分类编码
     * @param excludeId 排除的分类 ID
     * @return 是否存在重复编码
     */
    boolean existsByTypeAndCodeExcludingId(String type, String code, Long excludeId);
}

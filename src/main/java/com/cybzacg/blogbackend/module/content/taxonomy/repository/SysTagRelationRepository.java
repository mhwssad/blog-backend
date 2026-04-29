package com.cybzacg.blogbackend.module.content.taxonomy.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysTagRelation;

import java.util.List;

/**
 * 标签关联 Repository。<p>封装标签与目标实体（文章等）多对多关联关系的持久化操作。
 */
public interface SysTagRelationRepository extends IService<SysTagRelation> {
    /**
     * 检查指定标签是否存在关联记录。
     *
     * @param tagId 标签 ID
     * @return 是否存在关联
     */
    boolean existsByTagId(Long tagId);

    /**
     * 删除指定标签的全部关联记录。
     *
     * @param tagId 标签 ID
     * @return 是否删除成功
     */
    boolean removeByTagId(Long tagId);

    /**
     * 删除指定目标类型和目标 ID 的全部标签关联。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 按目标类型和标签 ID 查询关联的目标 ID 列表。
     *
     * @param targetType 目标类型
     * @param tagId      标签 ID
     * @return 目标 ID 列表（按关联 ID 升序）
     */
    List<Long> listTargetIdsByTargetTypeAndTagId(String targetType, Long tagId);

    /**
     * 按目标类型和目标 ID 查询关联的标签 ID 列表。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 标签 ID 列表（按关联 ID 升序）
     */
    List<Long> listTagIdsByTargetTypeAndTargetId(String targetType, Long targetId);
}

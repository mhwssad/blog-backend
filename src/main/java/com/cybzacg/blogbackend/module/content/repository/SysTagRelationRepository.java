package com.cybzacg.blogbackend.module.content.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysTagRelation;

/**
 * 标签关联 Repository。
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
}

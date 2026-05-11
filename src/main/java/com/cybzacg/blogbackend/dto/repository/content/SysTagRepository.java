package com.cybzacg.blogbackend.dto.repository.content;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.content.SysTag;

import java.util.List;

/**
 * 标签 Repository。<p>封装标签表的持久化操作，提供标签列表查询与重名校验。
 */
public interface SysTagRepository extends IService<SysTag> {
    /**
     * 按 ID 倒序查询全部标签。
     *
     * @return 标签列表
     */
    List<SysTag> findAllOrderByIdDesc();

    /**
     * 校验标签名称在排除指定 ID 后是否仍然存在。
     *
     * @param name      标签名称
     * @param excludeId 需要排除的标签 ID
     * @return 是否存在重名标签
     */
    boolean existsByNameExcludingId(String name, Long excludeId);

    /**
     * 按目标类型查询标签。
     *
     * @param targetType 目标类型
     * @return 标签列表
     */
    List<SysTag> findByTargetType(String targetType);

    /**
     * 按名称集合查询标签。
     */
    List<SysTag> listByNames(List<String> names);
}

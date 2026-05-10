package com.cybzacg.blogbackend.module.content.interaction.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.content.SysInteraction;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionPageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 互动 Repository。<p>封装用户互动（点赞、收藏等）记录的持久化操作，提供多维度查询与批量删除。
 */
public interface SysInteractionRepository extends IService<SysInteraction> {
    /**
     * 按管理端查询条件分页查询互动记录。
     *
     * @param query 查询条件
     * @return 互动分页结果
     */
    Page<SysInteraction> pageByAdminConditions(InteractionPageQuery query);

    /**
     * 判断用户是否已对目标执行指定互动。
     *
     * @param userId     用户 ID
     * @param targetId   目标 ID
     * @param targetType 目标类型
     * @param actionType 行为类型
     * @return 是否存在互动
     */
    boolean existsByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId,
                                                                Long targetId,
                                                                String targetType,
                                                                String actionType);

    /**
     * 查询用户对目标的指定互动记录。
     *
     * @param userId     用户 ID
     * @param targetId   目标 ID
     * @param targetType 目标类型
     * @param actionType 行为类型
     * @return 互动记录
     */
    SysInteraction findOneByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId,
                                                                        Long targetId,
                                                                        String targetType,
                                                                        String actionType);

    /**
     * 按目标类型和目标 ID 删除互动记录。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 按目标类型和目标 ID 集合删除互动记录。
     *
     * @param targetType 目标类型
     * @param targetIds  目标 ID 集合
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetIds(String targetType, Collection<Long> targetIds);

    /**
     * 查询用户在一组目标上的互动记录。
     *
     * @param userId     用户 ID
     * @param targetType 目标类型
     * @param actionType 行为类型
     * @param targetIds  目标 ID 集合
     * @return 互动记录列表
     */
    List<SysInteraction> findByUserIdAndTargetTypeAndActionTypeInTargetIds(Long userId,
                                                                           String targetType,
                                                                           String actionType,
                                                                           Collection<Long> targetIds);
}

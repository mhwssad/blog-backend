package com.cybzacg.blogbackend.module.content.interaction.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.content.SysInteraction;
import com.cybzacg.blogbackend.mapper.content.SysInteractionMapper;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 互动 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供用户互动（点赞、收藏等）数据的增删改查。
 */
@Repository
public class SysInteractionRepositoryImpl extends ServiceImpl<SysInteractionMapper, SysInteraction>
        implements SysInteractionRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysInteraction> pageByAdminConditions(InteractionPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysInteraction>()
                .eq(query.getUserId() != null, SysInteraction::getUserId, query.getUserId())
                .eq(query.getTargetId() != null, SysInteraction::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysInteraction::getTargetType, query.getTargetType())
                .eq(query.getActionType() != null, SysInteraction::getActionType, query.getActionType())
                .orderByDesc(SysInteraction::getCreatedAt)
                .orderByDesc(SysInteraction::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId,
                                                                       Long targetId,
                                                                       String targetType,
                                                                       String actionType) {
        return exists(new LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getActionType, actionType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysInteraction findOneByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId,
                                                                               Long targetId,
                                                                               String targetType,
                                                                               String actionType) {
        return getOne(new LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, targetId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getActionType, actionType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getTargetId, targetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetIds(String targetType, Collection<Long> targetIds) {
        // 空集合时无需执行删除，直接返回成功
        if (targetIds == null || targetIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getTargetType, targetType)
                .in(SysInteraction::getTargetId, targetIds));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysInteraction> findByUserIdAndTargetTypeAndActionTypeInTargetIds(Long userId,
                                                                                  String targetType,
                                                                                  String actionType,
                                                                                  Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetType, targetType)
                .eq(SysInteraction::getActionType, actionType)
                .in(SysInteraction::getTargetId, targetIds));
    }
}

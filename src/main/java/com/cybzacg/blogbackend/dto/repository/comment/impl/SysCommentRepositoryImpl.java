package com.cybzacg.blogbackend.dto.repository.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.content.SysComment;
import com.cybzacg.blogbackend.dto.mapper.content.SysCommentMapper;
import com.cybzacg.blogbackend.dto.repository.comment.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentPageQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供评论数据的增删改查。
 */
@Repository
public class SysCommentRepositoryImpl extends ServiceImpl<SysCommentMapper, SysComment>
        implements SysCommentRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysComment> pageByAdminConditions(CommentPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<SysComment>()
                .eq(query.getTargetId() != null, SysComment::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysComment::getTargetType, query.getTargetType())
                .eq(query.getUserId() != null, SysComment::getUserId, query.getUserId())
                .eq(query.getRootId() != null, SysComment::getRootId, query.getRootId())
                .eq(query.getParentId() != null, SysComment::getParentId, query.getParentId())
                .eq(query.getStatus() != null, SysComment::getStatus, query.getStatus())
                .orderByDesc(SysComment::getCreatedAt)
                .orderByDesc(SysComment::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysComment> findByTargetTypeAndTargetId(String targetType, Long targetId) {
        return list(new LambdaQueryWrapper<SysComment>()
                .eq(SysComment::getTargetType, targetType)
                .eq(SysComment::getTargetId, targetId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<SysComment> pageRootCommentsByTarget(Long targetId, String targetType, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<SysComment>()
                .eq(SysComment::getTargetId, targetId)
                .eq(SysComment::getTargetType, targetType)
                .eq(SysComment::getRootId, 0L)
                .eq(SysComment::getParentId, 0L)
                .eq(SysComment::getStatus, 1)
                .orderByAsc(SysComment::getCreatedAt)
                .orderByAsc(SysComment::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysComment> selectRepliesByRootIds(List<Long> rootIds) {
        return baseMapper.selectRepliesByRootIds(rootIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysComment>()
                .eq(SysComment::getTargetType, targetType)
                .eq(SysComment::getTargetId, targetId));
    }

    @Override
    public void incrementLikeCount(Long id, int delta) {
        baseMapper.incrementLikeCount(id, delta);
    }

    @Override
    public void incrementReplyCount(Long id, int delta) {
        baseMapper.incrementReplyCount(id, delta);
    }
}

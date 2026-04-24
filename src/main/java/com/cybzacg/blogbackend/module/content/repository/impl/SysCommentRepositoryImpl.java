package com.cybzacg.blogbackend.module.content.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.mapper.SysCommentMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论 Repository 实现。
 */
@Repository
public class SysCommentRepositoryImpl extends ServiceImpl<SysCommentMapper, SysComment>
        implements SysCommentRepository {

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

    @Override
    public List<SysComment> findByTargetTypeAndTargetId(String targetType, Long targetId) {
        return list(new LambdaQueryWrapper<SysComment>()
                .eq(SysComment::getTargetType, targetType)
                .eq(SysComment::getTargetId, targetId));
    }

    @Override
    public List<SysComment> selectRootCommentsByTarget(Long targetId, String targetType) {
        return baseMapper.selectRootCommentsByTarget(targetId, targetType);
    }

    @Override
    public List<SysComment> selectRepliesByRootIds(List<Long> rootIds) {
        return baseMapper.selectRepliesByRootIds(rootIds);
    }

    @Override
    public boolean removeByTargetTypeAndTargetId(String targetType, Long targetId) {
        return remove(new LambdaQueryWrapper<SysComment>()
                .eq(SysComment::getTargetType, targetType)
                .eq(SysComment::getTargetId, targetId));
    }
}

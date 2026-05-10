package com.cybzacg.blogbackend.module.forum.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.dto.mapper.forum.ForumReplyMapper;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 论坛回复 Repository 实现。
 */
@Repository
public class ForumReplyRepositoryImpl extends ServiceImpl<ForumReplyMapper, ForumReply>
        implements ForumReplyRepository {
    @Override
    public Page<ForumReply> pageRootReplies(Long postId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<ForumReply>()
                .eq(ForumReply::getPostId, postId)
                .eq(ForumReply::getParentId, 0L)
                .eq(ForumReply::getRootId, 0L)
                .eq(ForumReply::getStatus, ForumReplyStatusEnum.NORMAL.getValue())
                .orderByAsc(ForumReply::getFloorNo)
                .orderByAsc(ForumReply::getId));
    }

    @Override
    public Page<ForumReply> pageAdminReplies(ForumReplyAdminPageQuery query) {
        LambdaQueryWrapper<ForumReply> wrapper = new LambdaQueryWrapper<ForumReply>()
                .eq(query.getPostId() != null, ForumReply::getPostId, query.getPostId())
                .eq(query.getUserId() != null, ForumReply::getUserId, query.getUserId())
                .eq(query.getStatus() != null, ForumReply::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(ForumReply::getContent, query.getKeyword()))
                .orderByAsc(ForumReply::getFloorNo)
                .orderByAsc(ForumReply::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    @Override
    public List<ForumReply> listByPostId(Long postId) {
        return list(new LambdaQueryWrapper<ForumReply>()
                .eq(ForumReply::getPostId, postId)
                .eq(ForumReply::getStatus, ForumReplyStatusEnum.NORMAL.getValue())
                .orderByAsc(ForumReply::getFloorNo)
                .orderByAsc(ForumReply::getId));
    }

    @Override
    public List<ForumReply> listRepliesByRootIds(List<Long> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ForumReply>()
                .in(ForumReply::getRootId, rootIds)
                .eq(ForumReply::getStatus, ForumReplyStatusEnum.NORMAL.getValue())
                .orderByAsc(ForumReply::getFloorNo)
                .orderByAsc(ForumReply::getId));
    }

    @Override
    public long countByPostId(Long postId) {
        return count(new LambdaQueryWrapper<ForumReply>()
                .eq(ForumReply::getPostId, postId)
                .ne(ForumReply::getStatus, ForumReplyStatusEnum.DELETED.getValue()));
    }

    @Override
    public int nextFloorNo(Long postId) {
        return (int) countByPostId(postId) + 1;
    }

    @Override
    public void incrementLikeCount(Long id, int delta) {
        baseMapper.incrementLikeCount(id, delta);
    }

    @Override
    public void incrementReplyCount(Long id, int delta) {
        baseMapper.incrementReplyCount(id, delta);
    }

    @Override
    public void softDeleteById(Long id) {
        lambdaUpdate()
                .eq(ForumReply::getId, id)
                .set(ForumReply::getStatus, ForumReplyStatusEnum.DELETED.getValue())
                .update();
    }

    @Override
    public void updateStatusById(Long id, Integer status) {
        lambdaUpdate()
                .eq(ForumReply::getId, id)
                .set(ForumReply::getStatus, status)
                .update();
    }
}

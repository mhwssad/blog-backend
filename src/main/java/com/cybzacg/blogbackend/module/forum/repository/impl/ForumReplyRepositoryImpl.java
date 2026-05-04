package com.cybzacg.blogbackend.module.forum.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.forum.ForumReply;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.mapper.forum.ForumReplyMapper;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import org.springframework.stereotype.Repository;

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
}

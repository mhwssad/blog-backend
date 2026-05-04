package com.cybzacg.blogbackend.module.forum.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.forum.ForumReply;

import java.util.List;

/**
 * 论坛回复 Repository。
 */
public interface ForumReplyRepository extends IService<ForumReply> {
    Page<ForumReply> pageRootReplies(Long postId, long current, long size);

    List<ForumReply> listByPostId(Long postId);

    List<ForumReply> listRepliesByRootIds(List<Long> rootIds);

    long countByPostId(Long postId);

    int nextFloorNo(Long postId);

    void incrementLikeCount(Long id, int delta);

    void incrementReplyCount(Long id, int delta);

    void softDeleteById(Long id);
}

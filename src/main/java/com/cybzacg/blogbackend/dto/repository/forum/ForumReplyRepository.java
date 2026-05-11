package com.cybzacg.blogbackend.dto.repository.forum;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumReplyAdminPageQuery;

import java.util.List;

/**
 * 论坛回复 Repository。
 */
public interface ForumReplyRepository extends IService<ForumReply> {
    Page<ForumReply> pageRootReplies(Long postId, long current, long size);

    Page<ForumReply> pageAdminReplies(ForumReplyAdminPageQuery query);

    List<ForumReply> listByPostId(Long postId);

    List<ForumReply> listRepliesByRootIds(List<Long> rootIds);

    long countByPostId(Long postId);

    int nextFloorNo(Long postId);

    void incrementLikeCount(Long id, int delta);

    void incrementReplyCount(Long id, int delta);

    void softDeleteById(Long id);

    void updateStatusById(Long id, Integer status);
}

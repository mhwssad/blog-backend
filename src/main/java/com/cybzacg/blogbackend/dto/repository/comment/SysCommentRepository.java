package com.cybzacg.blogbackend.dto.repository.comment;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.content.SysComment;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentPageQuery;

import java.util.List;

/**
 * 评论 Repository。<p>封装评论表的持久化操作，提供后台分页查询、根评论/回复树查询及批量删除。
 */
public interface SysCommentRepository extends IService<SysComment> {
    /**
     * 按后台条件分页查询评论。
     *
     * @param query 查询条件
     * @return 评论分页结果
     */
    Page<SysComment> pageByAdminConditions(CommentPageQuery query);

    /**
     * 查询指定目标下的全部评论。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 评论列表
     */
    List<SysComment> findByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 分页查询指定目标下的根评论。
     *
     * @param targetId   目标 ID
     * @param targetType 目标类型
     * @param current    页码
     * @param size       每页数量
     * @return 根评论分页结果
     */
    Page<SysComment> pageRootCommentsByTarget(Long targetId, String targetType, long current, long size);

    /**
     * 按根评论 ID 批量查询回复。
     *
     * @param rootIds 根评论 ID 列表
     * @return 回复列表
     */
    List<SysComment> selectRepliesByRootIds(List<Long> rootIds);

    /**
     * 删除指定目标类型和目标 ID 的全部评论。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetId(String targetType, Long targetId);

    void incrementLikeCount(Long id, int delta);

    void incrementReplyCount(Long id, int delta);
}

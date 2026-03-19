package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.model.user.CommentSaveRequest;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.UserCommentService;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户评论服务实现。
 *
 * <p>负责评论点赞、发表评论、删除评论，以及评论统计数据的联动维护。
 */
@Service
@RequiredArgsConstructor
public class UserCommentServiceImpl implements UserCommentService {
    private final SysCommentService sysCommentService;
    private final SysInteractionService sysInteractionService;
    private final BlogArticleService blogArticleService;
    private final ArticleAccessControlService articleAccessControlService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        boolean exists = sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, commentId)
                .eq(SysInteraction::getTargetType, "comment")
                .eq(SysInteraction::getActionType, "like")
                .exists();
        if (exists) {
            return;
        }
        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(userId);
        interaction.setTargetId(commentId);
        interaction.setTargetType("comment");
        interaction.setActionType("like");
        sysInteractionService.save(interaction);
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        sysCommentService.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        SysInteraction interaction = sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, commentId)
                .eq(SysInteraction::getTargetType, "comment")
                .eq(SysInteraction::getActionType, "like")
                .one();
        if (interaction == null) {
            return;
        }
        sysInteractionService.removeById(interaction.getId());
        comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
        sysCommentService.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(CommentSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        if (!"article".equals(request.getTargetType())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前仅支持文章评论");
        }
        BlogArticle article = blogArticleService.getById(request.getTargetId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "文章不存在");
        }
        articleAccessControlService.validateArticleAccess(article, userId);

        SysComment comment = new SysComment();
        comment.setTargetType("article");
        comment.setTargetId(request.getTargetId());
        comment.setContent(StrUtils.trim(request.getContent()));
        comment.setImages(request.getImages() == null || request.getImages().isEmpty() ? null : JsonUtils.toJson(request.getImages()));
        comment.setUserId(userId);
        comment.setRootId(request.getRootId() == null ? 0L : request.getRootId());
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = getCommentOrThrow(comment.getParentId());
            if (!parent.getTargetId().equals(request.getTargetId()) || !"article".equals(parent.getTargetType())) {
                throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父评论与目标不匹配");
            }
            if (comment.getRootId() == null || comment.getRootId() == 0L) {
                comment.setRootId(parent.getRootId() == null || parent.getRootId() == 0L ? parent.getId() : parent.getRootId());
            }
            parent.setReplyCount((parent.getReplyCount() == null ? 0 : parent.getReplyCount()) + 1);
            sysCommentService.updateById(parent);
        } else {
            comment.setRootId(0L);
            comment.setParentId(0L);
        }

        sysCommentService.save(comment);
        article.setCommentCount((article.getCommentCount() == null ? 0 : article.getCommentCount()) + 1);
        blogArticleService.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        if (!userId.equals(comment.getUserId())) {
            throw new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "只能删除自己的评论");
        }
        List<SysComment> subtree = collectSubtree(comment);
        sysCommentService.removeByIds(subtree.stream().map(SysComment::getId).toList());
        sysInteractionService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysInteraction>()
                .eq(SysInteraction::getTargetType, "comment")
                .in(SysInteraction::getTargetId, subtree.stream().map(SysComment::getId).toList()));

        BlogArticle article = blogArticleService.getById(comment.getTargetId());
        if (article != null) {
            article.setCommentCount(Math.max(0, (article.getCommentCount() == null ? 0 : article.getCommentCount()) - subtree.size()));
            blogArticleService.updateById(article);
        }
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = sysCommentService.getById(comment.getParentId());
            if (parent != null) {
                parent.setReplyCount(Math.max(0, (parent.getReplyCount() == null ? 0 : parent.getReplyCount()) - 1));
                sysCommentService.updateById(parent);
            }
        }
    }

    /**
     * 通过广度优先遍历收集评论子树，便于删除评论时一并清理其所有后代节点。
     */
    private List<SysComment> collectSubtree(SysComment root) {
        List<SysComment> allComments = sysCommentService.lambdaQuery()
                .eq(SysComment::getTargetType, root.getTargetType())
                .eq(SysComment::getTargetId, root.getTargetId())
                .list();
        java.util.Map<Long, List<SysComment>> byParent = allComments.stream().collect(Collectors.groupingBy(SysComment::getParentId));
        ArrayDeque<SysComment> queue = new ArrayDeque<>();
        queue.add(root);
        List<SysComment> result = new java.util.ArrayList<>();
        Set<Long> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            SysComment current = queue.poll();
            if (!visited.add(current.getId())) {
                continue;
            }
            result.add(current);
            for (SysComment child : byParent.getOrDefault(current.getId(), List.of())) {
                queue.add(child);
            }
        }
        return result;
    }

    /**
     * 按 ID 获取评论，不存在时抛出统一业务异常。
     */
    private SysComment getCommentOrThrow(Long commentId) {
        SysComment comment = sysCommentService.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "评论不存在");
        }
        return comment;
    }
}

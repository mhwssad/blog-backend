package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.CommentSaveRequest;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.service.UserCommentService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.TreeTraversalUtils;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户评论服务实现。
 *
 * <p>负责评论点赞、发表评论、删除评论，以及评论统计数据的联动维护。
 */
@Service
@RequiredArgsConstructor
public class UserCommentServiceImpl implements UserCommentService {
    private final SysCommentRepository sysCommentRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final ArticleContentFacadeService articleContentFacadeService;
    private final ContentModelMapper contentModelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 为评论新增点赞关系，并同步递增评论点赞数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        boolean exists = sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(
                userId,
                commentId,
                "comment",
                "like");
        if (exists) {
            return;
        }
        SysInteraction interaction = contentModelMapper.toInteraction(userId, commentId, "comment", "like");
        sysInteractionRepository.save(interaction);
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        sysCommentRepository.updateById(comment);
    }

    /**
     * 取消评论点赞，并同步回退评论点赞数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        SysInteraction interaction = sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(
                userId,
                commentId,
                "comment",
                "like");
        if (interaction == null) {
            return;
        }
        sysInteractionRepository.removeById(interaction.getId());
        comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
        sysCommentRepository.updateById(comment);
    }

    /**
     * 创建文章评论，并维护父评论回复数与文章评论总数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(CommentSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ExceptionThrowerCore.throwBusinessIf(!"article".equals(request.getTargetType()), ResultErrorCode.ILLEGAL_ARGUMENT, "当前仅支持文章评论");
        BlogArticle article = articleContentFacadeService.requireInteractableArticle(request.getTargetId(), userId, "评论");

        SysComment comment = contentModelMapper.toComment(request);
        comment.setTargetType("article");
        comment.setUserId(userId);
        comment.setRootId(request.getRootId() == null ? 0L : request.getRootId());
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = getCommentOrThrow(comment.getParentId());
            ExceptionThrowerCore.throwBusinessIf(!parent.getTargetId().equals(request.getTargetId()) || !"article".equals(parent.getTargetType()), ResultErrorCode.ILLEGAL_ARGUMENT, "父评论与目标不匹配");
            if (comment.getRootId() == null || comment.getRootId() == 0L) {
                comment.setRootId(parent.getRootId() == null || parent.getRootId() == 0L ? parent.getId() : parent.getRootId());
            }
            parent.setReplyCount((parent.getReplyCount() == null ? 0 : parent.getReplyCount()) + 1);
            sysCommentRepository.updateById(parent);
        } else {
            comment.setRootId(0L);
            comment.setParentId(0L);
        }

        sysCommentRepository.save(comment);
        articleContentFacadeService.adjustCommentCount(article.getId(), 1);
        eventPublisher.publishEvent(new XpAwardEvent(
                userId, ExperienceSourceTypeEnum.COMMENT_CREATE.getValue(),
                String.valueOf(comment.getId()),
                "comment_create:" + userId + ":" + comment.getId()));
        if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            notificationDeliveryService.deliverAfterCommit(
                    article.getAuthorId(),
                    NotificationTypeEnum.COMMENT_ME,
                    "你的文章收到了评论",
                    "《" + article.getTitle() + "》收到了新评论：" + abbreviate(comment.getContent(), 80),
                    userId);
        }
    }

    /**
     * 删除评论子树及其交互记录，并同步回退文章评论统计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = SecurityUtils.requireUserId();
        SysComment comment = getCommentOrThrow(commentId);
        ExceptionThrowerCore.throwBusinessIfNot(userId.equals(comment.getUserId()), ResultErrorCode.FORBIDDEN, "只能删除自己的评论");
        List<SysComment> allComments = sysCommentRepository.findByTargetTypeAndTargetId(comment.getTargetType(), comment.getTargetId());
        List<SysComment> subtree = TreeTraversalUtils.bfsCollectSubtree(comment, SysComment::getId, SysComment::getParentId, allComments);
        List<Long> subtreeIds = subtree.stream().map(SysComment::getId).toList();
        sysCommentRepository.removeByIds(subtreeIds);
        sysInteractionRepository.removeByTargetTypeAndTargetIds("comment", subtreeIds);

        articleContentFacadeService.adjustCommentCount(comment.getTargetId(), -subtree.size());
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = sysCommentRepository.getById(comment.getParentId());
            if (parent != null) {
                parent.setReplyCount(Math.max(0, (parent.getReplyCount() == null ? 0 : parent.getReplyCount()) - 1));
                sysCommentRepository.updateById(parent);
            }
        }
    }

    /**
     * 按 ID 获取评论，不存在时抛出统一业务异常。
     */
    private SysComment getCommentOrThrow(Long commentId) {
        SysComment comment = sysCommentRepository.getById(commentId);
        ExceptionThrowerCore.throwBusinessIfNull(comment, ResultErrorCode.ILLEGAL_ARGUMENT, "评论不存在");
        return comment;
    }

    private String abbreviate(String value, int maxLength) {
        String text = StrUtils.trimToNull(value);
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }
}




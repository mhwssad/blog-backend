package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.service.CommentAdminService;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论后台管理服务实现。
 *
 * <p>负责后台评论分页查询、详情查看、状态修改以及整棵评论子树的删除处理。
 */
@Service
@RequiredArgsConstructor
public class CommentAdminServiceImpl implements CommentAdminService {
    private final SysCommentService sysCommentService;
    private final BlogArticleService blogArticleService;
    private final SysUserService sysUserService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<CommentVO> pageComments(CommentPageQuery query) {
        LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<SysComment>()
                .eq(query.getTargetId() != null, SysComment::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysComment::getTargetType, query.getTargetType())
                .eq(query.getUserId() != null, SysComment::getUserId, query.getUserId())
                .eq(query.getRootId() != null, SysComment::getRootId, query.getRootId())
                .eq(query.getParentId() != null, SysComment::getParentId, query.getParentId())
                .eq(query.getStatus() != null, SysComment::getStatus, query.getStatus())
                .orderByDesc(SysComment::getCreatedAt)
                .orderByDesc(SysComment::getId);
        Page<SysComment> page = sysCommentService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords().stream().map(SysComment::getUserId).collect(Collectors.toSet()));
        List<CommentVO> records = page.getRecords().stream()
                .map(contentModelMapper::toCommentVO)
                .peek(vo -> fillUserInfo(vo, userMap.get(vo.getUserId())))
                .peek(vo -> vo.setLiked(false))
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public CommentVO getComment(Long id) {
        SysComment comment = getCommentOrThrow(id);
        CommentVO vo = contentModelMapper.toCommentVO(comment);
        fillUserInfo(vo, sysUserService.getById(comment.getUserId()));
        vo.setLiked(false);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        SysComment comment = getCommentOrThrow(id);
        comment.setStatus(status);
        sysCommentService.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        SysComment comment = getCommentOrThrow(id);
        List<SysComment> subtree = collectSubtree(comment);
        Set<Long> deleteIds = subtree.stream().map(SysComment::getId).collect(Collectors.toSet());
        sysCommentService.removeByIds(deleteIds);

        if ("article".equals(comment.getTargetType())) {
            BlogArticle article = blogArticleService.getById(comment.getTargetId());
            if (article != null) {
                int nextCount = Math.max(0, (article.getCommentCount() == null ? 0 : article.getCommentCount()) - subtree.size());
                article.setCommentCount(nextCount);
                blogArticleService.updateById(article);
            }
        }

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = sysCommentService.getById(comment.getParentId());
            if (parent != null) {
                int nextReplyCount = Math.max(0, (parent.getReplyCount() == null ? 0 : parent.getReplyCount()) - 1);
                parent.setReplyCount(nextReplyCount);
                sysCommentService.updateById(parent);
            }
        }
    }

    /**
     * 通过广度优先遍历收集整棵评论子树，确保批量删除时不会遗漏后代节点。
     */
    private List<SysComment> collectSubtree(SysComment root) {
        List<SysComment> allComments = sysCommentService.lambdaQuery()
                .eq(SysComment::getTargetType, root.getTargetType())
                .eq(SysComment::getTargetId, root.getTargetId())
                .list();
        Map<Long, List<SysComment>> byParent = allComments.stream().collect(Collectors.groupingBy(SysComment::getParentId));
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

    private Map<Long, SysUser> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> map = new HashMap<>();
        sysUserService.listByIds(userIds).forEach(user -> map.put(user.getId(), user));
        return map;
    }

    private void fillUserInfo(CommentVO vo, SysUser user) {
        if (user == null) {
            return;
        }
        vo.setUserNickname(user.getNickname());
        vo.setUserAvatar(user.getAvatar());
    }

    /**
     * 校验评论状态是否在后台允许的取值范围内。
     */
    private void validateStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status) && !Integer.valueOf(2).equals(status)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "评论状态非法");
        }
    }

    /**
     * 按 ID 获取评论，不存在时抛出统一业务异常。
     */
    private SysComment getCommentOrThrow(Long id) {
        SysComment comment = sysCommentService.getById(id);
        if (comment == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "评论不存在");
        }
        return comment;
    }
}

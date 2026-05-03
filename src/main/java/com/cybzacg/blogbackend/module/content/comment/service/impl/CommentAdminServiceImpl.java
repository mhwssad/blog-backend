package com.cybzacg.blogbackend.module.content.comment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.content.SysComment;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentPageQuery;
import com.cybzacg.blogbackend.module.content.comment.model.admin.CommentVO;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.comment.service.CommentAdminService;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.TreeTraversalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论后台管理服务实现。
 *
 * <p>负责后台评论分页查询、详情查看、状态修改以及整棵评论子树的删除处理。
 */
@Service
@RequiredArgsConstructor
public class CommentAdminServiceImpl implements CommentAdminService {
    private final SysCommentRepository sysCommentRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final ArticleContentFacadeService articleContentFacadeService;
    private final SysUserRepository sysUserRepository;
    private final ContentModelConvert contentModelConvert;

    /**
     * 按管理端条件分页查询评论列表，并填充用户昵称与头像。
     */
    @Override
    public PageResult<CommentVO> pageComments(CommentPageQuery query) {
        Page<SysComment> page = sysCommentRepository.pageByAdminConditions(query);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords().stream().map(SysComment::getUserId).collect(Collectors.toSet()));
        List<CommentVO> records = page.getRecords().stream()
                .map(contentModelConvert::toCommentVO)
                .peek(vo -> fillUserInfo(vo, userMap.get(vo.getUserId())))
                .peek(vo -> vo.setLiked(false))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 按ID获取评论详情，并填充用户信息。
     */
    @Override
    public CommentVO getComment(Long id) {
        SysComment comment = getCommentOrThrow(id);
        CommentVO vo = contentModelConvert.toCommentVO(comment);
        fillUserInfo(vo, sysUserRepository.getById(comment.getUserId()));
        vo.setLiked(false);
        return vo;
    }

    /**
     * 修改评论状态（正常/隐藏/删除）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        SysComment comment = getCommentOrThrow(id);
        comment.setStatus(status);
        sysCommentRepository.updateById(comment);
    }

    /**
     * 删除指定评论及其整棵回复子树，并同步回退文章评论数与父评论回复数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        SysComment comment = getCommentOrThrow(id);
        List<SysComment> allComments = sysCommentRepository.findByTargetTypeAndTargetId(comment.getTargetType(), comment.getTargetId());
        List<SysComment> subtree = TreeTraversalUtils.bfsCollectSubtree(comment, SysComment::getId, SysComment::getParentId, allComments);
        Set<Long> deleteIds = subtree.stream().map(SysComment::getId).collect(Collectors.toSet());
        sysCommentRepository.removeByIds(deleteIds);
        sysInteractionRepository.removeByTargetTypeAndTargetIds("comment", new ArrayList<>(deleteIds));

        if ("article".equals(comment.getTargetType())) {
            articleContentFacadeService.adjustCommentCount(comment.getTargetId(), -subtree.size());
        }

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            SysComment parent = sysCommentRepository.getById(comment.getParentId());
            if (parent != null) {
                int nextReplyCount = Math.max(0, (parent.getReplyCount() == null ? 0 : parent.getReplyCount()) - 1);
                parent.setReplyCount(nextReplyCount);
                sysCommentRepository.updateById(parent);
            }
        }
    }

    private Map<Long, SysUser> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> map = new HashMap<>();
        sysUserRepository.listByIds(userIds).forEach(user -> map.put(user.getId(), user));
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
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status) && !Integer.valueOf(2).equals(status), ResultErrorCode.ILLEGAL_ARGUMENT, "评论状态非法");
    }

    /**
     * 按 ID 获取评论，不存在时抛出统一业务异常。
     */
    private SysComment getCommentOrThrow(Long id) {
        SysComment comment = sysCommentRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(comment, ResultErrorCode.ILLEGAL_ARGUMENT, "评论不存在");
        return comment;
    }
}

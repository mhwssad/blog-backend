package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.mapper.SysTagMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentQuery;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.service.PublicContentQueryService;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 前台内容查询服务实现。
 *
 * <p>负责公开分类树、标签列表和评论树等前台展示数据的读取与组装。
 */
@Service
@RequiredArgsConstructor
public class PublicContentQueryServiceImpl implements PublicContentQueryService {
    private static final String ARTICLE_TYPE = "article";

    private final SysCategoryService sysCategoryService;
    private final SysTagService sysTagService;
    private final SysTagMapper sysTagMapper;
    private final SysCommentService sysCommentService;
    private final SysInteractionService sysInteractionService;
    private final SysUserService sysUserService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public List<PublicCategoryTreeVO> listCategoryTree() {
        List<SysCategory> categories = sysCategoryService.lambdaQuery()
                .eq(SysCategory::getType, ARTICLE_TYPE)
                .eq(SysCategory::getStatus, 1)
                .orderByAsc(SysCategory::getSortOrder)
                .orderByAsc(SysCategory::getId)
                .list();
        Map<Long, PublicCategoryTreeVO> categoryMap = new LinkedHashMap<>();
        for (SysCategory category : categories) {
            categoryMap.put(category.getId(), contentModelMapper.toPublicCategoryTreeVO(category));
        }
        List<PublicCategoryTreeVO> roots = new ArrayList<>();
        for (PublicCategoryTreeVO node : categoryMap.values()) {
            PublicCategoryTreeVO parent = categoryMap.get(node.getParentId());
            if (parent == null || Long.valueOf(0L).equals(node.getParentId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public List<PublicTagVO> listTags(String targetType) {
        String actualTargetType = StrUtils.trimToDefault(targetType, ARTICLE_TYPE);
        if (!ARTICLE_TYPE.equals(actualTargetType)) {
            return List.of();
        }
        return sysTagMapper.selectByTargetType(actualTargetType).stream()
                .map(contentModelMapper::toPublicTagVO)
                .toList();
    }

    @Override
    public List<PublicCommentVO> listComments(PublicCommentQuery query) {
        List<SysComment> comments = sysCommentService.lambdaQuery()
                .eq(SysComment::getTargetType, query.getTargetType())
                .eq(SysComment::getTargetId, query.getTargetId())
                .eq(SysComment::getStatus, 1)
                .orderByAsc(SysComment::getCreatedAt)
                .orderByAsc(SysComment::getId)
                .list();
        if (comments.isEmpty()) {
            return List.of();
        }

        Long currentUserId = SecurityUtils.getUserId();
        Set<Long> userIds = comments.stream().map(SysComment::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = loadUserMap(userIds);
        Set<Long> likedCommentIds = loadLikedCommentIds(currentUserId, comments.stream().map(SysComment::getId).collect(Collectors.toSet()));

        Map<Long, PublicCommentVO> commentMap = new LinkedHashMap<>();
        for (SysComment comment : comments) {
            PublicCommentVO vo = contentModelMapper.toPublicCommentVO(comment);
            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
            vo.setLiked(likedCommentIds.contains(comment.getId()));
            commentMap.put(comment.getId(), vo);
        }

        List<PublicCommentVO> roots = new ArrayList<>();
        for (PublicCommentVO comment : commentMap.values()) {
            if (comment.getRootId() == null || comment.getRootId() == 0L || comment.getParentId() == null || comment.getParentId() == 0L) {
                roots.add(comment);
                continue;
            }
            PublicCommentVO parent = commentMap.get(comment.getParentId());
            if (parent != null) {
                parent.getChildren().add(comment);
            } else {
                roots.add(comment);
            }
        }
        return roots;
    }

    private Map<Long, SysUser> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        sysUserService.listByIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        return userMap;
    }

    /**
     * 批量查询当前用户已点赞的评论 ID，用于前台评论列表状态回填。
     */
    private Set<Long> loadLikedCommentIds(Long currentUserId, Collection<Long> commentIds) {
        if (currentUserId == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, currentUserId)
                .eq(SysInteraction::getTargetType, "comment")
                .eq(SysInteraction::getActionType, "like")
                .in(SysInteraction::getTargetId, commentIds)
                .list()
                .stream()
                .map(SysInteraction::getTargetId)
                .collect(Collectors.toSet());
    }
}



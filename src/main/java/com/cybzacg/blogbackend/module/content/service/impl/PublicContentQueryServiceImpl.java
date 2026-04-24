package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentQuery;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.repository.SysCategoryRepository;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.repository.SysTagRepository;
import com.cybzacg.blogbackend.module.content.service.PublicContentQueryService;
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
    private static final String COMMENT_TYPE = "comment";
    private static final String LIKE_ACTION = "like";

    private final SysCategoryRepository sysCategoryRepository;
    private final SysTagRepository sysTagRepository;
    private final SysCommentRepository sysCommentRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final SysUserService sysUserService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public List<PublicCategoryTreeVO> listCategoryTree() {
        List<SysCategory> categories = sysCategoryRepository.findByTypeAndStatusOrderBySortOrderAndId(ARTICLE_TYPE, 1);
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
        return sysTagRepository.findByTargetType(actualTargetType).stream()
                .map(contentModelMapper::toPublicTagVO)
                .toList();
    }

    /**
     * 使用“根评论 + 回复”两段查询装配评论树，避免在高评论量场景下整表拉取同目标的所有记录。
     */
    @Override
    public List<PublicCommentVO> listComments(PublicCommentQuery query) {
        List<SysComment> roots = sysCommentRepository.selectRootCommentsByTarget(query.getTargetId(), query.getTargetType());
        if (roots.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = roots.stream().map(SysComment::getId).toList();
        List<SysComment> replies = sysCommentRepository.selectRepliesByRootIds(rootIds);
        List<SysComment> comments = new ArrayList<>(roots.size() + replies.size());
        comments.addAll(roots);
        comments.addAll(replies);

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

        List<PublicCommentVO> result = new ArrayList<>();
        for (SysComment root : roots) {
            PublicCommentVO rootVo = commentMap.get(root.getId());
            if (rootVo == null) {
                continue;
            }
            result.add(rootVo);
        }
        for (SysComment reply : replies) {
            PublicCommentVO replyVo = commentMap.get(reply.getId());
            if (replyVo == null) {
                continue;
            }
            PublicCommentVO parent = commentMap.get(reply.getParentId());
            if (parent != null) {
                parent.getChildren().add(replyVo);
            } else {
                result.add(replyVo);
            }
        }
        return result;
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
        return sysInteractionRepository.findByUserIdAndTargetTypeAndActionTypeInTargetIds(
                        currentUserId,
                        COMMENT_TYPE,
                        LIKE_ACTION,
                        commentIds)
                .stream()
                .map(SysInteraction::getTargetId)
                .collect(Collectors.toSet());
    }
}

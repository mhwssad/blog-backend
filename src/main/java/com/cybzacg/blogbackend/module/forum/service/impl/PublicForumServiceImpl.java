package com.cybzacg.blogbackend.module.forum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.domain.forum.ForumPostChannelLink;
import com.cybzacg.blogbackend.domain.forum.ForumReply;
import com.cybzacg.blogbackend.domain.forum.ForumSection;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.constant.ForumConstants;
import com.cybzacg.blogbackend.module.forum.model.publics.*;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumSectionRepository;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公开论坛查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class PublicForumServiceImpl implements com.cybzacg.blogbackend.module.forum.service.PublicForumService {
    private final ForumSectionRepository forumSectionRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final SysUserRepository sysUserRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ForumModelConvert forumModelConvert;

    @Override
    public List<ForumSectionVO> listSections() {
        boolean loginUser = SecurityUtils.getUserId() != null;
        Integer visibilityScope = loginUser ? 1 : 0;
        return forumSectionRepository.listPublicVisibleSections(visibilityScope).stream()
                .map(forumModelConvert::toSectionVO)
                .toList();
    }

    @Override
    public PageResult<PublicForumPostVO> pagePosts(ForumPostPageQuery query) {
        ForumPostPageQuery safeQuery = normalizeQuery(query);
        boolean loginUser = SecurityUtils.getUserId() != null;
        List<Long> visibleSectionIds = listVisibleSectionIds(loginUser);
        if (visibleSectionIds.isEmpty()) {
            return PageResult.empty(safeQuery.getCurrent(), safeQuery.getSize());
        }
        Page<ForumPost> page = forumPostRepository.pagePublicPosts(safeQuery, loginUser, visibleSectionIds);
        Map<Long, String> sectionNameMap = loadSectionNames(page.getRecords().stream()
                .map(ForumPost::getSectionId)
                .collect(Collectors.toSet()));
        Map<Long, String> authorNameMap = loadAuthorNames(page.getRecords().stream()
                .map(ForumPost::getAuthorId)
                .collect(Collectors.toSet()));
        List<PublicForumPostVO> records = page.getRecords().stream()
                .map(forumModelConvert::toPublicPostVO)
                .peek(vo -> {
                    vo.setSectionName(sectionNameMap.get(vo.getSectionId()));
                    vo.setAuthorName(authorNameMap.get(vo.getAuthorId()));
                })
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public PublicForumPostDetailVO getPost(Long id) {
        ForumPost post = getPublishedPostOrThrow(id);
        Long userId = SecurityUtils.getUserId();
        PublicForumPostDetailVO detailVO = forumModelConvert.toPublicPostDetailVO(post);
        detailVO.setSectionName(loadSectionNames(List.of(post.getSectionId())).get(post.getSectionId()));
        detailVO.setAuthorName(loadAuthorName(post.getAuthorId()));
        detailVO.setLiked(isPostLiked(id, userId));
        detailVO.setCollected(isPostCollected(id, userId));
        detailVO.setCanReply(userId != null && post.getStatus() != null && post.getStatus().equals(ForumPostStatusEnum.PUBLISHED.getValue()));
        detailVO.setLinkedChannel(loadLinkedChannel(id, userId));
        return detailVO;
    }

    @Override
    public PageResult<PublicForumReplyVO> pageReplies(Long postId, Long current, Long size) {
        ForumPost post = getPublishedPostOrThrow(postId);
        long safeCurrent = PaginationUtils.normalizeCurrent(current);
        long safeSize = PaginationUtils.normalizeSize(size, 10L, 100L);
        List<ForumReply> allReplies = forumReplyRepository.listByPostId(post.getId());
        Page<ForumReply> rootPage = forumReplyRepository.pageRootReplies(post.getId(), safeCurrent, safeSize);
        Map<Long, String> authorNameMap = loadAuthorNames(allReplies.stream()
                .map(ForumReply::getUserId)
                .collect(Collectors.toSet()));
        List<PublicForumReplyVO> records = rootPage.getRecords().stream()
                .map(reply -> buildReplyTree(reply, allReplies, authorNameMap))
                .toList();
        return PageResult.of(rootPage, records);
    }

    private ForumPost getPublishedPostOrThrow(Long id) {
        ForumPost post = forumPostRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(post == null, ResultErrorCode.NO_HANDLER_FOUND, "帖子不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(post.getStatus(), ForumPostStatusEnum.PUBLISHED.getValue()),
                ResultErrorCode.FORBIDDEN, "帖子未发布");
        Long userId = SecurityUtils.getUserId();
        boolean loginUser = userId != null;
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(post.getVisibilityScope(), ForumVisibilityScopeEnum.LOGIN_ONLY.getValue()) && !loginUser,
                ResultErrorCode.LOGIN_REQUIRED, "登录后才能查看帖子");
        ForumSection section = forumSectionRepository.getById(post.getSectionId());
        ExceptionThrowerCore.throwBusinessIf(section == null || !Integer.valueOf(1).equals(section.getStatus()),
                ResultErrorCode.NO_HANDLER_FOUND, "版块不存在或不可用");
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(section.getVisibilityScope(), ForumVisibilityScopeEnum.LOGIN_ONLY.getValue()) && !loginUser,
                ResultErrorCode.LOGIN_REQUIRED, "登录后才能查看版块内容");
        return post;
    }

    private ForumPostPageQuery normalizeQuery(ForumPostPageQuery query) {
        ForumPostPageQuery safeQuery = query == null ? new ForumPostPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), 10L, 100L));
        return safeQuery;
    }

    private List<Long> listVisibleSectionIds(boolean loginUser) {
        Integer visibilityScope = loginUser ? 1 : 0;
        return forumSectionRepository.listPublicVisibleSections(visibilityScope).stream()
                .map(ForumSection::getId)
                .toList();
    }

    private Map<Long, String> loadSectionNames(Collection<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return Map.of();
        }
        return forumSectionRepository.listByIds(sectionIds).stream()
                .collect(Collectors.toMap(ForumSection::getId, ForumSection::getName, (a, b) -> a));
    }

    private Map<Long, String> loadAuthorNames(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        sysUserRepository.listByIds(authorIds).forEach(user -> result.put(user.getId(), buildUserName(user)));
        return result;
    }

    private String loadAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        SysUser user = sysUserRepository.getById(authorId);
        return user == null ? null : buildUserName(user);
    }

    private String buildUserName(SysUser user) {
        return user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
    }

    private boolean isPostLiked(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        return sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(
                userId, postId, ForumConstants.TARGET_TYPE_POST, ForumConstants.ACTION_TYPE_LIKE);
    }

    private boolean isPostCollected(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        return sysCollectionRepository.existsByUserIdAndTargetTypeAndTargetId(
                userId, ForumConstants.TARGET_TYPE_POST, postId);
    }

    private ForumPostChannelLinkVO loadLinkedChannel(Long forumPostId, Long userId) {
        ForumPostChannelLink link = forumPostChannelLinkRepository.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ForumPostChannelLink>()
                        .eq(ForumPostChannelLink::getForumPostId, forumPostId),
                false);
        if (link == null) {
            return null;
        }
        ForumPostChannelLinkVO vo = new ForumPostChannelLinkVO();
        vo.setId(link.getId());
        vo.setForumPostId(link.getForumPostId());
        vo.setConversationId(link.getConversationId());
        vo.setLinkType(link.getLinkType());
        vo.setLinkedBy(link.getLinkedBy());
        vo.setLinkedAt(link.getLinkedAt());
        if (link.getConversationId() != null) {
            var conversation = chatConversationRepository.getById(link.getConversationId());
            vo.setChannelName(conversation == null ? null : conversation.getName());
        }
        return vo;
    }

    private PublicForumReplyVO toReplyVO(ForumReply reply, Map<Long, String> authorNameMap) {
        PublicForumReplyVO vo = forumModelConvert.toPublicReplyVO(reply);
        vo.setUserName(authorNameMap.get(reply.getUserId()));
        return vo;
    }

    private PublicForumReplyVO buildReplyTree(ForumReply reply,
                                              List<ForumReply> allReplies,
                                              Map<Long, String> authorNameMap) {
        PublicForumReplyVO vo = toReplyVO(reply, authorNameMap);
        List<PublicForumReplyVO> children = allReplies.stream()
                .filter(item -> Objects.equals(item.getParentId(), reply.getId()))
                .map(item -> buildReplyTree(item, allReplies, authorNameMap))
                .toList();
        vo.setChildren(children);
        return vo;
    }
}

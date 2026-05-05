package com.cybzacg.blogbackend.module.forum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.content.SysCollection;
import com.cybzacg.blogbackend.domain.content.SysCollectionFolder;
import com.cybzacg.blogbackend.domain.content.SysInteraction;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.domain.forum.ForumReply;
import com.cybzacg.blogbackend.domain.forum.ForumSection;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.conversation.service.ForumPostChannelLinkService;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.forum.constant.ForumConstants;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.user.*;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.service.UserForumService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户侧论坛服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserForumServiceImpl implements UserForumService {
    private final ForumSectionRepository forumSectionRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumReplyRepository forumReplyRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final SysCollectionRepository sysCollectionRepository;
    private final SysCollectionFolderRepository sysCollectionFolderRepository;
    private final UserExperienceService userExperienceService;
    private final ForumPostChannelLinkService forumPostChannelLinkService;
    private final ForumModelConvert forumModelConvert;
    private final NotificationDeliveryService notificationDeliveryService;

    @Override
    public PageResult<UserForumPostVO> pageMyPosts(UserForumPostPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        UserForumPostPageQuery safeQuery = normalizeQuery(query);
        Page<ForumPost> page = forumPostRepository.pageUserPosts(userId, safeQuery);
        return PageResult.of(page, page.getRecords().stream()
                .map(forumModelConvert::toUserPostVO)
                .toList());
    }

    @Override
    public UserForumPostDetailVO getMyPost(Long id) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireOwnedPost(id, userId);
        return forumModelConvert.toUserPostDetailVO(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserForumPostDetailVO createPost(ForumPostSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ForumSection section = requirePostableSection(request.getSectionId(), userId);
        ForumPost post = forumModelConvert.toPost(request);
        post.setSectionId(section.getId());
        post.setAuthorId(userId);
        post.setStatus(normalizeSaveStatus(request.getStatus()));
        post.setVisibilityScope(normalizeVisibilityScope(request.getVisibilityScope()));
        post.setIsTop(0);
        post.setIsEssence(0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setCollectCount(0);
        post.setShareCount(0);
        if (Objects.equals(post.getStatus(), ForumPostStatusEnum.PUBLISHED.getValue())) {
            post.setPublishedAt(LocalDateTime.now());
        }
        forumPostRepository.save(post);
        return forumModelConvert.toUserPostDetailVO(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserForumPostDetailVO updatePost(Long id, ForumPostSaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireOwnedPost(id, userId);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(post.getStatus(), ForumPostStatusEnum.DELETED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "已删除帖子不可编辑");
        ForumSection section = requirePostableSection(request.getSectionId(), userId);
        Integer previousStatus = post.getStatus();
        forumModelConvert.updatePost(request, post);
        post.setSectionId(section.getId());
        post.setStatus(normalizeSaveStatus(request.getStatus()));
        post.setVisibilityScope(normalizeVisibilityScope(request.getVisibilityScope()));
        if (!Objects.equals(previousStatus, ForumPostStatusEnum.PUBLISHED.getValue())
                && Objects.equals(post.getStatus(), ForumPostStatusEnum.PUBLISHED.getValue())) {
            post.setPublishedAt(LocalDateTime.now());
        }
        forumPostRepository.updateById(post);
        return forumModelConvert.toUserPostDetailVO(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireOwnedPost(id, userId);
        if (Objects.equals(post.getStatus(), ForumPostStatusEnum.DELETED.getValue())) {
            return;
        }
        forumPostRepository.softDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReply(Long postId, ForumReplySaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireReplyablePost(postId, userId);
        ForumReply reply = new ForumReply();
        reply.setPostId(post.getId());
        reply.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        reply.setUserId(userId);
        reply.setContent(StrUtils.trim(request.getContent()));
        reply.setStatus(ForumReplyStatusEnum.NORMAL.getValue());
        reply.setFloorNo(forumReplyRepository.nextFloorNo(post.getId()));
        reply.setLikeCount(0);
        reply.setReplyCount(0);
        if (reply.getParentId() != null && reply.getParentId() > 0) {
            ForumReply parent = requireNormalReply(reply.getParentId());
            ExceptionThrowerCore.throwBusinessIf(!Objects.equals(parent.getPostId(), post.getId()),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "父回复与帖子不匹配");
            reply.setRootId(parent.getRootId() == null || parent.getRootId() == 0L ? parent.getId() : parent.getRootId());
            forumReplyRepository.incrementReplyCount(parent.getId(), 1);
        } else {
            reply.setParentId(0L);
            reply.setRootId(0L);
        }
        forumReplyRepository.save(reply);
        forumPostRepository.incrementReplyCount(post.getId(), 1);

        // 通知帖子作者（非自己回自己的帖子）
        if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            notificationDeliveryService.deliverAfterCommit(
                    post.getAuthorId(),
                    NotificationTypeEnum.FORUM_REPLY_ME,
                    "你的帖子收到了回复",
                    "你的帖子「" + post.getTitle() + "」收到了新回复",
                    userId,
                    "forum_post", post.getId(), "/forum/posts/" + post.getId()
            );
        }
        // 通知父回复作者（非自己回自己、且非帖子作者的重复通知）
        if (reply.getParentId() != null && reply.getParentId() > 0) {
            ForumReply parentReply = forumReplyRepository.getById(reply.getParentId());
            if (parentReply != null && parentReply.getUserId() != null
                    && !parentReply.getUserId().equals(userId)
                    && !parentReply.getUserId().equals(post.getAuthorId())) {
                notificationDeliveryService.deliverAfterCommit(
                        parentReply.getUserId(),
                        NotificationTypeEnum.FORUM_REPLY_ME,
                        "你的回复收到了回复",
                        "你在帖子「" + post.getTitle() + "」中的回复收到了新回复",
                        userId,
                        "forum_post", post.getId(), "/forum/posts/" + post.getId()
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReply(Long replyId, ForumReplySaveRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ForumReply reply = requireOwnedReply(replyId, userId);
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(reply.getStatus(), ForumReplyStatusEnum.NORMAL.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "当前回复不可编辑");
        reply.setContent(StrUtils.trim(request.getContent()));
        forumReplyRepository.updateById(reply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReply(Long replyId) {
        Long userId = SecurityUtils.requireUserId();
        ForumReply reply = requireOwnedReply(replyId, userId);
        if (Objects.equals(reply.getStatus(), ForumReplyStatusEnum.DELETED.getValue())) {
            return;
        }
        forumReplyRepository.softDeleteById(replyId);
        forumPostRepository.incrementReplyCount(reply.getPostId(), -1);
        if (reply.getParentId() != null && reply.getParentId() > 0) {
            forumReplyRepository.incrementReplyCount(reply.getParentId(), -1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireInteractablePost(postId, userId, "点赞");
        boolean exists = sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(
                userId, postId, ForumConstants.TARGET_TYPE_POST, ForumConstants.ACTION_TYPE_LIKE);
        if (exists) {
            return;
        }
        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(userId);
        interaction.setTargetId(postId);
        interaction.setTargetType(ForumConstants.TARGET_TYPE_POST);
        interaction.setActionType(ForumConstants.ACTION_TYPE_LIKE);
        sysInteractionRepository.save(interaction);
        forumPostRepository.incrementLikeCount(postId, 1);

        // 通知帖子作者（非自己点赞自己的帖子）
        if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            notificationDeliveryService.deliverAfterCommit(
                    post.getAuthorId(),
                    NotificationTypeEnum.FORUM_LIKE_ME,
                    "你的帖子收到了点赞",
                    "你的帖子「" + post.getTitle() + "」收到了点赞",
                    userId,
                    "forum_post", post.getId(), "/forum/posts/" + post.getId()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long postId) {
        Long userId = SecurityUtils.requireUserId();
        SysInteraction interaction = sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(
                userId, postId, ForumConstants.TARGET_TYPE_POST, ForumConstants.ACTION_TYPE_LIKE);
        if (interaction == null) {
            return;
        }
        sysInteractionRepository.removeById(interaction.getId());
        forumPostRepository.incrementLikeCount(postId, -1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long postId, ForumPostCollectRequest request) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireInteractablePost(postId, userId, "收藏");
        Long folderId = request == null ? null : request.getFolderId();
        SysCollectionFolder folder = folderId == null ? getOrCreateDefaultForumFolder(userId) : requireOwnedForumFolder(folderId, userId);
        boolean exists = sysCollectionRepository.existsByUserIdAndFolderIdAndTargetIdAndTargetType(
                userId, folder.getId(), postId, ForumConstants.TARGET_TYPE_POST);
        if (exists) {
            return;
        }
        SysCollection collection = new SysCollection();
        collection.setUserId(userId);
        collection.setFolderId(folder.getId());
        collection.setTargetId(postId);
        collection.setTargetType(ForumConstants.TARGET_TYPE_POST);
        collection.setRemark(request == null ? null : StrUtils.normalize(request.getRemark()));
        collection.setTargetTitle(post.getTitle());
        collection.setTargetUrl("/forum/posts/" + post.getId());
        sysCollectionRepository.save(collection);
        sysCollectionFolderRepository.incrementCollectionCount(folder.getId(), 1);
        forumPostRepository.incrementCollectCount(postId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long postId) {
        Long userId = SecurityUtils.requireUserId();
        SysCollection collection = sysCollectionRepository.listByTargetTypeAndTargetId(ForumConstants.TARGET_TYPE_POST, postId)
                .stream()
                .filter(item -> Objects.equals(item.getUserId(), userId))
                .findFirst()
                .orElse(null);
        if (collection == null) {
            return;
        }
        sysCollectionRepository.removeById(collection.getId());
        sysCollectionFolderRepository.incrementCollectionCount(collection.getFolderId(), -1);
        forumPostRepository.incrementCollectCount(postId, -1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostChannelLinkVO sharePostToChannel(Long postId, Long conversationId) {
        Long userId = SecurityUtils.requireUserId();
        ForumPost post = requireInteractablePost(postId, userId, "分享");
        return forumPostChannelLinkService.sharePostToChannel(userId, post.getId(), conversationId);
    }

    private UserForumPostPageQuery normalizeQuery(UserForumPostPageQuery query) {
        UserForumPostPageQuery safeQuery = query == null ? new UserForumPostPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), 10L, 100L));
        return safeQuery;
    }

    private ForumSection requirePostableSection(Long sectionId, Long userId) {
        ForumSection section = forumSectionRepository.getById(sectionId);
        ExceptionThrowerCore.throwBusinessIf(section == null || !Integer.valueOf(1).equals(section.getStatus()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "论坛版块不存在或不可用");
        int requiredLevel = section.getPostLevelLimit() == null ? 1 : section.getPostLevelLimit();
        ExceptionThrowerCore.throwBusinessIf(!userExperienceService.checkLevelPermission(userId, requiredLevel),
                ResultErrorCode.FORBIDDEN, "用户等级不足，无法在该版块发帖");
        return section;
    }

    private ForumPost requireOwnedPost(Long id, Long userId) {
        ForumPost post = forumPostRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(post == null, ResultErrorCode.ILLEGAL_ARGUMENT, "帖子不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(post.getAuthorId(), userId),
                ResultErrorCode.FORBIDDEN, "只能操作自己的帖子");
        return post;
    }

    private ForumPost requireReplyablePost(Long postId, Long userId) {
        return requireInteractablePost(postId, userId, "回复");
    }

    private ForumPost requireInteractablePost(Long postId, Long userId, String actionName) {
        ForumPost post = forumPostRepository.getById(postId);
        ExceptionThrowerCore.throwBusinessIf(post == null, ResultErrorCode.ILLEGAL_ARGUMENT, "帖子不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(post.getStatus(), ForumPostStatusEnum.PUBLISHED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "当前帖子不可" + actionName);
        ExceptionThrowerCore.throwBusinessIf(Objects.equals(post.getVisibilityScope(), ForumVisibilityScopeEnum.LOGIN_ONLY.getValue()) && userId == null,
                ResultErrorCode.LOGIN_REQUIRED, "登录后才能" + actionName);
        return post;
    }

    private ForumReply requireNormalReply(Long replyId) {
        ForumReply reply = forumReplyRepository.getById(replyId);
        ExceptionThrowerCore.throwBusinessIf(reply == null || !Objects.equals(reply.getStatus(), ForumReplyStatusEnum.NORMAL.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "回复不存在或不可用");
        return reply;
    }

    private ForumReply requireOwnedReply(Long replyId, Long userId) {
        ForumReply reply = forumReplyRepository.getById(replyId);
        ExceptionThrowerCore.throwBusinessIf(reply == null, ResultErrorCode.ILLEGAL_ARGUMENT, "回复不存在");
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(reply.getUserId(), userId),
                ResultErrorCode.FORBIDDEN, "只能操作自己的回复");
        return reply;
    }

    private Integer normalizeSaveStatus(Integer status) {
        if (status == null) {
            return ForumPostStatusEnum.PUBLISHED.getValue();
        }
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(status, ForumPostStatusEnum.DRAFT.getValue())
                        && !Objects.equals(status, ForumPostStatusEnum.PUBLISHED.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "用户侧仅支持保存草稿或发布帖子");
        return status;
    }

    private Integer normalizeVisibilityScope(Integer visibilityScope) {
        if (visibilityScope == null) {
            return ForumVisibilityScopeEnum.PUBLIC.getValue();
        }
        ExceptionThrowerCore.throwBusinessIf(!Objects.equals(visibilityScope, ForumVisibilityScopeEnum.PUBLIC.getValue())
                        && !Objects.equals(visibilityScope, ForumVisibilityScopeEnum.LOGIN_ONLY.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "帖子可见范围非法");
        return visibilityScope;
    }

    private SysCollectionFolder getOrCreateDefaultForumFolder(Long userId) {
        SysCollectionFolder existing = sysCollectionFolderRepository.findDefaultByUserIdAndFolderType(userId, ForumConstants.TARGET_TYPE_POST);
        if (existing != null) {
            return existing;
        }
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setUserId(userId);
        folder.setFolderName(ForumConstants.DEFAULT_COLLECTION_FOLDER_NAME);
        folder.setFolderType(ForumConstants.TARGET_TYPE_POST);
        folder.setDescription(ForumConstants.DEFAULT_COLLECTION_FOLDER_DESCRIPTION);
        folder.setIsPublic(0);
        folder.setIsDefault(1);
        folder.setSortOrder(0);
        folder.setCollectionCount(0);
        sysCollectionFolderRepository.save(folder);
        return folder;
    }

    private SysCollectionFolder requireOwnedForumFolder(Long folderId, Long userId) {
        SysCollectionFolder folder = sysCollectionFolderRepository.getById(folderId);
        ExceptionThrowerCore.throwBusinessIf(folder == null
                        || !Objects.equals(folder.getUserId(), userId)
                        || !ForumConstants.TARGET_TYPE_POST.equals(folder.getFolderType()),
                ResultErrorCode.ILLEGAL_ARGUMENT, "论坛收藏夹不存在");
        return folder;
    }
}

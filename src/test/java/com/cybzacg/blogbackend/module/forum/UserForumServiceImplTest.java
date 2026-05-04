package com.cybzacg.blogbackend.module.forum;

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
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.conversation.service.ForumPostChannelLinkService;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.user.ForumPostCollectRequest;
import com.cybzacg.blogbackend.module.forum.model.user.ForumPostSaveRequest;
import com.cybzacg.blogbackend.module.forum.model.user.ForumReplySaveRequest;
import com.cybzacg.blogbackend.module.forum.model.user.UserForumPostDetailVO;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumReplyRepository;
import com.cybzacg.blogbackend.module.forum.repository.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.service.impl.UserForumServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserForumServiceImplTest {
    @Mock
    private ForumSectionRepository forumSectionRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private ForumReplyRepository forumReplyRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private SysCollectionFolderRepository sysCollectionFolderRepository;
    @Mock
    private UserExperienceService userExperienceService;
    @Mock
    private ForumPostChannelLinkService forumPostChannelLinkService;
    @Mock
    private ForumModelConvert forumModelConvert;

    private UserForumServiceImpl userForumService;

    @BeforeEach
    void setUp() {
        userForumService = new UserForumServiceImpl(
                forumSectionRepository,
                forumPostRepository,
                forumReplyRepository,
                sysInteractionRepository,
                sysCollectionRepository,
                sysCollectionFolderRepository,
                userExperienceService,
                forumPostChannelLinkService,
                forumModelConvert
        );
    }

    @Test
    void createPostShouldNormalizeDefaultsAndSave() {
        ForumPostSaveRequest request = postRequest();
        ForumSection section = section(10L);
        ForumPost post = new ForumPost();
        UserForumPostDetailVO detailVO = new UserForumPostDetailVO();
        detailVO.setId(20L);
        when(forumSectionRepository.getById(10L)).thenReturn(section);
        when(userExperienceService.checkLevelPermission(7L, 1)).thenReturn(true);
        when(forumModelConvert.toPost(request)).thenReturn(post);
        when(forumModelConvert.toUserPostDetailVO(post)).thenReturn(detailVO);

        UserForumPostDetailVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userForumService.createPost(request);
        }

        assertEquals(20L, result.getId());
        assertEquals(10L, post.getSectionId());
        assertEquals(7L, post.getAuthorId());
        assertEquals(ForumPostStatusEnum.PUBLISHED.getValue(), post.getStatus());
        assertEquals(ForumVisibilityScopeEnum.PUBLIC.getValue(), post.getVisibilityScope());
        assertEquals(0, post.getLikeCount());
        assertEquals(0, post.getReplyCount());
        verify(forumPostRepository).save(post);
    }

    @Test
    void createPostShouldRejectWhenLevelInsufficient() {
        ForumPostSaveRequest request = postRequest();
        ForumSection section = section(10L);
        section.setPostLevelLimit(3);
        when(forumSectionRepository.getById(10L)).thenReturn(section);
        when(userExperienceService.checkLevelPermission(7L, 3)).thenReturn(false);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userForumService.createPost(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        verify(forumPostRepository, never()).save(any(ForumPost.class));
    }

    @Test
    void updatePostShouldRejectOtherAuthor() {
        ForumPost existing = publishedPost(20L, 10L, 8L);
        when(forumPostRepository.getById(20L)).thenReturn(existing);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userForumService.updatePost(20L, postRequest()));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void createReplyShouldSaveRootReplyAndIncreasePostCount() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        ForumReplySaveRequest request = replyRequest(" 回复内容 ");
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumReplyRepository.nextFloorNo(20L)).thenReturn(3);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.createReply(20L, request);
        }

        ArgumentCaptor<ForumReply> captor = ArgumentCaptor.forClass(ForumReply.class);
        verify(forumReplyRepository).save(captor.capture());
        ForumReply reply = captor.getValue();
        assertEquals(20L, reply.getPostId());
        assertEquals(0L, reply.getParentId());
        assertEquals(0L, reply.getRootId());
        assertEquals(7L, reply.getUserId());
        assertEquals("回复内容", reply.getContent());
        assertEquals(ForumReplyStatusEnum.NORMAL.getValue(), reply.getStatus());
        assertEquals(3, reply.getFloorNo());
        verify(forumPostRepository).incrementReplyCount(20L, 1);
    }

    @Test
    void createReplyShouldSaveChildReplyAndIncreaseParentCount() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        ForumReply parent = reply(100L, 20L, 0L, 0L, 8L);
        ForumReplySaveRequest request = replyRequest("child");
        request.setParentId(100L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumReplyRepository.nextFloorNo(20L)).thenReturn(4);
        when(forumReplyRepository.getById(100L)).thenReturn(parent);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.createReply(20L, request);
        }

        ArgumentCaptor<ForumReply> captor = ArgumentCaptor.forClass(ForumReply.class);
        verify(forumReplyRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getRootId());
        verify(forumReplyRepository).incrementReplyCount(100L, 1);
        verify(forumPostRepository).incrementReplyCount(20L, 1);
    }

    @Test
    void deleteReplyShouldSoftDeleteAndRollbackCounts() {
        ForumReply reply = reply(100L, 20L, 50L, 50L, 7L);
        when(forumReplyRepository.getById(100L)).thenReturn(reply);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.deleteReply(100L);
        }

        verify(forumReplyRepository).softDeleteById(100L);
        verify(forumPostRepository).incrementReplyCount(20L, -1);
        verify(forumReplyRepository).incrementReplyCount(50L, -1);
    }

    @Test
    void likePostShouldBeIdempotentWhenAlreadyLiked() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 20L, "forum_post", "like"))
                .thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.likePost(20L);
        }

        verify(sysInteractionRepository, never()).save(any(SysInteraction.class));
        verify(forumPostRepository, never()).incrementLikeCount(20L, 1);
    }

    @Test
    void likePostShouldCreateInteractionAndIncreaseCount() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 20L, "forum_post", "like"))
                .thenReturn(false);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.likePost(20L);
        }

        ArgumentCaptor<SysInteraction> captor = ArgumentCaptor.forClass(SysInteraction.class);
        verify(sysInteractionRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(20L, captor.getValue().getTargetId());
        assertEquals("forum_post", captor.getValue().getTargetType());
        verify(forumPostRepository).incrementLikeCount(20L, 1);
    }

    @Test
    void collectPostShouldCreateDefaultFolderAndCollection() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        post.setTitle("标题");
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(sysCollectionFolderRepository.findDefaultByUserIdAndFolderType(7L, "forum_post")).thenReturn(null);
        when(sysCollectionFolderRepository.save(any(SysCollectionFolder.class))).thenAnswer(invocation -> {
            SysCollectionFolder saved = invocation.getArgument(0);
            saved.setId(30L);
            return true;
        });
        when(sysCollectionRepository.existsByUserIdAndFolderIdAndTargetIdAndTargetType(7L, 30L, 20L, "forum_post"))
                .thenReturn(false);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.collectPost(20L, new ForumPostCollectRequest());
        }

        ArgumentCaptor<SysCollection> captor = ArgumentCaptor.forClass(SysCollection.class);
        verify(sysCollectionRepository).save(captor.capture());
        assertEquals(30L, captor.getValue().getFolderId());
        assertEquals("标题", captor.getValue().getTargetTitle());
        assertEquals("/forum/posts/20", captor.getValue().getTargetUrl());
        verify(sysCollectionFolderRepository).incrementCollectionCount(30L, 1);
        verify(forumPostRepository).incrementCollectCount(20L, 1);
    }

    @Test
    void uncollectPostShouldRemoveOnlyCurrentUserCollection() {
        SysCollection other = collection(1L, 30L, 8L);
        SysCollection current = collection(2L, 31L, 7L);
        when(sysCollectionRepository.listByTargetTypeAndTargetId("forum_post", 20L)).thenReturn(List.of(other, current));

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userForumService.uncollectPost(20L);
        }

        verify(sysCollectionRepository).removeById(2L);
        verify(sysCollectionFolderRepository).incrementCollectionCount(31L, -1);
        verify(forumPostRepository).incrementCollectCount(20L, -1);
    }

    @Test
    void sharePostToChannelShouldValidatePostAndDelegate() {
        ForumPost post = publishedPost(20L, 10L, 8L);
        ForumPostChannelLinkVO linkVO = new ForumPostChannelLinkVO();
        linkVO.setConversationId(99L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L)).thenReturn(linkVO);

        ForumPostChannelLinkVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            result = userForumService.sharePostToChannel(20L, 99L);
        }

        assertEquals(99L, result.getConversationId());
        verify(forumPostChannelLinkService).sharePostToChannel(7L, 20L, 99L);
    }

    private ForumPostSaveRequest postRequest() {
        ForumPostSaveRequest request = new ForumPostSaveRequest();
        request.setSectionId(10L);
        request.setTitle("标题");
        request.setContent("内容");
        request.setStatus(ForumPostStatusEnum.PUBLISHED.getValue());
        request.setVisibilityScope(ForumVisibilityScopeEnum.PUBLIC.getValue());
        return request;
    }

    private ForumReplySaveRequest replyRequest(String content) {
        ForumReplySaveRequest request = new ForumReplySaveRequest();
        request.setContent(content);
        return request;
    }

    private ForumSection section(Long id) {
        ForumSection section = new ForumSection();
        section.setId(id);
        section.setStatus(1);
        section.setPostLevelLimit(1);
        return section;
    }

    private ForumPost publishedPost(Long id, Long sectionId, Long authorId) {
        ForumPost post = new ForumPost();
        post.setId(id);
        post.setSectionId(sectionId);
        post.setAuthorId(authorId);
        post.setStatus(ForumPostStatusEnum.PUBLISHED.getValue());
        post.setVisibilityScope(ForumVisibilityScopeEnum.PUBLIC.getValue());
        return post;
    }

    private ForumReply reply(Long id, Long postId, Long parentId, Long rootId, Long userId) {
        ForumReply reply = new ForumReply();
        reply.setId(id);
        reply.setPostId(postId);
        reply.setParentId(parentId);
        reply.setRootId(rootId);
        reply.setUserId(userId);
        reply.setStatus(ForumReplyStatusEnum.NORMAL.getValue());
        return reply;
    }

    private SysCollection collection(Long id, Long folderId, Long userId) {
        SysCollection collection = new SysCollection();
        collection.setId(id);
        collection.setFolderId(folderId);
        collection.setUserId(userId);
        collection.setTargetId(20L);
        collection.setTargetType("forum_post");
        return collection;
    }
}

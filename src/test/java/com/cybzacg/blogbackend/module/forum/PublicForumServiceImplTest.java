package com.cybzacg.blogbackend.module.forum;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.forum.ForumPost;
import com.cybzacg.blogbackend.dto.domain.forum.ForumReply;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumReplyStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ChatConversationRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysCollectionRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysInteractionRepository;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumPostPageQuery;
import com.cybzacg.blogbackend.module.forum.model.publics.ForumSectionVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostDetailVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumPostVO;
import com.cybzacg.blogbackend.module.forum.model.publics.PublicForumReplyVO;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumReplyRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.service.impl.PublicForumServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicForumServiceImplTest {
    @Mock
    private ForumSectionRepository forumSectionRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private ForumReplyRepository forumReplyRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    @Mock
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private ForumModelConvert forumModelConvert;

    private PublicForumServiceImpl publicForumService;

    @BeforeEach
    void setUp() {
        publicForumService = new PublicForumServiceImpl(
                forumSectionRepository,
                forumPostRepository,
                forumReplyRepository,
                sysUserRepository,
                sysInteractionRepository,
                sysCollectionRepository,
                forumPostChannelLinkRepository,
                chatConversationRepository,
                forumModelConvert
        );
    }

    @Test
    void listSectionsShouldUseGuestVisibilityWhenAnonymous() {
        ForumSection section = section(10L, "公开版块", ForumVisibilityScopeEnum.PUBLIC.getValue());
        ForumSectionVO vo = new ForumSectionVO();
        vo.setId(10L);
        vo.setName("公开版块");
        when(forumSectionRepository.listPublicVisibleSections(ForumVisibilityScopeEnum.PUBLIC.getValue())).thenReturn(List.of(section));
        when(forumModelConvert.toSectionVO(section)).thenReturn(vo);

        List<ForumSectionVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            result = publicForumService.listSections();
        }

        assertEquals(1, result.size());
        assertEquals("公开版块", result.get(0).getName());
    }

    @Test
    void pagePostsShouldReturnEmptyWhenNoVisibleSection() {
        when(forumSectionRepository.listPublicVisibleSections(ForumVisibilityScopeEnum.PUBLIC.getValue())).thenReturn(List.of());

        PageResult<PublicForumPostVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            result = publicForumService.pagePosts(new ForumPostPageQuery());
        }

        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        verify(forumPostRepository, never()).pagePublicPosts(any(), anyBoolean(), anyCollection());
    }

    @Test
    void getPostShouldRejectLoginOnlyPostForAnonymous() {
        ForumPost post = publishedPost(20L, 10L, 7L);
        post.setVisibilityScope(ForumVisibilityScopeEnum.LOGIN_ONLY.getValue());
        when(forumPostRepository.getById(20L)).thenReturn(post);

        BusinessException exception;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            exception = assertThrows(BusinessException.class, () -> publicForumService.getPost(20L));
        }

        assertEquals(ResultErrorCode.LOGIN_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void getPostShouldReturnDetailAndUserStatesForLoginUser() {
        ForumPost post = publishedPost(20L, 10L, 7L);
        ForumSection section = section(10L, "问答", ForumVisibilityScopeEnum.PUBLIC.getValue());
        SysUser author = new SysUser();
        author.setId(7L);
        author.setNickname("作者");
        PublicForumPostDetailVO detailVO = new PublicForumPostDetailVO();
        detailVO.setId(20L);
        detailVO.setSectionId(10L);
        detailVO.setAuthorId(7L);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumSectionRepository.getById(10L)).thenReturn(section);
        when(forumModelConvert.toPublicPostDetailVO(post)).thenReturn(detailVO);
        when(forumSectionRepository.listByIds(List.of(10L))).thenReturn(List.of(section));
        when(sysUserRepository.getById(7L)).thenReturn(author);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(9L, 20L, "forum_post", "like"))
                .thenReturn(true);
        when(sysCollectionRepository.existsByUserIdAndTargetTypeAndTargetId(9L, "forum_post", 20L)).thenReturn(false);

        PublicForumPostDetailVO result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(9L)) {
            result = publicForumService.getPost(20L);
        }

        assertEquals("问答", result.getSectionName());
        assertEquals("作者", result.getAuthorName());
        assertTrue(result.getLiked());
        assertFalse(result.getCollected());
        assertTrue(result.getCanReply());
    }

    @Test
    void pageRepliesShouldBuildReplyTree() {
        ForumPost post = publishedPost(20L, 10L, 7L);
        ForumSection section = section(10L, "问答", ForumVisibilityScopeEnum.PUBLIC.getValue());
        ForumReply root = reply(100L, 20L, 0L, 0L, 7L);
        ForumReply child = reply(101L, 20L, 100L, 100L, 8L);
        Page<ForumReply> rootPage = new Page<>(1, 10);
        rootPage.setTotal(1);
        rootPage.setRecords(List.of(root));
        SysUser rootUser = user(7L, "root");
        SysUser childUser = user(8L, "child");
        PublicForumReplyVO rootVO = replyVO(root);
        PublicForumReplyVO childVO = replyVO(child);
        when(forumPostRepository.getById(20L)).thenReturn(post);
        when(forumSectionRepository.getById(10L)).thenReturn(section);
        when(forumReplyRepository.listByPostId(20L)).thenReturn(List.of(root, child));
        when(forumReplyRepository.pageRootReplies(20L, 1L, 10L)).thenReturn(rootPage);
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(rootUser, childUser));
        when(forumModelConvert.toPublicReplyVO(root)).thenReturn(rootVO);
        when(forumModelConvert.toPublicReplyVO(child)).thenReturn(childVO);

        PageResult<PublicForumReplyVO> result;
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(null)) {
            result = publicForumService.pageReplies(20L, 1L, 10L);
        }

        assertEquals(1L, result.getTotal());
        assertEquals("root", result.getRecords().get(0).getUserName());
        assertEquals(1, result.getRecords().get(0).getChildren().size());
        assertEquals("child", result.getRecords().get(0).getChildren().get(0).getUserName());
    }

    private ForumSection section(Long id, String name, Integer visibilityScope) {
        ForumSection section = new ForumSection();
        section.setId(id);
        section.setName(name);
        section.setVisibilityScope(visibilityScope);
        section.setStatus(1);
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

    private PublicForumReplyVO replyVO(ForumReply reply) {
        PublicForumReplyVO vo = new PublicForumReplyVO();
        vo.setId(reply.getId());
        vo.setPostId(reply.getPostId());
        vo.setParentId(reply.getParentId());
        vo.setRootId(reply.getRootId());
        vo.setUserId(reply.getUserId());
        return vo;
    }

    private SysUser user(Long id, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setUsername("user" + id);
        return user;
    }
}

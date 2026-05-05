package com.cybzacg.blogbackend.module.forum;

import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.forum.ForumPost;
import com.cybzacg.blogbackend.domain.forum.ForumPostChannelLink;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumPostStatusEnum;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.module.chat.conversation.service.impl.ForumPostChannelLinkServiceImpl;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMuteGovernanceService;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.message.service.ChatMessageSendService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.forum.repository.ForumPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumPostChannelLinkServiceImplTest {
    @Mock
    private ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    @Mock
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private ChatConversationMemberRepository chatConversationMemberRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private ChatMuteGovernanceService chatMuteGovernanceService;
    @Mock
    private ChatMessageSendService chatMessageSendService;

    private ForumPostChannelLinkServiceImpl forumPostChannelLinkService;

    @BeforeEach
    void setUp() {
        forumPostChannelLinkService = new ForumPostChannelLinkServiceImpl(
                forumPostChannelLinkRepository,
                chatConversationRepository,
                chatConversationMemberRepository,
                forumPostRepository,
                chatMuteGovernanceService,
                chatMessageSendService
        );
    }

    @Test
    void sharePostToChannelShouldRejectUnpublishedPost() {
        ForumPost post = post(20L);
        post.setStatus(ForumPostStatusEnum.DRAFT.getValue());
        when(forumPostRepository.getById(20L)).thenReturn(post);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumPostChannelLinkRepository, never()).save(any(ForumPostChannelLink.class));
    }

    @Test
    void sharePostToChannelShouldRejectNonMember() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L));
        when(chatConversationRepository.getById(99L)).thenReturn(conversation(99L));
        when(chatConversationMemberRepository.findByConversationAndUser(99L, 7L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L));

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        verify(forumPostChannelLinkRepository, never()).save(any(ForumPostChannelLink.class));
    }

    @Test
    void sharePostToChannelShouldRejectMutedMember() {
        ChatConversationMember member = member(99L, 7L);
        member.setMuteUntil(LocalDateTime.now().plusMinutes(5));
        when(forumPostRepository.getById(20L)).thenReturn(post(20L));
        when(chatConversationRepository.getById(99L)).thenReturn(conversation(99L));
        when(chatConversationMemberRepository.findByConversationAndUser(99L, 7L)).thenReturn(member);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L));

        assertEquals(ResultErrorCode.CHAT_USER_MUTED.getCode(), exception.getCode());
        verify(forumPostChannelLinkRepository, never()).save(any(ForumPostChannelLink.class));
    }

    @Test
    void sharePostToChannelShouldReturnExistingLinkWhenSameChannel() {
        ForumPostChannelLink existing = link(1L, 20L, 99L);
        when(forumPostRepository.getById(20L)).thenReturn(post(20L));
        when(chatConversationRepository.getById(99L)).thenReturn(conversation(99L));
        when(chatConversationMemberRepository.findByConversationAndUser(99L, 7L)).thenReturn(member(99L, 7L));
        when(forumPostChannelLinkRepository.getOne(any(), eq(false))).thenReturn(existing);

        ForumPostChannelLinkVO result = forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L);

        assertEquals(1L, result.getId());
        assertEquals(99L, result.getConversationId());
        verify(forumPostChannelLinkRepository, never()).save(any(ForumPostChannelLink.class));
        verify(forumPostRepository, never()).incrementShareCount(20L, 1);
    }

    @Test
    void sharePostToChannelShouldCreateLinkAndIncreaseShareCount() {
        when(forumPostRepository.getById(20L)).thenReturn(post(20L));
        when(chatConversationRepository.getById(99L)).thenReturn(conversation(99L));
        when(chatConversationMemberRepository.findByConversationAndUser(99L, 7L)).thenReturn(member(99L, 7L));
        when(forumPostChannelLinkRepository.getOne(any(), eq(false))).thenReturn(null);

        ForumPostChannelLinkVO result = forumPostChannelLinkService.sharePostToChannel(7L, 20L, 99L);

        ArgumentCaptor<ForumPostChannelLink> captor = ArgumentCaptor.forClass(ForumPostChannelLink.class);
        verify(forumPostChannelLinkRepository).save(captor.capture());
        assertEquals(20L, captor.getValue().getForumPostId());
        assertEquals(99L, captor.getValue().getConversationId());
        assertEquals("manual_share", captor.getValue().getLinkType());
        assertEquals(7L, captor.getValue().getLinkedBy());
        assertEquals(99L, result.getConversationId());
        verify(forumPostRepository).incrementShareCount(20L, 1);
    }

    private ForumPost post(Long id) {
        ForumPost post = new ForumPost();
        post.setId(id);
        post.setStatus(ForumPostStatusEnum.PUBLISHED.getValue());
        post.setVisibilityScope(ForumVisibilityScopeEnum.PUBLIC.getValue());
        return post;
    }

    private ChatConversation conversation(Long id) {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(id);
        conversation.setName("频道");
        conversation.setStatus(ChatConstants.CONVERSATION_STATUS_NORMAL);
        return conversation;
    }

    private ChatConversationMember member(Long conversationId, Long userId) {
        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setStatus(ChatConstants.MEMBER_STATUS_NORMAL);
        return member;
    }

    private ForumPostChannelLink link(Long id, Long postId, Long conversationId) {
        ForumPostChannelLink link = new ForumPostChannelLink();
        link.setId(id);
        link.setForumPostId(postId);
        link.setConversationId(conversationId);
        link.setLinkType("manual_share");
        link.setLinkedBy(7L);
        return link;
    }
}

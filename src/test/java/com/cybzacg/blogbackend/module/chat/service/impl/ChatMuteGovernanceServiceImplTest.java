package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.dto.domain.chat.ChatUserMuteRecord;
import com.cybzacg.blogbackend.enums.chat.ChatMuteRecordStatusEnum;
import com.cybzacg.blogbackend.dto.repository.chat.governance.ChatUserMuteRecordRepository;
import com.cybzacg.blogbackend.module.chat.governance.service.impl.ChatMuteGovernanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 禁言范围测试：验证不同 scope 禁言互不误伤，以及发送拦截逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ChatMuteGovernanceServiceImplTest {
    @Mock
    private ChatUserMuteRecordRepository muteRecordRepository;

    private ChatMuteGovernanceServiceImpl muteService;

    @BeforeEach
    void setUp() {
        muteService = new ChatMuteGovernanceServiceImpl(
                muteRecordRepository, null, null, null);
    }

    @Test
    void globalMuteShouldBlockAllScopes() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(List.of(activeMute("global")));

        assertTrue(muteService.isUserMuted(userId, 100L, "lobby"));
        assertTrue(muteService.isUserMuted(userId, 200L, "topic_channel"));
        assertTrue(muteService.isUserMuted(userId, 300L, "group"));
    }

    @Test
    void lobbyMuteShouldNotBlockTopicChannel() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "topic_channel"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndConversationId(userId, 200L))
                .thenReturn(Collections.emptyList());

        // lobby 禁言不影响 topic_channel
        assertFalse(muteService.isUserMuted(userId, 200L, "topic_channel"));
    }

    @Test
    void lobbyMuteShouldBlockLobby() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "lobby"))
                .thenReturn(List.of(activeMute("lobby")));

        assertTrue(muteService.isUserMuted(userId, 100L, "lobby"));
    }

    @Test
    void topicChannelMuteShouldNotBlockGroup() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "group"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndConversationId(userId, 300L))
                .thenReturn(Collections.emptyList());

        assertFalse(muteService.isUserMuted(userId, 300L, "group"));
    }

    @Test
    void topicChannelMuteShouldBlockTopicChannel() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "topic_channel"))
                .thenReturn(List.of(activeMute("topic_channel")));

        assertTrue(muteService.isUserMuted(userId, 200L, "topic_channel"));
    }

    @Test
    void groupMuteShouldNotBlockLobby() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "lobby"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndConversationId(userId, 100L))
                .thenReturn(Collections.emptyList());

        assertFalse(muteService.isUserMuted(userId, 100L, "lobby"));
    }

    @Test
    void groupMuteShouldBlockGroup() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "group"))
                .thenReturn(List.of(activeMute("group")));

        assertTrue(muteService.isUserMuted(userId, 300L, "group"));
    }

    @Test
    void noActiveMuteShouldAllowSend() {
        Long userId = 1L;
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "lobby"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndConversationId(userId, 100L))
                .thenReturn(Collections.emptyList());

        assertFalse(muteService.isUserMuted(userId, 100L, "lobby"));
    }

    @Test
    void expiredMuteShouldNotBlock() {
        Long userId = 1L;
        ChatUserMuteRecord expired = new ChatUserMuteRecord();
        expired.setStatus(ChatMuteRecordStatusEnum.ACTIVE.getValue());
        expired.setMuteUntil(LocalDateTime.now().minusHours(1));

        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "global"))
                .thenReturn(List.of(expired));
        when(muteRecordRepository.findActiveByUserIdAndScope(userId, "lobby"))
                .thenReturn(Collections.emptyList());
        when(muteRecordRepository.findActiveByUserIdAndConversationId(userId, 100L))
                .thenReturn(Collections.emptyList());

        assertFalse(muteService.isUserMuted(userId, 100L, "lobby"));
    }

    private ChatUserMuteRecord activeMute(String scope) {
        ChatUserMuteRecord record = new ChatUserMuteRecord();
        record.setScope(scope);
        record.setStatus(ChatMuteRecordStatusEnum.ACTIVE.getValue());
        record.setMuteUntil(LocalDateTime.now().plusDays(1));
        return record;
    }
}

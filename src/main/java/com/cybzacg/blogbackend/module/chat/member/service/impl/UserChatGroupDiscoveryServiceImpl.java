package com.cybzacg.blogbackend.module.chat.member.service.impl;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.member.service.UserChatGroupDiscoveryService;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 用户侧群聊发现服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserChatGroupDiscoveryServiceImpl implements UserChatGroupDiscoveryService {
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final ChatConversationRepository chatConversationRepository;
    private final ChatModelMapper chatModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ChatGroupSearchVO> searchGroups(ChatGroupSearchQuery query) {
        Long userId = SecurityUtils.requireUserId();
        ChatGroupSearchQuery safeQuery = query == null ? new ChatGroupSearchQuery() : query;
        long current = PaginationUtils.normalizeCurrent(safeQuery.getCurrent());
        long size = PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        String keyword = StrUtils.trimToNull(safeQuery.getKeyword());
        String categoryCode = StrUtils.trimToNull(safeQuery.getCategoryCode());
        long total = Objects.requireNonNullElse(chatConversationRepository.countSearchableGroupPage(userId, keyword, categoryCode), 0L);
        if (total == 0L) {
            return PageResult.empty(current, size);
        }
        List<ChatConversationListItem> items = chatConversationRepository.selectSearchableGroupPage(
                userId,
                keyword,
                categoryCode,
                (current - 1) * size,
                size
        );
        return PageResult.of(total, current, size, items.stream().map(chatModelMapper::toGroupSearchVO).toList());
    }
}

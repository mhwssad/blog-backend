package com.cybzacg.blogbackend.module.chat.conversation.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.mapper.chat.ChatConversationMapper;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatConversationListItem;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天会话 Repository 实现。
 */
@Repository
public class ChatConversationRepositoryImpl extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countConversationPage(Long userId, String keyword) {
        return baseMapper.countConversationPage(userId, keyword);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationListItem> selectConversationPage(Long userId, String keyword, Long offset, Long size) {
        return baseMapper.selectConversationPage(userId, keyword, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatConversationListItem selectConversationDetail(Long conversationId, Long userId) {
        return baseMapper.selectConversationDetail(conversationId, userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countSearchableGroupPage(Long userId, String keyword, String categoryCode) {
        return baseMapper.countSearchableGroupPage(userId, keyword, categoryCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationListItem> selectSearchableGroupPage(Long userId, String keyword, String categoryCode, Long offset, Long size) {
        return baseMapper.selectSearchableGroupPage(userId, keyword, categoryCode, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countAdminConversationPage(ChatAdminConversationPageQuery query) {
        return baseMapper.countAdminConversationPage(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatAdminConversationListItem> selectAdminConversationPage(ChatAdminConversationPageQuery query, Long offset, Long size) {
        return baseMapper.selectAdminConversationPage(query, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatAdminConversationListItem selectAdminConversationDetail(Long conversationId) {
        return baseMapper.selectAdminConversationDetail(conversationId);
    }

    /**
     * 根据 singlePairKey 查找会话，按 ID 降序取最新一条，防止脏数据。
     */
    @Override
    public ChatConversation findBySinglePairKey(String singlePairKey) {
        return getOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getSinglePairKey, singlePairKey)
                .orderByDesc(ChatConversation::getId)
                .last("limit 1"), false);
    }

    /**
     * 查找全站广播会话，按 ID 降序取最新一条以确保唯一性。
     */
    @Override
    public ChatConversation findGlobalConversation() {
        return getOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getIsAllSite, 1)
                .orderByDesc(ChatConversation::getId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countNormalGroupsByOwner(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }
        return count(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getOwnerId, ownerId)
                .eq(ChatConversation::getConversationType, ChatConstants.CONVERSATION_TYPE_GROUP)
                .eq(ChatConversation::getIsAllSite, 0)
                .eq(ChatConversation::getStatus, ChatConstants.CONVERSATION_STATUS_NORMAL));
    }
}

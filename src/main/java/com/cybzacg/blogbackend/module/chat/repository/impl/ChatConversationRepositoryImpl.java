package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatConversation;
import com.cybzacg.blogbackend.mapper.ChatConversationMapper;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminConversationListItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatConversationListItem;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天会话 Repository 实现。
 */
@Repository
public class ChatConversationRepositoryImpl extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationRepository {

    @Override
    public Long countConversationPage(Long userId, String keyword) {
        return baseMapper.countConversationPage(userId, keyword);
    }

    @Override
    public List<ChatConversationListItem> selectConversationPage(Long userId, String keyword, Long offset, Long size) {
        return baseMapper.selectConversationPage(userId, keyword, offset, size);
    }

    @Override
    public ChatConversationListItem selectConversationDetail(Long conversationId, Long userId) {
        return baseMapper.selectConversationDetail(conversationId, userId);
    }

    @Override
    public Long countAdminConversationPage(ChatAdminConversationPageQuery query) {
        return baseMapper.countAdminConversationPage(query);
    }

    @Override
    public List<ChatAdminConversationListItem> selectAdminConversationPage(ChatAdminConversationPageQuery query, Long offset, Long size) {
        return baseMapper.selectAdminConversationPage(query, offset, size);
    }

    @Override
    public ChatAdminConversationListItem selectAdminConversationDetail(Long conversationId) {
        return baseMapper.selectAdminConversationDetail(conversationId);
    }

    @Override
    public ChatConversation findBySinglePairKey(String singlePairKey) {
        return getOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getSinglePairKey, singlePairKey)
                .orderByDesc(ChatConversation::getId)
                .last("limit 1"), false);
    }

    @Override
    public ChatConversation findGlobalConversation() {
        return getOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getIsAllSite, 1)
                .orderByDesc(ChatConversation::getId)
                .last("limit 1"), false);
    }
}

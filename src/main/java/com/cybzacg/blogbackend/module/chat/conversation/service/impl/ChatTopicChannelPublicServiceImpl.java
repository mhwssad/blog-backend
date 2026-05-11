package com.cybzacg.blogbackend.module.chat.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ChatConversationRepository;
import com.cybzacg.blogbackend.dto.repository.chat.member.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatTopicChannelPublicService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatTopicChannelPublicServiceImpl implements ChatTopicChannelPublicService {
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;

    @Override
    public PageResult<ChatConversationVO> pageChannels(Long current, Long size, String categoryCode) {
        long currentVal = PaginationUtils.normalizeCurrent(current);
        long sizeVal = PaginationUtils.normalizeSize(size, 20L, 100L);

        LambdaQueryWrapper<ChatConversation> countWrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getSceneType, ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                .eq(ChatConversation::getStatus, ChatConstants.CONVERSATION_STATUS_NORMAL)
                .eq(ChatConversation::getVisibilityScope, ChatConstants.VISIBILITY_SCOPE_PUBLIC);
        if (StrUtils.hasText(categoryCode)) {
            countWrapper.eq(ChatConversation::getChannelCategoryCode, categoryCode);
        }
        long total = chatConversationRepository.count(countWrapper);
        if (total == 0L) {
            return PageResult.empty(currentVal, sizeVal);
        }

        LambdaQueryWrapper<ChatConversation> queryWrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getSceneType, ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                .eq(ChatConversation::getStatus, ChatConstants.CONVERSATION_STATUS_NORMAL)
                .eq(ChatConversation::getVisibilityScope, ChatConstants.VISIBILITY_SCOPE_PUBLIC)
                .orderByAsc(ChatConversation::getDisplaySort)
                .orderByDesc(ChatConversation::getLastMessageTime)
                .last("LIMIT " + sizeVal + " OFFSET " + (currentVal - 1) * sizeVal);
        if (StrUtils.hasText(categoryCode)) {
            queryWrapper.eq(ChatConversation::getChannelCategoryCode, categoryCode);
        }
        List<ChatConversation> channels = chatConversationRepository.list(queryWrapper);

        List<ChatConversationVO> records = channels.stream().map(ch -> {
            ChatConversationVO vo = new ChatConversationVO();
            vo.setId(ch.getId());
            vo.setConversationType(ch.getConversationType());
            vo.setSceneType(ch.getSceneType());
            vo.setName(ch.getName());
            vo.setAvatar(ch.getAvatar());
            vo.setNotice(ch.getAnnouncement());
            vo.setStatus(ch.getStatus());
            vo.setVisibilityScope(ch.getVisibilityScope());
            vo.setJoinRule(ch.getJoinRule());
            vo.setSpeakLevelLimit(ch.getSpeakLevelLimit());
            vo.setSlowModeSeconds(ch.getSlowModeSeconds());
            vo.setDisplaySort(ch.getDisplaySort());
            vo.setChannelCategoryCode(ch.getChannelCategoryCode());
            vo.setCreatedAt(ch.getCreatedAt());
            vo.setUpdatedAt(ch.getUpdatedAt());
            return vo;
        }).toList();

        return PageResult.of(total, currentVal, sizeVal, records);
    }

    @Override
    public ChatConversationVO getChannel(Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "频道ID不能为空");
        ChatConversation channel = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(channel == null
                        || !Objects.equals(channel.getSceneType(), ChatConstants.SCENE_TYPE_TOPIC_CHANNEL)
                        || !Objects.equals(channel.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT, "频道不存在或不可用");
        // Only show public channels to non-members
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(channel.getVisibilityScope(), ChatConstants.VISIBILITY_SCOPE_PUBLIC),
                ResultErrorCode.FORBIDDEN, "该频道不可公开访问");

        List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(conversationId);
        ChatConversationVO vo = new ChatConversationVO();
        vo.setId(channel.getId());
        vo.setConversationType(channel.getConversationType());
        vo.setSceneType(channel.getSceneType());
        vo.setName(channel.getName());
        vo.setAvatar(channel.getAvatar());
        vo.setNotice(channel.getAnnouncement());
        vo.setStatus(channel.getStatus());
        vo.setVisibilityScope(channel.getVisibilityScope());
        vo.setJoinRule(channel.getJoinRule());
        vo.setSpeakLevelLimit(channel.getSpeakLevelLimit());
        vo.setSlowModeSeconds(channel.getSlowModeSeconds());
        vo.setDisplaySort(channel.getDisplaySort());
        vo.setChannelCategoryCode(channel.getChannelCategoryCode());
        vo.setMemberCount((long) activeMembers.size());
        vo.setCreatedAt(channel.getCreatedAt());
        vo.setUpdatedAt(channel.getUpdatedAt());
        return vo;
    }
}

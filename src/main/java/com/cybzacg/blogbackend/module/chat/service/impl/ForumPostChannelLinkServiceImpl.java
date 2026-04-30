package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.forum.ForumPostChannelLink;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.repository.ForumPostChannelLinkRepository;
import com.cybzacg.blogbackend.module.chat.service.ForumPostChannelLinkService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ForumPostChannelLinkServiceImpl implements ForumPostChannelLinkService {
    private final ForumPostChannelLinkRepository forumPostChannelLinkRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostChannelLinkVO sharePostToChannel(Long userId, Long forumPostId, Long conversationId) {
        ExceptionThrowerCore.throwBusinessIfNull(forumPostId, ResultErrorCode.ILLEGAL_ARGUMENT, "帖子ID不能为空");
        ExceptionThrowerCore.throwBusinessIfNull(conversationId, ResultErrorCode.ILLEGAL_ARGUMENT, "频道ID不能为空");

        // 校验会话存在且状态正常
        ChatConversation conversation = chatConversationRepository.getById(conversationId);
        ExceptionThrowerCore.throwBusinessIf(conversation == null
                        || !Objects.equals(conversation.getStatus(), ChatConstants.CONVERSATION_STATUS_NORMAL),
                ResultErrorCode.ILLEGAL_ARGUMENT, "频道不存在或不可用");

        // 校验当前用户是该频道正常成员
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(conversationId, userId);
        ExceptionThrowerCore.throwBusinessIf(member == null
                        || !Objects.equals(member.getStatus(), ChatConstants.MEMBER_STATUS_NORMAL),
                ResultErrorCode.FORBIDDEN, "当前用户不在该频道中");

        // 校验禁言状态
        boolean muted = member.getMuteUntil() != null && member.getMuteUntil().isAfter(LocalDateTime.now());
        ExceptionThrowerCore.throwBusinessIf(muted, ResultErrorCode.FORBIDDEN, "当前用户已被禁言");

        // 检查帖子是否已关联频道（阶段一：一个帖子只能关联一个频道）
        ForumPostChannelLink existing = findPostLink(forumPostId);
        if (existing != null) {
            // 幂等：已关联到同一频道时直接返回
            if (Objects.equals(existing.getConversationId(), conversationId)) {
                return toVO(existing, conversation.getName());
            }
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "一个帖子只能关联一个频道");
        }

        // 创建关联
        ForumPostChannelLink link = new ForumPostChannelLink();
        link.setForumPostId(forumPostId);
        link.setConversationId(conversationId);
        link.setLinkType("manual_share");
        link.setLinkedBy(userId);
        link.setLinkedAt(LocalDateTime.now());
        try {
            forumPostChannelLinkRepository.save(link);
        } catch (DuplicateKeyException ex) {
            // 并发插入 - 获取已有记录
            link = findPostLink(forumPostId);
        }

        return toVO(link, conversation.getName());
    }

    @Override
    public ForumPostChannelLinkVO getPostLinkedChannel(Long forumPostId) {
        ForumPostChannelLink link = findPostLink(forumPostId);
        if (link == null) {
            return null;
        }
        ChatConversation channel = chatConversationRepository.getById(link.getConversationId());
        return toVO(link, channel != null ? channel.getName() : null);
    }

    @Override
    public PageResult<ForumPostChannelLinkVO> pageChannelLinks(Long conversationId, Long current, Long size) {
        long currentVal = PaginationUtils.normalizeCurrent(current);
        long sizeVal = PaginationUtils.normalizeSize(size, 20L, 100L);

        LambdaQueryWrapper<ForumPostChannelLink> countWrapper = new LambdaQueryWrapper<ForumPostChannelLink>()
                .eq(ForumPostChannelLink::getConversationId, conversationId);
        long total = forumPostChannelLinkRepository.count(countWrapper);
        if (total == 0L) {
            return PageResult.empty(currentVal, sizeVal);
        }

        LambdaQueryWrapper<ForumPostChannelLink> queryWrapper = new LambdaQueryWrapper<ForumPostChannelLink>()
                .eq(ForumPostChannelLink::getConversationId, conversationId)
                .orderByDesc(ForumPostChannelLink::getLinkedAt)
                .last("LIMIT " + sizeVal + " OFFSET " + (currentVal - 1) * sizeVal);
        List<ForumPostChannelLink> links = forumPostChannelLinkRepository.list(queryWrapper);

        List<ForumPostChannelLinkVO> records = links.stream()
                .map(link -> toVO(link, null))
                .toList();

        return PageResult.of(total, currentVal, sizeVal, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkPost(Long userId, Long forumPostId) {
        ForumPostChannelLink link = findPostLink(forumPostId);
        ExceptionThrowerCore.throwBusinessIfNull(link, ResultErrorCode.ILLEGAL_ARGUMENT, "帖子未关联任何频道");
        // 仅关联人或管理员可取消关联
        forumPostChannelLinkRepository.removeById(link.getId());
    }

    private ForumPostChannelLink findPostLink(Long forumPostId) {
        return forumPostChannelLinkRepository.getOne(
                new LambdaQueryWrapper<ForumPostChannelLink>()
                        .eq(ForumPostChannelLink::getForumPostId, forumPostId),
                false);
    }

    private ForumPostChannelLinkVO toVO(ForumPostChannelLink link, String channelName) {
        ForumPostChannelLinkVO vo = new ForumPostChannelLinkVO();
        vo.setId(link.getId());
        vo.setForumPostId(link.getForumPostId());
        vo.setConversationId(link.getConversationId());
        vo.setChannelName(channelName);
        vo.setLinkType(link.getLinkType());
        vo.setLinkedBy(link.getLinkedBy());
        vo.setLinkedAt(link.getLinkedAt());
        return vo;
    }
}

package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatConversationMember;

import java.util.Collection;
import java.util.List;

/**
 * 聊天会话成员 Repository。
 */
public interface ChatConversationMemberRepository extends IService<ChatConversationMember> {
    ChatConversationMember findByConversationAndUser(Long conversationId, Long userId);

    ChatConversationMember findOwnerByConversationId(Long conversationId);

    List<ChatConversationMember> listActiveByConversationId(Long conversationId);

    List<ChatConversationMember> listActiveByConversationIds(Collection<Long> conversationIds);

    List<ChatConversationMember> listByConversationId(Long conversationId);

    List<ChatConversationMember> listByConversationIds(Collection<Long> conversationIds);

    boolean removeAllActiveMembers(Long conversationId);

    /**
     * 单调递增更新成员的已投递游标（CAS 语义）。
     * 仅当当前 lastDeliveredMessageId 为空或小于目标值时才更新。
     *
     * @param id 成员记录 ID
     * @param messageId 目标已投递消息 ID
     * @param deliveredAt 投递时间
     * @return 是否更新成功
     */
    boolean advanceDeliveredState(Long id, Long messageId, java.util.Date deliveredAt);
}

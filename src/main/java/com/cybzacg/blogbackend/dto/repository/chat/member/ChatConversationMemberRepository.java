package com.cybzacg.blogbackend.module.chat.member.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversationMember;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 聊天会话成员 Repository。<p>
 * 封装会话成员实体的持久化操作，包括成员查询、批量查询、状态变更及投递游标推进。
 */
public interface ChatConversationMemberRepository extends IService<ChatConversationMember> {

    /**
     * 根据会话 ID 和用户 ID 查找成员记录。
     */
    ChatConversationMember findByConversationAndUser(Long conversationId, Long userId);

    /**
     * 查找指定会话的群主（Owner）成员记录。
     */
    ChatConversationMember findOwnerByConversationId(Long conversationId);

    /**
     * 查询指定会话中状态为正常的成员列表。
     */
    List<ChatConversationMember> listActiveByConversationId(Long conversationId);

    /**
     * 批量查询多个会话中状态为正常的成员列表。
     */
    List<ChatConversationMember> listActiveByConversationIds(Collection<Long> conversationIds);

    /**
     * 查询指定会话的全部成员列表（含所有状态）。
     */
    List<ChatConversationMember> listByConversationId(Long conversationId);

    /**
     * 批量查询多个会话的全部成员列表。
     */
    List<ChatConversationMember> listByConversationIds(Collection<Long> conversationIds);

    /**
     * 将指定会话中所有正常状态的成员标记为已移除。
     */
    boolean removeAllActiveMembers(Long conversationId);

    /**
     * 统计指定会话中状态为正常的成员数量。
     */
    long countActiveByConversationId(Long conversationId);

    /**
     * 单调递增更新成员的已投递游标（CAS 语义）。
     * 仅当当前 lastDeliveredMessageId 为空或小于目标值时才更新。
     *
     * @param id          成员记录 ID
     * @param messageId   目标已投递消息 ID
     * @param deliveredAt 投递时间
     * @return 是否更新成功
     */
    boolean advanceDeliveredState(Long id, Long messageId, LocalDateTime deliveredAt);
}

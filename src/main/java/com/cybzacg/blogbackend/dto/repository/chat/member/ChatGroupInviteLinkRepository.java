package com.cybzacg.blogbackend.dto.repository.chat.member;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.chat.ChatGroupInviteLink;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupInviteLinkPageQuery;

/**
 * 群聊邀请链接 Repository。
 */
public interface ChatGroupInviteLinkRepository extends IService<ChatGroupInviteLink> {

    /**
     * 根据邀请令牌查询邀请链接。
     */
    ChatGroupInviteLink findByToken(String inviteToken);

    /**
     * 分页查询指定群的邀请链接。
     */
    Page<ChatGroupInviteLink> pageByConversationId(Long conversationId, ChatGroupInviteLinkPageQuery query);
}

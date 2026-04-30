package com.cybzacg.blogbackend.module.chat.member.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupInviteLinkCreateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupInviteLinkPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupInviteLinkVO;

/**
 * 用户侧群邀请链接服务。
 */
public interface UserChatGroupInviteLinkService {

    /**
     * 群主或管理员创建邀请链接。
     */
    ChatGroupInviteLinkVO createInviteLink(Long conversationId, ChatGroupInviteLinkCreateRequest request);

    /**
     * 群主或管理员分页查询邀请链接。
     */
    PageResult<ChatGroupInviteLinkVO> pageInviteLinks(Long conversationId, ChatGroupInviteLinkPageQuery query);

    /**
     * 群主或管理员停用邀请链接。
     */
    void disableInviteLink(Long conversationId, Long inviteLinkId);

    /**
     * 当前登录用户通过邀请链接加入群聊。
     */
    void joinByInviteToken(String inviteToken);
}

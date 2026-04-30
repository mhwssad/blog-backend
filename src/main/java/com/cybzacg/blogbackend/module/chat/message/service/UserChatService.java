package com.cybzacg.blogbackend.module.chat.message.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.*;
import com.cybzacg.blogbackend.module.chat.member.model.user.*;
import com.cybzacg.blogbackend.module.chat.message.model.user.*;

import java.util.List;

/**
 * 用户侧聊天服务。
 */
public interface UserChatService {
    PageResult<ChatConversationVO> pageMyConversations(ChatConversationPageQuery query);

    ChatConversationVO getMyConversation(Long conversationId);

    ChatConversationVO openSingleConversation(ChatOpenSingleConversationRequest request);

    PageResult<ChatMessageVO> pageMyMessages(Long conversationId, ChatMessagePageQuery query);

    ChatMessageVO sendTextMessage(ChatSendTextRequest request);

    ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request);

    ChatMessageVO sendFileMessage(ChatSendFileRequest request);

    ChatMessageVO editMessage(Long messageId, ChatEditMessageRequest request);

    void revokeMessage(Long messageId);

    void deleteMessage(Long messageId);

    ChatReadStateVO markRead(Long conversationId, ChatMarkReadRequest request);

    ChatReadStateVO markRead(Long userId, Long conversationId, Long readMessageId);

    ChatConversationVO createGroup(ChatCreateGroupRequest request);

    ChatConversationVO getGroupDetail(Long conversationId);

    List<ChatMemberVO> listGroupMembers(Long conversationId);

    List<ChatMemberVO> inviteGroupMembers(Long conversationId, ChatGroupMemberOperateRequest request);

    List<ChatMemberVO> appointGroupAdmin(Long conversationId, Long memberUserId);

    List<ChatMemberVO> removeGroupAdmin(Long conversationId, Long memberUserId);

    ChatConversationVO transferGroupOwner(Long conversationId, ChatTransferGroupOwnerRequest request);

    List<ChatMemberVO> muteGroupMember(Long conversationId, Long memberUserId, ChatMuteMemberRequest request);

    ChatConversationVO updateGroupNotice(Long conversationId, ChatGroupNoticeUpdateRequest request);

    void removeGroupMember(Long conversationId, Long memberUserId);

    void leaveGroup(Long conversationId);

    void dissolveGroup(Long conversationId);

    /**
     * 访客查看大厅频道消息（无需登录）。
     */
    PageResult<ChatLobbyMessageVO> pageLobbyMessages(Long current, Long size, Long beforeMessageId);

    /**
     * 加入公开频道或公开群（仅 join_rule = free 的会话）。
     */
    ChatConversationVO joinConversation(Long conversationId);

    /**
     * 离开频道或公开群（非群主均可退出）。
     */
    void leaveConversation(Long conversationId);
}

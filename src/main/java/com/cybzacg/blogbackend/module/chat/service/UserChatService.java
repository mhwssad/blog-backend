package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatEditMessageRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupNoticeUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMarkReadRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMuteMemberRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatOpenSingleConversationRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendFileRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatTransferGroupOwnerRequest;
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
}

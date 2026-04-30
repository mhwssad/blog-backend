package com.cybzacg.blogbackend.module.chat.member.service;

import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatCreateGroupRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupNoticeUpdateRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatTransferGroupOwnerRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMuteMemberRequest;

import java.util.List;

/**
 * 群组管理子服务：创建群、成员管理、角色变更、群设置等。
 */
public interface ChatGroupManageService {

    ChatConversationVO createGroup(Long userId, ChatCreateGroupRequest request);

    ChatConversationVO getGroupDetail(Long userId, Long conversationId);

    List<ChatMemberVO> listGroupMembers(Long userId, Long conversationId);

    List<ChatMemberVO> inviteGroupMembers(Long userId, Long conversationId, ChatGroupMemberOperateRequest request);

    List<ChatMemberVO> appointGroupAdmin(Long userId, Long conversationId, Long memberUserId);

    List<ChatMemberVO> removeGroupAdmin(Long userId, Long conversationId, Long memberUserId);

    ChatConversationVO transferGroupOwner(Long userId, Long conversationId, ChatTransferGroupOwnerRequest request);

    List<ChatMemberVO> muteGroupMember(Long userId, Long conversationId, Long memberUserId, ChatMuteMemberRequest request);

    ChatConversationVO updateGroupNotice(Long userId, Long conversationId, ChatGroupNoticeUpdateRequest request);

    void removeGroupMember(Long userId, Long conversationId, Long memberUserId);

    void leaveGroup(Long userId, Long conversationId);

    void dissolveGroup(Long userId, Long conversationId);
}

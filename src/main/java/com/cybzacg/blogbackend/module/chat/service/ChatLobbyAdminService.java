package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatLobbySettingsUpdateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatLobbyPinnedMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;

import java.util.List;

/**
 * 大厅频道后台管理服务。
 */
public interface ChatLobbyAdminService {

    /**
     * 更新大厅频道设置（公告、慢速模式、发言等级）。
     *
     * @param request 设置更新请求
     * @return 更新后的大厅会话信息
     */
    ChatConversationVO updateLobbySettings(ChatLobbySettingsUpdateRequest request);

    /**
     * 置顶大厅消息。
     *
     * @param messageId 消息ID
     */
    void pinMessage(Long messageId);

    /**
     * 取消置顶大厅消息。
     *
     * @param messageId 消息ID
     */
    void unpinMessage(Long messageId);

    /**
     * 分页查询大厅置顶消息。
     *
     * @param current 页码
     * @param size    每页大小
     * @return 置顶消息分页结果
     */
    PageResult<ChatLobbyPinnedMessageVO> pagePinnedMessages(Long current, Long size);

    /**
     * 禁言大厅用户。
     *
     * @param memberUserId 被禁言的用户ID
     * @param request      禁言请求（含截止时间）
     * @return 更新后的活跃成员列表
     */
    List<ChatMemberVO> muteLobbyMember(Long memberUserId, ChatAdminMemberMuteUpdateRequest request);

    /**
     * 踢出大厅用户（将成员状态设为已移除）。
     *
     * @param memberUserId 被踢出的用户ID
     * @return 更新后的活跃成员列表
     */
    List<ChatMemberVO> kickLobbyMember(Long memberUserId);
}

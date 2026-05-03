package com.cybzacg.blogbackend.module.chat.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatLobbySettingsUpdateRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatLobbyPinnedMessageVO;
import com.cybzacg.blogbackend.module.chat.conversation.repository.ChatConversationRepository;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatLobbyAdminService;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatConversationMemberRepository;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelConvert;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.UserDisplayNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 大厅频道后台管理服务实现。
 *
 * <p>负责大厅频道设置更新、消息置顶/取消置顶、成员禁言与踢出等运营操作。
 */
@Service
@RequiredArgsConstructor
public class ChatLobbyAdminServiceImpl implements ChatLobbyAdminService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationMemberRepository chatConversationMemberRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatModelConvert chatModelConvert;
    private final ChatPushService chatPushService;

    /**
     * 更新大厅频道设置（公告、慢速模式、发言等级），变更后通过 WebSocket 推送通知所有在线成员。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversationVO updateLobbySettings(ChatLobbySettingsUpdateRequest request) {
        ChatConversation conversation = requireLobbyConversation();
        boolean changed = false;
        if (request.getAnnouncement() != null) {
            conversation.setAnnouncement(request.getAnnouncement());
            changed = true;
        }
        if (request.getSlowModeSeconds() != null) {
            conversation.setSlowModeSeconds(request.getSlowModeSeconds());
            changed = true;
        }
        if (request.getSpeakLevelLimit() != null) {
            conversation.setSpeakLevelLimit(request.getSpeakLevelLimit());
            changed = true;
        }
        if (changed) {
            chatConversationRepository.updateById(conversation);
            List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(conversation.getId());
            List<Long> userIds = activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList();
            chatPushService.pushConversationUpdated(
                    ChatWsConversationUpdatedPayload.builder()
                            .action("lobby_settings_updated")
                            .conversationId(conversation.getId())
                            .conversationType(conversation.getConversationType())
                            .name(conversation.getName())
                            .avatar(conversation.getAvatar())
                            .notice(conversation.getAnnouncement())
                            .status(conversation.getStatus())
                            .memberCount((long) activeMembers.size())
                            .build(),
                    userIds
            );
        }
        return buildConversationVO(conversation);
    }

    /**
     * 置顶大厅消息，幂等操作（已置顶则跳过）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinMessage(Long messageId) {
        ChatConversation lobby = requireLobbyConversation();
        ChatMessage message = requireMessage(lobby.getId(), messageId);
        if (message.getPinnedBy() != null) {
            return;
        }
        message.setPinnedBy(0L);
        chatMessageRepository.updateById(message);
        List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(lobby.getId());
        chatPushService.pushMessageUpdated(
                buildMessageUpdatedVO(message),
                activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList()
        );
    }

    /**
     * 取消置顶大厅消息，幂等操作（未置顶则跳过）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinMessage(Long messageId) {
        ChatConversation lobby = requireLobbyConversation();
        ChatMessage message = requireMessage(lobby.getId(), messageId);
        if (message.getPinnedBy() == null) {
            return;
        }
        message.setPinnedBy(null);
        chatMessageRepository.updateById(message);
        List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(lobby.getId());
        chatPushService.pushMessageUpdated(
                buildMessageUpdatedVO(message),
                activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList()
        );
    }

    /**
     * 分页查询大厅置顶消息，按消息ID倒序排列。
     */
    @Override
    public PageResult<ChatLobbyPinnedMessageVO> pagePinnedMessages(Long current, Long size) {
        ChatConversation lobby = requireLobbyConversation();
        long currentVal = PaginationUtils.normalizeCurrent(current);
        long sizeVal = PaginationUtils.normalizeSize(size, 20L, 100L);

        LambdaQueryWrapper<ChatMessage> countWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, lobby.getId())
                .isNotNull(ChatMessage::getPinnedBy)
                .eq(ChatMessage::getRevokeStatus, ChatConstants.REVOKE_STATUS_NORMAL);
        long total = chatMessageRepository.count(countWrapper);

        if (total == 0L) {
            return PageResult.empty(currentVal, sizeVal);
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, lobby.getId())
                .isNotNull(ChatMessage::getPinnedBy)
                .eq(ChatMessage::getRevokeStatus, ChatConstants.REVOKE_STATUS_NORMAL)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + sizeVal + " OFFSET " + (currentVal - 1) * sizeVal);
        List<ChatMessage> messages = chatMessageRepository.list(queryWrapper);

        Map<Long, SysUser> userMap = loadUsers(messages.stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<ChatLobbyPinnedMessageVO> records = messages.stream().map(msg -> {
            ChatLobbyPinnedMessageVO vo = new ChatLobbyPinnedMessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            SysUser sender = userMap.get(msg.getSenderId());
            vo.setSenderName(UserDisplayNameUtils.resolveDisplayName(sender, msg.getSenderId()));
            vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
            vo.setMessageType(msg.getMessageType());
            vo.setContent(msg.getContent());
            vo.setPinnedBy(msg.getPinnedBy());
            vo.setCreatedAt(msg.getCreatedAt());
            return vo;
        }).toList();

        return PageResult.of(total, currentVal, sizeVal, records);
    }

    /**
     * 禁言大厅用户，设置禁言截止时间后推送成员变更通知。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> muteLobbyMember(Long memberUserId, ChatAdminMemberMuteUpdateRequest request) {
        ChatConversation lobby = requireLobbyConversation();
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(lobby.getId(), memberUserId);
        ExceptionThrowerCore.throwBusinessIfNull(member, ResultErrorCode.ILLEGAL_ARGUMENT, "成员不存在");
        LocalDateTime muteUntil = request == null ? null : request.getMuteUntil();
        member.setMuteUntil(muteUntil != null && muteUntil.isAfter(LocalDateTime.now()) ? muteUntil : null);
        chatConversationMemberRepository.updateById(member);
        List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(lobby.getId());
        List<ChatMemberVO> records = buildMemberRecords(activeMembers);
        chatPushService.pushMembersUpdated(
                ChatWsMembersUpdatedPayload.builder()
                        .action("admin_lobby_member_mute_updated")
                        .conversationId(lobby.getId())
                        .affectedUserId(memberUserId)
                        .members(records)
                        .build(),
                activeMembers.stream().map(ChatConversationMember::getUserId).distinct().toList()
        );
        return records;
    }

    /**
     * 踢出大厅用户，将成员状态设为已移除并清除禁言时间，推送成员变更通知。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChatMemberVO> kickLobbyMember(Long memberUserId) {
        ChatConversation lobby = requireLobbyConversation();
        ChatConversationMember member = chatConversationMemberRepository.findByConversationAndUser(lobby.getId(), memberUserId);
        ExceptionThrowerCore.throwBusinessIfNull(member, ResultErrorCode.ILLEGAL_ARGUMENT, "成员不存在");
        member.setStatus(ChatConstants.MEMBER_STATUS_REMOVED);
        member.setMuteUntil(null);
        chatConversationMemberRepository.updateById(member);
        List<Long> notifyUserIds = new ArrayList<>(
                chatConversationMemberRepository.listActiveByConversationId(lobby.getId())
                        .stream().map(ChatConversationMember::getUserId).toList()
        );
        if (!notifyUserIds.contains(memberUserId)) {
            notifyUserIds.add(memberUserId);
        }
        List<ChatConversationMember> activeMembers = chatConversationMemberRepository.listActiveByConversationId(lobby.getId());
        List<ChatMemberVO> records = buildMemberRecords(activeMembers);
        chatPushService.pushMembersUpdated(
                ChatWsMembersUpdatedPayload.builder()
                        .action("admin_lobby_member_kicked")
                        .conversationId(lobby.getId())
                        .affectedUserId(memberUserId)
                        .members(records)
                        .build(),
                notifyUserIds
        );
        return records;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取大厅频道会话，不存在则抛出业务异常。
     */
    private ChatConversation requireLobbyConversation() {
        ChatConversation conversation = chatConversationRepository.findGlobalConversation();
        ExceptionThrowerCore.throwBusinessIfNull(conversation, ResultErrorCode.ILLEGAL_ARGUMENT, "大厅频道不存在");
        return conversation;
    }

    /**
     * 获取指定会话中的消息，校验消息归属和存在性。
     */
    private ChatMessage requireMessage(Long conversationId, Long messageId) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        ChatMessage message = chatMessageRepository.getById(messageId);
        ExceptionThrowerCore.throwBusinessIf(
                message == null || !Objects.equals(message.getConversationId(), conversationId),
                ResultErrorCode.ILLEGAL_ARGUMENT, "消息不存在"
        );
        return message;
    }

    /**
     * 构建消息更新推送 VO。
     */
    private ChatMessageVO buildMessageUpdatedVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setMessageType(message.getMessageType());
        vo.setContent(message.getContent());
        vo.setRevoked(Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED));
        vo.setCreatedAt(message.getCreatedAt());
        vo.setUpdatedAt(message.getUpdatedAt());
        return vo;
    }

    /**
     * 从会话实体构建 ChatConversationVO。
     */
    private ChatConversationVO buildConversationVO(ChatConversation conversation) {
        ChatConversationVO vo = new ChatConversationVO();
        vo.setId(conversation.getId());
        vo.setConversationType(conversation.getConversationType());
        vo.setSceneType(conversation.getSceneType());
        vo.setName(conversation.getName());
        vo.setAvatar(conversation.getAvatar());
        vo.setNotice(conversation.getAnnouncement());
        vo.setAllSite(conversation.getIsAllSite() != null && conversation.getIsAllSite() == 1);
        vo.setStatus(conversation.getStatus());
        vo.setVisibilityScope(conversation.getVisibilityScope());
        vo.setAllowGuestView(conversation.getAllowGuestView());
        vo.setRequireJoinToSpeak(conversation.getRequireJoinToSpeak());
        vo.setJoinRule(conversation.getJoinRule());
        vo.setSpeakLevelLimit(conversation.getSpeakLevelLimit());
        vo.setMemberLimit(conversation.getMemberLimit());
        vo.setSlowModeSeconds(conversation.getSlowModeSeconds());
        vo.setDisplaySort(conversation.getDisplaySort());
        vo.setChannelCategoryCode(conversation.getChannelCategoryCode());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    /**
     * 批量加载用户信息。
     */
    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        sysUserRepository.listByIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        return userMap;
    }

    /**
     * 构建成员 VO 列表，按角色排序并补齐用户信息。
     */
    private List<ChatMemberVO> buildMemberRecords(List<ChatConversationMember> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = loadUsers(members.stream()
                .map(ChatConversationMember::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<ChatMemberVO> records = new ArrayList<>();
        members.stream()
                .sorted(Comparator.comparingInt(this::memberRoleOrder)
                        .thenComparing(ChatConversationMember::getStatus, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatConversationMember::getJoinedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ChatConversationMember::getUserId, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(member -> {
                    ChatMemberVO vo = chatModelConvert.toMemberVO(member);
                    SysUser user = userMap.get(member.getUserId());
                    vo.setUserId(member.getUserId());
                    vo.setUsername(user != null ? user.getUsername() : null);
                    vo.setNickname(user != null ? user.getNickname() : null);
                    vo.setAvatar(user != null ? user.getAvatar() : null);
                    records.add(vo);
                });
        return records;
    }

    /**
     * 成员角色排序权重：owner=0, admin=1, 其他=2。
     */
    private int memberRoleOrder(ChatConversationMember member) {
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_OWNER)) {
            return 0;
        }
        if (Objects.equals(member.getMemberRole(), ChatConstants.MEMBER_ROLE_ADMIN)) {
            return 1;
        }
        return 2;
    }
}

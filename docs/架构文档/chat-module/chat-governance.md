# 治理功能

## 1. 概述

Chat 模块的治理功能涵盖了禁言管理、举报处理和后台管理接口，确保社区秩序和内容安全。

## 2. 禁言体系

### 2.1 禁言范围

| 范围 | scope | 说明 |
|------|-------|------|
| 全站禁言 | global | 用户在所有聊天室都被禁言 |
| 大厅禁言 | lobby | 用户在大厅频道被禁言 |
| 话题频道禁言 | topic_channel | 用户在指定话题频道被禁言 |
| 群组禁言 | group | 用户在指定群组被禁言 |

### 2.2 禁言来源

| 来源 | sourceType | 说明 |
|------|-------------|------|
| 管理员操作 | admin | 管理员手动禁言 |
| 举报处理 | report | 通过举报系统处理后的禁言 |
| 自动禁言 | auto | 系统自动检测后禁言（如发广告） |

### 2.3 禁言校验

```java
// ChatMuteGovernanceServiceImpl.isUserMuted()
public boolean isUserMuted(Long userId, Long conversationId, String scope) {
    // 1. 查询用户的有效禁言记录
    ChatUserMuteRecord muteRecord = chatUserMuteRecordRepository
        .findActiveMute(userId, scope, conversationId);

    if (muteRecord == null) {
        return false;
    }

    // 2. 校验禁言是否过期
    if (muteRecord.getMuteUntil() != null &&
        muteRecord.getMuteUntil().isBefore(LocalDateTime.now())) {
        // 已过期，更新状态
        muteRecord.setStatus(0); // 已解除
        chatUserMuteRecordRepository.updateById(muteRecord);
        return false;
    }

    return true;
}
```

### 2.4 创建禁言

```java
// ChatMuteGovernanceServiceImpl.createMute()
public ChatMuteRecordVO createMute(ChatMuteCreateRequest request, Long operatorId) {
    // 1. 校验禁言范围
    if ("global".equals(request.getScope())) {
        // 全站禁言不需要会话ID
    } else if (request.getConversationId() == null) {
        throwBusinessEx("非全站禁言需要指定会话ID");
    }

    // 2. 检查是否已有生效中的禁言
    ChatUserMuteRecord existing = chatUserMuteRecordRepository
        .findActiveMute(request.getUserId(), request.getScope(), request.getConversationId());
    if (existing != null) {
        throwBusinessEx("该用户已被禁言");
    }

    // 3. 创建禁言记录
    ChatUserMuteRecord muteRecord = ChatUserMuteRecord.builder()
        .userId(request.getUserId())
        .scope(request.getScope())
        .conversationId(request.getConversationId())
        .muteUntil(request.getMuteUntil())
        .status(1) // 生效中
        .reason(request.getReason())
        .sourceType(request.getSourceType())
        .reportId(request.getReportId())
        .operatorId(operatorId)
        .build();

    chatUserMuteRecordRepository.insert(muteRecord);

    // 4. 如果是群组禁言，同时更新成员表的禁言时间
    if ("group".equals(request.getScope()) && request.getConversationId() != null) {
        ChatConversationMember member = findMember(request.getConversationId(), request.getUserId());
        if (member != null) {
            member.setMuteUntil(request.getMuteUntil());
            chatConversationMemberRepository.updateById(member);
        }
    }

    return convertToVO(muteRecord);
}
```

### 2.5 解除禁言

```java
// ChatMuteGovernanceServiceImpl.releaseMute()
public void releaseMute(Long recordId, Long operatorId) {
    // 1. 获取禁言记录
    ChatUserMuteRecord muteRecord = chatUserMuteRecordRepository.selectById(recordId);
    if (muteRecord == null) {
        throwBusinessEx("禁言记录不存在");
    }

    // 2. 校验状态
    if (muteRecord.getStatus() != 1) {
        throwBusinessEx("该禁言已解除");
    }

    // 3. 更新状态
    muteRecord.setStatus(0); // 已解除
    muteRecord.setReleasedBy(operatorId);
    muteRecord.setReleasedAt(LocalDateTime.now());
    chatUserMuteRecordRepository.updateById(muteRecord);

    // 4. 如果是群组禁言，清除成员表的禁言时间
    if ("group".equals(muteRecord.getScope()) && muteRecord.getConversationId() != null) {
        ChatConversationMember member = findMember(
            muteRecord.getConversationId(), muteRecord.getUserId());
        if (member != null) {
            member.setMuteUntil(null);
            chatConversationMemberRepository.updateById(member);
        }
    }
}
```

## 3. 举报处理

### 3.1 举报来源禁言

```java
// ChatMuteGovernanceServiceImpl.createMuteFromReport()
public ChatMuteRecordVO createMuteFromReport(Long userId, String scope,
    Long conversationId, String reason, Long reportId, Long operatorId,
    LocalDateTime muteUntil) {

    return createMute(ChatMuteCreateRequest.builder()
        .userId(userId)
        .scope(scope)
        .conversationId(conversationId)
        .muteUntil(muteUntil)
        .reason(reason)
        .sourceType("report")
        .reportId(reportId)
        .operatorId(operatorId)
        .build(), operatorId);
}
```

### 3.2 举报处理流程

```
举报创建 → 举报审核 → 判定处罚 → 执行禁言（如需要）
   ↓          ↓           ↓            ↓
ReportService  Admin    管理员决策   创建禁言记录
                           ↓
                     推送处罚通知
```

## 4. 后台管理接口

### 4.1 会话管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sys/chats/conversations` | GET | 分页查询会话 |
| `/api/sys/chats/conversations/{id}` | GET | 查询会话详情 |
| `/api/sys/chats/conversations/{id}/status` | PUT | 更新会话状态 |

### 4.2 成员管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sys/chats/conversations/{id}/members` | GET | 查询会话成员 |
| `/api/sys/chats/conversations/{id}/members/{userId}/role` | PUT | 更新成员角色 |
| `/api/sys/chats/conversations/{id}/members/{userId}/status` | PUT | 更新成员状态 |
| `/api/sys/chats/conversations/{id}/members/{userId}/mute` | PUT | 更新成员禁言 |

### 4.3 消息管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sys/chats/conversations/{id}/messages` | GET | 分页查询会话消息 |
| `/api/sys/chats/conversations/{id}/messages/{msgId}` | GET | 查询消息详情 |
| `/api/sys/chats/conversations/{id}/messages/{msgId}/receipts` | GET | 查询消息回执 |
| `/api/sys/chats/conversations/{id}/messages/{msgId}/revoke` | POST | 后台撤回消息 |

### 4.4 禁言管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sys/chat/mutes` | GET | 分页查询禁言记录 |
| `/api/sys/chat/mutes/{id}/release` | POST | 解除禁言 |

### 4.5 大厅/频道管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sys/chat/lobby/settings` | PUT | 更新大厅设置 |
| `/api/sys/chat/lobby/pin/{messageId}` | POST | 置顶大厅消息 |
| `/api/sys/chat/lobby/unpin/{messageId}` | DELETE | 取消置顶大厅消息 |
| `/api/sys/chat/lobby/pinned-messages` | GET | 分页查询置顶消息 |
| `/api/sys/chat/lobby/kick/{userId}` | POST | 踢出大厅用户 |
| `/api/sys/chat/topic-channels` | GET/POST/PUT/DELETE | 话题频道管理 |

## 5. 消息治理

### 5.1 后台撤回消息

```java
// ChatAdminServiceImpl.revokeMessage()
public void revokeMessage(Long conversationId, Long messageId) {
    // 1. 获取消息
    ChatMessage message = chatMessageRepository.selectById(messageId);
    if (message == null) {
        throwBusinessEx("消息不存在");
    }

    // 2. 校验消息所属会话
    if (!message.getConversationId().equals(conversationId)) {
        throwBusinessEx("消息不属于该会话");
    }

    // 3. 更新撤回状态
    message.setRevokeStatus(1); // 已撤回
    message.setRevokedBy(0L); // 后台撤回
    message.setRevokedAt(LocalDateTime.now());
    chatMessageRepository.updateById(message);

    // 4. 推送撤回通知给所有在线成员
    List<Long> memberUserIds = listActiveUserIds(conversationId);
    chatPushService.pushMessageRevoked(
        buildMessageVO(messageId, ...),
        memberUserIds
    );
}
```

### 5.2 消息可见性管理

```java
// ChatMessageGovernanceServiceImpl.updateMessageVisibleStatus()
public void updateMessageVisibleStatus(Long messageId, Long userId, Integer visibleStatus) {
    // 1. 获取消息接收记录
    ChatMessageRecipient recipient = chatMessageRecipientRepository
        .findByMessageAndRecipient(messageId, userId);

    if (recipient == null) {
        throwBusinessEx("接收记录不存在");
    }

    // 2. 更新可见状态
    recipient.setVisibleStatus(visibleStatus);
    chatMessageRecipientRepository.updateById(recipient);
}
```

## 6. 大厅治理

### 6.1 大厅设置更新

```java
// ChatLobbyAdminServiceImpl.updateLobbySettings()
public ChatConversationVO updateLobbySettings(ChatLobbySettingsUpdateRequest request) {
    // 1. 获取大厅会话
    ChatConversation lobby = chatConversationRepository
        .findBySceneType("hall_channel");

    // 2. 更新设置
    if (request.getAnnouncement() != null) {
        lobby.setAnnouncement(request.getAnnouncement());
    }
    if (request.getSlowModeSeconds() != null) {
        lobby.setSlowModeSeconds(request.getSlowModeSeconds());
    }
    if (request.getSpeakLevelLimit() != null) {
        lobby.setSpeakLevelLimit(request.getSpeakLevelLimit());
    }

    chatConversationRepository.updateById(lobby);

    // 3. 推送更新通知
    chatPushService.pushConversationUpdated(
        ChatWsConversationUpdatedPayload.builder()
            .conversationId(lobby.getId())
            .updatedField("settings")
            .build(),
        listActiveUserIds(lobby.getId())
    );

    return getConversationVO(0L, lobby.getId());
}
```

### 6.2 踢出大厅用户

```java
// ChatLobbyAdminServiceImpl.kickLobbyMember()
public List<ChatMemberVO> kickLobbyMember(Long memberUserId) {
    // 1. 获取大厅会话
    ChatConversation lobby = chatConversationRepository
        .findBySceneType("hall_channel");

    // 2. 获取成员
    ChatConversationMember member = findMember(lobby.getId(), memberUserId);
    if (member == null) {
        throwBusinessEx("该用户不在大厅");
    }

    // 3. 不能踢出管理员
    if (!"member".equals(member.getMemberRole())) {
        throwBusinessEx("不能踢出管理员");
    }

    // 4. 更新成员状态
    member.setStatus(2); // 已移除
    chatConversationMemberRepository.updateById(member);

    // 5. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(lobby.getId())
            .action("member_kicked")
            .kickedUserId(memberUserId)
            .build(),
        listActiveUserIds(lobby.getId())
    );

    return buildMemberRecords(listActiveMembers(lobby.getId()));
}
```

## 7. 统计指标

### 7.1 ChatMetricsService

```java
// ChatMetricsService
public interface ChatMetricsService {
    /**
     * 获取会话统计数据
     */
    ChatMetricsVO getConversationMetrics(Long conversationId);

    /**
     * 获取用户聊天统计数据
     */
    ChatMetricsVO getUserChatMetrics(Long userId);

    /**
     * 获取全站聊天统计数据
     */
    ChatMetricsVO getSiteChatMetrics();
}
```

### 7.2 统计维度

| 维度 | 说明 |
|------|------|
| 消息数 | 今日/本周/本月/总消息数 |
| 活跃用户数 | 今日/本周/本月/活跃用户数 |
| 会话数 | 现有会话数/历史总会话数 |
| 入群/退群数 | 成员变动统计 |

## 8. 权限控制

### 8.1 后台接口权限

| 权限标识 | 说明 |
|----------|------|
| `content:chat:query` | 聊天查询权限 |
| `content:chat:update` | 聊天更新权限（禁言、踢人等） |
| `content:chat:update-status` | 会话状态管理权限 |

### 8.2 权限校验注解

```java
// ChatAdminController
@GetMapping("/conversations")
@PreAuthorize("@permission.hasPermission('content:chat:query')")
public Result<PageResult<ChatAdminConversationVO>> pageConversations(
    ChatAdminConversationPageQuery query) {
    return Result.success(chatAdminService.pageConversations(query));
}

@PutMapping("/conversations/{conversationId}/status")
@PreAuthorize("@permission.hasPermission('content:chat:update-status')")
public Result<Void> updateConversationStatus(@PathVariable Long conversationId,
    @Valid @RequestBody ChatConversationStatusUpdateRequest request) {
    chatAdminService.updateConversationStatus(conversationId, request.getStatus());
    return Result.success();
}
```

## 9. 相关文档

- [Chat 模块总览](./00-chat-module-overview.md)
- [Chat 数据模型](./chat-data-model.md)
- [消息收发流程](./chat-message-flow.md)
- [会话管理流程](./chat-conversation-flow.md)
- [群组管理流程](./chat-group-management.md)

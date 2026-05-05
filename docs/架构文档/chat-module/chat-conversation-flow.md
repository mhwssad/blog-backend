# 会话管理流程

## 1. 概述

Chat 模块的会话管理涵盖了单聊、群聊、大厅频道、话题频道的创建、查询和管理的完整流程。

## 2. 会话类型

| 类型 | conversationType | sceneType | 说明 |
|------|------------------|-----------|------|
| 单聊 | single | single_chat | 用户间一对一聊天 |
| 用户群聊 | group | user_group | 用户创建的群组 |
| 大厅频道 | group | hall_channel | 全站公开的大厅 |
| 话题频道 | group | topic_channel | 按话题分类的公开频道 |
| 全站频道 | group | global_channel | 全站级别的特殊频道 |

## 3. 单聊流程

### 3.1 打开或创建单聊

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ Controller  │────▶│   Service   │────▶│  Repository │
│             │     │             │     │             │     │             │
│openSingle   │     │UserChatCtrl │     │UserChatService│  │ChatConversation│
│Conversation │     │             │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

### 3.2 单聊会话创建逻辑

```java
// UserChatServiceImpl.openSingleConversation()
public ChatConversationVO openSingleConversation(ChatOpenSingleConversationRequest request) {
    Long userId = request.getUserId();      // 当前用户
    Long targetUserId = request.getTargetUserId(); // 对方用户

    // 1. 生成单聊唯一标识（双方用户ID排序后拼接的哈希）
    String singlePairKey = buildSinglePairKey(userId, targetUserId);

    // 2. 查询是否已存在单聊会话
    ChatConversation existing = chatConversationRepository
        .findBySinglePairKey(singlePairKey);

    if (existing != null) {
        // 3a. 会话已存在，直接返回
        return getMyConversation(userId, existing.getId());
    }

    // 3b. 会话不存在，创建新会话
    ChatConversation conversation = ChatConversation.builder()
        .conversationType("single")
        .sceneType("single_chat")
        .singlePairKey(singlePairKey)
        .name("")  // 单聊不显示名称，显示对方昵称
        .status(1) // 正常
        .build();

    chatConversationRepository.insert(conversation);

    // 4. 创建双方成员记录
    upsertConversationMembership(conversation, userId, "member", "search");
    upsertConversationMembership(conversation, targetUserId, "member", "search");

    return getMyConversation(userId, conversation.getId());
}
```

### 3.3 单聊唯一标识生成

```java
// ChatServiceSupport.buildSinglePairKey()
public String buildSinglePairKey(Long userId1, Long userId2) {
    // 确保 ID 顺序一致，无论谁先发起
    long minId = Math.min(userId1, userId2);
    long maxId = Math.max(userId1, userId2);
    return String.format("%d_%d", minId, maxId);
}
```

## 4. 群聊流程

### 4.1 创建群聊

```java
// ChatGroupManageServiceImpl.createGroup()
public ChatConversationVO createGroup(Long userId, ChatCreateGroupRequest request) {
    // 1. 构建群聊会话
    ChatConversation conversation = ChatConversation.builder()
        .conversationType("group")
        .sceneType("user_group")
        .name(request.getName())
        .avatar(request.getAvatar())
        .ownerId(userId)
        .visibilityScope(request.getVisibilityScope() != null ?
            request.getVisibilityScope() : "private")
        .joinRule(request.getJoinRule() != null ?
            request.getJoinRule() : "approval")
        .memberLimit(request.getMemberLimit() != null ?
            request.getMemberLimit() : 0)
        .status(1)
        .build();

    chatConversationRepository.insert(conversation);

    // 2. 创建者作为群主加入
    upsertConversationMembership(conversation, userId, "owner", "create");

    // 3. 如果有初始成员，邀请加入
    if (CollectionUtils.isNotEmpty(request.getMemberIds())) {
        for (Long memberId : request.getMemberIds()) {
            if (!memberId.equals(userId)) {
                upsertConversationMembership(conversation, memberId, "member", "invite");
            }
        }
    }

    return getGroupDetail(userId, conversation.getId());
}
```

### 4.2 群聊加入规则

| 规则 | joinRule | 说明 |
|------|----------|------|
| 自由加入 | free | 任何人可以直接加入 |
| 审批加入 | approval | 需要群主/管理员审批 |
| 仅邀请 | invite_only | 只能通过邀请链接或邀请加入 |

## 5. 大厅/话题频道流程

### 5.1 大厅频道特性

- **公开可见**：游客可以查看消息（只读）
- **自由加入**：无需审批即可加入
- **全站唯一**：每个站点只有一个大厅频道
- **置顶消息**：管理员可以置顶重要消息

### 5.2 大厅消息查询

```java
// ChatConversationQueryServiceImpl.pageLobbyMessages()
public PageResult<ChatLobbyMessageVO> pageLobbyMessages(Long current, Long size, Long beforeMessageId) {
    // 1. 查询大厅会话
    ChatConversation lobby = chatConversationRepository
        .findBySceneType("hall_channel");

    // 2. 分页查询消息（反向，即新消息在前）
    Page<ChatMessage> page = new Page<>(current, size);
    QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
    wrapper.eq("conversation_id", lobby.getId());
    wrapper.eq("revoke_status", 0); // 未撤回

    if (beforeMessageId != null) {
        wrapper.lt("id", beforeMessageId);
    }

    wrapper.orderByDesc("id");
    Page<ChatMessage> messagePage = chatMessageRepository.selectPage(page, wrapper);

    // 3. 转换为 VO
    return convertToLobbyMessageVOs(messagePage);
}
```

### 5.3 游客访问

```java
// PublicChatLobbyController.pageMessages()
@GetMapping("/lobby/messages")
public Result<PageResult<ChatLobbyMessageVO>> pageMessages(
    @RequestParam(defaultValue = "1") Long current,
    @RequestParam(defaultValue = "20") Long size,
    @RequestParam(required = false) Long beforeMessageId) {

    // 允许游客访问（无需登录）
    return Result.success(chatConversationQueryService.pageLobbyMessages(
        current, size, beforeMessageId));
}
```

## 6. 会话查询流程

### 6.1 我的会话列表

```java
// ChatConversationQueryServiceImpl.pageMyConversations()
public PageResult<ChatConversationVO> pageMyConversations(Long userId,
    ChatConversationPageQuery query) {

    // 1. 查询用户参与的所有活跃会话
    List<ChatConversationListItem> items = chatConversationRepository
        .pageMyConversations(userId, query);

    // 2. 批量加载会话成员
    Map<Long, List<ChatConversationMember>> membersMap =
        chatConversationMemberRepository.findActiveMembersByConversationIds(
            items.stream().map(ChatConversationListItem::getId).collect(Collectors.toList()));

    // 3. 批量加载用户信息
    Set<Long> userIds = collectConversationUserIds(items, membersMap);
    Map<Long, SysUser> userMap = loadUsers(userIds);

    // 4. 构建 VO
    List<ChatConversationVO> voList = buildConversationRecords(userId, items);

    return new PageResult<>(voList, query.getCurrent(), query.getSize());
}
```

### 6.2 会话详情查询

```java
// ChatServiceSupport.getConversationVO()
public ChatConversationVO getConversationVO(Long userId, Long conversationId) {
    // 1. 查询会话
    ChatConversation conversation = chatConversationRepository
        .selectById(conversationId);

    // 2. 校验用户是否为成员
    ConversationAccessContext ctx = requireConversationAccess(userId, conversationId);

    // 3. 加载成员列表
    List<ChatConversationMember> members = listActiveMembers(conversationId);

    // 4. 加载用户信息
    Map<Long, SysUser> userMap = loadUsers(
        members.stream().map(ChatConversationMember::getUserId).collect(Collectors.toList()));

    // 5. 构建 VO
    return buildConversationVO(userId, item, members, userMap);
}
```

## 7. 会话访问控制

### 7.1 访问上下文

```java
// ChatServiceSupport.ConversationAccessContext
public record ConversationAccessContext(
    ChatConversation conversation,
    ChatConversationMember selfMember,
    List<ChatConversationMember> activeMembers
) {
    public List<Long> activeUserIds() {
        return activeMembers.stream()
            .map(ChatConversationMember::getUserId)
            .collect(Collectors.toList());
    }
}
```

### 7.2 访问校验方法

| 方法 | 说明 |
|------|------|
| `requireConversationAccess` | 校验用户是否为会话成员 |
| `requireGroupAccess` | 校验用户是否为群聊成员 |
| `requireGroupOwner` | 校验用户是否为群主 |
| `requireGroupManager` | 校验用户是否为群主或管理员 |

### 7.3 访问校验实现

```java
// ChatServiceSupport.requireConversationAccess()
public ConversationAccessContext requireConversationAccess(Long userId, Long conversationId) {
    ChatConversation conversation = chatConversationRepository.selectById(conversationId);
    if (conversation == null) {
        throwBusinessEx("会话不存在");
    }

    ChatConversationMember member = findMember(conversationId, userId);
    if (member == null || member.getStatus() != 0) {
        throwBusinessEx("您不是该会话的成员");
    }

    List<ChatConversationMember> activeMembers = listActiveMembers(conversationId);

    return new ConversationAccessContext(conversation, member, activeMembers);
}
```

## 8. 会话状态管理

### 8.1 会话状态

| 状态 | status | 说明 |
|------|---------|------|
| 禁用 | 0 | 会话被禁用，无法使用 |
| 正常 | 1 | 会话正常开放 |
| 已解散 | 2 | 会话已解散 |

### 8.2 状态变更

```java
// ChatAdminServiceImpl.updateConversationStatus()
public void updateConversationStatus(Long conversationId, Integer status) {
    ChatConversation conversation = chatConversationRepository.selectById(conversationId);
    if (conversation == null) {
        throwBusinessEx("会话不存在");
    }

    // 状态流转校验
    if (conversation.getStatus() == 2) {
        throwBusinessEx("已解散的会话无法恢复");
    }

    conversation.setStatus(status);
    chatConversationRepository.updateById(conversation);

    // 推送会话状态变更通知
    chatPushService.pushConversationUpdated(
        ChatWsConversationUpdatedPayload.builder()
            .conversationId(conversationId)
            .updatedField("status")
            .newValue(status)
            .build(),
        listActiveUserIds(conversationId)
    );
}
```

## 9. 会话最新消息更新

### 9.1 发送消息后更新

```java
// ChatMessageSendServiceImpl.updateSenderCursorAfterSend()
private void updateSenderCursorAfterSend(ChatConversation conversation,
    Long messageId, LocalDateTime now) {

    conversation.setLastMessageId(messageId);
    conversation.setLastMessageTime(now);

    chatConversationRepository.updateById(conversation);
}
```

## 10. 相关文档

- [Chat 模块总览](./00-chat-module-overview.md)
- [Chat 数据模型](./chat-data-model.md)
- [消息收发流程](./chat-message-flow.md)
- [群组管理流程](./chat-group-management.md)

# 消息收发流程

## 1. 概述

Chat 模块的消息收发流程涵盖了从用户发送消息到消息最终送达的全部链路，包括：

- **消息发送**：文本消息、图片消息、文件消息的发送
- **消息校验**：禁言检查、慢速模式检查、权限检查
- **消息持久化**：消息入库、接收方记录、游标更新
- **消息投递**：在线用户实时推送、离线用户消息存储
- **已读回执**：已读状态推进、已读回执推送

## 2. 消息发送流程

### 2.1 文本消息发送（REST API）

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ Controller  │────▶│   Service   │────▶│  Repository │
│             │     │UserChatCtrl │     │ChatMessage  │     │             │
│             │     │             │     │ SendService │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                    │
                                                    ▼
                                           ┌─────────────────┐
                                           │ ChatPushService  │
                                           │  (WebSocket/Redis)│
                                           └─────────────────┘
```

### 2.2 文本消息发送（WebSocket）

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│  WebSocket  │────▶│   Service   │────▶│  Repository │
│  (Browser) │     │  Handler    │     │ChatMessage  │     │             │
│             │     │             │     │ SendService │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                    │
                                                    ▼
                                           ┌─────────────────┐
                                           │ ChatPushService  │
                                           │  (WebSocket/Redis)│
                                           └─────────────────┘
```

### 2.3 发送流程详解

```java
// 核心入口：UserChatServiceImpl.sendTextMessage()
ChatMessageVO sendTextMessage(Long userId, ChatSendTextRequest request) {
    // 1. 参数校验
    validateSendRequest(request);

    // 2. 解析目标会话
    ChatConversation conversation = resolveSendConversation(userId, request);

    // 3. 权限校验
    ChatConversationMember selfMember = requireActiveGroupMember(userId, conversationId);
    validateMemberCanSend(userId, selfMember, conversation);

    // 4. 禁言校验
    String muteScope = resolveMuteScope(conversation);
    if (chatMuteGovernanceService.isUserMuted(userId, conversationId, muteScope)) {
        throwBusinessEx("您已被禁言");
    }

    // 5. 慢速模式校验
    validateSlowMode(userId, conversation);

    // 6. 构建消息
    ChatMessage message = ChatMessage.builder()
        .conversationId(conversationId)
        .senderId(userId)
        .messageType("text")
        .content(request.getContent())
        .build();

    // 7. 持久化消息
    chatMessageRepository.insert(message);

    // 8. 持久化接收方记录（单聊直接创建，群聊为每个成员创建）
    persistRecipients(message, recipientIds, userId, now);

    // 9. 更新会话最新消息
    updateSenderCursorAfterSend(conversation, messageId, now);

    // 10. 更新未读计数
    incrementUnreadForRecipients(conversationId, recipientIds, messageId);

    // 11. 推送消息给在线用户
    markDeliveredForOnlineRecipients(conversationId, recipientIds, messageId, now);

    // 12. 返回消息VO
    return buildMessageVO(messageId, historyItem, users, replyMap);
}
```

### 2.4 发送校验项

| 校验项 | 说明 | 异常码 |
|--------|------|--------|
| 禁言检查 | 检查用户在当前会话的禁言状态 | MUTE |
| 慢速模式 | 检查距上次发言是否满足慢速模式间隔 | SLOW_MODE |
| 发言等级 | 检查用户等级是否满足最低发言等级 | LEVEL_LIMIT |
| 成员状态 | 检查用户是否为活跃成员 | NOT_MEMBER |
| 会话状态 | 检查会话是否正常开放 | CONVERSATION_DISABLED |
| 内容长度 | 检查消息内容是否超长（MAX_MESSAGE_CONTENT_LENGTH） | CONTENT_TOO_LONG |

## 3. 文件消息发送流程

### 3.1 流程概览

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ Controller  │────▶│   Service   │────▶│ FileService │
│             │     │             │     │             │     │ 上传文件    │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                    │
                                                    ▼
                                           ┌─────────────────┐
                                           │ 附件异步处理任务  │
                                           │ ChatAttachment  │
                                           │ ProcessTask    │
                                           └─────────────────┘
```

### 3.2 异步处理流程

```java
// ChatAttachmentAsyncProcessingServiceImpl
public void processAttachmentTask(ChatAttachmentProcessTask task) {
    // 1. 更新任务状态为处理中
    task.setTaskStatus(1); // PROCESSING
    task.setStartedAt(now);

    // 2. 根据消息类型选择处理器
    if ("image".equals(messageType)) {
        // 图片处理：压缩、水印、缩略图
        processImage(task);
    } else if ("voice".equals(messageType)) {
        // 语音处理：转码、时长提取
        processVoice(task);
    }

    // 3. 更新消息 payload
    updateMessagePayload(task);

    // 4. 更新任务状态为完成
    task.setTaskStatus(2); // COMPLETED
    task.setCompletedAt(now);

    // 5. 重试失败处理
    if (task.getTaskStatus() == 3) { // FAILED
        if (task.getRetryCount() < task.getMaxRetryCount()) {
            task.setNextRetryAt(calculateNextRetry(task.getRetryCount()));
            task.setRetryCount(task.getRetryCount() + 1);
        }
    }
}
```

## 4. 消息投递流程

### 4.1 投递状态

| 状态 | delivery_status | 说明 |
|------|-----------------|------|
| 待投递 | 0 | 消息已发送但尚未投递给接收方 |
| 已投递 | 1 | 消息已投递给接收方（用户在线） |
| 已读 | 2 | 接收方已阅读消息 |

### 4.2 在线用户投递

```java
// ChatMessageSendServiceImpl.markDeliveredForOnlineRecipients()
private void markDeliveredForOnlineRecipients(Long conversationId,
    List<Long> userIds, Long messageId, LocalDateTime now) {

    // 1. 查询当前在线用户
    List<Long> onlineUserIds = chatWebSocketSessionRegistry.getOnlineUserIds(userIds);

    // 2. 更新已投递状态
    for (Long userId : onlineUserIds) {
        chatMessageRecipientRepository.updateDeliveryStatus(
            messageId, userId, 1, now);

        // 3. 通过 Redis Pub/Sub 推送消息
        chatPushService.pushMessageCreated(messageVO, Collections.singletonList(userId));
    }
}
```

### 4.3 离线用户处理

- 消息持久化到 `chat_message_recipient` 表
- 用户下次上线时通过 `last_delivered_message_id` 游标恢复未读消息

## 5. 已读流程

### 5.1 已读推进

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ Controller  │────▶│   Service   │────▶│  Repository │
│             │     │             │     │             │     │             │
│mark_read    │     │UserChatCtrl │     │UserChatService│   │ChatMessage  │
│             │     │             │     │  .markRead   │     │ReadCursor   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                    │
                                                    ▼
                                           ┌─────────────────┐
                                           │ ChatPushService  │
                                           │ pushReadUpdated  │
                                           └─────────────────┘
```

### 5.2 已读游标更新

```java
// ChatServiceSupport.updateMemberReadState()
public void updateMemberReadState(ChatConversationMember member,
    Long readMessageId, LocalDateTime readAt) {

    // 1. 更新成员表的已读游标
    member.setLastReadMessageId(readMessageId);
    member.setLastReadAt(readAt);

    // 2. 更新或创建游标记录
    ChatMessageReadCursor cursor = getOrCreateCursor(
        member.getConversationId(), member.getUserId(), readMessageId, readAt);
    cursor.setReadMessageId(readMessageId);
    cursor.setReadAt(readAt);
    cursor.setUnreadCount(0); // 重置未读数
    saveOrUpdateCursor(cursor);

    // 3. 更新消息接收状态为已读
    chatMessageRecipientRepository.updateReadStatus(
        member.getConversationId(), member.getUserId(), readMessageId, readAt);
}
```

## 6. 实时推送架构

### 6.1 推送通道

| 通道 | 用途 | 说明 |
|------|------|------|
| WebSocket | 在线用户实时推送 | 通过 ChatPushService 直接推送给浏览器 |
| Redis Pub/Sub | 跨节点消息分发 | 通过 ChatPushRedisSubscriber 订阅并转发 |

### 6.2 推送消息类型

| 事件 | 推送方法 | 说明 |
|------|----------|------|
| 新消息 | pushMessageCreated | 发送新消息时推送给相关用户 |
| 消息更新 | pushMessageUpdated | 消息被编辑时推送更新 |
| 消息撤回 | pushMessageRevoked | 消息撤回时推送 |
| 消息删除 | pushMessageDeleted | 消息删除时推送 |
| 已读更新 | pushReadUpdated | 已读游标推进时推送 |
| 会话更新 | pushConversationUpdated | 会话信息更新时推送 |
| 成员变动 | pushMembersUpdated | 成员加入/退出时推送 |

### 6.3 Redis 推送配置

```java
// ChatRedisPushConfig
@Configuration
public class ChatRedisPushConfig {
    @Bean
    public ChannelTopic chatPushTopic() {
        // 聊天推送的 Redis 频道
        return new ChannelTopic("chat:push:channel");
    }
}

// ChatPushServiceImpl
public void pushMessageCreated(ChatMessageVO message, Collection<Long> userIds) {
    // 1. 构建推送信封
    ChatPushEventEnvelope envelope = ChatPushEventEnvelope.builder()
        .type("MESSAGE_CREATED")
        .messageId(message.getId())
        .conversationId(message.getConversationId())
        .payload(message)
        .userIds(Lists.newArrayList(userIds))
        .build();

    // 2. 发布到 Redis 频道
    redisTemplate.convertAndSend("chat:push:channel", envelope);
}
```

## 7. WebSocket 处理

### 7.1 连接建立

```java
// ChatWebSocketHandler.afterConnectionEstablished()
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    // 1. 注册会话
    sessionRegistry.register(session);

    // 2. 发送 Ready 消息
    session.sendMessage(messageCodec.buildReady(session));
}
```

### 7.2 消息类型

| 类型 | 说明 | 客户端发送 |
|------|------|-----------|
| PING | 心跳检测 | ✅ |
| SEND_MESSAGE | 发送消息 | ✅ |
| MARK_READ | 标记已读 | ✅ |
| ACK | 消息确认 | ⬅️ 服务端发送 |
| PONG | 心跳响应 | ⬅️ 服务端发送 |
| ERROR | 错误响应 | ⬅️ 服务端发送 |

### 7.3 消息编解码

```java
// ChatWebSocketMessageCodec
public ChatWsResponse buildAck(String requestId, ChatWsAckPayload data) {
    return ChatWsResponse.builder()
        .requestId(requestId)
        .type("ACK")
        .code(0)
        .data(data)
        .build();
}

public ChatWsResponse buildBusinessError(String requestId, String code, String message) {
    return ChatWsResponse.builder()
        .requestId(requestId)
        .type("ERROR")
        .code(-1)
        .errorCode(code)
        .errorMessage(message)
        .build();
}
```

## 8. 相关文档

- [Chat 模块总览](./00-chat-module-overview.md)
- [Chat 数据模型](./chat-data-model.md)
- [会话管理流程](./chat-conversation-flow.md)
- [群组管理流程](./chat-group-management.md)

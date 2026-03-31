# Chat 模块 Repository 迁移计划

## 模块信息

- **优先级**：第6轮（最复杂）
- **复杂度**：高
- **前置依赖**：auth + file 模块的 Repository 已创建
- **涉及薄服务**：5个
- **涉及业务服务**：约8个（仅含数据操作的服务需要迁移）
- **数据访问总数**：约80处
- **直接 Mapper 注入**：2个服务注入3个 Mapper

## Repository 列表

| Repository 接口 | 对应实体 | 薄服务来源 | Mapper自定义方法 |
|---|---|---|---|
| `ChatConversationRepository` | ChatConversation | ChatConversationService | 6个XML方法 |
| `ChatConversationMemberRepository` | ChatConversationMember | ChatConversationMemberService | 无 |
| `ChatMessageRepository` | ChatMessage | ChatMessageService | 7个XML方法 |
| `ChatMessageRecipientRepository` | ChatMessageRecipient | ChatMessageRecipientService | 无 |
| `ChatMessageReadCursorRepository` | ChatMessageReadCursor | ChatMessageReadCursorService | 无 |

## 各 Repository 方法设计

### ChatConversationRepository（核心，含大量XML）

```java
public interface ChatConversationRepository extends IService<ChatConversation> {
    // === Mapper XML 包装 ===
    Long countConversationPage(Long userId, String keyword);
    List<ChatConversationListItem> selectConversationPage(Long userId, String keyword, Long offset, Long size);
    ChatConversationListItem selectConversationDetail(Long conversationId, Long userId);
    Long countAdminConversationPage(ChatAdminConversationQuery query);
    List<ChatAdminConversationListItem> selectAdminConversationPage(ChatAdminConversationQuery query, Long offset, Long size);
    ChatAdminConversationDetailVO selectAdminConversationDetail(Long conversationId);

    // === lambdaQuery 提取 ===
    ChatConversation findBySinglePairKey(String singlePairKey);
    ChatConversation findGlobalConversation();
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `countConversationPage` | ChatConversationMapper | 用户会话列表计数 |
| `selectConversationPage` | ChatConversationMapper | 用户会话列表 |
| `selectConversationDetail` | ChatConversationMapper | 会话详情 |
| `countAdminConversationPage` | ChatConversationMapper | 管理端会话计数 |
| `selectAdminConversationPage` | ChatConversationMapper | 管理端会话列表 |
| `selectAdminConversationDetail` | ChatConversationMapper | 管理端会话详情 |
| `findBySinglePairKey` | UserChatServiceImpl:637, `lambdaQuery().eq(singlePairKey).last("limit 1").one()` | 单聊会话查找 |
| `findGlobalConversation` | UserChatServiceImpl:670, `lambdaQuery().eq(isAllSite,1).last("limit 1").one()` | 全站会话查找 |

### ChatConversationMemberRepository

```java
public interface ChatConversationMemberRepository extends IService<ChatConversationMember> {
    ChatConversationMember findByConversationAndUser(Long conversationId, Long userId);
    ChatConversationMember findOwnerByConversationId(Long conversationId);
    List<ChatConversationMember> listActiveByConversationId(Long conversationId);
    List<ChatConversationMember> listActiveByConversationIds(Collection<Long> conversationIds);
    List<ChatConversationMember> listByConversationId(Long conversationId);
    List<ChatConversationMember> listByConversationIds(Collection<Long> conversationIds);
    boolean removeAllActiveMembers(Long conversationId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByConversationAndUser` | UserChatServiceImpl:1264, ChatAdminServiceImpl:512, `lambdaQuery().eq(convId).eq(userId).last("limit 1").one()` | 查成员关系 |
| `findOwnerByConversationId` | ChatAdminServiceImpl:523, `lambdaQuery().eq(role OWNER).last("limit 1").one()` | 查群主 |
| `listActiveByConversationId` | UserChatServiceImpl:1189, ChatAdminServiceImpl:472, `lambdaQuery().eq(convId).eq(status NORMAL).list()` | 活跃成员 |
| `listActiveByConversationIds` | UserChatServiceImpl:1177, `lambdaQuery().in(convIds).eq(status NORMAL).list()` | 批量活跃成员 |
| `listByConversationId` | ChatAdminServiceImpl:466, `lambdaQuery().eq(convId).list()` | 所有成员 |
| `listByConversationIds` | ChatAdminServiceImpl:455, `lambdaQuery().in(convIds).list()` | 批量所有成员 |
| `removeAllActiveMembers` | UserChatServiceImpl:523, `lambdaUpdate().eq(convId).eq(status NORMAL).set(status REMOVED).update()` | 解散群组 |

### ChatMessageRepository（含大量XML）

```java
public interface ChatMessageRepository extends IService<ChatMessage> {
    // === Mapper XML 包装 ===
    Long countMessagePage(Long conversationId, Long userId, Long beforeMessageId);
    List<ChatMessageHistoryItem> selectMessagePage(Long conversationId, Long userId, Long beforeMessageId, Long offset, Long size);
    ChatMessageHistoryItem selectVisibleMessageById(Long conversationId, Long userId, Long messageId);
    List<ChatMessageHistoryItem> selectVisibleMessagesByIds(Long conversationId, Long userId, Collection<Long> messageIds);
    Long countAdminMessagePage(Long conversationId, ChatAdminMessageQuery query);
    List<ChatAdminMessageItem> selectAdminMessagePage(Long conversationId, ChatAdminMessageQuery query, Long offset, Long size);
    List<ChatAdminMessageItem> selectAdminMessagesByIds(Long conversationId, Collection<Long> messageIds);

    // === lambdaQuery 提取 ===
    ChatMessage findBySenderAndClientMessageId(Long senderId, String clientMessageId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `countMessagePage` | ChatMessageMapper | 消息分页计数 |
| `selectMessagePage` | ChatMessageMapper | 消息分页列表 |
| `selectVisibleMessageById` | ChatMessageMapper | 单条可见消息 |
| `selectVisibleMessagesByIds` | ChatMessageMapper | 批量可见消息 |
| `countAdminMessagePage` | ChatMessageMapper | 管理端消息计数 |
| `selectAdminMessagePage` | ChatMessageMapper | 管理端消息列表 |
| `selectAdminMessagesByIds` | ChatMessageMapper | 管理端批量消息 |
| `findBySenderAndClientMessageId` | UserChatServiceImpl:1056, `lambdaQuery().eq(senderId).eq(clientMsgId).last("limit 1").one()` | 幂等查重 |

### ChatMessageRecipientRepository

```java
public interface ChatMessageRecipientRepository extends IService<ChatMessageRecipient> {
    // lambdaUpdate 提取（消息投递/已读状态）
    boolean hideMessage(Long conversationId, Long recipientUserId, Long messageId);
    boolean markReadUpTo(Long conversationId, Long recipientUserId, Long messageId, Date readAt);
    boolean markDelivered(Long conversationId, Long recipientUserId, Long messageId, Date deliveredAt);
    boolean batchMarkDelivered(Long conversationId, Long recipientUserId, Collection<Long> messageIds, Date deliveredAt);

    // lambdaQuery 提取
    long countUnread(Long conversationId, Long recipientUserId);
    ChatMessageRecipient findVisibleByUserAndMessage(Long recipientUserId, Long messageId);
    List<ChatMessageRecipient> listByMessageId(Long messageId);

    // 管理端
    Page<ChatMessageRecipient> pageAdminReceipts(AdminReceiptQuery query);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `hideMessage` | UserChatServiceImpl:292, `lambdaUpdate().set(HIDDEN).update()` | 隐藏消息 |
| `markReadUpTo` | UserChatServiceImpl:333, `lambdaUpdate().set(READ).update()` | 标记已读 |
| `markDelivered` | UserChatServiceImpl:795, `lambdaUpdate().eq(PENDING).set(DELIVERED).update()` | 标记投递 |
| `batchMarkDelivered` | UserChatServiceImpl:828, `lambdaUpdate().in(messageIds).set(DELIVERED).update()` | 批量标记投递 |
| `countUnread` | UserChatServiceImpl:857, `lambdaQuery().lt(READ).count()` | 未读计数 |
| `findVisibleByUserAndMessage` | UserChatServiceImpl:1011, `lambdaQuery().eq(userId).eq(msgId).last("limit 1").one()` | 查可见接收记录 |
| `listByMessageId` | ChatAdminServiceImpl:532, `lambdaQuery().eq(messageId).list()` | 消息接收人列表 |
| `pageAdminReceipts` | ChatAdminServiceImpl:188, `LambdaQueryWrapper + page()` | 管理端回执分页 |

### ChatMessageReadCursorRepository

```java
public interface ChatMessageReadCursorRepository extends IService<ChatMessageReadCursor> {
    ChatMessageReadCursor findByConversationAndUser(Long conversationId, Long userId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findByConversationAndUser` | UserChatServiceImpl:1273, `lambdaQuery().eq(convId).eq(userId).last("limit 1").one()` | 查读游标 |

## 直接 Mapper 注入迁移

| 服务 | 直接注入的 Mapper | 迁移到 |
|---|---|---|
| UserChatServiceImpl | ChatConversationMapper, ChatMessageMapper | ChatConversationRepository, ChatMessageRepository |
| ChatAdminServiceImpl | ChatConversationMapper, ChatMessageMapper | ChatConversationRepository, ChatMessageRepository |

## 需迁移的业务服务

| 服务 | 数据操作数 | 复杂度 |
|---|---|---|
| `UserChatServiceImpl` | ~50处 | 极高（1600+行） |
| `ChatAdminServiceImpl` | ~25处 | 高 |
| `ChatMessageGovernanceServiceImpl` | 少量 | 低 |
| `ChatMetricsServiceImpl` | 少量 | 低 |

**无需迁移的服务**（无直接数据操作或仅有少量调用）：
- `ChatPushServiceImpl` — 推送逻辑，通过其他服务间接操作
- `ChatWebSocketSessionRegistryImpl` — 会话管理，无DB操作
- `ChatAttachmentAsyncProcessingServiceImpl` — 异步处理，注入的是 Service 非 Mapper
- `ChatAttachmentMetadataResolverImpl` — 元数据解析

## 执行步骤

### Step 1: 创建5个 Repository 接口 + 5个实现

### Step 2: 修改业务服务

1. **`UserChatServiceImpl`**（最高优先级，最大文件）：
   - 移除 `ChatConversationMapper` 和 `ChatMessageMapper` 直接注入
   - 注入 `ChatConversationRepository`, `ChatConversationMemberRepository`
   - 注入 `ChatMessageRepository`, `ChatMessageRecipientRepository`, `ChatMessageReadCursorRepository`
   - 注入 `SysUserRepository`（auth模块）, `FileInfoRepository`/`FileBusinessInfoRepository`（file模块）
   - 提取14处 lambdaQuery、6处 lambdaUpdate 到对应 Repository 方法

2. **`ChatAdminServiceImpl`**：
   - 移除 `ChatConversationMapper` 和 `ChatMessageMapper` 直接注入
   - 注入各 Chat Repository

3. **`ChatMessageGovernanceServiceImpl`**、**`ChatMetricsServiceImpl`**：
   - 替换注入的薄服务为 Repository

### Step 3: 更新测试

### Step 4: 删除旧薄服务（本模块5个）

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.chat.*Test"
```

确认 `UserChatServiceImpl` 和 `ChatAdminServiceImpl` 中无直接 Mapper 注入、无 `lambdaQuery()`/`lambdaUpdate()` 调用。

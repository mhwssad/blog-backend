# 群组管理流程

## 1. 概述

Chat 模块的群组管理涵盖了群聊的成员管理、角色变更、邀请链接、入群申请等功能。

## 2. 成员角色

| 角色 | memberRole | 权限说明 |
|------|------------|----------|
| 群主 | owner | 转让群主、解散群聊、设置管理员、踢除成员、修改群信息 |
| 管理员 | admin | 禁言成员、踢除普通成员、审核入群申请 |
| 普通成员 | member | 发送消息、查看消息、主动退出 |

## 3. 成员管理

### 3.1 邀请成员

```java
// ChatGroupManageServiceImpl.inviteGroupMembers()
public List<ChatMemberVO> inviteGroupMembers(Long userId, Long conversationId,
    ChatGroupMemberOperateRequest request) {

    // 1. 校验群主或管理员权限
    ConversationAccessContext ctx = requireGroupManager(userId, conversationId);

    // 2. 校验被邀请者数量
    List<Long> inviteeIds = request.getUserIds();
    int currentCount = ctx.activeMembers().size();
    int additionalCount = countAdditionalMembers(conversationId, inviteeIds);
    ensureConversationMemberLimitAllows(ctx.conversation(), currentCount + additionalCount);

    // 3. 添加成员
    List<ChatConversationMember> addedMembers = new ArrayList<>();
    for (Long inviteeId : inviteeIds) {
        ChatConversationMember member = upsertConversationMembership(
            ctx.conversation(), inviteeId, "member", "invite");
        addedMembers.add(member);
    }

    // 4. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("member_joined")
            .addedUserIds(inviteeIds)
            .build(),
        ctx.activeUserIds()
    );

    return buildMemberRecords(addedMembers);
}
```

### 3.2 移除成员

```java
// ChatGroupManageServiceImpl.removeGroupMember()
public void removeGroupMember(Long userId, Long conversationId, Long memberUserId) {
    // 1. 校验权限：群主可移除任何人，管理员只能移除普通成员
    ConversationAccessContext ctx = requireGroupAccess(userId, conversationId);
    ChatConversationMember targetMember = findMember(conversationId, memberUserId);

    validateManagerCanOperateMember(ctx.selfMember(), targetMember);

    // 2. 不能移除群主
    if ("owner".equals(targetMember.getMemberRole())) {
        throwBusinessEx("不能移除群主");
    }

    // 3. 更新成员状态
    targetMember.setStatus(2); // 已移除
    chatConversationMemberRepository.updateById(targetMember);

    // 4. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("member_removed")
            .removedUserIds(Collections.singletonList(memberUserId))
            .build(),
        ctx.activeUserIds()
    );
}
```

### 3.3 退出群聊

```java
// ChatGroupManageServiceImpl.leaveGroup()
public void leaveGroup(Long userId, Long conversationId) {
    // 1. 校验用户是否为成员
    ConversationAccessContext ctx = requireGroupAccess(userId, conversationId);

    // 2. 群主不能退出，只能解散
    if ("owner".equals(ctx.selfMember().getMemberRole())) {
        throwBusinessEx("群主不能退出群聊，请先转让群主或解散群聊");
    }

    // 3. 更新成员状态
    ctx.selfMember().setStatus(1); // 已退出
    chatConversationMemberRepository.updateById(ctx.selfMember());

    // 4. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("member_left")
            .leftUserId(userId)
            .build(),
        ctx.activeUserIds()
    );
}
```

### 3.4 解散群聊

```java
// ChatGroupManageServiceImpl.dissolveGroup()
public void dissolveGroup(Long userId, Long conversationId) {
    // 1. 校验群主权限
    ConversationAccessContext ctx = requireGroupOwner(userId, conversationId);

    // 2. 更新会话状态
    ctx.conversation().setStatus(2); // 已解散
    chatConversationRepository.updateById(ctx.conversation());

    // 3. 更新所有成员状态
    for (ChatConversationMember member : ctx.activeMembers()) {
        member.setStatus(2); // 已移除
        chatConversationMemberRepository.updateById(member);
    }

    // 4. 推送群聊解散通知
    chatPushService.pushConversationUpdated(
        ChatWsConversationUpdatedPayload.builder()
            .conversationId(conversationId)
            .updatedField("status")
            .newValue(2)
            .build(),
        ctx.activeUserIds()
    );
}
```

## 4. 角色变更

### 4.1 设置管理员

```java
// ChatGroupManageServiceImpl.appointGroupAdmin()
public List<ChatMemberVO> appointGroupAdmin(Long userId, Long conversationId, Long memberUserId) {
    // 1. 校验群主权限
    ConversationAccessContext ctx = requireGroupOwner(userId, conversationId);

    // 2. 获取目标成员
    ChatConversationMember targetMember = findMember(conversationId, memberUserId);
    if (targetMember == null) {
        throwBusinessEx("该用户不是群成员");
    }

    // 3. 不能对自己操作
    if (userId.equals(memberUserId)) {
        throwBusinessEx("不能对自己进行此操作");
    }

    // 4. 更新角色
    targetMember.setMemberRole("admin");
    chatConversationMemberRepository.updateById(targetMember);

    // 5. 刷新成员列表
    List<ChatConversationMember> updatedMembers = listActiveMembers(conversationId);

    // 6. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("role_changed")
            .changedUserId(memberUserId)
            .newRole("admin")
            .build(),
        ctx.activeUserIds()
    );

    return buildMemberRecords(updatedMembers);
}
```

### 4.2 取消管理员

```java
// ChatGroupManageServiceImpl.removeGroupAdmin()
public List<ChatMemberVO> removeGroupAdmin(Long userId, Long conversationId, Long memberUserId) {
    // 1. 校验群主权限
    ConversationAccessContext ctx = requireGroupOwner(userId, conversationId);

    // 2. 获取目标成员
    ChatConversationMember targetMember = findMember(conversationId, memberUserId);

    // 3. 更新角色
    targetMember.setMemberRole("member");
    chatConversationMemberRepository.updateById(targetMember);

    // 4. 推送成员变动通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("role_changed")
            .changedUserId(memberUserId)
            .newRole("member")
            .build(),
        ctx.activeUserIds()
    );

    return buildMemberRecords(listActiveMembers(conversationId));
}
```

### 4.3 转让群主

```java
// ChatGroupManageServiceImpl.transferGroupOwner()
public ChatConversationVO transferGroupOwner(Long userId, Long conversationId,
    ChatTransferGroupOwnerRequest request) {

    // 1. 校验群主权限
    ConversationAccessContext ctx = requireGroupOwner(userId, conversationId);

    Long newOwnerId = request.getNewOwnerId();

    // 2. 新群主必须是成员
    ChatConversationMember newOwnerMember = findMember(conversationId, newOwnerId);
    if (newOwnerMember == null || newOwnerMember.getStatus() != 0) {
        throwBusinessEx("指定的用户不是群成员");
    }

    // 3. 更新原群主为管理员
    ctx.selfMember().setMemberRole("admin");
    chatConversationMemberRepository.updateById(ctx.selfMember());

    // 4. 更新新群主为群主
    newOwnerMember.setMemberRole("owner");
    chatConversationMemberRepository.updateById(newOwnerMember);

    // 5. 更新会话群主
    ctx.conversation().setOwnerId(newOwnerId);
    chatConversationRepository.updateById(ctx.conversation());

    // 6. 推送转让通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("owner_transferred")
            .oldOwnerId(userId)
            .newOwnerId(newOwnerId)
            .build(),
        ctx.activeUserIds()
    );

    return getGroupDetail(userId, conversationId);
}
```

## 5. 成员禁言

### 5.1 禁言成员

```java
// ChatGroupManageServiceImpl.muteGroupMember()
public List<ChatMemberVO> muteGroupMember(Long userId, Long conversationId,
    Long memberUserId, ChatMuteMemberRequest request) {

    // 1. 校验管理员权限
    ConversationAccessContext ctx = requireGroupManager(userId, conversationId);

    // 2. 获取目标成员
    ChatConversationMember targetMember = findMember(conversationId, memberUserId);
    validateManagerCanOperateMember(ctx.selfMember(), targetMember);

    // 3. 不能禁言比自己权限高的人
    if (isGroupManager(targetMember) && !isGroupOwner(ctx.selfMember())) {
        throwBusinessEx("不能禁言管理员");
    }

    // 4. 更新禁言状态
    targetMember.setMuteUntil(request.getMuteUntil());
    chatConversationMemberRepository.updateById(targetMember);

    // 5. 创建禁言记录
    chatMuteGovernanceService.createMute(
        ChatMuteCreateRequest.builder()
            .userId(memberUserId)
            .scope("group")
            .conversationId(conversationId)
            .muteUntil(request.getMuteUntil())
            .reason(request.getReason())
            .sourceType("admin")
            .operatorId(userId)
            .build(),
        userId
    );

    // 6. 推送禁言通知
    chatPushService.pushMembersUpdated(
        ChatWsMembersUpdatedPayload.builder()
            .conversationId(conversationId)
            .action("member_muted")
            .affectedUserId(memberUserId)
            .muteUntil(request.getMuteUntil())
            .build(),
        ctx.activeUserIds()
    );

    return buildMemberRecords(listActiveMembers(conversationId));
}
```

## 6. 邀请链接

### 6.1 创建邀请链接

```java
// ChatGroupInviteLinkServiceImpl.createInviteLink()
public ChatGroupInviteLinkVO createInviteLink(Long userId, Long conversationId,
    ChatGroupInviteLinkCreateRequest request) {

    // 1. 校验用户为群主或管理员
    ConversationAccessContext ctx = requireGroupManager(userId, conversationId);

    // 2. 生成邀请令牌
    String inviteToken = generateInviteToken();

    // 3. 创建链接记录
    ChatGroupInviteLink link = ChatGroupInviteLink.builder()
        .conversationId(conversationId)
        .inviteToken(inviteToken)
        .createdBy(userId)
        .expireAt(request.getExpireAt())
        .maxUseCount(request.getMaxUseCount() != null ? request.getMaxUseCount() : 0)
        .usedCount(0)
        .status(1)
        .build();

    chatGroupInviteLinkRepository.insert(link);

    return convertToVO(link);
}
```

### 6.2 通过邀请链接加入

```java
// ChatChannelJoinServiceImpl.joinByInviteLink()
public ChatConversationVO joinByInviteLink(String inviteToken) {
    // 1. 查询邀请链接
    ChatGroupInviteLink link = chatGroupInviteLinkRepository
        .findByToken(inviteToken);

    // 2. 校验链接有效性
    if (link == null || link.getStatus() != 1) {
        throwBusinessEx("邀请链接无效");
    }

    if (link.getExpireAt() != null && link.getExpireAt().isBefore(now)) {
        throwBusinessEx("邀请链接已过期");
    }

    if (link.getMaxUseCount() > 0 && link.getUsedCount() >= link.getMaxUseCount()) {
        throwBusinessEx("邀请链接已达到使用上限");
    }

    // 3. 获取会话
    ChatConversation conversation = chatConversationRepository
        .selectById(link.getConversationId());

    // 4. 校验用户是否已是成员
    ChatConversationMember existing = findMember(conversation.getId(), currentUserId);
    if (existing != null && existing.getStatus() == 0) {
        throwBusinessEx("您已是群成员");
    }

    // 5. 加入会话
    upsertConversationMembership(conversation, currentUserId, "member", "link");

    // 6. 更新链接使用次数
    link.setUsedCount(link.getUsedCount() + 1);
    chatGroupInviteLinkRepository.updateById(link);

    return getConversationVO(currentUserId, conversation.getId());
}
```

## 7. 入群申请

### 7.1 提交入群申请

```java
// ChatChannelJoinServiceImpl.submitJoinApplication()
public ChatGroupJoinApplicationVO submitJoinApplication(Long userId, Long conversationId,
    ChatGroupJoinApplyRequest request) {

    // 1. 获取会话
    ChatConversation conversation = chatConversationRepository
        .selectById(conversationId);

    // 2. 校验加入规则
    if (!"approval".equals(conversation.getJoinRule())) {
        throwBusinessEx("该群不允许申请加入");
    }

    // 3. 校验用户是否已是成员
    ChatConversationMember existing = findMember(conversationId, userId);
    if (existing != null && existing.getStatus() == 0) {
        throwBusinessEx("您已是群成员");
    }

    // 4. 校验是否有待处理的申请
    ChatGroupJoinApplication existingApp = chatGroupJoinApplicationRepository
        .findPendingApplication(conversationId, userId);
    if (existingApp != null) {
        throwBusinessEx("您已有待处理的入群申请");
    }

    // 5. 创建申请记录
    ChatGroupJoinApplication application = ChatGroupJoinApplication.builder()
        .conversationId(conversationId)
        .applicantUserId(userId)
        .applyMessage(request.getMessage())
        .applyStatus(0) // 待审核
        .submittedAt(now)
        .build();

    chatGroupJoinApplicationRepository.insert(application);

    return convertToVO(application);
}
```

### 7.2 审核入群申请

```java
// ChatChannelJoinServiceImpl.reviewJoinApplication()
public void reviewJoinApplication(Long userId, Long applicationId,
    ChatGroupJoinReviewRequest request) {

    // 1. 获取申请记录
    ChatGroupJoinApplication application = chatGroupJoinApplicationRepository
        .selectById(applicationId);

    // 2. 校验申请状态
    if (application.getApplyStatus() != 0) {
        throwBusinessEx("该申请已处理");
    }

    // 3. 校验审核人权限
    ConversationAccessContext ctx = requireGroupManager(userId, application.getConversationId());

    // 4. 更新申请状态
    application.setApplyStatus(request.getApproved() ? 1 : 2); // 通过/拒绝
    application.setReviewerId(userId);
    application.setReviewComment(request.getComment());
    application.setReviewedAt(now);
    chatGroupJoinApplicationRepository.updateById(application);

    // 5. 如果通过，添加成员
    if (request.getApproved()) {
        upsertConversationMembership(
            ctx.conversation(),
            application.getApplicantUserId(),
            "member",
            "approval"
        );
    }
}
```

## 8. 成员状态

### 8.1 成员状态流转

| 状态 | status | 说明 |
|------|---------|------|
| 正常 | 0 | 正常参与群聊 |
| 已退出 | 1 | 用户主动退出 |
| 已移除 | 2 | 被管理员/群主移除 |

### 8.2 权限判断方法

```java
// ChatServiceSupport
public boolean isGroupManager(ChatConversationMember member) {
    return "owner".equals(member.getMemberRole()) ||
           "admin".equals(member.getMemberRole());
}

public void validateManagerCanOperateMember(ChatConversationMember manager,
    ChatConversationMember target) {

    // 群主可以操作任何人
    if ("owner".equals(manager.getMemberRole())) {
        return;
    }

    // 管理员只能操作普通成员
    if ("admin".equals(manager.getMemberRole())) {
        if (!"member".equals(target.getMemberRole())) {
            throwBusinessEx("管理员只能操作普通成员");
        }
        return;
    }

    throwBusinessEx("您没有权限进行此操作");
}
```

## 9. 相关文档

- [Chat 模块总览](./00-chat-module-overview.md)
- [Chat 数据模型](./chat-data-model.md)
- [消息收发流程](./chat-message-flow.md)
- [会话管理流程](./chat-conversation-flow.md)
- [治理功能](./chat-governance.md)

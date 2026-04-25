# 聊天与 WebSocket 接口文档

本文档面向前端联调，对应 2026-03-30 当前仓库中的 chat 模块实现。

## 1. 当前能力范围

当前已支持：

- 单聊、群聊、全站群聊
- 文本消息、文件消息、图片消息、语音消息
- 回复能力，当前同时返回 `replyMessageId` 和 `reply` 快照
- 消息编辑、撤回、仅当前用户视角删除
- 会话列表 / 会话详情 / 历史消息 / 已读推进
- 群管理员、转让群主、禁言、群公告
- 后台会话管理、消息详情、回执明细、成员角色/状态/禁言管理、后台撤回
- WebSocket 新消息、编辑、撤回、删除、会话更新、成员更新、已读推进推送
- WebSocket 多节点广播，当前基于 Redis pub/sub 分发到各节点本地会话
- 聊天域用户级分钟频控、敏感词拦截，以及媒体任务完成后的 `message_updated` 更新推送

当前仍未支持：

- 富文本引用块、跨会话引用与多层嵌套回复展示
- 更完整的审核工作流、人工复核和未读/会话缓存

## 2. 鉴权要求

### 2.1 用户侧 HTTP

除 `security.unsecured-urls` 中声明的匿名接口外，`/api/user/chat/**` 都要求登录。

```http
Authorization: Bearer <accessToken>
```

### 2.2 后台聊天管理 HTTP

后台统一走 `/api/sys/chats/**`，除登录外还要求对应权限：

- `content:chat:query`
- `content:chat:update`

### 2.3 WebSocket

- 地址：`/ws/chat`
- 令牌来源：
    - `Authorization` 请求头
    - Query 参数 `accessToken`

浏览器示例：

```js
const socket = new WebSocket(
  `ws://localhost:8000/ws/chat?accessToken=${accessToken}`,
);
```

## 3. 用户侧 HTTP 接口

### 3.1 接口总览

| 接口       | 方法       | 路径                                                                   |
|----------|----------|----------------------------------------------------------------------|
| 分页查询我的会话 | `GET`    | `/api/user/chat/conversations`                                       |
| 查询会话详情   | `GET`    | `/api/user/chat/conversations/{conversationId}`                      |
| 打开或创建单聊  | `POST`   | `/api/user/chat/single-conversations`                                |
| 分页查询会话消息 | `GET`    | `/api/user/chat/conversations/{conversationId}/messages`             |
| 发送文本消息   | `POST`   | `/api/user/chat/messages/text`                                       |
| 发送文件消息   | `POST`   | `/api/user/chat/messages/file`                                       |
| 编辑消息     | `PUT`    | `/api/user/chat/messages/{messageId}`                                |
| 撤回消息     | `POST`   | `/api/user/chat/messages/{messageId}/revoke`                         |
| 删除我的消息视图 | `DELETE` | `/api/user/chat/messages/{messageId}`                                |
| 推进会话已读   | `POST`   | `/api/user/chat/conversations/{conversationId}/read`                 |
| 创建群聊     | `POST`   | `/api/user/chat/groups`                                              |
| 查询群详情    | `GET`    | `/api/user/chat/groups/{conversationId}`                             |
| 查询群成员    | `GET`    | `/api/user/chat/groups/{conversationId}/members`                     |
| 邀请群成员    | `POST`   | `/api/user/chat/groups/{conversationId}/members`                     |
| 设置群管理员   | `PUT`    | `/api/user/chat/groups/{conversationId}/admins/{memberUserId}`       |
| 取消群管理员   | `DELETE` | `/api/user/chat/groups/{conversationId}/admins/{memberUserId}`       |
| 转让群主     | `PUT`    | `/api/user/chat/groups/{conversationId}/owner`                       |
| 设置成员禁言   | `PUT`    | `/api/user/chat/groups/{conversationId}/members/{memberUserId}/mute` |
| 更新群公告    | `PUT`    | `/api/user/chat/groups/{conversationId}/notice`                      |
| 移除群成员    | `DELETE` | `/api/user/chat/groups/{conversationId}/members/{memberUserId}`      |
| 退出群聊     | `POST`   | `/api/user/chat/groups/{conversationId}/leave`                       |
| 解散群聊     | `DELETE` | `/api/user/chat/groups/{conversationId}`                             |

### 3.2 会话查询

`GET /api/user/chat/conversations`

请求参数：

| 参数        | 位置    | 必填 | 说明              |
|-----------|-------|----|-----------------|
| `current` | query | 否  | 页码，默认 `1`       |
| `size`    | query | 否  | 每页条数，默认 `20`    |
| `keyword` | query | 否  | 按会话名、最后一条消息内容筛选 |

会话对象重点字段：

| 字段                 | 说明                                  |
|--------------------|-------------------------------------|
| `conversationType` | `single/group/global`               |
| `notice`           | 群公告；当前复用 `chat_conversation.remark` |
| `selfRole`         | 当前用户角色：`owner/admin/member`         |
| `memberCount`      | 当前活跃成员数                             |
| `unreadCount`      | 当前用户未读数                             |
| `lastMessage`      | 最后一条消息摘要                            |

说明：

- 首次访问时，如果全站群不存在成员关系，服务端会自动补建。
- 单聊详情会额外返回 `targetUserId / targetUsername / targetNickname`。

### 3.3 消息查询

`GET /api/user/chat/conversations/{conversationId}/messages`

请求参数：

| 参数                | 位置    | 必填 | 说明                |
|-------------------|-------|----|-------------------|
| `current`         | query | 否  | 页码，默认 `1`         |
| `size`            | query | 否  | 每页条数，默认 `20`      |
| `beforeMessageId` | query | 否  | 只查询该消息 ID 之前的历史消息 |

消息对象重点字段：

| 字段                  | 说明                               |
|---------------------|----------------------------------|
| `messageType`       | `text/file/image/voice`          |
| `content`           | 文本内容或附件摘要；撤回后固定为“消息已撤回”          |
| `file`              | 附件消息载荷；文本消息为空                    |
| `replyMessageId`    | 被回复消息 ID；未回复时为空                  |
| `reply`             | 被回复消息快照；如旧消息未落快照且原消息已不可见，会返回占位快照 |
| `deliveryStatus`    | 当前用户视角下的投递状态：`0/1/2`             |
| `readByCurrentUser` | 当前用户是否已读                         |
| `revoked`           | 是否已撤回                            |
| `edited`            | 是否编辑过；当前仅文本消息可能为 `true`          |
| `updatedAt`         | 更新时间                             |

附件载荷 `file` 字段：

| 字段                | 说明                                          |
|-------------------|---------------------------------------------|
| `businessId`      | 聊天文件业务引用 ID                                 |
| `fileId`          | 文件 ID                                       |
| `fileName`        | 文件名                                         |
| `originalName`    | 原始文件名                                       |
| `fileUrl`         | 文件地址                                        |
| `fileSize`        | 文件大小                                        |
| `fileType`        | 文件类型                                        |
| `mimeType`        | MIME 类型                                     |
| `previewUrl`      | 预览地址，图片/语音当前复用原文件地址                         |
| `thumbnailUrl`    | 缩略图地址；图片刚发送时可能先回落原图，媒体任务完成后会更新为 sidecar 缩略图 |
| `width`           | 图片宽度，异步媒体任务完成前可能为空                          |
| `height`          | 图片高度，异步媒体任务完成前可能为空                          |
| `durationSeconds` | 语音时长，异步媒体任务完成前可能为空                          |
| `waveform`        | 语音波形采样点，异步媒体任务完成前可能为空                       |
| `transcodeStatus` | 转码状态：`source/pending/ready/failed`          |

回复快照 `reply` 字段：

| 字段                 | 说明                                |
|--------------------|-----------------------------------|
| `id`               | 被回复消息 ID                          |
| `senderId`         | 被回复消息发送人 ID                       |
| `senderUsername`   | 被回复消息发送人用户名                       |
| `senderNickname`   | 被回复消息发送人昵称                        |
| `senderAvatar`     | 被回复消息发送人头像                        |
| `messageType`      | 被回复消息类型                           |
| `replyToMessageId` | 被回复消息自己又回复了哪条消息，仅作为状态链接，不继续内联多层快照 |
| `content`          | 被回复消息摘要                           |
| `file`             | 被回复消息附件快照                         |
| `revoked`          | 被回复消息是否已撤回                        |
| `deleted`          | 仅用于提示原消息当前不可见的兜底状态                |
| `state`            | 当前状态：`normal/revoked/unavailable` |
| `createdAt`        | 被回复消息发送时间                         |

说明：

- 当前实现采用“在线即 delivered”语义：如果发送时服务端检测到接收方存在在线 WebSocket 会话，会立即把 recipient 推进到
  `已送达`。
- 用户拉取历史消息时，如命中此前仍是 `待投递` 的消息，也会补记为 `已送达`；`deliveredMessageId` / `lastDeliveredMessageId`
  只会单调前进，不会被并发旧事务回退。
- 新消息发送时会把被回复消息摘要快照持久化到 payload；旧消息若没有快照，服务端会尽量回查原消息并补齐。
- 若被回复消息当前仍可见，接口会优先返回原消息的实时摘要，因此编辑或撤回后，`reply.content / reply.state / reply.revoked`
  会跟随更新；只有原消息不可见时才回退到 payload 快照。
- 图片和语音消息发送成功后会先返回基础载荷，随后由异步媒体任务补齐缩略图、WAV 预览、时长和波形，并通过 `message_updated`
  推给在线成员。
- 若旧消息的原始被回复消息已经不可见，`reply.deleted = true`，`reply.state = unavailable`，`reply.content` 会回退为“引用消息已不可见”。
- 当前不会返回多层 `reply.reply...` 结构；如需展示“被引用消息本身也是回复”，前端可结合 `reply.replyToMessageId` 做弱提示或跳转入口。

### 3.4 发送文本消息

`POST /api/user/chat/messages/text`

请求体支持两种模式：

已有会话发送：

```json
{
  "conversationId": 1001,
  "content": "hello",
  "clientMessageId": "msg-001",
  "replyMessageId": 90001
}
```

自动建立单聊发送：

```json
{
  "targetUserId": 2,
  "content": "hello",
  "clientMessageId": "msg-001",
  "replyMessageId": 90001
}
```

说明：

- `conversationId` 和 `targetUserId` 不能同时为空。
- `clientMessageId` 作为同一发送人的幂等键；并发重复发送命中唯一键冲突时，服务端会回查并返回已落库消息。
- `replyMessageId` 非空时，必须是当前用户在同一会话内可见的消息。
- 当前成员被禁言时会拒绝发送。
- 聊天域当前还会按用户维度做分钟级发送频控，并按系统配置 `chat.sensitive-words` 做基础敏感词拦截。
- 发送成功后，服务端会把被回复消息的摘要快照一并写入消息 payload，避免后续前端必须二次查原消息。

### 3.5 发送文件消息

`POST /api/user/chat/messages/file`

请求体：

```json
{
  "conversationId": 1001,
  "businessId": 501,
  "clientMessageId": "file-001",
  "replyMessageId": 90001
}
```

或：

```json
{
  "targetUserId": 2,
  "businessId": 501
}
```

说明：

- `businessId` 来自 `file` 模块上传完成后的业务引用 ID。
- 当前只接受：
    - 上传阶段的临时引用 `temp`
    - 尚未绑定具体消息的 `chat_message` 引用
- 服务端会根据 `file_info.mime_type` 自动推断消息类型：
    - `image/*` -> `image`
    - `audio/*` -> `voice`
    - 其他 -> `file`
- 附件载荷当前会统一补出 `previewUrl / thumbnailUrl / transcodeStatus` 等扩展字段；发送成功后会触发异步媒体任务，补齐图片缩略图、语音
  WAV 预览、时长和波形。
- 语音消息初始通常返回 `transcodeStatus = pending`；如果异步转码成功会更新成 `ready`，失败则更新成 `failed` 并继续保留原始
  `previewUrl`。
- 若底层存储或音频格式暂不支持自动解析/转码，对应 `width / height / durationSeconds / waveform` 仍可能为空，或
  `transcodeStatus = failed`；这不影响消息主流程发送成功。
- `replyMessageId` 非空时，同样要求当前用户在该会话内可见。
- 发送成功后，服务端会把文件业务引用重绑到 `chat_message`，并清理临时引用。
- `clientMessageId` 的并发重复提交同样会回查并返回已存在消息，不重复生成新的聊天记录。

### 3.6 编辑 / 撤回 / 删除

编辑：

- 方法：`PUT`
- 路径：`/api/user/chat/messages/{messageId}`

```json
{
  "content": "修改后的内容"
}
```

约束：

- 仅允许编辑自己发送的文本消息
- 文件消息、已撤回消息不允许编辑
- 编辑成功后，服务端会向当前会话活跃成员推送 `message_updated`

撤回：

- 方法：`POST`
- 路径：`/api/user/chat/messages/{messageId}/revoke`

说明：

- 当前仅允许撤回自己发送的消息
- 撤回后消息主记录保留，但内容会改为“消息已撤回”
- 文件消息撤回时会释放聊天引用
- 撤回成功后，服务端会向当前会话活跃成员推送 `message_revoked`

删除我的消息视图：

- 方法：`DELETE`
- 路径：`/api/user/chat/messages/{messageId}`

说明：

- 仅隐藏当前用户视角，不影响其他成员
- 删除后会重新计算当前会话未读数
- 删除成功后，服务端会只向当前用户在线会话推送 `message_deleted`，用于多标签页同步本人视图

### 3.7 推进会话已读

`POST /api/user/chat/conversations/{conversationId}/read`

```json
{
  "readMessageId": 90013
}
```

响应会返回：

- `conversationId`
- `userId`
- `readMessageId`
- `readAt`
- `deliveredMessageId`
- `deliveredAt`
- `unreadCount`

### 3.8 群治理接口

创建群聊：

```json
POST /api/user/chat/groups
{
  "name": "项目群",
  "avatar": "https://example.com/group.png",
  "memberUserIds": [2, 3, 4]
}
```

群治理规则：

- 群主可设置/取消管理员、转让群主、禁言管理员和普通成员、更新公告、移除管理员和普通成员
- 管理员可邀请成员、禁言普通成员、更新公告、移除普通成员
- 管理员不能操作群主，也不能操作其他管理员

设置群管理员：

```http
PUT /api/user/chat/groups/{conversationId}/admins/{memberUserId}
```

取消群管理员：

```http
DELETE /api/user/chat/groups/{conversationId}/admins/{memberUserId}
```

转让群主：

```json
PUT /api/user/chat/groups/{conversationId}/owner
{
  "targetUserId": 2
}
```

设置禁言：

```json
PUT /api/user/chat/groups/{conversationId}/members/{memberUserId}/mute
{
  "muteUntil": "2026-03-31 12:00:00"
}
```

更新群公告：

```json
PUT /api/user/chat/groups/{conversationId}/notice
{
  "notice": "今晚 8 点发版，请提前同步。"
}
```

移除群成员：

```http
DELETE /api/user/chat/groups/{conversationId}/members/{memberUserId}
```

说明：

- 用户不能通过“移除成员”接口移除自己
- 群主不能直接退群，只能先转让群主或解散
- 邀请成员、设置管理员、取消管理员、转让群主、禁言成员、移除成员、成员退群后，服务端会推送 `members_updated`
- 转让群主、更新群公告、解散群聊后，服务端还会补推 `conversation_updated`
- 群解散后，用户侧会话详情和历史消息接口都会视为“会话不可用”；如需审计历史消息，请走后台聊天管理接口
- 全站群会在访问消息时自动补建/恢复当前用户成员资格，但邀请、退群、移除成员、管理员任免、群公告等普通群治理接口不适用于全站群

## 4. 后台聊天管理接口

### 4.1 接口总览

| 接口       | 方法     | 路径                                                                            | 权限                    |
|----------|--------|-------------------------------------------------------------------------------|-----------------------|
| 分页查询会话   | `GET`  | `/api/sys/chats/conversations`                                                | `content:chat:query`  |
| 查询会话详情   | `GET`  | `/api/sys/chats/conversations/{conversationId}`                               | `content:chat:query`  |
| 查询会话成员   | `GET`  | `/api/sys/chats/conversations/{conversationId}/members`                       | `content:chat:query`  |
| 分页查询会话消息 | `GET`  | `/api/sys/chats/conversations/{conversationId}/messages`                      | `content:chat:query`  |
| 查询消息详情   | `GET`  | `/api/sys/chats/conversations/{conversationId}/messages/{messageId}`          | `content:chat:query`  |
| 分页查询消息回执 | `GET`  | `/api/sys/chats/conversations/{conversationId}/messages/{messageId}/receipts` | `content:chat:query`  |
| 更新成员角色   | `PUT`  | `/api/sys/chats/conversations/{conversationId}/members/{memberUserId}/role`   | `content:chat:update` |
| 更新成员状态   | `PUT`  | `/api/sys/chats/conversations/{conversationId}/members/{memberUserId}/status` | `content:chat:update` |
| 更新成员禁言   | `PUT`  | `/api/sys/chats/conversations/{conversationId}/members/{memberUserId}/mute`   | `content:chat:update` |
| 后台撤回消息   | `POST` | `/api/sys/chats/conversations/{conversationId}/messages/{messageId}/revoke`   | `content:chat:update` |
| 更新会话状态   | `PUT`  | `/api/sys/chats/conversations/{conversationId}/status`                        | `content:chat:update` |

### 4.2 会话与消息分页

会话分页：

- `GET /api/sys/chats/conversations`
- 支持 `keyword / conversationType / status / ownerId / memberUserId / isAllSite`

消息分页：

- `GET /api/sys/chats/conversations/{conversationId}/messages`
- 支持 `beforeMessageId / senderId / messageType / keyword`

后台消息对象重点字段：

| 字段                        | 说明                      |
|---------------------------|-------------------------|
| `messageType`             | `text/file/image/voice` |
| `file`                    | 附件消息载荷                  |
| `replyMessageId`          | 被回复消息 ID                |
| `reply`                   | 被回复消息快照                 |
| `revokeStatus`            | 撤回状态                    |
| `revokedBy`               | 撤回操作人 ID                |
| `revokedAt`               | 撤回时间                    |
| `totalRecipientCount`     | 接收成员总数                  |
| `deliveredRecipientCount` | 已送达成员数                  |
| `readRecipientCount`      | 已读成员数                   |
| `edited`                  | 是否编辑过                   |
| `updatedAt`               | 更新时间                    |

### 4.3 消息详情

`GET /api/sys/chats/conversations/{conversationId}/messages/{messageId}`

返回内容在消息分页基础上，额外强调单条消息完整视角，适合后台详情抽屉或审计页。

### 4.4 消息回执明细

`GET /api/sys/chats/conversations/{conversationId}/messages/{messageId}/receipts`

请求参数：

| 参数                | 位置    | 必填 | 说明                 |
|-------------------|-------|----|--------------------|
| `current`         | query | 否  | 页码，默认 `1`          |
| `size`            | query | 否  | 每页条数，默认 `20`       |
| `recipientUserId` | query | 否  | 接收人用户 ID           |
| `deliveryStatus`  | query | 否  | `0-待投递，1-已送达，2-已读` |
| `visibleStatus`   | query | 否  | `0-已隐藏，1-可见`       |

响应记录重点字段：

- `recipientUserId`
- `recipientUsername`
- `recipientNickname`
- `receiveType`
- `deliveryStatus`
- `deliveredAt`
- `readAt`
- `visibleStatus`

### 4.5 后台成员管理

更新角色：

```json
PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/role
{
  "role": "admin"
}
```

支持值：`owner/admin/member`

更新状态：

```json
PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/status
{
  "status": 3
}
```

支持值：

- `0` 已退出
- `1` 正常
- `2` 已移除
- `3` 已禁用

更新禁言：

```json
PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/mute
{
  "muteUntil": "2026-03-31 12:00:00"
}
```

说明：

- 把成员角色更新为 `owner` 时，服务端会同步更新 `chat_conversation.owner_id`
- 后台成员治理当前仅支持普通群聊会话，单聊和全站群会直接拒绝
- 角色/状态/禁言更新成功后，服务端会推送 `members_updated`
- 群主转移类角色变更还会补推 `conversation_updated`

### 4.6 后台撤回消息

`POST /api/sys/chats/conversations/{conversationId}/messages/{messageId}/revoke`

说明：

- 已撤回消息再次调用会直接返回成功，不重复报错
- 文件消息撤回时同样会释放聊天文件引用

### 4.7 更新会话状态

`PUT /api/sys/chats/conversations/{conversationId}/status`

```json
{
  "status": 0
}
```

当前后台只支持：

- `0` 禁用
- `1` 正常

## 5. WebSocket 协议

### 5.1 当前支持的客户端请求

- `ping`
- `send_message`
- `mark_read`

### 5.2 当前支持的服务端推送

- `ready`
- `pong`
- `ack`
- `message_created`
- `message_updated`
- `message_revoked`
- `message_deleted`
- `conversation_updated`
- `members_updated`
- `read_updated`
- `error`

### 5.3 `send_message`

请求：

```json
{
  "type": "send_message",
  "requestId": "req-001",
  "payload": {
    "conversationId": 1001,
    "content": "hello",
    "clientMessageId": "msg-001",
    "replyMessageId": 90001
  }
}
```

说明：

- 当前 WebSocket 侧 `send_message` 仍只映射到文本消息发送
- `replyMessageId` 的校验规则与 HTTP 文本消息发送保持一致
- 文件消息仍建议走 HTTP

### 5.4 `mark_read`

请求：

```json
{
  "type": "mark_read",
  "requestId": "req-002",
  "payload": {
    "conversationId": 1001,
    "readMessageId": 90013
  }
}
```

### 5.5 当前限制

- 当前客户端主动请求仍只有 `send_message`、`mark_read` 两类业务请求
- `message_updated` / `message_revoked` / `message_deleted` / `conversation_updated` / `members_updated`
  都是服务端推送事件，客户端不能反向发送
- `message_deleted` 只会推给执行删除操作的当前用户，不会广播给其他成员
- 回复快照当前是单层摘要，不支持多层嵌套回复块，但会补 `reply.state` 与 `reply.replyToMessageId`
- 后台审计当前仍以 HTTP 详情 / 回执接口为主，没有额外拆出一套后台专属 WS 事件
- 当前多节点下，服务端会先推送本机在线连接，再通过 Redis pub/sub 广播到其他节点
- 媒体任务完成后会继续复用 `message_updated` 通知前端刷新同一条消息，无需额外轮询 HTTP

## 6. 联调建议

- 文件消息先走 `file` 模块上传，拿到 `businessId` 后再调用 chat 发送文件消息。
- 会话列表、会话详情、后台会话详情当前都会返回 `notice`，前端不要再额外拼群公告来源。
- 若前端需要区分“撤回”和“删除”：
    - 撤回看消息对象的 `revoked`
    - 删除当前仅是本人视角隐藏，其他成员不会看到变化事件
- 后台做审计时，优先组合使用：
    - 会话消息分页
    - 单条消息详情
    - 消息回执明细

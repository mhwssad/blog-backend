# WebSocket 实时通信接入文档

本文档面向前端开发者，说明如何建立 WebSocket 连接、处理认证、收发消息与处理服务端推送事件。

## 1. 连接地址

```
ws(s)://{host}/ws/chat?accessToken={jwt}
```

- 开发环境示例：`ws://localhost:8000/ws/chat?accessToken=eyJhbGci...`
- 生产环境使用 `wss://`

### 1.1 认证方式

浏览器原生 `WebSocket` API 不支持自定义请求头，因此采用 **Query 参数** 传递令牌：

```js
const accessToken = localStorage.getItem("accessToken");
const socket = new WebSocket(
  `ws://localhost:8000/ws/chat?accessToken=${accessToken}`,
);
```

令牌解析优先级：

| 优先级 | 来源                     | 说明        |
|-----|------------------------|-----------|
| 1   | `Authorization` 请求头    | 非浏览器客户端可用 |
| 2   | Query 参数 `accessToken` | 浏览器推荐方式   |

认证失败时握手阶段直接返回 HTTP `401`，不会建立连接。

### 1.2 连接生命周期

```
客户端发起连接 -> 握手认证 -> 连接建立 -> 收到 ready 事件 -> 正常通信
```

连接建立后，服务端会 **立即** 发送一条 `ready` 消息（见第 4 节）。收到 `ready` 即表示连接可用。

## 2. 消息格式

所有通信使用 **JSON 文本帧**，客户端和服务端都遵循统一的信封格式。

### 2.1 客户端请求

```json
{
  "type": "send_message",
  "requestId": "req-001",
  "payload": { ... }
}
```

| 字段          | 类型     | 必填  | 说明                               |
|-------------|--------|-----|----------------------------------|
| `type`      | String | 是   | 消息类型，见第 3 节                      |
| `requestId` | String | 否   | 客户端生成的请求 ID，服务端会在响应中回传，用于关联请求/响应 |
| `payload`   | Object | 视类型 | 业务载荷，具体结构见各类型说明                  |

### 2.2 服务端响应

```json
{
  "type": "ack",
  "requestId": "req-001",
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": { ... }
}
```

| 字段          | 类型      | 说明                           |
|-------------|---------|------------------------------|
| `type`      | String  | 消息类型                         |
| `requestId` | String  | 回传客户端的请求 ID；服务端主动推送时为 `null` |
| `timestamp` | Long    | 服务端时间戳（毫秒）                   |
| `code`      | Integer | 业务状态码，`200` 表示成功             |
| `message`   | String  | 提示信息                         |
| `payload`   | Object  | 业务载荷，具体结构见各类型说明              |

**区分响应与推送**：

- `requestId` 非空 → 对某次客户端请求的响应（`ack` 或 `error`）
- `requestId` 为 `null` → 服务端主动推送事件

## 3. 消息类型总览

### 3.1 客户端可发送的类型

| type           | 方向        | 说明                    |
|----------------|-----------|-----------------------|
| `ping`         | 客户端 → 服务端 | 心跳检测，也可以直接发送文本 `ping` |
| `send_message` | 客户端 → 服务端 | 发送文本消息                |
| `mark_read`    | 客户端 → 服务端 | 推进会话已读                |

### 3.2 服务端推送的类型

| type                   | 方向        | 说明                 |
|------------------------|-----------|--------------------|
| `ready`                | 服务端 → 客户端 | 连接建立成功通知           |
| `pong`                 | 服务端 → 客户端 | 心跳响应               |
| `ack`                  | 服务端 → 客户端 | 业务请求确认             |
| `message_created`      | 服务端 → 客户端 | 新消息                |
| `message_updated`      | 服务端 → 客户端 | 消息内容已编辑            |
| `message_revoked`      | 服务端 → 客户端 | 消息已撤回              |
| `message_deleted`      | 服务端 → 客户端 | 消息已删除（仅推送给操作者本人）   |
| `conversation_updated` | 服务端 → 客户端 | 会话信息变更（群名、公告、群主等）  |
| `members_updated`      | 服务端 → 客户端 | 群成员变更（加入、移除、角色变化等） |
| `read_updated`         | 服务端 → 客户端 | 已读状态推进             |
| `error`                | 服务端 → 客户端 | 错误响应               |

## 4. 连接就绪 (`ready`)

连接建立后，服务端立即推送 `ready`：

```json
{
  "type": "ready",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "sessionId": "ws-session-abc123",
    "userId": 42,
    "username": "alice",
    "supportedRequestTypes": ["ping", "send_message", "mark_read"]
  }
}
```

| 字段                      | 类型       | 说明                |
|-------------------------|----------|-------------------|
| `sessionId`             | String   | WebSocket 会话 ID   |
| `userId`                | Long     | 当前登录用户 ID         |
| `username`              | String   | 当前登录用户名           |
| `supportedRequestTypes` | String[] | 服务端当前支持的客户端请求类型列表 |

前端可根据 `supportedRequestTypes` 判断服务端支持哪些操作。

## 5. 心跳 (`ping` / `pong`)

为保持连接活跃，客户端应 **定期** 发送心跳。支持两种方式：

### 5.1 文本 ping

直接发送纯文本 `ping`（不区分大小写）：

```
ping
```

服务端回复：

```json
{
  "type": "pong",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": null
}
```

### 5.2 JSON ping

```json
{
  "type": "ping",
  "requestId": "hb-001"
}
```

服务端回复（回传 `requestId`）：

```json
{
  "type": "pong",
  "requestId": "hb-001",
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": null
}
```

### 5.3 建议

- 推荐每 **30 秒** 发送一次心跳
- 若超过一定时间（如 60 秒）未收到 `pong`，应视为连接断开并触发重连

## 6. 客户端请求详情

### 6.1 发送文本消息 (`send_message`)

```json
{
  "type": "send_message",
  "requestId": "req-001",
  "payload": {
    "conversationId": 1001,
    "content": "你好！",
    "clientMessageId": "msg-001",
    "replyMessageId": 90001
  }
}
```

payload 字段：

| 字段                | 类型     | 必填                     | 说明                          |
|-------------------|--------|------------------------|-----------------------------|
| `conversationId`  | Long   | 与 `targetUserId` 二选一   | 目标会话 ID                     |
| `targetUserId`    | Long   | 与 `conversationId` 二选一 | 自动创建/查找单聊的目标用户 ID           |
| `content`         | String | 是                      | 文本内容，最大 2000 字符             |
| `clientMessageId` | String | 否                      | 客户端幂等键，最大 64 字符；重复提交返回已有消息  |
| `replyMessageId`  | Long   | 否                      | 回复的消息 ID，必须是当前用户在当前会话内可见的消息 |

成功确认 (`ack`)：

```json
{
  "type": "ack",
  "requestId": "req-001",
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "requestType": "send_message",
    "data": {
      "id": 5001,
      "conversationId": 1001,
      "senderId": 42,
      "senderUsername": "alice",
      "senderNickname": "Alice",
      "senderAvatar": "https://...",
      "messageType": "text",
      "content": "你好！",
      "file": null,
      "replyMessageId": 90001,
      "reply": {
        "id": 90001,
        "senderId": 5,
        "senderUsername": "bob",
        "senderNickname": "Bob",
        "senderAvatar": "https://...",
        "messageType": "text",
        "replyToMessageId": null,
        "content": "原始消息内容",
        "file": null,
        "revoked": false,
        "deleted": false,
        "state": "normal",
        "createdAt": "2026-03-31T10:00:00.000+00:00"
      },
      "clientMessageId": "msg-001",
      "self": true,
      "deliveryStatus": 1,
      "readByCurrentUser": true,
      "readAt": null,
      "revoked": false,
      "edited": false,
      "updatedAt": null,
      "createdAt": "2026-03-31T12:00:00.000+00:00"
    }
  }
}
```

> 文件消息（图片、语音、普通文件）当前不支持通过 WebSocket 发送，请走 HTTP `POST /api/user/chat/messages/file`。

### 6.2 推进已读 (`mark_read`)

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

payload 字段：

| 字段               | 类型   | 必填 | 说明        |
|------------------|------|----|-----------|
| `conversationId` | Long | 是  | 会话 ID     |
| `readMessageId`  | Long | 是  | 已读到的消息 ID |

成功确认 (`ack`)：

```json
{
  "type": "ack",
  "requestId": "req-002",
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "requestType": "mark_read",
    "data": {
      "conversationId": 1001,
      "userId": 42,
      "readMessageId": 90013,
      "readAt": "2026-03-31T12:00:00.000+00:00",
      "deliveredMessageId": 90010,
      "deliveredAt": "2026-03-31T11:59:00.000+00:00",
      "unreadCount": 0
    }
  }
}
```

## 7. 服务端推送事件详情

所有推送事件的 `requestId` 均为 `null`，`code` 均为 `200`。

### 7.1 新消息 (`message_created`)

当会话中有新消息产生时，服务端向该会话所有在线成员推送。

```json
{
  "type": "message_created",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "id": 5002,
    "conversationId": 1001,
    "senderId": 5,
    "senderUsername": "bob",
    "senderNickname": "Bob",
    "senderAvatar": "https://...",
    "messageType": "text",
    "content": "大家好！",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": "client-xyz",
    "self": false,
    "deliveryStatus": 1,
    "readByCurrentUser": false,
    "readAt": null,
    "revoked": false,
    "edited": false,
    "updatedAt": null,
    "createdAt": "2026-03-31T12:00:00.000+00:00"
  }
}
```

消息对象字段说明：

| 字段                  | 类型       | 说明                                       |
|---------------------|----------|------------------------------------------|
| `id`                | Long     | 消息 ID                                    |
| `conversationId`    | Long     | 所属会话 ID                                  |
| `senderId`          | Long     | 发送人 ID                                   |
| `senderUsername`    | String   | 发送人用户名                                   |
| `senderNickname`    | String   | 发送人昵称                                    |
| `senderAvatar`      | String   | 发送人头像                                    |
| `messageType`       | String   | 消息类型：`text` / `file` / `image` / `voice` |
| `content`           | String   | 文本内容；撤回后固定为"消息已撤回"                       |
| `file`              | Object   | 附件载荷，文本消息为 `null`（字段结构见下方）               |
| `replyMessageId`    | Long     | 回复的消息 ID                                 |
| `reply`             | Object   | 回复消息快照（字段结构见下方）                          |
| `clientMessageId`   | String   | 客户端幂等 ID                                 |
| `self`              | Boolean  | 是否是当前用户发送的                               |
| `deliveryStatus`    | Integer  | 投递状态：`0` 待投递 / `1` 已送达 / `2` 已读          |
| `readByCurrentUser` | Boolean  | 当前用户是否已读                                 |
| `readAt`            | DateTime | 当前用户已读时间                                 |
| `revoked`           | Boolean  | 是否已撤回                                    |
| `edited`            | Boolean  | 是否已编辑（仅文本消息）                             |
| `updatedAt`         | DateTime | 更新时间                                     |
| `createdAt`         | DateTime | 发送时间                                     |

附件载荷 `file` 字段：

| 字段                | 类型      | 说明                                             |
|-------------------|---------|------------------------------------------------|
| `businessId`      | Long    | 聊天文件业务引用 ID                                    |
| `fileId`          | Long    | 文件 ID                                          |
| `fileName`        | String  | 文件名                                            |
| `originalName`    | String  | 原始文件名                                          |
| `fileUrl`         | String  | 文件地址                                           |
| `fileSize`        | Long    | 文件大小                                           |
| `fileType`        | String  | 文件类型                                           |
| `mimeType`        | String  | MIME 类型                                        |
| `previewUrl`      | String  | 预览地址                                           |
| `thumbnailUrl`    | String  | 缩略图地址                                          |
| `width`           | Integer | 图片宽度                                           |
| `height`          | Integer | 图片高度                                           |
| `durationSeconds` | Integer | 语音时长（秒）                                        |
| `waveform`        | String  | 语音波形采样点                                        |
| `transcodeStatus` | String  | 转码状态：`source` / `pending` / `ready` / `failed` |

回复快照 `reply` 字段：

| 字段                 | 类型       | 说明                                      |
|--------------------|----------|-----------------------------------------|
| `id`               | Long     | 被回复消息 ID                                |
| `senderId`         | Long     | 发送人 ID                                  |
| `senderUsername`   | String   | 发送人用户名                                  |
| `senderNickname`   | String   | 发送人昵称                                   |
| `senderAvatar`     | String   | 发送人头像                                   |
| `messageType`      | String   | 消息类型                                    |
| `replyToMessageId` | Long     | 被回复消息自身又回复了哪条消息                         |
| `content`          | String   | 消息摘要                                    |
| `file`             | Object   | 附件快照                                    |
| `revoked`          | Boolean  | 是否已撤回                                   |
| `deleted`          | Boolean  | 原消息是否已不可见                               |
| `state`            | String   | 状态：`normal` / `revoked` / `unavailable` |
| `createdAt`        | DateTime | 发送时间                                    |

### 7.2 消息已编辑 (`message_updated`)

消息编辑成功或媒体任务完成后推送，载荷结构与 `message_created` 相同（完整的 `ChatMessageVO`）。

```json
{
  "type": "message_updated",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "id": 5001,
    "conversationId": 1001,
    "senderId": 42,
    "content": "修改后的内容",
    "edited": true,
    "updatedAt": "2026-03-31T12:05:00.000+00:00",
    "...": "..."
  }
}
```

触发场景：

- 用户编辑了自己的文本消息
- 图片消息的缩略图生成完成
- 语音消息的转码完成（`transcodeStatus` 更新）

### 7.3 消息已撤回 (`message_revoked`)

载荷结构与 `message_created` 相同，关键字段变化：

- `content` 固定为 `"消息已撤回"`
- `revoked` 为 `true`
- `file` 为 `null`（文件引用已释放）

### 7.4 消息已删除 (`message_deleted`)

仅推送给执行删除操作的用户本人，用于多标签页同步视图。

```json
{
  "type": "message_deleted",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "conversationId": 1001,
    "messageId": 5001,
    "userId": 42,
    "unreadCount": 3
  }
}
```

| 字段               | 类型      | 说明         |
|------------------|---------|------------|
| `conversationId` | Long    | 会话 ID      |
| `messageId`      | Long    | 被删除的消息 ID  |
| `userId`         | Long    | 执行删除的用户 ID |
| `unreadCount`    | Integer | 删除后该会话的未读数 |

### 7.5 已读状态推进 (`read_updated`)

会话中有成员推进已读时推送。

```json
{
  "type": "read_updated",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "conversationId": 1001,
    "userId": 5,
    "readMessageId": 5000,
    "readAt": "2026-03-31T12:00:00.000+00:00",
    "deliveredMessageId": 4999,
    "deliveredAt": "2026-03-31T11:59:00.000+00:00",
    "unreadCount": 0
  }
}
```

| 字段                   | 类型       | 说明         |
|----------------------|----------|------------|
| `conversationId`     | Long     | 会话 ID      |
| `userId`             | Long     | 推进已读的用户 ID |
| `readMessageId`      | Long     | 已读到的消息 ID  |
| `readAt`             | DateTime | 已读时间       |
| `deliveredMessageId` | Long     | 已送达到的消息 ID |
| `deliveredAt`        | DateTime | 送达时间       |
| `unreadCount`        | Integer  | 该用户的未读数    |

### 7.6 会话信息变更 (`conversation_updated`)

群名、群公告、群主转让、会话状态等变更时推送。

```json
{
  "type": "conversation_updated",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "action": "updated",
    "conversationId": 1001,
    "conversationType": "group",
    "name": "项目交流群",
    "avatar": "https://...",
    "ownerId": 1,
    "notice": "今晚 8 点发版",
    "status": 1,
    "memberCount": 15
  }
}
```

| 字段                 | 类型      | 说明                                     |
|--------------------|---------|----------------------------------------|
| `action`           | String  | 变更动作：`updated` / `owner_transferred` 等 |
| `conversationId`   | Long    | 会话 ID                                  |
| `conversationType` | String  | 会话类型：`single` / `group` / `global`     |
| `name`             | String  | 会话名称                                   |
| `avatar`           | String  | 会话头像                                   |
| `ownerId`          | Long    | 群主 ID                                  |
| `notice`           | String  | 群公告                                    |
| `status`           | Integer | 会话状态                                   |
| `memberCount`      | Long    | 当前活跃成员数                                |

### 7.7 群成员变更 (`members_updated`)

成员加入、移除、角色变化、禁言等操作后推送。

```json
{
  "type": "members_updated",
  "requestId": null,
  "timestamp": 1710000000000,
  "code": 200,
  "message": "成功",
  "payload": {
    "action": "joined",
    "conversationId": 1001,
    "affectedUserId": 42,
    "members": [
      {
        "userId": 42,
        "username": "alice",
        "nickname": "Alice",
        "avatar": "https://...",
        "role": "member",
        "status": 1,
        "joinedAt": "2026-03-31T12:00:00.000+00:00",
        "muteUntil": null
      }
    ]
  }
}
```

| 字段               | 类型     | 说明                                                     |
|------------------|--------|--------------------------------------------------------|
| `action`         | String | 变更动作：`joined` / `removed` / `role_changed` / `muted` 等 |
| `conversationId` | Long   | 会话 ID                                                  |
| `affectedUserId` | Long   | 受影响的用户 ID                                              |
| `members`        | Array  | 当前活跃成员列表                                               |

`members` 数组中的成员对象：

| 字段          | 类型       | 说明                                      |
|-------------|----------|-----------------------------------------|
| `userId`    | Long     | 用户 ID                                   |
| `username`  | String   | 用户名                                     |
| `nickname`  | String   | 昵称                                      |
| `avatar`    | String   | 头像                                      |
| `role`      | String   | 角色：`owner` / `admin` / `member`         |
| `status`    | Integer  | 状态：`0` 已退出 / `1` 正常 / `2` 已移除 / `3` 已禁用 |
| `joinedAt`  | DateTime | 加入时间                                    |
| `muteUntil` | DateTime | 禁言截止时间，`null` 表示未禁言                     |

## 8. 错误处理

### 8.1 错误响应格式

```json
{
  "type": "error",
  "requestId": "req-001",
  "timestamp": 1710000000000,
  "code": 40011,
  "message": "消息内容不能为空",
  "payload": null
}
```

### 8.2 错误码

| code    | 场景                   |
|---------|----------------------|
| `40011` | 参数错误（缺少必填字段、格式不合法等）  |
| `50008` | 消息不是合法 JSON          |
| `50000` | 系统异常                 |
| `50011` | 未实现的消息类型             |
| 其他业务码   | 对应具体业务异常（如禁言、会话不可用等） |

### 8.3 连接级错误

| 场景           | 表现                      | 处理建议                         |
|--------------|-------------------------|------------------------------|
| 令牌无效或过期      | 握手阶段 HTTP `401`         | 刷新令牌后重连                      |
| JSON 格式错误    | 收到 `error`，`code=50008` | 检查发送内容格式                     |
| 发送未支持的 type  | 收到 `error`，`code=50011` | 检查 `type` 拼写                 |
| 发送服务端专有 type | 收到 `error`，`code=40011` | 如 `message_created` 不允许客户端发送 |
| 连接意外断开       | `onclose` 触发            | 按第 9 节策略重连                   |

## 9. 前端集成建议

### 9.1 重连策略

连接断开后应自动重连，推荐策略：

- 采用 **指数退避**：首次 1 秒，逐次翻倍，最大间隔 30 秒
- 重连前检查令牌有效性，若令牌已过期则先刷新
- 重连成功后会重新收到 `ready` 事件，可根据需要重新拉取会话列表和未读数

```js
class ChatWebSocket {
  constructor(url, getToken) {
    this.url = url;
    this.getToken = getToken;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectDelay = 30000;
    this.heartbeatTimer = null;
  }

  connect() {
    const token = this.getToken();
    this.ws = new WebSocket(`${this.url}?accessToken=${token}`);

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      if (msg.type === "ready") {
        console.log("连接就绪:", msg.payload);
      }
      this.onMessage?.(msg);
    };

    this.ws.onclose = () => {
      this.stopHeartbeat();
      this.scheduleReconnect();
    };

    this.ws.onerror = () => {
      // onclose 会在 onerror 之后触发，重连逻辑放在 onclose
    };
  }

  scheduleReconnect() {
    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempts),
      this.maxReconnectDelay,
    );
    this.reconnectAttempts++;
    setTimeout(() => this.connect(), delay);
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send("ping");
      }
    }, 30000);
  }

  stopHeartbeat() {
    clearInterval(this.heartbeatTimer);
  }

  send(type, requestId, payload) {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      console.warn("WebSocket 未连接，消息已丢弃");
      return;
    }
    this.ws.send(JSON.stringify({ type, requestId, payload }));
  }
}
```

### 9.2 多标签页 / 多设备

- 同一用户可同时建立多个 WebSocket 连接，服务端会向该用户所有在线连接推送消息
- `message_deleted` 只推送给执行删除操作的本人，用于多标签页同步本人视图，其他成员不会收到
- 建议前端根据 `payload.self` 判断是否是自己发送的消息，以区分展示

### 9.3 消息发送流程建议

```
1. 用户输入消息内容
2. 前端生成 clientMessageId（UUID 或时间戳）
3. 通过 WebSocket send_message 发送
4. 等待 ack 响应（匹配 requestId）
5. 收到 ack 后标记消息为"已发送"
6. 同时会收到自己触发的 message_created，可用于确认广播到达
```

### 9.4 消息幂等

- `clientMessageId` 作为同一发送人的幂等键
- 网络超时或重试时，使用相同的 `clientMessageId` 不会产生重复消息
- 服务端会返回已有消息的完整数据

### 9.5 与 HTTP 接口配合

| 操作        | 推荐方式                                                  | 说明                      |
|-----------|-------------------------------------------------------|-------------------------|
| 发送文本消息    | WebSocket `send_message`                              | 实时性好                    |
| 发送文件消息    | HTTP `POST /api/user/chat/messages/file`              | WebSocket 不支持文件上传       |
| 拉取历史消息    | HTTP `GET /api/user/chat/conversations/{id}/messages` | 分页加载                    |
| 拉取会话列表    | HTTP `GET /api/user/chat/conversations`               | 初始化加载                   |
| 编辑消息      | HTTP `PUT /api/user/chat/messages/{id}`               | 编辑后推送 `message_updated` |
| 撤回消息      | HTTP `POST /api/user/chat/messages/{id}/revoke`       | 撤回后推送 `message_revoked` |
| 推进已读      | WebSocket `mark_read`                                 | 实时性好                    |
| 实时接收新消息   | 监听 `message_created`                                  | 不需要轮询                   |
| 实时接收编辑/撤回 | 监听 `message_updated` / `message_revoked`              | 不需要轮询                   |

## 10. 快速参考

### 消息流向速查

```
┌──────────┐                         ┌──────────┐
│  客户端  │                         │  服务端  │
└────┬─────┘                         └────┬─────┘
     │  连接 ws://host/ws/chat?accessToken=xxx
     │──────────────────────────────────►│
     │         ready (sessionId, userId) │
     │◄──────────────────────────────────│
     │                                   │
     │  ping                             │
     │──────────────────────────────────►│
     │         pong                      │
     │◄──────────────────────────────────│
     │                                   │
     │  send_message                     │
     │──────────────────────────────────►│
     │         ack (ChatMessageVO)       │
     │◄──────────────────────────────────│
     │                                   │
     │         message_created (广播)    │
     │◄──────────────────────────────────│
     │                                   │
     │  mark_read                        │
     │──────────────────────────────────►│
     │         ack (ChatReadStateVO)     │
     │◄──────────────────────────────────│
     │                                   │
     │         read_updated (推送)       │
     │◄──────────────────────────────────│
     │                                   │
     │         conversation_updated      │
     │◄──────────────────────────────────│
     │         members_updated           │
     │◄──────────────────────────────────│
     │         message_updated           │
     │◄──────────────────────────────────│
     │         message_revoked           │
     │◄──────────────────────────────────│
     │         message_deleted (仅自己)  │
     │◄──────────────────────────────────│
     │         error                     │
     │◄──────────────────────────────────│
```

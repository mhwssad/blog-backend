# WebSocket 实时通信接入文档

本文档描述聊天模块 WebSocket 通信协议，供前端开发者集成参考。

---

## 1. 连接地址

### 端点配置

| 配置项 | 值 |
|--------|-----|
| 默认端点 | `/ws/chat` |
| 令牌参数名 | `accessToken` |
| 协议 | `ws://` (HTTP) 或 `wss://` (HTTPS) |

### 连接地址格式

```
ws://{host}/ws/chat?accessToken={accessToken}
```

### 连接示例

```javascript
class ChatWebSocket {
  constructor() {
    this.socket = null;
    this.reconnectTimer = null;
    this.heartbeatTimer = null;
    this.pendingRequests = new Map();
  }

  connect(token) {
    const wsUrl = `ws://localhost:8000/ws/chat?accessToken=${token}`;
    this.socket = new WebSocket(wsUrl);
    
    this.socket.onopen = () => {
      console.log('WebSocket connected');
      this.startHeartbeat();
    };
    
    this.socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleMessage(data);
    };
    
    this.socket.onclose = (event) => {
      console.log('WebSocket disconnected', event.code, event.reason);
      this.stopHeartbeat();
      this.scheduleReconnect(token);
    };
    
    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  }

  handleMessage(data) {
    // 处理 ack 响应，唤醒等待中的请求
    if (data.type === 'ack' && data.requestId && this.pendingRequests.has(data.requestId)) {
      const { resolve, reject } = this.pendingRequests.get(data.requestId);
      this.pendingRequests.delete(data.requestId);
      if (data.code === 200) {
        resolve(data.payload);
      } else {
        reject(new Error(data.message));
      }
      return;
    }

    // 处理其他服务端推送
    switch (data.type) {
      case 'ready':
        console.log('Session ready:', data.payload);
        break;
      case 'message_created':
        console.log('New message:', data.payload);
        break;
      case 'message_updated':
        console.log('Message updated:', data.payload);
        break;
      case 'message_revoked':
        console.log('Message revoked:', data.payload);
        break;
      case 'message_deleted':
        console.log('Message deleted:', data.payload);
        break;
      case 'read_updated':
        console.log('Read status updated:', data.payload);
        break;
      case 'conversation_updated':
        console.log('Conversation updated:', data.payload);
        break;
      case 'members_updated':
        console.log('Members updated:', data.payload);
        break;
      case 'pong':
        // 心跳响应，无需处理
        break;
      case 'error':
        console.error('Server error:', data.code, data.message);
        break;
      default:
        console.log('Unknown message type:', data.type);
    }
  }

  send(type, payload) {
    return new Promise((resolve, reject) => {
      const requestId = this.generateRequestId();
      const message = { type, requestId, payload };
      
      this.pendingRequests.set(requestId, { resolve, reject });
      
      // 设置超时
      setTimeout(() => {
        if (this.pendingRequests.has(requestId)) {
          this.pendingRequests.delete(requestId);
          reject(new Error('Request timeout'));
        }
      }, 10000);
      
      this.socket.send(JSON.stringify(message));
    });
  }

  generateRequestId() {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  scheduleReconnect(token) {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    const delay = 3000;
    console.log(`Scheduling reconnect in ${delay}ms...`);
    this.reconnectTimer = setTimeout(() => {
      this.connect(token);
    }, delay);
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send('ping');
      }
    }, 30000);
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopHeartbeat();
    if (this.socket) {
      this.socket.close();
    }
  }
}

// 使用示例
const ws = new ChatWebSocket();
ws.connect('your-access-token');
```

---

## 2. 消息格式

### 2.1 客户端请求

```json
{
  "type": "send_message",
  "requestId": "1710000000000-abc123",
  "payload": {
    "conversationId": 1001,
    "content": "你好",
    "clientMessageId": "client-msg-001"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 消息类型，见 3.1 节 |
| requestId | string | 否 | 请求唯一标识，用于匹配响应 |
| payload | object | 否 | 请求业务载荷，结构视 type 而定 |

### 2.2 服务端响应

```json
{
  "type": "ack",
  "requestId": "1710000000000-abc123",
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 响应类型，见 3.2 节 |
| requestId | string | 关联的请求 ID（服务端推送时为 null） |
| timestamp | string | 服务器时间（ISO 8601） |
| code | integer | 状态码，200 表示成功 |
| message | string | 状态描述 |
| payload | object | 业务载荷 |

---

## 3. 消息类型总览

### 3.1 客户端可发送的类型

| type | 说明 | 备注 |
|------|------|------|
| `ping` | 心跳请求 | 可发送纯文本 "ping"，无需 JSON |
| `send_message` | 发送文本消息 | 需要 payload |
| `mark_read` | 推进已读游标 | 需要 payload |

### 3.2 服务端推送的类型

| type | 说明 | 方向 |
|------|------|------|
| `ready` | 连接建立成功 | 服务端 → 客户端 |
| `pong` | 心跳响应 | 服务端 → 客户端 |
| `ack` | 业务请求确认 | 服务端 → 客户端 |
| `message_created` | 新消息 | 服务端 → 客户端 |
| `message_updated` | 消息已编辑 | 服务端 → 客户端 |
| `message_revoked` | 消息已撤回 | 服务端 → 客户端 |
| `message_deleted` | 消息已删除 | 服务端 → 客户端 |
| `read_updated` | 已读状态推进 | 服务端 → 客户端 |
| `conversation_updated` | 会话信息变更 | 服务端 → 客户端 |
| `members_updated` | 群成员变更 | 服务端 → 客户端 |
| `error` | 错误响应 | 服务端 → 客户端 |

---

## 4. 连接就绪 (`ready`)

连接建立后，服务端立即发送 ready 消息，前端应等待此消息后再开始业务操作。

### 响应格式

```json
{
  "type": "ready",
  "requestId": null,
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "sessionId": "abc123def456",
    "userId": 5,
    "username": "alice",
    "supportedRequestTypes": ["ping", "send_message", "mark_read"]
  }
}
```

| payload 字段 | 类型 | 说明 |
|-------------|------|------|
| sessionId | string | WebSocket 会话 ID |
| userId | long | 当前用户 ID |
| username | string | 当前用户名 |
| supportedRequestTypes | array | 当前支持的请求类型 |

---

## 5. 心跳 (`ping` / `pong`)

### 5.1 文本 ping（推荐）

发送纯文本 `ping`，无需 JSON 格式：

```
ping
```

服务端响应（纯文本）：

```
pong
```

### 5.2 JSON ping

发送 JSON 格式 ping，可带 requestId：

```json
{
  "type": "ping",
  "requestId": "ping-001",
  "payload": null
}
```

服务端响应：

```json
{
  "type": "pong",
  "requestId": "ping-001",
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": null
}
```

### 5.3 心跳建议

- 推荐每 30 秒发送一次文本 `ping`
- 若 60 秒内未收到任何消息，应主动断开并重连
- 重连策略见第 9 节

---

## 6. 客户端请求详情

### 6.1 发送文本消息 (`send_message`)

#### 请求

```json
{
  "type": "send_message",
  "requestId": "msg-001",
  "payload": {
    "conversationId": 1001,
    "content": "你好，这是测试消息",
    "clientMessageId": "client-msg-001",
    "replyMessageId": null
  }
}
```

| payload 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| conversationId | long | 是 | 会话 ID |
| content | string | 是 | 消息内容，最大 4000 字符 |
| clientMessageId | string | 否 | 客户端消息 ID，用于幂等校验 |
| replyMessageId | long | 否 | 回复的消息 ID |

#### 成功响应 (ack)

```json
{
  "type": "ack",
  "requestId": "msg-001",
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "requestType": "send_message",
    "data": {
      "id": 5002,
      "conversationId": 1001,
      "senderId": 5,
      "senderUsername": "alice",
      "senderNickname": "Alice",
      "senderAvatar": "https://example.com/avatar/5.png",
      "messageType": "text",
      "content": "你好，这是测试消息",
      "file": null,
      "replyMessageId": null,
      "reply": null,
      "clientMessageId": "client-msg-001",
      "self": true,
      "deliveryStatus": "delivered",
      "readByCurrentUser": true,
      "readAt": null,
      "revoked": false,
      "edited": false,
      "updatedAt": null,
      "createdAt": "2024-03-09T12:00:00"
    }
  }
}
```

#### 错误响应

```json
{
  "type": "error",
  "requestId": "msg-001",
  "timestamp": "2024-03-09T12:00:00",
  "code": 40003,
  "message": "会话不存在或无权发送消息",
  "payload": null
}
```

### 6.2 推进已读 (`mark_read`)

#### 请求

```json
{
  "type": "mark_read",
  "requestId": "read-001",
  "payload": {
    "conversationId": 1001,
    "readMessageId": 5000
  }
}
```

| payload 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| conversationId | long | 是 | 会话 ID |
| readMessageId | long | 是 | 已读的消息 ID，之前的消息均视为已读 |

#### 成功响应 (ack)

```json
{
  "type": "ack",
  "requestId": "read-001",
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "requestType": "mark_read",
    "data": {
      "conversationId": 1001,
      "userId": 5,
      "readMessageId": 5000,
      "readAt": "2024-03-09T12:00:00",
      "deliveredMessageId": 5001,
      "deliveredAt": "2024-03-09T11:59:30",
      "unreadCount": 0
    }
  }
}
```

---

## 7. 服务端推送事件详情

### 7.1 新消息 (`message_created`)

当群聊/单聊中有新消息时，推送给会话所有成员。

```json
{
  "type": "message_created",
  "requestId": null,
  "timestamp": "2024-03-09T12:00:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "id": 5002,
    "conversationId": 1001,
    "senderId": 5,
    "senderUsername": "alice",
    "senderNickname": "Alice",
    "senderAvatar": "https://example.com/avatar/5.png",
    "messageType": "text",
    "content": "大家好",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": null,
    "self": false,
    "deliveryStatus": "delivered",
    "readByCurrentUser": false,
    "readAt": null,
    "revoked": false,
    "edited": false,
    "updatedAt": null,
    "createdAt": "2024-03-09T12:00:00"
  }
}
```

| payload 字段 | 类型 | 说明 |
|-------------|------|------|
| id | long | 消息 ID |
| conversationId | long | 会话 ID |
| senderId | long | 发送者用户 ID |
| senderUsername | string | 发送者用户名 |
| senderNickname | string | 发送者昵称 |
| senderAvatar | string | 发送者头像 URL |
| messageType | string | 消息类型：text, image, video, audio, file |
| content | string | 文本内容（文本消息时） |
| file | object | 文件信息（文件消息时），见文件消息格式 |
| replyMessageId | long | 回复的消息 ID |
| reply | object | 被回复的消息内容 |
| clientMessageId | string | 客户端消息 ID |
| self | boolean | 是否为当前用户发送的消息 |
| deliveryStatus | string | 投递状态：pending, delivered, read |
| readByCurrentUser | boolean | 当前用户是否已读 |
| readAt | string | 当前用户已读时间 |
| revoked | boolean | 是否已撤回 |
| edited | boolean | 是否已编辑 |
| createdAt | string | 创建时间 |

#### 文件消息格式

当 `messageType` 为 `image`、`video`、`audio` 或 `file` 时，`file` 字段包含文件信息：

```json
"file": {
  "businessId": "file-001",
  "fileId": 12345,
  "fileName": "document.pdf",
  "originalName": "原始文件名.pdf",
  "fileUrl": "https://example.com/files/document.pdf",
  "fileSize": 1024000,
  "fileType": "pdf",
  "mimeType": "application/pdf",
  "previewUrl": "https://example.com/files/preview/document.pdf",
  "thumbnailUrl": null,
  "width": null,
  "height": null,
  "durationSeconds": null,
  "waveform": null,
  "transcodeStatus": "completed"
}
```

### 7.2 消息已编辑 (`message_updated`)

消息内容被编辑后推送，更新消息显示。

```json
{
  "type": "message_updated",
  "requestId": null,
  "timestamp": "2024-03-09T12:05:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "id": 5002,
    "conversationId": 1001,
    "senderId": 5,
    "content": "编辑后的消息内容",
    "edited": true,
    "updatedAt": "2024-03-09T12:05:00"
  }
}
```

### 7.3 消息已撤回 (`message_revoked`)

消息被撤回后推送，客户端应将消息标记为已撤回状态。

```json
{
  "type": "message_revoked",
  "requestId": null,
  "timestamp": "2024-03-09T12:10:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "id": 5002,
    "conversationId": 1001,
    "senderId": 5,
    "revoked": true
  }
}
```

### 7.4 消息已删除 (`message_deleted`)

消息被删除后推送（仅推送给操作者本人）。

```json
{
  "type": "message_deleted",
  "requestId": null,
  "timestamp": "2024-03-09T12:15:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "conversationId": 1001,
    "messageId": 5002,
    "userId": 5,
    "unreadCount": 3
  }
}
```

### 7.5 已读状态推进 (`read_updated`)

当会话中其他成员的已读游标发生变化时推送。

```json
{
  "type": "read_updated",
  "requestId": null,
  "timestamp": "2024-03-09T12:20:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "conversationId": 1001,
    "userId": 5,
    "readMessageId": 5000,
    "readAt": "2024-03-09T12:20:00",
    "deliveredMessageId": 5001,
    "deliveredAt": "2024-03-09T12:19:30",
    "unreadCount": 2
  }
}
```

### 7.6 会话信息变更 (`conversation_updated`)

群聊信息（群名、公告、群主等）变更时推送。

```json
{
  "type": "conversation_updated",
  "requestId": null,
  "timestamp": "2024-03-09T12:25:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "action": "updated",
    "conversationId": 1001,
    "conversationType": "group",
    "name": "项目交流群",
    "avatar": "https://example.com/avatar/group-1001.png",
    "ownerId": 5,
    "notice": "请大家注意文明发言",
    "status": 1,
    "memberCount": 42
  }
}
```

| payload 字段 | 类型 | 说明 |
|-------------|------|------|
| action | string | 变更动作：created, updated |
| conversationId | long | 会话 ID |
| conversationType | string | 会话类型：single, group, channel |
| name | string | 会话名称 |
| avatar | string | 会话头像 |
| ownerId | long | 群主 ID |
| notice | string | 群公告 |
| status | int | 会话状态 |
| memberCount | long | 当前成员数 |

### 7.7 群成员变更 (`members_updated`)

群成员加入、退出或角色变更时推送。

```json
{
  "type": "members_updated",
  "requestId": null,
  "timestamp": "2024-03-09T12:30:00",
  "code": 200,
  "message": "成功",
  "payload": {
    "action": "joined",
    "conversationId": 1001,
    "affectedUserId": 42,
    "members": [
      {
        "userId": 42,
        "username": "bob",
        "nickname": "Bob",
        "avatar": "https://example.com/avatar/42.png",
        "role": "member",
        "status": 1,
        "joinedAt": "2024-03-09T12:30:00",
        "muteUntil": null
      }
    ]
  }
}
```

| payload 字段 | 类型 | 说明 |
|-------------|------|------|
| action | string | 变更动作：joined, left, role_changed, mute_changed |
| conversationId | long | 会话 ID |
| affectedUserId | long | 受影响用户 ID |
| members | array | 当前活跃成员列表 |

---

## 8. 错误处理

### 8.1 错误响应格式

```json
{
  "type": "error",
  "requestId": "msg-001",
  "timestamp": "2024-03-09T12:00:00",
  "code": 40003,
  "message": "会话不存在或无权发送消息",
  "payload": null
}
```

### 8.2 业务错误码

| code | 说明 | 处理建议 |
|------|------|---------|
| 200 | 成功 | - |
| 40001 | 参数错误 | 检查请求参数 |
| 40002 | 不支持的操 作 | 如发送服务端专用类型 |
| 40011 | JSON 解析失败 | 检查 JSON 格式 |
| 40101 | 登录失效 | 刷新令牌后重连 |
| 40303 | 无权限 | 检查会话权限 |
| 40403 | 会话不存在 | 检查 conversationId |
| 50001 | 系统错误 | 重试或联系技术支持 |

### 8.3 连接级错误

| 场景 | 表现 | 处理建议 |
|------|------|---------|
| 令牌无效或过期 | 握手阶段 HTTP `401` | 刷新令牌后重连 |
| JSON 格式错误 | 收到 `error`，`code=40011` | 检查发送内容格式 |
| 发送未支持的 type | 收到 `error`，`code=40002` | 检查 `type` 拼写 |
| 连接意外断开 | `onclose` 触发 | 按第 9 节策略重连 |
| 服务器内部错误 | `onerror` 触发，收到 `error` | 记录日志，稍后重试 |

---

## 9. 前端集成建议

### 9.1 重连策略

```javascript
class ChatWebSocketManager {
  constructor() {
    this.socket = null;
    this.reconnectAttempt = 0;
    this.maxReconnectAttempts = 10;
    this.baseDelay = 1000;
    this.maxDelay = 30000;
    this.heartbeatInterval = null;
    this.lastPongTime = 0;
    this.token = null;
  }

  connect(token) {
    this.token = token;
    this.reconnectAttempt = 0;
    this.doConnect();
  }

  doConnect() {
    const wsUrl = `ws://localhost:8000/ws/chat?accessToken=${this.token}`;
    console.log(`Connecting to ${wsUrl}...`);
    
    this.socket = new WebSocket(wsUrl);
    
    this.socket.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempt = 0;
      this.startHeartbeat();
    };
    
    this.socket.onmessage = (event) => this.handleMessage(event);
    
    this.socket.onclose = (event) => {
      console.log(`WebSocket closed: code=${event.code}, reason=${event.reason}`);
      this.stopHeartbeat();
      this.scheduleReconnect();
    };
    
    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  }

  handleMessage(event) {
    const data = JSON.parse(event.data);
    
    if (data.type === 'pong') {
      this.lastPongTime = Date.now();
      return;
    }
    
    // 处理其他消息...
  }

  scheduleReconnect() {
    if (this.reconnectAttempt >= this.maxReconnectAttempts) {
      console.error('Max reconnect attempts reached');
      this.notifyUser('连接失败，请刷新页面');
      return;
    }
    
    // 指数退避，最大 30 秒
    const delay = Math.min(
      this.baseDelay * Math.pow(2, this.reconnectAttempt),
      this.maxDelay
    );
    
    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempt + 1}/${this.maxReconnectAttempts})`);
    this.reconnectAttempt++;
    
    setTimeout(() => this.doConnect(), delay);
  }

  startHeartbeat() {
    // 每 30 秒发送一次 ping
    this.heartbeatInterval = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        // 检查是否收到过 pong，超时则断开
        if (this.lastPongTime > 0 && Date.now() - this.lastPongTime > 60000) {
          console.warn('No pong received for 60s, closing connection');
          this.socket.close();
          return;
        }
        this.socket.send('ping');
      }
    }, 30000);
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
    this.lastPongTime = 0;
  }

  notifyUser(message) {
    // 可结合 UI 框架显示通知
    console.warn('Chat notification:', message);
  }

  disconnect() {
    this.reconnectAttempt = this.maxReconnectAttempts; // 阻止自动重连
    this.stopHeartbeat();
    if (this.socket) {
      this.socket.close();
    }
  }

  // 发送消息并等待 ack
  async sendMessage(conversationId, content, clientMessageId) {
    return this.send('send_message', {
      conversationId,
      content,
      clientMessageId
    });
  }

  async markRead(conversationId, readMessageId) {
    return this.send('mark_read', {
      conversationId,
      readMessageId
    });
  }

  send(type, payload) {
    return new Promise((resolve, reject) => {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        reject(new Error('WebSocket not connected'));
        return;
      }
      
      const requestId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      const message = JSON.stringify({ type, requestId, payload });
      
      this.socket.send(message);
      
      // 简单超时处理，实际项目可配合 pendingRequests Map
      const timeout = setTimeout(() => {
        reject(new Error('Request timeout'));
      }, 10000);
      
      // 实际使用时应在 handleMessage 中根据 requestId 匹配响应
      // 这里简化处理，假设服务端会立即响应
      resolve({ requestId });
    });
  }
}
```

### 9.2 多标签页 / 多设备

- 每个标签页独立建立 WebSocket 连接
- 服务端通过 `read_updated` 事件协调各端的已读状态
- 建议在多标签页场景下，活跃标签页才建立长连接，非活跃标签页可断开以节省资源

### 9.3 消息发送流程建议

1. 用户输入消息内容
2. 前端生成 `clientMessageId`（UUID 或时间戳 + 随机字符串）
3. 通过 WebSocket `send_message` 发送
4. 等待 `ack` 响应（匹配 `requestId`）
5. 收到 `ack` 后标记消息为"已发送"状态
6. 同时会收到自己触发的 `message_created`，可用于确认消息已广播到服务器

### 9.4 消息幂等

- 使用 `clientMessageId` 实现幂等发送
- 若未收到 `ack` 就断开连接，重新发送相同 `clientMessageId` 的消息
- 服务端会拒绝重复的 `clientMessageId`

### 9.5 与 HTTP 接口配合

| 操作 | 推荐方式 | 说明 |
|------|---------|------|
| 发送文本消息 | WebSocket `send_message` | 实时性好 |
| 发送文件消息 | HTTP `POST /api/user/chat/messages/file` | WebSocket 不支持文件上传 |
| 拉取历史消息 | HTTP `GET /api/user/chat/conversations/{id}/messages` | 分页加载 |
| 拉取会话列表 | HTTP `GET /api/user/chat/conversations` | 初始化加载 |
| 编辑消息 | HTTP `PUT /api/user/chat/messages/{id}` | 编辑后推送 `message_updated` |
| 撤回消息 | HTTP `POST /api/user/chat/messages/{id}/revoke` | 撤回后推送 `message_revoked` |
| 推进已读 | WebSocket `mark_read` | 实时性好 |
| 实时接收新消息 | 监听 `message_created` | 不需要轮询 |
| 实时接收编辑/撤回 | 监听 `message_updated` / `message_revoked` | 不需要轮询 |

---

## 10. 快速参考

### 10.1 消息流向速查

```
客户端                              服务端
  |                                   |
  |-------- connect(token) ---------->|
  |                                   |
  |<------- ready (sessionId) --------|
  |                                   |
  |-------- ping -------------------->|
  |<------- pong ---------------------|
  |                                   |
  |-------- send_message ------------>|
  |<------- ack ----------------------|
  |<------- message_created (广播) ---|
  |                                   |
  |-------- mark_read -------------->|
  |<------- ack ----------------------|
  |<------- read_updated (广播) ------|
  |                                   |
  |-------- disconnect -------------->|
  |                                   |
```

### 10.2 常用代码模板

#### 初始化连接

```javascript
const chatWs = new ChatWebSocket();
chatWs.connect(accessToken);
```

#### 发送消息

```javascript
try {
  const result = await chatWs.sendMessage(conversationId, content, clientMessageId);
  console.log('Message sent:', result);
} catch (error) {
  console.error('Send failed:', error);
}
```

#### 推进已读

```javascript
try {
  await chatWs.markRead(conversationId, readMessageId);
} catch (error) {
  console.error('Mark read failed:', error);
}
```

#### 断开连接

```javascript
chatWs.disconnect();
```

---

## 附录：枚举值说明

### 会话类型 (conversationType)

| 值 | 说明 |
|----|------|
| `single` | 单聊 |
| `group` | 群聊 |
| `channel` | 频道 |

### 消息类型 (messageType)

| 值 | 说明 |
|----|------|
| `text` | 文本消息 |
| `image` | 图片消息 |
| `video` | 视频消息 |
| `audio` | 音频消息 |
| `file` | 文件消息 |

### 投递状态 (deliveryStatus)

| 值 | 说明 |
|----|------|
| `pending` | 发送中 |
| `delivered` | 已送达 |
| `read` | 已读 |

### 成员角色 (role)

| 值 | 说明 |
|----|------|
| `owner` | 群主 |
| `admin` | 管理员 |
| `member` | 普通成员 |

### 变更动作 (action)

| 值 | 说明 |
|----|------|
| `joined` | 成员加入 |
| `left` | 成员退出 |
| `role_changed` | 角色变更 |
| `mute_changed` | 禁言状态变更 |
| `updated` | 会话信息更新 |
| `created` | 会话创建 |
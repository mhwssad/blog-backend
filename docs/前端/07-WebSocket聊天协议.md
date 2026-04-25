# 07 - WebSocket 聊天协议

## 1. 连接

### 连接地址

```
ws://localhost:8000/ws/chat?accessToken=<jwt>
```

### 认证方式

通过 URL 查询参数 `accessToken` 传递 JWT Token 进行认证。服务端在 WebSocket 握手阶段验证 Token 有效性：

- Token 有效 → 握手成功，建立连接
- Token 无效/过期 → 握手被拒绝，返回 401

### 连接失败处理

| 错误    | 原因          | 处理           |
|-------|-------------|--------------|
| 401   | Token 无效或过期 | 刷新 Token 后重连 |
| 403   | 无权限         | 提示用户         |
| 连接超时  | 网络问题        | 按重连策略自动重连    |
| 连接被关闭 | 服务端重启       | 按重连策略自动重连    |

```javascript
// 连接示例
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`ws://localhost:8000/ws/chat?accessToken=${token}`);

ws.onerror = (event) => {
  console.error('WebSocket 连接错误', event);
};

ws.onclose = (event) => {
  if (event.code === 4001) {
    // Token 无效，需要重新登录
    redirectToLogin();
  } else {
    // 其他原因，自动重连
    scheduleReconnect();
  }
};
```

## 2. 连接就绪 (ready)

连接建立成功后，服务端会立即推送一条 `ready` 消息，表示连接已就绪，客户端可以开始发送消息。

```json
{
  "type": "ready",
  "requestId": null,
  "timestamp": 1735123456789,
  "code": 200,
  "message": "连接就绪",
  "payload": {
    "userId": 1001,
    "username": "zhangsan"
  }
}
```

客户端收到 `ready` 后即可正常收发消息。

## 3. 消息格式

### 通用信封结构

所有消息都使用统一的信封格式包装。

#### 客户端请求 (ChatWsRequest)

```json
{
  "type": "send_message",        // String - 消息类型
  "requestId": "req_1735123456",  // String - 请求ID（用于匹配 ack 响应）
  "payload": {                    // JsonNode - 业务载荷，根据 type 不同而不同
    // ... 具体内容
  }
}
```

字段说明：

| 字段        | 类型       | 必填 | 说明                          |
|-----------|----------|----|-----------------------------|
| type      | String   | 是  | 消息类型，见 ChatWsMessageType 枚举 |
| requestId | String   | 是  | 客户端生成的唯一请求ID，用于匹配服务端 ack    |
| payload   | JsonNode | 否  | 业务载荷，部分类型（如 ping）不需要        |

#### 服务端响应 (ChatWsResponse)

```json
{
  "type": "message_created",     // String - 消息类型
  "requestId": "req_1735123456",  // String - 对应的请求ID（推送消息为 null）
  "timestamp": 1735123456789,     // Long - 服务器时间戳（毫秒）
  "code": 200,                    // Integer - 状态码
  "message": "success",           // String - 状态消息
  "payload": {                    // T - 业务载荷
    // ... 具体内容
  }
}
```

字段说明：

| 字段        | 类型      | 说明                       |
|-----------|---------|--------------------------|
| type      | String  | 消息类型                     |
| requestId | String  | 对应客户端请求ID，服务端主动推送时为 null |
| timestamp | Long    | 服务器时间戳（毫秒）               |
| code      | Integer | 状态码，200 表示成功             |
| message   | String  | 状态描述                     |
| payload   | T       | 业务载荷，泛型，类型取决于 type       |

## 4. 客户端请求详情

### send_message - 发送消息

```json
{
  "type": "send_message",
  "requestId": "req_1701001001",
  "payload": {
    "conversationId": 2001,           // Long, 可选 - 会话ID（已存在会话时优先传）
    "targetUserId": 3001,             // Long, 可选 - 单聊目标用户ID（无会话ID时用来自动创建/获取单聊）
    "content": "你好，在吗？",          // String, 必填 - 文本消息内容，最大2000字
    "clientMessageId": "msg_abc123",   // String, 可选 - 客户端幂等消息ID，最大64字
    "replyMessageId": 5001             // Long, 可选 - 回复的消息ID
  }
}
```

字段优先级说明：

- 同时传 `conversationId` 和 `targetUserId` 时，以 `conversationId` 为准
- 仅传 `targetUserId` 时，服务端自动查找或创建单聊会话

### mark_read - 标记已读

```json
{
  "type": "mark_read",
  "requestId": "req_1701001002",
  "payload": {
    "conversationId": 2001
  }
}
```

### ping - 心跳

发送裸文本 `"ping"` 或使用标准信封格式：

```
ping
```

或者：

```json
{
  "type": "ping",
  "requestId": null,
  "payload": null
}
```

> 注意：ping 消息支持发送纯文本 `"ping"`，服务端同时支持两种格式。

## 5. 服务端推送详情

### ready - 连接就绪

连接建立后服务端主动推送。

```json
{
  "type": "ready",
  "requestId": null,
  "timestamp": 1735123456789,
  "code": 200,
  "message": "连接就绪",
  "payload": {
    "userId": 1001,
    "username": "zhangsan"
  }
}
```

### pong - 心跳响应

响应客户端的 ping 请求。

```json
{
  "type": "pong",
  "requestId": null,
  "timestamp": 1735123460000,
  "code": 200,
  "message": "pong",
  "payload": null
}
```

### ack - 请求确认

确认收到客户端请求，`requestId` 与客户端请求对应。

```json
{
  "type": "ack",
  "requestId": "req_1701001001",
  "timestamp": 1735123461000,
  "code": 200,
  "message": "消息已接收",
  "payload": null
}
```

### message_created - 新消息

新消息创建后推送给会话内所有成员。payload 为 ChatMessageVO 完整结构。

```json
{
  "type": "message_created",
  "requestId": null,
  "timestamp": 1735123462000,
  "code": 200,
  "message": "success",
  "payload": {
    "id": 6001,                                  // Long - 消息ID
    "conversationId": 2001,                      // Long - 会话ID
    "senderId": 1001,                            // Long - 发送人ID
    "senderUsername": "zhangsan",                // String - 发送人用户名
    "senderNickname": "张三",                     // String - 发送人昵称
    "senderAvatar": "/avatars/zhangsan.jpg",     // String - 发送人头像
    "messageType": "text",                       // String - 消息类型（text/image/file/voice/system）
    "content": "你好，在吗？",                     // String - 文本内容
    "file": null,                                // ChatFilePayloadVO - 文件消息载荷（文本消息为null）
    "replyMessageId": 5001,                      // Long - 回复的消息ID
    "reply": {                                   // ChatReplyMessageVO - 回复消息快照
      "id": 5001,                                // Long - 被回复消息ID
      "senderId": 3001,                          // Long - 被回复消息发送人ID
      "senderUsername": "lisi",                  // String - 被回复消息发送人用户名
      "senderNickname": "李四",                   // String - 被回复消息发送人昵称
      "senderAvatar": "/avatars/lisi.jpg",       // String - 被回复消息发送人头像
      "messageType": "text",                     // String - 被回复消息类型
      "replyToMessageId": null,                  // Long - 上一层引用消息ID（不继续内联多层）
      "content": "大家好",                        // String - 被回复消息摘要内容
      "file": null,                              // ChatFilePayloadVO - 被回复消息附件快照
      "revoked": false,                          // Boolean - 被回复消息是否已撤回
      "deleted": false,                          // Boolean - 被回复消息是否不可见
      "state": "normal",                         // String - 被回复消息状态：normal/revoked/unavailable
      "createdAt": "2025-12-25T10:00:00.000+08:00"  // Date - 被回复消息发送时间
    },
    "clientMessageId": "msg_abc123",             // String - 客户端消息ID
    "self": true,                                // Boolean - 是否当前用户自己发送
    "deliveryStatus": 2,                         // Integer - 投递状态：0待投递，1已送达，2已读
    "readByCurrentUser": true,                   // Boolean - 当前用户是否已读
    "readAt": "2025-12-25T10:05:00.000+08:00",  // Date - 当前用户读到该消息的时间
    "revoked": false,                            // Boolean - 是否已撤回
    "edited": false,                             // Boolean - 是否编辑过
    "updatedAt": null,                           // Date - 更新时间
    "createdAt": "2025-12-25T10:05:00.000+08:00" // Date - 发送时间
  }
}
```

#### ChatFilePayloadVO 完整结构（文件消息时 file 字段有值）

```json
{
  "businessId": 8001,                           // Long - 聊天文件业务引用ID
  "fileId": 5001,                               // Long - 文件ID
  "fileName": "report.pdf",                     // String - 文件名称
  "originalName": "2025年度报告.pdf",             // String - 原始文件名
  "fileUrl": "/files/2025/12/report.pdf",       // String - 文件地址
  "fileSize": 1048576,                          // Long - 文件大小（字节）
  "fileType": "document",                       // String - 文件类型
  "mimeType": "application/pdf",                // String - MIME 类型
  "previewUrl": null,                           // String - 预览地址（图片/语音可复用）
  "thumbnailUrl": null,                         // String - 缩略图地址（图片默认回落原图）
  "width": null,                                // Integer - 图片宽度
  "height": null,                               // Integer - 图片高度
  "durationSeconds": null,                      // Integer - 语音时长（秒）
  "waveform": null,                             // List<Integer> - 语音波形采样点
  "transcodeStatus": "source"                   // String - 转码状态：source/pending/ready/failed
}
```

### message_updated - 消息更新

消息内容被编辑后推送。

```json
{
  "type": "message_updated",
  "requestId": null,
  "timestamp": 1735123470000,
  "code": 200,
  "message": "success",
  "payload": {
    "id": 6001,
    "conversationId": 2001,
    "senderId": 1001,
    "senderUsername": "zhangsan",
    "senderNickname": "张三",
    "senderAvatar": "/avatars/zhangsan.jpg",
    "messageType": "text",
    "content": "你好，在吗？（已编辑）",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": "msg_abc123",
    "self": true,
    "deliveryStatus": 2,
    "readByCurrentUser": true,
    "readAt": "2025-12-25T10:05:00.000+08:00",
    "revoked": false,
    "edited": true,
    "updatedAt": "2025-12-25T10:10:00.000+08:00",
    "createdAt": "2025-12-25T10:05:00.000+08:00"
  }
}
```

### message_revoked - 消息撤回

消息被发送者撤回后推送。撤回后内容应显示为"该消息已撤回"。

```json
{
  "type": "message_revoked",
  "requestId": null,
  "timestamp": 1735123480000,
  "code": 200,
  "message": "success",
  "payload": {
    "id": 6001,
    "conversationId": 2001,
    "senderId": 1001,
    "senderUsername": "zhangsan",
    "senderNickname": "张三",
    "senderAvatar": "/avatars/zhangsan.jpg",
    "messageType": "text",
    "content": "该消息已撤回",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": "msg_abc123",
    "self": true,
    "deliveryStatus": 2,
    "readByCurrentUser": true,
    "readAt": "2025-12-25T10:05:00.000+08:00",
    "revoked": true,
    "edited": false,
    "updatedAt": "2025-12-25T10:12:00.000+08:00",
    "createdAt": "2025-12-25T10:05:00.000+08:00"
  }
}
```

### message_deleted - 消息删除

消息被删除后推送，payload 中包含当前未读数。

```json
{
  "type": "message_deleted",
  "requestId": null,
  "timestamp": 1735123490000,
  "code": 200,
  "message": "success",
  "payload": {
    "messageId": 6002,
    "conversationId": 2001,
    "unreadCount": 5
  }
}
```

### read_updated - 已读状态更新

会话中某成员的已读状态发生变化时推送。payload 为 ChatReadStateVO 完整结构。

```json
{
  "type": "read_updated",
  "requestId": null,
  "timestamp": 1735123500000,
  "code": 200,
  "message": "success",
  "payload": {
    "conversationId": 2001,                          // Long - 会话ID
    "userId": 3001,                                  // Long - 用户ID
    "readMessageId": 6010,                           // Long - 最后已读消息ID
    "readAt": "2025-12-25T11:00:00.000+08:00",      // Date - 最后已读时间
    "deliveredMessageId": 6008,                      // Long - 最后已送达消息ID
    "deliveredAt": "2025-12-25T10:58:00.000+08:00", // Date - 最后已送达时间
    "unreadCount": 2                                 // Integer - 未读数
  }
}
```

### conversation_updated - 会话更新

会话信息变更时推送，payload 中包含 `action` 字段表示变更类型。

```json
{
  "type": "conversation_updated",
  "requestId": null,
  "timestamp": 1735123510000,
  "code": 200,
  "message": "success",
  "payload": {
    "conversationId": 2001,
    "action": "notice_updated",
    "conversation": {
      "id": 2001,
      "name": "项目讨论组",
      "avatar": "/groups/project.jpg",
      "notice": "新的群公告内容"
    }
  }
}
```

### members_updated - 成员变更

群组成员变更时推送，payload 中包含 `action` 字段表示变更类型。

```json
{
  "type": "members_updated",
  "requestId": null,
  "timestamp": 1735123520000,
  "code": 200,
  "message": "success",
  "payload": {
    "conversationId": 2001,
    "action": "members_invited",
    "members": [
      {
        "userId": 4001,
        "username": "wangwu",
        "nickname": "王五",
        "role": "member"
      }
    ],
    "operator": {
      "userId": 1001,
      "username": "zhangsan"
    }
  }
}
```

### error - 错误

服务端推送的错误消息。

```json
{
  "type": "error",
  "requestId": "req_1701001001",
  "timestamp": 1735123530000,
  "code": 400,
  "message": "消息内容不能为空",
  "payload": {
    "detail": "content field is required"
  }
}
```

## 6. action 枚举值完整列表

以下 action 值用于 `conversation_updated` 和 `members_updated` 推送消息的 `payload.action` 字段：

| action 值                 | 适用推送类型               | 说明         |
|--------------------------|----------------------|------------|
| `members_invited`        | members_updated      | 新成员被邀请加入群组 |
| `admin_appointed`        | members_updated      | 成员被任命为管理员  |
| `admin_removed`          | members_updated      | 成员被取消管理员身份 |
| `owner_transferred`      | members_updated      | 群主身份转让     |
| `member_mute_updated`    | members_updated      | 成员禁言状态变更   |
| `notice_updated`         | conversation_updated | 群公告更新      |
| `member_removed`         | members_updated      | 成员被移出群组    |
| `member_left`            | members_updated      | 成员主动退出群组   |
| `conversation_dissolved` | conversation_updated | 群组被解散      |

## 7. 心跳机制

- **发送间隔**：客户端每 **30 秒** 发送一次 `ping`
- **响应超时**：发送 `ping` 后 **10 秒** 内未收到 `pong` 视为连接异常
- **超时处理**：关闭当前连接，触发重连策略

```javascript
// 心跳发送
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
    // 设置超时检测
    heartbeatTimeout = setTimeout(() => {
      console.warn('心跳超时，关闭连接');
      ws.close();
    }, 10000);
  }
}, 30000);

// 收到 pong 时清除超时
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type === 'pong') {
    clearTimeout(heartbeatTimeout);
  }
};
```

## 8. 前端 WebSocket 封装类设计

```javascript
/**
 * ChatWebSocket - 聊天 WebSocket 封装
 *
 * 提供：连接管理、自动重连、心跳保活、消息收发、事件监听。
 */
class ChatWebSocket {
  /**
   * @param {Object} options
   * @param {string} options.baseURL - WebSocket 基础地址，如 'ws://localhost:8000/ws/chat'
   * @param {number} options.heartbeatInterval - 心跳间隔（毫秒），默认 30000
   * @param {number} options.heartbeatTimeout - 心跳超时（毫秒），默认 10000
   * @param {number} options.reconnectBaseDelay - 重连基础延迟（毫秒），默认 1000
   * @param {number} options.reconnectMaxDelay - 重连最大延迟（毫秒），默认 30000
   */
  constructor(options = {}) {
    this.baseURL = options.baseURL || 'ws://localhost:8000/ws/chat';
    this.heartbeatInterval = options.heartbeatInterval || 30000;
    this.heartbeatTimeout = options.heartbeatTimeout || 10000;
    this.reconnectBaseDelay = options.reconnectBaseDelay || 1000;
    this.reconnectMaxDelay = options.reconnectMaxDelay || 30000;

    this.ws = null;
    this.reconnectAttempts = 0;
    this.reconnectTimer = null;
    this.heartbeatTimer = null;
    this.heartbeatTimeoutTimer = null;
    this.requestIdCounter = 0;
    this.pendingAcks = new Map(); // requestId -> { resolve, reject, timer }

    // 事件监听器
    this.listeners = {
      open: [],
      close: [],
      error: [],
      ready: [],
      message_created: [],
      message_updated: [],
      message_revoked: [],
      message_deleted: [],
      read_updated: [],
      conversation_updated: [],
      members_updated: [],
      ack: [],
      pong: [],
      error_message: [],
    };
  }

  /**
   * 连接 WebSocket
   */
  connect() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      throw new Error('未找到 accessToken，请先登录');
    }

    const url = `${this.baseURL}?accessToken=${token}`;
    this.ws = new WebSocket(url);

    this.ws.onopen = (event) => {
      console.log('[ChatWS] 连接已建立');
      this.reconnectAttempts = 0;
      this._startHeartbeat();
      this._emit('open', event);
    };

    this.ws.onclose = (event) => {
      console.log(`[ChatWS] 连接关闭: code=${event.code}, reason=${event.reason}`);
      this._stopHeartbeat();
      this._emit('close', event);

      // 非主动关闭时自动重连
      if (event.code !== 4001) {
        this._scheduleReconnect();
      }
    };

    this.ws.onerror = (event) => {
      console.error('[ChatWS] 连接错误', event);
      this._emit('error', event);
    };

    this.ws.onmessage = (event) => {
      this._handleMessage(event.data);
    };
  }

  /**
   * 断开连接
   */
  disconnect() {
    this._stopHeartbeat();
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;

    if (this.ws) {
      this.ws.onclose = null; // 阻止触发重连
      this.ws.close(1000, '客户端主动断开');
      this.ws = null;
    }
  }

  /**
   * 注册事件监听
   * @param {string} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
    return this; // 支持链式调用
  }

  /**
   * 移除事件监听
   */
  off(event, callback) {
    if (!this.listeners[event]) return this;
    this.listeners[event] = this.listeners[event].filter((cb) => cb !== callback);
    return this;
  }

  /**
   * 触发事件
   */
  _emit(event, data) {
    const callbacks = this.listeners[event] || [];
    callbacks.forEach((cb) => {
      try {
        cb(data);
      } catch (err) {
        console.error(`[ChatWS] 事件处理错误 (${event}):`, err);
      }
    });
  }

  /**
   * 发送聊天消息
   * @param {Object} params
   * @param {number} [params.conversationId] - 会话ID
   * @param {number} [params.targetUserId] - 目标用户ID
   * @param {string} params.content - 消息内容
   * @param {string} [params.clientMessageId] - 客户端消息ID
   * @param {number} [params.replyMessageId] - 回复消息ID
   * @returns {Promise<void>} ack 确认
   */
  sendMessage(params) {
    const requestId = this._generateRequestId();
    const message = {
      type: 'send_message',
      requestId,
      payload: {
        conversationId: params.conversationId,
        targetUserId: params.targetUserId,
        content: params.content,
        clientMessageId: params.clientMessageId || `client_${Date.now()}_${requestId}`,
        replyMessageId: params.replyMessageId,
      },
    };

    return this._sendWithAck(requestId, message);
  }

  /**
   * 标记已读
   */
  markRead(conversationId) {
    const requestId = this._generateRequestId();
    const message = {
      type: 'mark_read',
      requestId,
      payload: { conversationId },
    };

    return this._sendWithAck(requestId, message);
  }

  /**
   * 发送心跳 ping
   */
  sendPing() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send('ping');
    }
  }

  /**
   * 带超时的重连
   */
  _scheduleReconnect() {
    if (this.reconnectTimer) return;

    const delay = Math.min(
      this.reconnectBaseDelay * Math.pow(2, this.reconnectAttempts),
      this.reconnectMaxDelay
    );

    console.log(`[ChatWS] ${delay}ms 后重连（第 ${this.reconnectAttempts + 1} 次）`);

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.reconnectAttempts++;
      this.connect();
    }, delay);
  }

  /**
   * 处理收到的消息
   */
  _handleMessage(rawData) {
    // 处理裸文本 pong
    if (rawData === 'pong') {
      this._handlePong(null);
      return;
    }

    let data;
    try {
      data = JSON.parse(rawData);
    } catch (e) {
      console.warn('[ChatWS] 无法解析消息:', rawData);
      return;
    }

    const { type, requestId, payload, code, message } = data;

    // 处理 ack 响应（匹配 pending 请求）
    if (type === 'ack' && requestId && this.pendingAcks.has(requestId)) {
      const pending = this.pendingAcks.get(requestId);
      clearTimeout(pending.timer);
      this.pendingAcks.delete(requestId);

      if (code === 200) {
        pending.resolve(payload);
      } else {
        pending.reject(new Error(message || `请求失败: code=${code}`));
      }
    }

    // 分发到对应事件
    switch (type) {
      case 'ready':
        this._emit('ready', payload);
        break;
      case 'pong':
        this._handlePong(data);
        break;
      case 'ack':
        this._emit('ack', data);
        break;
      case 'message_created':
        this._emit('message_created', payload);
        break;
      case 'message_updated':
        this._emit('message_updated', payload);
        break;
      case 'message_revoked':
        this._emit('message_revoked', payload);
        break;
      case 'message_deleted':
        this._emit('message_deleted', payload);
        break;
      case 'read_updated':
        this._emit('read_updated', payload);
        break;
      case 'conversation_updated':
        this._emit('conversation_updated', payload);
        break;
      case 'members_updated':
        this._emit('members_updated', payload);
        break;
      case 'error':
        console.error(`[ChatWS] 服务端错误: ${message}`);
        this._emit('error_message', data);
        break;
      default:
        console.warn(`[ChatWS] 未知消息类型: ${type}`, data);
    }
  }

  /**
   * 处理 pong 响应
   */
  _handlePong(data) {
    clearTimeout(this.heartbeatTimeoutTimer);
    this._emit('pong', data);
  }

  /**
   * 启动心跳
   */
  _startHeartbeat() {
    this._stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.sendPing();
        // 设置超时检测
        this.heartbeatTimeoutTimer = setTimeout(() => {
          console.warn('[ChatWS] 心跳超时，关闭连接');
          if (this.ws) {
            this.ws.close();
          }
        }, this.heartbeatTimeout);
      }
    }, this.heartbeatInterval);
  }

  /**
   * 停止心跳
   */
  _stopHeartbeat() {
    clearInterval(this.heartbeatTimer);
    this.heartbeatTimer = null;
    clearTimeout(this.heartbeatTimeoutTimer);
    this.heartbeatTimeoutTimer = null;
  }

  /**
   * 发送消息并等待 ack
   */
  _sendWithAck(requestId, message) {
    return new Promise((resolve, reject) => {
      // 设置 ack 超时
      const timer = setTimeout(() => {
        this.pendingAcks.delete(requestId);
        reject(new Error('请求超时，未收到 ack'));
      }, 10000);

      this.pendingAcks.set(requestId, { resolve, reject, timer });

      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify(message));
      } else {
        clearTimeout(timer);
        this.pendingAcks.delete(requestId);
        reject(new Error('WebSocket 未连接'));
      }
    });
  }

  /**
   * 生成请求ID
   */
  _generateRequestId() {
    return `req_${Date.now()}_${++this.requestIdCounter}`;
  }
}

// ========== 使用示例 ==========

const chatWs = new ChatWebSocket({
  baseURL: 'ws://localhost:8000/ws/chat',
  heartbeatInterval: 30000,
  reconnectBaseDelay: 1000,
  reconnectMaxDelay: 30000,
});

// 注册事件
chatWs
  .on('ready', (payload) => {
    console.log('聊天连接就绪:', payload);
  })
  .on('message_created', (msg) => {
    console.log('收到新消息:', msg);
    // 添加到消息列表
    appendMessage(msg);
  })
  .on('message_revoked', (msg) => {
    // 更新消息状态为已撤回
    updateMessageRevoked(msg.id);
  })
  .on('read_updated', (state) => {
    // 更新已读状态
    updateReadState(state);
  })
  .on('close', (event) => {
    if (event.code !== 1000) {
      console.log('连接断开，将自动重连...');
    }
  });

// 连接
chatWs.connect();

// 发送消息
async function handleSend() {
  try {
    await chatWs.sendMessage({
      conversationId: 2001,
      content: '你好！',
      clientMessageId: `msg_${Date.now()}`,
    });
  } catch (err) {
    console.error('发送失败:', err);
  }
}

// 标记已读
function handleMarkRead(conversationId) {
  chatWs.markRead(conversationId).catch(console.error);
}
```

## 9. 重连策略

采用**指数退避**策略，避免短时间内大量重连请求冲击服务端：

```
重连延迟 = min(baseDelay * 2^attempts, maxDelay)
```

| 重连次数   | 延迟      |
|--------|---------|
| 第 1 次  | 1s      |
| 第 2 次  | 2s      |
| 第 3 次  | 4s      |
| 第 4 次  | 8s      |
| 第 5 次  | 16s     |
| 第 6 次+ | 30s（上限） |

### 重连后拉取消息历史

重连成功后，客户端需要：

1. 等待 `ready` 消息确认连接就绪
2. 通过 HTTP 接口拉取断线期间的消息历史

```javascript
chatWs.on('ready', async (payload) => {
  // 重连后拉取未读消息
  const lastMessageId = getLastLocalMessageId();
  if (lastMessageId) {
    const messages = await fetchMessagesAfter(lastMessageId);
    appendMessages(messages);
    updateUnreadCount();
  }
});

async function fetchMessagesAfter(lastMessageId) {
  const response = await fetch(
    `/api/user/chat/messages?afterId=${lastMessageId}&limit=50`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      },
    }
  );
  const result = await response.json();
  return result.data.records || [];
}
```

## 10. 消息排序与去重

### messageId 排序

服务端保证消息 ID 单调递增，前端按 `id` 字段升序排列：

```javascript
function sortMessages(messages) {
  return [...messages].sort((a, b) => a.id - b.id);
}
```

### clientMessageId 去重

客户端发送消息时携带 `clientMessageId`，用于防止重复显示：

```javascript
const displayedMessageIds = new Set();
const displayedClientIds = new Set();

function addMessage(msg) {
  // 服务端消息ID去重
  if (msg.id && displayedMessageIds.has(msg.id)) return;
  if (msg.id) displayedMessageIds.add(msg.id);

  // 客户端消息ID去重（处理乐观更新的确认）
  if (msg.clientMessageId && displayedClientIds.has(msg.clientMessageId)) {
    // 已存在同 clientMessageId 的消息，用服务端数据替换
    replaceOptimisticMessage(msg.clientMessageId, msg);
    return;
  }
  if (msg.clientMessageId) displayedClientIds.add(msg.clientMessageId);

  messageList.push(msg);
  renderMessages();
}
```

### 乐观更新 vs 确认回调

```javascript
async function sendMessageWithOptimisticUpdate(params) {
  const clientMessageId = `msg_${Date.now()}_${Math.random().toString(36).slice(2)}`;

  // 1. 乐观更新：立即在 UI 显示消息
  const optimisticMessage = {
    id: null,                                    // 暂无服务端ID
    clientMessageId,
    senderId: currentUserId,
    senderNickname: currentUserNickname,
    senderAvatar: currentUserAvatar,
    content: params.content,
    messageType: 'text',
    self: true,
    deliveryStatus: 0,                           // 待投递
    createdAt: new Date().toISOString(),
    _optimistic: true,                           // 标记为乐观消息
  };

  addMessage(optimisticMessage);
  renderMessages();

  // 2. 发送 WebSocket 消息
  try {
    await chatWs.sendMessage({
      ...params,
      clientMessageId,
    });
    // 3. 更新投递状态
    updateOptimisticMessage(clientMessageId, { deliveryStatus: 1 });
  } catch (err) {
    // 4. 发送失败，标记消息状态
    updateOptimisticMessage(clientMessageId, { deliveryStatus: -1, _failed: true });
    console.error('消息发送失败:', err);
  }
}

// 收到 message_created 时替换乐观消息
chatWs.on('message_created', (msg) => {
  if (msg.clientMessageId) {
    replaceOptimisticMessage(msg.clientMessageId, {
      ...msg,
      _optimistic: false,
    });
  } else {
    addMessage(msg);
  }
  renderMessages();
});

function replaceOptimisticMessage(clientMessageId, serverMsg) {
  const index = messageList.findIndex(
    (m) => m.clientMessageId === clientMessageId && m._optimistic
  );
  if (index !== -1) {
    messageList[index] = serverMsg;
  } else {
    addMessage(serverMsg);
  }
}
```

## 11. 多节点支持说明

后端采用 **Redis Pub/Sub** 实现多节点消息分发：

- 用户 A 连接到节点 1，用户 B 连接到节点 2
- 用户 A 发送消息 → 节点 1 处理 → 通过 Redis Pub/Sub 广播到所有节点 → 节点 2 推送给用户 B
- 前端**无需关心**多节点部署细节，所有消息通过统一的 WebSocket 连接接收

前端注意事项：

- 每个用户同时只需维持**一个** WebSocket 连接
- 如果建立新连接，旧连接会被服务端自动断开
- 建议在页面 `visibilitychange` 事件中检查连接状态，页面恢复可见时确保连接正常

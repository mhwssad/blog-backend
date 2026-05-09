# 聊天模块前端参考手册

本文档面向前端开发者，基于实际代码编写，提供完整的接口调用示例和响应说明。

---

## 会话列表页

### 获取我的会话列表

**接口信息**
- 路径: `GET /api/user/chat/conversations`
- 鉴权: 是
- 说明: 分页查询当前用户的会话列表，支持按关键字搜索会话名称或最后一条消息内容

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |
| keyword | String | 否 | - | 关键字，模糊查询会话名或最后一条消息内容 |

**请求示例**

```javascript
// axios
axios.get('/api/user/chat/conversations', {
  params: { current: 1, size: 20 }
})

// 请求 URL
GET /api/user/chat/conversations?current=1&size=20
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 3,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 1001,
        "conversationType": "single",
        "sceneType": "single_chat",
        "name": "李四",
        "avatar": "https://example.com/avatar/lisi.jpg",
        "ownerId": null,
        "notice": null,
        "allSite": null,
        "status": 0,
        "visibilityScope": null,
        "allowGuestView": null,
        "requireJoinToSpeak": null,
        "joinRule": null,
        "speakLevelLimit": null,
        "memberLimit": null,
        "slowModeSeconds": null,
        "displaySort": null,
        "channelCategoryCode": null,
        "selfRole": null,
        "memberCount": null,
        "unreadCount": 2,
        "targetUserId": 102,
        "targetUsername": "lisi",
        "targetNickname": "李四",
        "lastReadMessageId": 9001,
        "lastReadAt": "2025-01-15T10:30:00",
        "lastDeliveredMessageId": 9002,
        "lastDeliveredAt": "2025-01-15T10:31:00",
        "lastMessage": {
          "id": 9010,
          "senderId": 102,
          "senderNickname": "李四",
          "messageType": "text",
          "content": "好的，明天见！",
          "createdAt": "2025-01-15T10:32:00"
        },
        "createdAt": "2025-01-10T08:00:00",
        "updatedAt": "2025-01-15T10:32:00"
      },
      {
        "id": 1002,
        "conversationType": "group",
        "sceneType": "group_chat",
        "name": "技术交流群",
        "avatar": "https://example.com/avatar/tech-group.jpg",
        "ownerId": 101,
        "notice": "本群禁止广告",
        "allSite": false,
        "status": 0,
        "visibilityScope": "private",
        "allowGuestView": 0,
        "requireJoinToSpeak": 0,
        "joinRule": "free",
        "speakLevelLimit": 1,
        "memberLimit": 200,
        "slowModeSeconds": 0,
        "displaySort": 1,
        "channelCategoryCode": "tech",
        "selfRole": "member",
        "memberCount": 45,
        "unreadCount": 5,
        "targetUserId": null,
        "targetUsername": null,
        "targetNickname": null,
        "lastReadMessageId": 8001,
        "lastReadAt": "2025-01-15T09:00:00",
        "lastDeliveredMessageId": 8005,
        "lastDeliveredAt": "2025-01-15T09:05:00",
        "lastMessage": {
          "id": 8010,
          "senderId": 103,
          "senderNickname": "王五",
          "messageType": "text",
          "content": "有人知道怎么解决吗？",
          "createdAt": "2025-01-15T11:00:00"
        },
        "createdAt": "2025-01-05T10:00:00",
        "updatedAt": "2025-01-15T11:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 会话ID |
| conversationType | String | 会话类型：`single` 单聊、`group` 群聊、`global` 全局频道 |
| sceneType | String | 业务场景：`single_chat`、`group_chat`、`public_channel` 等 |
| name | String | 会话名称（群聊时为群名称，单聊时为对方昵称） |
| avatar | String | 会话头像URL |
| ownerId | Long | 群主用户ID（仅群聊） |
| notice | String | 群公告（仅群聊） |
| allSite | Boolean | 是否全站群聊 |
| status | Integer | 会话状态：`0` 正常 |
| visibilityScope | String | 可见范围：`public` 公开、`private` 私有 |
| allowGuestView | Integer | 访客是否可见：`0` 否、`1` 是 |
| requireJoinToSpeak | Integer | 是否需要加入后发言：`0` 否、`1` 是 |
| joinRule | String | 加入规则：`free` 自由加入、`approval` 需要审批、`invite_only` 邀请制 |
| speakLevelLimit | Integer | 发言最低等级限制 |
| memberLimit | Integer | 成员上限，`0` 表示不限制 |
| slowModeSeconds | Integer | 慢速模式秒数，`0` 表示关闭 |
| displaySort | Integer | 展示排序权重 |
| channelCategoryCode | String | 频道或群分类编码 |
| selfRole | String | 当前用户在会话中的角色：`owner` 群主、`admin` 管理员、`member` 成员 |
| memberCount | Long | 活跃成员数量 |
| unreadCount | Integer | 未读消息数 |
| targetUserId | Long | 单聊目标用户ID |
| targetUsername | String | 单聊目标用户名 |
| targetNickname | String | 单聊目标昵称 |
| lastReadMessageId | Long | 当前用户最后已读的消息ID |
| lastReadAt | LocalDateTime | 最后已读时间 |
| lastDeliveredMessageId | Long | 最后已送达的消息ID |
| lastDeliveredAt | LocalDateTime | 最后送达时间 |
| lastMessage | Object | 最后一条消息摘要 |
| lastMessage.id | Long | 消息ID |
| lastMessage.senderId | Long | 发送人ID |
| lastMessage.senderNickname | String | 发送人昵称 |
| lastMessage.messageType | String | 消息类型：`text` 文本、`file` 文件等 |
| lastMessage.content | String | 消息内容摘要 |
| lastMessage.createdAt | LocalDateTime | 发送时间 |
| createdAt | LocalDateTime | 会话创建时间 |
| updatedAt | LocalDateTime | 会话最后更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 401 | 未登录或Token无效 | 跳转登录页 |
| 500 | 服务器内部错误 | 显示错误提示 |

---

### 打开或创建单聊会话

**接口信息**
- 路径: `POST /api/user/chat/single-conversations`
- 鉴权: 是
- 说明: 根据目标用户ID打开或创建一个单聊会话

**请求示例**

```javascript
// axios
axios.post('/api/user/chat/single-conversations', {
  targetUserId: 102
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| targetUserId | Long | 是 | 目标用户ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1001,
    "conversationType": "single",
    "sceneType": "single_chat",
    "name": "李四",
    "avatar": "https://example.com/avatar/lisi.jpg",
    "targetUserId": 102,
    "targetUsername": "lisi",
    "targetNickname": "李四",
    "unreadCount": 0,
    "status": 0,
    "createdAt": "2025-01-10T08:00:00",
    "updatedAt": "2025-01-15T12:00:00"
  }
}
```

---

### 查询会话详情

**接口信息**
- 路径: `GET /api/user/chat/conversations/{conversationId}`
- 鉴权: 是
- 说明: 根据会话ID查询会话详细信息

**请求示例**

```javascript
axios.get('/api/user/chat/conversations/1001')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1001,
    "conversationType": "single",
    "sceneType": "single_chat",
    "name": "李四",
    "avatar": "https://example.com/avatar/lisi.jpg",
    "status": 0,
    "targetUserId": 102,
    "targetUsername": "lisi",
    "targetNickname": "李四",
    "lastReadMessageId": 9001,
    "lastReadAt": "2025-01-15T10:30:00",
    "lastDeliveredMessageId": 9002,
    "lastDeliveredAt": "2025-01-15T10:31:00",
    "createdAt": "2025-01-10T08:00:00",
    "updatedAt": "2025-01-15T10:32:00"
  }
}
```

---

## 聊天页

### 分页查询会话消息

**接口信息**
- 路径: `GET /api/user/chat/conversations/{conversationId}/messages`
- 鉴权: 是
- 说明: 分页获取指定会话的历史消息，默认按时间倒序

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |
| beforeMessageId | Long | 否 | - | 查询该消息ID之前的消息，用于滚动加载更多 |

**请求示例**

```javascript
// 首次加载
axios.get('/api/user/chat/conversations/1001/messages', {
  params: { current: 1, size: 20 }
})

// 加载更多历史消息
axios.get('/api/user/chat/conversations/1001/messages', {
  params: { current: 1, size: 20, beforeMessageId: 9000 }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 100,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 9020,
        "conversationId": 1001,
        "senderId": 101,
        "senderUsername": "zhangsan",
        "senderNickname": "张三",
        "senderAvatar": "https://example.com/avatar/zhangsan.jpg",
        "messageType": "text",
        "content": "你好，明天有空吗？",
        "file": null,
        "replyMessageId": null,
        "reply": null,
        "clientMessageId": "msg-uuid-123456",
        "self": true,
        "deliveryStatus": 2,
        "readByCurrentUser": true,
        "readAt": "2025-01-15T10:35:00",
        "revoked": false,
        "edited": false,
        "updatedAt": "2025-01-15T10:30:00",
        "createdAt": "2025-01-15T10:30:00"
      },
      {
        "id": 9019,
        "conversationId": 1001,
        "senderId": 102,
        "senderUsername": "lisi",
        "senderNickname": "李四",
        "senderAvatar": "https://example.com/avatar/lisi.jpg",
        "messageType": "text",
        "content": "有空的，什么事？",
        "file": null,
        "replyMessageId": null,
        "reply": null,
        "clientMessageId": null,
        "self": false,
        "deliveryStatus": 2,
        "readByCurrentUser": true,
        "readAt": "2025-01-15T10:35:00",
        "revoked": false,
        "edited": false,
        "updatedAt": "2025-01-15T10:32:00",
        "createdAt": "2025-01-15T10:32:00"
      },
      {
        "id": 9018,
        "conversationId": 1001,
        "senderId": 102,
        "senderUsername": "lisi",
        "senderNickname": "李四",
        "senderAvatar": "https://example.com/avatar/lisi.jpg",
        "messageType": "file",
        "content": null,
        "file": {
          "businessId": 5001,
          "fileId": 3001,
          "fileName": "document.pdf",
          "originalName": "项目文档.pdf",
          "fileUrl": "https://example.com/files/document.pdf",
          "fileSize": 1024000,
          "fileType": "pdf",
          "mimeType": "application/pdf",
          "previewUrl": null,
          "thumbnailUrl": null,
          "width": null,
          "height": null,
          "durationSeconds": null,
          "waveform": null,
          "transcodeStatus": "ready"
        },
        "replyMessageId": null,
        "reply": null,
        "clientMessageId": null,
        "self": false,
        "deliveryStatus": 1,
        "readByCurrentUser": false,
        "readAt": null,
        "revoked": false,
        "edited": false,
        "updatedAt": "2025-01-15T10:33:00",
        "createdAt": "2025-01-15T10:33:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 消息ID |
| conversationId | Long | 所属会话ID |
| senderId | Long | 发送人用户ID |
| senderUsername | String | 发送人用户名 |
| senderNickname | String | 发送人昵称 |
| senderAvatar | String | 发送人头像URL |
| messageType | String | 消息类型：`text` 文本、`file` 文件等 |
| content | String | 文本消息内容 |
| file | Object | 文件消息载荷，详见下方 |
| replyMessageId | Long | 回复的消息ID |
| reply | Object | 回复消息快照，详见下方 |
| clientMessageId | String | 客户端消息ID（用于幂等控制） |
| self | Boolean | 是否当前用户自己发送 |
| deliveryStatus | Integer | 投递状态：`0` 待投递、`1` 已送达、`2` 已读 |
| readByCurrentUser | Boolean | 当前用户是否已读 |
| readAt | LocalDateTime | 当前用户读到该消息的时间 |
| revoked | Boolean | 是否已撤回 |
| edited | Boolean | 是否编辑过 |
| updatedAt | LocalDateTime | 消息更新时间 |
| createdAt | LocalDateTime | 消息发送时间 |

**file 对象字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| businessId | Long | 聊天文件业务引用ID |
| fileId | Long | 文件ID |
| fileName | String | 文件名称（聊天展示名） |
| originalName | String | 原始文件名 |
| fileUrl | String | 文件访问地址 |
| fileSize | Long | 文件大小（字节） |
| fileType | String | 文件类型（如 pdf、jpg、mp4） |
| mimeType | String | MIME 类型 |
| previewUrl | String | 预览地址（图片/语音可直接复用） |
| thumbnailUrl | String | 缩略图地址 |
| width | Integer | 图片宽度（仅图片） |
| height | Integer | 图片高度（仅图片） |
| durationSeconds | Integer | 语音时长秒数（仅语音） |
| waveform | List<Integer> | 语音波形采样点 |
| transcodeStatus | String | 转码状态：`source` 原始、`pending` 转码中、`ready` 可用、`failed` 失败 |

**reply 对象字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 被回复消息ID |
| senderId | Long | 被回复消息发送人ID |
| senderUsername | String | 被回复消息发送人用户名 |
| senderNickname | String | 被回复消息发送人昵称 |
| senderAvatar | String | 被回复消息发送人头像 |
| messageType | String | 被回复消息类型 |
| replyToMessageId | Long | 被回复消息自身所引用的上一层消息ID |
| content | String | 被回复消息摘要内容 |
| file | Object | 被回复消息附件快照 |
| revoked | Boolean | 被回复消息是否已撤回 |
| deleted | Boolean | 被回复消息是否已不可见 |
| state | String | 被回复消息状态：`normal` 正常、`revoked` 已撤回、`unavailable` 不可用 |
| createdAt | LocalDateTime | 被回复消息发送时间 |

---

### 发送文本消息

**接口信息**
- 路径: `POST /api/user/chat/messages/text`
- 鉴权: 是
- 说明: 发送文本消息，支持单聊和群聊

**请求示例**

```javascript
// 已有会话ID时
axios.post('/api/user/chat/messages/text', {
  conversationId: 1001,
  content: '你好，明天有空吗？',
  clientMessageId: 'msg-uuid-123456',
  replyMessageId: null
})

// 无会话ID（单聊新会话）
axios.post('/api/user/chat/messages/text', {
  targetUserId: 102,
  content: '你好，明天有空吗？'
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| conversationId | Long | 否 | 会话ID，已存在会话时优先传该字段 |
| targetUserId | Long | 否 | 单聊目标用户ID，未传会话ID时用于自动创建/获取单聊 |
| content | String | 是 | 文本消息内容，最大2000字符 |
| clientMessageId | String | 否 | 客户端幂等消息ID，建议使用UUID |
| replyMessageId | Long | 否 | 回复的消息ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 9021,
    "conversationId": 1001,
    "senderId": 101,
    "senderUsername": "zhangsan",
    "senderNickname": "张三",
    "senderAvatar": "https://example.com/avatar/zhangsan.jpg",
    "messageType": "text",
    "content": "你好，明天有空吗？",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": "msg-uuid-123456",
    "self": true,
    "deliveryStatus": 0,
    "readByCurrentUser": false,
    "readAt": null,
    "revoked": false,
    "edited": false,
    "updatedAt": "2025-01-15T12:00:00",
    "createdAt": "2025-01-15T12:00:00"
  }
}
```

---

### 发送文件消息

**接口信息**
- 路径: `POST /api/user/chat/messages/file`
- 鉴权: 是
- 说明: 发送文件消息（图片、语音、视频、文档等），文件需先通过文件上传接口获取 businessId

**请求示例**

```javascript
// 已有会话ID时
axios.post('/api/user/chat/messages/file', {
  conversationId: 1001,
  businessId: 5001,
  clientMessageId: 'msg-uuid-789012',
  replyMessageId: null
})

// 无会话ID（单聊新会话）
axios.post('/api/user/chat/messages/file', {
  targetUserId: 102,
  businessId: 5001
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| conversationId | Long | 否 | 会话ID，已存在会话时优先传该字段 |
| targetUserId | Long | 否 | 单聊目标用户ID，未传会话ID时用于自动创建/获取单聊 |
| businessId | Long | 是 | 上传完成后得到的文件业务引用ID |
| clientMessageId | String | 否 | 客户端幂等消息ID |
| replyMessageId | Long | 否 | 回复的消息ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 9022,
    "conversationId": 1001,
    "senderId": 101,
    "senderUsername": "zhangsan",
    "senderNickname": "张三",
    "senderAvatar": "https://example.com/avatar/zhangsan.jpg",
    "messageType": "file",
    "content": null,
    "file": {
      "businessId": 5001,
      "fileId": 3001,
      "fileName": "photo.jpg",
      "originalName": "照片.jpg",
      "fileUrl": "https://example.com/files/photo.jpg",
      "fileSize": 2048000,
      "fileType": "jpg",
      "mimeType": "image/jpeg",
      "previewUrl": "https://example.com/files/photo.jpg",
      "thumbnailUrl": "https://example.com/files/photo_thumb.jpg",
      "width": 1920,
      "height": 1080,
      "durationSeconds": null,
      "waveform": null,
      "transcodeStatus": "ready"
    },
    "clientMessageId": "msg-uuid-789012",
    "self": true,
    "deliveryStatus": 0,
    "readByCurrentUser": false,
    "readAt": null,
    "revoked": false,
    "edited": false,
    "updatedAt": "2025-01-15T12:01:00",
    "createdAt": "2025-01-15T12:01:00"
  }
}
```

---

### 编辑消息

**接口信息**
- 路径: `PUT /api/user/chat/messages/{messageId}`
- 鉴权: 是
- 说明: 编辑已发送的文本消息，仅本人发送的消息可编辑

**请求示例**

```javascript
axios.put('/api/user/chat/messages/9021', {
  content: '你好，明天有空吗？想约你吃饭。'
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| content | String | 是 | 新的文本消息内容，最大2000字符 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 9021,
    "conversationId": 1001,
    "senderId": 101,
    "senderUsername": "zhangsan",
    "senderNickname": "张三",
    "senderAvatar": "https://example.com/avatar/zhangsan.jpg",
    "messageType": "text",
    "content": "你好，明天有空吗？想约你吃饭。",
    "file": null,
    "replyMessageId": null,
    "reply": null,
    "clientMessageId": "msg-uuid-123456",
    "self": true,
    "deliveryStatus": 1,
    "readByCurrentUser": false,
    "readAt": null,
    "revoked": false,
    "edited": true,
    "updatedAt": "2025-01-15T12:05:00",
    "createdAt": "2025-01-15T12:00:00"
  }
}
```

---

### 撤回消息

**接口信息**
- 路径: `POST /api/user/chat/messages/{messageId}/revoke`
- 鉴权: 是
- 说明: 撤回已发送的消息，仅本人发送的消息可撤回

**请求示例**

```javascript
axios.post('/api/user/chat/messages/9021/revoke')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

### 删除消息（当前用户视角）

**接口信息**
- 路径: `DELETE /api/user/chat/messages/{messageId}`
- 鉴权: 是
- 说明: 删除消息，仅从当前用户视角删除，不影响其他用户看到

**请求示例**

```javascript
axios.delete('/api/user/chat/messages/9021')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

### 推进会话已读游标

**接口信息**
- 路径: `POST /api/user/chat/conversations/{conversationId}/read`
- 鉴权: 是
- 说明: 告诉服务器当前用户已阅读到某条消息，推进已读进度

**请求示例**

```javascript
axios.post('/api/user/chat/conversations/1001/read', {
  readMessageId: 9020
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| readMessageId | Long | 是 | 最后已读消息ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "conversationId": 1001,
    "userId": 101,
    "readMessageId": 9020,
    "readAt": "2025-01-15T12:10:00",
    "deliveredMessageId": 9022,
    "deliveredAt": "2025-01-15T12:05:00",
    "unreadCount": 0
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| conversationId | Long | 会话ID |
| userId | Long | 用户ID |
| readMessageId | Long | 最后已读消息ID |
| readAt | LocalDateTime | 最后已读时间 |
| deliveredMessageId | Long | 最后已送达的消息ID |
| deliveredAt | LocalDateTime | 最后送达时间 |
| unreadCount | Integer | 更新后的未读数 |

---

## 群聊页

### 创建群聊

**接口信息**
- 路径: `POST /api/user/chat/groups`
- 鉴权: 是
- 说明: 创建一个新的群聊

**请求示例**

```javascript
axios.post('/api/user/chat/groups', {
  name: '技术交流群',
  avatar: 'https://example.com/avatar/tech-group.jpg',
  description: '欢迎技术爱好者加入',
  announcement: '本群禁止广告和无关内容',
  categoryCode: 'tech',
  visibilityScope: 'private',
  joinRule: 'free',
  speakLevelLimit: 1,
  memberLimit: 200,
  memberUserIds: [102, 103, 104]
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| name | String | 是 | 群名称，最大128字符 |
| avatar | String | 否 | 群头像URL，最大512字符 |
| description | String | 否 | 群简介，最大256字符 |
| announcement | String | 否 | 群公告，最大512字符 |
| categoryCode | String | 否 | 群分类编码，最大32字符 |
| visibilityScope | String | 否 | 可见范围：`public` 公开、`private` 私有，默认 `private` |
| joinRule | String | 否 | 加入规则：`free` 自由加入、`approval` 需要审批、`invite_only` 邀请制，默认 `free` |
| speakLevelLimit | Integer | 否 | 发言最低等级，默认1 |
| memberLimit | Integer | 否 | 成员上限，0表示不限制 |
| memberUserIds | List<Long> | 是 | 初始成员用户ID列表，不需要包含自己 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1003,
    "conversationType": "group",
    "sceneType": "group_chat",
    "name": "技术交流群",
    "avatar": "https://example.com/avatar/tech-group.jpg",
    "ownerId": 101,
    "notice": "本群禁止广告和无关内容",
    "allSite": false,
    "status": 0,
    "visibilityScope": "private",
    "allowGuestView": 0,
    "requireJoinToSpeak": 0,
    "joinRule": "free",
    "speakLevelLimit": 1,
    "memberLimit": 200,
    "slowModeSeconds": 0,
    "displaySort": 1,
    "channelCategoryCode": "tech",
    "selfRole": "owner",
    "memberCount": 4,
    "unreadCount": 0,
    "createdAt": "2025-01-15T12:00:00",
    "updatedAt": "2025-01-15T12:00:00"
  }
}
```

---

### 查询群聊详情

**接口信息**
- 路径: `GET /api/user/chat/groups/{conversationId}`
- 鉴权: 是
- 说明: 获取群聊的详细信息

**请求示例**

```javascript
axios.get('/api/user/chat/groups/1003')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1003,
    "conversationType": "group",
    "sceneType": "group_chat",
    "name": "技术交流群",
    "avatar": "https://example.com/avatar/tech-group.jpg",
    "ownerId": 101,
    "notice": "本群禁止广告和无关内容",
    "allSite": false,
    "status": 0,
    "visibilityScope": "private",
    "allowGuestView": 0,
    "requireJoinToSpeak": 0,
    "joinRule": "free",
    "speakLevelLimit": 1,
    "memberLimit": 200,
    "slowModeSeconds": 0,
    "displaySort": 1,
    "channelCategoryCode": "tech",
    "selfRole": "owner",
    "memberCount": 4,
    "createdAt": "2025-01-15T12:00:00",
    "updatedAt": "2025-01-15T12:00:00"
  }
}
```

---

### 查询群成员列表

**接口信息**
- 路径: `GET /api/user/chat/groups/{conversationId}/members`
- 鉴权: 是
- 说明: 获取群聊的所有成员列表

**请求示例**

```javascript
axios.get('/api/user/chat/groups/1003/members')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "userId": 101,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "https://example.com/avatar/zhangsan.jpg",
      "role": "owner",
      "status": 0,
      "joinedAt": "2025-01-15T12:00:00",
      "muteUntil": null
    },
    {
      "userId": 102,
      "username": "lisi",
      "nickname": "李四",
      "avatar": "https://example.com/avatar/lisi.jpg",
      "role": "admin",
      "status": 0,
      "joinedAt": "2025-01-15T12:01:00",
      "muteUntil": null
    },
    {
      "userId": 103,
      "username": "wangwu",
      "nickname": "王五",
      "avatar": "https://example.com/avatar/wangwu.jpg",
      "role": "member",
      "status": 0,
      "joinedAt": "2025-01-15T12:02:00",
      "muteUntil": "2025-01-16T12:00:00"
    }
  ]
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| userId | Long | 用户ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| role | String | 成员角色：`owner` 群主、`admin` 管理员、`member` 成员 |
| status | Integer | 成员状态：`0` 正常 |
| joinedAt | LocalDateTime | 加入时间 |
| muteUntil | LocalDateTime | 禁言截止时间，为空表示未禁言 |

---

### 邀请群成员

**接口信息**
- 路径: `POST /api/user/chat/groups/{conversationId}/members`
- 鉴权: 是
- 说明: 邀请用户加入群聊，需要群主或管理员权限

**请求示例**

```javascript
axios.post('/api/user/chat/groups/1003/members', {
  memberUserIds: [105, 106]
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| memberUserIds | List<Long> | 是 | 要邀请的用户ID列表 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "userId": 105,
      "username": "zhaoqi",
      "nickname": "赵七",
      "avatar": "https://example.com/avatar/zhaoqi.jpg",
      "role": "member",
      "status": 0,
      "joinedAt": "2025-01-15T12:10:00",
      "muteUntil": null
    },
    {
      "userId": 106,
      "username": "sunba",
      "nickname": "孙八",
      "avatar": "https://example.com/avatar/sunba.jpg",
      "role": "member",
      "status": 0,
      "joinedAt": "2025-01-15T12:10:00",
      "muteUntil": null
    }
  ]
}
```

---

### 设置群管理员

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/admins/{memberUserId}`
- 鉴权: 是
- 说明: 将群成员设置为管理员，需要群主权限

**请求示例**

```javascript
axios.put('/api/user/chat/groups/1003/admins/102')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "userId": 101,
      "username": "zhangsan",
      "nickname": "张三",
      "role": "owner",
      "status": 0,
      "joinedAt": "2025-01-15T12:00:00",
      "muteUntil": null
    },
    {
      "userId": 102,
      "username": "lisi",
      "nickname": "李四",
      "role": "admin",
      "status": 0,
      "joinedAt": "2025-01-15T12:01:00",
      "muteUntil": null
    }
  ]
}
```

---

### 取消群管理员

**接口信息**
- 路径: `DELETE /api/user/chat/groups/{conversationId}/admins/{memberUserId}`
- 鉴权: 是
- 说明: 取消群成员的管理员身份，需要群主权限

**请求示例**

```javascript
axios.delete('/api/user/chat/groups/1003/admins/102')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "userId": 101,
      "username": "zhangsan",
      "nickname": "张三",
      "role": "owner",
      "status": 0,
      "joinedAt": "2025-01-15T12:00:00",
      "muteUntil": null
    },
    {
      "userId": 102,
      "username": "lisi",
      "nickname": "李四",
      "role": "member",
      "status": 0,
      "joinedAt": "2025-01-15T12:01:00",
      "muteUntil": null
    }
  ]
}
```

---

### 转让群主

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/owner`
- 鉴权: 是
- 说明: 将群主身份转让给其他成员，需要群主权限

**请求示例**

```javascript
axios.put('/api/user/chat/groups/1003/owner', {
  targetUserId: 102
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| targetUserId | Long | 是 | 新群主用户ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1003,
    "conversationType": "group",
    "name": "技术交流群",
    "ownerId": 102,
    "selfRole": "member",
    "updatedAt": "2025-01-15T12:15:00"
  }
}
```

---

### 设置群成员禁言

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/members/{memberUserId}/mute`
- 鉴权: 是
- 说明: 对群成员禁言或取消禁言，需要管理员或群主权限

**请求示例**

```javascript
// 禁言到指定时间
axios.put('/api/user/chat/groups/1003/members/103/mute', {
  muteUntil: '2025-01-16T12:00:00'
})

// 取消禁言
axios.put('/api/user/chat/groups/1003/members/103/mute', {
  muteUntil: null
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| muteUntil | LocalDateTime | 否 | 禁言截止时间，为空表示取消禁言 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "userId": 101,
      "username": "zhangsan",
      "nickname": "张三",
      "role": "owner",
      "status": 0,
      "joinedAt": "2025-01-15T12:00:00",
      "muteUntil": null
    },
    {
      "userId": 103,
      "username": "wangwu",
      "nickname": "王五",
      "role": "member",
      "status": 0,
      "joinedAt": "2025-01-15T12:02:00",
      "muteUntil": "2025-01-16T12:00:00"
    }
  ]
}
```

---

### 更新群公告

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/notice`
- 鉴权: 是
- 说明: 更新群公告，需要管理员或群主权限

**请求示例**

```javascript
axios.put('/api/user/chat/groups/1003/notice', {
  notice: '本群公告：请大家遵守群规，禁止发广告。'
})

// 清空公告
axios.put('/api/user/chat/groups/1003/notice', {
  notice: ''
})
```

**请求体**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| notice | String | 否 | 群公告内容，最大500字符，为空表示清空 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1003,
    "conversationType": "group",
    "name": "技术交流群",
    "notice": "本群公告：请大家遵守群规，禁止发广告。",
    "updatedAt": "2025-01-15T12:20:00"
  }
}
```

---

### 移除群成员

**接口信息**
- 路径: `DELETE /api/user/chat/groups/{conversationId}/members/{memberUserId}`
- 鉴权: 是
- 说明: 将成员移出群聊，需要管理员或群主权限

**请求示例**

```javascript
axios.delete('/api/user/chat/groups/1003/members/103')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

### 退出群聊

**接口信息**
- 路径: `POST /api/user/chat/groups/{conversationId}/leave`
- 鉴权: 是
- 说明: 当前用户退出群聊，群主退出会导致群聊解散

**请求示例**

```javascript
axios.post('/api/user/chat/groups/1003/leave')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

### 解散群聊

**接口信息**
- 路径: `DELETE /api/user/chat/groups/{conversationId}`
- 鉴权: 是
- 说明: 解散群聊，需要群主权限

**请求示例**

```javascript
axios.delete('/api/user/chat/groups/1003')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

### 加入公开频道或公开群

**接口信息**
- 路径: `POST /api/user/chat/conversations/{conversationId}/join`
- 鉴权: 是
- 说明: 加入一个公开的频道或群聊

**请求示例**

```javascript
axios.post('/api/user/chat/conversations/1005/join')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1005,
    "conversationType": "group",
    "sceneType": "public_channel",
    "name": "公开聊天室",
    "avatar": "https://example.com/avatar/public-room.jpg",
    "ownerId": 1,
    "status": 0,
    "visibilityScope": "public",
    "selfRole": "member",
    "memberCount": 128,
    "createdAt": "2024-12-01T10:00:00",
    "updatedAt": "2025-01-15T12:00:00"
  }
}
```

---

### 离开频道或公开群

**接口信息**
- 路径: `POST /api/user/chat/conversations/{conversationId}/leave`
- 鉴权: 是
- 说明: 离开一个公开的频道或群聊

**请求示例**

```javascript
axios.post('/api/user/chat/conversations/1005/leave')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

## 群聊发现页

### 搜索公开群聊

**接口信息**
- 路径: `GET /api/user/chat/groups/search`
- 鉴权: 是
- 说明: 搜索公开的群聊，支持按群名称和群简介模糊查询

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |
| keyword | String | 否 | - | 关键词，按群名称和群简介搜索 |
| categoryCode | String | 否 | - | 群分类编码 |

**请求示例**

```javascript
// 搜索所有公开群
axios.get('/api/user/chat/groups/search', {
  params: { current: 1, size: 20 }
})

// 搜索特定关键词
axios.get('/api/user/chat/groups/search', {
  params: { current: 1, size: 20, keyword: '技术' }
})

// 按分类筛选
axios.get('/api/user/chat/groups/search', {
  params: { current: 1, size: 20, categoryCode: 'tech' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 50,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 1005,
        "name": "技术交流群",
        "avatar": "https://example.com/avatar/tech-group.jpg",
        "ownerId": 1,
        "description": "欢迎技术爱好者加入，共同探讨技术问题",
        "notice": "本群禁止广告",
        "visibilityScope": "public",
        "joinRule": "free",
        "speakLevelLimit": 1,
        "memberLimit": 200,
        "channelCategoryCode": "tech",
        "memberCount": 128,
        "joined": false,
        "selfRole": null,
        "createdAt": "2024-12-01T10:00:00"
      },
      {
        "id": 1006,
        "name": "前端开发群",
        "avatar": "https://example.com/avatar/fe-group.jpg",
        "ownerId": 2,
        "description": "前端开发者交流群",
        "notice": null,
        "visibilityScope": "public",
        "joinRule": "approval",
        "speakLevelLimit": 1,
        "memberLimit": 100,
        "channelCategoryCode": "tech",
        "memberCount": 56,
        "joined": true,
        "selfRole": "member",
        "createdAt": "2024-11-15T10:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 群聊ID |
| name | String | 群名称 |
| avatar | String | 群头像URL |
| ownerId | Long | 群主用户ID |
| description | String | 群简介 |
| notice | String | 群公告 |
| visibilityScope | String | 可见范围：`public` 公开、`private` 私有 |
| joinRule | String | 加入规则：`free` 自由加入、`approval` 需要审批、`invite_only` 邀请制 |
| speakLevelLimit | Integer | 发言最低等级限制 |
| memberLimit | Integer | 成员上限，0表示不限制 |
| channelCategoryCode | String | 群分类编码 |
| memberCount | Long | 当前成员数量 |
| joined | Boolean | 当前用户是否已加入 |
| selfRole | String | 当前用户在群中的角色，为空表示未加入 |
| createdAt | LocalDateTime | 群创建时间 |

---

## 大厅页（公开访客）

### 访客查看大厅消息

**接口信息**
- 路径: `GET /api/public/chat/lobby/messages`
- 鉴权: 否（公开接口，访客可访问）
- 说明: 大厅是公开频道，无需登录即可查看消息

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |
| beforeMessageId | Long | 否 | - | 查询该消息ID之前的消息，用于滚动加载 |

**请求示例**

```javascript
// 首次加载（无需鉴权）
axios.get('/api/public/chat/lobby/messages', {
  params: { current: 1, size: 20 }
})

// 加载更多
axios.get('/api/public/chat/lobby/messages', {
  params: { current: 1, size: 20, beforeMessageId: 5000 }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 200,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 5020,
        "senderId": 1,
        "senderName": "系统管理员",
        "senderAvatar": "https://example.com/avatar/admin.jpg",
        "messageType": "text",
        "content": "欢迎来到大厅！",
        "createdAt": "2025-01-15T08:00:00"
      },
      {
        "id": 5019,
        "senderId": 10,
        "senderName": "游客张三",
        "senderAvatar": "https://example.com/avatar/guest.jpg",
        "messageType": "text",
        "content": "大家好",
        "createdAt": "2025-01-15T08:05:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 消息ID |
| senderId | Long | 发送者用户ID |
| senderName | String | 发送者名称 |
| senderAvatar | String | 发送者头像URL |
| messageType | String | 消息类型：`text` 文本、`file` 文件等 |
| content | String | 消息内容 |
| createdAt | LocalDateTime | 发送时间 |

---

## 枚举值说明

### 会话类型 (conversationType)
| 值 | 说明 |
|---|------|
| single | 单聊 |
| group | 群聊 |
| global | 全局频道 |

### 业务场景 (sceneType)
| 值 | 说明 |
|---|------|
| single_chat | 单聊 |
| group_chat | 群聊 |
| public_channel | 公开频道 |

### 消息类型 (messageType)
| 值 | 说明 |
|---|------|
| text | 文本消息 |
| file | 文件消息 |
| image | 图片消息（file 的子类型） |
| audio | 语音消息（file 的子类型） |
| video | 视频消息（file 的子类型） |

### 成员角色 (role)
| 值 | 说明 |
|---|------|
| owner | 群主 |
| admin | 管理员 |
| member | 普通成员 |

### 投递状态 (deliveryStatus)
| 值 | 说明 |
|---|------|
| 0 | 待投递 |
| 1 | 已送达 |
| 2 | 已读 |

### 加入规则 (joinRule)
| 值 | 说明 |
|---|------|
| free | 自由加入 |
| approval | 需要审批 |
| invite_only | 邀请制 |

### 可见范围 (visibilityScope)
| 值 | 说明 |
|---|------|
| public | 公开 |
| private | 私有 |

### 回复消息状态 (state)
| 值 | 说明 |
|---|------|
| normal | 正常 |
| revoked | 已撤回 |
| unavailable | 不可用 |

### 文件转码状态 (transcodeStatus)
| 值 | 说明 |
|---|------|
| source | 原始文件 |
| pending | 转码中 |
| ready | 可用 |
| failed | 转码失败 |

---

## 后台主题频道管理

### 创建主题频道

**接口信息**
- 路径: `POST /api/sys/chats/topic-channels`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 创建主题频道

**请求示例**

```javascript
axios.post('/api/sys/chats/topic-channels', {
  name: '技术讨论频道',
  avatar: 'https://example.com/avatar/tech-channel.jpg',
  categoryCode: 'tech',
  visibilityScope: 'public',
  joinRule: 'free',
  speakLevelLimit: 1,
  memberLimit: 500
})
```

---

### 更新主题频道

**接口信息**
- 路径: `PUT /api/sys/chats/topic-channels/{conversationId}`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 更新主题频道信息

**请求示例**

```javascript
axios.put('/api/sys/chats/topic-channels/1005', {
  name: '技术讨论频道（新）',
  notice: '请遵守频道规则'
})
```

---

## 公开主题频道

### 分页查询主题频道

**接口信息**
- 路径: `GET /api/public/chat/channels`
- 鉴权: 否（公开接口）
- 说明: 分页查询公开主题频道列表

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |
| categoryCode | String | 否 | - | 频道分类编码筛选 |

**请求示例**

```javascript
axios.get('/api/public/chat/channels', {
  params: { current: 1, size: 20, categoryCode: 'tech' }
})
```

---

### 查询主题频道详情

**接口信息**
- 路径: `GET /api/public/chat/channels/{conversationId}`
- 鉴权: 否（公开接口）
- 说明: 查询指定主题频道的详细信息

**请求示例**

```javascript
axios.get('/api/public/chat/channels/1005')
```

---

## 帖子频道关联

### 分享帖子到频道

**接口信息**
- 路径: `POST /api/user/chat/forum-links`
- 鉴权: 是
- 说明: 将论坛帖子分享到聊天频道

**请求示例**

```javascript
axios.post('/api/user/chat/forum-links', {
  forumPostId: 2001,
  conversationId: 1005
})
```

---

### 查询帖子关联的频道

**接口信息**
- 路径: `GET /api/user/chat/forum-links/posts/{forumPostId}`
- 鉴权: 是
- 说明: 查询指定帖子所关联的频道信息

**请求示例**

```javascript
axios.get('/api/user/chat/forum-links/posts/2001')
```

---

### 分页查询频道关联的帖子

**接口信息**
- 路径: `GET /api/user/chat/forum-links/channels/{conversationId}`
- 鉴权: 是
- 说明: 分页查询指定频道关联的帖子列表

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |

**请求示例**

```javascript
axios.get('/api/user/chat/forum-links/channels/1005', {
  params: { current: 1, size: 20 }
})
```

---

### 取消帖子频道关联

**接口信息**
- 路径: `DELETE /api/user/chat/forum-links/posts/{forumPostId}`
- 鉴权: 是
- 说明: 取消帖子与频道的关联

**请求示例**

```javascript
axios.delete('/api/user/chat/forum-links/posts/2001')
```

---

## 后台频道申请管理

### 分页查询频道申请

**接口信息**
- 路径: `GET /api/sys/chats/channel-applications`
- 鉴权: 是（权限: `content:channel-application:query`）
- 说明: 分页查询频道创建申请列表

**请求示例**

```javascript
axios.get('/api/sys/chats/channel-applications', {
  params: { current: 1, size: 20 }
})
```

---

### 查询频道申请详情

**接口信息**
- 路径: `GET /api/sys/chats/channel-applications/{id}`
- 鉴权: 是（权限: `content:channel-application:query`）
- 说明: 查询指定频道申请的详细信息

**请求示例**

```javascript
axios.get('/api/sys/chats/channel-applications/301')
```

---

### 审核频道申请

**接口信息**
- 路径: `PUT /api/sys/chats/channel-applications/{id}/review`
- 鉴权: 是（权限: `content:channel-application:review`）
- 说明: 审核频道创建申请，通过或拒绝

**请求示例**

```javascript
// 通过
axios.put('/api/sys/chats/channel-applications/301/review', {
  approved: true,
  reviewRemark: '符合要求，予以通过'
})

// 拒绝
axios.put('/api/sys/chats/channel-applications/301/review', {
  approved: false,
  reviewRemark: '频道名称不规范，请修改后重新提交'
})
```

---

## 用户频道创建申请

### 提交频道创建申请

**接口信息**
- 路径: `POST /api/user/chat/channel-applications`
- 鉴权: 是
- 说明: 用户提交频道创建申请

**请求示例**

```javascript
axios.post('/api/user/chat/channel-applications', {
  name: '前端学习频道',
  description: '前端开发者学习与交流',
  categoryCode: 'tech',
  reason: '希望创建一个专注于前端技术学习的频道'
})
```

---

### 查询最近一次申请

**接口信息**
- 路径: `GET /api/user/chat/channel-applications/latest`
- 鉴权: 是
- 说明: 查询当前用户最近一次频道创建申请

**请求示例**

```javascript
axios.get('/api/user/chat/channel-applications/latest')
```

---

### 分页查询我的申请

**接口信息**
- 路径: `GET /api/user/chat/channel-applications`
- 鉴权: 是
- 说明: 分页查询当前用户的频道创建申请列表

**请求示例**

```javascript
axios.get('/api/user/chat/channel-applications', {
  params: { current: 1, size: 20 }
})
```

---

## 群邀请链接

### 创建邀请链接

**接口信息**
- 路径: `POST /api/user/chat/groups/{conversationId}/invite-links`
- 鉴权: 是
- 说明: 为群聊创建邀请链接

**请求示例**

```javascript
axios.post('/api/user/chat/groups/1003/invite-links', {
  maxUses: 10,
  expireHours: 24
})
```

---

### 分页查询邀请链接

**接口信息**
- 路径: `GET /api/user/chat/groups/{conversationId}/invite-links`
- 鉴权: 是
- 说明: 分页查询群聊的邀请链接列表

**请求示例**

```javascript
axios.get('/api/user/chat/groups/1003/invite-links', {
  params: { current: 1, size: 20 }
})
```

---

### 禁用邀请链接

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/invite-links/{inviteLinkId}/disable`
- 鉴权: 是
- 说明: 禁用指定的邀请链接

**请求示例**

```javascript
axios.put('/api/user/chat/groups/1003/invite-links/501/disable')
```

---

### 通过邀请令牌加入群

**接口信息**
- 路径: `POST /api/user/chat/group-invite-links/{inviteToken}/join`
- 鉴权: 是
- 说明: 通过邀请令牌加入群聊

**请求示例**

```javascript
axios.post('/api/user/chat/group-invite-links/abc123token/join')
```

---

## 入群申请

### 提交入群申请

**接口信息**
- 路径: `POST /api/user/chat/groups/{conversationId}/join-applications`
- 鉴权: 是
- 说明: 向需要审批的群聊提交入群申请

**请求示例**

```javascript
axios.post('/api/user/chat/groups/1003/join-applications', {
  reason: '希望加入群聊学习交流'
})
```

---

### 我的入群申请

**接口信息**
- 路径: `GET /api/user/chat/group-join-applications`
- 鉴权: 是
- 说明: 分页查询当前用户提交的入群申请列表

**请求示例**

```javascript
axios.get('/api/user/chat/group-join-applications', {
  params: { current: 1, size: 20 }
})
```

---

### 查询群的入群申请列表

**接口信息**
- 路径: `GET /api/user/chat/groups/{conversationId}/join-applications`
- 鉴权: 是
- 说明: 查询指定群的入群申请列表，需要管理员或群主权限

**请求示例**

```javascript
axios.get('/api/user/chat/groups/1003/join-applications', {
  params: { current: 1, size: 20 }
})
```

---

### 审核入群申请

**接口信息**
- 路径: `PUT /api/user/chat/groups/{conversationId}/join-applications/{applicationId}/review`
- 鉴权: 是
- 说明: 审核入群申请，通过或拒绝，需要管理员或群主权限

**请求示例**

```javascript
// 通过
axios.put('/api/user/chat/groups/1003/join-applications/401/review', {
  approved: true
})

// 拒绝
axios.put('/api/user/chat/groups/1003/join-applications/401/review', {
  approved: false,
  rejectReason: '不符合群聊加入条件'
})
```

---

## 后台大厅管理

### 更新大厅设置

**接口信息**
- 路径: `PUT /api/sys/chats/lobby/settings`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 更新大厅的全局设置

**请求示例**

```javascript
axios.put('/api/sys/chats/lobby/settings', {
  speakLevelLimit: 2,
  slowModeSeconds: 5,
  allowGuestSpeak: false
})
```

---

### 置顶消息

**接口信息**
- 路径: `POST /api/sys/chats/lobby/messages/{messageId}/pin`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 置顶大厅中的指定消息

**请求示例**

```javascript
axios.post('/api/sys/chats/lobby/messages/5020/pin')
```

---

### 取消置顶

**接口信息**
- 路径: `DELETE /api/sys/chats/lobby/messages/{messageId}/pin`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 取消大厅中指定消息的置顶状态

**请求示例**

```javascript
axios.delete('/api/sys/chats/lobby/messages/5020/pin')
```

---

### 分页查询置顶消息

**接口信息**
- 路径: `GET /api/sys/chats/lobby/messages/pinned`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 分页查询大厅中的置顶消息列表

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|-----|------|------|--------|-----|
| current | Long | 否 | 1 | 页码 |
| size | Long | 否 | 20 | 每页条数 |

**请求示例**

```javascript
axios.get('/api/sys/chats/lobby/messages/pinned', {
  params: { current: 1, size: 20 }
})
```

---

### 禁言大厅成员

**接口信息**
- 路径: `PUT /api/sys/chats/lobby/members/{memberUserId}/mute`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 禁言大厅中的指定成员

**请求示例**

```javascript
axios.put('/api/sys/chats/lobby/members/103/mute', {
  muteUntil: '2025-01-16T12:00:00'
})
```

---

### 踢出大厅成员

**接口信息**
- 路径: `PUT /api/sys/chats/lobby/members/{memberUserId}/kick`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 将指定成员踢出大厅

**请求示例**

```javascript
axios.put('/api/sys/chats/lobby/members/103/kick')
```

---

## 后台聊天治理

### 分页查询会话

**接口信息**
- 路径: `GET /api/sys/chats/conversations`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 分页查询所有会话列表，用于后台管理

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations', {
  params: { current: 1, size: 20 }
})
```

---

### 查询会话详情

**接口信息**
- 路径: `GET /api/sys/chats/conversations/{conversationId}`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 查询指定会话的详细信息

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations/1001')
```

---

### 查询会话成员列表

**接口信息**
- 路径: `GET /api/sys/chats/conversations/{conversationId}/members`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 查询指定会话的成员列表

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations/1003/members')
```

---

### 分页查询会话消息

**接口信息**
- 路径: `GET /api/sys/chats/conversations/{conversationId}/messages`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 分页查询指定会话的消息列表

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations/1001/messages', {
  params: { current: 1, size: 20 }
})
```

---

### 查询消息详情

**接口信息**
- 路径: `GET /api/sys/chats/conversations/{conversationId}/messages/{messageId}`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 查询指定消息的详细信息

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations/1001/messages/9021')
```

---

### 查询消息已读回执

**接口信息**
- 路径: `GET /api/sys/chats/conversations/{conversationId}/messages/{messageId}/receipts`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 查询指定消息的已读回执列表

**请求示例**

```javascript
axios.get('/api/sys/chats/conversations/1001/messages/9021/receipts', {
  params: { current: 1, size: 20 }
})
```

---

### 修改成员角色

**接口信息**
- 路径: `PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/role`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 修改会话中指定成员的角色

**请求示例**

```javascript
axios.put('/api/sys/chats/conversations/1003/members/103/role', {
  role: 'admin'
})
```

---

### 修改成员状态

**接口信息**
- 路径: `PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/status`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 修改会话中指定成员的状态（如正常、禁用）

**请求示例**

```javascript
axios.put('/api/sys/chats/conversations/1003/members/103/status', {
  status: 1
})
```

---

### 修改成员禁言

**接口信息**
- 路径: `PUT /api/sys/chats/conversations/{conversationId}/members/{memberUserId}/mute`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 修改会话中指定成员的禁言状态

**请求示例**

```javascript
axios.put('/api/sys/chats/conversations/1003/members/103/mute', {
  muteUntil: '2025-01-16T12:00:00'
})
```

---

### 后台撤回消息

**接口信息**
- 路径: `POST /api/sys/chats/conversations/{conversationId}/messages/{messageId}/revoke`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 后台管理员撤回指定会话中的消息

**请求示例**

```javascript
axios.post('/api/sys/chats/conversations/1001/messages/9021/revoke')
```

---

### 修改会话状态

**接口信息**
- 路径: `PUT /api/sys/chats/conversations/{conversationId}/status`
- 鉴权: 是（权限: `content:chat:update-status`）
- 说明: 修改指定会话的状态（如正常、禁用、归档）

**请求示例**

```javascript
axios.put('/api/sys/chats/conversations/1003/status', {
  status: 1
})
```

---

## 后台禁言管理

### 创建禁言记录

**接口信息**
- 路径: `POST /api/sys/chats/mutes`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 创建禁言记录，对用户进行禁言

**请求示例**

```javascript
axios.post('/api/sys/chats/mutes', {
  userId: 103,
  conversationId: 1003,
  reason: '发布违规内容',
  muteUntil: '2025-01-20T12:00:00'
})
```

---

### 分页查询禁言记录

**接口信息**
- 路径: `GET /api/sys/chats/mutes`
- 鉴权: 是（权限: `content:chat:query`）
- 说明: 分页查询禁言记录列表

**请求示例**

```javascript
axios.get('/api/sys/chats/mutes', {
  params: { current: 1, size: 20 }
})
```

---

### 解除禁言

**接口信息**
- 路径: `PUT /api/sys/chats/mutes/{id}/release`
- 鉴权: 是（权限: `content:chat:update`）
- 说明: 解除指定的禁言记录

**请求示例**

```javascript
axios.put('/api/sys/chats/mutes/601/release')
```
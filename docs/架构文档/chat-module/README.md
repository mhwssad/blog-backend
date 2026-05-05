# Chat 模块架构文档

本文档收集了 Chat 模块（即时通讯/私聊）完整的架构设计文档。

## 文档索引

| 文档 | 说明 |
|------|------|
| [00-chat-module-overview.md](./00-chat-module-overview.md) | Chat 模块总览：定位、分层、目录结构、能力矩阵 |
| [chat-data-model.md](./chat-data-model.md) | Chat 数据模型：数据库表结构、实体映射、枚举定义 |
| [chat-message-flow.md](./chat-message-flow.md) | 消息收发流程：消息发送、投递、已读、推送完整链路 |
| [chat-conversation-flow.md](./chat-conversation-flow.md) | 会话管理流程：单聊、群聊、频道的创建与查询 |
| [chat-group-management.md](./chat-group-management.md) | 群组管理流程：成员管理、角色变更、邀请链接 |
| [chat-governance.md](./chat-governance.md) | 治理功能：禁言、举报、后台管理接口 |

## 核心能力概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Chat 模块能力                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   单聊            │  │   群聊           │  │   大厅/话题频道    │          │
│  │   会话/消息       │  │   成员管理       │  │   公开聊天        │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   消息收发        │  │   已读/投递状态   │  │   实时推送        │          │
│  │   文本/文件       │  │   游标管理       │  │   WebSocket/Push │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   附件处理        │  │   禁言管理        │  │   后台管理        │          │
│  │   图片/语音异步   │  │   全局/会话级别   │  │   审计/运营       │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐                                │
│  │   邀请链接        │  │   举报处理        │                                │
│  │   入群申请        │  │   内容治理        │                                │
│  │   ✅ 完成         │  │   ✅ 完成        │                                │
│  └──────────────────┘  └──────────────────┘                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 快速导航

### 开发者视角

1. **理解模块架构** → 参考 [00-chat-module-overview.md](./00-chat-module-overview.md)
2. **查看数据表结构** → 参考 [chat-data-model.md](./chat-data-model.md)
3. **理解消息收发** → 参考 [chat-message-flow.md](./chat-message-flow.md)
4. **理解会话管理** → 参考 [chat-conversation-flow.md](./chat-conversation-flow.md)
5. **理解群组管理** → 参考 [chat-group-management.md](./chat-group-management.md)
6. **理解治理功能** → 参考 [chat-governance.md](./chat-governance.md)

### 运营视角

1. **会话管理** → 后台 `/api/sys/chats/conversations`
2. **消息审计** → 后台 `/api/sys/chats/conversations/{id}/messages`
3. **成员管理** → 后台 `/api/sys/chats/conversations/{id}/members`
4. **禁言管理** → 后台 `/api/sys/chat/mutes`
5. **大厅管理** → 后台 `/api/sys/chat/lobby`
6. **频道管理** → 后台 `/api/sys/chat/topic-channels`

### 用户侧接口

- 会话列表：`GET /api/user/chat/conversations`
- 发送消息：`POST /api/user/chat/messages/text`
- 群组操作：`POST /api/user/chat/groups`
- 加入频道：`POST /api/user/chat/conversations/{id}/join`

## 相关文档

- [AI 模块架构文档](../ai-module/README.md)
- [项目代码编写规范](../../项目代码编写规范.md)
- [项目结构规范](../../项目结构规范.md)

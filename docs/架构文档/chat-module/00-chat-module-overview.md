# Chat 模块总览

## 1. 模块定位

Chat 模块（`module/chat`）是博客后端的核心即时通讯服务层，负责：

- **单聊**：用户之间的一对一私密对话
- **群聊**：多人参与的群组会话，支持管理员角色
- **大厅/话题频道**：公开的聊天室，支持游客访问（只读）
- **消息分发**：文本、图片、文件等消息类型的发送与投递
- **实时推送**：通过 WebSocket 和 Redis Pub/Sub 实现消息实时推送
- **治理能力**：禁言、举报、内容审核等社区治理功能

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   REST: /api/user/chat/*                                        │
│   WebSocket: /ws/chat                                           │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│   UserChatController        ChatWebSocketHandler                │
│   PublicChatLobbyController  ChatTopicChannelAdminController    │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │ UserChatService  │  │ ChatMessageSend  │  │ChatPushService │ │
│  │ 会话/消息/群管理   │  │ 文本/文件发送     │  │ 实时推送       │ │
│  └──────────────────┘  └──────────────────┘  └────────────────┘ │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │ChatGroupManage   │  │ChatMuteGovernance│  │ChatAdminService│ │
│  │ 群组管理          │  │ 禁言治理          │  │ 后台管理       │ │
│  └──────────────────┘  └──────────────────┘  └────────────────┘ │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ChatLobbyAdmin    │  │ChatMetricsService│                     │
│  │ 大厅管理          │  │ 统计指标          │                     │
│  └──────────────────┘  └──────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │  ChatPushService│   │  File Service   │
│   数据访问        │   │  Redis Pub/Sub │   │  文件处理       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  Redis         │   │  OSS            │
│  持久化存储       │   │  会话缓存/推送   │   │  文件存储       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 目录结构

```
module/chat/
├── attachment/                    # 附件处理
│   ├── repository/              # 附件处理任务数据访问
│   ├── service/                 # 附件元数据解析、异步处理服务
│   └── task/                    # 定时任务（处理失败重试）
├── conversation/                 # 会话管理
│   ├── controller/              # 控制器（用户侧 + 后台管理）
│   ├── model/                   # 请求/响应模型
│   ├── repository/             # 数据访问层
│   └── service/                # 业务服务
├── governance/                  # 治理功能
│   ├── controller/             # 后台管理控制器
│   ├── convert/                # 模型转换
│   ├── model/                  # 请求/响应模型
│   ├── repository/             # 数据访问层
│   └── service/                # 治理服务
├── member/                      # 成员管理
│   ├── controller/             # 控制器
│   ├── model/                  # 请求/响应模型
│   ├── repository/             # 数据访问层
│   └── service/                # 成员管理服务
├── message/                     # 消息管理
│   ├── controller/             # 控制器
│   ├── model/                  # 请求/响应模型
│   ├── repository/             # 数据访问层
│   └── service/                # 消息服务
├── push/                        # 实时推送
│   ├── config/                 # Redis推送配置
│   └── service/                # 推送服务实现
├── shared/                      # 共享组件
│   ├── constant/               # 常量定义
│   ├── convert/                # MapStruct转换器
│   ├── model/                  # 共享模型（data/internal/common）
│   └── support/                # 共享工具类
└── websocket/                   # WebSocket处理
    ├── codec/                   # 消息编解码
    ├── handler/                # WebSocket处理器
    └── model/                  # WebSocket模型
```

## 4. 子域划分

| 子域 | 职责 | 核心服务 |
|------|------|----------|
| conversation | 会话生命周期管理 | ChatConversationQueryService, ChatLobbyAdminService |
| message | 消息发送、编辑、撤回、删除 | ChatMessageSendService, ChatMessageLifecycleService |
| member | 成员管理、入群审批、邀请链接 | ChatGroupManageService, ChatChannelJoinService |
| push | 实时消息推送、已读状态推送 | ChatPushService, ChatNotificationService |
| attachment | 附件元数据解析、异步处理 | ChatAttachmentAsyncProcessingService |
| governance | 禁言、举报、内容治理 | ChatMuteGovernanceService, ChatAdminGovernanceService |

## 5. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 单聊会话 | ChatConversationQueryService | ✅ 完成 |
| 群聊管理 | ChatGroupManageService | ✅ 完成 |
| 大厅频道 | ChatLobbyAdminService | ✅ 完成 |
| 话题频道 | ChatTopicChannelPublicService | ✅ 完成 |
| 消息发送 | ChatMessageSendService | ✅ 完成 |
| 消息生命周期 | ChatMessageLifecycleService | ✅ 完成 |
| 实时推送 | ChatPushService | ✅ 完成 |
| WebSocket | ChatWebSocketHandler | ✅ 完成 |
| 已读游标 | ChatMessageReadCursor | ✅ 完成 |
| 附件处理 | ChatAttachmentAsyncProcessingService | ✅ 完成 |
| 禁言管理 | ChatMuteGovernanceService | ✅ 完成 |
| 入群申请 | ChatChannelJoinService | ✅ 完成 |
| 邀请链接 | ChatGroupInviteLinkService | ✅ 完成 |
| 后台管理 | ChatAdminService | ✅ 完成 |
| 统计指标 | ChatMetricsService | ✅ 完成 |

## 6. 会话类型

| 类型 | conversationType | sceneType | 说明 |
|------|------------------|-----------|------|
| 单聊 | single | single_chat | 用户间一对一聊天 |
| 用户群聊 | group | user_group | 用户创建的群组 |
| 大厅频道 | group | hall_channel | 全站公开的大厅 |
| 话题频道 | group | topic_channel | 按话题分类的公开频道 |
| 全局频道 | group | global_channel | 全站级别的特殊频道 |

## 7. 成员角色

| 角色 | memberRole | 说明 |
|------|------------|------|
| 群主 | owner | 群组创建者，拥有最高权限 |
| 管理员 | admin | 群组管理员，可管理普通成员 |
| 普通成员 | member | 群组普通成员 |

## 8. 消息类型

| 类型 | messageType | 说明 |
|------|-------------|------|
| 文本消息 | text | 普通文本内容 |
| 图片消息 | image | 图片附件 |
| 文件消息 | file | 通用文件附件 |
| 系统消息 | system | 系统通知（如成员变动） |

## 9. 扩展方向

- **消息撤回时间限制**：目前支持任意时间撤回，可考虑增加时间限制
- **消息反应/表情**：增加点赞、表情等互动功能
- **@提及优化**：支持@所有人的次数限制
- **消息搜索**：全站消息内容搜索
- **历史消息导出**：管理员导出聊天记录

## 10. 相关文档

- [Chat 数据模型](./chat-data-model.md)
- [消息收发流程](./chat-message-flow.md)
- [会话管理流程](./chat-conversation-flow.md)
- [群组管理流程](./chat-group-management.md)
- [治理功能](./chat-governance.md)

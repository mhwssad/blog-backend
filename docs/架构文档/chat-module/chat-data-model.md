# Chat 数据模型

## 1. 数据库表概览

| 表名 | 说明 |
|------|------|
| chat_conversation | 聊天会话表 |
| chat_conversation_member | 聊天会话成员表 |
| chat_message | 聊天消息表 |
| chat_message_recipient | 聊天消息接收状态表 |
| chat_message_read_cursor | 聊天会话已读游标表 |
| chat_user_mute_record | 统一禁言记录表 |
| chat_group_join_application | 群聊入群申请表 |
| chat_channel_create_application | 频道创建申请表 |
| chat_group_invite_link | 群聊邀请链接表 |
| chat_attachment_process_task | 聊天附件异步处理任务表 |

## 2. 核心表结构

### 2.1 chat_conversation（聊天会话表）

```sql
chat_conversation @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatConversation.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_type | VARCHAR(20) | 会话类型：single/group/system |
| name | VARCHAR(100) | 会话名称（群聊时由创建者指定） |
| avatar | VARCHAR(500) | 会话头像URL |
| owner_id | BIGINT | 创建者ID（群主） |
| single_pair_key | VARCHAR(64) | 单聊唯一标识（双方用户ID排序拼接的哈希） |
| scene_type | VARCHAR(50) | 业务场景：single_chat/user_group/hall_channel/topic_channel/global_channel |
| visibility_scope | VARCHAR(20) | 可见范围：public/member/private |
| allow_guest_view | TINYINT | 访客是否可见：0-否，1-是 |
| require_join_to_speak | TINYINT | 是否需要加入后发言：0-否，1-是 |
| join_rule | VARCHAR(20) | 加入规则：free/approval/invite_only |
| speak_level_limit | INT | 发言最低等级限制 |
| member_limit | INT | 成员上限：0-不限制 |
| is_all_site | TINYINT | 是否全站会话：0-否，1-是 |
| all_site_key | VARCHAR(50) | 全站会话唯一标识 |
| status | INT | 会话状态：0-禁用，1-正常，2-已解散 |
| remark | VARCHAR(500) | 备注 |
| announcement | VARCHAR(1000) | 频道/群公告 |
| slow_mode_seconds | INT | 慢速模式秒数：0-关闭 |
| display_sort | INT | 展示排序 |
| channel_category_code | VARCHAR(50) | 频道分类编码/群分类编码 |
| last_message_id | BIGINT | 最新消息ID |
| last_message_time | DATETIME | 最新消息时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.2 chat_conversation_member（聊天会话成员表）

```sql
chat_conversation_member @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatConversationMember.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 会话ID |
| user_id | BIGINT | 用户ID |
| member_role | VARCHAR(20) | 成员角色：owner/admin/member |
| join_source | VARCHAR(20) | 加入来源：invite/link/search |
| status | INT | 成员状态：0-正常，1-已退出，2-已移除 |
| mute_until | DATETIME | 禁言截止时间 |
| joined_at | DATETIME | 加入时间 |
| last_read_message_id | BIGINT | 最后已读消息ID |
| last_read_at | DATETIME | 最后已读时间 |
| last_delivered_message_id | BIGINT | 最后已投递消息ID |
| last_delivered_at | DATETIME | 最后已投递时间 |
| remark | VARCHAR(100) | 成员备注名 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.3 chat_message（聊天消息表）

```sql
chat_message @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatMessage.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 所属会话ID |
| sender_id | BIGINT | 发送者ID |
| message_type | VARCHAR(20) | 消息类型：text/image/file/system |
| content | TEXT | 消息文本内容 |
| payload_json | TEXT | 消息附加数据（JSON格式，含附件URL等） |
| reply_message_id | BIGINT | 回复目标消息ID |
| mention_all | TINYINT | 是否@全体：0-否，1-是 |
| mentioned_user_ids | VARCHAR(500) | 被@的用户ID列表（逗号分隔） |
| send_status | INT | 发送状态：0-发送中，1-已发送，2-发送失败 |
| revoke_status | INT | 撤回状态：0-正常，1-已撤回 |
| revoked_by | BIGINT | 撤回操作者ID |
| revoked_at | DATETIME | 撤回时间 |
| client_message_id | VARCHAR(100) | 客户端消息唯一标识（用于幂等去重） |
| pinned_by | BIGINT | 置顶操作人ID（NULL表示未置顶） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.4 chat_message_recipient（聊天消息接收状态表）

```sql
chat_message_recipient @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatMessageRecipient.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| message_id | BIGINT | 消息ID |
| conversation_id | BIGINT | 会话ID |
| recipient_user_id | BIGINT | 接收者ID |
| receive_type | VARCHAR(20) | 接收类型：single/group |
| delivery_status | INT | 投递状态：0-待投递，1-已投递，2-已读 |
| delivered_at | DATETIME | 投递时间 |
| read_at | DATETIME | 已读时间 |
| visible_status | INT | 可见状态：0-正常，1-已隐藏 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.5 chat_message_read_cursor（聊天会话已读游标表）

```sql
chat_message_read_cursor @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatMessageReadCursor.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 会话ID |
| user_id | BIGINT | 用户ID |
| read_message_id | BIGINT | 已读位置消息ID |
| read_at | DATETIME | 已读时间 |
| delivered_message_id | BIGINT | 已投递位置消息ID |
| delivered_at | DATETIME | 已投递时间 |
| unread_count | INT | 未读消息数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.6 chat_user_mute_record（统一禁言记录表）

```sql
chat_user_mute_record @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatUserMuteRecord.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 被禁言用户ID |
| scope | VARCHAR(20) | 禁言范围：global/lobby/topic_channel/group |
| conversation_id | BIGINT | 关联会话ID（lobby/topic_channel/group时必填） |
| mute_until | DATETIME | 禁言截止时间（NULL表示永久禁言） |
| status | INT | 状态：0-已解除，1-生效中 |
| reason | VARCHAR(500) | 禁言原因 |
| source_type | VARCHAR(20) | 来源：admin/report/auto |
| report_id | BIGINT | 关联举报ID |
| operator_id | BIGINT | 操作人ID |
| released_by | BIGINT | 解除人ID |
| released_at | DATETIME | 解除时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.7 chat_group_join_application（群聊入群申请表）

```sql
chat_group_join_application @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatGroupJoinApplication.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 会话/群ID |
| applicant_user_id | BIGINT | 申请用户ID |
| apply_message | VARCHAR(200) | 申请附言 |
| apply_status | INT | 申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消 |
| reviewer_id | BIGINT | 审核人ID |
| review_comment | VARCHAR(200) | 审核意见 |
| submitted_at | DATETIME | 提交时间 |
| reviewed_at | DATETIME | 审核时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.8 chat_channel_create_application（频道创建申请表）

```sql
chat_channel_create_application @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatChannelCreateApplication.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| applicant_user_id | BIGINT | 申请用户ID |
| desired_name | VARCHAR(100) | 期望频道名称 |
| desired_scene_type | VARCHAR(50) | 期望频道类型 |
| desired_category_code | VARCHAR(50) | 期望分类编码 |
| description | VARCHAR(500) | 申请说明 |
| apply_status | INT | 申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充 |
| conversation_id | BIGINT | 审核通过后关联频道ID |
| reviewer_id | BIGINT | 审核人ID |
| review_comment | VARCHAR(200) | 审核意见 |
| submitted_at | DATETIME | 提交时间 |
| reviewed_at | DATETIME | 审核时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.9 chat_group_invite_link（群聊邀请链接表）

```sql
chat_group_invite_link @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatGroupInviteLink.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 群聊会话ID |
| invite_token | VARCHAR(64) | 邀请链接令牌 |
| created_by | BIGINT | 创建人ID |
| expire_at | DATETIME | 过期时间，空表示不过期 |
| max_use_count | INT | 最大使用次数，0表示不限制 |
| used_count | INT | 已使用次数 |
| status | INT | 状态：0-停用，1-启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.10 chat_attachment_process_task（聊天附件异步处理任务表）

```sql
chat_attachment_process_task @ blog-backend/src/main/java/com/cybzacg/blogbackend/domain/chat/ChatAttachmentProcessTask.java
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| message_id | BIGINT | 关联消息ID |
| message_type | VARCHAR(20) | 消息类型：image/file |
| task_status | INT | 任务状态：0-待处理，1-处理中，2-已完成，3-失败 |
| retry_count | INT | 已重试次数 |
| max_retry_count | INT | 最大重试次数 |
| next_retry_at | DATETIME | 下次重试时间 |
| lease_expire_at | DATETIME | 处理租约过期时间 |
| started_at | DATETIME | 开始处理时间 |
| completed_at | DATETIME | 完成时间 |
| last_error | TEXT | 最近一次错误信息 |
| message_snapshot_json | TEXT | 消息快照（JSON，用于重试） |
| push_user_ids_json | TEXT | 待推送用户ID列表（JSON，用于重试） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 3. 枚举定义

### 3.1 会话类型（conversation_type）

| 值 | 说明 |
|------|------|
| single | 单聊 |
| group | 群聊 |
| system | 系统会话 |

### 3.2 业务场景（scene_type）

| 值 | 说明 |
|------|------|
| single_chat | 单聊 |
| user_group | 用户群聊 |
| hall_channel | 大厅频道 |
| topic_channel | 话题频道 |
| global_channel | 全局频道 |

### 3.3 成员角色（member_role）

| 值 | 说明 |
|------|------|
| owner | 群主 |
| admin | 管理员 |
| member | 普通成员 |

### 3.4 消息类型（message_type）

| 值 | 说明 |
|------|------|
| text | 文本消息 |
| image | 图片消息 |
| file | 文件消息 |
| system | 系统消息 |

### 3.5 禁言范围（mute_scope）

| 值 | 说明 |
|------|------|
| global | 全站禁言 |
| lobby | 大厅禁言 |
| topic_channel | 话题频道禁言 |
| group | 群组禁言 |

### 3.6 禁言来源（source_type）

| 值 | 说明 |
|------|------|
| admin | 管理员操作 |
| report | 举报处理 |
| auto | 自动禁言（如发广告自动检测） |

## 4. 实体关系图

```
┌─────────────────────┐       ┌─────────────────────┐
│ chat_conversation   │       │ chat_message        │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │───────┤ conversation_id (FK)│
│ name                │       │ id (PK)             │
│ conversation_type   │       │ sender_id           │
│ owner_id            │       │ message_type        │
│ single_pair_key     │       │ content             │
│ scene_type          │       └─────────────────────┘
│ ...                 │               │
└─────────────────────┘               │
         │                            │
         │         ┌─────────────────┴─────────────────┐
         │         │                                   │
         ▼         ▼                                   ▼
┌─────────────────────┐       ┌─────────────────────────────┐
│chat_conversation_    │       │ chat_message_recipient       │
│member                │       ├─────────────────────────────┤
├─────────────────────┤       │ id (PK)                      │
│ id (PK)             │       │ message_id (FK)              │
│ conversation_id (FK)│       │ recipient_user_id            │
│ user_id             │       │ delivery_status              │
│ member_role         │       └─────────────────────────────┘
│ status              │
│ mute_until          │
└─────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────┐
│chat_message_read_    │
│cursor                │
├─────────────────────┤
│ id (PK)             │
│ conversation_id (FK)│
│ user_id             │
│ read_message_id     │
│ unread_count        │
└─────────────────────┘
```

## 5. 相关文档

- [Chat 模块总览](./00-chat-module-overview.md)
- [消息收发流程](./chat-message-flow.md)
- [会话管理流程](./chat-conversation-flow.md)
- [群组管理流程](./chat-group-management.md)

# Chat 实现文档

本文档对应 2026-03-30 当前仓库中的 chat 模块实现，用于说明现状边界、关键语义和后续维护时必须保持一致的规则。

## 1. 当前实现范围

当前已经落地：

- 会话模型：单聊、群聊、全站群聊
- 消息类型：文本消息、文件消息
- 消息治理：编辑、撤回、仅当前用户视角删除
- 群治理：邀请、移除成员、设置/取消管理员、转让群主、禁言、群公告、退群、解散
- 后台管理：会话分页/详情、成员分页视图、消息分页、消息详情、回执明细、成员角色/状态/禁言调整、后台撤回
- 实时能力：HTTP + 单机 WebSocket，新消息和已读推进统一通过 `ChatPushService` 推送

当前仍未落地：

- 图片、语音、引用回复等更多消息类型
- 编辑/撤回/成员治理专用 WebSocket 事件
- 多节点 WebSocket 广播
- 基于客户端 ACK 的 delivered 语义

## 2. 核心数据模型

### 2.1 会话与成员

- `chat_conversation`
  - `single` 通过 `single_pair_key` 唯一收口
  - `group` 通过 `owner_id` 维护群主语义
  - `global` 通过 `is_all_site = 1` 收口全站群
- `chat_conversation_member`
  - `status = 1` 视为当前有效成员
  - `member_role` 当前使用 `owner/admin/member`
  - `mute_until` 晚于当前时间时，用户侧发送会被拒绝
  - 恢复成员关系时，会把游标重置到当前会话最后一条消息，避免历史消息直接算进未读

### 2.2 消息与回执

- `chat_message`
  - 文本消息：`message_type = text`
  - 文件消息：`message_type = file`
  - 扩展载荷统一走 `payload_json`
  - 撤回通过 `revoke_status / revoked_by / revoked_at` 表达
- `chat_message_recipient`
  - 每条消息会为当前活跃成员写一条 recipient 记录
  - 发送人自己的 recipient 直接写为 `已读`
  - “删除消息”当前是把当前用户对应 recipient 的 `visible_status` 改为隐藏，不做全局物理删除
- `chat_message_read_cursor`
  - 存储会话级 `read_message_id / delivered_message_id / unread_count`
  - 已读推进和拉历史消息时的 delivered 补记，最终都收口到这里

## 3. 文件消息与 file 模块复用边界

### 3.1 当前设计

- chat 模块不自己维护文件落库、上传任务和物理对象。
- 文件上传仍然完全复用 `file` 模块现有的初始化、秒传、分片上传、引用计数和生命周期收口。
- chat 发送文件消息时，只消费 `file_business_info.id`，不直接消费原始文件路径。

### 3.2 发送文件消息时的收口流程

1. 前端先通过 `file` 模块上传文件，拿到 `businessId`。
2. chat 发送文件消息时，校验该业务引用属于当前用户，且引用类型只能是 `temp` 或尚未绑定具体消息的 `chat_message`。
3. chat 根据真实 `file_info` 生成消息摘要，并写入 `chat_message`。
4. 随后创建新的 `file_business_info`：
   - `reference_type = chat_message`
   - `reference_id = chat_message.id`
   - `category = chat_attachment`
5. 若原始引用是上传阶段的临时引用，则删除旧临时引用。
6. 最终通过 `FileLifecycleService.refreshReferenceMetadata(...)` 统一回刷引用计数与公开性。

### 3.3 撤回文件消息时的处理

- 撤回会把消息内容改为“消息已撤回”，并清空 `payload_json`。
- 同时删除该消息对应的 `chat_message` 文件业务引用。
- 文件是否继续保留，统一交给 `FileLifecycleService.syncFileAfterReferenceRemoval(...)` 判断，不在 chat 模块内直接删物理文件。

## 4. 消息治理语义

### 4.1 编辑

- 仅允许编辑自己发送的文本消息。
- 文件消息、已撤回消息都不允许编辑。
- 当前没有单独的“是否编辑过”字段，接口返回中的 `edited` 由 `text` 消息且 `updated_at > created_at` 推导得到。

### 4.2 撤回

- 用户侧当前仅允许撤回自己发送的消息。
- 后台支持直接撤回任意会话内消息。
- 撤回会保留消息主记录，不做物理删除，只改撤回状态和展示内容。

### 4.3 删除

- 用户侧“删除消息”是隐藏当前用户视角，不影响其他成员。
- 删除后会重新计算当前用户该会话的 `unread_count`。

## 5. 群治理规则

- 群主：
  - 可邀请成员
  - 可设置/取消管理员
  - 可转让群主
  - 可禁言普通成员和管理员
  - 可更新群公告
  - 可移除普通成员和管理员
  - 不能直接退群，只能先转让群主或解散群聊
- 管理员：
  - 可邀请成员
  - 可禁言普通成员
  - 可更新群公告
  - 可移除普通成员
  - 不能操作群主，也不能操作其他管理员
- 普通成员：
  - 无治理权限

当前群公告直接复用 `chat_conversation.remark` 存储，没有新增独立公告表或字段。

## 6. 后台管理实现

后台聊天管理当前除了原有会话/消息分页，还新增：

- 消息详情：直接查看消息主体、文件载荷、撤回信息和接收统计
- 回执明细：按 `recipientUserId / deliveryStatus / visibleStatus` 筛选 recipient
- 成员管理：
  - 调整角色
  - 调整状态
  - 调整禁言截止时间
- 后台撤回：可直接撤回消息，并同步释放文件引用

后台成员管理当前允许直接作用于会话成员表；若把成员设为 `owner`，服务会同步更新 `chat_conversation.owner_id`。

## 7. WebSocket 现状

当前 WebSocket 仍只支持两类客户端请求：

- `send_message`
- `mark_read`

当前服务端主动推送也仍只有两类：

- `message_created`
- `read_updated`

也就是说：

- 文件消息发送后会像普通消息一样走 `message_created`
- 编辑、撤回、删除、群治理变更当前没有独立 WS 事件
- 前端若执行这些动作后需要立即刷新别人视图，当前应主动补拉 HTTP 数据

## 8. 事务边界

以下操作当前都在统一事务内收口：

- 单聊懒创建与成员补建
- 创建群聊与初始化成员/游标
- 发送文本/文件消息与 recipient、cursor、会话最后消息更新
- 已读推进与 recipient/cursor/member 状态同步
- 群治理动作
- 用户撤回 / 后台撤回消息与文件引用释放

WebSocket 推送仍是事务外尽力而为，不回滚数据库状态。

## 9. 当前必须保持一致的实现约束

- delivered 语义当前固定为“在线即 delivered”，不是 ACK 驱动。
- 文件消息必须复用 `file` 模块，不允许在 chat 模块直接维护文件物理生命周期。
- 群公告当前就是 `chat_conversation.remark`，后续若拆独立字段或表，必须同步迁移现有读写口径。
- 删除消息当前是“仅隐藏本人视图”，不是全局删除。
- `edited` 当前是推导字段，不是数据库持久化字段。

## 10. 下一阶段建议

建议后续按以下顺序继续推进：

1. 为编辑/撤回/群治理补充专用 WebSocket 事件
2. 扩展图片、语音、回复等更多消息类型
3. 明确后台成员管理是否需要更严格限制单聊/全站群场景
4. 把单机会话注册与推送抽象到 Redis / MQ，支持多节点部署

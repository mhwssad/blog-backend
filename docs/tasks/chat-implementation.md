# Chat 实现文档

本文档对应 2026-03-31 当前仓库中的 chat 模块实现，用于说明现状边界、关键语义和后续维护时必须保持一致的规则。

## 1. 当前实现范围

当前已经落地：

- 会话模型：单聊、群聊、全站群聊
- 消息类型：文本消息、文件消息、图片消息、语音消息
- 基础回复：消息主表持久化 `reply_message_id`，并把回复摘要快照写入消息 payload
- 富引用基础协议：`reply` 快照已补入 `state` 和 `replyToMessageId`，用于前端展示原消息状态与上一层链接
- 消息治理：编辑、撤回、仅当前用户视角删除
- 群治理：邀请、移除成员、设置/取消管理员、转让群主、禁言、群公告、退群、解散
- 后台管理：会话分页/详情、成员分页视图、消息分页、消息详情、回执明细、成员角色/状态/禁言调整、后台撤回
- 实时能力：HTTP + WebSocket，新消息、编辑、撤回、删除、会话更新、成员更新、已读推进统一通过 `ChatPushService`
  推送；当前已升级为“本地会话表 + Redis pub/sub”多节点广播
- 聊天治理：聊天域自身的用户分钟级发送频控、敏感词拦截，以及发送/媒体处理指标埋点
- 异步媒体处理：图片缩略图、语音转码预览与波形补齐已从发送链路迁移到“事务内落持久化任务 + 提交后异步抢占执行 + 定时恢复补偿”的流水线

当前仍未落地：

- 多层嵌套回复块与跨会话富引用
- 基于客户端 ACK 的 delivered 语义
- 后台专属实时审计订阅、未读/会话缓存，以及更强的聊天审核工作流

当前已完成评估但暂不优先推进：

- Redis Testcontainers 广播链路集成测试：值得做，但更适合作为后续集成测试专项，重点验证 Redis pub/sub
  与跨节点在线会话协同，而不是重做现有服务级回归
- 敏感词升级方案：短期先维持同步拦截，下一步优先补“命中留痕 + 审计记录 + 管理端可查”，再评估人工复核或外部审核服务
- 未读缓存 / 推送异步化：当前先保持数据库真值与 Redis 广播，等热点会话和连接规模明显上升后再引入缓存与削峰
- ACK 驱动 delivered：会影响 recipient/cursor/member/WebSocket 协议与多端重连补偿，需作为独立改造项推进

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
    - 附件消息：`message_type = file/image/voice`
    - 回复消息通过 `reply_message_id` 关联同一会话内可见消息
    - 扩展载荷统一走 `payload_json`
    - 撤回通过 `revoke_status / revoked_by / revoked_at` 表达
- `chat_message_recipient`
    - 每条消息会为当前活跃成员写一条 recipient 记录
    - 发送人自己的 recipient 直接写为 `已读`
    - “删除消息”当前是把当前用户对应 recipient 的 `visible_status` 改为隐藏，不做全局物理删除
- `chat_message_read_cursor`
    - 存储会话级 `read_message_id / delivered_message_id / unread_count`
    - 已读推进和拉历史消息时的 delivered 补记，最终都收口到这里

### 2.3 附件处理任务

- `chat_attachment_process_task`
    - 以 `message_id` 保证同一条附件消息只保留一条媒体处理任务
    - `task_status` 当前使用 `pending/processing/success/failed`
    - `next_retry_at + lease_expire_at` 用于多节点抢占、失败重试和节点重启后的超时恢复
    - `message_snapshot_json + push_user_ids_json` 用于在异步线程内补齐 payload 后继续回推 `message_updated`

## 3. 文件消息与 file 模块复用边界

### 3.1 当前设计

- chat 模块不自己维护文件落库、上传任务和物理对象。
- 文件上传仍然完全复用 `file` 模块现有的初始化、秒传、分片上传、引用计数和生命周期收口。
- chat 发送文件消息时，只消费 `file_business_info.id`，不直接消费原始文件路径。

### 3.2 发送文件消息时的收口流程

1. 前端先通过 `file` 模块上传文件，拿到 `businessId`。
2. chat 发送文件消息时，校验该业务引用属于当前用户，且引用类型只能是 `temp` 或尚未绑定具体消息的 `chat_message`。
3. chat 根据真实 `file_info` 生成消息摘要，并写入 `chat_message`。
    - `image/*` -> `image`
    - `audio/*` -> `voice`
    - 其他 MIME -> `file`
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

### 4.0 回复

- 当前回复能力同时保存 `reply_message_id` 和 `reply` 快照。
- 发送时要求目标消息在同一会话内对当前发送者可见；随后把发送时刻的消息摘要、发送人信息、附件快照写入当前消息 payload。
- 这样做的目的不是“替代原消息”，而是让前端在原消息后续被编辑、被撤回、被本人隐藏或暂时查不到时，仍能稳定展示引用摘要。
- 当前读取消息时会优先尝试回查仍可见的原消息；命中时直接返回原消息的实时摘要与状态，只有回查不到时才回退到 payload 中持久化的
  reply 快照。
- 当前 `reply` 额外补入：
    - `state = normal/revoked/unavailable`
    - `replyToMessageId`，表示“被引用消息自己又回复了哪条消息”，仅作为状态链接，不继续内联多层嵌套快照
- 对历史旧消息，如果 payload 中还没有 `reply` 快照，服务端会尽量回查原消息补齐；如果回查不到，则回落为 `deleted = true`
  的占位快照。
- 当前已明确不直接内联多层 `reply.reply...` 结构，避免 payload 体积和兼容复杂度失控；后续若要支持多层富引用，应单独设计前端折叠协议。

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
- 当前已补入 `message_deleted` 事件，但只推给当前用户本人，用于同步多标签页或多端的“本人视角消息隐藏”。

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
- 群解散后，用户侧不再允许继续查询该群的会话详情和历史消息；后台仍可按审计视角查询已存消息。
- 全站群走“自动补建/恢复成员资格”的消息访问路径，保证任何正常用户都能读取全站群历史；但普通群治理接口仍然只面向普通群聊，不对全站群开放。
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

后台成员管理当前只允许作用于普通群聊会话；单聊和全站群会统一拒绝。若把成员设为 `owner`，服务会同步更新
`chat_conversation.owner_id`。

## 7. WebSocket 现状

当前 WebSocket 仍只支持两类客户端请求：

- `send_message`
- `mark_read`

当前服务端主动推送当前包括：

- `message_created`
- `message_updated`
- `message_revoked`
- `message_deleted`
- `conversation_updated`
- `members_updated`
- `read_updated`

也就是说：

- 附件消息发送后仍统一走 `message_created`
- 编辑、撤回、群治理和群资料变更已具备独立 WS 事件
- 删除消息已具备专用 WS 事件，但只同步本人视角
- 客户端不能主动发送这些服务端事件类型，协议层会直接拒绝
- 后台审计当前仍优先使用 HTTP 详情、回执和分页接口，不额外拆后台专属实时推送
- 会话注册表仍只维护当前节点在线连接；跨节点广播通过 Redis topic `chat:ws:push` 完成

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
- `client_message_id` 在发送人维度保持唯一；若并发重试命中唯一键冲突，服务层会回查并返回已存在消息，避免把数据库异常暴露给上层。
- delivered 相关的 `chat_message_read_cursor.delivered_message_id` 与
  `chat_conversation_member.last_delivered_message_id` 当前都按“只能前进不能回退”收口，避免并发旧事务覆盖新高水位。
- 文件消息必须复用 `file` 模块，不允许在 chat 模块直接维护文件物理生命周期。
- 群公告当前就是 `chat_conversation.remark`，后续若拆独立字段或表，必须同步迁移现有读写口径。
- 删除消息当前是“仅隐藏本人视图”，不是全局删除。
- `edited` 当前是推导字段，不是数据库持久化字段。
- 回复消息当前会持久化一份单层摘要快照，并通过 `reply.state`、`reply.replyToMessageId` 表达 richer 引用状态，但还不支持多层嵌套引用块。
- 图片 / 语音附件当前已进入“持久化异步媒体任务版”：发送时先落 `chat_attachment_process_task`
  ，提交后异步抢占执行；失败按指数退避重试，处理中租约超时后会被调度器恢复，再异步生成图片缩略图、语音 WAV 预览、时长和波形，并通过
  `message_updated` 回推。
- 多节点实时推送当前采用 Redis pub/sub，未读数缓存、会话列表缓存和推送异步化暂未启用，优先保持一致性简单可控。
- 聊天域当前已补入用户级分钟频控、敏感词拦截和 Micrometer 指标；更复杂的人工审核、审核结果订阅和风控画像仍未落地。
- Redis Testcontainers、审核升级、未读缓存/推送异步化，以及 ACK 驱动 delivered 当前都已完成方向评估，但尚未进入实现阶段。

## 10. 下一阶段建议

建议后续按以下顺序继续推进：

1. 继续评估是否需要真正的多层富引用块、回复折叠和跨消息跳转协议
2. 继续补齐更贴近真实部署的 Redis/Testcontainers 广播链路集成验证
3. 继续把聊天审核从敏感词拦截扩展到命中审计、人工复核或外部审核服务
4. 当热点会话、连接规模或送达语义要求继续提升后，再推进未读缓存、推送异步化和 ACK 驱动 delivered



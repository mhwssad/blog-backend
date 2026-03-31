# Chat 模块待办清单

本文档用于收口 chat 模块接下来要持续推进的任务，避免"知道有缺口，但每次都重新梳理一遍"。本清单按 2026-03-31 当前代码状态整理。

## 1. 模块结构概览

```
module/chat/
├── config/              (Redis推送订阅配置)
├── constant/            (ChatConstants: 会话/角色/消息类型/状态常量)
├── controller/
│   ├── UserChatController.java      (22个端点: 会话/消息/群管理/成员)
│   └── ChatAdminController.java     (11个端点: 后台会话/消息/成员/状态管理)
├── convert/             (ChatModelMapper MapStruct映射器)
├── model/
│   ├── data/            (4个Mapper结果对象: 会话列表项/管理会话项/消息历史项/管理消息项)
│   ├── user/            (18个DTO: 请求/响应/分页查询)
│   ├── admin/           (11个DTO: 管理侧请求/响应)
│   ├── common/          (3个共享DTO: payload/文件/回复快照)
│   ├── websocket/       (9个WS协议模型: 类型枚举/请求/响应/各类payload)
│   └── internal/        (1个内部模型: Redis推送信封)
├── service/
│   ├── UserChatService.java           (24个方法: 会话/消息/群治理)
│   ├── ChatAdminService.java          (11个方法: 后台管理)
│   ├── ChatPushService.java           (7个方法: 实时推送+Redis广播)
│   ├── ChatConversationService.java   (基础仓储, extends IService)
│   ├── ChatConversationMemberService  (基础仓储, extends IService)
│   ├── ChatMessageService.java        (基础仓储, extends IService)
│   ├── ChatMessageRecipientService    (基础仓储, extends IService)
│   ├── ChatMessageReadCursorService   (基础仓储, extends IService)
│   ├── ChatWebSocketSessionRegistry   (5个方法: 会话注册/查找/在线数)
│   ├── ChatAttachmentMetadataResolver (1个方法: 附件元数据抽取)
│   ├── ChatAttachmentAsyncProcessing  (3个方法: 提交后调度/到期派发/租约恢复)
│   ├── ChatMessageGovernanceService   (2个方法: 频控+敏感词)
│   └── ChatMetricsService             (2个方法: Micrometer指标)
├── impl/                (14个实现: UserChatServiceImpl~1629行/ChatAdminServiceImpl~825行/其他)
└── websocket/
    ├── ChatWebSocketHandler.java      (连接生命周期+消息分发)
    └── ChatWebSocketMessageCodec.java (协议编解码)
```

数据库: 6张表 (`chat_conversation` / `chat_conversation_member` / `chat_message` / `chat_message_recipient` / `chat_message_read_cursor` / `chat_attachment_process_task`)

## 2. 功能完成度评估

### 2.1 接口方法完成情况

| 服务                           | 方法数  | 已实现 | 状态                    |
| ------------------------------ | ------- | ------ | ----------------------- |
| UserChatService                | 24      | 24     | ✅ 全部完成             |
| ChatAdminService               | 11      | 11     | ✅ 全部完成             |
| ChatPushService                | 7       | 7      | ✅ 全部完成             |
| ChatWebSocketSessionRegistry   | 5       | 5      | ✅ 全部完成             |
| ChatAttachmentMetadataResolver | 1       | 1      | ✅ 全部完成             |
| ChatAttachmentAsyncProcessing  | 1       | 1      | ✅ 全部完成             |
| ChatMessageGovernanceService   | 2       | 2      | ✅ 全部完成             |
| ChatMetricsService             | 2       | 2      | ✅ 全部完成             |
| 基础仓储(5个)                  | 0自定义 | 全部   | ✅ MyBatis-Plus标准CRUD |

**结论: chat 模块所有接口方法均已完整实现，不存在缺失的方法。**

### 2.2 WebSocket协议完成情况

| 方向          | 类型                                                                                                                                        | 状态    |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 客户端→服务端 | `send_message` / `mark_read` / `ping`                                                                                                       | ✅ 完成 |
| 服务端→客户端 | `message_created` / `message_updated` / `message_revoked` / `message_deleted` / `read_updated` / `conversation_updated` / `members_updated` | ✅ 完成 |
| 多节点广播    | Redis pub/sub + 本地会话表                                                                                                                  | ✅ 完成 |

## 3. 本轮已完成

- [x] 实现用户侧聊天完整链路（会话列表/单聊懒创建/消息收发/已读/群管理）。
- [x] 实现后台聊天管理（会话/消息/成员/回执/撤回/状态管理）。
- [x] 实现 WebSocket 实时通道（send_message/mark_read + 9种服务端推送事件）。
- [x] 实现 Redis pub/sub 多节点广播。
- [x] 实现文件消息（复用file模块上传+生命周期）。
- [x] 实现消息编辑/撤回/删除（本人视角）。
- [x] 实现群治理（管理员/转让群主/禁言/群公告/邀请/移除/退群/解散）。
- [x] 实现 reply 快照持久化（replyMessageId + reply摘要 + state/replyToMessageId）。
- [x] 实现附件 MIME 自动分类（image/voice/file）。
- [x] 实现异步媒体处理（缩略图/语音转码/波形）+ message_updated 回推。
- [x] 实现聊天域频控（用户分钟级发送限制）+ 敏感词拦截。
- [x] 实现 Micrometer 指标埋点（发送/媒体处理/治理拒绝）。
- [x] 补入用户侧/后台控制器鉴权测试（19个权限场景）。
- [x] 补入 ChatWebSocketHandler 协议回归测试（25个场景）。
- [x] 补入 UserChatServiceImpl 服务级测试（28个场景）。
- [x] 补入 ChatAdminServiceImpl 服务级测试（18个场景）。
- [x] 补入 ChatPushServiceImpl + ChatPushRedisSubscriber 测试。
- [x] 补入 ChatAttachmentMetadataResolverImpl + ChatAttachmentAsyncProcessingServiceImpl 测试。
- [x] 补入 ChatMessageGovernanceServiceImpl 测试。
- [x] 继续补入 UserChatServiceImpl 高优先级空白方法测试（会话详情/群详情/群成员/管理员任免/转让群主/禁言）。
- [x] 继续补入 UserChatServiceImpl 补充场景测试（非成员发言拦截、建群 owner、全新成员邀请、普通会话分页、编辑成功路径）。
- [x] 继续补入 ChatAdminServiceImpl 成员状态/禁言正常路径测试。
- [x] 继续补入 ChatPushServiceImpl 全量推送方法测试。
- [x] 继续补入 ChatWebSocketSessionRegistryImpl 会话注册表测试。
- [x] 继续补入 ChatMessageGovernanceServiceImpl 正常通过路径测试。

## 4. 现有测试文件 (10个)

| 测试文件                                       | 测试方法数 | 覆盖范围                                                               |
| ---------------------------------------------- | ---------- | ---------------------------------------------------------------------- |
| `ChatWebSocketHandlerTest`                     | 25         | 连接生命周期/ping/协议分发/拒绝服务端类型/send_message/mark_read全分支 |
| `ChatControllerSecurityTest`                   | 19         | 用户端登录要求/后台权限拦截(11个场景)                                  |
| `UserChatServiceImplTest`                      | 53         | 会话详情/文本发送/文件发送/已读/群创建/群治理/编辑/撤回/删除/分页边界  |
| `ChatAdminServiceImplTest`                     | 33         | 会话分页/消息分页/详情/回执/成员治理/撤回/状态切换                     |
| `ChatPushServiceImplTest`                      | 10         | 本地推送+Redis广播/全量推送方法/集群事件处理/未知类型忽略              |
| `ChatPushRedisSubscriberTest`                  | 2          | 有效payload转发/无效JSON忽略                                           |
| `ChatAttachmentMetadataResolverImplTest`       | 2          | 图片尺寸/WAV时长+波形                                                  |
| `ChatAttachmentAsyncProcessingServiceImplTest` | 5          | 持久化任务落库/图片缩略图/语音转码/legacy兼容/失败重试                |
| `ChatMessageGovernanceServiceImplTest`         | 3          | 敏感词拦截/频控拦截/正常通过路径                                       |
| `ChatWebSocketSessionRegistryImplTest`         | 3          | 会话注册/注销/数字型用户ID解析/在线数统计                              |

## 5. 下一批高优先级

### 5.1 UserChatService 未覆盖方法 (已清空)

- [x] `getMyConversation` - 获取单个会话详情（成员/游标/最后消息装配）
- [x] `getGroupDetail` - 获取群聊详情（群信息+自身角色校验）
- [x] `listGroupMembers` - 列出群成员列表
- [x] `appointGroupAdmin` - 设置管理员（角色升级+推送members_updated）
- [x] `removeGroupAdmin` - 取消管理员（角色降级+推送members_updated）
- [x] `transferGroupOwner` - 转让群主（原群主降级+目标升级+推送）
- [x] `muteGroupMember` - 禁言成员（muteUntil设置+推送members_updated）

### 5.2 已有方法的补充场景

- [x] `sendTextMessage` - 非群成员发送拦截（当前群类型校验）
- [x] `createGroup` - 建群者自动成为owner的验证
- [x] `inviteGroupMembers` - 全新成员（非恢复）邀请路径
- [x] `pageMyConversations` - 正常分页查询（非全站群场景）
- [x] `editMessage` - 正常编辑成功路径（内容更新+推送）

### 5.3 ChatAdminService 补充场景

- [x] `updateMemberStatus` - 正常路径（禁用/启用成员）
- [x] `updateMemberMute` - 正常路径（设置/取消禁言）
- [x] `listMembers` - 更多排序稳定性场景

### 5.4 推送服务补充

- [x] `ChatPushServiceImpl` - `pushMessageUpdated` / `pushMessageRevoked` / `pushMessageDeleted` / `pushReadUpdated` / `pushConversationUpdated` / `pushMembersUpdated` 各推送方法验证
- [x] `ChatMessageGovernanceServiceImpl` - 正常通过路径（无敏感词+未超频）
- [x] `ChatWebSocketSessionRegistryImpl` - 会话注册/注销/查找/在线数统计

## 6. 中期一致性补强

- [x] 核对群解散后消息可见性规则（当前用户侧会话详情/历史消息不可继续查询，后台仍可审计已存消息）
- [x] 核对全站群与普通群在成员管理/消息可见性上的差异一致性（全站群会自动恢复成员资格以读取消息，但不支持普通群治理接口）
- [x] 核对 reply 快照在原消息被编辑/撤回后的状态同步（当前可见原消息优先返回实时状态，不可见时再回退 payload 快照）
- [x] 核对并发发送同一 clientMessageId 的幂等行为（保存阶段唯一键冲突会回查并返回既有消息）
- [x] 补充高成本方法的 Javadoc 注释（已补 `UserChatServiceImpl` 中已读推进、reply可见性校验、reply快照装配）
- [x] 核对 delivered 语义（在线即delivered）在高并发下的游标一致性（cursor/member delivered 高水位改为单调推进）

## 7. 中长期基础设施

- [x] 将异步媒体处理从单机线程池升级为持久化任务（节点重启不丢任务）
- [x] 评估更完整的 Redis Testcontainers 广播链路集成测试（结论：值得做，但应作为后续集成测试专项，优先验证 Redis pub/sub 广播、跨节点会话注册表协同，以及消息更新/已读事件跨节点送达）
- [x] 评估从敏感词拦截升级到命中审计/人工复核/外部审核服务（结论：v1 先保持同步拦截；下一阶段优先补“命中留痕 + 审计记录 + 管理端查询”，再视合规要求接人工复核或外部审核）
- [x] 当会话量和在线连接数显著增长后，评估未读数缓存/推送异步化（结论：当前先保持 DB 真值 + Redis 广播；当热点会话、未读聚合查询或广播风暴明显出现后，再按“未读缓存 + 推送削峰队列”拆分）
- [x] 评估 delivered 从"在线即delivered"改为ACK驱动的改造影响（结论：会同步影响 recipient/cursor/member 高水位、WebSocket 协议、多端重连补 ACK 和消息状态回放；当前维持在线即 delivered，若业务要求更强送达语义再单独立项）

## 8. 完成标志

- 用户侧会话/消息/群治理全链路都具备服务级回归覆盖。
- 后台管理操作都具备基础自动化验证。
- WebSocket 协议分发与推送都具备回归路径。
- 聊天域频控/敏感词/指标都具备验证。

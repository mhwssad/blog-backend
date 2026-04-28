# 数据库结构与业务实现对齐任务清单

本文档用于跟踪“需求文档中已补入原始建表脚本的结构”与后续业务代码的对齐工作。当前项目处于开发阶段，数据库调整默认直接维护原始建表脚本。

## 1. 任务来源

- `docs/需求文档/数据库表优化方案.md`
- `docs/需求文档/项目需求草案-PRD.md`
- `docs/需求文档/项目执行任务清单.md`
- `docs/项目代码编写规范.md`

## 2. 当前状态

**当前阶段：进行中。已完成 `sys_author_application`、`sys_user` 等级字段、`sys_user_notification_setting`、博客内容侧结构承接、聊天侧结构对齐、AI 侧结构对齐、举报治理侧结构对齐及权限菜单补全，其余结构继续按任务分批推进。**

```
已具备:
  ✅ 1.sys.sql 已补用户等级、作者申请、通知偏好、AI、举报治理结构
  ✅ 02_article.sql 已补文章审核、可见范围、定时发布、系列结构
  ✅ 05_chat.sql 已补频道 / 群治理、申请、入群、论坛挂接结构
  ✅ 开发阶段直接修改原始建表脚本的规范已明确
  ✅ 系列文章 Repository 已补齐
  ✅ ChatConversation 11 个频道治理字段已补齐
  ✅ 频道创建申请 / 入群申请 / 论坛关联 domain/mapper/repository 已建
  ✅ AI 渠道配置 / 对话会话 / 消息 / 使用日志 domain/mapper/repository 已建
  ✅ 举报记录 / 处理日志 domain/mapper/repository 已建
  ✅ 系列文章 / 频道申请 / 入群申请 / AI / 举报 / 看板 / 审计菜单已补

待推进:
  ⏳ 清理重复、废弃、同义结构
  ⏳ AI 额度等业务配置继续补齐
```

## 3. 系统与权限侧结构

### 3.1 `sys_user` 等级字段

- [x] domain 补齐 `userLevel`、`experiencePoints`、`levelUpdatedAt`。
- [x] 用户查询 VO 返回等级信息。
- [x] 后台用户详情返回等级与经验。
- [x] 更新用户时避免误覆盖经验字段。
- [x] 补齐等级字段默认值测试。

### 3.2 `sys_author_application`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 新增用户侧和后台侧 DTO / VO。
- [x] 新增申请状态枚举。
- [x] 补齐唯一性和状态索引使用场景。

### 3.3 `sys_user_notification_setting`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 新增通知类型枚举。
- [x] 新用户默认配置生成逻辑。
- [x] 投递前偏好查询方法。

## 4. 博客内容侧结构

### 4.1 `blog_article` 新字段

- [x] domain 补齐 `reviewStatus`。
- [x] domain 补齐 `visibilityScope`。
- [x] domain 补齐 `scheduledPublishTime`。
- [x] 保存和更新文章时显式处理新字段。
- [x] 公开查询 SQL 加入审核状态和可见范围过滤。
- [x] 文章详情访问控制使用新字段。

### 4.2 `blog_article_review_log`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 审核动作统一写日志。
- [x] 后台查询审核日志接口。

### 4.3 `blog_article_series`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 系列创建、编辑、删除业务服务。
- [x] 公开系列查询 SQL。

### 4.4 `blog_article_series_item`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 系列文章排序方法。
- [x] 同一系列文章唯一约束校验。

## 5. 聊天与频道侧结构

### 5.1 `chat_conversation` 新字段

- [x] domain 补齐 `sceneType`。
- [x] domain 补齐 `visibilityScope`。
- [x] domain 补齐 `allowGuestView`。
- [x] domain 补齐 `requireJoinToSpeak`。
- [x] domain 补齐 `joinRule`。
- [x] domain 补齐 `speakLevelLimit`。
- [x] domain 补齐 `memberLimit`。
- [x] domain 补齐 `announcement`。
- [x] domain 补齐 `slowModeSeconds`。
- [x] domain 补齐 `displaySort`。
- [x] domain 补齐 `channelCategoryCode`。
- [x] 会话列表和详情 VO 按需返回新增字段。

### 5.2 `chat_channel_create_application`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 新增申请状态枚举。
- [x] 新增用户申请和后台审批服务。

### 5.3 `chat_group_join_application`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 新增入群申请状态枚举。
- [x] 新增主动申请、邀请审批、审批通过入群服务。

### 5.4 `chat_group_invite_link`

- [x] 原始建表脚本新增邀请链接表。
- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 新增邀请链接创建、停用、过期和次数限制入群服务。

### 5.5 `forum_post_channel_link`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [ ] 第一阶段只提供手动挂接能力。
- [ ] 不引入论坛正式模块时，可先保留轻量接口或等待论坛立项。

## 6. AI 侧结构

### 6.1 `ai_channel_config`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [ ] API Key 脱敏和加密 / 解密策略。
- [ ] 高风险配置审计字段对齐。

### 6.2 `ai_chat_session`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 会话归属和状态查询方法。

### 6.3 `ai_chat_message`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 按会话分页查询消息。
- [ ] 支持调用失败消息落库。

### 6.4 `ai_usage_log`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 支持按用户、日期、渠道统计调用次数。
- [ ] 支持后台调用统计查询。

## 7. 举报治理侧结构

### 7.1 `sys_report_record`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 支持按状态、对象类型、举报人、时间分页。
- [ ] 支持状态流转更新。

### 7.2 `sys_report_handle_log`

- [x] 新增 domain。
- [x] 新增 Mapper / XML。
- [x] 新增 Repository。
- [x] 支持按举报单查询日志。
- [ ] 每次处理动作追加日志。

## 8. 权限菜单与初始化数据

- [x] 作者申请后台菜单。
- [x] 用户等级后台菜单。
- [x] 文章审核后台菜单。
- [x] 系列文章管理菜单。
- [x] 频道管理菜单。
- [x] 频道申请审核菜单。
- [x] 群入群申请管理菜单。
- [x] AI 配置菜单。
- [x] AI 调用统计菜单。
- [x] 举报处理菜单。
- [x] 数据看板菜单。
- [x] 高风险审计查询菜单。

## 9. 清爽性检查

- [ ] 不新增与现有表语义重复的平行结构。
- [ ] 已明确废弃的字段、表、文档和任务清单及时删除。
- [ ] 新增枚举只保留当前阶段会使用的值。
- [ ] 初始化脚本只保留当前实际执行入口。
- [ ] 文档中不保留“多年历史兼容”的无效说明。

## 10. 验证方式

- [x] 执行 `mvn -q -DskipTests compile`。
- [ ] 新增表对应 Repository 基础测试或服务级测试。
- [ ] 对新增字段涉及的查询补服务级回归。
- [ ] 对权限菜单初始化脚本做人工核对或脚本级校验。

## 11. 完成标志

- 需求文档中列出的新表和新字段均已有代码模型承接。
- 新增业务不绕过 Repository 直接拼装数据访问。
- 枚举、状态流转、接口文档和数据库脚本一致。
- 初始化脚本、权限菜单和项目文档保持清爽。

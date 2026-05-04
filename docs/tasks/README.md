# 任务执行清单导航

本文档用于统一收口 `docs/tasks` 下的执行任务清单。任务来源于 `docs/需求文档`，每份清单按模块或能力主题拆分，便于后续按批次开发、验收和回写进度。

## 1. 当前执行清单

| 文件 | 对应需求 | 当前状态 | 优先级 |
| --- | --- | --- | --- |
| [01-author-level-permission-todo.md](01-author-level-permission-todo.md) | 作者申请、用户等级、超级管理员安全 | 业务功能+测试已完成 | P0 |
| [02-blog-content-lifecycle-todo.md](02-blog-content-lifecycle-todo.md) | 文章状态、可见范围、审核、系列文章 | 业务功能+测试已完成 | P0 |
| [03-chat-community-todo.md](03-chat-community-todo.md) | 大厅频道、主题频道、群聊增强、论坛挂接 | 业务功能+测试已完成 | P0 |
| [04-ai-module-todo.md](04-ai-module-todo.md) | AI 问答、配置中心、额度、数据范围 | 业务功能+测试已完成 | P0 |
| [05-governance-report-audit-todo.md](05-governance-report-audit-todo.md) | 举报处理、治理动作、审计增强 | 业务功能+测试已完成 | P0 |
| [06-notification-dashboard-todo.md](06-notification-dashboard-todo.md) | 通知设置、通知投递过滤、数据看板 | 业务功能+测试已完成 | P1 |
| [07-database-schema-alignment-todo.md](07-database-schema-alignment-todo.md) | 已补表结构与业务实现对齐 | 业务功能+清爽性检查已完成，测试补强转入二期 | P0 |
| [08-source-structure-optimization-todo.md](08-source-structure-optimization-todo.md) | 源码结构治理、边界收口、重型类拆分与子域化 | 全部完成 | P1 |
| [09-code-review-fixes-todo.md](09-code-review-fixes-todo.md) | 全项目代码审查整改（安全/数据一致性/性能/测试） | 第三阶段完成，第四阶段后转入二期按需推进 | P1 |
| [10-phase2-roadmap-todo.md](10-phase2-roadmap-todo.md) | 第二期主线与第一期遗留增强 | 待开始 | P0 |

## 2. 阶段结论

第一期后端主线已达到可进入第二期的状态：

- 用户注册登录、权限、作者、等级、超级管理员安全已成基线。
- 博客内容生命周期、审核、可见范围、系列文章、附件引用已成基线。
- 大厅、主题频道、群聊增强、频道与帖子轻量挂接已成基线。
- AI 基础问答、后台配置、额度、统计、数据范围已成基线。
- 举报治理、通知偏好、后台看板、数据库结构和源码结构已完成当前阶段收口。

第一期遗留但不阻塞第二期的内容，统一进入 [10-phase2-roadmap-todo.md](10-phase2-roadmap-todo.md)，包括全站禁言、举报通知、AI 任务完成通知、RAG、agents、论坛正式模块、外部博客迁移、性能优化和测试补强。

## 3. 参考文档

| 文件 | 用途 | 当前状态 |
| --- | --- | --- |
| [chat-implementation.md](chat-implementation.md) | 记录 chat 当前实现边界、关键语义和后续维护约束 | 参考文档 |
| [follow-module-todo.md](follow-module-todo.md) | 跟踪关注模块主页联动、缓存和集成验证等后续评估项 | 持续评估 |

## 4. 推荐执行顺序

1. 第二期优先从 `10-phase2-roadmap-todo.md` 中选择一条主线推进：论坛正式模块、知识库 / RAG、agents、外部博客迁移。
2. 与主线直接相关的遗留增强同步处理；无关性能、测试、代码质量项拆成独立小提交。
3. 代码审查整改的第四阶段及后续项，不再作为第一期阻塞项，按第二期风险和使用场景穿插推进。
4. 参考文档 `chat-implementation.md`、`follow-module-todo.md` 只在改动对应模块时同步更新。

## 5. 通用完成标准

- 表结构、枚举和状态定义已与原始建表脚本一致。
- Controller、Service、Repository / Mapper 已按项目结构规范落位。
- 新增或修改 Service 方法时应补充对应服务级测试；若因阶段拆分暂缓，必须在任务清单中明确归入二期测试补强。
- 前端可见接口已同步更新 `docs/api文档`。
- 影响项目阶段判断的变更已同步更新 `docs/tasks/README.md` 或对应任务清单。

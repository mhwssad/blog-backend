# 任务执行清单导航

本文档用于统一收口 `docs/tasks` 下的执行任务清单。任务来源于 `docs/需求文档`，每份清单按模块或能力主题拆分，便于后续按批次开发、验收和回写进度。

## 1. 当前执行清单

| 文件 | 对应需求 | 当前状态 | 优先级 |
| --- | --- | --- | --- |
| [01-author-level-permission-todo.md](01-author-level-permission-todo.md) | 作者申请、用户等级、超级管理员安全 | 待开始 | P0 |
| [02-blog-content-lifecycle-todo.md](02-blog-content-lifecycle-todo.md) | 文章状态、可见范围、审核、系列文章 | 待开始 | P0 |
| [03-chat-community-todo.md](03-chat-community-todo.md) | 大厅频道、主题频道、群聊增强、论坛挂接 | 待开始 | P0 |
| [04-ai-module-todo.md](04-ai-module-todo.md) | AI 问答、配置中心、额度、数据范围 | 待开始 | P0 |
| [05-governance-report-audit-todo.md](05-governance-report-audit-todo.md) | 举报处理、治理动作、审计增强 | 待开始 | P0 |
| [06-notification-dashboard-todo.md](06-notification-dashboard-todo.md) | 通知设置、通知投递过滤、数据看板 | 待开始 | P1 |
| [07-database-schema-alignment-todo.md](07-database-schema-alignment-todo.md) | 已补表结构与业务实现对齐 | 待开始 | P0 |

## 2. 参考文档

| 文件 | 用途 | 当前状态 |
| --- | --- | --- |
| [chat-implementation.md](chat-implementation.md) | 记录 chat 当前实现边界、关键语义和后续维护约束 | 参考文档 |
| [follow-module-todo.md](follow-module-todo.md) | 跟踪关注模块主页联动、缓存和集成验证等后续评估项 | 持续评估 |

## 3. 推荐执行顺序

1. 先执行 `01-author-level-permission-todo.md` 和 `07-database-schema-alignment-todo.md`，把权限、等级、作者身份和数据库落位口径收稳。
2. 再执行 `02-blog-content-lifecycle-todo.md`，完成博客主线的状态、审核和系列能力。
3. 接着执行 `03-chat-community-todo.md`，把大厅、主题频道和群聊规则补齐。
4. 然后执行 `04-ai-module-todo.md` 和 `05-governance-report-audit-todo.md`，形成 AI 基础问答和平台治理闭环。
5. 最后执行 `06-notification-dashboard-todo.md`，补齐通知偏好和后台统计看板。

## 4. 通用完成标准

- 表结构、枚举和状态定义已与原始建表脚本一致。
- Controller、Service、Repository / Mapper 已按项目结构规范落位。
- 核心正常路径、边界条件和异常分支有服务级测试。
- 涉及权限的接口有必要的 WebMvc / 权限测试。
- 前端可见接口已同步更新 `docs/api文档`。
- 影响项目阶段判断的变更已同步更新 `docs/项目进度文档.md`。

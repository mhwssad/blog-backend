# 第二期任务清单导航

本文档是 `docs/tasks` 的当前入口。第一期任务清单已归档移除，后续研发从第二期重新规划，不再按一期 `01` 到 `09` 的执行文档推进。

## 1. 阶段基线

第二期默认以下能力已经作为后端基线存在：

- 账号认证、RBAC、作者申请、等级经验、超级管理员安全。
- 博客文章生命周期、审核、可见范围、系列文章、评论、点赞、收藏和附件引用。
- 私聊、群聊、大厅频道、主题频道、频道申请、群申请、邀请链接、频道与帖子轻量挂接。
- AI 基础问答、渠道配置、额度控制、调用统计和数据读取范围控制。
- 举报处理、会话级禁言、账号封禁、审计日志、通知偏好和后台首批数据看板。

第二期不再重复拆解上述一期能力，只在新增需求直接依赖时做局部补强。

## 2. 当前二期任务

| 文件                                                                                             | 主题                     | 当前状态            | 优先级 |
| ------------------------------------------------------------------------------------------------ | ------------------------ | ------------------- | ------ |
| [01-forum-module-todo.md](01-forum-module-todo.md)                                               | 论坛正式模块             | P0 已完成           | P0     |
| [02-ai-rag-agents-todo.md](02-ai-rag-agents-todo.md)                                             | 知识库 / RAG / agents    | P0 知识源边界已完成 | P0     |
| [03-migration-user-service-todo.md](03-migration-user-service-todo.md)                           | 外部博客迁移与用户自服务 | 待开始              | P1     |
| [04-governance-notification-enhancement-todo.md](04-governance-notification-enhancement-todo.md) | 治理、通知和后台运营增强 | 待开始              | P1     |
| [05-performance-test-quality-todo.md](05-performance-test-quality-todo.md)                       | 性能、测试和代码质量补强 | 待开始              | P2     |
| [06-mysql-to-postgresql-migration-todo.md](06-mysql-to-postgresql-migration-todo.md)             | MySQL 迁移 PostgreSQL    | 计划中              | P0     |

## 3. 推荐执行顺序

1. 优先推进 [01-forum-module-todo.md](01-forum-module-todo.md)，把社区沉淀能力补上，并承接第一期频道挂接基础。
2. 并行设计 [02-ai-rag-agents-todo.md](02-ai-rag-agents-todo.md)，但先完成知识源边界和数据权限，再进入 RAG / agents 实现。
3. 如果确认 RAG 后续使用 `pgvector`，优先执行 [06-mysql-to-postgresql-migration-todo.md](06-mysql-to-postgresql-migration-todo.md)，先完成 PostgreSQL profile、脚本和核心链路验证。
4. 根据前端联调需要推进 [03-migration-user-service-todo.md](03-migration-user-service-todo.md) 中的用户自服务接口；外部博客迁移在博客结构稳定后推进。
5. 治理通知增强和性能测试质量任务按风险穿插，不与主功能混在同一提交。

## 4. 二期完成标准

- 至少一个二期主线完成端到端业务闭环。
- 前端可见接口已同步更新 `docs/api文档`。
- 新增或修改 Service 方法已有服务级测试，关键权限入口有权限测试。
- 数据结构变更已同步原始建表脚本和相关需求文档。
- 任务状态已回写到本目录对应任务清单。

## 5. 当前推进记录

- 论坛 P0 已完成公开侧和用户侧主链路：版块、帖子、回复、点赞、收藏、频道分享。
- 论坛 P0 API 文档已新增 `docs/api文档/forum-api.md`，并已挂入 API 导航。
- 论坛 P0 服务级测试已覆盖公开查询、用户行为和频道分享校验。
- AI 知识库 P0 知识源边界已完成：知识源类型枚举、3 张表结构（ai_knowledge_source_config / ai_knowledge_entry / ai_knowledge_sync_task）、后台配置管理、条目管理和同步任务管理接口。
- 已新增 MySQL 迁移 PostgreSQL 计划：`06-mysql-to-postgresql-migration-todo.md`。
- RAG 检索增强暂缓，优先推进 `02-ai-rag-agents-todo.md` 的 P1 Agents 实现。RAG 不阻塞 agents、治理通知、迁移等其他二期任务。
- 下一步推进 agents 定义与任务执行，同步可根据前端联调需要推进 `03-migration-user-service-todo.md` 用户自服务接口。

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
| [01-forum-module-todo.md](01-forum-module-todo.md)                                               | 论坛正式模块             | P1 后台版块管理已完成 | P0     |
| [02-ai-rag-agents-todo.md](02-ai-rag-agents-todo.md)                                             | 知识库 / RAG / agents / 工具调用 / MCP | P1 Agents 已完成，工具调用与 MCP 待规划 | P0     |
| [03-migration-user-service-todo.md](03-migration-user-service-todo.md)                           | 外部博客迁移与用户自服务 | P0 用户自服务已完成 | P1     |
| [04-governance-notification-enhancement-todo.md](04-governance-notification-enhancement-todo.md) | 治理、通知和后台运营增强 | P1 运营看板增强已完成 | P1     |
| [05-performance-test-quality-todo.md](05-performance-test-quality-todo.md)                       | 性能、测试和代码质量补强 | 待开始              | P2     |

## 3. 推荐执行顺序

1. 优先推进 [01-forum-module-todo.md](01-forum-module-todo.md)，把社区沉淀能力补上，并承接第一期频道挂接基础。
2. 并行设计 [02-ai-rag-agents-todo.md](02-ai-rag-agents-todo.md)，但先完成知识源边界和数据权限，再进入 RAG / agents 实现。
3. 根据前端联调需要推进 [03-migration-user-service-todo.md](03-migration-user-service-todo.md) 中的用户自服务接口；外部博客迁移在博客结构稳定后推进。
4. 治理通知增强和性能测试质量任务按风险穿插，不与主功能混在同一提交。

## 4. 二期完成标准

- 至少一个二期主线完成端到端业务闭环。
- 前端可见接口已同步更新 `docs/api文档`。
- 新增或修改 Service 方法已有服务级测试，关键权限入口有权限测试。
- 当前业务快速落地阶段可先提交具体业务实现，但必须在对应任务文档记录测试后置项，后续集中补强。
- 数据结构变更已同步原始建表脚本和相关需求文档。
- 任务状态已回写到本目录对应任务清单。

## 5. 当前推进记录

- 论坛 P0 已完成公开侧和用户侧主链路：版块、帖子、回复、点赞、收藏、频道分享。
- 论坛 P0 API 文档已新增 `docs/api文档/forum-api.md`，并已挂入 API 导航。
- 论坛 P0 服务级测试已覆盖公开查询、用户行为和频道分享校验。
- 论坛 P1 后台版块管理已完成：新增 `/api/sys/forum/sections` 后台分页、详情、新增、修改、状态切换和删除空版块接口，权限为 `content:forum:*`，并同步后台菜单权限初始化脚本和 API 文档。
- 当前推进策略：先落具体业务代码，测试补强后置并回写任务清单；下一步继续按推荐顺序推进论坛后台帖子 / 回复治理或补后台版块测试。
- AI 知识库 P0 知识源边界已完成：知识源类型枚举、3 张表结构（ai_knowledge_source_config / ai_knowledge_entry / ai_knowledge_sync_task）、后台配置管理、条目管理和同步任务管理接口。
- RAG 检索增强暂缓，优先推进 `02-ai-rag-agents-todo.md` 的 P1 Agents 实现。RAG 不阻塞 agents、治理通知、迁移等其他二期任务。
- AI Agents P1 已完成：3 张表结构（ai_agent_definition / ai_agent_task / ai_agent_task_log）、Agent 定义后台管理、用户任务发起/查询/取消、后台任务管理、任务执行流程（额度校验→模型调用→日志记录→通知投递）。
- AI 新增工具调用与 MCP 接入需求：后续需补齐工具定义、授权范围、调用执行、MCP 服务配置、工具发现、连接状态和调用审计，且不得绕过 AI 数据范围和后台治理边界。
- 用户自服务 P0 已完成：个人资料查看/更新（新增 bio/website 字段）、修改密码（验证旧密码）、找回密码（邮箱验证码）、用户搜索（公开接口）。
- 用户自服务测试补强已推进：已补齐个人资料查看/更新服务级测试，覆盖联系方式脱敏、MapStruct 更新委托和用户不存在异常。
- 治理通知增强 P0 禁言体系已完成：统一禁言记录表、4 种范围（全站/大厅/主题频道/群聊）、发送拦截接入、举报联动（可选禁言范围和时长）、后台管理接口。
- 下一步可推进 `04-governance-notification-enhancement-todo.md` 剩余的举报通知、AI/Agent 通知，或根据前端联调需要推进其他任务。
- 治理通知增强 P0 举报通知已完成：举报处理（通过/驳回）后自动通知举报人，新增 `REPORT_RESULT` 通知类型和偏好开关，通知内容按结果类型区分模板且不泄露处理人信息。
- 下一步可推进 `04-governance-notification-enhancement-todo.md` 剩余的 AI/Agent 通知、运营看板增强，或根据前端联调需要推进其他任务。
- 治理通知增强 P1 运营看板增强已完成：社区统计增加论坛发帖数、回复数和热门版块，AI 统计增加 RAG 调用数和 Agent 任务数，治理统计增加举报处理耗时和处罚分布，并支持当前时间范围 Excel 导出。
- 下一步可推进 `04-governance-notification-enhancement-todo.md` 剩余的禁言测试/API 文档补强，或穿插 `05-performance-test-quality-todo.md` 的质量任务。

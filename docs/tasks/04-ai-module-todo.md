# AI 模块任务清单

本文档用于收口 AI 第一阶段能力：统一问答入口、对话记录、模型调用、后台配置、额度控制和数据读取范围。

## 1. 任务来源

- `docs/需求文档/AI模块-PRD.md`
- `docs/需求文档/项目需求草案-PRD.md`
- `docs/需求文档/项目执行任务清单.md`
- `docs/需求文档/数据库表优化方案.md`
- `docs/需求文档/用户等级体系设计草案.md`

## 2. 当前状态

**当前阶段：待开始。数据库脚本已预留 AI 配置、会话、消息和调用日志结构；LangChain4j 依赖与默认 OpenAI-compatible 配置已落地，业务模块尚未落地。**

```
已具备:
  ✅ ai_channel_config 表结构预留
  ✅ ai_chat_session 表结构预留
  ✅ ai_chat_message 表结构预留
  ✅ ai_usage_log 表结构预留
  ✅ 用户等级与 AI 额度联动需求已明确
  ✅ LangChain4j 依赖与默认配置已接入

待推进:
  ⏳ AI 模块包结构和模型
  ⏳ 用户侧 AI 会话 / 问答 / 历史记录
  ⏳ 通用大模型调用封装
  ⏳ 后台 AI 配置中心
  ⏳ 每日额度、用户级限额、上下文长度限制
  ⏳ 数据读取范围控制，默认禁止读取私聊
```

## 3. 模块结构

- [ ] 新增 `module/ai/controller`。
- [ ] 新增 `module/ai/service` 与 `module/ai/service/impl`。
- [ ] 新增 `module/ai/repository` 与 `module/ai/repository/impl`。
- [ ] 新增 `module/ai/model/user`、`module/ai/model/admin`、`module/ai/model/internal`。
- [ ] 新增 `module/ai/convert`，优先使用 MapStruct。
- [ ] 新增 AI 相关 domain、mapper、XML。

## 4. T09 AI 问答主链路

### 4.1 目标

- 登录用户可创建 AI 会话。
- 登录用户可在会话内提问。
- 系统调用通用大模型并保存回答。
- 用户可查询历史对话。

### 4.2 数据与模型

- [ ] 复核 `ai_chat_session` 是否覆盖用户 ID、标题、状态、模型渠道、最后消息时间。
- [ ] 复核 `ai_chat_message` 是否覆盖会话 ID、角色、内容、token 数、调用状态、错误信息。
- [ ] 定义消息角色：`user`、`assistant`、`system`。
- [ ] 定义调用状态：`pending`、`success`、`failed`。

### 4.3 用户侧接口

- [ ] `POST /api/user/ai/sessions`：创建 AI 会话。
- [ ] `GET /api/user/ai/sessions`：分页查询我的会话。
- [ ] `GET /api/user/ai/sessions/{id}`：查询会话详情。
- [ ] `GET /api/user/ai/sessions/{id}/messages`：查询会话消息。
- [ ] `POST /api/user/ai/sessions/{id}/messages`：发送问题并获取回答。
- [ ] `DELETE /api/user/ai/sessions/{id}`：删除或归档会话。

### 4.4 调用封装

- [ ] 定义 `AiModelClient` 抽象，屏蔽具体渠道。
- [x] 接入 LangChain4j 默认 OpenAI-compatible 配置，默认 provider 为 `deepseek`。
- [ ] 支持按配置选择渠道和模型。
- [ ] 支持提示词模板拼接。
- [ ] 支持上下文消息裁剪。
- [ ] 模型调用失败时写入失败消息和调用日志。
- [ ] 第一阶段同步返回即可，流式输出可后置。

### 4.5 权限与归属

- [ ] AI 仅登录用户可用。
- [ ] 会话归属必须校验。
- [ ] 删除会话只影响当前用户自己的会话。
- [ ] 管理员查询用户会话需独立后台权限。

### 4.6 测试

- [ ] 未登录用户访问 AI 接口被拦截。
- [ ] 用户创建会话成功。
- [ ] 用户提问成功并保存问答消息。
- [ ] 模型调用失败时返回明确错误并保存失败记录。
- [ ] 越权查询他人会话被拦截。
- [ ] 上下文超长时按规则裁剪或拒绝。

## 5. T10 AI 后台配置中心与限额控制

### 5.1 目标

- 管理员可配置模型、渠道、API Key、提示词模板、开关和数据范围。
- 用户使用 AI 时受每日额度、用户级限额、等级额度和上下文长度限制。
- 默认禁止 AI 读取用户私聊内容。

### 5.2 配置中心

- [ ] `GET /api/sys/ai/channels`：查询渠道配置。
- [ ] `POST /api/sys/ai/channels`：新增渠道配置。
- [ ] `PUT /api/sys/ai/channels/{id}`：修改渠道配置。
- [ ] `PUT /api/sys/ai/channels/{id}/status`：启用 / 禁用渠道。
- [ ] `DELETE /api/sys/ai/channels/{id}`：删除或停用渠道。
- [ ] API Key 存储必须脱敏返回。
- [ ] 高风险配置修改写入审计日志。

### 5.3 限额控制

- [ ] 每日平台总额度。
- [ ] 每日用户默认额度。
- [ ] 用户级单独额度。
- [ ] 等级联动额度。
- [ ] 单次上下文长度限制。
- [ ] 单次输入长度限制。
- [ ] 配额检查在模型调用前完成。

### 5.4 调用统计

- [ ] 每次调用写入 `ai_usage_log`。
- [ ] 记录用户、渠道、模型、token、成功失败、耗时、错误信息。
- [ ] 后台支持按用户、渠道、模型、时间范围查询统计。
- [ ] 支持今日剩余额度查询。

### 5.5 数据读取范围

- [ ] 定义数据范围枚举：`none`、`public_articles`、`own_articles`、`profile`、`public_chat`、`private_chat`。
- [ ] 默认不启用 `private_chat`。
- [ ] 私聊读取必须有明确配置和高风险审计。
- [ ] 第一阶段不做知识库 / RAG，只保留范围配置和基础读取入口。

### 5.6 测试

- [ ] AI 总开关关闭时用户调用被拦截。
- [ ] 用户每日额度耗尽时调用被拦截。
- [ ] 用户单独额度覆盖默认额度。
- [ ] 等级额度计算正确。
- [ ] API Key 返回脱敏。
- [ ] 默认配置下 AI 不读取私聊内容。
- [ ] 高风险配置修改生成审计日志。

## 6. 后置能力

### 6.1 T18 知识库 / RAG

- [ ] 文章向量化和索引。
- [ ] 检索增强上下文拼接。
- [ ] 引用来源返回。
- [ ] 知识库更新和失效策略。

### 6.2 T19 agents

- [ ] agent 定义和工具调用模型。
- [ ] agent 任务状态。
- [ ] 异步任务完成通知。
- [ ] 管理端 agent 配置。

## 7. 完成标志

- 登录用户可完成基础 AI 问答。
- 对话记录可保存、查询和删除。
- 管理员可配置渠道、模型、额度、提示词和开关。
- 配额、上下文长度和数据范围控制生效。
- AI 默认不读取用户私聊内容。
- 对应 API 文档、需求文档和任务清单已同步更新。

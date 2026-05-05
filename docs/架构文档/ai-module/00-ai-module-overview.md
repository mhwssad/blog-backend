# AI 模块总览

## 1. 模块定位

AI 模块（`module/ai`）是博客后端的核心智能服务层，负责：

- **AI 对话**：用户与 AI 的多轮会话管理
- **知识库**：RAG 向量知识检索（待实现）
- **Agent**：任务型 AI 代理执行
- **工具调用**：AI 调用外部工具与 MCP 服务集成

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   POST /api/user/ai/sessions    GET /api/user/ai/sessions/quota │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│   AiChatController        AiAgentTaskController                │
│   AiSessionAdminController  AiToolAdminController               │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ AiChatService│  │AiQuotaService│  │AiModelClient │           │
│  │ 会话/消息    │  │ 额度校验     │  │ 模型调用      │           │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │AiAgentTask  │  │AiToolExec   │  │AiUsageLog   │              │
│  │ Agent任务   │  │ 工具执行      │  │ 调用日志     │              │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │  LangChain4j    │   │  Redis          │
│   数据访问       │   │  模型调用       │   │  额度计数器      │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  第三方 AI API  │   │  TTL 当日午夜    │
│  持久化存储       │   │  OpenAI/DeepSeek│   │  日配额控制      │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 目录结构

```
module/ai/
├── constant/           # 常量定义（角色类型、场景类型）
├── controller/          # 控制器（用户侧 + 后台管理）
│   ├── user/           # 用户侧接口
│   └── admin/          # 后台管理接口
├── convert/            # MapStruct 转换器
├── domain/             # 数据库实体（ai_channel_config 等）
├── enums/              # AI 相关枚举
├── event/              # Spring Event（内容变更事件）
├── listener/           # 事件监听器（知识库同步监听）
├── model/              # 请求/响应模型
│   ├── admin/          # 后台管理请求/VO
│   ├── user/           # 用户侧请求/VO
│   └── data/           # 内部数据传输对象
├── repository/         # 数据访问层
│   └── impl/           # MyBatis-Plus 实现
└── service/            # 业务服务
    └── impl/           # 服务实现
```

## 4. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| AI 对话 | AiChatService | ✅ 完成 |
| 会话管理 | AiChatSession | ✅ 完成 |
| 渠道配置 | AiChannelConfig | ✅ 完成 |
| 额度控制 | AiQuotaService | ✅ 完成 |
| 调用日志 | AiUsageLogService | ✅ 完成 |
| Agent 任务 | AiAgentTaskService | ✅ 完成 |
| 工具调用 | AiToolExecutionService | ✅ 完成 |
| MCP 集成 | AiMcpServerAdminService | ✅ 完成 |
| 知识库同步 | KnowledgeSyncContentListener | ✅ 完成 |
| RAG 向量化 | - | ⬜ 待推进 |

## 5. 扩展方向

- **号池管理**：多 API Key 轮询、高可用切换
- **RAG 主链路**：向量化、分段、检索、上下文拼接
- **流式输出**：StreamingChatModel 支持
- **工具频控**：按工具/场景的调用频率限制

## 6. 相关文档

- [AI 调用流程](./ai-invocation-flow.md)
- [AI 数据模型](./ai-data-model.md)
- [AI 额度体系](./ai-quota-system.md)
- [AI 渠道配置](./ai-channel-config.md)

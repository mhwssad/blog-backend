# AI 模块架构文档

本文档收集了 AI 模块完整的架构设计文档。

## 文档索引

| 文档 | 说明 |
|------|------|
| [00-ai-module-overview.md](./00-ai-module-overview.md) | AI 模块总览：定位、分层、目录结构、能力矩阵 |
| [ai-invocation-flow.md](./ai-invocation-flow.md) | AI 调用流程详解：整体调用链、额度校验、模型调用、上下文裁剪 |
| [ai-data-model.md](./ai-data-model.md) | AI 数据模型：数据库表结构、实体映射、枚举定义 |
| [ai-quota-system.md](./ai-quota-system.md) | AI 额度体系：三层额度控制、等级配置、Redis Key 设计 |
| [ai-channel-config.md](./ai-channel-config.md) | AI 渠道配置：渠道选择策略、后台管理接口、高风险审计 |
| [ai-agent-tool.md](./ai-agent-tool.md) | AI Agent 与工具调用：Agent 架构、执行流程、工具授权、MCP 集成 |
| [ai-knowledge-sync.md](./ai-knowledge-sync.md) | AI 知识库与同步：知识源配置、内容变更事件、状态流转 |
| [ai-rag-retrieval.md](./ai-rag-retrieval.md) | AI RAG 检索增强：分块、向量化、向量存储、提示词增强 |

## 核心能力概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI 模块能力                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   AI 对话         │  │   知识库         │  │   Agent          │          │
│  │   会话/消息       │  │   知识源/同步    │  │   任务/工具      │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   额度控制        │  │   渠道配置       │  │   MCP 集成       │          │
│  │   三层校验        │  │   多渠道管理     │  │   stdio/http     │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐                                                      │
│  │   RAG 检索增强     │                                                      │
│  │   向量化/分块/检索  │                                                      │
│  │   ✅ 完成         │                                                      │
│  └──────────────────┘                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 快速导航

### 开发者视角

1. **新增 AI 渠道** → 参考 [ai-channel-config.md](./ai-channel-config.md)
2. **理解调用流程** → 参考 [ai-invocation-flow.md](./ai-invocation-flow.md)
3. **查看数据表结构** → 参考 [ai-data-model.md](./ai-data-model.md)
4. **了解额度控制** → 参考 [ai-quota-system.md](./ai-quota-system.md)
5. **扩展 Agent 工具** → 参考 [ai-agent-tool.md](./ai-agent-tool.md)
6. **理解知识同步** → 参考 [ai-knowledge-sync.md](./ai-knowledge-sync.md)
7. **RAG 检索增强** → 参考 [ai-rag-retrieval.md](./ai-rag-retrieval.md)

### 运营视角

1. **配置渠道** → 后台 `/api/sys/ai/channels`
2. **查看用量** → 后台 `/api/sys/ai/usage/log/page`
3. **管理 Agent** → 后台 `/api/sys/ai/agent/definition`
4. **管理工具** → 后台 `/api/sys/ai/tool/definition`
5. **配置 MCP** → 后台 `/api/sys/ai/mcp/server`
6. **管理知识库** → 后台 `/api/sys/ai/knowledge/entry/page`
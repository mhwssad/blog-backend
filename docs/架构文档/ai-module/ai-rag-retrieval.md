# AI RAG 检索增强

## 1. RAG 架构总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI RAG 体系                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        RAG 配置 (AiRagProperties)                    │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐         │   │
│  │  │ enabled   │ │ chunkSize │ │ chunkOver │ │ topK      │         │   │
│  │  │ true      │ │ 800       │ │ 120       │ │ 5         │         │   │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘         │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐                      │   │
│  │  │ minScore  │ │ cacheTtl  │ │ provider  │                      │   │
│  │  │ 0.35      │ │ 30min     │ │ mysql-redis│                      │   │
│  │  └───────────┘ └───────────┘ └───────────┘                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│                                    ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     RAG 服务层 (AiRagService)                         │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │  retrieve() - 检索知识库并生成上下文                          │   │   │
│  │  │  enrichSystemPrompt() - 拼接系统提示词                        │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                      │
│              ┌─────────────────────┼─────────────────────┐              │
│              ▼                     ▼                     ▼              │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐ │
│  │  AiEmbeddingService  │  │    AiVectorStore    │  │  AiChatService  │ │
│  │  文本 → 向量          │  │  向量存储/检索       │  │  AI 对话整合     │ │
│  │  AllMiniLmL6V2      │  │  MysqlRedisImpl     │  │                │ │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────┘ │
│              │                     │                                      │
│              ▼                     ▼                                      │
│  ┌─────────────────────┐  ┌─────────────────────┐                       │
│  │  LangChain4j        │  │  MySQL + Redis       │                       │
│  │  Local Embedding    │  │  向量 + 缓存          │                       │
│  └─────────────────────┘  └─────────────────────┘                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. RAG 完整流程

```
用户提问
         │
         ▼
┌─────────────────────┐
│   AiRagService      │
│   .retrieve()       │
└─────────────────────┘
         │
         ├─1─ RAG 开关检查
         │         │
         │         ▼
         │    ragProperties.enabled == true ?
         │         │
         │         ├─ false → 返回空结果
         │         └─ true → 继续
         │
         ├─2─ 渠道权限检查
         │         │
         │         ▼
         │    allowsRag(channelConfig) ?
         │    检查 dataScopeJson 是否包含
         │    PUBLIC_ARTICLES / PROFILE / PUBLIC_CHAT
         │         │
         │         ├─ 不允许 → 返回空结果
         │         └─ 允许 → 继续
         │
         ├─3─ 问题向量化
         │         │
         │         ▼
         │    aiEmbeddingService.embed(question)
         │    → List<Float> queryEmbedding
         │         │
         │         ▼
         │    使用 AllMiniLmL6V2QuantizedEmbeddingModel
         │    本地模型，无需 API 调用
         │
         ├─4─ 向量检索
         │         │
         │         ▼
         │    aiVectorStore.search(queryEmbedding, topK, minScore)
         │         │
         │         ▼
         │    MysqlRedisAiVectorStore
         │    ├─ 加载活跃分块（Redis 缓存 → MySQL 回源）
         │    ├─ 余弦相似度计算
         │    ├─ 过滤 minScore
         │    └─ 排序返回 topK
         │
         └─5─ 构建结果
                   │
                   ▼
            AiRagRetrievalResult
            ├─ enabled: true
            ├─ hits: List<AiRagHit>
            ├─ contextText: 拼接的上下文
            ├─ references: 引用列表
            ├─ referenceJson: JSON 序列化
            └─ durationMs: 耗时
```

## 3. 分块与向量化

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       知识分块与向量化流程                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│  │ AiKnowledge │     │   分块       │     │   向量化     │                  │
│  │   Entry     │     │   切割       │     │   Embedding │                  │
│  │             │     │             │     │             │                  │
│  │ contentSnap │────▶│ chunkSize=800│────▶│ AllMiniLm   │                  │
│  │ 完整内容     │     │ chunkOverlap│     │ L6V2        │                  │
│  │             │     │   =120      │     │ 本地模型     │                  │
│  └─────────────┘     └─────────────┘     └─────────────┘                  │
│         │                   │                   │                         │
│         │                   ▼                   ▼                         │
│         │          ┌─────────────────────────────┐                        │
│         │          │    AiKnowledgeChunk         │                        │
│         │          │  ┌────────────────────────┐ │                        │
│         │          │  │ chunkText: 分块文本     │ │                        │
│         │          │  │ embeddingJson: 向量JSON │ │                        │
│         │          │  │ embeddingDim: 384       │ │                        │
│         │          │  │ embeddingModel: ...     │ │                        │
│         │          │  │ chunkIndex: 0,1,2...   │ │                        │
│         │          │  │ contentHash: SHA-256   │ │                        │
│         │          │  └────────────────────────┘ │                        │
│         │          └─────────────────────────────┘                        │
│         │                          │                                      │
│         │                          ▼                                      │
│         │                 ┌─────────────────────┐                         │
│         │                 │  AiVectorStore      │                         │
│         │                 │  upsertChunks()     │                         │
│         │                 └─────────────────────┘                         │
│         │                          │                                      │
│         │          ┌───────────────┴───────────────┐                    │
│         │          ▼                               ▼                     │
│         │  ┌─────────────────────┐    ┌─────────────────────┐           │
│         │  │   MySQL              │    │   Redis             │           │
│         │  │  ai_knowledge_chunk  │    │  ai:rag:chunks:active│           │
│         │  │  持久化存储           │    │  活跃分块缓存         │           │
│         │  └─────────────────────┘    └─────────────────────┘           │
│         │                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. 向量存储实现 (MysqlRedisAiVectorStore)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MysqlRedisAiVectorStore 实现                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  upsertChunks(entryId, chunks)                                      │   │
│  │  ├─ aiKnowledgeChunkRepository.disableByEntryId(entryId)           │   │
│  │  │     (将旧分块标记为失效)                                          │   │
│  │  ├─ saveBatch(chunks) 保存新分块                                    │   │
│  │  └─ invalidateCache() 删除 Redis 缓存                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  search(queryEmbedding, topK, minScore)                              │   │
│  │  ├─ loadActiveChunks() 加载分块                                      │   │
│  │  │     │                                                            │   │
│  │  │     ├─ 先查 Redis 缓存 ai:rag:chunks:active                     │   │
│  │  │     ├─ 缓存命中 → 返回                                           │   │
│  │  │     └─ 缓存未命中 → 查询 MySQL → 写入 Redis → 返回               │   │
│  │  │                                                                  │   │
│  │  ├─ 遍历所有活跃分块                                                │   │
│  │  ├─ cosineSimilarity() 计算余弦相似度                               │   │
│  │  ├─ 过滤 minScore                                                   │   │
│  │  ├─ 排序 (score DESC)                                               │   │
│  │  └─ 返回 topK                                                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  deleteByEntryId(entryId)                                            │   │
│  │  ├─ disableByEntryId(entryId) 将分块标记为失效                      │   │
│  │  └─ invalidateCache() 删除 Redis 缓存                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 余弦相似度计算

```java
private double cosineSimilarity(List<Float> left, List<Float> right) {
    int size = Math.min(left.size(), right.size());
    if (size == 0) return 0D;

    double dot = 0D, leftNorm = 0D, rightNorm = 0D;
    for (int i = 0; i < size; i++) {
        dot += left.get(i) * right.get(i);
        leftNorm += left.get(i) * left.get(i);
        rightNorm += right.get(i) * right.get(i);
    }
    if (leftNorm == 0D || rightNorm == 0D) return 0D;
    return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
}
```

## 5. RAG 与 AI 对话集成

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      RAG 融入 AI 对话流程                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  AiChatService.sendMessage()                                                │
│         │                                                                   │
│         ├─1─ verifySessionOwnership / 加载渠道配置                           │
│         │                                                                   │
│         ├─2─ checkQuota() 额度校验                                          │
│         │                                                                   │
│         ├─3─ 保存用户消息                                                    │
│         │                                                                   │
│         ├─4─ 加载上下文消息                                                  │
│         │                                                                   │
│         ├─5─ RAG 检索                                                      │
│         │         │                                                        │
│         │         ▼                                                        │
│         │    AiRagService.retrieve(channelConfig, question)                │
│         │         │                                                        │
│         │         ├─ 向量化问题                                             │
│         │         ├─ 向量检索                                              │
│         │         └─ 返回 AiRagRetrievalResult                              │
│         │         │                                                        │
│         │         ▼                                                        │
│         │    AiRagService.enrichSystemPrompt(systemPrompt, result)          │
│         │         │                                                        │
│         │         ▼                                                        │
│         │    拼接知识库片段到系统提示词                                        │
│         │                                                                   │
│         ├─6─ aiModelClient.chat()                                          │
│         │         │                                                        │
│         │         ▼                                                        │
│         │    LangChain4j OpenAiChatModel                                   │
│         │         │                                                        │
│         │         ▼                                                        │
│         │    返回 AiModelCallResult (含 content)                            │
│         │                                                                   │
│         ├─7─ 保存 AI 回复                                                   │
│         │                                                                   │
│         ├─8─ recordUsage() 额度扣减                                        │
│         │                                                                   │
│         └─9─ logUsage() 调用日志（含 RAG 元数据）                            │
│                   │                                                        │
│                   ▼                                                        │
│            AiUsageLogService.logUsage(..., ragEnabled, ragHitCount,         │
│                                        ragDurationMs, ragReferenceJson)     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 6. 系统提示词增强

```java
// AiRagServiceImpl.enrichSystemPrompt()
String enrichSystemPrompt(String systemPrompt, AiRagRetrievalResult result) {
    if (!result.isEnabled() || !hasText(result.getContextText())) {
        return systemPrompt;
    }
    return systemPrompt + """

        你可以参考以下知识库片段回答用户问题。
        要求：
        1. 优先基于引用内容回答。
        2. 如果引用内容不足以支撑答案，请明确说明"未在知识库中找到直接依据"。
        3. 不要编造不存在的来源或引用。

        知识库片段：
        """ + result.getContextText();
}
```

生成的增强提示词示例：

```
你是一个专业的技术博客助手。

你可以参考以下知识库片段回答用户问题。
要求：
1. 优先基于引用内容回答。
2. 如果引用内容不足以支撑答案，请明确说明"未在知识库中找到直接依据"。
3. 不要编造不存在的来源或引用。

知识库片段：
[引用1] Java RAG 入门指南，来源：PUBLIC_ARTICLE:123，相似度：0.8542

RAG（Retrieval-Augmented Generation）是一种结合检索和生成的技术...
```

## 7. 知识分块表结构

```sql
CREATE TABLE ai_knowledge_chunk (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    entry_id        BIGINT       NOT NULL COMMENT '知识条目ID',
    source_type     VARCHAR(32)  NOT NULL COMMENT '来源类型',
    source_id       BIGINT       NOT NULL COMMENT '来源对象ID',
    title           VARCHAR(256) COMMENT '标题快照',
    source_url      VARCHAR(512) COMMENT '来源页面URL',
    chunk_index     INT          NOT NULL COMMENT '分块序号',
    chunk_text      TEXT         NOT NULL COMMENT '分块文本',
    content_hash    VARCHAR(64)  COMMENT '分块内容 SHA-256',
    embedding_json  TEXT         COMMENT 'embedding 向量 JSON 数组',
    embedding_dim   INT          COMMENT '向量维度',
    embedding_model VARCHAR(64)   COMMENT 'embedding 模型名称',
    status          TINYINT      DEFAULT 1 COMMENT '状态：0-失效，1-有效',
    entry_version   INT          DEFAULT 1 COMMENT '知识条目版本号',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_entry_id (entry_id),
    INDEX idx_status (status),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识分块表';
```

## 8. RAG 配置属性

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `ai.rag.enabled` | Boolean | `true` | 是否启用 RAG 主链路 |
| `ai.rag.chunkSize` | Integer | `800` | 单个知识分块最大字符数 |
| `ai.rag.chunkOverlap` | Integer | `120` | 相邻分块重叠字符数 |
| `ai.rag.topK` | Integer | `5` | 检索返回数量 |
| `ai.rag.minScore` | Double | `0.35` | 最低相似度阈值 |
| `ai.rag.cacheTtl` | Duration | `30min` | Redis 缓存 TTL |
| `ai.rag.embeddingProvider` | String | `mysql-redis` | 向量存储提供者 |

## 9. 关键文件

| 文件 | 职责 |
|------|------|
| `AiRagService.java` | RAG 服务接口 |
| `AiRagServiceImpl.java` | RAG 检索与提示词增强 |
| `AiEmbeddingService.java` | Embedding 向量生成接口 |
| `AiEmbeddingServiceImpl.java` | 本地模型 AllMiniLmL6V2 实现 |
| `AiVectorStore.java` | 向量存储抽象 |
| `MysqlRedisAiVectorStore.java` | MySQL + Redis 向量存储实现 |
| `AiKnowledgeChunkService.java` | 知识分块服务接口 |
| `AiKnowledgeChunkServiceImpl.java` | 分块切割与向量化 |
| `AiRagProperties.java` | RAG 配置属性 |
| `AiRagRetrievalResult.java` | 检索结果 |
| `AiRagHit.java` | 检索命中 |
| `AiRagReferenceVO.java` | 引用信息 |
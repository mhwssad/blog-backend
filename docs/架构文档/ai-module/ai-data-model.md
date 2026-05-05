# AI 数据模型

## 1. 数据库表结构

### 1.1 渠道配置表 (ai_channel_config)

```sql
CREATE TABLE ai_channel_config (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    channel_code    VARCHAR(64)  NOT NULL COMMENT '渠道编码',
    channel_name    VARCHAR(128) NOT NULL COMMENT '渠道名称',
    provider        VARCHAR(64)  NOT NULL COMMENT '提供方',
    model_name      VARCHAR(128) NOT NULL COMMENT '模型名称',
    api_base_url    VARCHAR(512) COMMENT '接口基础地址',
    api_key_encrypted VARCHAR(512) COMMENT '加密后的API Key',
    daily_quota     INT          DEFAULT 0 COMMENT '全局每日额度，0表示不限制',
    user_daily_quota INT         DEFAULT 0 COMMENT '单用户每日额度，0表示不限制',
    max_context_tokens INT       DEFAULT 0 COMMENT '上下文长度上限，0表示不限制',
    data_scope_json  TEXT        COMMENT '可读取数据范围配置JSON',
    system_prompt_template TEXT COMMENT '系统提示词模板',
    status          TINYINT      DEFAULT 0 COMMENT '状态：0-停用，1-启用',
    is_default      TINYINT      DEFAULT 0 COMMENT '是否默认渠道：0-否，1-是',
    created_by      BIGINT       COMMENT '创建人ID',
    updated_by      BIGINT       COMMENT '更新人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_code (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI渠道配置表';
```

### 1.2 会话表 (ai_chat_session)

```sql
CREATE TABLE ai_chat_session (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    channel_config_id BIGINT      NOT NULL COMMENT '渠道配置ID',
    title           VARCHAR(256) COMMENT '会话标题',
    scene_type      VARCHAR(32)  DEFAULT 'GENERAL' COMMENT '场景类型',
    status          TINYINT      DEFAULT 1 COMMENT '状态：1-正常，2-关闭',
    last_message_at DATETIME     COMMENT '最后消息时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_channel_config_id (channel_config_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';
```

### 1.3 消息表 (ai_chat_message)

```sql
CREATE TABLE ai_chat_message (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT       NOT NULL COMMENT '会话ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    role_type       VARCHAR(16)  NOT NULL COMMENT '角色类型：system/user/assistant',
    content         TEXT         COMMENT '消息内容',
    token_count     INT          DEFAULT 0 COMMENT 'token数量',
    request_scene_type VARCHAR(32) COMMENT '请求场景类型',
    request_target_id BIGINT      COMMENT '请求目标ID',
    response_status TINYINT      DEFAULT 1 COMMENT '响应状态：1-成功，2-失败',
    error_message   VARCHAR(512) COMMENT '错误信息',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';
```

### 1.4 使用日志表 (ai_usage_log)

```sql
CREATE TABLE ai_usage_log (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    channel_config_id BIGINT      NOT NULL COMMENT '渠道配置ID',
    session_id      BIGINT       COMMENT '会话ID',
    request_scene_type VARCHAR(32) COMMENT '请求场景类型',
    request_tokens   INT          DEFAULT 0 COMMENT '请求token数',
    response_tokens  INT          DEFAULT 0 COMMENT '响应token数',
    total_tokens     INT          DEFAULT 0 COMMENT '总token数',
    quota_cost       INT          DEFAULT 0 COMMENT '额度消耗',
    success_status   TINYINT      DEFAULT 0 COMMENT '成功状态：0-失败，1-成功',
    error_code       VARCHAR(64)  COMMENT '错误码',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_channel_config_id (channel_config_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI使用日志表';
```

### 1.5 知识源配置表 (ai_knowledge_source_config)

```sql
CREATE TABLE ai_knowledge_source_config (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    source_type     VARCHAR(32)  NOT NULL COMMENT '知识源类型',
    source_name     VARCHAR(128) NOT NULL COMMENT '知识源名称',
    enabled         TINYINT      DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    config_json     TEXT         COMMENT '配置JSON',
    sync_interval_minutes INT     DEFAULT 60 COMMENT '同步间隔（分钟）',
    created_by      BIGINT       COMMENT '创建人ID',
    updated_by      BIGINT       COMMENT '更新人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识源配置表';
```

### 1.6 知识条目表 (ai_knowledge_entry)

```sql
CREATE TABLE ai_knowledge_entry (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    source_type     VARCHAR(32)  NOT NULL COMMENT '知识源类型',
    source_id       BIGINT       NOT NULL COMMENT '来源ID',
    title           VARCHAR(256) COMMENT '标题',
    summary         TEXT         COMMENT '摘要',
    content_snapshot TEXT        COMMENT '内容快照',
    author_id       BIGINT       COMMENT '作者ID',
    status          VARCHAR(16)  DEFAULT 'PENDING' COMMENT '状态：PENDING/OUTDATED/INDEXED/DISABLED/DELETED',
    version         INT          DEFAULT 1 COMMENT '版本号',
    synced_at       DATETIME     COMMENT '同步时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source (source_type, source_id),
    INDEX idx_type_status (source_type, status),
    INDEX idx_author (author_id, status),
    INDEX idx_synced (synced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识条目表';
```

### 1.7 Agent 定义表 (ai_agent_definition)

```sql
CREATE TABLE ai_agent_definition (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL COMMENT 'Agent名称',
    description     VARCHAR(512) COMMENT 'Agent描述',
    system_prompt   TEXT         NOT NULL COMMENT '系统提示词',
    channel_config_id BIGINT      COMMENT '关联AI渠道配置ID',
    data_scope_json TEXT         COMMENT '数据读取范围配置JSON',
    enabled         TINYINT      DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    max_turns       INT          DEFAULT 10 COMMENT '最大轮次',
    extra_config_json TEXT       COMMENT '扩展配置JSON（工具授权等）',
    created_by      BIGINT       COMMENT '创建人ID',
    updated_by      BIGINT       COMMENT '更新人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent定义表';
```

### 1.8 Agent 任务表 (ai_agent_task)

```sql
CREATE TABLE ai_agent_task (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    agent_id        BIGINT       NOT NULL COMMENT 'Agent定义ID',
    status          VARCHAR(16)  DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    input_content   TEXT         NOT NULL COMMENT '输入内容',
    output_content  TEXT         COMMENT '输出内容',
    error_message   VARCHAR(512) COMMENT '错误信息',
    token_count     INT          DEFAULT 0 COMMENT '消耗token数',
    started_at      DATETIME     COMMENT '开始时间',
    completed_at    DATETIME     COMMENT '完成时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent任务表';
```

### 1.9 工具定义表 (ai_tool_definition)

```sql
CREATE TABLE ai_tool_definition (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tool_code       VARCHAR(64)  NOT NULL COMMENT '工具编码',
    tool_name       VARCHAR(128) NOT NULL COMMENT '工具名称',
    description     VARCHAR(512) COMMENT '工具描述',
    param_schema    TEXT         COMMENT '参数Schema（JSON）',
    return_schema   TEXT         COMMENT '返回Schema（JSON）',
    risk_level      TINYINT      DEFAULT 0 COMMENT '风险等级：0-低，1-中，2-高',
    enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tool_code (tool_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具定义表';
```

### 1.10 工具授权表 (ai_tool_authorization)

```sql
CREATE TABLE ai_tool_authorization (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tool_id         BIGINT       NOT NULL COMMENT '工具ID',
    auth_type       VARCHAR(32)  NOT NULL COMMENT '授权类型：AGENT/SCENE/USER',
    auth_value      VARCHAR(128) NOT NULL COMMENT '授权值：agentId/sceneType/userId',
    data_scope_json TEXT         COMMENT '数据范围限制',
    enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
    created_by      BIGINT       COMMENT '创建人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tool_id (tool_id),
    INDEX idx_auth (auth_type, auth_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具授权表';
```

### 1.11 MCP 服务配置表 (ai_mcp_server_config)

```sql
CREATE TABLE ai_mcp_server_config (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    server_name     VARCHAR(128) NOT NULL COMMENT '服务名称',
    transport_type  VARCHAR(16)  NOT NULL COMMENT '传输类型：STDIO/HTTP',
    connection_config_json TEXT   COMMENT '连接配置JSON',
    auth_config_json TEXT        COMMENT '鉴权配置JSON',
    timeout_seconds INT          DEFAULT 30 COMMENT '超时秒数',
    enabled         TINYINT      DEFAULT 1 COMMENT '是否启用',
    created_by      BIGINT       COMMENT '创建人ID',
    updated_by      BIGINT       COMMENT '更新人ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI MCP服务配置表';
```

## 2. 实体类映射

```
ai_channel_config    →  AiChannelConfig (domain/ai/)
ai_chat_session      →  AiChatSession (domain/ai/)
ai_chat_message      →  AiChatMessage (domain/ai/)
ai_usage_log         →  AiUsageLog (domain/ai/)
ai_knowledge_source_config → AiKnowledgeSourceConfig (domain/ai/)
ai_knowledge_entry   →  AiKnowledgeEntry (domain/ai/)
ai_agent_definition  →  AiAgentDefinition (domain/ai/)
ai_agent_task        →  AiAgentTask (domain/ai/)
ai_agent_task_log    →  AiAgentTaskLog (domain/ai/)
ai_tool_definition   →  AiToolDefinition (domain/ai/)
ai_tool_authorization → AiToolAuthorization (domain/ai/)
ai_tool_call_log     →  AiToolCallLog (domain/ai/)
ai_mcp_server_config →  AiMcpServerConfig (domain/ai/)
```

## 3. 数据流关系

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            AiChannelConfig                               │
│                         渠道配置（多个）                                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                          │
│  │ 渠道A      │  │ 渠道B      │  │ 渠道C      │                          │
│  │ gpt-4     │  │ deepseek   │  │ claude     │                          │
│  └────────────┘  └────────────┘  └────────────┘                          │
└──────────────────────────────────────────────────────────────────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│   AiChatSession     │   │   AiAgentDefinition │   │   AiAgentTask       │
│   用户会话           │   │   Agent定义         │   │   Agent任务          │
│   绑定一个渠道       │   │   绑定一个渠道       │   │   绑定Agent定义      │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
              │                       │                       │
              ▼                       │                       │
┌─────────────────────┐               │                       │
│   AiChatMessage     │               │                       │
│   会话消息列表       │               │                       │
│   历史上下文         │               │                       │
└─────────────────────┘               │                       │
                                      │                       ▼
                                      │           ┌─────────────────────┐
                                      │           │   AiAgentTaskLog     │
                                      │           │   任务执行日志        │
                                      │           └─────────────────────┘
                                      │
┌─────────────────────────────────────┴───────────────────────────────────┐
│                              AiUsageLog                                  │
│                           调用日志（统一记录）                              │
│  userId / channelConfigId / sessionId / tokens / successStatus            │
└──────────────────────────────────────────────────────────────────────────┘
```

## 4. 枚举值定义

### 渠道状态 (AiChannelStatusEnum)

| 值 | 常量 | 说明 |
|----|------|------|
| 0 | DISABLED | 停用 |
| 1 | ENABLED | 启用 |

### 会话状态 (AiChatSessionStatusEnum)

| 值 | 常量 | 说明 |
|----|------|------|
| 1 | NORMAL | 正常 |
| 2 | CLOSED | 已关闭 |

### 消息角色类型 (AiMessageRoleTypeEnum)

| 值 | 常量 | 说明 |
|----|------|------|
| system | SYSTEM | 系统消息 |
| user | USER | 用户消息 |
| assistant | ASSISTANT | AI 助手消息 |

### 消息响应状态 (AiMessageResponseStatusEnum)

| 值 | 常量 | 说明 |
|----|------|------|
| 1 | SUCCESS | 成功 |
| 2 | FAILED | 失败 |

### 知识源类型 (AiKnowledgeSourceTypeEnum)

| 值 | 说明 |
|------|------|
| PUBLIC_ARTICLE | 公开文章 |
| FORUM_POST | 论坛帖子 |
| AUTHOR_PROFILE | 作者公开资料 |
| MANUAL_ENTRY | 后台手动维护 |

### 知识条目状态 (AiKnowledgeEntryStatusEnum)

| 值 | 说明 |
|------|------|
| PENDING | 待同步 |
| OUTDATED | 已过期 |
| INDEXED | 已索引 |
| DISABLED | 已禁用 |
| DELETED | 已删除 |

### Agent 任务状态 (AiAgentTaskStatusEnum)

| 值 | 说明 |
|------|------|
| PENDING | 待执行 |
| RUNNING | 执行中 |
| COMPLETED | 已完成 |
| FAILED | 失败 |
| CANCELLED | 已取消 |

### 数据范围 (AiDataScopeEnum)

| 值 | 说明 |
|------|------|
| PUBLIC_ARTICLES | 公开文章 |
| FORUM_POSTS | 论坛帖子 |
| AUTHOR_PROFILE | 作者公开资料 |
| KNOWLEDGE_ENTRY | 知识库条目 |
| PRIVATE_CHAT | 私聊（高风险） |
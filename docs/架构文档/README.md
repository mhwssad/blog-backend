# 架构文档

本目录收集项目各模块的架构设计文档。

## 目录结构

```
架构文档/
├── ai-module/          # AI 模块架构文档
│   ├── README.md       # 模块文档索引
│   ├── 00-ai-module-overview.md
│   ├── ai-invocation-flow.md
│   ├── ai-data-model.md
│   ├── ai-quota-system.md
│   ├── ai-channel-config.md
│   ├── ai-agent-tool.md
│   ├── ai-knowledge-sync.md
│   └── ai-rag-retrieval.md
├── article-module/     # 文章模块架构文档
│   ├── README.md
│   ├── 00-article-module-overview.md
│   ├── article-data-model.md
│   ├── article-access-control.md
│   └── article-status-flow.md
├── auth-module/        # 认证授权模块架构文档
│   ├── README.md
│   ├── 00-auth-module-overview.md
│   ├── auth-data-model.md
│   ├── auth-login-flow.md
│   ├── auth-rbac-system.md
│   ├── auth-experience-system.md
│   ├── auth-notice-system.md
│   └── auth-author-system.md
├── chat-module/        # 聊天模块架构文档
│   ├── README.md
│   ├── 00-chat-module-overview.md
│   ├── chat-data-model.md
│   ├── chat-message-flow.md
│   ├── chat-conversation-flow.md
│   ├── chat-group-management.md
│   └── chat-governance.md
├── content-module/     # 内容模块架构文档
│   ├── README.md
│   ├── 00-content-module-overview.md
│   ├── content-collection-flow.md
│   ├── content-comment-flow.md
│   └── content-taxonomy-flow.md
├── file-module/        # 文件模块架构文档
│   ├── README.md
│   ├── 00-file-module-overview.md
│   ├── file-upload-flow.md
│   ├── file-lifecycle-management.md
│   ├── file-data-model.md
│   └── file-chat-facade.md
└── forum-module/       # 论坛模块架构文档
    ├── README.md
    ├── 00-forum-module-overview.md
    ├── forum-data-model.md
    └── forum-core-flow.md
```

## 模块文档

### AI 模块

AI 模块负责博客后端的智能服务，包括 AI 对话、知识库、Agent、工具调用、RAG 检索增强等能力。

**文档索引**：[ai-module/README.md](./ai-module/README.md)

| 能力 | 状态 |
|------|------|
| AI 对话（会话/消息） | ✅ 完成 |
| 额度控制（三层校验） | ✅ 完成 |
| 渠道配置（多渠道管理） | ✅ 完成 |
| Agent（任务/工具） | ✅ 完成 |
| MCP 集成（stdio/http） | ✅ 完成 |
| 知识库同步（内容变更事件） | ✅ 完成 |
| RAG 检索增强（向量化/分块/存储/检索） | ✅ 完成 |

### Article 模块

Article 模块负责博客后端的文章管理，包括文章创作、审核、发布、访问控制、状态流转等能力。

**文档索引**：[article-module/README.md](./article-module/README.md)

| 能力 | 状态 |
|------|------|
| 文章创作（富文本/Markdown） | ✅ 完成 |
| 审核流程（送审/通过/拒绝） | ✅ 完成 |
| 访问控制（可见范围/访问级别） | ✅ 完成 |
| 定时发布 | ✅ 完成 |
| 分类标签管理 | ✅ 完成 |
| 收藏点赞 | ✅ 完成 |
| 作者等级体系 | ✅ 完成 |

### Auth 模块

Auth 模块负责博客后端的认证授权，包括账号登录、RBAC 权限、经验等级、通知体系、作者申请等能力。

**文档索引**：[auth-module/README.md](./auth-module/README.md)

| 能力 | 状态 |
|------|------|
| 账号登录（密码/邮箱验证码） | ✅ 完成 |
| RBAC 权限系统 | ✅ 完成 |
| 经验等级体系 | ✅ 完成 |
| 通知推送（站内/WebSocket） | ✅ 完成 |
| 作者申请审核 | ✅ 完成 |
| 超级管理员安全 | ✅ 完成 |
| 审计日志 | ✅ 完成 |

### Chat 模块

Chat 模块（即时通讯/私聊）负责博客后端的实时聊天服务，包括单聊、群聊、大厅频道、消息收发、实时推送、禁言治理等能力。

**文档索引**：[chat-module/README.md](./chat-module/README.md)

| 能力 | 状态 |
|------|------|
| 单聊会话 | ✅ 完成 |
| 群聊管理（成员/角色/邀请） | ✅ 完成 |
| 大厅/话题频道 | ✅ 完成 |
| 消息收发（文本/文件） | ✅ 完成 |
| 已读/投递状态 | ✅ 完成 |
| 实时推送（WebSocket/Redis） | ✅ 完成 |
| 附件处理（图片/语音异步） | ✅ 完成 |
| 禁言管理（全局/会话级） | ✅ 完成 |
| 举报处理与内容治理 | ✅ 完成 |
| 后台管理接口 | ✅ 完成 |

### Content 模块

Content 模块负责博客后端的通用内容管理，包括收藏、评论、分类标签、用户足迹、互动记录、友情链接等能力。

**文档索引**：[content-module/README.md](./content-module/README.md)

| 能力 | 状态 |
|------|------|
| 收藏管理（收藏夹/收藏记录） | ✅ 完成 |
| 评论管理（评论/点赞/回复树） | ✅ 完成 |
| 分类标签（分类树/标签） | ✅ 完成 |
| 足迹记录 | ✅ 完成 |
| 互动记录 | ✅ 完成 |
| 友情链接 | ✅ 完成 |

### File 模块

File 模块负责博客后端的文件存储与管理，包括文件上传、存储策略、生命周期管理、引用追踪等能力。

**文档索引**：[file-module/README.md](./file-module/README.md)

| 能力 | 状态 |
|------|------|
| 普通/分片/MD5 秒传上传 | ✅ 完成 |
| 多存储策略（本地/S3/OSS） | ✅ 完成 |
| 文件生命周期管理 | ✅ 完成 |
| 引用计数与垃圾回收 | ✅ 完成 |
| 聊天文件门面 | ✅ 完成 |
| 后台文件管理 | ✅ 完成 |

### Forum 模块

Forum 模块负责博客后端的社区论坛功能，包括版块管理、帖子发布、回复互动、点赞收藏、后台治理、AI 知识同步等能力。

**文档索引**：[forum-module/README.md](./forum-module/README.md)

| 能力 | 状态 |
|------|------|
| 版块管理（后台） | ✅ 完成 |
| 公开浏览 | ✅ 完成 |
| 发帖/回复/编辑/删除 | ✅ 完成 |
| 点赞/收藏/分享 | ✅ 完成 |
| 后台治理（隐藏/删除/置顶/精华） | ✅ 完成 |
| 经验值体系 | ✅ 完成 |
| AI 知识库同步事件 | ✅ 完成 |
# Forum 模块总览

## 1. 模块定位

Forum 模块（`module/forum`）是博客后端的社区论坛核心，负责：

- **版块管理**：论坛版块的创建、配置、可见性控制
- **帖子管理**：用户发帖、编辑、删除、置顶、精华
- **回复管理**：树形回复结构、楼层计数
- **互动功能**：点赞、收藏、分享到频道
- **后台治理**：隐藏/恢复/删除帖子，审核状态管理
- **知识同步**：帖子内容同步到 AI 知识库（RAG）

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                                │
│   GET /api/forum/*           POST /api/user/forum/*            │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                               │
│   PublicForumController    UserForumController                  │
│   ForumPostAdminController  ForumReplyAdminController           │
│   ForumSectionAdminController                                    │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ PublicForum     │  │ UserForum       │  │ ForumPostAdmin  │ │
│  │ 公开浏览         │  │ 用户操作         │  │ 后台治理         │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐                       │
│  │ ForumReplyAdmin │  │ ForumSectionAdmin│                      │
│  │ 回复治理         │  │ 版块管理         │                       │
│  └─────────────────┘  └─────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │   SysInteraction│   │   SysCollection │
│   数据访问       │   │   互动（点赞）    │   │   收藏           │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  MySQL          │   │  MySQL          │
│  forum_* 表      │   │  sys_interaction │   │  sys_collection │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 目录结构

```
module/forum/
├── constant/              # 常量定义（互动类型、收藏夹名称）
├── controller/            # 控制器（公开/用户/后台管理）
├── convert/               # MapStruct 转换器
├── model/                 # 请求/响应模型
│   ├── admin/             # 后台管理请求/VO
│   ├── publics/           # 公开接口请求/VO
│   └── user/              # 用户侧请求/VO
├── repository/            # 数据访问层（MyBatis-Plus）
│   └── impl/              # Repository 实现
└── service/               # 业务服务
    └── impl/              # 服务实现
```

## 4. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 版块管理 | ForumSectionAdminService | ✅ 完成 |
| 公开浏览 | PublicForumService | ✅ 完成 |
| 用户发帖 | UserForumService | ✅ 完成 |
| 回复管理 | ForumReplyAdminService | ✅ 完成 |
| 帖子治理 | ForumPostAdminService | ✅ 完成 |
| 点赞互动 | SysInteractionRepository | ✅ 完成 |
| 收藏功能 | SysCollectionRepository | ✅ 完成 |
| 分享频道 | ForumPostChannelLinkService | ✅ 完成 |
| 经验值 | UserExperienceService | ✅ 完成 |
| 知识同步 | ContentChangeEvent | ✅ 完成 |

## 5. 核心实体

### ForumSection（论坛版块）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 版块名称 |
| description | String | 版块简介 |
| sortOrder | Integer | 排序（越小越靠前） |
| visibilityScope | Integer | 可见范围：0-公开，1-登录可见 |
| postLevelLimit | Integer | 发帖最低等级 |
| status | Integer | 状态：0-禁用，1-启用 |

### ForumPost（论坛帖子）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| sectionId | Long | 版块ID |
| authorId | Long | 作者ID |
| title | String | 标题 |
| content | String | 内容 |
| status | Integer | 状态：0-草稿，1-已发布，2-审核中，3-已拒绝，4-已删除 |
| visibilityScope | Integer | 可见范围：0-公开，1-登录可见 |
| isTop | Integer | 是否置顶 |
| isEssence | Integer | 是否精华 |
| viewCount | Integer | 浏览数 |
| likeCount | Integer | 点赞数 |
| replyCount | Integer | 回复数 |
| collectCount | Integer | 收藏数 |
| shareCount | Integer | 分享数 |
| publishedAt | LocalDateTime | 发布时间 |

### ForumReply（论坛回复）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| postId | Long | 帖子ID |
| parentId | Long | 父回复ID |
| rootId | Long | 根回复ID |
| userId | Long | 用户ID |
| content | String | 回复内容 |
| status | Integer | 状态：1-正常，2-隐藏，3-删除，4-审核中 |
| floorNo | Integer | 楼层号 |
| likeCount | Integer | 点赞数 |
| replyCount | Integer | 子回复数 |

### ForumPostChannelLink（帖子与频道关联）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| forumPostId | Long | 论坛帖子ID |
| conversationId | Long | 频道会话ID |
| linkType | String | 关联方式：manual_share |
| linkedBy | Long | 关联人ID |
| linkedAt | LocalDateTime | 关联时间 |
| status | Integer | 状态：0-失效，1-正常 |

## 6. 扩展方向

- **帖子审核**：提交 → 审核中 → 已发布/已拒绝
- **分级权限**：版块发帖等级限制
- **热门排序**：综合热度算法（浏览/点赞/回复）
- **举报处理**：用户举报 → 治理端处理
- **消息通知**：回复提醒、精华提醒、删除提醒

## 7. 相关文档

- [Forum 数据模型](./forum-data-model.md)
- [Forum 核心流程](./forum-core-flow.md)
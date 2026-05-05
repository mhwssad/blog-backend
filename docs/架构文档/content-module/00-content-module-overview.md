# Content 模块总览

## 1. 模块定位

Content 模块（`module/content`）是博客后端的内容管理层，负责：

- **收藏管理**：用户收藏夹与收藏记录维护
- **评论管理**：文章评论、点赞、回复树
- **分类标签**：文章分类树、标签管理
- **用户足迹**：用户浏览历史记录
- **互动记录**：用户点赞等互动行为
- **友情链接**：前台友链展示与后台管理

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   /api/user/comments    /api/user/collections                   │
│   /api/public/categories  /api/public/tags                      │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│  UserCommentController    UserCollectionController              │
│  PublicCommentController   PublicCategoryController             │
│  ContentCommentAdminController  ContentCollectionAdminController│
│  ContentCategoryAdminController  ContentTagAdminController       │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  UserCommentService      UserCollectionService                  │
│  CommentAdminService     CollectionAdminService                 │
│  CategoryAdminService    TagAdminService                        │
│  PublicContentQueryService                                      │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │   Article       │   │   Notification  │
│   数据访问        │   │   Service       │   │   Service       │
└─────────────────┘   │  (跨模块调用)    │   │  (通知发送)      │
          │           └─────────────────┘   └─────────────────┘
          ▼
┌─────────────────┐
│     MySQL       │
│   持久化存储      │
└─────────────────┘
```

## 3. 目录结构

```
module/content/
├── collection/           # 收藏子域
│   ├── controller/       # 用户收藏 + 后台管理控制器
│   ├── model/           # 请求/响应模型
│   │   ├── admin/       # 后台管理
│   │   └── user/        # 用户侧
│   ├── repository/     # 数据访问层
│   └── service/         # 用户收藏 + 后台管理服务
├── comment/             # 评论子域
│   ├── controller/     # 用户评论 + 后台管理 + 公开接口
│   ├── model/          # 请求/响应模型
│   │   ├── admin/
│   │   ├── publics/
│   │   └── user/
│   ├── repository/     # 数据访问层
│   └── service/        # 用户评论 + 评论管理服务
├── taxonomy/           # 分类标签子域
│   ├── controller/     # 分类管理 + 公开接口
│   ├── model/          # 请求/响应模型
│   │   ├── admin/
│   │   └── publics/
│   ├── repository/     # 数据访问层
│   └── service/        # 分类 + 标签管理服务
├── footprint/         # 用户足迹子域
│   ├── controller/
│   ├── model/
│   ├── repository/
│   └── service/
├── interaction/       # 互动记录子域
│   ├── controller/
│   ├── model/
│   ├── repository/
│   └── service/
├── friendlink/       # 友情链接子域
│   ├── controller/
│   ├── convert/
│   ├── model/
│   ├── repository/
│   └── service/
└── shared/           # 共享服务
    ├── convert/      # MapStruct 转换器
    └── service/      # 公共内容查询服务
```

## 4. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 用户收藏夹 | UserCollectionService | ✅ 完成 |
| 收藏记录管理 | UserCollectionService | ✅ 完成 |
| 评论发表/删除 | UserCommentService | ✅ 完成 |
| 评论点赞/取消 | UserCommentService | ✅ 完成 |
| 评论树形结构 | PublicCommentController | ✅ 完成 |
| 后台评论审核 | CommentAdminService | ✅ 完成 |
| 分类树维护 | CategoryAdminService | ✅ 完成 |
| 标签管理 | TagAdminService | ✅ 完成 |
| 用户足迹记录 | UserFootprintService | ✅ 完成 |
| 互动记录管理 | InteractionAdminService | ✅ 完成 |
| 友情链接展示 | PublicFriendLinkService | ✅ 完成 |
| 后台友链管理 | FriendLinkAdminService | ✅ 完成 |
| 公共内容查询 | PublicContentQueryService | ✅ 完成 |

## 5. 数据模型

### 核心实体

| 实体 | 说明 | 关键字段 |
|------|------|----------|
| `SysCollection` | 收藏记录 | userId, folderId, targetId, targetType |
| `SysCollectionFolder` | 收藏夹 | userId, folderName, folderType, isDefault |
| `SysComment` | 评论 | targetId, targetType, userId, rootId, parentId, status |
| `SysTag` | 标签 | name, color |
| `SysCategory` | 分类 | parentId, name, code, ancestors, level |
| `SysUserFootprint` | 用户足迹 | userId, targetId, targetType, visitedAt |
| `SysInteraction` | 互动记录 | userId, targetId, targetType, actionType |
| `BlogFriendLink` | 友情链接 | name, url, status, sortOrder |

### 关系图

```
用户 ──┬── 收藏夹(SysCollectionFolder) ──┬── 收藏记录(SysCollection)
       │                                  └── 关联文章
       │
       ├── 评论(SysComment) ──┬── 根评论
       │                      └── 回复(通过rootId/parentId)
       │
       ├── 互动(SysInteraction) ──── actionType: like
       │
       └── 足迹(SysUserFootprint)

文章 ──┬── 分类(BlogArticleCategory) ─── SysCategory(树形)
       │
       └── 标签(SysTagRelation) ──────── SysTag
```

## 6. 扩展方向

- **收藏公开**：支持收藏夹公开/私密控制
- **评论楼层**：支持嵌套楼层的评论展示
- **足迹分析**：用户行为分析
- **互动统计**：互动趋势分析

## 7. 相关文档

- [收藏模块流程](./content-collection-flow.md)
- [评论模块流程](./content-comment-flow.md)
- [分类标签流程](./content-taxonomy-flow.md)
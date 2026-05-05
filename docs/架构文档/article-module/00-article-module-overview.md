# Article 模块总览

## 1. 模块定位

Article 模块（`module/article`）是博客后端的核心内容服务层，负责：

- **文章管理**：博客文章的创建、编辑、发布、下架、删除
- **内容组织**：文章分类、标签关联、系列文集
- **访问控制**：多级访问权限（公开、登录可见、密码访问、白名单）
- **审核流程**：文章送审、审核通过/拒绝
- **定时发布**：支持定时自动发布
- **互动统计**：点赞、评论、收藏、分享计数
- **RAG 集成**：文章内容同步至 AI 知识库

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   GET /api/articles           GET /api/user/articles           │
│   POST /api/user/articles/review  PUT /api/user/articles/{id}/access
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │PublicArticle    │  │UserArticle       │  │ArticleAdmin     ││
│  │Controller       │  │ManageController  │  │Controller       ││
│  │ 前台文章浏览     │  │ 用户文章管理      │  │ 后台文章管理     ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
│  ┌─────────────────┐  ┌─────────────────┐                       │
│  │UserArticle      │  │ArticleReview    │                       │
│  │ActionController │  │AdminController  │                       │
│  │ 用户互动        │  │ 后台审核        │                       │
│  └─────────────────┘  └─────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │PublicArticle    │  │UserArticle      │  │ArticleAdmin     ││
│  │Service          │  │ManageService     │  │Service          ││
│  │ 前台文章查询     │  │ 用户文章管理      │  │ 后台管理服务     ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │ArticleStatus    │  │ArticleAccess    │  │ArticleSeries    ││
│  │Machine          │  │ControlService   │  │Service          ││
│  │ 状态机          │  │ 访问控制         │  │ 系列管理         ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │   内容模块       │   │   任务调度       │
│   数据访问       │   │   分类/标签     │   │   定时发布       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  SysCategory    │   │  Scheduled      │
│  blog_article   │   │  SysTag         │   │  PublishTask    │
│  blog_article_* │   │  SysInteraction  │   │                 │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 前台文章浏览 | PublicArticleService | ✅ 完成 |
| 用户文章管理 | UserArticleManageService | ✅ 完成 |
| 用户文章互动 | UserArticleActionService | ✅ 完成 |
| 文章审核流程 | UserArticleReviewService | ✅ 完成 |
| 后台文章管理 | ArticleAdminService | ✅ 完成 |
| 后台文章审核 | ArticleReviewAdminService | ✅ 完成 |
| 文章访问控制 | ArticleAccessControlService | ✅ 完成 |
| 文章系列管理 | ArticleSeriesService | ✅ 完成 |
| 定时发布 | ScheduledPublishTask | ✅ 完成 |
| RAG 知识同步 | ArticleContentFacadeService | ✅ 完成 |

## 4. 接口概览

### 前台接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 分页查询已发布文章 | `GET /api/articles` | 支持分类、标签、作者筛选 |
| 查询文章详情 | `GET /api/articles/{id}` | 需通过访问控制校验 |
| 分页查询我的文章 | `GET /api/user/articles` | 需登录 |
| 查询我的文章详情 | `GET /api/user/articles/{id}` | 需登录且为作者 |
| 送审文章 | `POST /api/user/articles/{id}/review` | 提交审核 |
| 撤回审核 | `DELETE /api/user/articles/{id}/review` | 撤回待审文章 |
| 配置文章访问 | `PUT /api/user/articles/{id}/access` | 配置访问白名单 |
| 分页查询我的系列 | `GET /api/user/articles/series` | 系列列表 |
| 查询系列详情 | `GET /api/user/articles/series/{id}` | 系列及文章列表 |

### 后台接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 分页查询文章 | `GET /api/sys/articles` | 支持多条件筛选 |
| 查询文章详情 | `GET /api/sys/articles/{id}` | 含完整信息 |
| 新增文章 | `POST /api/sys/articles` | 含分类/标签设置 |
| 修改文章 | `PUT /api/sys/articles/{id}` | 含分类/标签更新 |
| 修改文章状态 | `PUT /api/sys/articles/{id}/status` | 发布/下架 |
| 配置访问权限 | `PUT /api/sys/articles/{id}/access` | 后台配置访问 |
| 切换置顶 | `PUT /api/sys/articles/{id}/top` | 置顶/取消 |
| 切换推荐 | `PUT /api/sys/articles/{id}/recommend` | 推荐/取消 |
| 删除文章 | `DELETE /api/sys/articles/{id}` | 软删除 |
| 分页查询待审核 | `GET /api/sys/articles/review` | 审核列表 |
| 审核通过 | `POST /api/sys/articles/{id}/approve` | 审核通过 |
| 审核拒绝 | `POST /api/sys/articles/{id}/reject` | 审核拒绝 |
| 分页查询系列 | `GET /api/sys/articles/series` | 后台系列管理 |
| 新增系列 | `POST /api/sys/articles/series` | 创建系列 |
| 修改系列 | `PUT /api/sys/articles/series/{id}` | 更新系列 |
| 删除系列 | `DELETE /api/sys/articles/series/{id}` | 删除系列 |

## 5. 扩展方向

- **评论楼中楼**：支持多级评论回复
- **文章草稿箱**：多版本草稿管理
- **文章回收站**：软删除与恢复
- **访问日志**：详细的文章访问审计
- **内容推荐**：基于标签和阅读历史的智能推荐

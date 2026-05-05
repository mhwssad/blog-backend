# Article 模块架构文档

本文档收集了 Article（文章）模块完整的架构设计文档。

## 文档索引

| 文档 | 说明 |
|------|------|
| [00-article-module-overview.md](./00-article-module-overview.md) | Article 模块总览：定位、分层、目录结构、能力矩阵 |
| [article-data-model.md](./article-data-model.md) | Article 数据模型：数据库表结构、实体映射、枚举定义 |
| [article-access-control.md](./article-access-control.md) | Article 访问控制：访问级别、可见范围、权限校验 |
| [article-status-flow.md](./article-status-flow.md) | Article 状态流转：状态机、审核流程、发布调度 |

## 核心能力概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Article 模块能力                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   文章管理        │  │   文章审核        │  │   文章系列        │          │
│  │   CRUD/分页      │  │   送审/通过/拒绝  │  │   专栏/文集       │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   访问控制        │  │   定时发布       │  │   互动统计        │          │
│  │   公开/登录/密码  │  │   定时任务调度   │  │   点赞/评论/收藏  │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   前台文章浏览    │  │   用户文章管理    │  │   后台文章管理    │          │
│  │   列表/详情       │  │   我的文章/访问   │  │   审核/置顶/推荐  │          │
│  │   ✅ 完成         │  │   ✅ 完成        │  │   ✅ 完成        │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐                                                      │
│  │   RAG 知识同步     │                                                      │
│  │   文章->知识库     │                                                      │
│  │   ✅ 完成         │                                                      │
│  └──────────────────┘                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 模块定位

Article 模块（`module/article`）是博客后端的核心内容服务层，负责：

- **文章管理**：博客文章的创建、编辑、发布、下架
- **内容组织**：文章分类、标签关联、系列文集
- **访问控制**：多级访问权限（公开、登录可见、密码访问、白名单）
- **审核流程**：文章送审、审核通过/拒绝
- **定时发布**：支持定时自动发布
- **互动统计**：点赞、评论、收藏、分享计数
- **RAG 集成**：文章内容同步至 AI 知识库

## 目录结构

```
module/article/
├── controller/              # 控制器层
│   ├── PublicArticleController.java        # 前台文章接口
│   ├── UserArticleManageController.java     # 用户文章管理接口
│   ├── UserArticleActionController.java    # 用户文章互动接口
│   ├── UserArticleSeriesController.java    # 用户系列管理接口
│   ├── UserArticleReviewController.java     # 用户文章审核接口
│   ├── ArticleAdminController.java          # 后台文章管理接口
│   ├── ArticleReviewAdminController.java    # 后台文章审核接口
│   └── PublicArticleSeriesController.java   # 前台系列接口
├── service/                  # 业务服务层
│   ├── PublicArticleService.java           # 前台文章服务
│   ├── UserArticleManageService.java       # 用户文章管理服务
│   ├── UserArticleActionService.java       # 用户文章互动服务
│   ├── UserArticleSeriesService.java       # 用户系列服务
│   ├── UserArticleReviewService.java        # 用户文章审核服务
│   ├── ArticleAdminService.java             # 后台文章管理服务
│   ├── ArticleAdminCrudService.java         # 后台文章增删改服务
│   ├── ArticleAdminModerationService.java   # 后台审核服务
│   ├── ArticleReviewAdminService.java       # 后台审核管理服务
│   ├── ArticleAccessControlService.java    # 访问控制服务
│   ├── ArticleAccessManageService.java      # 访问权限管理服务
│   ├── ArticleContentFacadeService.java    # 内容门面服务
│   ├── ArticleProfileQueryService.java     # 文章画像查询服务
│   ├── ArticleSeriesItemService.java       # 系列文章项服务
│   ├── ArticleSeriesService.java            # 系列服务
│   ├── ArticleStatusMachine.java            # 状态机
│   └── impl/                                # 服务实现
├── repository/              # 数据访问层
│   ├── BlogArticleRepository.java          # 文章 Repository
│   ├── BlogArticleAccessRepository.java     # 文章访问权限 Repository
│   ├── BlogArticleCategoryRepository.java   # 文章分类 Repository
│   ├── BlogArticleReviewLogRepository.java  # 审核日志 Repository
│   ├── BlogArticleSeriesRepository.java     # 系列 Repository
│   ├── BlogArticleSeriesItemRepository.java # 系列文章项 Repository
│   └── impl/                                # MyBatis-Plus 实现
├── convert/                 # MapStruct 转换器
├── model/                   # 请求/响应模型
│   ├── admin/              # 后台管理请求/VO
│   ├── user/               # 用户侧请求/VO
│   ├── publics/            # 前台公开请求/VO
│   ├── common/             # 通用模型
│   └── internal/           # 内部数据传输对象
└── task/                   # 定时任务
    └── ScheduledPublishTask.java            # 定时发布任务
```

## 快速导航

### 开发者视角

1. **理解文章状态流转** → 参考 [article-status-flow.md](./article-status-flow.md)
2. **理解访问控制机制** → 参考 [article-access-control.md](./article-access-control.md)
3. **查看数据表结构** → 参考 [article-data-model.md](./article-data-model.md)
4. **新增文章接口** → 参考 `ArticleAdminController`
5. **扩展审核流程** → 参考 `UserArticleReviewService` / `ArticleReviewAdminService`
6. **理解系列功能** → 参考 `ArticleSeriesService`

### 运营视角

1. **管理文章** → 后台 `/api/sys/articles`
2. **审核文章** → 后台 `/api/sys/articles/review`
3. **管理系列** → 后台 `/api/sys/articles/series`
4. **前台浏览** → `/api/articles`
5. **用户文章** → `/api/user/articles`

## 相关文档

- [项目代码编写规范](../../项目代码编写规范.md)
- [项目结构规范](../../项目结构规范.md)
- [AI 模块架构文档](../ai-module/README.md)

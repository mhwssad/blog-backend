# Content 模块待办清单

本文档用于收口 content 模块接下来要持续推进的任务，避免"知道有缺口，但每次都重新梳理一遍"。本清单按 2026-03-30 当前代码状态整理。

## 1. 模块结构概览

```
module/content/
├── controller/
│   ├── CategoryAdminController.java      (6个端点: 树/详情/创建/修改/状态/删除)
│   ├── TagAdminController.java           (5个端点: 列表/详情/创建/修改/删除)
│   ├── CommentAdminController.java       (4个端点: 分页/详情/状态/删除)
│   ├── CollectionAdminController.java    (3个端点: 收藏夹分页/收藏分页/删除)
│   ├── InteractionAdminController.java   (2个端点: 分页/删除)
│   ├── FootprintAdminController.java     (3个端点: 分页/删除/批量清理)
│   ├── PublicContentController.java      (3个端点: 分类树/标签列表/评论树)
│   ├── UserCommentController.java        (4个端点: 创建/删除/点赞/取消点赞)
│   ├── UserCollectionController.java     (7个端点: 收藏夹CRUD/收藏分页/收藏/取消收藏)
│   └── UserFootprintController.java      (3个端点: 分页/删除/清空)
├── convert/             (ContentModelMapper MapStruct映射器)
├── service/
│   ├── CategoryAdminService.java         (6个方法: 树/详情/创建/修改/状态/删除)
│   ├── TagAdminService.java              (5个方法: 列表/详情/创建/修改/删除)
│   ├── CommentAdminService.java          (4个方法: 分页/详情/状态/删除)
│   ├── CollectionAdminService.java       (3个方法: 收藏夹分页/收藏分页/删除)
│   ├── InteractionAdminService.java      (2个方法: 分页/删除)
│   ├── FootprintAdminService.java        (3个方法: 分页/删除/批量清理)
│   ├── PublicContentQueryService.java    (3个方法: 分类树/标签列表/评论列表)
│   ├── UserCommentService.java           (4个方法: 创建/删除/点赞/取消点赞)
│   ├── UserCollectionService.java        (7个方法: 收藏夹CRUD/收藏分页/收藏/取消收藏)
│   ├── UserFootprintService.java         (4个方法: 分页/删除/清空/记录浏览)
│   ├── SysCategoryService.java           (基础仓储, extends IService)
│   ├── SysTagService.java                (基础仓储, extends IService)
│   ├── SysTagRelationService.java        (基础仓储, extends IService)
│   ├── SysCommentService.java            (基础仓储, extends IService)
│   ├── SysCollectionFolderService.java   (基础仓储, extends IService)
│   ├── SysCollectionService.java         (基础仓储, extends IService)
│   ├── SysInteractionService.java        (基础仓储, extends IService)
│   └── SysUserFootprintService.java      (基础仓储, extends IService)
├── impl/                (18个实现: 10个管理/用户服务 + 8个基础仓储空壳)
└── model/
    ├── admin/           (12个DTO: 分类/标签/评论/收藏夹/互动/足迹的查询/保存/VO)
    ├── publics/         (3个DTO: 分类树VO/标签VO/评论VO)
    └── user/            (5个DTO: 评论请求/收藏夹请求/收藏请求/分页查询)
```

数据库: 8张表 (`sys_category` / `sys_tag` / `sys_tag_relation` / `sys_comment` / `sys_collection_folder` /
`sys_collection` / `sys_interaction` / `sys_user_footprint`)

## 2. 功能完成度评估

### 2.1 接口方法完成情况

| 服务                        | 方法数  | 已实现 | 状态                   |
|---------------------------|------|-----|----------------------|
| CategoryAdminService      | 6    | 6   | ✅ 全部完成               |
| TagAdminService           | 5    | 5   | ✅ 全部完成               |
| CommentAdminService       | 4    | 4   | ✅ 全部完成               |
| CollectionAdminService    | 3    | 3   | ✅ 全部完成               |
| InteractionAdminService   | 2    | 2   | ✅ 全部完成               |
| FootprintAdminService     | 3    | 3   | ✅ 全部完成               |
| PublicContentQueryService | 3    | 3   | ✅ 全部完成               |
| UserCommentService        | 4    | 4   | ✅ 全部完成               |
| UserCollectionService     | 7    | 7   | ✅ 全部完成               |
| UserFootprintService      | 4    | 4   | ✅ 全部完成               |
| 基础仓储(8个)                  | 0自定义 | 全部  | ✅ MyBatis-Plus标准CRUD |

**结论: content 模块所有接口方法均已完整实现，不存在缺失的方法。**

### 2.2 Mapper XML 自定义查询

| Mapper                 | 自定义查询                                                             |
|------------------------|-------------------------------------------------------------------|
| SysCategoryMapper      | `selectTreeByType` - 按类型查分类树                                      |
| SysTagMapper           | `selectByTargetType` - 按目标类型查标签                                   |
| SysCommentMapper       | `selectRootCommentsByTarget` + `selectRepliesByRootIds` - 评论树两段查询 |
| SysCollectionMapper    | `selectUserCollectionPage` - 用户收藏分页                               |
| SysInteractionMapper   | `existsUserAction` - 互动存在性检查                                      |
| SysUserFootprintMapper | `upsertFootprint` - 浏览足迹 UPSERT                                   |

## 3. 本轮已完成

- [x] 实现后台分类/标签/评论/收藏/互动/足迹管理控制器。
- [x] 实现公开分类/标签/评论查询接口。
- [x] 实现用户评论/收藏/足迹接口。
- [x] 将内容域 DTO 转换收口到 `ContentModelMapper`。
- [x] 补入评论创建/删除服务级测试（访问控制校验/父评论回复数同步/子树删除/文章评论数回退）。
- [x] 补入收藏服务级测试（默认收藏夹自动创建/收藏夹数量与文章收藏数同步回写/删除计数回退）。
- [x] 将 `UserCollectionServiceImpl` 中默认收藏夹与收藏记录的实体构建迁移到 `ContentModelMapper`。
- [x] 将 `TagAdminServiceImpl` 删除标签流程改为事务内清理 `sys_tag_relation`，避免遗留失效关联。
- [x] 将 `PublicContentQueryServiceImpl` 评论树查询切回 `SysCommentMapper` 两段查询，并在 XML 中显式过滤 `status=1`。
- [x] 补入 `TagAdminServiceImpl` 服务级测试（列表/详情/创建/更新/删除及名称唯一性、关联关系边界）。
- [x] 补入 `CommentAdminServiceImpl` 服务级测试（分页/详情/状态切换/子树删除及评论数、回复数回退）。
- [x] 补入 `CollectionAdminServiceImpl` 服务级测试（收藏夹分页/收藏分页/删除及收藏夹、文章计数回退）。
- [x] 补入 `InteractionAdminServiceImpl` 服务级测试（互动分页/删除及文章、评论点赞计数回退）。
- [x] 补入 `FootprintAdminServiceImpl` 服务级测试（足迹分页/删除/批量清理）。
- [x] 补入 `UserFootprintServiceImpl` 服务级测试（用户足迹分页/删除/清空及文章浏览足迹 UPSERT）。
- [x] 补入 `PublicContentQueryServiceImpl` 服务级测试（分类树/标签过滤/评论树组装及点赞状态回填）。
- [x] 补入内容安全配置集成测试（公开端点暴露验证/权限SQL范围校验）。
- [x] 补入分类管理控制器测试（树查询端点）。
- [x] 补入收藏控制器测试（收藏夹分页/收藏创建端点）。

## 4. 现有测试文件 (13个)

| 测试文件                                | 覆盖范围                            |
|-------------------------------------|---------------------------------|
| `UserCommentServiceImplTest`        | 评论创建/删除 + 点赞/取消点赞幂等、计数同步与回退     |
| `UserCollectionServiceImplTest`     | 收藏创建/删除 + 收藏夹创建/更新/删除与收藏分页      |
| `TagAdminServiceImplTest`           | 标签列表/详情/创建/更新/删除 + 名称唯一性/关联关系清理 |
| `CommentAdminServiceImplTest`       | 评论分页/详情/状态切换/子树删除 + 用户信息回填/计数回退 |
| `CollectionAdminServiceImplTest`    | 收藏夹分页/收藏分页/删除 + 收藏夹数量与文章收藏数回退   |
| `InteractionAdminServiceImplTest`   | 互动分页/删除 + 文章/评论点赞计数回退           |
| `FootprintAdminServiceImplTest`     | 足迹分页/删除/批量清理                    |
| `UserFootprintServiceImplTest`      | 用户足迹分页/删除/清空 + 文章浏览足迹 UPSERT    |
| `PublicContentQueryServiceImplTest` | 分类树/标签过滤/评论树组装 + 用户信息与点赞状态回填    |
| `CategoryAdminServiceImplTest`      | 分类详情/创建/更新/状态/删除 + 层级路径刷新       |
| `ContentSecurityIntegrationTest`    | 公开端点暴露/权限SQL范围                  |
| `CategoryAdminServiceTest`          | 分类树查询(控制器层)                     |
| `UserCollectionServiceTest`         | 收藏夹分页/收藏创建(控制器层)                |

## 5. 下一批高优先级

### 5.1 管理服务服务级测试收口 (已完成)

- [x] **TagAdminServiceImpl** 服务级测试
    - [x] `listTags` - 标签列表查询
    - [x] `getTag` - 标签详情
    - [x] `createTag` - 创建标签（名称唯一性校验）
    - [x] `updateTag` - 更新标签
    - [x] `deleteTag` - 删除标签（关联关系清理）

- [x] **CommentAdminServiceImpl** 服务级测试
    - [x] `pageComments` - 评论分页+过滤
    - [x] `getComment` - 评论详情
    - [x] `updateStatus` - 状态切换
    - [x] `deleteComment` - 删除评论（子树级联）

- [x] **CollectionAdminServiceImpl** 服务级测试
    - [x] `pageFolders` - 收藏夹分页
    - [x] `pageCollections` - 收藏分页
    - [x] `deleteCollection` - 删除收藏

- [x] **InteractionAdminServiceImpl** 服务级测试
    - [x] `pageInteractions` - 互动分页
    - [x] `deleteInteraction` - 删除互动

- [x] **FootprintAdminServiceImpl** 服务级测试
    - [x] `pageFootprints` - 足迹分页
    - [x] `deleteFootprint` - 删除足迹
    - [x] `cleanFootprints` - 批量清理

### 5.2 用户/公开服务服务级测试收口 (已完成)

- [x] **UserFootprintServiceImpl** 服务级测试
    - [x] `pageFootprints` - 用户足迹分页
    - [x] `deleteFootprint` - 删除足迹
    - [x] `clearFootprints` - 清空足迹
    - [x] `recordArticleFootprint` - 记录浏览足迹（UPSERT逻辑）

- [x] **PublicContentQueryServiceImpl** 服务级测试
    - [x] `listCategoryTree` - 分类树构建（层级/排序）
    - [x] `listTags` - 标签列表（按目标类型过滤）
    - [x] `listComments` - 评论树构建（根评论+回复两段查询）

### 5.3 部分覆盖的服务补充 (已完成)

- [x] **CategoryAdminServiceImpl** 服务级测试 (仅树查询已测)
    - [x] `getCategory` - 分类详情
    - [x] `createCategory` - 创建分类（树路径/层级计算）
    - [x] `updateCategory` - 更新分类
    - [x] `updateStatus` - 状态切换
    - [x] `deleteCategory` - 删除分类（子分类检查/文章绑定检查）

- [x] **UserCommentServiceImpl** 补充测试
    - [x] `likeComment` - 评论点赞（幂等+计数同步）
    - [x] `unlikeComment` - 取消点赞（幂等+计数回退）

- [x] **UserCollectionServiceImpl** 补充测试
    - [x] `createFolder` - 创建收藏夹
    - [x] `updateFolder` - 更新收藏夹
    - [x] `deleteFolder` - 删除收藏夹（默认夹不可删除）
    - [x] `pageCollections` - 收藏分页

## 6. 中期一致性补强

- [x] 核对分类删除时的文章绑定检查逻辑（有文章绑定时拒绝删除）
- [x] 核对标签删除时的关联关系清理（删除标签时同步清理 `sys_tag_relation` 关联）
- [x] 核对收藏夹删除时的收藏记录处理策略
- [x] 核对评论点赞幂等性（重复点赞不重复创建互动记录）
- [x] 核对足迹 UPSERT 在并发访问时的行为（依赖 `uk_user_target` 唯一键 + `ON DUPLICATE KEY UPDATE` 收口并发）
- [x] 核对评论树两段查询在大数据量下的性能表现（公开评论查询已切回根评论 + 回复两段读取）
- [x] 补充高成本方法的 Javadoc 注释（已补评论子树删除、标签删除清理、公开评论查询与足迹 UPSERT 说明）

## 7. 中长期基础设施

- [x] 评估是否需要评论内容审核/敏感词过滤接入
  结论：当前评论入口仍受登录态与文章访问控制约束，先不在 `content` 侧直接接入审核链路；若后续开放匿名评论、违规反馈增多或需要后台审核台，优先复用
  `chat` 域“系统配置敏感词 + 命中留痕”模式扩展为内容审核服务。
- [x] 评估是否需要分类树缓存（频繁查询场景）
  结论：当前公开分类树是单表读取 + 内存组装，查询成本低，暂不引入 Redis 缓存；若首页导航成为明显热点或分类层级、数量显著增长，再按
  `type` 维度缓存，并在分类增删改/状态变更时统一失效。
- [x] 评估是否需要足迹归档策略（数据量增长后）
  结论：当前 `sys_user_footprint` 依赖 `uk_user_target`
  唯一键，同一用户同一目标只保留最新记录，短期先复用现有后台清理能力；当表规模增长到需要长期保留审计/运营分析时，再拆冷热表或按月份归档并补
  retention 配置。
- [x] 评估是否需要互动统计聚合（如文章总互动数查询优化）
  结论：当前文章/评论点赞数已冗余在业务表，`sys_interaction`
  主要承担幂等校验和明细查询，暂不追加总互动聚合层；若后续出现作者看板、排行榜或跨动作综合统计，再引入离线聚合或冗余总互动字段。

## 8. 完成标志

- 分类/标签/评论/收藏/互动/足迹后台管理都具备服务级回归覆盖。
- 用户评论/收藏/足迹操作都具备基础自动化验证。
- 公开内容查询具备基础验证。
- 级联删除、计数同步、幂等操作等关键逻辑不再只依赖人工联调验证。



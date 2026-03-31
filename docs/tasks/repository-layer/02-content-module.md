# Content 模块 Repository 迁移计划

## 模块信息

- **优先级**：第2轮
- **复杂度**：中
- **前置依赖**：无（数据提供方，被其他模块依赖）
- **涉及薄服务**：8个
- **涉及业务服务**：10个
- **数据访问总数**：约78处

## 当前进展（2026-03-31）

- [x] 已新增 `SysUserFootprintRepository` 与 `SysUserFootprintRepositoryImpl`，先收口用户足迹路径的 Repository 访问入口。
- [x] 已将 `UserFootprintServiceImpl` 改为注入 `SysUserFootprintRepository`，分页、按用户清空和文章浏览足迹 UPSERT 均改走 Repository。
- [x] 已将 `FootprintAdminServiceImpl` 改为注入 `SysUserFootprintRepository`，管理端分页、按条件批量删除和按 ID 删除均改走 Repository。
- [x] 已移除 `UserFootprintServiceImpl` 对 `SysUserFootprintMapper` 的直接注入，并同步更新 `UserFootprintServiceImplTest`。
- [x] 已新增 `SysTagRepository`、`SysTagRelationRepository`，并将 `TagAdminServiceImpl` 迁到 Repository，标签列表、名称唯一性校验和标签关联清理都已收口到 Repository。
- [x] 已新增 `SysCategoryRepository`，并将 `CategoryAdminServiceImpl` 迁到 Repository，分类树查询、分类编码唯一性校验、子分类查询和是否存在子分类判断都已收口到 Repository。
- [ ] 其余 content 业务服务的 Repository 迁移仍待继续。

## Repository 列表

| Repository 接口 | 对应实体 | 薄服务来源 | 自定义方法 |
|---|---|---|---|
| `SysCategoryRepository` | SysCategory | SysCategoryService | 无 |
| `SysTagRepository` | SysTag | SysTagService | 无 + 包装Mapper XML |
| `SysTagRelationRepository` | SysTagRelation | SysTagRelationService | 无 |
| `SysCommentRepository` | SysComment | SysCommentService | 包装Mapper XML |
| `SysInteractionRepository` | SysInteraction | SysInteractionService | 包装Mapper XML |
| `SysCollectionRepository` | SysCollection | SysCollectionService | 无 |
| `SysCollectionFolderRepository` | SysCollectionFolder | SysCollectionFolderService | 无 |
| `SysUserFootprintRepository` | SysUserFootprint | SysUserFootprintService | 包装Mapper XML |

## 各 Repository 方法设计

### SysCategoryRepository

```java
public interface SysCategoryRepository extends IService<SysCategory> {
    List<SysCategory> findByTypeOrderBySortOrderAndId(String type);
    List<SysCategory> findByTypeAndStatusOrderBySortOrderAndId(String type, Integer status);
    List<SysCategory> findByParentId(Long parentId);
    boolean existsByParentId(Long parentId);
    boolean existsByTypeAndCodeExcludingId(String type, String code, Long excludeId);
}
```

| 方法 | 来源 | 行号 |
|---|---|---|
| `findByTypeOrderBySortOrderAndId` | CategoryAdminServiceImpl:42, `sysCategoryService.lambdaQuery()...list()` | 按类型查分类列表 |
| `findByTypeAndStatusOrderBySortOrderAndId` | PublicContentQueryServiceImpl:55, `sysCategoryService.lambdaQuery().eq(status,1)...list()` | 公开接口查可用分类 |
| `findByParentId` | CategoryAdminServiceImpl:161, `sysCategoryService.lambdaQuery().eq(parentId)...list()` | 查子分类 |
| `existsByParentId` | CategoryAdminServiceImpl:90, `sysCategoryService.lambdaQuery()...exists()` | 检查是否有子分类 |
| `existsByTypeAndCodeExcludingId` | CategoryAdminServiceImpl:102, `sysCategoryService.lambdaQuery().eq(code).ne(id).exists()` | 唯一性校验 |

> `save`、`updateById`、`getById`、`removeById` 继承自 IService，无需额外定义。

### SysTagRepository

```java
public interface SysTagRepository extends IService<SysTag> {
    List<SysTag> findAllOrderByIdDesc();
    boolean existsByNameExcludingId(String name, Long excludeId);
    List<SysTag> findByTargetType(String targetType); // 包装SysTagMapper XML
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `findAllOrderByIdDesc` | TagAdminServiceImpl:34, `lambdaQuery().orderByDesc(id).list()` | 管理端标签列表 |
| `existsByNameExcludingId` | TagAdminServiceImpl:79, `lambdaQuery().eq(name).ne(id).exists()` | 标签名唯一性校验 |
| `findByTargetType` | PublicContentQueryServiceImpl:83, **直接注入 SysTagMapper** | 包装XML多表查询 |

### SysTagRelationRepository

```java
public interface SysTagRelationRepository extends IService<SysTagRelation> {
    boolean existsByTagId(Long tagId);
    boolean removeByTagId(Long tagId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `existsByTagId` | TagAdminServiceImpl:70, `lambdaQuery().eq(tagId).exists()` | 删除标签前检查关联 |
| `removeByTagId` | TagAdminServiceImpl:70, `remove(new LambdaQueryWrapper)` | 删除标签时清理标签关联 |

### SysCommentRepository

```java
public interface SysCommentRepository extends IService<SysComment> {
    Page<SysComment> pageByAdminConditions(CommentAdminPageQuery query);
    List<SysComment> findByTargetTypeAndTargetId(String targetType, Long targetId);
    List<SysComment> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAndId(String targetType, Long targetId, Integer status);
    List<SysComment> selectRootCommentsByTarget(Long targetId, String targetType); // XML
    List<SysComment> selectRepliesByRootIds(List<Long> rootIds); // XML
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `pageByAdminConditions` | CommentAdminServiceImpl:46, `new LambdaQueryWrapper<>() + page()` | 管理端多条件分页 |
| `findByTargetTypeAndTargetId` | CommentAdminServiceImpl:114, UserCommentServiceImpl:159, `lambdaQuery().eq(targetType).eq(targetId).list()` | 按目标查评论列表 |
| `findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAndId` | PublicContentQueryServiceImpl:90, `lambdaQuery().eq(status,1)...list()` | 公开接口查可用评论 |
| `selectRootCommentsByTarget` | SysCommentMapper XML | 多表关联查根评论 |
| `selectRepliesByRootIds` | SysCommentMapper XML | 批量查回复 |

### SysInteractionRepository

```java
public interface SysInteractionRepository extends IService<SysInteraction> {
    Page<SysInteraction> pageByAdminConditions(InteractionAdminPageQuery query);
    boolean existsByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId, Long targetId, String targetType, String actionType);
    SysInteraction findOneByUserIdAndTargetIdAndTargetTypeAndActionType(Long userId, Long targetId, String targetType, String actionType);
    boolean removeByTargetTypeAndTargetIds(String targetType, Collection<Long> targetIds);
    List<SysInteraction> findByUserIdAndTargetTypeAndActionTypeInTargetIds(Long userId, String targetType, String actionType, Collection<Long> targetIds);
    boolean existsUserAction(Long userId, Long targetId, String targetType, String actionType); // XML
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `pageByAdminConditions` | InteractionAdminServiceImpl:34, `LambdaQueryWrapper + page()` | 管理端分页 |
| `existsByUserIdAndTargetIdAndTargetTypeAndActionType` | UserCommentServiceImpl:50, `lambdaQuery()...exists()` | 检查是否已点赞 |
| `findOneByUserIdAndTargetIdAndTargetTypeAndActionType` | UserCommentServiceImpl:73, `lambdaQuery()...one()` | 查找已有点赞记录 |
| `removeByTargetTypeAndTargetIds` | UserCommentServiceImpl:137, `remove(new LambdaQueryWrapper)` | 级联删除关联互动 |
| `findByUserIdAndTargetTypeAndActionTypeInTargetIds` | PublicContentQueryServiceImpl:150, `lambdaQuery().in(targetIds).list()` | 批量查用户互动状态 |
| `existsUserAction` | SysInteractionMapper XML | XML查询 |

### SysCollectionRepository

```java
public interface SysCollectionRepository extends IService<SysCollection> {
    Page<SysCollection> pageByAdminConditions(CollectionAdminPageQuery query);
    Page<SysCollection> pageByUserId(Long userId, int current, int size);
    List<SysCollection> findByFolderId(Long folderId);
    boolean removeByFolderId(Long folderId);
    boolean existsByUserIdAndFolderIdAndTargetIdAndTargetType(Long userId, Long folderId, Long targetId, String targetType);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `pageByAdminConditions` | CollectionAdminServiceImpl:49, `LambdaQueryWrapper + page()` | 管理端分页 |
| `pageByUserId` | UserCollectionServiceImpl:121, `LambdaQueryWrapper + page()` | 用户收藏分页 |
| `findByFolderId` | UserCollectionServiceImpl:108, `lambdaQuery().eq(folderId).list()` | 查文件夹下收藏 |
| `removeByFolderId` | UserCollectionServiceImpl:113, `remove(wrapper)` | 删除文件夹下所有收藏 |
| `existsByUserIdAndFolderIdAndTargetIdAndTargetType` | UserCollectionServiceImpl:143, `lambdaQuery()...exists()` | 收藏重复检查 |

### SysCollectionFolderRepository

```java
public interface SysCollectionFolderRepository extends IService<SysCollectionFolder> {
    Page<SysCollectionFolder> pageByAdminConditions(CollectionAdminPageQuery query);
    Page<SysCollectionFolder> pageByUserIdOrderByDefaultAndSort(Long userId, int current, int size);
    SysCollectionFolder findDefaultByUserIdAndFolderType(Long userId, String folderType);
    List<SysCollectionFolder> findDefaultsByUserIdAndFolderType(Long userId, String folderType);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `pageByAdminConditions` | CollectionAdminServiceImpl:35, `LambdaQueryWrapper + page()` | 管理端分页 |
| `pageByUserIdOrderByDefaultAndSort` | UserCollectionServiceImpl:48, `LambdaQueryWrapper + page()` | 用户收藏夹分页 |
| `findDefaultByUserIdAndFolderType` | UserCollectionServiceImpl:182, `lambdaQuery().eq(isDefault,1).one()` | 查默认收藏夹 |
| `findDefaultsByUserIdAndFolderType` | UserCollectionServiceImpl:200, `lambdaQuery().eq(isDefault,1).list()` | 查所有默认收藏夹 |

### `SysUserFootprintRepository`

```java
public interface SysUserFootprintRepository extends IService<SysUserFootprint> {
    Page<SysUserFootprint> pageByAdminConditions(FootprintAdminPageQuery query);
    Page<SysUserFootprint> pageByUserIdAndTargetType(Long userId, String targetType, int current, int size);
    boolean removeByUserId(Long userId);
    boolean removeByAdminConditions(FootprintAdminPageQuery query);
    int upsertFootprint(SysUserFootprint footprint); // 包装SysUserFootprintMapper XML
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `pageByAdminConditions` | FootprintAdminServiceImpl:28, `LambdaQueryWrapper + page()` | 管理端分页 |
| `pageByUserIdAndTargetType` | UserFootprintServiceImpl:37, `LambdaQueryWrapper + page()` | 用户足迹分页 |
| `removeByUserId` | UserFootprintServiceImpl:60, `remove(wrapper)` | 清空用户足迹 |
| `removeByAdminConditions` | FootprintAdminServiceImpl:49, `remove(wrapper)` | 管理端批量删除 |
| `upsertFootprint` | UserFootprintServiceImpl:82, **直接注入 SysUserFootprintMapper** | 包装XML upsert |

## 跨模块依赖

### 被其他模块注入的服务（迁移后其他模块需同步修改）

本模块的薄服务被以下外部模块的业务服务注入：

| 薄服务 | 外部注入方 | 迁移后替换为 |
|---|---|---|
| SysCategoryService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysCategoryRepository |
| SysTagService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysTagRepository |
| SysTagRelationService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysTagRelationRepository |
| SysCommentService | ArticleAdminServiceImpl, PublicArticleServiceImpl, UserCommentServiceImpl(chat?) | SysCommentRepository |
| SysInteractionService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysInteractionRepository |
| SysCollectionService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysCollectionRepository |
| SysCollectionFolderService | ArticleAdminServiceImpl, PublicArticleServiceImpl | SysCollectionFolderRepository |
| SysUserFootprintService | ArticleAdminServiceImpl, PublicArticleServiceImpl | `SysUserFootprintRepository` |

> **注意**：article 模块第5轮迁移时需同步处理这些依赖替换。本模块迁移期间，旧薄服务保持不变，article 模块仍注入旧服务接口。

## 直接 Mapper 注入迁移

| 文件 | 直接注入的 Mapper | 迁移到 |
|---|---|---|
| PublicContentQueryServiceImpl:83 | SysTagMapper | SysTagRepository.findByTargetType() |
| UserFootprintServiceImpl:82 | SysUserFootprintMapper | SysUserFootprintRepository.upsertFootprint() |

## 执行步骤

### Step 1: 创建8个 Repository 接口 + 8个实现

文件位置：`module/content/repository/` 和 `module/content/repository/impl/`

### Step 2: 修改10个业务服务

逐个替换注入和调用：
1. `CategoryAdminServiceImpl` — 注入 `SysCategoryRepository`
2. `TagAdminServiceImpl` — 注入 `SysTagRepository`, `SysTagRelationRepository`
3. `CommentAdminServiceImpl` — 注入 `SysCommentRepository`
4. `CollectionAdminServiceImpl` — 注入 `SysCollectionRepository`, `SysCollectionFolderRepository`
5. `InteractionAdminServiceImpl` — 注入 `SysInteractionRepository`
6. `FootprintAdminServiceImpl` — 注入 `SysUserFootprintRepository`
7. `UserCommentServiceImpl` — 注入 `SysCommentRepository`, `SysInteractionRepository`
8. `UserCollectionServiceImpl` — 注入 `SysCollectionRepository`, `SysCollectionFolderRepository`
9. `UserFootprintServiceImpl` — 注入 `SysUserFootprintRepository`（移除直接 Mapper 注入）
10. `PublicContentQueryServiceImpl` — 注入各 Repository（移除 SysTagMapper 直接注入）

### Step 3: 更新测试

将测试中 mock 的薄服务替换为 mock Repository。

### Step 4: 删除旧薄服务

确认无外部引用后删除8个薄服务接口 + 8个实现。

> **注意**：article 模块仍引用本模块的薄服务，因此需等 article 模块也迁移完成后才能删除。建议 Phase B 期间保留旧薄服务，待第5轮 article 完成后再统一清理。

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.content.*Test"
```

确认所有业务服务中无 `lambdaQuery()`、`lambdaUpdate()`、`new LambdaQueryWrapper<>()`、直接 Mapper 注入。













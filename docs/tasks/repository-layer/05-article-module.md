# Article 模块 Repository 迁移计划

## 模块信息

- **优先级**：第5轮（最重的跨模块消费者）
- **复杂度**：高
- **前置依赖**：content + auth + file 模块的 Repository 已创建
- **涉及薄服务**：3个
- **涉及业务服务**：4个
- **数据访问总数**：约43处
- **跨模块数据访问**：content(7个服务) + auth(1个服务) + file(1个服务)

## Repository 列表

| Repository 接口 | 对应实体 | 薄服务来源 | Mapper自定义方法 |
|---|---|---|---|
| `BlogArticleRepository` | BlogArticle | BlogArticleService | `selectAdminPage`, `selectPublishedPage`, `selectArticleDetailById` |
| `BlogArticleCategoryRepository` | BlogArticleCategory | BlogArticleCategoryService | 无 |
| `BlogArticleAccessRepository` | BlogArticleAccess | BlogArticleAccessService | 无 |

## 各 Repository 方法设计

### BlogArticleRepository

```java
public interface BlogArticleRepository extends IService<BlogArticle> {
    // Mapper XML 包装
    List<BlogArticle> selectAdminPage(ArticleAdminPageQuery query);
    List<BlogArticle> selectPublishedPage(ArticlePublishedPageQuery query);
    BlogArticle selectArticleDetailById(Long id);

    // lambdaQuery 提取
    Page<BlogArticle> pageAdminArticles(ArticleAdminPageQuery query, Set<Long> filteredIds);
    List<BlogArticle> listAllPublished();
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `selectAdminPage` | BlogArticleMapper XML | 管理端文章分页（多表JOIN） |
| `selectPublishedPage` | BlogArticleMapper XML | 公开文章分页 |
| `selectArticleDetailById` | BlogArticleMapper XML | 文章详情 |
| `pageAdminArticles` | ArticleAdminServiceImpl:98, `LambdaQueryWrapper + page()` | 多条件管理端分页 |
| `listAllPublished` | PublicArticleServiceImpl:77, `lambdaQuery().eq(status,1).list()` | 所有已发布文章ID |

### BlogArticleCategoryRepository

```java
public interface BlogArticleCategoryRepository extends IService<BlogArticleCategory> {
    List<BlogArticleCategory> listByArticleIdOrdered(Long articleId);
    List<BlogArticleCategory> listArticleIdsByCategoryId(Long categoryId);
    List<BlogArticleCategory> listArticleCategoriesByIds(Collection<Long> ids, String type);
    boolean deleteByArticleId(Long articleId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `listByArticleIdOrdered` | PublicArticleServiceImpl:186, `lambdaQuery().eq(articleId).orderByAsc(sortOrder).list()` | 文章关联分类（排序） |
| `listArticleIdsByCategoryId` | PublicArticleServiceImpl:140, `lambdaQuery().eq(categoryId).list()` | 分类下的文章 |
| `listArticleCategoriesByIds` | ArticleAdminServiceImpl:331, `lambdaQuery().in(ids).eq(type).list()` | 批量查分类 |
| `deleteByArticleId` | ArticleAdminServiceImpl:213, `remove(LambdaQueryWrapper)` | 删除文章分类关联 |

### BlogArticleAccessRepository

```java
public interface BlogArticleAccessRepository extends IService<BlogArticleAccess> {
    boolean deleteByArticleId(Long articleId);
}
```

| 方法 | 来源 | 说明 |
|---|---|---|
| `deleteByArticleId` | ArticleAdminServiceImpl:213, `remove(LambdaQueryWrapper)` | 删除文章访问权限 |

## 跨模块 Repository 注入

Article 模块的业务服务是最大的跨模块消费者，需要注入以下外部模块 Repository：

### 来自 content 模块

| 外部 Repository | 用途 | 调用方 |
|---|---|---|
| `SysCategoryRepository` | 查文章分类 | ArticleAdminServiceImpl, PublicArticleServiceImpl |
| `SysTagRepository` | 查标签 | ArticleAdminServiceImpl, PublicArticleServiceImpl |
| `SysTagRelationRepository` | 文章标签关联 | ArticleAdminServiceImpl, PublicArticleServiceImpl |
| `SysCommentRepository` | 文章评论级联 | ArticleAdminServiceImpl |
| `SysInteractionRepository` | 文章互动 | ArticleAdminServiceImpl, UserArticleActionServiceImpl, PublicArticleServiceImpl |
| `SysCollectionRepository` | 文章收藏 | ArticleAdminServiceImpl, PublicArticleServiceImpl |
| `SysUserFootprintRepository` | 文章足迹 | ArticleAdminServiceImpl |

### 来自 auth 模块

| 外部 Repository | 用途 | 调用方 |
|---|---|---|
| `SysUserRepository` | 查作者信息 | ArticleAdminServiceImpl, PublicArticleServiceImpl |

### 来自 file 模块

| 外部 Repository | 用途 | 调用方 |
|---|---|---|
| `FileBusinessInfoRepository` | 文章附件 | ArticleAdminServiceImpl |

## 执行步骤

### Step 1: 创建3个 Repository 接口 + 3个实现

### Step 2: 修改4个业务服务

1. **`ArticleAdminServiceImpl`**（最复杂，16个依赖）：
   - 注入 `BlogArticleRepository`, `BlogArticleCategoryRepository`, `BlogArticleAccessRepository`
   - 注入 `SysCategoryRepository`, `SysTagRepository`, `SysTagRelationRepository`
   - 注入 `SysCommentRepository`, `SysInteractionRepository`, `SysCollectionRepository`, `SysUserFootprintRepository`
   - 注入 `SysUserRepository`, `FileBusinessInfoRepository`
   - 提取13处 LambdaQueryWrapper、9处 lambdaQuery 到对应 Repository 方法

2. **`PublicArticleServiceImpl`**：
   - 注入 `BlogArticleRepository`, `BlogArticleCategoryRepository`
   - 注入 `SysCategoryRepository`, `SysTagRepository`, `SysTagRelationRepository`
   - 注入 `SysInteractionRepository`, `SysCollectionRepository`, `SysUserRepository`

3. **`UserArticleActionServiceImpl`**：
   - 注入 `BlogArticleRepository`, `SysInteractionRepository`

4. **`ArticleAccessControlServiceImpl`**：
   - 注入 `BlogArticleAccessRepository`

### Step 3: 更新测试

### Step 4: 删除旧薄服务（本模块3个）

**同时清理**：此时 content/auth/file 模块的旧薄服务不再被引用，可以一并删除。

## 验证

```bash
mvn compile -q
mvn test -Dtest="com.cybzacg.blogbackend.module.article.*Test"
```

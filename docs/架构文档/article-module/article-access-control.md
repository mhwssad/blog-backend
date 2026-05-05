# Article 访问控制

## 1. 访问控制模型

Article 模块采用两层访问控制模型：

- **可见范围（Visibility Scope）**：控制文章在列表页的可见性
- **访问级别（Access Level）**：控制文章详情页的访问权限

```
┌─────────────────────────────────────────────────────────────┐
│                    访问控制两层模型                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   第一层：可见范围 (Visibility Scope)                        │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  PUBLIC      → 文章出现在公开列表                     │   │
│   │  SELF_ONLY   → 仅作者本人可见                        │   │
│   │  WHITELIST   → 白名单用户可见                        │   │
│   │  LOGIN_REQUIRED → 登录用户可见                       │   │
│   └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│   第二层：访问级别 (Access Level)                            │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  PUBLIC           → 无限制访问                       │   │
│   │  LOGIN_REQUIRED   → 需登录                         │   │
│   │  PASSWORD_PROTECTED → 需密码                        │   │
│   │  WHITELIST        → 白名单用户可访问                  │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. 可见范围定义

| 值 | 枚举 | 说明 | 列表可见性 |
|----|------|------|-----------|
| 0 | PUBLIC | 公开 | 所有用户可见 |
| 1 | SELF_ONLY | 仅自己可见 | 仅作者可见 |
| 2 | WHITELIST | 白名单可见 | 白名单用户可见 |
| 3 | LOGIN_REQUIRED | 登录可见 | 登录用户可见 |

### 2.1 可见范围校验逻辑

```
canShowInPublicList(article):
  1. 校验文章已发布 (status = 1)
  2. 校验审核状态通过 (reviewStatus = 0 或 2)
  3. 校验未设置定时发布或定时发布时间已过
  4. 校验 visibilityScope = PUBLIC
  5. 校验 accessLevel = 0 (公开)
  → 返回 true 表示可在公开列表展示
```

## 3. 访问级别定义

| 值 | 枚举 | 说明 | 详情访问 |
|----|------|------|---------|
| 0 | PUBLIC | 公开 | 无需验证 |
| 1 | LOGIN_REQUIRED | 登录可见 | 需登录 |
| 2 | PASSWORD_PROTECTED | 密码访问 | 需提供密码 |
| 3 | WHITELIST | 指定用户 | 白名单用户可访问 |

### 3.1 访问级别校验逻辑

```
canAccessArticle(article, userId):
  1. 校验文章已发布
  2. 校验 visibilityScope:
     - SELF_ONLY: 仅作者可访问
     - LOGIN_REQUIRED: 需登录
     - WHITELIST: 需在白名单中
  3. 校验 accessLevel:
     - PUBLIC: 无限制
     - LOGIN_REQUIRED: 需登录
     - PASSWORD_PROTECTED: 需验证密码
     - WHITELIST: 需在白名单中
  → 返回 true 表示可访问文章详情
```

## 4. 访问控制服务

### 4.1 ArticleAccessControlService

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleAccessControlService.java#L1-20
public interface ArticleAccessControlService {
    /**
     * 判断用户是否能访问文章。
     */
    boolean canAccessArticle(BlogArticle article, Long userId);

    /**
     * 校验用户对文章的访问权限，不通过则抛出业务异常。
     */
    void validateArticleAccess(BlogArticle article, Long userId);

    /**
     * 判断用户是否在文章的白名单中。
     */
    boolean hasArticleAccess(Long articleId, Long userId);

    /**
     * 查询文章的所有访问授权记录。
     */
    List<BlogArticleAccess> listArticleAccesses(Long articleId);
}
```

### 4.2 校验流程

```
用户请求文章详情
       │
       ▼
┌──────────────────┐
│ 获取当前用户 ID   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 调用 validateArticleAccess │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ 1. visibilityScope 校验           │
│    - SELF_ONLY: 仅作者通过        │
│    - LOGIN_REQUIRED: 需登录       │
│    - WHITELIST: 需白名单校验      │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ 2. accessLevel 校验               │
│    - PUBLIC: 直接通过             │
│    - LOGIN_REQUIRED: 需登录      │
│    - PASSWORD_PROTECTED: 验密码  │
│    - WHITELIST: 需白名单校验      │
└────────┬─────────────────────────┘
         │
         ▼
    通过 / 抛出异常
```

## 5. 白名单管理

### 5.1 BlogArticleAccess 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| article_id | BIGINT | 文章 ID |
| user_id | BIGINT | 授权用户 ID |
| access_type | TINYINT | 授权类型：1-密码访问，2-指定用户 |
| grant_time | DATETIME | 授权时间 |
| expire_time | DATETIME | 过期时间 |
| grant_reason | VARCHAR | 授权原因 |

### 5.2 白名单校验

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/impl/ArticleAccessControlServiceImpl.java#L53-63
// 白名单可见：校验用户是否在授权列表中
if (ArticleVisibilityScopeEnum.WHITELIST.getValue().equals(visibilityScope)
        && !hasWhitelistAccess(article.getId(), userId)) {
    return false;
}
```

### 5.3 用户侧访问配置

用户可以通过 `PUT /api/user/articles/{id}/access` 配置自己文章的访问白名单：

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/UserArticleManageService.java#L28-31
/**
 * 配置当前登录用户自己文章的访问名单。
 */
void assignMyArticleAccess(Long id, List<ArticleAccessItem> accessList);
```

## 6. 前台文章查询流程

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/impl/PublicArticleServiceImpl.java#L45-70
/**
 * 分页读取当前用户可见的已发布文章，并在内存中统一完成条件过滤、排序和分页切片。
 */
@Override
public PageResult<PublicArticleCardVO> pageArticles(PublicArticlePageQuery query) {
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<BlogArticle> page =
            blogArticleRepository.pagePublishedArticles(query, null);
    // ... 加载作者信息
    return PageResult.of(page, records);
}

/**
 * 查询单篇文章详情，并在访问校验通过后补齐分类、标签和当前用户状态。
 */
@Override
public PublicArticleDetailVO getArticle(Long id) {
    BlogArticle article = blogArticleRepository.getById(id);
    ExceptionThrowerCore.throwBusinessIf(article == null, ...);

    Long userId = SecurityUtils.getUserId();
    articleAccessControlService.validateArticleAccess(article, userId);
    // ... 加载分类、标签、互动状态
    return detailVO;
}
```

## 7. 访问控制与状态机联动

Article 模块通过 `ArticleStatusMachine` 统一管理文章状态与访问控制的联动：

```blog-backend/src/main/java/com/cybzacg/blogbackend/module/article/service/ArticleStatusMachine.java#L103-106
public boolean canShowInPublicList(BlogArticle article) {
    return isPublishedForNormalUsers(article, LocalDateTime.now())
            && normalizeVisibilityScope(article.getVisibilityScope()).equals(ArticleVisibilityScopeEnum.PUBLIC.getValue())
            && CollectionUtils.defaultInt(article.getAccessLevel()) == 0;
}
```

关键联动规则：
- **草稿/下架文章**：任何可见范围均不可见
- **审核中/拒绝文章**：不可在公开列表展示
- **定时发布文章**：在定时时间前不可见
- **访问级别 > 0**：即使公开列表可见，详情页仍需校验

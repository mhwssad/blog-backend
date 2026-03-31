# Article 模块待办清单

本文档用于收口 article 模块接下来要持续推进的任务，避免"知道有缺口，但每次都重新梳理一遍"。本清单按 2026-03-30 当前代码状态整理。

## 1. 本轮已完成

- [x] 建立文章后台管理、公开查询、用户互动三个控制器。
- [x] 实现文章保存、访问控制、分类关联、公开查询、用户行为等服务分层。
- [x] 将文章域 DTO 转换收口到 `ArticleModelMapper`。
- [x] 补入后台文章发布状态切换、指定用户授权拦截与授权重建服务级测试。
- [x] 补入文章级联删除服务级测试，覆盖附件引用清理、评论互动移除和收藏夹计数重算。
- [x] 补入访问控制服务级测试，覆盖 5 级访问、权限绕过、白名单/黑名单与过期。
- [x] 补入后台文章控制器、公开文章控制器、用户文章行为控制器的 WebMvc 鉴权测试。
- [x] 补入 `PublicArticleServiceImpl` 服务级测试，覆盖公开分页过滤、排序、访问控制与详情装配。
- [x] 补入 `UserArticleActionServiceImpl` 服务级测试，覆盖点赞/取消点赞幂等、权限拦截与计数同步。
- [x] 扩展 `ArticleAdminServiceImplTest`，覆盖文章创建、更新、后台分页与详情装配主链路。

## 2. 下一批高优先级

### 2.1 PublicArticleServiceImpl 服务级测试

- [x] `pageArticles` - 关键词过滤（标题/摘要匹配）
- [x] `pageArticles` - 按分类过滤
- [x] `pageArticles` - 按标签过滤
- [x] `pageArticles` - 分类+标签联合过滤
- [x] `pageArticles` - 排序模式: latest（按发布时间倒序）
- [x] `pageArticles` - 排序模式: top（置顶优先+发布时间）
- [x] `pageArticles` - 排序模式: hot（浏览+点赞+评论综合）
- [x] `pageArticles` - 空结果/无已发布文章
- [x] `pageArticles` - 分页边界（超出范围返回空页）
- [x] `pageArticles` - 访问控制过滤（未登录/已登录/指定用户级别）
- [x] `getArticle` - 文章不存在抛异常
- [x] `getArticle` - 非发布状态文章拒绝访问
- [x] `getArticle` - 登录必读级别（accessLevel=1）未登录拦截
- [x] `getArticle` - 正常返回详情（含分类/标签/liked/collected 状态）
- [x] `getArticle` - 浏览足迹记录调用验证

### 2.2 UserArticleActionServiceImpl 服务级测试

- [x] `likeArticle` - 首次点赞，互动记录创建+计数+1
- [x] `likeArticle` - 重复点赞幂等（不重复创建，计数不变）
- [x] `likeArticle` - 文章不存在或未发布抛异常
- [x] `likeArticle` - 无访问权限时抛异常
- [x] `unlikeArticle` - 取消已点赞，互动删除+计数-1
- [x] `unlikeArticle` - 未点赞幂等（无操作）
- [x] `unlikeArticle` - 计数不低于 0（下限保护）

### 2.3 ArticleAdminServiceImplTest 扩展

- [x] `createArticle` - 基本创建+分类/标签/访问绑定
- [x] `createArticle` - 无分类无标签时正常创建
- [x] `updateArticle` - 基本更新+分类/标签重建
- [x] `updateArticle` - 清空分类/标签（传入空列表）
- [x] `pageArticles` - 基本分页查询
- [x] `pageArticles` - 按状态/作者/关键词过滤
- [x] `getArticle` - 文章不存在抛异常
- [x] `getArticle` - 正常返回含分类/标签/访问列表的详情

## 3. 中期一致性补强

- [x] 核对后台文章保存与公开查询之间的状态一致性（草稿不应出现在公开列表）
- [x] 核对文章更新后分类/标签/访问级别的联动刷新是否完整
- [x] 核对文章访问级别从指定用户（level=4）降级为公开时，已有访问记录的清理策略
- [x] 补充文章域高成本方法的 Javadoc 注释（如 `deleteArticle` 级联逻辑、`pageArticles` 内存分页）
- [x] 若仍有手写实体装配，按规范优先迁移到 MapStruct

## 4. 本轮执行说明

- 本轮已优先完成"测试覆盖缺口最大、业务逻辑最复杂"的服务层测试补齐。
- `PublicArticleServiceImpl` 的公开分页、访问控制和详情聚合现已具备服务级回归覆盖。
- `UserArticleActionServiceImpl` 的点赞 / 取消点赞计数同步与幂等性现已具备服务级回归覆盖。
- `ArticleAdminServiceImpl` 的创建 / 更新 / 后台分页 / 详情装配主路径现已具备服务级回归覆盖。
- 本轮已继续把文章模块的中期一致性项收口为“有测试 + 有注释 + 有实现依据”的完成状态，包括草稿不进入公开口径、访问级别降级时授权清理，以及剩余可收口手工装配移交 mapper。

## 5. 完成标志

- 文章公开查询、用户互动、后台管理三条主链路都具备服务级回归覆盖。
- 排序、过滤、分页、访问控制、计数同步等关键逻辑不再只依赖人工联调验证。

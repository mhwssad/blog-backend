# 内容域 API

这份文档按前端场景拆成三块：

- 前台页面直接读取的公开接口
- 登录后用户行为接口
- 后台内容管理接口

如果你在做博客首页、文章详情、评论区、收藏夹或后台内容管理页，都看这份文档。

## 1. 快速接入

### 1.1 路由分组

| 路由前缀                                                          | 面向场景        | 是否需要登录               |
|---------------------------------------------------------------|-------------|----------------------|
| `/api/articles`、`/api/categories`、`/api/tags`、`/api/comments` | 前台公开内容      | 否，部分资源级校验仍生效         |
| `/api/public/**`                                              | 前台作者系列、公开系列详情 | 否，部分资源级校验仍生效         |
| `/api/user/**` 中的内容行为接口                                       | 点赞、评论、收藏、足迹 | 是                    |
| `/api/sys/**` 中的内容管理接口                                        | 后台管理台       | 是，且要求 `content:*` 权限 |

### 1.2 通用响应

统一返回：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

分页 `data` 固定为：

```json
{
  "total": 1,
  "current": 1,
  "size": 10,
  "records": []
}
```

### 1.3 页面开发时怎么找接口

| 页面 / 功能          | 优先看哪些接口                                                                     |
|------------------|-----------------------------------------------------------------------------|
| 首页文章列表           | `GET /api/articles`                                                         |
| 文章详情页            | `GET /api/articles/{id}`                                                    |
| 作者主页系列 / 系列详情页   | `GET /api/public/authors/{authorId}/series`、`GET /api/public/article-series/{id}` |
| 分类筛选、导航树         | `GET /api/categories/tree`                                                  |
| 标签筛选             | `GET /api/tags`                                                             |
| 评论区              | `GET /api/comments`、`POST /api/user/comments`                               |
| 点赞按钮             | `POST /api/user/articles/{id}/likes`、`DELETE /api/user/articles/{id}/likes` |
| 收藏夹 / 收藏弹窗       | `/api/user/collection-folders`、`/api/user/collections`                      |
| 作者系列管理           | `/api/user/article-series`                                                  |
| 浏览历史             | `/api/user/footprints`                                                      |
| 后台文章管理           | `/api/sys/articles` 相关接口                                                    |
| 后台分类 / 标签 / 评论管理 | `/api/sys/categories`、`/api/sys/tags`、`/api/sys/comments`                   |

## 2. 前台页面接口

这一组接口支持博客前台页面直接使用，匿名用户也可以调，但文章详情仍会走资源级访问控制。

- 当前公开列表仅返回满足以下条件的文章：`status=1`、`reviewStatus in (0,2)`、`visibilityScope=0`、`accessLevel=0`，且未处于未来定时发布时间。

### 2.1 文章列表

- 请求：`GET /api/articles`
- 鉴权：否
- 用途：首页、搜索页、分类页、标签页
- 查询参数：

| 参数           | 类型     | 说明                   |
|--------------|--------|----------------------|
| `current`    | Long   | 页码                   |
| `size`       | Long   | 每页数量                 |
| `keyword`    | String | 标题 / 摘要关键字           |
| `categoryId` | Long   | 分类 ID                |
| `tagId`      | Long   | 标签 ID                |
| `sort`       | String | `latest`、`top`、`hot` |

- 响应字段：`PublicArticleCardVO`

| 字段             | 类型       | 说明    |
|----------------|----------|-------|
| `id`           | Long     | 文章 ID |
| `title`        | String   | 标题    |
| `summary`      | String   | 摘要    |
| `coverImage`   | String   | 封面地址  |
| `authorId`     | Long     | 作者 ID |
| `authorName`   | String   | 作者名   |
| `isTop`        | Integer  | 是否置顶  |
| `accessLevel`  | Integer  | 访问级别  |
| `viewCount`    | Long     | 浏览数   |
| `likeCount`    | Long     | 点赞数   |
| `commentCount` | Long     | 评论数   |
| `collectCount` | Long     | 收藏数   |
| `publishTime`  | DateTime | 发布时间  |

### 2.2 文章详情

- 请求：`GET /api/articles/{id}`
- 鉴权：否，但资源级权限仍生效
- 用途：文章详情页
- 路径参数：`id`
- 响应字段：`PublicArticleDetailVO`

- 访问规则：
    - 作者本人和具备 `content:article:query` 权限的后台用户可查看非公开状态文章。
    - `visibilityScope=1` 仅作者本人和后台有权限用户可见。
    - `visibilityScope=2` 需要命中访问名单白名单，黑名单优先拒绝。
    - `visibilityScope=3` 需要登录后访问。

| 字段             | 类型       | 说明          |
|----------------|----------|-------------|
| `id`           | Long     | 文章 ID       |
| `title`        | String   | 标题          |
| `summary`      | String   | 摘要          |
| `content`      | String   | 正文          |
| `coverImage`   | String   | 封面地址        |
| `authorId`     | Long     | 作者 ID       |
| `authorName`   | String   | 作者名         |
| `isTop`        | Integer  | 是否置顶        |
| `isOriginal`   | Integer  | 是否原创        |
| `sourceUrl`    | String   | 原文地址        |
| `accessLevel`  | Integer  | 访问级别        |
| `visibilityScope` | Integer | 可见范围：`0` 公开，`1` 仅自己可见，`2` 白名单可见，`3` 登录可见 |
| `viewCount`    | Long     | 浏览数         |
| `likeCount`    | Long     | 点赞数         |
| `commentCount` | Long     | 评论数         |
| `collectCount` | Long     | 收藏数         |
| `shareCount`   | Long     | 分享数         |
| `publishTime`  | DateTime | 发布时间        |
| `categories`   | List     | 分类列表        |
| `tags`         | List     | 标签列表        |
| `liked`        | Boolean  | 当前登录用户是否已点赞 |
| `collected`    | Boolean  | 当前登录用户是否已收藏 |
| `canComment`   | Boolean  | 当前用户是否允许评论  |
| `seriesList`   | List     | 当前用户可见的所属系列摘要 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "Spring Boot 4 + JWT 认证实践",
    "summary": "使用当前项目的认证模块快速搭建账号登录能力。",
    "content": "正文内容",
    "coverImage": null,
    "authorId": 1,
    "authorName": "管理员",
    "isTop": 1,
    "isOriginal": 1,
    "sourceUrl": null,
    "accessLevel": 0,
    "viewCount": 128,
    "likeCount": 1,
    "commentCount": 2,
    "collectCount": 1,
    "shareCount": 6,
    "publishTime": "2026-03-12 10:00:00",
    "categories": [
      {
        "id": 3,
        "parentId": 1,
        "name": "Java 后端",
        "code": "article-java-backend",
        "type": "article",
        "level": 2,
        "sortOrder": 1,
        "icon": "java",
        "description": "Spring Boot、MyBatis Plus 等后端内容",
        "children": []
      }
    ],
    "tags": [
      {
        "id": 1,
        "name": "Spring Boot",
        "color": "#409EFF"
      }
    ],
    "seriesList": [
      {
        "id": 8,
        "title": "Spring Boot 实战系列",
        "coverImage": null,
        "articleCount": 6,
        "sortOrder": 1,
        "visibilityScope": 0
      }
    ],
    "liked": false,
    "collected": false,
    "canComment": false
  }
}
```

### 2.2.1 文章系列

#### 查询作者系列列表

- 请求：`GET /api/public/authors/{authorId}/series`
- 鉴权：否；登录后会按当前登录身份放宽 `登录可见` 系列
- 用途：作者主页系列区块
- 路径参数：`authorId`
- 响应字段：`PublicArticleSeriesVO`

| 字段                | 类型       | 说明 |
|-------------------|----------|----|
| `id`              | Long     | 系列 ID |
| `title`           | String   | 系列标题 |
| `description`     | String   | 系列描述 |
| `coverImage`      | String   | 系列封面 |
| `ownerUserId`     | Long     | 创建人 ID |
| `ownerName`       | String   | 创建人名称 |
| `visibilityScope` | Integer  | `0` 公开，`1` 仅自己可见，`3` 登录可见 |
| `articleCount`    | Integer  | 系列文章数 |
| `sortOrder`       | Integer  | 排序值 |
| `createdAt`       | DateTime | 创建时间 |
| `updatedAt`       | DateTime | 更新时间 |

- 关键规则：
    - 匿名用户只能看到 `status=1` 且 `visibilityScope=0` 的系列。
    - 已登录用户额外可看到 `visibilityScope=3` 的系列。
    - 作者本人和具备 `content:article:query` 权限的后台用户可看到自己的非公开系列。

#### 查询公开系列详情

- 请求：`GET /api/public/article-series/{id}`
- 鉴权：否，但系列本身和系列内文章都会走资源级访问控制
- 用途：系列详情页
- 路径参数：`id`
- 响应字段：`PublicArticleSeriesDetailVO`

| 字段                | 类型       | 说明 |
|-------------------|----------|----|
| `id`              | Long     | 系列 ID |
| `title`           | String   | 系列标题 |
| `description`     | String   | 系列描述 |
| `coverImage`      | String   | 系列封面 |
| `ownerUserId`     | Long     | 创建人 ID |
| `ownerName`       | String   | 创建人名称 |
| `visibilityScope` | Integer  | 系列可见范围 |
| `articleCount`    | Integer  | 当前用户实际可见的系列文章数 |
| `sortOrder`       | Integer  | 排序值 |
| `createdAt`       | DateTime | 创建时间 |
| `updatedAt`       | DateTime | 更新时间 |
| `articles`        | List     | 当前用户可见的系列文章列表 |

- `articles` 项使用 `ArticleSeriesArticleVO`：

| 字段              | 类型       | 说明 |
|-----------------|----------|----|
| `id`            | Long     | 文章 ID |
| `title`         | String   | 文章标题 |
| `summary`       | String   | 文章摘要 |
| `coverImage`    | String   | 文章封面 |
| `status`        | Integer  | 文章状态 |
| `reviewStatus`  | Integer  | 审核状态 |
| `visibilityScope` | Integer | 文章可见范围 |
| `publishTime`   | DateTime | 发布时间 |
| `seqNo`         | Integer  | 系列内顺序 |

- 关键规则：
    - 系列详情会过滤掉当前用户无权访问的文章。
    - 系列详情中的文章顺序按 `seqNo` 返回。
    - 系列删除只解除文章关联，不会删除文章本身。

### 2.3 分类树

- 请求：`GET /api/categories/tree`
- 鉴权：否
- 用途：分类导航、分类筛选
- 响应字段：`PublicCategoryTreeVO`

| 字段            | 类型      | 说明             |
|---------------|---------|----------------|
| `id`          | Long    | 分类 ID          |
| `parentId`    | Long    | 父分类 ID         |
| `name`        | String  | 分类名            |
| `code`        | String  | 分类编码           |
| `type`        | String  | 当前固定 `article` |
| `level`       | Integer | 层级             |
| `sortOrder`   | Integer | 排序             |
| `icon`        | String  | 图标             |
| `description` | String  | 描述             |
| `children`    | List    | 子节点            |

### 2.4 标签列表

- 请求：`GET /api/tags`
- 鉴权：否
- 用途：标签筛选、标签云
- 查询参数：

| 参数           | 类型     | 说明           |
|--------------|--------|--------------|
| `targetType` | String | 默认 `article` |

- 响应字段：`PublicTagVO`

| 字段      | 类型     | 说明    |
|---------|--------|-------|
| `id`    | Long   | 标签 ID |
| `name`  | String | 标签名   |
| `color` | String | 标签颜色  |

- 说明：
    - 当前仅对 `targetType=article` 返回数据。
    - 其他目标类型按当前实现返回空数组。

### 2.5 评论树

- 请求：`GET /api/comments`
- 鉴权：否
- 用途：文章评论区
- 查询参数：

| 参数           | 类型     | 说明             |
|--------------|--------|----------------|
| `current`    | Long   | 页码             |
| `size`       | Long   | 每页数量           |
| `targetType` | String | 当前固定 `article` |
| `targetId`   | Long   | 目标文章 ID        |

- 响应字段：`PublicCommentVO`

| 字段             | 类型           | 说明        |
|----------------|--------------|-----------|
| `id`           | Long         | 评论 ID     |
| `targetId`     | Long         | 目标 ID     |
| `targetType`   | String       | 目标类型      |
| `content`      | String       | 评论内容      |
| `images`       | List<String> | 图片列表      |
| `userId`       | Long         | 评论用户 ID   |
| `userNickname` | String       | 评论用户昵称    |
| `userAvatar`   | String       | 评论用户头像    |
| `rootId`       | Long         | 根评论 ID    |
| `parentId`     | Long         | 父评论 ID    |
| `likeCount`    | Long         | 点赞数       |
| `replyCount`   | Long         | 回复数       |
| `status`       | Integer      | 评论状态      |
| `createdAt`    | DateTime     | 创建时间      |
| `liked`        | Boolean      | 当前用户是否已点赞 |
| `children`     | List         | 回复树       |

- 关键规则：
    - 评论查询会复用文章资源级访问控制，不可见文章不会返回评论。
    - 访客仅返回一级评论，不展开回复树。
    - 登录用户返回完整评论树。

- 当前行为：
    - 返回树形结构。
    - 内部通过“根评论 + 回复”两段查询组装评论树。
    - `current/size` 当前仅保留在请求模型中，现阶段不会对评论树做分页截断。
    - 仅返回 `status=1` 的评论。
    - 已登录用户会额外拿到 `liked` 状态。

## 3. 登录后用户行为接口

所有接口都要求：

```http
Authorization: Bearer <accessToken>
```

### 3.1 我的文章

| 场景      | 方法 | 路径                     |
|---------|----|------------------------|
| 我的文章分页  | GET | `/api/user/articles`   |
| 我的文章详情  | GET | `/api/user/articles/{id}` |
| 配置我的文章访问名单 | PUT | `/api/user/articles/{id}/access` |

#### 我的文章分页

- 请求：`GET /api/user/articles`
- 鉴权：是
- 用途：作者后台、个人内容中心查看草稿 / 审核中 / 已拒绝 / 已发布文章
- 查询参数：

| 参数 | 类型 | 说明 |
|------|------|------|
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页数量，默认 `10` |
| `keyword` | String | 标题 / 摘要关键字 |
| `status` | Integer | 文章状态：`0` 草稿，`1` 已发布，`2` 已下线 |
| `reviewStatus` | Integer | 审核状态：`0` 未送审，`1` 审核中，`2` 审核通过，`3` 审核拒绝 |
| `visibilityScope` | Integer | 可见范围：`0` 公开，`1` 仅自己可见，`2` 白名单可见，`3` 登录可见 |
| `categoryId` | Long | 分类 ID |
| `tagId` | Long | 标签 ID |

- 响应字段：`UserArticleVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 文章 ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `coverImage` | String | 封面 |
| `isTop` | Integer | 是否置顶 |
| `isOriginal` | Integer | 是否原创 |
| `status` | Integer | 文章状态 |
| `reviewStatus` | Integer | 审核状态 |
| `accessLevel` | Integer | 访问级别 |
| `visibilityScope` | Integer | 可见范围 |
| `viewCount` | Long | 浏览数 |
| `likeCount` | Long | 点赞数 |
| `commentCount` | Long | 评论数 |
| `collectCount` | Long | 收藏数 |
| `shareCount` | Long | 分享数 |
| `publishTime` | DateTime | 发布时间 |
| `scheduledPublishTime` | DateTime | 定时发布时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |
| `remark` | String | 备注 |

- 关键规则：
    - 只返回当前登录用户自己的文章。
    - 支持直接筛出草稿、审核中、已拒绝文章。
    - 分类和标签筛选会复用文章绑定关系过滤。

#### 我的文章详情

- 请求：`GET /api/user/articles/{id}`
- 鉴权：是
- 用途：用户查看自己的文章详情，继续编辑、送审、管理白名单
- 路径参数：`id`
- 响应字段：`UserArticleDetailVO`

- 相比列表额外返回：

| 字段 | 类型 | 说明 |
|------|------|------|
| `content` | String | 正文 |
| `authorId` | Long | 作者 ID |
| `authorName` | String | 作者名称 |
| `sourceUrl` | String | 原文地址 |
| `categoryIds` | List<Long> | 分类 ID 列表 |
| `tagIds` | List<Long> | 标签 ID 列表 |
| `accessList` | List<ArticleAccessItem> | 当前访问名单 |
| `seriesList` | List<ArticleSeriesSummaryVO> | 所属系列摘要 |

- 关键规则：
    - 只能查看自己的文章详情。
    - 文章详情会返回当前文章的分类、标签、访问名单和所属系列摘要，便于后续继续编辑和送审。

#### 配置我的文章访问名单

- 请求：`PUT /api/user/articles/{id}/access`
- 鉴权：是
- 用途：作者本人维护自己文章的白名单 / 黑名单访问名单
- 路径参数：`id`
- 请求体：`ArticleAccessAssignRequest`

```json
{
  "accessList": [
    {
      "userId": 2,
      "accessType": 1,
      "expireTime": "2026-05-31 23:59:59",
      "grantReason": "专栏内测"
    }
  ]
}
```

- 关键规则：
    - 只能配置当前登录用户自己的文章。
    - 仅当文章 `visibilityScope=2` 或 `accessLevel=4` 时允许调用。
    - 本次提交的名单会覆盖旧名单；传空列表表示清空现有名单。
    - `accessType=1` 表示白名单，`accessType=2` 表示黑名单。
    - `expireTime` 为空表示长期有效。
    - 当前文章详情接口会同步返回最新 `accessList`，前端可直接回显。

### 3.2 文章点赞

| 场景     | 方法     | 路径                              | 说明 |
|--------|--------|---------------------------------|----|
| 点赞文章   | POST   | `/api/user/articles/{id}/likes` | 幂等 |
| 取消点赞文章 | DELETE | `/api/user/articles/{id}/likes` | 幂等 |

- 成功响应：

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

- 关键规则：
    - 新增点赞前会校验文章访问权限与互动状态，草稿、审核中、已拒绝、未来定时发布或当前用户无权访问的文章都不能点赞。
    - 取消点赞仍按幂等处理；若文章后续被隐藏，已存在点赞记录仍可撤销。

### 3.3 文章审核

| 场景       | 方法   | 路径                                      |
|----------|------|-----------------------------------------|
| 提交文章审核   | POST | `/api/user/articles/{id}/submit-review` |
| 查询文章审核日志 | GET  | `/api/user/articles/{id}/review-log`    |

#### 提交文章审核

- 请求：`POST /api/user/articles/{id}/submit-review`
- 请求体：`ArticleReviewSubmitRequest`

```json
{
  "reviewComment": "已补充目录结构和引用说明，请帮忙复核。"
}
```

- 关键规则：
    - 只能提交当前登录用户自己的文章。
    - 审核中不可重复提交。
    - 未送审、已通过、已拒绝文章均可再次提交；已拒绝或已通过后再次提交会记为 `resubmit`。
    - 未来定时发布文章需先取消定时发布时间后再提交审核。

#### 查询文章审核日志

- 请求：`GET /api/user/articles/{id}/review-log`
- 响应字段：`ArticleReviewLogVO`

| 字段                      | 类型       | 说明                                   |
|-------------------------|----------|--------------------------------------|
| `id`                    | Long     | 日志 ID                                |
| `articleId`             | Long     | 文章 ID                                |
| `actionType`            | String   | `submit`、`resubmit`、`approve`、`reject` |
| `actionTypeLabel`       | String   | 动作标签                                 |
| `fromReviewStatus`      | Integer  | 变更前审核状态                              |
| `fromReviewStatusLabel` | String   | 变更前审核状态标签                            |
| `toReviewStatus`        | Integer  | 变更后审核状态                              |
| `toReviewStatusLabel`   | String   | 变更后审核状态标签                            |
| `operatorUserId`        | Long     | 操作人 ID                               |
| `operatorUsername`      | String   | 操作人用户名                                |
| `operatorNickname`      | String   | 操作人昵称                                 |
| `reviewComment`         | String   | 审核说明 / 备注                            |
| `operatedAt`            | DateTime | 操作时间                                 |

### 3.3.1 系列管理

#### 接口速览

| 场景        | 方法     | 路径                                        |
|-----------|--------|-------------------------------------------|
| 查询我的系列列表  | GET    | `/api/user/article-series`                |
| 查询我的系列详情  | GET    | `/api/user/article-series/{id}`           |
| 创建系列      | POST   | `/api/user/article-series`                |
| 修改系列      | PUT    | `/api/user/article-series/{id}`           |
| 删除系列      | DELETE | `/api/user/article-series/{id}`           |
| 向系列加入文章   | POST   | `/api/user/article-series/{id}/articles`  |
| 从系列移出文章   | DELETE | `/api/user/article-series/{id}/articles/{articleId}` |
| 调整系列文章顺序  | PUT    | `/api/user/article-series/{id}/articles/sort` |

- 关键规则：
    - 所有系列管理接口都要求登录，且当前用户已具备 `author` 角色。
    - 系列当前只支持 `visibilityScope=0/1/3`，暂不开放白名单系列。
    - 作者只能管理自己的系列，只能把自己的文章加入系列。
    - 同一文章在同一系列内不可重复加入。
    - 删除文章或文章作者变更时，系统会自动清理对应系列关联并重算系列文章数。

#### 创建 / 修改系列

- 创建：`POST /api/user/article-series`
- 修改：`PUT /api/user/article-series/{id}`
- 请求体：`ArticleSeriesSaveRequest`

```json
{
  "title": "Spring Boot 实战系列",
  "description": "持续收录当前博客里的 Spring Boot 相关文章。",
  "coverImage": null,
  "status": 1,
  "visibilityScope": 0,
  "sortOrder": 1
}
```

#### 向系列加入文章

- 请求：`POST /api/user/article-series/{id}/articles`
- 请求体：`ArticleSeriesArticleRequest`

```json
{
  "articleId": 12
}
```

#### 调整系列文章顺序

- 请求：`PUT /api/user/article-series/{id}/articles/sort`
- 请求体：`ArticleSeriesSortRequest`

```json
{
  "articleIds": [12, 18, 25]
}
```

#### 用户侧系列响应

- 系列列表项：`UserArticleSeriesVO`
- 系列详情：`UserArticleSeriesDetailVO`

详情会额外返回 `articles` 字段，字段结构同 `ArticleSeriesArticleVO`。

### 3.4 评论行为

| 场景     | 方法     | 路径                              |
|--------|--------|---------------------------------|
| 点赞评论   | POST   | `/api/user/comments/{id}/likes` |
| 取消点赞评论 | DELETE | `/api/user/comments/{id}/likes` |
| 发表评论   | POST   | `/api/user/comments`            |
| 删除我的评论 | DELETE | `/api/user/comments/{id}`       |

#### 发表评论

- 请求体：`CommentSaveRequest`

```json
{
  "targetType": "article",
  "targetId": 1,
  "content": "这篇内容适合作为联调样例。",
  "images": [],
  "rootId": 0,
  "parentId": 0
}
```

- 关键规则：
    - 当前仅支持 `targetType=article`。
    - 顶级评论请传 `rootId=0`、`parentId=0`。
    - `parentId>0` 时必须与目标文章匹配；若未显式传 `rootId`，服务端会按父评论自动推导根评论 ID。
    - 点赞评论与取消点赞都按幂等处理。
    - 删除评论仅允许删除当前登录用户自己的评论。
    - 删除根评论时会级联删除其回复树。

### 3.5 收藏夹与收藏

#### 接口速览

| 场景      | 方法     | 路径                                  |
|---------|--------|-------------------------------------|
| 查询我的收藏夹 | GET    | `/api/user/collection-folders`      |
| 新增收藏夹   | POST   | `/api/user/collection-folders`      |
| 修改收藏夹   | PUT    | `/api/user/collection-folders/{id}` |
| 删除收藏夹   | DELETE | `/api/user/collection-folders/{id}` |
| 查询我的收藏  | GET    | `/api/user/collections`             |
| 新增收藏    | POST   | `/api/user/collections`             |
| 删除收藏    | DELETE | `/api/user/collections/{id}`        |

#### 收藏夹请求体

- 请求体：`CollectionFolderSaveRequest`

```json
{
  "folderName": "默认收藏夹",
  "folderType": "article",
  "description": "文章收藏",
  "isPublic": 0,
  "isDefault": 1,
  "sortOrder": 0
}
```

#### 收藏请求体

- 请求体：`CollectionSaveRequest`

```json
{
  "folderId": 1,
  "targetId": 1,
  "targetType": "article",
  "remark": "待复习"
}
```

#### 响应字段

- 收藏夹：`CollectionFolderVO`

| 字段                | 类型       | 说明             |
|-------------------|----------|----------------|
| `id`              | Long     | 收藏夹 ID         |
| `userId`          | Long     | 用户 ID          |
| `folderName`      | String   | 收藏夹名称          |
| `folderType`      | String   | 当前固定 `article` |
| `description`     | String   | 描述             |
| `isPublic`        | Integer  | 是否公开           |
| `isDefault`       | Integer  | 是否默认收藏夹        |
| `sortOrder`       | Integer  | 排序             |
| `collectionCount` | Integer  | 收藏数量           |
| `createdAt`       | DateTime | 创建时间           |
| `updatedAt`       | DateTime | 更新时间           |

- 收藏记录：`CollectionVO`

| 字段            | 类型       | 说明             |
|---------------|----------|----------------|
| `id`          | Long     | 收藏记录 ID        |
| `folderId`    | Long     | 收藏夹 ID         |
| `targetId`    | Long     | 目标 ID          |
| `targetType`  | String   | 当前固定 `article` |
| `remark`      | String   | 备注             |
| `targetTitle` | String   | 目标标题           |
| `targetUrl`   | String   | 目标链接           |
| `createdAt`   | DateTime | 创建时间           |

- 关键规则：
    - 当前仅支持收藏 `article`。
    - 未传 `folderId` 时自动落入默认收藏夹，不存在则自动创建。
    - 默认收藏夹不可删除。
    - 新增收藏前会校验文章访问权限与互动状态，草稿、审核中、已拒绝、未来定时发布或当前用户无权访问的文章都不能收藏。

### 3.6 足迹

#### 接口速览

| 场景     | 方法     | 路径                          |
|--------|--------|-----------------------------|
| 查询我的足迹 | GET    | `/api/user/footprints`      |
| 删除我的足迹 | DELETE | `/api/user/footprints/{id}` |
| 清空我的足迹 | DELETE | `/api/user/footprints`      |

#### 足迹列表

- 查询参数：`current`、`size`、`targetType`
- 响应字段：`UserFootprintVO`

| 字段            | 类型       | 说明             |
|---------------|----------|----------------|
| `id`          | Long     | 足迹 ID          |
| `targetId`    | Long     | 目标 ID          |
| `targetType`  | String   | 当前固定 `article` |
| `targetTitle` | String   | 目标标题           |
| `targetUrl`   | String   | 目标链接           |
| `visitedAt`   | DateTime | 最近访问时间         |

- 当前行为：
    - 登录用户访问文章详情时，系统会自动记录文章足迹。
    - 同一用户同一文章按 upsert 方式更新，而不是重复插入。
    - 该行为依赖 `uk_user_target` 唯一键和 `ON DUPLICATE KEY UPDATE` 保证并发下只保留最新记录。

## 4. 后台内容管理接口

所有接口都需要登录，且要具备对应 `content:*` 权限。

### 4.1 后台文章管理

#### 接口速览

| 场景       | 方法     | 路径                              | 权限                       |
|----------|--------|---------------------------------|--------------------------|
| 分页查询文章   | GET    | `/api/sys/articles`             | `content:article:query`  |
| 查询文章详情   | GET    | `/api/sys/articles/{id}`        | `content:article:query`  |
| 新增文章     | POST   | `/api/sys/articles`             | `content:article:create` |
| 修改文章     | PUT    | `/api/sys/articles/{id}`        | `content:article:update` |
| 修改文章状态   | PUT    | `/api/sys/articles/{id}/status` | `content:article:update` |
| 切换文章置顶   | PUT    | `/api/sys/articles/{id}/top`    | `content:article:top`     |
| 切换文章推荐   | PUT    | `/api/sys/articles/{id}/recommend` | `content:article:recommend` |
| 配置文章访问名单 | PUT    | `/api/sys/articles/{id}/access` | `content:article:access` |
| 删除文章     | DELETE | `/api/sys/articles/{id}`        | `content:article:delete` |

#### 分页查询文章

- 请求：`GET /api/sys/articles`
- 查询参数：

| 参数                 | 类型       | 说明             |
|--------------------|----------|----------------|
| `current`          | Long     | 页码，默认 `1`      |
| `size`             | Long     | 每页数量，默认 `10`   |
| `keyword`          | String   | 标题 / 摘要关键字     |
| `authorId`         | Long     | 作者 ID          |
| `status`           | Integer  | `0` 草稿，`1` 已发布 |
| `reviewStatus`     | Integer  | `0` 未送审，`1` 审核中，`2` 审核通过，`3` 审核拒绝 |
| `accessLevel`      | Integer  | 访问级别           |
| `visibilityScope`  | Integer  | `0` 公开，`1` 仅自己可见，`2` 白名单可见，`3` 登录可见 |
| `categoryId`       | Long     | 分类 ID          |
| `tagId`            | Long     | 标签 ID          |
| `isTop`            | Integer  | `0/1`          |
| `publishTimeStart` | DateTime | 发布时间起          |
| `publishTimeEnd`   | DateTime | 发布时间止          |

- 响应字段：`ArticleAdminVO`

| 字段             | 类型       | 说明    |
|----------------|----------|-------|
| `id`           | Long     | 文章 ID |
| `title`        | String   | 标题    |
| `summary`      | String   | 摘要    |
| `coverImage`   | String   | 封面    |
| `authorId`     | Long     | 作者 ID |
| `authorName`   | String   | 作者名   |
| `isTop`        | Integer  | 是否置顶  |
| `isOriginal`   | Integer  | 是否原创  |
| `status`       | Integer  | 文章状态  |
| `reviewStatus` | Integer  | 审核状态：`0` 未送审，`1` 审核中，`2` 审核通过，`3` 审核拒绝 |
| `accessLevel`  | Integer  | 访问级别  |
| `visibilityScope` | Integer | 可见范围 |
| `viewCount`    | Long     | 浏览数   |
| `likeCount`    | Long     | 点赞数   |
| `commentCount` | Long     | 评论数   |
| `collectCount` | Long     | 收藏数   |
| `shareCount`   | Long     | 分享数   |
| `publishTime`  | DateTime | 发布时间  |
| `scheduledPublishTime` | DateTime | 定时发布时间 |
| `createdAt`    | DateTime | 创建时间  |
| `updatedAt`    | DateTime | 更新时间  |
| `remark`       | String   | 备注    |

#### 文章详情

- 请求：`GET /api/sys/articles/{id}`
- 响应字段：`ArticleDetailVO`
- 相比列表额外返回：

| 字段            | 类型                      | 说明       |
|---------------|-------------------------|----------|
| `content`     | String                  | 正文       |
| `sourceUrl`   | String                  | 原文地址     |
| `reviewStatus` | Integer                | 审核状态     |
| `scheduledPublishTime` | DateTime       | 定时发布时间   |
| `visibilityScope` | Integer             | 可见范围     |
| `categoryIds` | List<Long>              | 分类 ID 列表 |
| `tagIds`      | List<Long>              | 标签 ID 列表 |
| `accessList`  | List<ArticleAccessItem> | 访问名单     |

- `ArticleAccessItem`：

| 字段            | 类型       | 说明              |
|---------------|----------|-----------------|
| `userId`      | Long     | 用户 ID           |
| `accessType`  | Integer  | `1` 白名单，`2` 黑名单 |
| `expireTime`  | DateTime | 过期时间，可为空        |
| `grantReason` | String   | 授权原因            |

#### 新增 / 修改文章

- 新增：`POST /api/sys/articles`
- 修改：`PUT /api/sys/articles/{id}`
- 请求体：`ArticleSaveRequest`

| 字段            | 类型                      | 必填 | 说明                  |
|---------------|-------------------------|----|---------------------|
| `title`       | String                  | 是  | 最长 128              |
| `summary`     | String                  | 否  | 最长 2000             |
| `content`     | String                  | 否  | 正文                  |
| `coverImage`  | String                  | 否  | 封面地址，最长 512         |
| `authorId`    | Long                    | 是  | 作者 ID               |
| `isTop`       | Integer                 | 否  | 是否置顶                |
| `isOriginal`  | Integer                 | 否  | 默认 `1`              |
| `sourceUrl`   | String                  | 否  | 非原创时必填              |
| `status`      | Integer                 | 否  | `0` 草稿，`1` 已发布      |
| `publishTime` | DateTime                | 否  | 发布时间                |
| `scheduledPublishTime` | DateTime         | 否  | 定时发布时间；未来时间会先以草稿保存 |
| `accessLevel` | Integer                 | 否  | 访问级别                |
| `visibilityScope` | Integer              | 否  | `0` 公开，`1` 仅自己可见，`2` 白名单可见，`3` 登录可见 |
| `remark`      | String                  | 否  | 备注，最长 256           |
| `categoryIds` | List<Long>              | 否  | 分类 ID 列表            |
| `tagIds`      | List<Long>              | 否  | 标签 ID 列表            |
| `accessList`  | List<ArticleAccessItem> | 否  | `accessLevel=4` 或 `visibilityScope=2` 时使用 |

- 请求示例：

```json
{
  "title": "内容域并行实施说明",
  "summary": "记录内容域文章、分类、标签、评论与用户行为接口的实现边界。",
  "content": "正文内容",
  "authorId": 1,
  "isTop": 1,
  "isOriginal": 1,
  "status": 1,
  "scheduledPublishTime": "2026-05-01 09:00:00",
  "accessLevel": 4,
  "visibilityScope": 2,
  "categoryIds": [3],
  "tagIds": [1, 3],
  "accessList": [
    {
      "userId": 2,
      "accessType": 1,
      "expireTime": null,
      "grantReason": "联调开放"
    }
  ]
}
```

- 关键规则：
    - `isOriginal=0` 时必须提供 `sourceUrl`。
    - 仅允许绑定存在的分类和标签。
    - 创建文章时会按作者身份校验文章总量配额：普通用户默认上限 `20` 篇，作者默认上限 `200` 篇，可通过系统配置调整。
    - `scheduledPublishTime` 为未来时间且 `status=1` 时，文章会先以草稿保存，等待定时任务发布。
    - `visibilityScope=2` 或 `accessLevel=4` 时保存后会重建访问名单。
    - `accessLevel=2/3` 当前会统一拒绝前台访问。
    - 当前文章处于 `reviewStatus=1` 审核中时，默认禁止普通内容编辑；具备 `content:article-review:review` 权限的审核管理员可继续执行常规审核，异常状态修正需走超级管理员专用权限。

#### 修改文章状态

- 请求：`PUT /api/sys/articles/{id}/status`

```json
{
  "status": 1
}
```

#### 配置文章访问名单

- 请求：`PUT /api/sys/articles/{id}/access`

```json
{
  "accessList": [
    {
      "userId": 2,
      "accessType": 1,
      "expireTime": "2026-03-31 23:59:59",
      "grantReason": "专栏试读"
    }
  ]
}
```

- 关键规则：
    - 仅当文章 `accessLevel=4` 或 `visibilityScope=2` 时允许调用。
    - 本次提交的名单会覆盖旧名单。
    - `expireTime` 为空表示长期有效。

#### 删除文章

- 请求：`DELETE /api/sys/articles/{id}`
- 当前行为：会级联清理分类绑定、标签绑定、访问名单、评论、收藏、点赞互动、浏览足迹。

### 4.2 后台文章审核

#### 接口速览

| 场景       | 方法  | 路径                                     | 权限                             |
|----------|-----|----------------------------------------|--------------------------------|
| 分页查询审核文章 | GET | `/api/sys/article-reviews`             | `content:article-review:query` |
| 查询审核详情   | GET | `/api/sys/article-reviews/{id}`        | `content:article-review:query` |
| 审核通过     | PUT | `/api/sys/article-reviews/{id}/approve` | `content:article-review:review` |
| 审核拒绝     | PUT | `/api/sys/article-reviews/{id}/reject`  | `content:article-review:review` |
| 状态修正     | PUT | `/api/sys/article-reviews/{id}/repair-status` | `content:article-review:repair` |

#### 分页查询审核文章

- 请求：`GET /api/sys/article-reviews`
- 查询参数：`current`、`size`、`keyword`、`authorId`、`reviewStatus`
- 默认行为：`reviewStatus` 未传时按 `1=审核中` 查询。
- 响应字段：复用 `ArticleAdminVO`

#### 查询审核详情

- 请求：`GET /api/sys/article-reviews/{id}`
- 响应字段：`ArticleReviewAdminDetailVO`

| 字段         | 类型                        | 说明          |
|------------|---------------------------|-------------|
| `article`  | `ArticleDetailVO`         | 文章详情        |
| `reviewLogs` | `List<ArticleReviewLogVO>` | 审核日志列表 |

#### 审核通过

- 请求：`PUT /api/sys/article-reviews/{id}/approve`
- 请求体：`ArticleReviewDecisionRequest`

```json
{
  "reviewComment": "结构清晰，可以发布。"
}
```

#### 审核拒绝

- 请求：`PUT /api/sys/article-reviews/{id}/reject`
- 请求体：`ArticleReviewDecisionRequest`

```json
{
  "reviewComment": "请补充摘要和封面说明后重新提交。"
}
```

#### 修正审核状态

- 请求：`PUT /api/sys/article-reviews/{id}/repair-status`
- 请求体：`ArticleReviewRepairRequest`

```json
{
  "targetReviewStatus": 0,
  "reviewComment": "处理历史异常数据，恢复为未送审状态。"
}
```

- 关键规则：
    - 通过和拒绝都只允许处理当前 `reviewStatus=1` 的文章。
    - 拒绝时必须填写 `reviewComment`。
    - 状态修正只允许具备 `content:article-review:repair` 权限的超级管理员调用。
    - 状态修正可将文章审核状态修正为 `0` 未送审、`1` 审核中、`2` 审核通过、`3` 审核拒绝。
    - 状态修正必须填写 `reviewComment`，且目标状态不能与当前状态一致。
    - 每次提交、重提、通过、拒绝、状态修正都会追加一条审核日志，不覆盖历史记录。

### 4.3 分类管理

#### 接口速览

| 场景     | 方法     | 路径                                | 权限                        |
|--------|--------|-----------------------------------|---------------------------|
| 查询分类树  | GET    | `/api/sys/categories/tree`        | `content:category:query`  |
| 查询分类详情 | GET    | `/api/sys/categories/{id}`        | `content:category:query`  |
| 新增分类   | POST   | `/api/sys/categories`             | `content:category:create` |
| 修改分类   | PUT    | `/api/sys/categories/{id}`        | `content:category:update` |
| 修改分类状态 | PUT    | `/api/sys/categories/{id}/status` | `content:category:update` |
| 删除分类   | DELETE | `/api/sys/categories/{id}`        | `content:category:delete` |

#### 分类请求体

```json
{
  "parentId": 1,
  "name": "Java 后端",
  "code": "article-java-backend",
  "type": "article",
  "sortOrder": 1,
  "icon": "java",
  "description": "Spring Boot、MyBatis Plus 等后端内容",
  "status": 1
}
```

#### 分类响应字段

- `CategoryAdminVO`

| 字段            | 类型       | 说明             |
|---------------|----------|----------------|
| `id`          | Long     | 分类 ID          |
| `parentId`    | Long     | 父分类 ID         |
| `name`        | String   | 分类名称           |
| `code`        | String   | 分类编码           |
| `type`        | String   | 当前固定 `article` |
| `ancestors`   | String   | 祖先路径           |
| `level`       | Integer  | 层级             |
| `sortOrder`   | Integer  | 排序             |
| `icon`        | String   | 图标             |
| `description` | String   | 描述             |
| `status`      | Integer  | 状态             |
| `createdAt`   | DateTime | 创建时间           |
| `updatedAt`   | DateTime | 更新时间           |

#### 状态请求体

```json
{
  "status": 1
}
```

- 关键规则：
    - `type` 本期固定为 `article`。
    - 父分类不能为自身或自身子节点。
    - 删除前若存在子分类或已绑定文章，会返回业务异常。

### 4.4 标签管理

#### 接口速览

| 场景   | 方法     | 路径                   | 权限                   |
|------|--------|----------------------|----------------------|
| 标签列表 | GET    | `/api/sys/tags`      | `content:tag:query`  |
| 标签详情 | GET    | `/api/sys/tags/{id}` | `content:tag:query`  |
| 新增标签 | POST   | `/api/sys/tags`      | `content:tag:create` |
| 修改标签 | PUT    | `/api/sys/tags/{id}` | `content:tag:update` |
| 删除标签 | DELETE | `/api/sys/tags/{id}` | `content:tag:delete` |

#### 请求体

```json
{
  "name": "Spring Boot",
  "color": "#409EFF"
}
```

#### 响应字段

- `TagVO`

| 字段          | 类型       | 说明    |
|-------------|----------|-------|
| `id`        | Long     | 标签 ID |
| `name`      | String   | 标签名称  |
| `color`     | String   | 标签颜色  |
| `createdAt` | DateTime | 创建时间  |

- 关键规则：
    - 标签名称全局唯一。
    - 删除标签会同步清理 `sys_tag_relation` 关联记录。

### 4.5 评论管理

#### 接口速览

| 场景     | 方法     | 路径                              | 权限                       |
|--------|--------|---------------------------------|--------------------------|
| 评论分页   | GET    | `/api/sys/comments`             | `content:comment:query`  |
| 评论详情   | GET    | `/api/sys/comments/{id}`        | `content:comment:query`  |
| 修改评论状态 | PUT    | `/api/sys/comments/{id}/status` | `content:comment:update` |
| 删除评论   | DELETE | `/api/sys/comments/{id}`        | `content:comment:delete` |

#### 查询参数

- `current`、`size`、`targetId`、`targetType`、`userId`、`rootId`、`parentId`、`status`

#### 状态请求体

```json
{
  "status": 1
}
```

- 状态取值：`0` 待审核，`1` 正常，`2` 隐藏

#### 响应字段

- `CommentVO`

| 字段             | 类型           | 说明             |
|----------------|--------------|----------------|
| `id`           | Long         | 评论 ID          |
| `targetId`     | Long         | 目标 ID          |
| `targetType`   | String       | 当前固定 `article` |
| `content`      | String       | 评论内容           |
| `images`       | List<String> | 图片列表           |
| `userId`       | Long         | 评论用户 ID        |
| `userNickname` | String       | 评论用户昵称         |
| `userAvatar`   | String       | 评论用户头像         |
| `rootId`       | Long         | 根评论 ID         |
| `parentId`     | Long         | 父评论 ID         |
| `likeCount`    | Long         | 点赞数            |
| `replyCount`   | Long         | 回复数            |
| `status`       | Integer      | 状态             |
| `createdAt`    | DateTime     | 创建时间           |
| `liked`        | Boolean      | 当前用户是否已点赞      |
| `children`     | List         | 子回复            |

### 4.6 收藏管理

#### 接口速览

| 场景     | 方法     | 路径                             | 权限                          |
|--------|--------|--------------------------------|-----------------------------|
| 收藏夹分页  | GET    | `/api/sys/collections/folders` | `content:collection:query`  |
| 收藏记录分页 | GET    | `/api/sys/collections`         | `content:collection:query`  |
| 删除收藏记录 | DELETE | `/api/sys/collections/{id}`    | `content:collection:delete` |

#### 查询参数

- 收藏夹：`current`、`size`、`userId`
- 收藏记录：`current`、`size`、`userId`、`folderId`、`targetId`、`targetType`

#### 响应字段

- 收藏夹：`CollectionFolderVO`
- 收藏记录：`CollectionVO`

字段说明见本文件 `登录后用户行为接口` 中的收藏部分。

### 4.7 互动管理

#### 接口速览

| 场景   | 方法     | 路径                           | 权限                           |
|------|--------|------------------------------|------------------------------|
| 互动分页 | GET    | `/api/sys/interactions`      | `content:interaction:query`  |
| 删除互动 | DELETE | `/api/sys/interactions/{id}` | `content:interaction:delete` |

#### 查询参数

- `current`、`size`、`userId`、`targetId`、`targetType`、`actionType`

#### 响应字段

- `InteractionVO`

| 字段           | 类型       | 说明           |
|--------------|----------|--------------|
| `id`         | Long     | 互动记录 ID      |
| `userId`     | Long     | 用户 ID        |
| `targetId`   | Long     | 目标 ID        |
| `targetType` | String   | 目标类型         |
| `actionType` | String   | 当前主要为 `like` |
| `createdAt`  | DateTime | 创建时间         |

### 4.8 足迹管理

#### 接口速览

| 场景     | 方法     | 路径                         | 权限                         |
|--------|--------|----------------------------|----------------------------|
| 足迹分页   | GET    | `/api/sys/footprints`      | `content:footprint:query`  |
| 删除足迹   | DELETE | `/api/sys/footprints/{id}` | `content:footprint:delete` |
| 条件清空足迹 | DELETE | `/api/sys/footprints`      | `content:footprint:delete` |

#### 查询参数

- `current`、`size`、`userId`、`targetId`、`targetType`、`visitedAtStart`、`visitedAtEnd`

#### 响应字段

- `FootprintVO`

| 字段            | 类型       | 说明    |
|---------------|----------|-------|
| `id`          | Long     | 足迹 ID |
| `userId`      | Long     | 用户 ID |
| `targetId`    | Long     | 目标 ID |
| `targetType`  | String   | 目标类型  |
| `targetTitle` | String   | 目标标题  |
| `targetUrl`   | String   | 目标链接  |
| `ipAddress`   | String   | IP 地址 |
| `userAgent`   | String   | UA    |
| `visitedAt`   | DateTime | 访问时间  |

- 条件清理说明：
    - `DELETE /api/sys/footprints` 会按查询参数过滤条件批量清理。
    - 未传筛选参数时，按当前实现清理命中的全部足迹记录。

## 5. 权限、状态与取值速查

### 5.1 权限标识

| 权限标识                         | 说明          |
|------------------------------|-------------|
| `content:article:query`      | 查询后台文章      |
| `content:article:create`     | 新增文章        |
| `content:article:update`     | 修改文章、修改文章状态 |
| `content:article:delete`     | 删除文章        |
| `content:article:access`     | 配置文章访问名单    |
| `content:article-review:query` | 查询文章审核 |
| `content:article-review:review` | 审核文章 |
| `content:article-review:repair` | 修正异常审核状态 |
| `content:category:query`     | 查询分类        |
| `content:category:create`    | 新增分类        |
| `content:category:update`    | 修改分类、修改分类状态 |
| `content:category:delete`    | 删除分类        |
| `content:tag:query`          | 查询标签        |
| `content:tag:create`         | 新增标签        |
| `content:tag:update`         | 修改标签        |
| `content:tag:delete`         | 删除标签        |
| `content:comment:query`      | 查询评论        |
| `content:comment:update`     | 修改评论状态      |
| `content:comment:delete`     | 删除评论        |
| `content:collection:query`   | 查询收藏与收藏夹    |
| `content:collection:delete`  | 删除收藏记录      |
| `content:interaction:query`  | 查询互动        |
| `content:interaction:delete` | 删除互动        |
| `content:footprint:query`    | 查询足迹        |
| `content:footprint:delete`   | 删除 / 清空足迹   |

### 5.2 文章访问级别

| `accessLevel` | 说明     | 前台当前行为       |
|---------------|--------|--------------|
| `0`           | 公开     | 匿名可见         |
| `1`           | 登录可见   | 未登录返回登录错误    |
| `2`           | 付费可见   | 本期未开放，统一拒绝   |
| `3`           | VIP 可见 | 本期未开放，统一拒绝   |
| `4`           | 指定用户可见 | 按白名单 / 黑名单校验 |

- `accessType=1` 表示白名单授权。
- `accessType=2` 表示黑名单拒绝。
- 作者本人和具备 `content:article:query` 权限的后台用户可绕过普通访问限制。

### 5.3 常用业务状态

| 枚举   | 取值                                |
|------|-----------------------------------|
| 文章状态 | `0` 草稿，`1` 已发布                    |
| 分类状态 | `0` 禁用，`1` 启用                     |
| 评论状态 | `0` 待审核，`1` 正常，`2` 隐藏             |
| 互动类型 | 当前固定 `like`                       |
| 目标类型 | 文章相关固定 `article`，评论点赞固定 `comment` |

## 6. 常见联调问题

| 问题                             | 当前行为            |
|--------------------------------|-----------------|
| 匿名调用任意 `/api/user/**` 内容接口     | HTTP `401`      |
| 匿名调用任意 `/api/sys/**` 内容接口      | HTTP `401`      |
| 已登录但无 `content:*` 权限访问后台接口     | HTTP `403`      |
| `accessLevel=4` 文章未命中白名单或命中黑名单 | 返回业务错误码 `40300` |
| 删除分类前存在子节点或文章绑定                | 返回业务异常          |
| 删除默认收藏夹                        | 返回业务异常          |

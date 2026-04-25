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
| 分类筛选、导航树         | `GET /api/categories/tree`                                                  |
| 标签筛选             | `GET /api/tags`                                                             |
| 评论区              | `GET /api/comments`、`POST /api/user/comments`                               |
| 点赞按钮             | `POST /api/user/articles/{id}/likes`、`DELETE /api/user/articles/{id}/likes` |
| 收藏夹 / 收藏弹窗       | `/api/user/collection-folders`、`/api/user/collections`                      |
| 浏览历史             | `/api/user/footprints`                                                      |
| 后台文章管理           | `/api/sys/articles` 相关接口                                                    |
| 后台分类 / 标签 / 评论管理 | `/api/sys/categories`、`/api/sys/tags`、`/api/sys/comments`                   |

## 2. 前台页面接口

这一组接口支持博客前台页面直接使用，匿名用户也可以调，但文章详情仍会走资源级访问控制。

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
    "liked": false,
    "collected": false,
    "canComment": false
  }
}
```

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

### 3.1 文章点赞

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

### 3.2 评论行为

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

### 3.3 收藏夹与收藏

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

### 3.4 足迹

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
| `accessLevel`      | Integer  | 访问级别           |
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
| `accessLevel`  | Integer  | 访问级别  |
| `viewCount`    | Long     | 浏览数   |
| `likeCount`    | Long     | 点赞数   |
| `commentCount` | Long     | 评论数   |
| `collectCount` | Long     | 收藏数   |
| `shareCount`   | Long     | 分享数   |
| `publishTime`  | DateTime | 发布时间  |
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
| `accessLevel` | Integer                 | 否  | 访问级别                |
| `remark`      | String                  | 否  | 备注，最长 256           |
| `categoryIds` | List<Long>              | 否  | 分类 ID 列表            |
| `tagIds`      | List<Long>              | 否  | 标签 ID 列表            |
| `accessList`  | List<ArticleAccessItem> | 否  | `accessLevel=4` 时使用 |

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
  "accessLevel": 4,
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
    - `accessLevel=4` 时保存后会重建访问名单。
    - `accessLevel=2/3` 当前会统一拒绝前台访问。

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
    - 仅当文章 `accessLevel=4` 时允许调用。
    - 本次提交的名单会覆盖旧名单。
    - `expireTime` 为空表示长期有效。

#### 删除文章

- 请求：`DELETE /api/sys/articles/{id}`
- 当前行为：会级联清理分类绑定、标签绑定、访问名单、评论、收藏、点赞互动、浏览足迹。

### 4.2 分类管理

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

### 4.3 标签管理

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

### 4.4 评论管理

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

### 4.5 收藏管理

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

### 4.6 互动管理

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

### 4.7 足迹管理

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

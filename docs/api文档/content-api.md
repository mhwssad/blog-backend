# 内容域接口文档

## 基础信息

本文档面向前端联调，覆盖文章、分类、标签、评论、收藏、互动、足迹相关接口。

- **Base URL**: `/api`
- **认证方式**:
  - 前台公开只读接口支持匿名访问
  - 用户行为接口要求 `Authorization: Bearer <token>`
  - 后台管理接口要求登录且具备 `content:*` 权限
- **响应格式**: `application/json`
- **统一约束**:
  - 前台公开接口只读
  - 登录写操作统一走 `/api/user/**`
  - 后台管理统一走 `/api/sys/**`

## 通用响应结构

所有接口统一返回如下结构：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

分页接口 `data` 结构固定为：

```json
{
  "total": 1,
  "current": 1,
  "size": 10,
  "records": []
}
```

## 后台文章管理

### 1. 分页查询文章

- **请求方式**: `GET /api/sys/articles`
- **权限**: `content:article:query`
- **查询参数**:

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页数量，默认 `10` |
| keyword | String | 标题/摘要关键字 |
| authorId | Long | 作者 ID |
| status | Integer | 状态，`0` 草稿，`1` 已发布 |
| accessLevel | Integer | 访问级别 |
| categoryId | Long | 分类 ID |
| tagId | Long | 标签 ID |
| isTop | Integer | 是否置顶，`0/1` |
| publishTimeStart | DateTime | 发布时间起，格式 `yyyy-MM-dd HH:mm:ss` |
| publishTimeEnd | DateTime | 发布时间止，格式 `yyyy-MM-dd HH:mm:ss` |

- **响应字段**: `ArticleAdminVO`
  - `id`、`title`、`summary`、`coverImage`
  - `authorId`、`authorName`
  - `isTop`、`isOriginal`、`status`、`accessLevel`
  - `viewCount`、`likeCount`、`commentCount`、`collectCount`、`shareCount`
  - `publishTime`、`createdAt`、`updatedAt`、`remark`

- **响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "title": "Spring Boot 4 + JWT 认证实践",
        "summary": "使用当前项目的认证模块快速搭建账号登录能力。",
        "coverImage": null,
        "authorId": 1,
        "authorName": "管理员",
        "isTop": 1,
        "isOriginal": 1,
        "status": 1,
        "accessLevel": 0,
        "viewCount": 128,
        "likeCount": 1,
        "commentCount": 2,
        "collectCount": 1,
        "shareCount": 6,
        "publishTime": "2026-03-12 10:00:00",
        "createdAt": "2026-03-09 10:00:00",
        "updatedAt": "2026-03-18 10:00:00",
        "remark": "系统初始化示例文章"
      }
    ]
  }
}
```

### 2. 查询文章详情

- **请求方式**: `GET /api/sys/articles/{id}`
- **权限**: `content:article:query`
- **路径参数**: `id` 文章 ID
- **响应字段**: `ArticleDetailVO`
  - 基础字段同 `ArticleAdminVO`
  - 额外包含 `content`、`sourceUrl`、`categoryIds`、`tagIds`、`accessList`
- **访问控制列表项**: `ArticleAccessItem`
  - `userId`
  - `accessType`，`1` 白名单，`2` 黑名单
  - `expireTime`
  - `grantReason`

- **响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 3,
    "title": "仅对白名单开放的专栏样例",
    "summary": "演示 access_level=4 时的白名单授权数据结构。",
    "content": "当文章访问级别为指定用户可见时，可通过 blog_article_access 为白名单用户授权。",
    "coverImage": null,
    "authorId": 2,
    "authorName": "演示用户",
    "isTop": 0,
    "isOriginal": 1,
    "sourceUrl": null,
    "status": 1,
    "publishTime": "2026-03-18 10:00:00",
    "accessLevel": 4,
    "viewCount": 18,
    "likeCount": 0,
    "commentCount": 0,
    "collectCount": 0,
    "shareCount": 0,
    "createdAt": "2026-03-17 10:00:00",
    "updatedAt": "2026-03-19 10:00:00",
    "remark": "白名单文章示例",
    "categoryIds": [3],
    "tagIds": [1],
    "accessList": [
      {
        "userId": 2,
        "accessType": 1,
        "expireTime": null,
        "grantReason": "允许作者本人查看白名单文章"
      }
    ]
  }
}
```

### 3. 新增/修改文章

- **请求方式**:
  - `POST /api/sys/articles`
  - `PUT /api/sys/articles/{id}`
- **权限**:
  - 新增 `content:article:create`
  - 修改 `content:article:update`
- **请求体**: `ArticleSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | String | 是 | 文章标题，最长 128 |
| summary | String | 否 | 文章摘要，最长 2000 |
| content | String | 否 | 正文内容 |
| coverImage | String | 否 | 封面地址，最长 512 |
| authorId | Long | 是 | 作者 ID |
| isTop | Integer | 否 | 是否置顶 |
| isOriginal | Integer | 否 | 是否原创，默认 `1` |
| sourceUrl | String | 否 | 转载地址，非原创时必填 |
| status | Integer | 否 | `0` 草稿，`1` 已发布 |
| publishTime | DateTime | 否 | 发布时间 |
| accessLevel | Integer | 否 | 访问级别 |
| remark | String | 否 | 备注，最长 256 |
| categoryIds | List<Long> | 否 | 分类 ID 列表 |
| tagIds | List<Long> | 否 | 标签 ID 列表 |
| accessList | List<ArticleAccessItem> | 否 | `accessLevel=4` 时的白/黑名单 |

- **关键规则**:
  - `isOriginal=0` 时必须提供 `sourceUrl`
  - 仅允许绑定存在的文章分类和标签
  - `accessLevel=4` 时保存后会重建访问名单
  - `accessLevel=2/3` 允许保存配置，但前台只返回拒绝逻辑

- **请求示例**:

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

### 4. 修改文章状态

- **请求方式**: `PUT /api/sys/articles/{id}/status`
- **权限**: `content:article:update`
- **请求体**:

```json
{
  "status": 1
}
```

- **成功响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

### 5. 配置文章访问名单

- **请求方式**: `PUT /api/sys/articles/{id}/access`
- **权限**: `content:article:access`
- **请求体**:

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

- **关键规则**:
  - 仅当文章 `accessLevel=4` 时允许调用
  - 本次提交的名单会覆盖旧名单
  - `expireTime` 为空表示长期有效

### 6. 删除文章

- **请求方式**: `DELETE /api/sys/articles/{id}`
- **权限**: `content:article:delete`
- **说明**: 删除时会级联清理文章分类绑定、标签绑定、访问名单、评论、收藏、点赞互动、浏览足迹。

## 后台内容管理

### 1. 分类管理

- **分类树**: `GET /api/sys/categories/tree`，权限 `content:category:query`
- **分类详情**: `GET /api/sys/categories/{id}`，权限 `content:category:query`
- **新增分类**: `POST /api/sys/categories`，权限 `content:category:create`
- **修改分类**: `PUT /api/sys/categories/{id}`，权限 `content:category:update`
- **修改分类状态**: `PUT /api/sys/categories/{id}/status`，权限 `content:category:update`
- **删除分类**: `DELETE /api/sys/categories/{id}`，权限 `content:category:delete`

- **分类请求体**: `CategorySaveRequest`

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

- **分类详情/新增/修改响应字段**: `CategoryAdminVO`
  - `id`、`parentId`、`name`、`code`、`type`
  - `ancestors`、`level`、`sortOrder`
  - `icon`、`description`、`status`
  - `createdAt`、`updatedAt`

- **分类状态请求体**:

```json
{
  "status": 1
}
```

- **关键规则**:
  - `type` 本期固定为 `article`
  - 父分类不能为自身或自身子节点
  - 删除前若存在子分类或已绑定文章，返回业务异常

### 2. 标签管理

- **标签列表**: `GET /api/sys/tags`，权限 `content:tag:query`
- **标签详情**: `GET /api/sys/tags/{id}`，权限 `content:tag:query`
- **新增标签**: `POST /api/sys/tags`，权限 `content:tag:create`
- **修改标签**: `PUT /api/sys/tags/{id}`，权限 `content:tag:update`
- **删除标签**: `DELETE /api/sys/tags/{id}`，权限 `content:tag:delete`

- **请求体**:

```json
{
  "name": "Spring Boot",
  "color": "#409EFF"
}
```

- **响应字段**: `TagVO`
  - `id`、`name`、`color`、`createdAt`

- **列表响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "name": "Spring Boot",
      "color": "#409EFF",
      "createdAt": "2026-03-19 10:00:00"
    }
  ]
}
```

### 3. 评论管理

- **评论分页**: `GET /api/sys/comments`，权限 `content:comment:query`
- **评论详情**: `GET /api/sys/comments/{id}`，权限 `content:comment:query`
- **修改状态**: `PUT /api/sys/comments/{id}/status`，权限 `content:comment:update`
- **删除评论**: `DELETE /api/sys/comments/{id}`，权限 `content:comment:delete`

- **评论查询参数**: `current`、`size`、`targetId`、`targetType`、`userId`、`rootId`、`parentId`、`status`
- **评论状态请求体**:

```json
{
  "status": 1
}
```

- **响应字段**: `CommentVO`
  - `id`、`targetId`、`targetType`、`content`、`images`
  - `userId`、`userNickname`、`userAvatar`
  - `rootId`、`parentId`
  - `likeCount`、`replyCount`、`status`
  - `createdAt`、`liked`、`children`

### 4. 收藏管理

- **收藏夹分页**: `GET /api/sys/collections/folders`，权限 `content:collection:query`
- **收藏记录分页**: `GET /api/sys/collections`，权限 `content:collection:query`
- **删除收藏记录**: `DELETE /api/sys/collections/{id}`，权限 `content:collection:delete`

- **收藏查询参数**: `current`、`size`、`userId`、`folderId`、`targetId`、`targetType`

- **收藏夹分页响应字段**: `CollectionFolderVO`
  - `id`、`userId`、`folderName`、`folderType`
  - `description`、`isPublic`、`isDefault`
  - `sortOrder`、`collectionCount`
  - `createdAt`、`updatedAt`

- **收藏记录分页响应字段**: `CollectionVO`
  - `id`、`userId`、`folderId`、`targetId`
  - `targetType`、`remark`、`targetTitle`、`targetUrl`
  - `createdAt`

### 5. 互动管理

- **互动分页**: `GET /api/sys/interactions`，权限 `content:interaction:query`
- **删除互动**: `DELETE /api/sys/interactions/{id}`，权限 `content:interaction:delete`

- **互动查询参数**: `current`、`size`、`userId`、`targetId`、`targetType`、`actionType`

- **响应字段**: `InteractionVO`
  - `id`、`userId`、`targetId`、`targetType`、`actionType`、`createdAt`

### 6. 足迹管理

- **足迹分页**: `GET /api/sys/footprints`，权限 `content:footprint:query`
- **删除足迹**: `DELETE /api/sys/footprints/{id}`，权限 `content:footprint:delete`
- **清空足迹**: `DELETE /api/sys/footprints`，权限 `content:footprint:delete`

- **足迹查询参数**: `current`、`size`、`userId`、`targetId`、`targetType`、`visitedAtStart`、`visitedAtEnd`

- **响应字段**: `FootprintVO`
  - `id`、`userId`、`targetId`、`targetType`
  - `targetTitle`、`targetUrl`
  - `ipAddress`、`userAgent`、`visitedAt`

- **条件清理说明**:
  - `DELETE /api/sys/footprints` 按查询参数过滤条件批量清理
  - 未传筛选参数时表示按当前实现清理命中的全部足迹记录

## 前台公开内容接口

### 1. 文章列表

- **请求方式**: `GET /api/articles`
- **权限**: 匿名可访问
- **查询参数**:

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页数量 |
| keyword | String | 标题/摘要关键字 |
| categoryId | Long | 分类 ID |
| tagId | Long | 标签 ID |
| sort | String | `latest`、`top`、`hot` |

- **响应字段**: `PublicArticleCardVO`
  - `id`、`title`、`summary`、`coverImage`
  - `authorId`、`authorName`
  - `isTop`、`accessLevel`
  - `viewCount`、`likeCount`、`commentCount`、`collectCount`
  - `publishTime`

### 2. 文章详情

- **请求方式**: `GET /api/articles/{id}`
- **权限**: 匿名可访问，但资源级访问控制继续生效
- **路径参数**: `id` 文章 ID
- **响应字段**: `PublicArticleDetailVO`
  - `id`、`title`、`summary`、`content`、`coverImage`
  - `authorId`、`authorName`
  - `isTop`、`isOriginal`、`sourceUrl`
  - `accessLevel`、`viewCount`、`likeCount`、`commentCount`、`collectCount`、`shareCount`
  - `publishTime`
  - `categories`、`tags`
  - `liked`、`collected`、`canComment`

- **响应示例**:

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

### 3. 分类树

- **请求方式**: `GET /api/categories/tree`
- **权限**: 匿名可访问
- **响应字段**: `PublicCategoryTreeVO`
  - `id`、`parentId`、`name`、`code`、`type`、`level`、`sortOrder`、`icon`、`description`、`children`

- **响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "技术分享",
      "code": "article-tech",
      "type": "article",
      "level": 1,
      "sortOrder": 1,
      "icon": "cpu",
      "description": "技术类文章根分类",
      "children": [
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
      ]
    }
  ]
}
```

### 4. 标签列表

- **请求方式**: `GET /api/tags`
- **权限**: 匿名可访问
- **查询参数**: `targetType`，默认 `article`
- **响应字段**: `PublicTagVO`，包含 `id`、`name`、`color`

- **说明**:
  - 当前仅对 `targetType=article` 返回数据
  - 其他目标类型按当前实现返回空数组

### 5. 评论树

- **请求方式**: `GET /api/comments`
- **权限**: 匿名可访问
- **查询参数**: `current`、`size`、`targetType`、`targetId`
- **响应字段**: `PublicCommentVO`
  - `id`、`targetId`、`targetType`、`content`、`images`
  - `userId`、`userNickname`、`userAvatar`
  - `rootId`、`parentId`
  - `likeCount`、`replyCount`、`status`
  - `createdAt`、`liked`、`children`

- **说明**:
  - 当前返回树形结构
  - 仅返回 `status=1` 的评论
  - 已登录用户会额外得到当前评论的 `liked` 状态

## 用户内容行为接口

### 1. 文章点赞

- **点赞文章**: `POST /api/user/articles/{id}/likes`
- **取消点赞**: `DELETE /api/user/articles/{id}/likes`
- **认证**: Bearer Token
- **说明**: 重复点赞、重复取消点赞都按幂等处理。

- **成功响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

### 2. 评论行为

- **点赞评论**: `POST /api/user/comments/{id}/likes`
- **取消点赞评论**: `DELETE /api/user/comments/{id}/likes`
- **发表评论**: `POST /api/user/comments`
- **删除我的评论**: `DELETE /api/user/comments/{id}`

- **评论请求体**: `CommentSaveRequest`

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

- **关键规则**:
  - 当前仅支持 `targetType=article`
  - `parentId>0` 时必须与目标文章匹配
  - 删除评论仅允许删除当前登录用户自己的评论
  - 删除根评论时会级联删除其回复树

### 3. 收藏夹与收藏

- **查询我的收藏夹**: `GET /api/user/collection-folders`
- **新增收藏夹**: `POST /api/user/collection-folders`
- **修改收藏夹**: `PUT /api/user/collection-folders/{id}`
- **删除收藏夹**: `DELETE /api/user/collection-folders/{id}`
- **查询我的收藏**: `GET /api/user/collections`
- **新增收藏**: `POST /api/user/collections`
- **删除收藏**: `DELETE /api/user/collections/{id}`

- **收藏夹请求体**: `CollectionFolderSaveRequest`

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

- **收藏请求体**: `CollectionSaveRequest`

```json
{
  "folderId": 1,
  "targetId": 1,
  "targetType": "article",
  "remark": "待复习"
}
```

- **收藏夹响应字段**: `CollectionFolderVO`
  - `id`、`userId`、`folderName`、`folderType`、`description`
  - `isPublic`、`isDefault`、`sortOrder`、`collectionCount`
  - `createdAt`、`updatedAt`

- **收藏响应字段**: `CollectionVO`
  - `id`、`folderId`、`targetId`、`targetType`
  - `remark`、`targetTitle`、`targetUrl`、`createdAt`

- **关键规则**:
  - 当前仅支持收藏 `article`
  - 未传 `folderId` 时自动落入默认收藏夹，不存在则自动创建
  - 默认收藏夹不可删除

### 4. 足迹

- **查询我的足迹**: `GET /api/user/footprints`
- **删除我的足迹**: `DELETE /api/user/footprints/{id}`
- **清空我的足迹**: `DELETE /api/user/footprints`
- **查询参数**: `current`、`size`、`targetType`
- **响应字段**: `UserFootprintVO`，包含 `id`、`targetId`、`targetType`、`targetTitle`、`targetUrl`、`visitedAt`

- **说明**:
  - 登录用户访问前台文章详情时，系统会按当前实现自动记录文章足迹
  - 同一用户同一文章按 upsert 方式更新访问记录，而不是重复插入

## 权限标识列表

| 权限标识 | 说明 |
| --- | --- |
| `content:article:query` | 查询后台文章 |
| `content:article:create` | 新增文章 |
| `content:article:update` | 修改文章、修改文章状态 |
| `content:article:delete` | 删除文章 |
| `content:article:access` | 配置文章访问名单 |
| `content:category:query` | 查询分类 |
| `content:category:create` | 新增分类 |
| `content:category:update` | 修改分类、修改分类状态 |
| `content:category:delete` | 删除分类 |
| `content:tag:query` | 查询标签 |
| `content:tag:create` | 新增标签 |
| `content:tag:update` | 修改标签 |
| `content:tag:delete` | 删除标签 |
| `content:comment:query` | 查询评论 |
| `content:comment:update` | 修改评论状态 |
| `content:comment:delete` | 删除评论 |
| `content:collection:query` | 查询收藏与收藏夹 |
| `content:collection:delete` | 删除收藏记录 |
| `content:interaction:query` | 查询互动 |
| `content:interaction:delete` | 删除互动 |
| `content:footprint:query` | 查询足迹 |
| `content:footprint:delete` | 删除/清空足迹 |

## 访问级别说明

| accessLevel | 说明 | 当前行为 |
| --- | --- | --- |
| `0` | 公开 | 匿名可见 |
| `1` | 登录可见 | 未登录返回登录错误 |
| `2` | 付费可见 | 本期未开放，统一拒绝 |
| `3` | VIP 可见 | 本期未开放，统一拒绝 |
| `4` | 指定用户可见 | 按白名单/黑名单校验 |

- `accessType=1` 表示白名单授权。
- `accessType=2` 表示黑名单拒绝。
- 作者本人和具备 `content:article:query` 权限的后台用户可绕过普通访问限制。

## 附录

### 1. 常用业务状态

- 文章状态: `0` 草稿，`1` 已发布
- 分类状态: `0` 禁用，`1` 启用
- 评论状态: `0` 隐藏，`1` 展示
- 互动类型: 目前固定 `like`
- 目标类型:
  - 文章相关固定 `article`
  - 评论点赞固定 `comment`

### 2. 典型错误场景

- 匿名调用任意 `/api/user/**` 接口，HTTP 返回 `401`
- 匿名调用任意 `/api/sys/**` 接口，HTTP 返回 `401`
- 已登录但无 `content:*` 权限访问后台接口，HTTP 返回 `403`
- `accessLevel=4` 文章未命中白名单或命中黑名单，返回业务错误码 `40300`
- 删除分类前存在子节点或文章绑定，返回业务异常
- 删除默认收藏夹，返回业务异常



# 论坛 API

本文档覆盖二期论坛 P0 的公开查询接口、登录用户行为接口、帖子与频道挂接入口，以及 P1 后台版块管理、帖子/回复治理接口。

## 1. 路由分组

| 路由前缀 | 面向场景 | 是否需要登录 |
| --- | --- | --- |
| `/api/forum/**` | 论坛公开浏览 | 否，登录可见内容需要登录 |
| `/api/user/forum/**` | 用户发帖、回帖、互动、分享 | 是 |
| `/api/user/chat/forum-links/**` | 频道侧帖子挂接查询与取消 | 是 |
| `/api/sys/forum/**` | 后台论坛治理 | 是，需后台权限 |

## 2. 公开论坛接口

### 2.1 查询版块列表

- 请求：`GET /api/forum/sections`
- 鉴权：否；登录用户可额外看到登录可见版块
- 响应：`List<ForumSectionVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 版块 ID |
| `name` | String | 版块名称 |
| `description` | String | 版块简介 |
| `sortOrder` | Integer | 排序值 |
| `visibilityScope` | Integer | `0` 公开，`1` 登录可见 |
| `postLevelLimit` | Integer | 发帖最低等级 |
| `status` | Integer | `0` 禁用，`1` 启用 |

### 2.2 分页查询帖子

- 请求：`GET /api/forum/posts`
- 鉴权：否；匿名只返回公开版块和公开帖子
- 查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页数量，默认 `10` |
| `keyword` | String | 标题 / 内容关键字 |
| `sectionId` | Long | 版块 ID |
| `authorId` | Long | 作者 ID |
| `createdAtStart` | DateTime | 创建时间开始 |
| `createdAtEnd` | DateTime | 创建时间结束 |
| `sort` | String | `latest`、`hot` |

### 2.3 查询帖子详情

- 请求：`GET /api/forum/posts/{id}`
- 鉴权：否；登录可见帖子或版块要求登录
- 响应：`PublicForumPostDetailVO`

关键字段：`id`、`sectionId`、`sectionName`、`authorId`、`authorName`、`title`、`content`、`status`、`visibilityScope`、`viewCount`、`likeCount`、`replyCount`、`collectCount`、`shareCount`、`liked`、`collected`、`canReply`、`linkedChannel`。

### 2.4 分页查询回复

- 请求：`GET /api/forum/posts/{postId}/replies`
- 鉴权：否；跟随帖子详情可见性
- 查询参数：`current`、`size`
- 说明：分页只作用于根回复，当前页根回复下的子回复以树形结构返回。

## 3. 用户论坛接口

所有接口都要求：

```http
Authorization: Bearer <accessToken>
```

### 3.1 我的帖子

| 场景 | 方法 | 路径 |
| --- | --- | --- |
| 我的帖子分页 | GET | `/api/user/forum/posts` |
| 我的帖子详情 | GET | `/api/user/forum/posts/{id}` |
| 创建帖子 | POST | `/api/user/forum/posts` |
| 编辑帖子 | PUT | `/api/user/forum/posts/{id}` |
| 删除帖子 | DELETE | `/api/user/forum/posts/{id}` |

创建 / 编辑请求体：

```json
{
  "sectionId": 1,
  "title": "论坛帖子标题",
  "content": "帖子正文",
  "status": 1,
  "visibilityScope": 0
}
```

规则：

- `status` 仅支持 `0` 草稿、`1` 已发布。
- `visibilityScope` 仅支持 `0` 公开、`1` 登录可见。
- 用户只能编辑和删除自己的帖子。
- 发帖前会校验版块状态和发帖等级限制。

### 3.2 回复

| 场景 | 方法 | 路径 |
| --- | --- | --- |
| 发表回复 | POST | `/api/user/forum/posts/{postId}/replies` |
| 编辑回复 | PUT | `/api/user/forum/replies/{replyId}` |
| 删除回复 | DELETE | `/api/user/forum/replies/{replyId}` |

请求体：

```json
{
  "parentId": 100,
  "content": "回复内容"
}
```

规则：

- `parentId` 为空表示根回复。
- 只能编辑或删除自己的回复。
- 回复已发布帖子时会同步帖子回复数；回复指定回复时同步父回复回复数。

### 3.3 点赞与收藏

| 场景 | 方法 | 路径 |
| --- | --- | --- |
| 点赞帖子 | POST | `/api/user/forum/posts/{postId}/likes` |
| 取消点赞 | DELETE | `/api/user/forum/posts/{postId}/likes` |
| 收藏帖子 | POST | `/api/user/forum/posts/{postId}/collections` |
| 取消收藏 | DELETE | `/api/user/forum/posts/{postId}/collections` |

收藏请求体可为空，也可指定收藏夹：

```json
{
  "folderId": 10,
  "remark": "稍后阅读"
}
```

规则：

- 点赞和收藏均为幂等操作。
- 未指定收藏夹时，后端会自动创建或复用默认论坛收藏夹。
- 互动会同步帖子统计字段。

### 3.4 分享帖子到频道

- 请求：`POST /api/user/forum/posts/{postId}/channel-share`
- 请求体：

```json
{
  "conversationId": 99
}
```

规则：

- 只能分享已发布帖子。
- 当前用户必须是目标频道正常成员。
- 被禁言用户不能分享。
- 当前阶段一个帖子只能挂接一个频道；重复分享到同一频道时幂等返回。

## 4. 频道侧论坛挂接接口

| 场景 | 方法 | 路径 |
| --- | --- | --- |
| 分享帖子到频道 | POST | `/api/user/chat/forum-links` |
| 查询帖子关联频道 | GET | `/api/user/chat/forum-links/posts/{forumPostId}` |
| 分页查询频道挂接帖子 | GET | `/api/user/chat/forum-links/channels/{conversationId}` |
| 取消帖子频道关联 | DELETE | `/api/user/chat/forum-links/posts/{forumPostId}` |

说明：`POST /api/user/chat/forum-links` 是频道侧旧入口，与 `/api/user/forum/posts/{postId}/channel-share` 共用同一条分享校验规则。

## 5. 后台论坛版块管理

后台版块管理接口统一要求登录后台账号，并按接口校验 `content:forum:*` 权限。

### 5.1 分页查询版块

- 请求：`GET /api/sys/forum/sections`
- 鉴权：`content:forum:query`
- 查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页数量，默认 `10`，最大 `100` |
| `keyword` | String | 版块名称 / 简介关键字 |
| `status` | Integer | `0` 禁用，`1` 启用 |
| `visibilityScope` | Integer | `0` 公开，`1` 登录可见 |

- 响应：`PageResult<ForumSectionAdminVO>`

### 5.2 查询版块详情

- 请求：`GET /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:query`
- 响应：`ForumSectionAdminVO`

### 5.3 新增版块

- 请求：`POST /api/sys/forum/sections`
- 鉴权：`content:forum:create`
- 请求体：

```json
{
  "name": "技术交流",
  "description": "技术问题与经验分享",
  "sortOrder": 10,
  "visibilityScope": 0,
  "postLevelLimit": 1,
  "status": 1
}
```

### 5.4 修改版块

- 请求：`PUT /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:update`
- 请求体同新增版块。

### 5.5 修改版块状态

- 请求：`PUT /api/sys/forum/sections/{id}/status`
- 鉴权：`content:forum:update`
- 请求体：

```json
{
  "status": 0
}
```

### 5.6 删除版块

- 请求：`DELETE /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:delete`
- 规则：仅允许删除无帖子版块；已存在帖子时应改用禁用状态。

### 5.7 后台版块字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 版块 ID |
| `name` | String | 版块名称，必填，最大 64，唯一 |
| `description` | String | 版块简介，最大 512 |
| `sortOrder` | Integer | 排序值，空值默认 `0` |
| `visibilityScope` | Integer | `0` 公开，`1` 登录可见，空值默认 `0` |
| `postLevelLimit` | Integer | 发帖最低等级，空值默认 `1`，最小 `1` |
| `status` | Integer | `0` 禁用，`1` 启用，空值默认 `1` |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

异常边界：

- 版块不存在：返回业务失败，提示 `论坛版块不存在`。
- 版块名称重复：返回 `60006`，提示 `版块名称已存在`。
- 状态或可见范围非法：返回 `40011`。
- 删除已有帖子版块：返回 `40011`，提示 `当前版块已存在帖子，无法删除`。

## 6. 状态与枚举

| 枚举 | 值 |
| --- | --- |
| 帖子状态 | `0` 草稿，`1` 已发布，`2` 审核中，`3` 已拒绝，`4` 已删除，`5` 隐藏 |
| 回复状态 | `1` 正常，`2` 隐藏，`3` 删除，`4` 审核中 |
| 可见范围 | `0` 公开，`1` 登录可见 |
| 互动目标类型 | `forum_post` |
| 互动行为 | `like` |

## 7. 后台权限

| 权限 | 说明 |
| --- | --- |
| `content:forum:query` | 查询后台论坛版块/帖子/回复 |
| `content:forum:create` | 新增论坛版块 |
| `content:forum:update` | 修改论坛版块或状态，隐藏/恢复/置顶/精华帖子/回复 |
| `content:forum:delete` | 删除论坛版块/帖子/回复 |

## 8. 后台帖子管理接口

### 8.1 分页查询帖子

- 请求：`GET /api/sys/forum/posts`
- 鉴权：`content:forum:query`
- 响应：`PageResult<ForumPostAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 帖子 ID |
| `sectionId` | Long | 版块 ID |
| `sectionName` | String | 版块名称 |
| `authorId` | Long | 作者 ID |
| `authorName` | String | 作者昵称 |
| `title` | String | 帖子标题 |
| `status` | Integer | 状态值 |
| `statusName` | String | 状态描述 |
| `visibilityScope` | Integer | 可见范围 |
| `isTop` | Integer | 是否置顶 |
| `isEssence` | Integer | 是否精华 |
| `viewCount` | Integer | 浏览数 |
| `likeCount` | Integer | 点赞数 |
| `replyCount` | Integer | 回复数 |
| `collectCount` | Integer | 收藏数 |
| `shareCount` | Integer | 分享数 |
| `publishedAt` | DateTime | 发布时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

查询参数：`keyword`（标题/内容）、`sectionId`、`authorId`、`status`、`createdAtStart`、`createdAtEnd`、`isTop`、`isEssence`、`current`、`size`

### 8.2 查询帖子详情

- 请求：`GET /api/sys/forum/posts/{id}`
- 鉴权：`content:forum:query`
- 响应：`ForumPostAdminDetailVO`（列表 VO + `content` 字段）

### 8.3 隐藏帖子

- 请求：`PUT /api/sys/forum/posts/{id}/hide`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`
- 已删除帖子不能隐藏

### 8.4 恢复帖子

- 请求：`PUT /api/sys/forum/posts/{id}/restore`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`
- 仅隐藏状态可恢复，恢复后状态为已发布

### 8.5 删除帖子

- 请求：`DELETE /api/sys/forum/posts/{id}`
- 鉴权：`content:forum:delete`
- 响应：`Result<Void>`
- 软删除，已删除帖子不能重复删除

### 8.6 切换置顶

- 请求：`PUT /api/sys/forum/posts/{id}/top?enabled=true|false`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`

### 8.7 切换精华

- 请求：`PUT /api/sys/forum/posts/{id}/essence?enabled=true|false`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`
- 设为精华时通知帖子作者

## 9. 后台回复管理接口

### 9.1 分页查询回复

- 请求：`GET /api/sys/forum/replies`
- 鉴权：`content:forum:query`
- 响应：`PageResult<ForumReplyAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 回复 ID |
| `postId` | Long | 帖子 ID |
| `postTitle` | String | 帖子标题 |
| `parentId` | Long | 父回复 ID |
| `rootId` | Long | 根回复 ID |
| `userId` | Long | 回复用户 ID |
| `userName` | String | 回复用户昵称 |
| `content` | String | 回复内容 |
| `status` | Integer | 状态值 |
| `statusName` | String | 状态描述 |
| `floorNo` | Integer | 楼层号 |
| `likeCount` | Integer | 点赞数 |
| `replyCount` | Integer | 回复数 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

查询参数：`keyword`（内容）、`postId`、`userId`、`status`、`current`、`size`

### 9.2 隐藏回复

- 请求：`PUT /api/sys/forum/replies/{id}/hide`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`
- 已删除回复不能隐藏

### 9.3 恢复回复

- 请求：`PUT /api/sys/forum/replies/{id}/restore`
- 鉴权：`content:forum:update`
- 响应：`Result<Void>`
- 仅隐藏状态可恢复，恢复后状态为正常

### 9.4 删除回复

- 请求：`DELETE /api/sys/forum/replies/{id}`
- 鉴权：`content:forum:delete`
- 响应：`Result<Void>`
- 软删除，同步递减帖子回复数和父回复回复数

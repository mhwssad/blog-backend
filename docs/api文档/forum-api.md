# 论坛 API

本文档覆盖二期论坛 P0 的公开查询接口、登录用户行为接口，以及帖子与频道挂接入口。

## 1. 路由分组

| 路由前缀 | 面向场景 | 是否需要登录 |
| --- | --- | --- |
| `/api/forum/**` | 论坛公开浏览 | 否，登录可见内容需要登录 |
| `/api/user/forum/**` | 用户发帖、回帖、互动、分享 | 是 |
| `/api/user/chat/forum-links/**` | 频道侧帖子挂接查询与取消 | 是 |

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

## 5. 状态与枚举

| 枚举 | 值 |
| --- | --- |
| 帖子状态 | `0` 草稿，`1` 已发布，`2` 审核中，`3` 已拒绝，`4` 已删除 |
| 回复状态 | `1` 正常，`2` 隐藏，`3` 删除，`4` 审核中 |
| 可见范围 | `0` 公开，`1` 登录可见 |
| 互动目标类型 | `forum_post` |
| 互动行为 | `like` |

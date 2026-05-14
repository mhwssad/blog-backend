# 论坛 API 前端参考手册

> 本文档面向前端联调，覆盖论坛版块、帖子、回复、用户互动与后台管理完整能力。

**基础信息**

- 基础路径：`/api`
- 内容类型：`application/json`
- 统一响应格式：`Result<T>` 或 `Result<PageResult<T>>`
- 通用响应字段：`code`(业务码)、`message`(信息)、`timestamp`(时间)、`data`(数据)

**业务码约定**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 解析 data |
| 40011 | 参数/业务校验失败 | 提示 message |
| 40101 | 未登录 | 跳转登录页 |
| 40301 | 无权限 | 提示无权限 |
| 40401 | 资源不存在 | 提示不存在 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 论坛首页

### 获取版块列表

**接口信息**

- 路径：`GET /api/forum/sections`
- 鉴权：否
- 说明：获取所有论坛版块列表，未登录用户只看公开版块，登录用户额外看到登录可见版块

**请求示例**

```javascript
// axios
axios.get('/api/forum/sections')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": [
    {
      "id": 1,
      "name": "技术交流",
      "description": "技术问题与经验分享",
      "sortOrder": 10,
      "visibilityScope": 0,
      "postLevelLimit": 1,
      "status": 1,
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-10T12:00:00"
    },
    {
      "id": 2,
      "name": "内部交流",
      "description": "仅登录用户可见",
      "sortOrder": 20,
      "visibilityScope": 1,
      "postLevelLimit": 5,
      "status": 1,
      "createdAt": "2025-01-02T00:00:00",
      "updatedAt": "2025-01-11T12:00:00"
    }
  ]
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 版块ID |
| `name` | String | 版块名称 |
| `description` | String | 版块简介 |
| `sortOrder` | Integer | 排序值，值越小越靠前 |
| `visibilityScope` | Integer | `0` 公开，`1` 登录可见 |
| `postLevelLimit` | Integer | 发帖最低等级要求 |
| `status` | Integer | `0` 禁用，`1` 启用 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 50001 | 系统异常 | 提示稍后重试 |

---

### 分页查询帖子

**接口信息**

- 路径：`GET /api/forum/posts`
- 鉴权：否
- 说明：分页查询公开帖子列表，匿名用户只看公开版块和公开帖子，登录用户额外看到登录可见内容

**请求示例**

```javascript
// 查询全部最新帖子（默认）
axios.get('/api/forum/posts')

// 查询指定版块
axios.get('/api/forum/posts?sectionId=1')

// 关键字搜索
axios.get('/api/forum/posts?keyword=Java&sort=hot')

// 组合查询
axios.get('/api/forum/posts?sectionId=1&authorId=100&sort=latest&current=1&size=20')
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量 | 10 |
| `keyword` | String | 否 | 标题/内容关键字搜索 | - |
| `sectionId` | Long | 否 | 版块ID | - |
| `authorId` | Long | 否 | 作者ID | - |
| `createdAtStart` | DateTime | 否 | 创建时间开始，格式 `yyyy-MM-dd HH:mm:ss` | - |
| `createdAtEnd` | DateTime | 否 | 创建时间结束，格式 `yyyy-MM-dd HH:mm:ss` | - |
| `sort` | String | 否 | `latest`(最新) 或 `hot`(热门) | latest |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 10,
        "sectionId": 1,
        "sectionName": "技术交流",
        "authorId": 100,
        "authorName": "张三",
        "title": "Spring Boot 最佳实践",
        "status": 1,
        "visibilityScope": 0,
        "isTop": 1,
        "isEssence": 1,
        "viewCount": 1523,
        "likeCount": 88,
        "replyCount": 35,
        "collectCount": 12,
        "shareCount": 5,
        "publishedAt": "2025-01-14T15:30:00",
        "createdAt": "2025-01-14T15:30:00",
        "updatedAt": "2025-01-14T18:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records[].id` | Long | 帖子ID |
| `records[].sectionId` | Long | 版块ID |
| `records[].sectionName` | String | 版块名称 |
| `records[].authorId` | Long | 作者ID |
| `records[].authorName` | String | 作者昵称 |
| `records[].title` | String | 帖子标题 |
| `records[].status` | Integer | `0`草稿`1`已发布`5`隐藏 |
| `records[].visibilityScope` | Integer | `0`公开`1`登录可见 |
| `records[].isTop` | Integer | `0`否`1`是 |
| `records[].isEssence` | Integer | `0`否`1`是 |
| `records[].viewCount` | Integer | 浏览数 |
| `records[].likeCount` | Integer | 点赞数 |
| `records[].replyCount` | Integer | 回复数 |
| `records[].collectCount` | Integer | 收藏数 |
| `records[].shareCount` | Integer | 分享数 |
| `records[].publishedAt` | DateTime | 发布时间 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 50001 | 系统异常 | 提示稍后重试 |

---

## 帖子详情页

### 查询帖子详情

**接口信息**

- 路径：`GET /api/forum/posts/{id}`
- 鉴权：否
- 说明：获取帖子详情，包含内容、互动状态和关联频道信息

**请求示例**

```javascript
axios.get('/api/forum/posts/10')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 10,
    "sectionId": 1,
    "sectionName": "技术交流",
    "authorId": 100,
    "authorName": "张三",
    "title": "Spring Boot 最佳实践",
    "content": "本文总结了 Spring Boot 开发的最佳实践，包括配置管理、性能优化等方面...",
    "status": 1,
    "visibilityScope": 0,
    "isTop": 1,
    "isEssence": 1,
    "viewCount": 1523,
    "likeCount": 88,
    "replyCount": 35,
    "collectCount": 12,
    "shareCount": 5,
    "publishedAt": "2025-01-14T15:30:00",
    "createdAt": "2025-01-14T15:30:00",
    "updatedAt": "2025-01-14T18:00:00",
    "liked": false,
    "collected": false,
    "canReply": true,
    "linkedChannel": {
      "id": 5,
      "forumPostId": 10,
      "conversationId": 99,
      "channelName": "技术交流频道",
      "linkType": "forum_share",
      "linkedBy": 100,
      "linkedAt": "2025-01-14T16:00:00"
    }
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| (基础字段同帖子列表) | - | - |
| `content` | String | 帖子正文内容 |
| `liked` | Boolean | 当前用户是否已点赞（需登录） |
| `collected` | Boolean | 当前用户是否已收藏（需登录） |
| `canReply` | Boolean | 当前用户是否可以回复（需登录+帖子已发布+用户未被禁言） |
| `linkedChannel` | Object | 关联的频道信息，无关联时为 `null` |

**linkedChannel 字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 关联记录ID |
| `forumPostId` | Long | 论坛帖子ID |
| `conversationId` | Long | 频道会话ID |
| `channelName` | String | 频道名称 |
| `linkType` | String | 关联方式，如 `forum_share` |
| `linkedBy` | Long | 关联人ID |
| `linkedAt` | DateTime | 关联时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40011 | 帖子不可见 | 提示无权访问 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 分页查询回复

**接口信息**

- 路径：`GET /api/forum/posts/{postId}/replies`
- 鉴权：否
- 说明：分页查询帖子回复，根回复分页展示，子回复以树形结构嵌套在根回复下

**请求示例**

```javascript
// 获取第1页，每页10条
axios.get('/api/forum/posts/10/replies?current=1&size=10')
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量 | 10 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 35,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1001,
        "postId": 10,
        "parentId": null,
        "rootId": null,
        "userId": 101,
        "userName": "李四",
        "content": "写得很好，收藏了！",
        "status": 1,
        "floorNo": 1,
        "likeCount": 5,
        "replyCount": 2,
        "createdAt": "2025-01-14T16:00:00",
        "updatedAt": "2025-01-14T16:00:00",
        "children": [
          {
            "id": 1002,
            "postId": 10,
            "parentId": 1001,
            "rootId": 1001,
            "userId": 100,
            "userName": "张三",
            "content": "感谢支持！",
            "status": 1,
            "floorNo": 2,
            "likeCount": 1,
            "replyCount": 0,
            "createdAt": "2025-01-14T16:30:00",
            "updatedAt": "2025-01-14T16:30:00",
            "children": []
          }
        ]
      },
      {
        "id": 1003,
        "postId": 10,
        "parentId": null,
        "rootId": null,
        "userId": 102,
        "userName": "王五",
        "content": "请问有源码吗？",
        "status": 1,
        "floorNo": 3,
        "likeCount": 2,
        "replyCount": 1,
        "createdAt": "2025-01-14T17:00:00",
        "updatedAt": "2025-01-14T17:00:00",
        "children": []
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 根回复总数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records[].id` | Long | 回复ID |
| `records[].postId` | Long | 所属帖子ID |
| `records[].parentId` | Long | 父回复ID，顶级回复为 `null` |
| `records[].rootId` | Long | 根回复ID，顶级回复为 `null` |
| `records[].userId` | Long | 回复用户ID |
| `records[].userName` | String | 回复用户昵称 |
| `records[].content` | String | 回复内容 |
| `records[].status` | Integer | `1`正常`2`隐藏`3`删除 |
| `records[].floorNo` | Integer | 楼层号 |
| `records[].likeCount` | Integer | 点赞数 |
| `records[].replyCount` | Integer | 该回复下的子回复数 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |
| `records[].children` | Array | 子回复列表（树形结构） |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 发帖与编辑

> 以下接口均要求登录

**通用鉴权头**

```http
Authorization: Bearer <accessToken>
```

---

### 创建帖子

**接口信息**

- 路径：`POST /api/user/forum/posts`
- 鉴权：是
- 说明：创建新的论坛帖子，支持发布或存草稿

**请求示例**

```javascript
// 发布帖子
axios.post('/api/user/forum/posts', {
  sectionId: 1,
  title: "我的新帖子",
  content: "这是帖子内容...",
  status: 1,
  visibilityScope: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 保存草稿
axios.post('/api/user/forum/posts', {
  sectionId: 1,
  title: "草稿帖子",
  content: "",
  status: 0,
  visibilityScope: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `sectionId` | Long | 是 | 目标版块ID |
| `title` | String | 是 | 帖子标题，最大128字符 |
| `content` | String | 否 | 帖子正文内容 |
| `status` | Integer | 否 | `0`草稿`1`已发布，默认`1` |
| `visibilityScope` | Integer | 否 | `0`公开`1`登录可见，默认`0` |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 20,
    "sectionId": 1,
    "sectionName": "技术交流",
    "authorId": 100,
    "authorName": "张三",
    "title": "我的新帖子",
    "content": "这是帖子内容...",
    "status": 1,
    "visibilityScope": 0,
    "isTop": 0,
    "isEssence": 0,
    "viewCount": 0,
    "likeCount": 0,
    "replyCount": 0,
    "collectCount": 0,
    "shareCount": 0,
    "publishedAt": "2025-01-15T10:30:00",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00",
    "liked": false,
    "collected": false,
    "canReply": true,
    "linkedChannel": null
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40011 | 版块不存在或已禁用 | 提示版块不可用 |
| 40011 | 用户等级不足 | 提示等级要求 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 编辑帖子

**接口信息**

- 路径：`PUT /api/user/forum/posts/{id}`
- 鉴权：是
- 说明：编辑自己的帖子，仅作者可操作

**请求示例**

```javascript
axios.put('/api/user/forum/posts/20', {
  sectionId: 1,
  title: "修改后的标题",
  content: "修改后的内容...",
  status: 1,
  visibilityScope: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `sectionId` | Long | 是 | 目标版块ID |
| `title` | String | 是 | 帖子标题，最大128字符 |
| `content` | String | 否 | 帖子正文内容 |
| `status` | Integer | 否 | `0`草稿`1`已发布 |
| `visibilityScope` | Integer | 否 | `0`公开`1`登录可见 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:00:00",
  "data": {
    "id": 20,
    "sectionId": 1,
    "sectionName": "技术交流",
    "authorId": 100,
    "authorName": "张三",
    "title": "修改后的标题",
    "content": "修改后的内容...",
    "status": 1,
    "visibilityScope": 0,
    "isTop": 0,
    "isEssence": 0,
    "viewCount": 0,
    "likeCount": 0,
    "replyCount": 0,
    "collectCount": 0,
    "shareCount": 0,
    "publishedAt": "2025-01-15T10:30:00",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T11:00:00",
    "liked": false,
    "collected": false,
    "canReply": true,
    "linkedChannel": null
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40011 | 版块不存在或已禁用 | 提示版块不可用 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 非作者无权编辑 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 删除帖子

**接口信息**

- 路径：`DELETE /api/user/forum/posts/{id}`
- 鉴权：是
- 说明：删除自己的帖子，仅作者可操作

**请求示例**

```javascript
axios.delete('/api/user/forum/posts/20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 非作者无权删除 | 提示无权限 |
| 40011 | 帖子已删除 | 提示帖子已删除 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 回复功能

> 以下接口均要求登录

---

### 发表回复

**接口信息**

- 路径：`POST /api/user/forum/posts/{postId}/replies`
- 鉴权：是
- 说明：对帖子发表回复，支持回复某个已有回复（嵌套回复）

**请求示例**

```javascript
// 发表根回复
axios.post('/api/user/forum/posts/10/replies', {
  parentId: null,
  content: "这是我的回复内容"
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 回复某个已有回复
axios.post('/api/user/forum/posts/10/replies', {
  parentId: 1001,
  content: "这是我对某人的回复"
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `parentId` | Long | 否 | 父回复ID，空表示根回复 |
| `content` | String | 是 | 回复内容，最大5000字符 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40011 | 帖子已删除/隐藏 | 提示无法回复 |
| 40011 | 用户被禁言 | 提示禁言状态 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 编辑回复

**接口信息**

- 路径：`PUT /api/user/forum/replies/{replyId}`
- 鉴权：是
- 说明：编辑自己的回复，仅作者可操作

**请求示例**

```javascript
axios.put('/api/user/forum/replies/1002', {
  parentId: 1001,
  content: "修改后的回复内容"
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `parentId` | Long | 否 | 父回复ID，需与原值一致 |
| `content` | String | 是 | 回复内容，最大5000字符 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40401 | 回复不存在 | 提示回复不存在 |
| 40301 | 非作者无权编辑 | 提示无权限 |
| 40011 | 回复已删除 | 提示无法编辑 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 删除回复

**接口信息**

- 路径：`DELETE /api/user/forum/replies/{replyId}`
- 鉴权：是
- 说明：删除自己的回复，仅作者可操作

**请求示例**

```javascript
axios.delete('/api/user/forum/replies/1002', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T13:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 回复不存在 | 提示回复不存在 |
| 40301 | 非作者无权删除 | 提示无权限 |
| 40011 | 回复已删除 | 提示已删除 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 互动功能

> 以下接口均要求登录

---

### 点赞帖子

**接口信息**

- 路径：`POST /api/user/forum/posts/{postId}/likes`
- 鉴权：是
- 说明：点赞指定帖子，幂等操作

**请求示例**

```javascript
axios.post('/api/user/forum/posts/10/likes', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T13:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40011 | 帖子已删除/隐藏 | 提示无法点赞 |
| 40011 | 用户被禁言 | 提示禁言状态 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 取消点赞

**接口信息**

- 路径：`DELETE /api/user/forum/posts/{postId}/likes`
- 鉴权：是
- 说明：取消对帖子的点赞，幂等操作

**请求示例**

```javascript
axios.delete('/api/user/forum/posts/10/likes', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T14:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 收藏帖子

**接口信息**

- 路径：`POST /api/user/forum/posts/{postId}/collections`
- 鉴权：是
- 说明：收藏指定帖子到收藏夹，不指定folderId时使用默认论坛收藏夹

**请求示例**

```javascript
// 收藏到默认收藏夹
axios.post('/api/user/forum/posts/10/collections', {}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 收藏到指定收藏夹
axios.post('/api/user/forum/posts/10/collections', {
  folderId: 10,
  remark: "稍后阅读"
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `folderId` | Long | 否 | 收藏夹ID，不传则使用默认论坛收藏夹 |
| `remark` | String | 否 | 收藏备注 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T14:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 收藏夹不存在 | 提示收藏夹不存在 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40011 | 帖子已删除/隐藏 | 提示无法收藏 |
| 40011 | 用户被禁言 | 提示禁言状态 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 取消收藏

**接口信息**

- 路径：`DELETE /api/user/forum/posts/{postId}/collections`
- 鉴权：是
- 说明：取消对帖子的收藏，幂等操作

**请求示例**

```javascript
axios.delete('/api/user/forum/posts/10/collections', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T15:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 频道分享

### 分享帖子到频道

**接口信息**

- 路径：`POST /api/user/forum/posts/{postId}/channel-share`
- 鉴权：是
- 说明：将帖子分享到指定频道，每个帖子同一时间只能挂接一个频道

**请求示例**

```javascript
axios.post('/api/user/forum/posts/10/channel-share', {
  conversationId: 99
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `conversationId` | Long | 是 | 目标频道会话ID |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T15:30:00",
  "data": {
    "id": 5,
    "forumPostId": 10,
    "conversationId": 99,
    "channelName": "技术交流频道",
    "linkType": "forum_share",
    "linkedBy": 100,
    "linkedAt": "2025-01-15T15:30:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40011 | 帖子未发布 | 提示只能分享已发布帖子 |
| 40011 | 用户被禁言 | 提示禁言状态 |
| 40011 | 用户不是频道成员 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 我的帖子

> 以下接口均要求登录

---

### 我的帖子列表

**接口信息**

- 路径：`GET /api/user/forum/posts`
- 鉴权：是
- 说明：分页查询当前用户的帖子列表，包含草稿和已发布

**请求示例**

```javascript
// 查询全部
axios.get('/api/user/forum/posts', {
  headers: { Authorization: 'Bearer xxx' }
})

// 按状态筛选
axios.get('/api/user/forum/posts?status=1', {
  headers: { Authorization: 'Bearer xxx' }
})

// 搜索我的帖子
axios.get('/api/user/forum/posts?keyword=Java&sectionId=1&current=1&size=20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量 | 10 |
| `keyword` | String | 否 | 标题关键字搜索 | - |
| `sectionId` | Long | 否 | 版块ID | - |
| `status` | Integer | 否 | 帖子状态 `0`草稿`1`已发布 | - |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 15,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 20,
        "sectionId": 1,
        "sectionName": "技术交流",
        "authorId": 100,
        "authorName": "张三",
        "title": "我的新帖子",
        "status": 1,
        "visibilityScope": 0,
        "isTop": 0,
        "isEssence": 0,
        "viewCount": 100,
        "likeCount": 10,
        "replyCount": 5,
        "collectCount": 2,
        "shareCount": 1,
        "publishedAt": "2025-01-15T10:30:00",
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-01-15T10:30:00"
      },
      {
        "id": 19,
        "sectionId": 1,
        "sectionName": "技术交流",
        "authorId": 100,
        "authorName": "张三",
        "title": "草稿帖子",
        "status": 0,
        "visibilityScope": 0,
        "isTop": 0,
        "isEssence": 0,
        "viewCount": 0,
        "likeCount": 0,
        "replyCount": 0,
        "collectCount": 0,
        "shareCount": 0,
        "publishedAt": null,
        "createdAt": "2025-01-14T09:00:00",
        "updatedAt": "2025-01-14T09:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records` | Array | 帖子列表，字段同公开帖子列表 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 我的帖子详情

**接口信息**

- 路径：`GET /api/user/forum/posts/{id}`
- 鉴权：是
- 说明：获取当前用户帖子的完整详情，包含内容字段

**请求示例**

```javascript
axios.get('/api/user/forum/posts/20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 20,
    "sectionId": 1,
    "sectionName": "技术交流",
    "authorId": 100,
    "authorName": "张三",
    "title": "我的新帖子",
    "content": "这是帖子内容...",
    "status": 1,
    "visibilityScope": 0,
    "isTop": 0,
    "isEssence": 0,
    "viewCount": 100,
    "likeCount": 10,
    "replyCount": 5,
    "collectCount": 2,
    "shareCount": 1,
    "publishedAt": "2025-01-15T10:30:00",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00",
    "liked": false,
    "collected": false,
    "canReply": true,
    "linkedChannel": null
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 非本人帖子 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 后台管理 - 版块管理

> 以下接口均要求后台登录账号，并校验 `content:forum:*` 权限

---

### 分页查询版块

**接口信息**

- 路径：`GET /api/sys/forum/sections`
- 鉴权：`content:forum:query`
- 说明：分页查询论坛版块列表，支持关键字筛选

**请求示例**

```javascript
// 查询全部
axios.get('/api/sys/forum/sections', {
  headers: { Authorization: 'Bearer xxx' }
})

// 关键字搜索
axios.get('/api/sys/forum/sections?keyword=技术&status=1&visibilityScope=0', {
  headers: { Authorization: 'Bearer xxx' }
})

// 分页
axios.get('/api/sys/forum/sections?current=1&size=20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量，最大100 | 10 |
| `keyword` | String | 否 | 版块名称/简介关键字 | - |
| `status` | Integer | 否 | `0`禁用`1`启用 | - |
| `visibilityScope` | Integer | 否 | `0`公开`1`登录可见 | - |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 5,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "name": "技术交流",
        "description": "技术问题与经验分享",
        "sortOrder": 10,
        "visibilityScope": 0,
        "postLevelLimit": 1,
        "status": 1,
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-10T12:00:00"
      },
      {
        "id": 2,
        "name": "内部交流",
        "description": "仅登录用户可见",
        "sortOrder": 20,
        "visibilityScope": 1,
        "postLevelLimit": 5,
        "status": 1,
        "createdAt": "2025-01-02T00:00:00",
        "updatedAt": "2025-01-11T12:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records[].id` | Long | 版块ID |
| `records[].name` | String | 版块名称 |
| `records[].description` | String | 版块简介 |
| `records[].sortOrder` | Integer | 排序值 |
| `records[].visibilityScope` | Integer | `0`公开`1`登录可见 |
| `records[].postLevelLimit` | Integer | 发帖最低等级 |
| `records[].status` | Integer | `0`禁用`1`启用 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40301 | 无查询权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 查询版块详情

**接口信息**

- 路径：`GET /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:query`
- 说明：获取单个版块的完整信息

**请求示例**

```javascript
axios.get('/api/sys/forum/sections/1', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 1,
    "name": "技术交流",
    "description": "技术问题与经验分享",
    "sortOrder": 10,
    "visibilityScope": 0,
    "postLevelLimit": 1,
    "status": 1,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-10T12:00:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 版块不存在 | 提示版块不存在 |
| 40301 | 无查询权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 新增版块

**接口信息**

- 路径：`POST /api/sys/forum/sections`
- 鉴权：`content:forum:create`
- 说明：创建新的论坛版块

**请求示例**

```javascript
axios.post('/api/sys/forum/sections', {
  name: "技术交流",
  description: "技术问题与经验分享",
  sortOrder: 10,
  visibilityScope: 0,
  postLevelLimit: 1,
  status: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `name` | String | 是 | 版块名称，最大64字符，唯一 |
| `description` | String | 否 | 版块简介，最大512字符 |
| `sortOrder` | Integer | 否 | 排序值，越小越靠前，默认0 |
| `visibilityScope` | Integer | 否 | `0`公开`1`登录可见，默认0 |
| `postLevelLimit` | Integer | 否 | 发帖最低等级，最小1，默认1 |
| `status` | Integer | 否 | `0`禁用`1`启用，默认1 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 3,
    "name": "技术交流",
    "description": "技术问题与经验分享",
    "sortOrder": 10,
    "visibilityScope": 0,
    "postLevelLimit": 1,
    "status": 1,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40011 | 版块名称已存在 | 提示名称重复 |
| 40301 | 无创建权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 修改版块

**接口信息**

- 路径：`PUT /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:update`
- 说明：修改指定版块的信息

**请求示例**

```javascript
axios.put('/api/sys/forum/sections/3', {
  name: "技术交流区",
  description: "技术问题与经验分享平台",
  sortOrder: 5,
  visibilityScope: 0,
  postLevelLimit: 2,
  status: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `name` | String | 是 | 版块名称，最大64字符，唯一 |
| `description` | String | 否 | 版块简介，最大512字符 |
| `sortOrder` | Integer | 否 | 排序值，越小越靠前 |
| `visibilityScope` | Integer | 否 | `0`公开`1`登录可见 |
| `postLevelLimit` | Integer | 否 | 发帖最低等级，最小1 |
| `status` | Integer | 否 | `0`禁用`1`启用 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:00:00",
  "data": {
    "id": 3,
    "name": "技术交流区",
    "description": "技术问题与经验分享平台",
    "sortOrder": 5,
    "visibilityScope": 0,
    "postLevelLimit": 2,
    "status": 1,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T11:00:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40011 | 版块名称已存在 | 提示名称重复 |
| 40401 | 版块不存在 | 提示版块不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 修改版块状态

**接口信息**

- 路径：`PUT /api/sys/forum/sections/{id}/status`
- 鉴权：`content:forum:update`
- 说明：启用或禁用指定版块

**请求示例**

```javascript
// 禁用版块
axios.put('/api/sys/forum/sections/3/status', {
  status: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 启用版块
axios.put('/api/sys/forum/sections/3/status', {
  status: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `status` | Integer | 是 | `0`禁用`1`启用 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 参数校验失败 | 提示具体校验错误 |
| 40401 | 版块不存在 | 提示版块不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 删除版块

**接口信息**

- 路径：`DELETE /api/sys/forum/sections/{id}`
- 鉴权：`content:forum:delete`
- 说明：删除指定版块，仅允许删除无帖子的版块

**请求示例**

```javascript
axios.delete('/api/sys/forum/sections/3', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 版块已有帖子无法删除 | 提示当前版块已存在帖子 |
| 40401 | 版块不存在 | 提示版块不存在 |
| 40301 | 无删除权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 后台管理 - 帖子管理

> 以下接口均要求后台登录账号，并校验 `content:forum:*` 权限

---

### 分页查询帖子

**接口信息**

- 路径：`GET /api/sys/forum/posts`
- 鉴权：`content:forum:query`
- 说明：分页查询所有帖子，支持多条件筛选

**请求示例**

```javascript
// 查询全部
axios.get('/api/sys/forum/posts', {
  headers: { Authorization: 'Bearer xxx' }
})

// 筛选条件
axios.get('/api/sys/forum/posts?sectionId=1&status=1&isTop=1&isEssence=0', {
  headers: { Authorization: 'Bearer xxx' }
})

// 关键字搜索
axios.get('/api/sys/forum/posts?keyword=Spring&current=1&size=20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量 | 10 |
| `keyword` | String | 否 | 标题/内容关键字 | - |
| `sectionId` | Long | 否 | 版块ID | - |
| `authorId` | Long | 否 | 作者ID | - |
| `status` | Integer | 否 | 帖子状态 | - |
| `isTop` | Integer | 否 | `0`否`1`是 | - |
| `isEssence` | Integer | 否 | `0`否`1`是 | - |
| `createdAtStart` | DateTime | 否 | 创建时间开始 | - |
| `createdAtEnd` | DateTime | 否 | 创建时间结束 | - |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 50,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 10,
        "sectionId": 1,
        "sectionName": "技术交流",
        "authorId": 100,
        "authorName": "张三",
        "title": "Spring Boot 最佳实践",
        "status": 1,
        "statusName": "已发布",
        "visibilityScope": 0,
        "isTop": 1,
        "isEssence": 1,
        "viewCount": 1523,
        "likeCount": 88,
        "replyCount": 35,
        "collectCount": 12,
        "shareCount": 5,
        "publishedAt": "2025-01-14T15:30:00",
        "createdAt": "2025-01-14T15:30:00",
        "updatedAt": "2025-01-14T18:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records[].id` | Long | 帖子ID |
| `records[].sectionId` | Long | 版块ID |
| `records[].sectionName` | String | 版块名称 |
| `records[].authorId` | Long | 作者ID |
| `records[].authorName` | String | 作者昵称 |
| `records[].title` | String | 帖子标题 |
| `records[].status` | Integer | 状态值 |
| `records[].statusName` | String | 状态描述 |
| `records[].visibilityScope` | Integer | `0`公开`1`登录可见 |
| `records[].isTop` | Integer | `0`否`1`是 |
| `records[].isEssence` | Integer | `0`否`1`是 |
| `records[].viewCount` | Integer | 浏览数 |
| `records[].likeCount` | Integer | 点赞数 |
| `records[].replyCount` | Integer | 回复数 |
| `records[].collectCount` | Integer | 收藏数 |
| `records[].shareCount` | Integer | 分享数 |
| `records[].publishedAt` | DateTime | 发布时间 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40301 | 无查询权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 查询帖子详情

**接口信息**

- 路径：`GET /api/sys/forum/posts/{id}`
- 鉴权：`content:forum:query`
- 说明：获取帖子完整信息，包含正文内容

**请求示例**

```javascript
axios.get('/api/sys/forum/posts/10', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 10,
    "sectionId": 1,
    "sectionName": "技术交流",
    "authorId": 100,
    "authorName": "张三",
    "title": "Spring Boot 最佳实践",
    "content": "本文总结了 Spring Boot 开发的最佳实践...",
    "status": 1,
    "statusName": "已发布",
    "visibilityScope": 0,
    "isTop": 1,
    "isEssence": 1,
    "viewCount": 1523,
    "likeCount": 88,
    "replyCount": 35,
    "collectCount": 12,
    "shareCount": 5,
    "publishedAt": "2025-01-14T15:30:00",
    "createdAt": "2025-01-14T15:30:00",
    "updatedAt": "2025-01-14T18:00:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无查询权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 隐藏帖子

**接口信息**

- 路径：`PUT /api/sys/forum/posts/{id}/hide`
- 鉴权：`content:forum:update`
- 说明：将帖子设为隐藏状态，仅已发布帖子可隐藏

**请求示例**

```javascript
axios.put('/api/sys/forum/posts/10/hide', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 帖子已删除无法隐藏 | 提示帖子已删除 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 恢复帖子

**接口信息**

- 路径：`PUT /api/sys/forum/posts/{id}/restore`
- 鉴权：`content:forum:update`
- 说明：恢复已隐藏的帖子，恢复后状态为已发布

**请求示例**

```javascript
axios.put('/api/sys/forum/posts/10/restore', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 帖子状态不支持恢复 | 提示只有隐藏帖子可恢复 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 删除帖子

**接口信息**

- 路径：`DELETE /api/sys/forum/posts/{id}`
- 鉴权：`content:forum:delete`
- 说明：软删除帖子，已删除帖子不能重复删除

**请求示例**

```javascript
axios.delete('/api/sys/forum/posts/10', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 帖子已删除 | 提示帖子已删除 |
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无删除权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 切换置顶

**接口信息**

- 路径：`PUT /api/sys/forum/posts/{id}/top?enabled=true|false`
- 鉴权：`content:forum:update`
- 说明：设置或取消帖子置顶状态

**请求示例**

```javascript
// 设置置顶
axios.put('/api/sys/forum/posts/10/top?enabled=true', {}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 取消置顶
axios.put('/api/sys/forum/posts/10/top?enabled=false', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `enabled` | Boolean | 是 | `true` 置顶，`false` 取消置顶 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 切换精华

**接口信息**

- 路径：`PUT /api/sys/forum/posts/{id}/essence?enabled=true|false`
- 鉴权：`content:forum:update`
- 说明：设置或取消帖子精华状态，设为精华时会通知帖子作者

**请求示例**

```javascript
// 设置精华
axios.put('/api/sys/forum/posts/10/essence?enabled=true', {}, {
  headers: { Authorization: 'Bearer xxx' }
})

// 取消精华
axios.put('/api/sys/forum/posts/10/essence?enabled=false', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| `enabled` | Boolean | 是 | `true` 精华，`false` 取消精华 |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T13:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40401 | 帖子不存在 | 提示帖子不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 后台管理 - 回复管理

> 以下接口均要求后台登录账号，并校验 `content:forum:*` 权限

---

### 分页查询回复

**接口信息**

- 路径：`GET /api/sys/forum/replies`
- 鉴权：`content:forum:query`
- 说明：分页查询所有回复，支持多条件筛选

**请求示例**

```javascript
// 查询全部
axios.get('/api/sys/forum/replies', {
  headers: { Authorization: 'Bearer xxx' }
})

// 筛选条件
axios.get('/api/sys/forum/replies?postId=10&status=1', {
  headers: { Authorization: 'Bearer xxx' }
})

// 关键字搜索
axios.get('/api/sys/forum/replies?keyword=感谢&current=1&size=20', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 | 默认值 |
|-----|------|-----|------|-------|
| `current` | Long | 否 | 页码 | 1 |
| `size` | Long | 否 | 每页数量 | 10 |
| `keyword` | String | 否 | 回复内容关键字 | - |
| `postId` | Long | 否 | 帖子ID | - |
| `userId` | Long | 否 | 回复用户ID | - |
| `status` | Integer | 否 | 回复状态 | - |

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1001,
        "postId": 10,
        "postTitle": "Spring Boot 最佳实践",
        "parentId": 0,
        "rootId": 0,
        "userId": 101,
        "userName": "李四",
        "content": "写得很好，收藏了！",
        "status": 1,
        "statusName": "正常",
        "floorNo": 1,
        "likeCount": 5,
        "replyCount": 2,
        "createdAt": "2025-01-14T16:00:00",
        "updatedAt": "2025-01-14T16:00:00"
      },
      {
        "id": 1002,
        "postId": 10,
        "postTitle": "Spring Boot 最佳实践",
        "parentId": 1001,
        "rootId": 1001,
        "userId": 100,
        "userName": "张三",
        "content": "感谢支持！",
        "status": 1,
        "statusName": "正常",
        "floorNo": 2,
        "likeCount": 1,
        "replyCount": 0,
        "createdAt": "2025-01-14T16:30:00",
        "updatedAt": "2025-01-14T16:30:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页数量 |
| `records[].id` | Long | 回复ID |
| `records[].postId` | Long | 帖子ID |
| `records[].postTitle` | String | 帖子标题 |
| `records[].parentId` | Long | 父回复ID，顶级回复为0 |
| `records[].rootId` | Long | 根回复ID，顶级回复为0 |
| `records[].userId` | Long | 回复用户ID |
| `records[].userName` | String | 回复用户昵称 |
| `records[].content` | String | 回复内容 |
| `records[].status` | Integer | 回复状态值 |
| `records[].statusName` | String | 回复状态描述 |
| `records[].floorNo` | Integer | 楼层号 |
| `records[].likeCount` | Integer | 点赞数 |
| `records[].replyCount` | Integer | 该回复下子回复数 |
| `records[].createdAt` | DateTime | 创建时间 |
| `records[].updatedAt` | DateTime | 更新时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40301 | 无查询权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 隐藏回复

**接口信息**

- 路径：`PUT /api/sys/forum/replies/{id}/hide`
- 鉴权：`content:forum:update`
- 说明：将回复设为隐藏状态，仅正常状态回复可隐藏

**请求示例**

```javascript
axios.put('/api/sys/forum/replies/1001/hide', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 回复已删除无法隐藏 | 提示回复已删除 |
| 40401 | 回复不存在 | 提示回复不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 恢复回复

**接口信息**

- 路径：`PUT /api/sys/forum/replies/{id}/restore`
- 鉴权：`content:forum:update`
- 说明：恢复已隐藏的回复，恢复后状态为正常

**请求示例**

```javascript
axios.put('/api/sys/forum/replies/1001/restore', {}, {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T11:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 回复状态不支持恢复 | 提示只有隐藏回复可恢复 |
| 40401 | 回复不存在 | 提示回复不存在 |
| 40301 | 无修改权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

### 删除回复

**接口信息**

- 路径：`DELETE /api/sys/forum/replies/{id}`
- 鉴权：`content:forum:delete`
- 说明：软删除回复，同步递减帖子回复数和父回复回复数

**请求示例**

```javascript
axios.delete('/api/sys/forum/replies/1001', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T12:00:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40011 | 回复已删除 | 提示回复已删除 |
| 40401 | 回复不存在 | 提示回复不存在 |
| 40301 | 无删除权限 | 提示无权限 |
| 40101 | 未登录 | 跳转登录页 |
| 50001 | 系统异常 | 提示稍后重试 |

---

## 附录

### 状态值对照表

| 枚举 | 值 | 说明 |
|-----|---|------|
| 帖子状态 | `0` | 草稿 |
| 帖子状态 | `1` | 已发布 |
| 帖子状态 | `5` | 隐藏 |
| 回复状态 | `1` | 正常 |
| 回复状态 | `2` | 隐藏 |
| 回复状态 | `3` | 删除 |
| 可见范围 | `0` | 公开 |
| 可见范围 | `1` | 登录可见 |
| 版块状态 | `0` | 禁用 |
| 版块状态 | `1` | 启用 |

### 后台权限说明

| 权限 | 说明 |
|-----|------|
| `content:forum:query` | 查询后台论坛版块/帖子/回复 |
| `content:forum:create` | 新增论坛版块 |
| `content:forum:update` | 修改论坛版块或状态，隐藏/恢复/置顶/精华帖子/回复 |
| `content:forum:delete` | 删除论坛版块/帖子/回复 |
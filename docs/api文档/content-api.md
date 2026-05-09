# 内容域 API 前端参考手册

本文档面向前端开发人员，按页面和功能模块组织接口说明。每个接口包含完整的请求示例、响应示例、字段说明和错误处理指南。

---

## 通用规范

### 统一响应结构

所有接口均返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| code | Integer | 业务状态码，200 表示成功，非 200 为异常 |
| message | String | 描述信息 |
| timestamp | String | 响应时间（ISO 8601 格式） |
| data | Object | 响应数据，分页接口为 PageResult 结构 |

**分页响应结构 PageResult：**

```json
{
  "total": 100,
  "current": 1,
  "size": 10,
  "records": []
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| total | Long | 总记录数 |
| current | Long | 当前页码 |
| size | Long | 每页条数 |
| records | Array | 数据列表 |

### 错误响应示例

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

### 常用错误码

| code | 说明 | 前端处理建议 |
|-----|------|-------------|
| 200 | 成功 | - |
| 40001 | 参数校验失败 | 检查请求参数是否合法 |
| 40102 | 未登录或登录已过期 | 跳转登录页 |
| 40300 | 没有访问权限 | 提示用户无权限 |
| 40400 | 请求的接口不存在 | 检查接口地址 |
| 42900 | 请求过于频繁 | 显示提示，稍后重试 |
| 50000 | 系统异常 | 提示用户联系管理员 |

---

## 一、首页文章列表

### 1.1 获取文章分页列表

**接口信息**
- 路径: `GET /api/articles`
- 鉴权: 否
- 说明: 返回公开已发布的文章列表，支持分页、关键词搜索、分类筛选、标签筛选和排序

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|-----|------|
| current | Long | 否 | 页码，默认 1 | 1 |
| size | Long | 否 | 每页条数，默认 10 | 10 |
| keyword | String | 否 | 搜索关键词（匹配标题和摘要） | Java |
| categoryId | Long | 否 | 分类 ID | 1 |
| tagId | Long | 否 | 标签 ID | 5 |
| sort | String | 否 | 排序方式，默认 latest | latest |

**sort 取值说明：**
- `latest` - 按发布时间倒序（默认）
- `popular` - 按浏览数倒序
- `hot` - 按点赞数倒序

**请求示例**

```javascript
// axios
axios.get('/api/articles', {
  params: {
    current: 1,
    size: 10,
    keyword: 'Java',
    categoryId: 1,
    sort: 'latest'
  }
})

// fetch
fetch('/api/articles?current=1&size=10&keyword=Java&categoryId=1&sort=latest')
  .then(res => res.json())
  .then(data => console.log(data))
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 42,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 100,
        "title": "Spring Boot 权威指南",
        "summary": "本文详细介绍 Spring Boot 的核心特性...",
        "coverImage": "https://example.com/cover.jpg",
        "authorId": 8,
        "authorName": "张三",
        "isTop": 1,
        "isRecommend": 1,
        "accessLevel": 0,
        "viewCount": 1520,
        "likeCount": 128,
        "commentCount": 35,
        "collectCount": 67,
        "publishTime": "2025-01-10T08:00:00"
      }
    ]
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 文章 ID |
| title | String | 文章标题 |
| summary | String | 文章摘要 |
| coverImage | String | 封面图地址 |
| authorId | Long | 作者 ID |
| authorName | String | 作者昵称 |
| isTop | Integer | 是否置顶，1-是，0-否 |
| isRecommend | Integer | 是否推荐，1-是，0-否 |
| accessLevel | Integer | 访问级别，0-免费，1-付费等 |
| viewCount | Integer | 浏览数 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| collectCount | Integer | 收藏数 |
| publishTime | String | 发布时间 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | - |
| 40001 | 参数校验失败 | 检查参数格式 |

---

## 二、文章详情页

### 2.1 获取文章详情

**接口信息**
- 路径: `GET /api/articles/{id}`
- 鉴权: 否（部分字段如 liked、collected 需要登录）
- 说明: 返回文章完整详情，包含分类、标签、用户互动状态等信息

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求示例**

```javascript
// axios
axios.get('/api/articles/100')
  .then(res => {
    if (res.data.code === 200) {
      console.log(res.data.data)
    }
  })

// fetch
fetch('/api/articles/100')
  .then(res => res.json())
  .then(data => console.log(data))
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 100,
    "title": "Spring Boot 权威指南",
    "summary": "本文详细介绍 Spring Boot 的核心特性...",
    "content": "<p>文章正文 HTML 内容...</p>",
    "coverImage": "https://example.com/cover.jpg",
    "authorId": 8,
    "authorName": "张三",
    "isTop": 1,
    "isRecommend": 1,
    "isOriginal": 1,
    "sourceUrl": null,
    "accessLevel": 0,
    "visibilityScope": 0,
    "viewCount": 1520,
    "likeCount": 128,
    "commentCount": 35,
    "collectCount": 67,
    "shareCount": 12,
    "publishTime": "2025-01-10T08:00:00",
    "categories": [
      {
        "id": 1,
        "parentId": 0,
        "name": "后端",
        "code": "backend",
        "type": "article",
        "level": 1,
        "sortOrder": 1,
        "icon": "code",
        "description": "后端技术文章",
        "children": [
          {
            "id": 5,
            "parentId": 1,
            "name": "Java",
            "code": "java",
            "type": "article",
            "level": 2,
            "sortOrder": 1,
            "icon": "coffee",
            "description": null,
            "children": []
          }
        ]
      }
    ],
    "tags": [
      { "id": 10, "name": "Spring Boot", "color": "#6db33f" },
      { "id": 11, "name": "Java", "color": "#007396" }
    ],
    "seriesList": [
      {
        "id": 3,
        "title": "Spring 系列教程",
        "coverImage": "https://example.com/series.jpg",
        "articleCount": 12,
        "sortOrder": 1,
        "visibilityScope": 0
      }
    ],
    "liked": true,
    "collected": false,
    "canComment": true
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 文章 ID |
| title | String | 文章标题 |
| summary | String | 文章摘要 |
| content | String | 文章正文（HTML 格式） |
| coverImage | String | 封面图地址 |
| authorId | Long | 作者 ID |
| authorName | String | 作者昵称 |
| isTop | Integer | 是否置顶 |
| isRecommend | Integer | 是否推荐 |
| isOriginal | Integer | 是否原创，1-原创，0-转载 |
| sourceUrl | String | 来源地址，转载时有效 |
| accessLevel | Integer | 访问级别 |
| visibilityScope | Integer | 可见范围：0-公开，1-仅自己，2-白名单，3-登录可见 |
| viewCount | Integer | 浏览数 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| collectCount | Integer | 收藏数 |
| shareCount | Integer | 分享数 |
| publishTime | String | 发布时间 |
| categories | Array | 分类列表（树形结构） |
| tags | Array | 标签列表 |
| seriesList | Array | 所属系列列表 |
| liked | Boolean | 当前用户是否已点赞（需登录，未登录为 null） |
| collected | Boolean | 当前用户是否已收藏（需登录，未登录为 null） |
| canComment | Boolean | 当前用户是否允许评论 |

**categories 子字段**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 分类 ID |
| parentId | Long | 父分类 ID，0 表示顶级 |
| name | String | 分类名称 |
| code | String | 分类编码 |
| type | String | 分类类型，article 表示文章分类 |
| level | Integer | 层级，从 1 开始 |
| sortOrder | Integer | 排序序号 |
| icon | String | 图标名称 |
| description | String | 分类描述 |
| children | Array | 子分类列表 |

**tags 子字段**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 标签 ID |
| name | String | 标签名称 |
| color | String | 标签颜色（十六进制） |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | - |
| 40400 | 文章不存在 | 显示文章不存在提示 |
| 40300 | 无访问权限 | 显示无权访问提示 |

---

## 三、分类与标签

### 3.1 获取分类树

**接口信息**
- 路径: `GET /api/categories/tree`
- 鉴权: 否
- 说明: 返回文章分类的树形结构

**请求示例**

```javascript
// axios
axios.get('/api/categories/tree')

// fetch
fetch('/api/categories/tree')
  .then(res => res.json())
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
      "parentId": 0,
      "name": "后端",
      "code": "backend",
      "type": "article",
      "level": 1,
      "sortOrder": 1,
      "icon": "code",
      "description": "后端技术文章",
      "children": [
        {
          "id": 5,
          "parentId": 1,
          "name": "Java",
          "code": "java",
          "type": "article",
          "level": 2,
          "sortOrder": 1,
          "icon": "coffee",
          "description": null,
          "children": []
        }
      ]
    },
    {
      "id": 2,
      "parentId": 0,
      "name": "前端",
      "code": "frontend",
      "type": "article",
      "level": 1,
      "sortOrder": 2,
      "icon": "html",
      "description": "前端技术文章",
      "children": []
    }
  ]
}
```

**字段说明**

同 2.1 中 categories 字段说明。

---

### 3.2 获取标签列表

**接口信息**
- 路径: `GET /api/tags`
- 鉴权: 否
- 说明: 返回已启用的文章标签列表

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|-----|------|
| targetType | String | 否 | 目标类型，默认 article | article |

**请求示例**

```javascript
// axios
axios.get('/api/tags', { params: { targetType: 'article' } })

// fetch
fetch('/api/tags?targetType=article')
  .then(res => res.json())
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": [
    { "id": 10, "name": "Spring Boot", "color": "#6db33f" },
    { "id": 11, "name": "Java", "color": "#007396" },
    { "id": 12, "name": "Vue", "color": "#4fc08d" }
  ]
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 标签 ID |
| name | String | 标签名称 |
| color | String | 标签颜色（十六进制） |

---

## 四、文章评论

### 4.1 获取评论树

**接口信息**
- 路径: `GET /api/comments`
- 鉴权: 否（liked 字段需要登录）
- 说明: 返回指定文章的评论树形结构，包含根评论及其子评论

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|-----|------|
| targetType | String | 是 | 目标类型，固定为 article | article |
| targetId | Long | 是 | 目标 ID（文章 ID） | 100 |
| current | Long | 否 | 页码，默认 1 | 1 |
| size | Long | 否 | 每页条数，默认 10 | 10 |

**请求示例**

```javascript
// axios
axios.get('/api/comments', {
  params: {
    targetType: 'article',
    targetId: 100,
    current: 1,
    size: 10
  }
})

// fetch
fetch('/api/comments?targetType=article&targetId=100&current=1&size=10')
  .then(res => res.json())
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 25,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 500,
        "targetId": 100,
        "targetType": "article",
        "content": "写得很好，收藏了！",
        "images": ["https://example.com/img1.jpg"],
        "userId": 8,
        "userNickname": "Tom",
        "userAvatar": "https://example.com/avatar/8.png",
        "rootId": 0,
        "parentId": 0,
        "likeCount": 12,
        "replyCount": 3,
        "status": 1,
        "createdAt": "2025-01-12T14:30:00",
        "liked": false,
        "children": [
          {
            "id": 501,
            "targetId": 100,
            "targetType": "article",
            "content": "同感！",
            "images": [],
            "userId": 9,
            "userNickname": "Jerry",
            "userAvatar": "https://example.com/avatar/9.png",
            "rootId": 500,
            "parentId": 500,
            "likeCount": 2,
            "replyCount": 0,
            "status": 1,
            "createdAt": "2025-01-12T15:00:00",
            "liked": true,
            "children": []
          }
        ]
      }
    ]
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 评论 ID |
| targetId | Long | 目标 ID（文章 ID） |
| targetType | String | 目标类型 |
| content | String | 评论内容 |
| images | Array | 评论图片列表 |
| userId | Long | 评论用户 ID |
| userNickname | String | 评论用户昵称 |
| userAvatar | String | 评论用户头像 |
| rootId | Long | 根评论 ID，0 表示根评论 |
| parentId | Long | 父评论 ID，0 表示根评论 |
| likeCount | Integer | 点赞数 |
| replyCount | Integer | 回复数 |
| status | Integer | 评论状态，1-正常，0-隐藏 |
| createdAt | String | 创建时间 |
| liked | Boolean | 当前用户是否已点赞（需登录） |
| children | Array | 子评论列表 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | - |
| 40001 | 参数校验失败 | targetType 或 targetId 为空 |
| 40011 | 非法参数 | targetType 不支持 |

---

## 五、用户文章行为（需登录）

### 5.1 点赞文章

**接口信息**
- 路径: `POST /api/user/articles/{id}/likes`
- 鉴权: 必须登录
- 说明: 为指定文章点赞

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求示例**

```javascript
// axios
axios.post('/api/user/articles/100/likes')

// fetch
fetch('/api/user/articles/100/likes', { method: 'POST' })
  .then(res => res.json())
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 更新 UI 点赞状态 |
| 40102 | 未登录 | 跳转登录页 |
| 40011 | 非法参数 | 文章 ID 无效 |

---

### 5.2 取消点赞文章

**接口信息**
- 路径: `DELETE /api/user/articles/{id}/likes`
- 鉴权: 必须登录
- 说明: 取消对指定文章的点赞

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求示例**

```javascript
// axios
axios.delete('/api/user/articles/100/likes')

// fetch
fetch('/api/user/articles/100/likes', { method: 'DELETE' })
  .then(res => res.json())
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 更新 UI 点赞状态 |
| 40102 | 未登录 | 跳转登录页 |

---

## 六、用户收藏（需登录）

### 6.1 查询我的收藏夹

**接口信息**
- 路径: `GET /api/user/collection-folders`
- 鉴权: 必须登录
- 说明: 返回当前用户的收藏夹分页列表

**请求示例**

```javascript
// axios
axios.get('/api/user/collection-folders')

// fetch
fetch('/api/user/collection-folders')
  .then(res => res.json())
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 3,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "userId": 8,
        "folderName": "技术收藏",
        "folderType": "article",
        "description": "技术相关文章收藏",
        "isPublic": 0,
        "isDefault": 1,
        "sortOrder": 1,
        "collectionCount": 25,
        "createdAt": "2024-12-01T10:00:00",
        "updatedAt": "2025-01-15T09:00:00"
      }
    ]
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 收藏夹 ID |
| userId | Long | 用户 ID |
| folderName | String | 收藏夹名称 |
| folderType | String | 收藏夹类型，article 表示文章收藏 |
| description | String | 收藏夹描述 |
| isPublic | Integer | 是否公开，1-公开，0-私有 |
| isDefault | Integer | 是否默认收藏夹，1-是，0-否 |
| sortOrder | Integer | 排序序号 |
| collectionCount | Integer | 收藏数量 |
| createdAt | String | 创建时间 |
| updatedAt | String | 更新时间 |

---

### 6.2 新增收藏夹

**接口信息**
- 路径: `POST /api/user/collection-folders`
- 鉴权: 必须登录
- 说明: 创建一个新的收藏夹

**请求体**

```json
{
  "folderName": "我的收藏",
  "folderType": "article",
  "description": "收藏夹描述",
  "isPublic": 0,
  "isDefault": 0,
  "sortOrder": 0
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| folderName | String | 是 | 收藏夹名称 |
| folderType | String | 否 | 收藏夹类型，默认 article |
| description | String | 否 | 收藏夹描述 |
| isPublic | Integer | 否 | 是否公开，0-私有，1-公开 |
| isDefault | Integer | 否 | 是否默认，0-否，1-是 |
| sortOrder | Integer | 否 | 排序序号 |

**请求示例**

```javascript
// axios
axios.post('/api/user/collection-folders', {
  folderName: '我的收藏',
  folderType: 'article',
  description: '收藏夹描述',
  isPublic: 0
})

// fetch
fetch('/api/user/collection-folders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    folderName: '我的收藏',
    folderType: 'article',
    isPublic: 0
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 5,
    "userId": 8,
    "folderName": "我的收藏",
    "folderType": "article",
    "description": "收藏夹描述",
    "isPublic": 0,
    "isDefault": 0,
    "sortOrder": 0,
    "collectionCount": 0,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 关闭创建弹窗，刷新列表 |
| 40001 | 参数校验失败 | folderName 为空 |
| 40102 | 未登录 | 跳转登录页 |

---

### 6.3 修改收藏夹

**接口信息**
- 路径: `PUT /api/user/collection-folders/{id}`
- 鉴权: 必须登录
- 说明: 修改指定收藏夹的信息

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 收藏夹 ID |

**请求体**

同 6.2 新增收藏夹

**请求示例**

```javascript
// axios
axios.put('/api/user/collection-folders/5', {
  folderName: '修改后的名称',
  description: '修改后的描述'
})

// fetch
fetch('/api/user/collection-folders/5', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    folderName: '修改后的名称'
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 5,
    "userId": 8,
    "folderName": "修改后的名称",
    "folderType": "article",
    "description": "修改后的描述",
    "isPublic": 0,
    "isDefault": 0,
    "sortOrder": 0,
    "collectionCount": 0,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:35:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 关闭编辑弹窗，刷新列表 |
| 40001 | 参数校验失败 | folderName 为空 |
| 40102 | 未登录 | 跳转登录页 |
| 40400 | 收藏夹不存在 | 提示收藏夹不存在 |

---

### 6.4 删除收藏夹

**接口信息**
- 路径: `DELETE /api/user/collection-folders/{id}`
- 鉴权: 必须登录
- 说明: 删除指定的收藏夹（同时删除夹内所有收藏记录）

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 收藏夹 ID |

**请求示例**

```javascript
// axios
axios.delete('/api/user/collection-folders/5')

// fetch
fetch('/api/user/collection-folders/5', { method: 'DELETE' })
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新收藏夹列表 |
| 40102 | 未登录 | 跳转登录页 |
| 40400 | 收藏夹不存在 | 提示收藏夹不存在 |

---

### 6.5 查询我的收藏

**接口信息**
- 路径: `GET /api/user/collections`
- 鉴权: 必须登录
- 说明: 返回当前用户的收藏记录分页列表

**请求示例**

```javascript
// axios
axios.get('/api/user/collections')

// fetch
fetch('/api/user/collections')
  .then(res => res.json())
```

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
        "id": 200,
        "folderId": 1,
        "targetId": 100,
        "targetType": "article",
        "remark": "很实用的文章",
        "targetTitle": "Spring Boot 权威指南",
        "targetUrl": "/article/100",
        "createdAt": "2025-01-10T14:00:00"
      }
    ]
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 收藏记录 ID |
| folderId | Long | 所属收藏夹 ID |
| targetId | Long | 目标 ID（如文章 ID） |
| targetType | String | 目标类型，article 表示文章 |
| remark | String | 收藏备注 |
| targetTitle | String | 目标标题 |
| targetUrl | String | 目标地址 |
| createdAt | String | 收藏时间 |

---

### 6.6 新增收藏

**接口信息**
- 路径: `POST /api/user/collections`
- 鉴权: 必须登录
- 说明: 将目标（文章等）添加到收藏夹

**请求体**

```json
{
  "folderId": 1,
  "targetId": 100,
  "targetType": "article",
  "remark": "很实用的文章"
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| folderId | Long | 是 | 收藏夹 ID |
| targetId | Long | 是 | 目标 ID（文章 ID） |
| targetType | String | 是 | 目标类型，固定为 article |
| remark | String | 否 | 收藏备注 |

**请求示例**

```javascript
// axios
axios.post('/api/user/collections', {
  folderId: 1,
  targetId: 100,
  targetType: 'article',
  remark: '很实用的文章'
})

// fetch
fetch('/api/user/collections', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    folderId: 1,
    targetId: 100,
    targetType: 'article'
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 显示收藏成功提示 |
| 40001 | 参数校验失败 | folderId 或 targetId 为空 |
| 40102 | 未登录 | 跳转登录页 |

---

### 6.7 删除收藏

**接口信息**
- 路径: `DELETE /api/user/collections/{id}`
- 鉴权: 必须登录
- 说明: 删除指定的收藏记录

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 收藏记录 ID |

**请求示例**

```javascript
// axios
axios.delete('/api/user/collections/200')

// fetch
fetch('/api/user/collections/200', { method: 'DELETE' })
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新收藏列表 |
| 40102 | 未登录 | 跳转登录页 |
| 40400 | 收藏记录不存在 | 提示收藏记录不存在 |

---

## 七、后台文章管理（需管理员权限）

### 7.1 分页查询文章

**接口信息**
- 路径: `GET /api/sys/articles`
- 鉴权: 必须登录且有 `content:article:query` 权限
- 说明: 后台分页查询文章列表，支持多条件筛选

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|-----|------|
| current | Long | 否 | 页码，默认 1 | 1 |
| size | Long | 否 | 每页条数，默认 10 | 10 |
| keyword | String | 否 | 搜索关键字（匹配标题和摘要） | Spring |
| authorId | Long | 否 | 作者 ID | 8 |
| status | Integer | 否 | 文章状态 | 1 |
| reviewStatus | Integer | 否 | 审核状态 | 0 |
| accessLevel | Integer | 否 | 访问级别 | 0 |
| visibilityScope | Integer | 否 | 可见范围 | 0 |
| categoryId | Long | 否 | 分类 ID | 5 |
| tagId | Long | 否 | 标签 ID | 10 |
| isTop | Integer | 否 | 是否置顶 | 1 |
| isRecommend | Integer | 否 | 是否推荐 | 1 |
| publishTimeStart | String | 否 | 发布时间开始 | 2025-01-01 00:00:00 |
| publishTimeEnd | String | 否 | 发布时间结束 | 2025-01-31 23:59:59 |

**文章状态 status 取值：**
- `0` - 草稿
- `1` - 已发布
- `2` - 待发布
- `3` - 已下架

**审核状态 reviewStatus 取值：**
- `0` - 待审核
- `1` - 审核通过
- `2` - 审核拒绝

**请求示例**

```javascript
// axios
axios.get('/api/sys/articles', {
  params: {
    current: 1,
    size: 10,
    keyword: 'Spring',
    status: 1
  },
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles?current=1&size=10&status=1', {
  headers: { 'Authorization': 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "total": 120,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 100,
        "title": "Spring Boot 权威指南",
        "summary": "本文详细介绍 Spring Boot 的核心特性...",
        "coverImage": "https://example.com/cover.jpg",
        "authorId": 8,
        "authorName": "张三",
        "isTop": 1,
        "isRecommend": 1,
        "isOriginal": 1,
        "status": 1,
        "reviewStatus": 1,
        "accessLevel": 0,
        "visibilityScope": 0,
        "viewCount": 1520,
        "likeCount": 128,
        "commentCount": 35,
        "collectCount": 67,
        "shareCount": 12,
        "publishTime": "2025-01-10T08:00:00",
        "scheduledPublishTime": null,
        "createdAt": "2024-12-20T10:00:00",
        "updatedAt": "2025-01-10T08:00:00",
        "remark": "优质文章，已推荐至首页"
      }
    ]
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| id | Long | 文章 ID |
| title | String | 文章标题 |
| summary | String | 文章摘要 |
| coverImage | String | 封面图地址 |
| authorId| Long | 作者 ID |
| authorName | String | 作者昵称 |
| isTop | Integer | 是否置顶 |
| isRecommend | Integer | 是否推荐 |
| isOriginal | Integer | 是否原创 |
| status | Integer | 文章状态 |
| reviewStatus | Integer | 审核状态 |
| accessLevel | Integer | 访问级别 |
| visibilityScope | Integer | 可见范围 |
| viewCount | Integer | 浏览数 |
| likeCount | Integer | 点赞数 |
| commentCount | Integer | 评论数 |
| collectCount | Integer | 收藏数 |
| shareCount | Integer | 分享数 |
| publishTime | String | 发布时间 |
| scheduledPublishTime | String | 定时发布时间 |
| createdAt | String | 创建时间 |
| updatedAt | String | 更新时间 |
| remark | String | 备注 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | - |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |

---

### 7.2 查询文章详情

**接口信息**
- 路径: `GET /api/sys/articles/{id}`
- 鉴权: 必须登录且有 `content:article:query` 权限
- 说明: 后台查询文章的完整详情

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求示例**

```javascript
// axios
axios.get('/api/sys/articles/100', {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/100', {
  headers: { 'Authorization': 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 100,
    "title": "Spring Boot 权威指南",
    "summary": "本文详细介绍 Spring Boot 的核心特性...",
    "content": "<p>文章正文 HTML 内容...</p>",
    "coverImage": "https://example.com/cover.jpg",
    "authorId": 8,
    "authorName": "张三",
    "isTop": 1,
    "isRecommend": 1,
    "isOriginal": 1,
    "sourceUrl": null,
    "status": 1,
    "reviewStatus": 1,
    "publishTime": "2025-01-10T08:00:00",
    "scheduledPublishTime": null,
    "accessLevel": 0,
    "visibilityScope": 0,
    "viewCount": 1520,
    "likeCount": 128,
    "commentCount": 35,
    "collectCount": 67,
    "shareCount": 12,
    "createdAt": "2024-12-20T10:00:00",
    "updatedAt": "2025-01-10T08:00:00",
    "remark": "优质文章，已推荐至首页",
    "categoryIds": [1, 5],
    "tagIds": [10, 11],
    "accessList": [
      {
        "userId": 15,
        "accessType": 1,
        "expireTime": "2025-12-31T23:59:59",
        "grantReason": "VIP用户"
      }
    ],
    "seriesList": []
  }
}
```

**新增字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| categoryIds | Array | 分类 ID 列表 |
| tagIds | Array | 标签 ID 列表 |
| accessList | Array | 访问授权列表 |

**accessList 子字段**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| userId | Long | 被授权用户 ID |
| accessType | Integer | 授权类型 |
| expireTime | String | 过期时间 |
| grantReason | String | 授权原因 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | - |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |
| 40400 | 文章不存在 | 提示文章不存在 |

---

### 7.3 新增文章

**接口信息**
- 路径: `POST /api/sys/articles`
- 鉴权: 必须登录且有 `content:article:create` 权限
- 说明: 创建新文章

**请求体**

```json
{
  "title": "文章标题",
  "summary": "文章摘要",
  "content": "<p>文章正文内容</p>",
  "coverImage": "https://example.com/cover.jpg",
  "authorId": 8,
  "isTop": 0,
  "isRecommend": 0,
  "isOriginal": 1,
  "sourceUrl": null,
  "status": 1,
  "publishTime": "2025-01-10T08:00:00",
  "scheduledPublishTime": null,
  "accessLevel": 0,
  "visibilityScope": 0,
  "remark": "备注信息",
  "categoryIds": [1, 5],
  "tagIds": [10, 11],
  "accessList": []
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| title | String | 是 | 文章标题，最大 128 字符 |
| summary | String | 否 | 文章摘要，最大 2000 字符 |
| content | String | 否 | 文章正文（HTML） |
| coverImage | String | 否 | 封面图地址，最大 512 字符 |
| authorId | Long | 是 | 作者 ID |
| isTop | Integer | 否 | 是否置顶，0-否，1-是 |
| isRecommend | Integer | 否 | 是否推荐，0-否，1-是 |
| isOriginal | Integer | 否 | 是否原创，0-否，1-是 |
| sourceUrl | String | 否 | 来源地址（转载时填写） |
| status | Integer | 否 | 文章状态，0-草稿，1-已发布 |
| publishTime | String | 否 | 发布时间，格式 yyyy-MM-dd HH:mm:ss |
| scheduledPublishTime | String | 否 | 定时发布时间 |
| accessLevel | Integer | 否 | 访问级别，0-免费 |
| visibilityScope | Integer | 否 | 可见范围，0-公开 |
| remark | String | 否 | 备注，最大 256 字符 |
| categoryIds | Array | 否 | 分类 ID 列表 |
| tagIds | Array | 否 | 标签 ID 列表 |
| accessList | Array | 否 | 访问授权列表 |

**请求示例**

```javascript
// axios
axios.post('/api/sys/articles', {
  title: '新文章标题',
  authorId: 8,
  isOriginal: 1,
  status: 1,
  categoryIds: [1],
  tagIds: [10]
}, {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xxx'
  },
  body: JSON.stringify({
    title: '新文章标题',
    authorId: 8,
    status: 1
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:30:00",
  "data": {
    "id": 105,
    "title": "新文章标题",
    "summary": null,
    "content": null,
    "coverImage": null,
    "authorId": 8,
    "authorName": "张三",
    "isTop": 0,
    "isRecommend": 0,
    "isOriginal": 1,
    "sourceUrl": null,
    "status": 1,
    "reviewStatus": 1,
    "publishTime": "2025-01-15T10:30:00",
    "scheduledPublishTime": null,
    "accessLevel": 0,
    "visibilityScope": 0,
    "viewCount": 0,
    "likeCount": 0,
    "commentCount": 0,
    "collectCount": 0,
    "shareCount": 0,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00",
    "remark": null,
    "categoryIds": [1],
    "tagIds": [10],
    "accessList": [],
    "seriesList": []
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 关闭编辑弹窗，刷新列表，跳转详情 |
| 40001 | 参数校验失败 | 检查必填字段 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |

---

### 7.4 修改文章

**接口信息**
- 路径: `PUT /api/sys/articles/{id}`
- 鉴权: 必须登录且有 `content:article:update` 权限
- 说明: 修改指定文章的信息

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求体**

同 7.3 新增文章

**请求示例**

```javascript
// axios
axios.put('/api/sys/articles/105', {
  title: '修改后的标题',
  summary: '修改后的摘要'
}, {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xxx'
  },
  body: JSON.stringify({
    title: '修改后的标题'
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:35:00",
  "data": {
    "id": 105,
    "title": "修改后的标题",
    "summary": "修改后的摘要",
    "content": null,
    "coverImage": null,
    "authorId": 8,
    "authorName": "张三",
    "isTop": 0,
    "isRecommend": 0,
    "isOriginal": 1,
    "sourceUrl": null,
    "status": 1,
    "reviewStatus": 1,
    "publishTime": "2025-01-15T10:30:00",
    "scheduledPublishTime": null,
    "accessLevel": 0,
    "visibilityScope": 0,
    "viewCount": 0,
    "likeCount": 0,
    "commentCount": 0,
    "collectCount": 0,
    "shareCount": 0,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:35:00",
    "remark": null,
    "categoryIds": [1],
    "tagIds": [10],
    "accessList": [],
    "seriesList": []
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 关闭编辑弹窗，刷新列表 |
| 40001 | 参数校验失败 | 检查必填字段 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |
| 40400 | 文章不存在 | 提示文章不存在 |

---

### 7.5 修改文章状态

**接口信息**
- 路径: `PUT /api/sys/articles/{id}/status`
- 鉴权: 必须登录且有 `content:article:update-status` 权限
- 说明: 修改文章的发布状态

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求体**

```json
{
  "status": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| status | Integer | 是 | 文章状态，0-草稿，1-已发布，2-待发布，3-已下架 |

**请求示例**

```javascript
// axios
axios.put('/api/sys/articles/105/status', { status: 3 }, {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105/status', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xxx'
  },
  body: JSON.stringify({ status: 3 })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:40:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新列表状态 |
| 40001 | 参数校验失败 | status 为空 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |
| 40400 | 文章不存在 | 提示文章不存在 |

---

### 7.6 切换文章置顶状态

**接口信息**
- 路径: `PUT /api/sys/articles/{id}/top`
- 鉴权: 必须登录且有 `content:article:update` 权限
- 说明: 切换文章的置顶状态

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| enabled | Boolean | 是 | 是否置顶，true-置顶，false-取消置顶 |

**请求示例**

```javascript
// axios
axios.put('/api/sys/articles/105/top', null, {
  params: { enabled: true },
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105/top?enabled=true', {
  method: 'PUT',
  headers: { 'Authorization': 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:40:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新列表 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |

---

### 7.7 切换文章推荐状态

**接口信息**
- 路径: `PUT /api/sys/articles/{id}/recommend`
- 鉴权: 必须登录且有 `content:article:update` 权限
- 说明: 切换文章的推荐状态

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| enabled | Boolean | 是 | 是否推荐，true-推荐，false-取消推荐 |

**请求示例**

```javascript
// axios
axios.put('/api/sys/articles/105/recommend', null, {
  params: { enabled: true },
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105/recommend?enabled=true', {
  method: 'PUT',
  headers: { 'Authorization': 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:40:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新列表 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |

---

### 7.8 配置文章访问名单

**接口信息**
- 路径: `PUT /api/sys/articles/{id}/access`
- 鉴权: 必须登录且有 `content:article:access` 权限
- 说明: 配置文章的访问白名单用户

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求体**

```json
{
  "accessList": [
    {
      "userId": 15,
      "accessType": 1,
      "expireTime": "2025-12-31T23:59:59",
      "grantReason": "VIP用户"
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| accessList | Array | 是 | 访问授权列表 |
| accessList[].userId | Long | 是 | 被授权用户 ID |
| accessList[].accessType | Integer | 否 | 授权类型 |
| accessList[].expireTime | String | 否 | 过期时间，格式 yyyy-MM-dd HH:mm:ss |
| accessList[].grantReason | String | 否 | 授权原因 |

**请求示例**

```javascript
// axios
axios.put('/api/sys/articles/105/access', {
  accessList: [
    { userId: 15, accessType: 1, grantReason: 'VIP用户' }
  ]
}, {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105/access', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xxx'
  },
  body: JSON.stringify({
    accessList: [{ userId: 15, accessType: 1 }]
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:40:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 关闭弹窗，刷新 |
| 40001 | 参数校验失败 | 检查参数 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |

---

### 7.9 删除文章

**接口信息**
- 路径: `DELETE /api/sys/articles/{id}`
- 鉴权: 必须登录且有 `content:article:delete` 权限
- 说明: 删除指定文章

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| id | Long | 是 | 文章 ID |

**请求示例**

```javascript
// axios
axios.delete('/api/sys/articles/105', {
  headers: { 'Authorization': 'Bearer xxx' }
})

// fetch
fetch('/api/sys/articles/105', {
  method: 'DELETE',
  headers: { 'Authorization': 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": "2025-01-15T10:40:00",
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 200 | 成功 | 刷新列表 |
| 40102 | 未登录 | 跳转登录页 |
| 40300 | 无权限 | 提示无权限 |
| 40400 | 文章不存在 | 提示文章不存在 |

---

## 八、接口速查表

### 公开接口（无需登录）

| 接口 | 方法 | 路径 | 说明 |
|-----|------|-----|------|
| 文章分页列表 | GET | /api/articles | 首页/列表页文章 |
| 文章详情 | GET | /api/articles/{id} | 文章详情页 |
| 分类树 | GET | /api/categories/tree | 获取分类结构 |
| 标签列表 | GET | /api/tags | 获取所有标签 |
| 评论树 | GET | /api/comments | 获取文章评论 |

### 用户接口（需登录）

| 接口 | 方法 | 路径 | 说明 |
|-----|------|-----|------|
| 点赞文章 | POST | /api/user/articles/{id}/likes | 点赞 |
| 取消点赞 | DELETE | /api/user/articles/{id}/likes | 取消点赞 |
| 收藏夹列表 | GET | /api/user/collection-folders | 我的收藏夹 |
| 新增收藏夹 | POST | /api/user/collection-folders | 创建收藏夹 |
| 修改收藏夹 | PUT | /api/user/collection-folders/{id} | 编辑收藏夹 |
| 删除收藏夹 | DELETE | /api/user/collection-folders/{id} | 删除收藏夹 |
| 收藏列表 | GET | /api/user/collections | 我的收藏 |
| 新增收藏 | POST | /api/user/collections | 添加收藏 |
| 删除收藏 | DELETE | /api/user/collections/{id} | 移除收藏 |

### 后台接口（需管理员权限）

| 接口 | 方法 | 路径 | 权限 |
|-----|------|-----|------|
| 文章列表 | GET | /api/sys/articles | content:article:query |
| 文章详情 | GET | /api/sys/articles/{id} | content:article:query |
| 新增文章 | POST | /api/sys/articles | content:article:create |
| 修改文章 | PUT | /api/sys/articles/{id} | content:article:update |
| 修改状态 | PUT | /api/sys/articles/{id}/status | content:article:update-status |
| 配置访问 | PUT | /api/sys/articles/{id}/access | content:article:access |
| 切换置顶 | PUT | /api/sys/articles/{id}/top | content:article:update |
| 切换推荐 | PUT | /api/sys/articles/{id}/recommend | content:article:update |
| 删除文章 | DELETE | /api/sys/articles/{id} | content:article:delete |

---

## 九、友情链接

### 公开接口

#### 查询启用的友情链接

**接口信息**

- 路径: `GET /api/public/friend-links`
- 鉴权: 无（公开接口）
- 说明: 查询所有启用状态的友情链接，按 sortOrder 排序

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `name` | String | 站点名称 |
| `url` | String | 站点地址 |
| `iconUrl` | String | 图标地址 |
| `description` | String | 站点描述 |

---

### 后台管理

#### 分页查询友情链接

**接口信息**

- 路径: `GET /api/sys/friend-links`
- 鉴权: `content:friend-link:query`

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `name` | String | 否 | 站点名称（模糊匹配） |
| `status` | Integer | 否 | 状态：0-停用，1-启用 |

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | ID |
| `name` | String | 站点名称 |
| `url` | String | 站点地址 |
| `iconUrl` | String | 图标地址 |
| `description` | String | 站点描述 |
| `sortOrder` | Integer | 排序值 |
| `status` | Integer | 状态：0-停用，1-启用 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

---

#### 查询友情链接详情

**接口信息**

- 路径: `GET /api/sys/friend-links/{id}`
- 鉴权: `content:friend-link:query`

---

#### 创建友情链接

**接口信息**

- 路径: `POST /api/sys/friend-links`
- 鉴权: `content:friend-link:create`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `name` | String | 是 | 站点名称（最多 64 字符） |
| `url` | String | 是 | 站点地址（需以 http:// 或 https:// 开头） |
| `iconUrl` | String | 否 | 图标地址 |
| `description` | String | 否 | 站点描述（最多 255 字符） |
| `sortOrder` | Integer | 否 | 排序值 |

---

#### 更新友情链接

**接口信息**

- 路径: `PUT /api/sys/friend-links/{id}`
- 鉴权: `content:friend-link:update`
- 请求体字段: 同创建友情链接

---

#### 更新友情链接状态

**接口信息**

- 路径: `PUT /api/sys/friend-links/{id}/status`
- 鉴权: `content:friend-link:update`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态：0-停用，1-启用 |

---

#### 删除友情链接

**接口信息**

- 路径: `DELETE /api/sys/friend-links/{id}`
- 鉴权: `content:friend-link:delete`

---

## 十、后台分类管理

### 分页查询分类树

**接口信息**

- 路径: `GET /api/sys/categories/tree`
- 鉴权: `content:category:query`
- 说明: 返回分类树形结构

---

### 查询分类详情

**接口信息**

- 路径: `GET /api/sys/categories/{id}`
- 鉴权: `content:category:query`

---

### 创建分类

**接口信息**

- 路径: `POST /api/sys/categories`
- 鉴权: `content:category:create`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `parentId` | Long | 是 | 父分类 ID，顶级分类传 `0` |
| `name` | String | 是 | 分类名称 |
| `code` | String | 是 | 分类编码 |
| `type` | String | 是 | 分类类型 |
| `sortOrder` | Integer | 否 | 排序值 |
| `icon` | String | 否 | 图标 |
| `description` | String | 否 | 描述 |
| `status` | Integer | 否 | 状态：0-停用，1-启用 |

---

### 更新分类

**接口信息**

- 路径: `PUT /api/sys/categories/{id}`
- 鉴权: `content:category:update`
- 请求体字段: 同创建分类

---

### 更新分类状态

**接口信息**

- 路径: `PUT /api/sys/categories/{id}/status`
- 鉴权: `content:category:update`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态：0-停用，1-启用 |

---

### 删除分类

**接口信息**

- 路径: `DELETE /api/sys/categories/{id}`
- 鉴权: `content:category:delete`

---

## 十一、后台标签管理

### 查询标签列表

**接口信息**

- 路径: `GET /api/sys/tags`
- 鉴权: `content:tag:query`
- 说明: 返回标签列表

---

### 查询标签详情

**接口信息**

- 路径: `GET /api/sys/tags/{id}`
- 鉴权: `content:tag:query`

---

### 创建标签

**接口信息**

- 路径: `POST /api/sys/tags`
- 鉴权: `content:tag:create`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `name` | String | 是 | 标签名称 |
| `color` | String | 否 | 标签颜色（十六进制） |

---

### 更新标签

**接口信息**

- 路径: `PUT /api/sys/tags/{id}`
- 鉴权: `content:tag:update`
- 请求体字段: 同创建标签

---

### 删除标签

**接口信息**

- 路径: `DELETE /api/sys/tags/{id}`
- 鉴权: `content:tag:delete`

---

## 十二、用户评论行为

### 点赞评论

**接口信息**

- 路径: `POST /api/user/comments/{id}/likes`
- 鉴权: 必须登录

---

### 取消点赞评论

**接口信息**

- 路径: `DELETE /api/user/comments/{id}/likes`
- 鉴权: 必须登录

---

### 创建评论

**接口信息**

- 路径: `POST /api/user/comments`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `targetType` | String | 是 | 目标类型 |
| `targetId` | Long | 是 | 目标 ID |
| `content` | String | 是 | 评论内容 |
| `images` | List\<String\> | 否 | 图片列表 |
| `rootId` | Long | 否 | 根评论 ID，默认 `0` |
| `parentId` | Long | 否 | 父评论 ID，默认 `0` |

---

### 删除评论

**接口信息**

- 路径: `DELETE /api/user/comments/{id}`
- 鉴权: 必须登录

---

## 十三、后台评论管理

### 分页查询评论

**接口信息**

- 路径: `GET /api/sys/comments`
- 鉴权: `content:comment:query`
- 说明: 分页查询评论列表

---

### 查询评论详情

**接口信息**

- 路径: `GET /api/sys/comments/{id}`
- 鉴权: `content:comment:query`

---

### 更新评论状态

**接口信息**

- 路径: `PUT /api/sys/comments/{id}/status`
- 鉴权: `content:comment:update`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `status` | Integer | 是 | 状态 |

---

### 删除评论

**接口信息**

- 路径: `DELETE /api/sys/comments/{id}`
- 鉴权: `content:comment:delete`

---

## 十四、后台收藏管理

### 分页查询收藏夹

**接口信息**

- 路径: `GET /api/sys/collections/folders`
- 鉴权: `content:collection:query`
- 说明: 分页查询收藏夹列表

---

### 分页查询收藏记录

**接口信息**

- 路径: `GET /api/sys/collections`
- 鉴权: `content:collection:query`
- 说明: 分页查询收藏记录

---

### 删除收藏记录

**接口信息**

- 路径: `DELETE /api/sys/collections/{id}`
- 鉴权: `content:collection:delete`

---

## 十五、用户足迹

### 分页查询足迹

**接口信息**

- 路径: `GET /api/user/footprints`
- 鉴权: 必须登录
- 说明: 分页查询当前用户的浏览足迹

---

### 删除足迹

**接口信息**

- 路径: `DELETE /api/user/footprints/{id}`
- 鉴权: 必须登录

---

### 清空足迹

**接口信息**

- 路径: `DELETE /api/user/footprints`
- 鉴权: 必须登录
- 说明: 清空当前用户全部浏览足迹

---

## 十六、后台足迹管理

### 分页查询足迹

**接口信息**

- 路径: `GET /api/sys/footprints`
- 鉴权: `content:footprint:query`
- 说明: 分页查询足迹列表

---

### 删除足迹

**接口信息**

- 路径: `DELETE /api/sys/footprints/{id}`
- 鉴权: `content:footprint:delete`

---

### 按条件清理足迹

**接口信息**

- 路径: `DELETE /api/sys/footprints`
- 鉴权: `content:footprint:delete`
- 说明: 按条件批量清理足迹

---

## 十七、后台互动管理

### 分页查询互动记录

**接口信息**

- 路径: `GET /api/sys/interactions`
- 鉴权: `content:interaction:query`
- 说明: 分页查询互动记录（点赞等）

---

### 删除互动记录

**接口信息**

- 路径: `DELETE /api/sys/interactions/{id}`
- 鉴权: `content:interaction:delete`

---

## 十八、公开文章系列

### 查询作者系列列表

**接口信息**

- 路径: `GET /api/public/authors/{authorId}/series`
- 鉴权: 无（公开接口）
- 说明: 返回指定作者的文章系列列表

---

### 查询系列详情

**接口信息**

- 路径: `GET /api/public/article-series/{id}`
- 鉴权: 无（公开接口）

---

## 十九、用户文章审核

### 提交文章审核

**接口信息**

- 路径: `POST /api/user/articles/{id}/submit-review`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `reviewComment` | String | 否 | 审核备注（最多 512 字符） |

---

### 查询审核日志

**接口信息**

- 路径: `GET /api/user/articles/{id}/review-log`
- 鉴权: 必须登录

---

## 二十、用户文章管理

### 分页查询我的文章

**接口信息**

- 路径: `GET /api/user/articles`
- 鉴权: 必须登录
- 说明: 分页查询当前用户的文章列表

---

### 查询我的文章详情

**接口信息**

- 路径: `GET /api/user/articles/{id}`
- 鉴权: 必须登录

---

### 配置文章访问名单

**接口信息**

- 路径: `PUT /api/user/articles/{id}/access`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `accessList` | Array | 是 | 访问授权列表 |
| `accessList[].userId` | Long | 是 | 被授权用户 ID |
| `accessList[].accessType` | Integer | 否 | 授权类型 |
| `accessList[].expireTime` | LocalDateTime | 否 | 过期时间 |
| `accessList[].grantReason` | String | 否 | 授权原因 |

---

## 二十一、用户文章系列

### 查询我的系列列表

**接口信息**

- 路径: `GET /api/user/article-series`
- 鉴权: 必须登录
- 说明: 返回当前用户的文章系列列表

---

### 查询我的系列详情

**接口信息**

- 路径: `GET /api/user/article-series/{id}`
- 鉴权: 必须登录

---

### 创建系列

**接口信息**

- 路径: `POST /api/user/article-series`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `title` | String | 是 | 系列标题（最多 128 字符） |
| `description` | String | 否 | 系列描述（最多 1024 字符） |
| `coverImage` | String | 否 | 封面图（最多 512 字符） |
| `status` | Integer | 否 | 状态 |
| `visibilityScope` | Integer | 否 | 可见范围 |
| `sortOrder` | Integer | 否 | 排序值 |

---

### 更新系列

**接口信息**

- 路径: `PUT /api/user/article-series/{id}`
- 鉴权: 必须登录
- 请求体字段: 同创建系列

---

### 删除系列

**接口信息**

- 路径: `DELETE /api/user/article-series/{id}`
- 鉴权: 必须登录

---

### 添加文章到系列

**接口信息**

- 路径: `POST /api/user/article-series/{id}/articles`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `articleId` | Long | 是 | 文章 ID |

---

### 从系列移除文章

**接口信息**

- 路径: `DELETE /api/user/article-series/{id}/articles/{articleId}`
- 鉴权: 必须登录

---

### 系列内文章排序

**接口信息**

- 路径: `PUT /api/user/article-series/{id}/articles/sort`
- 鉴权: 必须登录

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `articleIds` | List\<Long\> | 是 | 文章 ID 列表（按顺序排列） |

---

## 二十二、后台文章审核管理

### 分页查询审核记录

**接口信息**

- 路径: `GET /api/sys/article-reviews`
- 鉴权: `content:article-review:query`
- 说明: 分页查询文章审核记录

---

### 查询审核详情

**接口信息**

- 路径: `GET /api/sys/article-reviews/{id}`
- 鉴权: `content:article-review:query`

---

### 通过审核

**接口信息**

- 路径: `PUT /api/sys/article-reviews/{id}/approve`
- 鉴权: `content:article-review:review`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `reviewComment` | String | 否 | 审核备注（最多 512 字符） |

---

### 驳回审核

**接口信息**

- 路径: `PUT /api/sys/article-reviews/{id}/reject`
- 鉴权: `content:article-review:review`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `reviewComment` | String | 是 | 驳回原因（最多 512 字符） |

---

### 修复审核状态

**接口信息**

- 路径: `PUT /api/sys/article-reviews/{id}/repair-status`
- 鉴权: `content:article-review:repair`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `targetReviewStatus` | Integer | 是 | 目标审核状态 |
| `reviewComment` | String | 否 | 备注（最多 512 字符） |

---

## 二十三、公开文件访问

### 下载文件

**接口信息**

- 路径: `GET /api/public/files/{fileId}`
- 鉴权: 无（公开接口）
- 说明: 返回文件流（非 Result 包装，直接下载）
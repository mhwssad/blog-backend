# 认证与用户 API 前端参考手册

> 本文档面向前端联调。覆盖登录注册、个人资料、通知中心等功能的完整接口参考，包含请求示例、响应示例、错误码说明和前端集成指南。

## 目录

- [快速接口索引](#快速接口索引)
- [统一响应结构](#统一响应结构)
- [登录页 / 注册页](#登录页--注册页)
  - [账号密码登录](#账号密码登录)
  - [账号注册](#账号注册)
  - [发送邮箱验证码](#发送邮箱验证码)
  - [邮箱验证码登录](#邮箱验证码登录)
- [应用启动初始化](#应用启动初始化)
  - [恢复登录态](#恢复登录态)
  - [获取当前用户信息](#获取当前用户信息)
  - [获取用户菜单](#获取用户菜单)
- [Token 刷新机制](#token-刷新机制)
- [退出登录](#退出登录)
- [个人中心](#个人中心)
  - [查看个人资料](#查看个人资料)
  - [更新个人资料](#更新个人资料)
  - [修改密码](#修改密码)
- [通知中心](#通知中心)
  - [通知列表](#通知列表)
  - [通知详情](#通知详情)
  - [未读数量](#未读数量)
  - [标记已读](#标记已读)
- [错误码速查](#错误码速查)
- [前端集成指南](#前端集成指南)
  - [登录流程时序](#登录流程时序)
  - [axios 拦截器配置](#axios-拦截器配置)
  - [Token 存储建议](#token-存储建议)

---

## 快速接口索引

| 功能 | 方法 | 路径 | 鉴权 | 说明 |
|-----|------|------|------|------|
| 账号登录 | POST | `/api/auth/login` | 否 | 用户名/邮箱/手机号 + 密码 |
| 账号注册 | POST | `/api/auth/register` | 否 | 支持邮箱/手机号 |
| 发送邮箱验证码 | POST | `/api/auth/email-code` | 否 | 用于邮箱登录/注册 |
| 邮箱验证码登录 | POST | `/api/auth/email-login` | 否 | 邮箱 + 验证码 |
| 刷新令牌 | POST | `/api/auth/refresh` | 否 | 使用 refreshToken |
| 退出登录 | POST | `/api/auth/logout` | 是 | 支持不传 token |
| 获取当前用户 | GET | `/api/auth/current-user` | 是 | 包含角色权限 |
| 获取用户菜单 | GET | `/api/auth/current-user-menus` | 是 | 树形菜单结构 |
| 获取个人资料 | GET | `/api/user/profile` | 是 | 个人详细信息 |
| 更新个人资料 | PUT | `/api/user/profile` | 是 | 修改昵称/头像等 |
| 修改密码 | PUT | `/api/user/profile/password` | 是 | 需验证原密码 |
| 通知列表 | GET | `/api/user/notices` | 是 | 分页查询 |
| 通知详情 | GET | `/api/user/notices/{id}` | 是 | 单条通知 |
| 未读数量 | GET | `/api/user/notices/unread-count` | 是 | 数字 |
| 单条已读 | POST | `/api/user/notices/{id}/read` | 是 | - |
| 全部已读 | POST | `/api/user/notices/read-all` | 是 | - |

---

## 统一响应结构

所有接口均返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

**分页响应**（通知列表等）：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": []
  }
}
```

---

## 登录页 / 注册页

### 账号密码登录

**接口信息**
- 路径: `POST /api/auth/login`
- 鉴权: 否
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| username | string | 是 | 登录账号，支持用户名/邮箱/手机号 | `admin` |
| password | string | 是 | 密码 | `Password123` |

**请求示例**

```javascript
// axios
axios.post('/api/auth/login', {
  username: 'admin',
  password: 'Password123'
})
```

```javascript
// fetch
fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'Password123'
  })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|------|
| tokenType | string | 令牌类型，固定为 `Bearer` |
| accessToken | string | 访问令牌，用于接口鉴权 |
| refreshToken | string | 刷新令牌，用于续期 |
| expiresIn | integer | 过期时间，单位：秒（7200 = 2小时） |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40101 | 用户名或密码错误 | 显示「用户名或密码错误」 |
| 40104 | 账号已锁定 | 显示「连续失败过多，请15分钟后再试」 |
| 40105 | 账号已禁用 | 显示「账号已被禁用」 |

---

### 账号注册

**接口信息**
- 路径: `POST /api/auth/register`
- 鉴权: 否
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| username | string | 是 | 用户名 | `new_user` |
| password | string | 是 | 密码（需包含大小写字母和数字，8-64位） | `Abc12345` |
| nickname | string | 否 | 昵称 | `新用户` |
| email | string | 否 | 邮箱地址 | `user@example.com` |
| phone | string | 否 | 手机号 | `13800138000` |

**请求示例**

```javascript
// axios
axios.post('/api/auth/register', {
  username: 'new_user',
  password: 'Abc12345',
  nickname: '新用户',
  email: 'user@example.com'
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40001 | 参数校验失败 | 显示具体字段错误信息 |
| 40114 | 验证码发送失败 | 显示「验证码发送失败，请重试」 |

---

### 发送邮箱验证码

**接口信息**
- 路径: `POST /api/auth/email-code`
- 鉴权: 否
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| email | string | 是 | 邮箱地址 | `user@example.com` |

**请求示例**

```javascript
// axios
axios.post('/api/auth/email-code', {
  email: 'user@example.com'
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40115 | 发送过于频繁 | 显示「发送过于频繁，请稍后再试」 |
| 40114 | 发送失败 | 显示「验证码发送失败」 |

---

### 邮箱验证码登录

**接口信息**
- 路径: `POST /api/auth/email-login`
- 鉴权: 否
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| email | string | 是 | 邮箱地址 | `admin@example.com` |
| code | string | 是 | 邮箱验证码（6位数字） | `123456` |

**请求示例**

```javascript
// axios
axios.post('/api/auth/email-login', {
  email: 'admin@example.com',
  code: '123456'
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40112 | 验证码错误 | 显示「验证码错误」 |
| 40113 | 验证码已过期 | 显示「验证码已过期，请重新获取」 |

---

## 应用启动初始化

### 恢复登录态

应用启动时（如页面刷新、前端路由守卫），按以下顺序调用：

```
1. GET /api/auth/current-user    → 获取用户基本信息
2. GET /api/auth/current-user-menus → 获取菜单权限
```

---

### 获取当前用户信息

**接口信息**
- 路径: `GET /api/auth/current-user`
- 鉴权: 是（需携带 `Authorization: Bearer <accessToken>`）

**请求示例**

```javascript
// axios
const res = await axios.get('/api/auth/current-user')
// res.data.data 即为用户信息对象
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "https://example.com/avatar.jpg",
    "bio": "这是个人简介",
    "website": "https://example.com",
    "gender": 1,
    "birthday": "1990-01-01",
    "email": "admin@example.com",
    "phone": "13800138000",
    "status": 1,
    "userLevel": 5,
    "experiencePoints": 15000,
    "roles": ["admin"],
    "permissions": ["system:user:view", "system:user:edit"]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | long | 用户ID |
| username | string | 用户名 |
| nickname | string | 昵称 |
| avatar | string | 头像URL |
| bio | string | 个人简介 |
| website | string | 个人站点 |
| gender | integer | 性别：0-未知，1-男，2-女，3-保密 |
| birthday | string | 生日，格式 `yyyy-MM-dd` |
| email | string | 邮箱 |
| phone | string | 手机号 |
| status | integer | 状态：1-正常 |
| userLevel | integer | 用户等级 |
| experiencePoints | integer | 经验值 |
| roles | array | 角色编码列表，如 `["admin", "author"]` |
| permissions | array | 权限标识列表，如 `["system:user:view"]` |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40102 | 未登录或登录已过期 | 跳转登录页 |
| 40108 | 无效的令牌 | 跳转登录页 |

---

### 获取用户菜单

**接口信息**
- 路径: `GET /api/auth/current-user-menus`
- 鉴权: 是

**请求示例**

```javascript
// axios
const res = await axios.get('/api/auth/current-user-menus')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "工作台",
      "type": "menu",
      "routeName": "Dashboard",
      "routePath": "/dashboard",
      "component": "dashboard/index",
      "visible": 1,
      "sort": 1,
      "icon": "ant-design:dashboard-outlined",
      "redirect": null,
      "alwaysShow": null,
      "keepAlive": null,
      "params": null,
      "children": []
    },
    {
      "id": 2,
      "parentId": 0,
      "name": "系统管理",
      "type": "menu",
      "routeName": "System",
      "routePath": "/system",
      "component": "Layout",
      "visible": 1,
      "sort": 2,
      "icon": "ant-design:setting-outlined",
      "redirect": "/system/user",
      "alwaysShow": 1,
      "children": [
        {
          "id": 3,
          "parentId": 2,
          "name": "用户管理",
          "type": "menu",
          "routeName": "UserManagement",
          "routePath": "user",
          "component": "system/user/index",
          "visible": 1,
          "sort": 1,
          "icon": null,
          "children": []
        }
      ]
    }
  ]
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | long | 菜单ID |
| parentId | long | 父菜单ID，0表示顶级 |
| name | string | 菜单名称 |
| type | string | 菜单类型 |
| routeName | string | 路由名称（用于 keep-alive） |
| routePath | string | 路由路径 |
| component | string | 组件路径（前端项目内） |
| perm | string | 权限标识 |
| visible | integer | 是否显示：1-显示，0-隐藏 |
| sort | integer | 排序序号 |
| icon | string | 菜单图标 |
| redirect | string | 重定向地址 |
| alwaysShow | integer | 是否始终显示根菜单 |
| keepAlive | integer | 是否缓存 |
| params | object | 路由参数 |
| children | array | 子菜单列表 |

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40102 | 未登录或登录已过期 | 跳转登录页 |

---

## Token 刷新机制

**接口信息**
- 路径: `POST /api/auth/refresh`
- 鉴权: 否
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| refreshToken | string | 是 | 刷新令牌（登录时获取） |

**请求示例**

```javascript
// axios
axios.post('/api/auth/refresh', {
  refreshToken: localStorage.getItem('refreshToken')
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40108 | 无效的令牌 | 跳转登录页重新登录 |

---

## 退出登录

**接口信息**
- 路径: `POST /api/auth/logout`
- 鉴权: 是
- Content-Type: `application/json`

**请求参数**（可选）

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| accessToken | string | 否 | 访问令牌，不传时从 Authorization header 读取 |

**请求示例**

```javascript
// axios - 不传 token，自动从 header 获取
await axios.post('/api/auth/logout')

// axios - 显式传 token
await axios.post('/api/auth/logout', {
  accessToken: localStorage.getItem('accessToken')
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

## 个人中心

### 查看个人资料

**接口信息**
- 路径: `GET /api/user/profile`
- 鉴权: 是

**请求示例**

```javascript
// axios
const res = await axios.get('/api/user/profile')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "https://example.com/avatar.jpg",
    "bio": "这是个人简介",
    "website": "https://example.com",
    "gender": 1,
    "birthday": "1990-01-01",
    "email": "a***@example.com",
    "phone": "138****8000",
    "userLevel": 5,
    "experiencePoints": 15000,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | long | 用户ID |
| username | string | 用户名 |
| nickname | string | 昵称 |
| avatar | string | 头像URL |
| bio | string | 个人简介 |
| website | string | 个人站点 |
| gender | integer | 性别：0-未知，1-男，2-女，3-保密 |
| birthday | string | 生日，格式 `yyyy-MM-dd` |
| email | string | 邮箱（脱敏显示） |
| phone | string | 手机号（脱敏显示） |
| userLevel | integer | 用户等级 |
| experiencePoints | integer | 经验值 |
| createdAt | string | 注册时间 |

---

### 更新个人资料

**接口信息**
- 路径: `PUT /api/user/profile`
- 鉴权: 是
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 限制 |
|-----|------|------|------|------|
| nickname | string | 否 | 昵称 | 最多50字符 |
| avatar | string | 否 | 头像URL | 最多500字符 |
| bio | string | 否 | 个人简介 | 最多500字符 |
| website | string | 否 | 个人站点 | 合法HTTP/HTTPS URL，最多255字符 |
| gender | integer | 否 | 性别 | 0-未知，1-男，2-女，3-保密 |

**请求示例**

```javascript
// axios
await axios.put('/api/user/profile', {
  nickname: '新昵称',
  avatar: 'https://example.com/new-avatar.jpg',
  bio: '这是我的新简介',
  website: 'https://mysite.com',
  gender: 1
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "新昵称",
    "avatar": "https://example.com/new-avatar.jpg",
    "bio": "这是我的新简介",
    "website": "https://mysite.com",
    "gender": 1,
    "birthday": "1990-01-01",
    "email": "a***@example.com",
    "phone": "138****8000",
    "userLevel": 5,
    "experiencePoints": 15000,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40001 | 参数校验失败 | 显示具体字段错误 |
| 40133 | 昵称已被占用 | 显示「昵称已被占用」 |

---

### 修改密码

**接口信息**
- 路径: `PUT /api/user/profile/password`
- 鉴权: 是
- Content-Type: `application/json`

**请求参数**

| 字段 | 类型 | 必填 | 说明 | 限制 |
|-----|------|------|------|------|
| oldPassword | string | 是 | 原密码 | 不能为空 |
| newPassword | string | 是 | 新密码 | 8-64位，需包含大小写字母和数字 |

**请求示例**

```javascript
// axios
await axios.put('/api/user/profile/password', {
  oldPassword: 'OldPass123',
  newPassword: 'NewPass456'
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| 40001 | 参数校验失败 | 显示具体字段错误 |
| 40130 | 原密码错误 | 显示「原密码错误」 |
| 40131 | 新密码不能与原密码相同 | 显示对应提示 |

---

## 通知中心

### 通知列表

**接口信息**
- 路径: `GET /api/user/notices`
- 鉴权: 是
- 分页参数通过 Query 传递

**请求参数**（Query）

| 字段 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| current | integer | 否 | 当前页，默认1 | `1` |
| size | integer | 否 | 每页条数，默认10 | `10` |
| title | string | 否 | 标题（模糊搜索） | `系统` |
| isRead | integer | 否 | 已读状态：0-未读，1-已读 | `0` |

**请求示例**

```javascript
// axios
const res = await axios.get('/api/user/notices', {
  params: {
    current: 1,
    size: 10,
    isRead: 0
  }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 25,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "title": "系统通知",
        "content": "您的文章已审核通过",
        "type": 1,
        "level": "info",
        "publishTime": "2024-01-15T10:30:00",
        "isRead": 0,
        "readTime": null,
        "businessType": "article",
        "businessId": 123,
        "actionPath": "/article/123"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | long | 通知ID |
| title | string | 通知标题 |
| content | string | 通知内容 |
| type | integer | 通知类型 |
| level | string | 通知等级：`info`/`warning`/`error` |
| publishTime | string | 发布时间 |
| isRead | integer | 已读状态：0-未读，1-已读 |
| readTime | string | 阅读时间，未读为 null |
| businessType | string | 业务目标类型（如 article） |
| businessId | long | 业务目标ID |
| actionPath | string | 点击跳转路径 |

---

### 通知详情

**接口信息**
- 路径: `GET /api/user/notices/{id}`
- 鉴权: 是

**请求示例**

```javascript
// axios
const res = await axios.get('/api/user/notices/1')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "title": "系统通知",
    "content": "您的文章已审核通过",
    "type": 1,
    "level": "info",
    "publishTime": "2024-01-15T10:30:00",
    "isRead": 1,
    "readTime": "2024-01-15T11:00:00",
    "businessType": "article",
    "businessId": 123,
    "actionPath": "/article/123"
  }
}
```

---

### 未读数量

**接口信息**
- 路径: `GET /api/user/notices/unread-count`
- 鉴权: 是

**请求示例**

```javascript
// axios
const res = await axios.get('/api/user/notices/unread-count')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": 5
}
```

---

### 标记已读

**单条已读**

**接口信息**
- 路径: `POST /api/user/notices/{id}/read`
- 鉴权: 是

**请求示例**

```javascript
// axios
await axios.post('/api/user/notices/1/read')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

**全部已读**

**接口信息**
- 路径: `POST /api/user/notices/read-all`
- 鉴权: 是

**请求示例**

```javascript
// axios
await axios.post('/api/user/notices/read-all')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": null
}
```

---

## 错误码速查

### 认证相关（401xx）

| code | 说明 | 前端处理建议 |
|-----|------|-------------|
| 40101 | 用户名或密码错误 | 登录页显示「用户名或密码错误」 |
| 40102 | 未登录或登录已过期 | 跳转登录页 |
| 40104 | 账号已锁定 | 显示「连续失败过多，请15分钟后再试」 |
| 40105 | 账号已禁用 | 显示「账号已被禁用」 |
| 40108 | 无效的令牌 | 跳转登录页 |
| 40112 | 邮箱验证码错误 | 显示「验证码错误」 |
| 40113 | 邮箱验证码已过期 | 显示「验证码已过期，请重新获取」 |
| 40115 | 发送验证码过于频繁 | 显示「发送过于频繁，请稍后再试」 |
| 40130 | 原密码错误 | 修改密码页显示「原密码错误」 |

### 参数校验（400xx）

| code | 说明 | 前端处理建议 |
|-----|------|-------------|
| 40001 | 参数校验失败 | 显示具体字段的错误提示 |

### 权限相关（403xx）

| code | 说明 | 前端处理建议 |
|-----|------|-------------|
| 40300 | 没有访问权限 | 显示「无权限访问该资源」 |
| 40304 | 仅超级管理员可执行此操作 | 显示「需要管理员权限」 |

---

## 前端集成指南

### 登录流程时序

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│   登录页    │     │   后端 API   │     │   前端存储    │
└──────┬──────┘     └──────┬───────┘     └───────┬───────┘
       │                    │                     │
       │  1. POST /login    │                     │
       │  username, password│                     │
       │───────────────────>│                     │
       │                    │                     │
       │  200 { accessToken,│                     │
       │        refreshToken,│                     │
       │        expiresIn } │                     │
       │<───────────────────│                     │
       │                    │                     │
       │                    │         ┌───────────┴───┐
       │                    │         │ accessToken   │
       │                    │         │ refreshToken  │
       │                    │         │ expiresIn     │
       │                    │         └───────────────┘
       │                    │                     │
       │  2. GET /current-user│                    │
       │───────────────────>│                     │
       │  200 { user info } │                     │
       │<───────────────────│                     │
       │                    │                     │
       │  3. GET /current-user-menus            │
       │───────────────────>│                     │
       │  200 [ menus ]     │                     │
       │<───────────────────│                     │
       │                    │                     │
       ▼                    ▼                     ▼
```

### axios 拦截器配置

```javascript
// main.js 或独立的 api 配置文件中

// 假设使用 localStorage 存储 token
const getToken = () => localStorage.getItem('accessToken')
const getRefreshToken = () => localStorage.getItem('refreshToken')

// 请求拦截器：自动附加 token
axios.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理 token 过期
let isRefreshing = false
let refreshQueue = []

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // token 过期，尝试刷新
    if (error.response?.status === 401 && 
        error.response?.data?.code === 40108 && 
        !originalRequest._retry) {
      
      if (isRefreshing) {
        // 正在刷新，把请求加入队列
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return axios(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = getRefreshToken()
        if (!refreshToken) throw new Error('No refresh token')

        const res = await axios.post('/api/auth/refresh', {
          refreshToken
        })

        const { accessToken, refreshToken: newRefresh } = res.data.data
        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', newRefresh)

        // 重试排队的请求
        refreshQueue.forEach(({ resolve }) => resolve(accessToken))
        refreshQueue = []

        // 重试当前请求
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return axios(originalRequest)

      } catch (refreshError) {
        // 刷新失败，清除 token 跳转登录
        refreshQueue.forEach(({ reject }) => reject(refreshError))
        refreshQueue = []
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)
```

### Token 存储建议

| 存储方式 | 优点 | 缺点 | 建议场景 |
|---------|------|------|---------|
| localStorage | 简单，跨标签页共享 | 易受 XSS 攻击 | 非敏感应用 |
| sessionStorage | 标签页关闭即清除 | 不跨标签页 | 需要严格安全 |
| cookies (HttpOnly) | 可防 XSS，服务端控制 | 跨域配置复杂 | 高安全要求 |

**最低安全建议**：
1. accessToken 存储在 localStorage，用于接口鉴权
2. refreshToken 可以存储在 cookie（HttpOnly）或较安全的存储
3. 敏感操作（如修改密码）要求用户重新输入密码
4. 退出登录时清除所有 token
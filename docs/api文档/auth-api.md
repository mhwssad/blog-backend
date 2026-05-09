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
- [密码重置（忘记密码）](#密码重置忘记密码)
  - [发送重置验证码](#发送重置验证码)
  - [重置密码](#重置密码)
- [用户搜索（公开）](#用户搜索公开)
- [接管登录](#接管登录)
- [经验体系后台管理](#经验体系后台管理)
- [审计日志后台管理](#审计日志后台管理)
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

---

## 密码重置（忘记密码）

### 发送重置验证码

**接口信息**

- 路径: `POST /api/auth/password-reset/code`
- 鉴权: 无（公开接口）
- 说明: 向邮箱发送密码重置验证码

**请求示例**

```javascript
axios.post('/api/auth/password-reset/code', {
  email: 'user@example.com'
})
```

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `email` | String | 是 | 邮箱地址 |

---

### 重置密码

**接口信息**

- 路径: `POST /api/auth/password-reset`
- 鉴权: 无（公开接口）
- 说明: 使用验证码重置密码

**请求示例**

```javascript
axios.post('/api/auth/password-reset', {
  email: 'user@example.com',
  code: '123456',
  newPassword: 'NewPass123'
})
```

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `email` | String | 是 | 邮箱地址 |
| `code` | String | 是 | 验证码 |
| `newPassword` | String | 是 | 新密码，8-64 位 |

---

## 用户搜索（公开）

### 搜索用户

**接口信息**

- 路径: `GET /api/users/search`
- 鉴权: 无（公开接口）
- 说明: 根据关键词搜索用户

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `keyword` | String | 否 | 搜索关键词 |
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 用户ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像 URL |
| `bio` | String | 个人简介 |

---

## 接管登录

### 使用接管令牌登录

**接口信息**

- 路径: `POST /api/auth/takeover/login`
- 鉴权: 无（通过接管令牌认证）
- 说明: 使用超管通过 `POST /api/admin/takeover` 获取的接管令牌登录为目标用户

**请求示例**

```javascript
axios.post('/api/auth/takeover/login', {
  takeoverToken: 'xxx'
})
```

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `takeoverToken` | String | 是 | 接管令牌（由超管通过 `/api/admin/takeover` 获取） |

**响应字段说明**: 返回标准 `AuthenticationToken`（accessToken + refreshToken），与普通登录一致。

---

## 经验体系后台管理

### 查询用户经验汇总

**接口信息**

- 路径: `GET /api/sys/experience/users/{userId}/summary`
- 鉴权: `sys:experience:query`
- 说明: 查询指定用户的经验等级与各来源汇总

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `userId` | Long | 用户ID |
| `level` | Integer | 当前等级 |
| `title` | String | 等级称号 |
| `experiencePoints` | Integer | 当前经验值 |
| `todayXp` | Integer | 今日已获得经验 |
| `dailyLoginXp` | Integer | 登录经验总计 |
| `articlePublishXp` | Integer | 发文经验总计 |
| `commentCreateXp` | Integer | 评论经验总计 |
| `likeGivenXp` | Integer | 点赞经验总计 |
| `likeReceivedXp` | Integer | 被点赞经验总计 |
| `chatMessageXp` | Integer | 聊天经验总计 |

---

### 分页查询经验流水

**接口信息**

- 路径: `GET /api/sys/experience/logs`
- 鉴权: `sys:experience:query`

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `userId` | Long | 否 | 用户ID |
| `sourceType` | String | 否 | 经验来源类型 |
| `startDate` | Date | 否 | 开始日期 |
| `endDate` | Date | 否 | 结束日期 |

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 记录ID |
| `userId` | Long | 用户ID |
| `sourceType` | String | 经验来源类型 |
| `sourceBizId` | String | 来源业务ID |
| `xpValue` | Integer | 经验值 |
| `logDate` | Date | 入账日期 |
| `createdAt` | DateTime | 创建时间 |

---

### 调整用户等级/经验

**接口信息**

- 路径: `POST /api/sys/experience/users/{userId}/adjust`
- 鉴权: `sys:experience:adjust`

**请求体字段说明**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `adjustType` | String | 是 | 调整类型：`level` 或 `experience` |
| `value` | Integer | 是 | 调整值（设置等级或增减经验，经验支持负数） |
| `reason` | String | 否 | 调整原因 |

---

### 查询经验来源配置

**接口信息**

- 路径: `GET /api/sys/experience/config`
- 鉴权: `sys:experience:config`
- 说明: 查询所有经验来源配置项（返回列表，非分页）

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `configKey` | String | 配置键 |
| `configName` | String | 配置名称 |
| `configValue` | String | 配置值 |
| `remark` | String | 备注 |

---

### 更新经验来源配置

**接口信息**

- 路径: `PUT /api/sys/experience/config`
- 鉴权: `sys:experience:config`

---

## 审计日志后台管理

> 审计日志仅超级管理员可查看，且操作本身不记录系统日志。

### 分页查询审计日志

**接口信息**

- 路径: `GET /api/sys/audit-logs`
- 鉴权: `sys:audit:query` + 超级管理员

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10` |
| `operatorUserId` | Long | 否 | 操作人ID |
| `targetUserId` | Long | 否 | 目标用户ID |
| `operationType` | String | 否 | 操作类型 |

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `id` | Long | 主键 |
| `operatorUserId` | Long | 操作人ID |
| `operatorUsername` | String | 操作人用户名 |
| `targetUserId` | Long | 目标用户ID |
| `targetUsername` | String | 目标用户名 |
| `operationType` | String | 操作类型 |
| `operationTypeDesc` | String | 操作类型描述 |
| `targetTypeName` | String | 目标对象类型名称 |
| `targetId` | Long | 目标对象ID |
| `beforeState` | String | 操作前状态 |
| `afterState` | String | 操作后状态 |
| `mfaPassed` | Integer | 2FA 是否通过 |
| `requestIp` | String | 请求 IP |
| `userAgent` | String | User-Agent |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |

---

### 查询审计日志详情

**接口信息**

- 路径: `GET /api/sys/audit-logs/{id}`
- 鉴权: `sys:audit:query` + 超级管理员

---

## 后台用户管理

> 路径前缀：`/api/sys/users`，需要对应权限 + 管理员角色。

### 分页查询用户

- 路径: `GET /api/sys/users`
- 鉴权: `sys:user:query`
- 说明: 分页查询用户列表

### 查询用户详情

- 路径: `GET /api/sys/users/{id}`
- 鉴权: `sys:user:query`

### 创建用户

- 路径: `POST /api/sys/users`
- 鉴权: `sys:user:create`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码，8-64 位，需含大小写字母和数字 |
| `nickname` | String | 否 | 昵称 |
| `email` | String | 否 | 邮箱（@Email 格式校验） |
| `phone` | String | 否 | 手机号 |
| `avatar` | String | 否 | 头像 |
| `gender` | Integer | 否 | 性别 |
| `birthday` | LocalDate | 否 | 生日 |
| `status` | Integer | 否 | 状态 |
| `remark` | String | 否 | 备注 |

### 更新用户

- 路径: `PUT /api/sys/users/{id}`
- 鉴权: `sys:user:update`
- 请求体: 同创建用户

### 更新用户状态

- 路径: `PUT /api/sys/users/{id}/status`
- 鉴权: `sys:user:update`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `status` | Integer | 是 | 用户状态 |

### 重置用户密码

- 路径: `PUT /api/sys/users/{id}/password/reset`
- 鉴权: `sys:user:reset-password`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `password` | String | 是 | 新密码，8-64 位，需含大小写字母和数字 |

### 删除用户

- 路径: `DELETE /api/sys/users/{id}`
- 鉴权: `sys:user:delete`

### 查询用户角色

- 路径: `GET /api/sys/users/{id}/roles`
- 鉴权: `sys:user:query`
- 响应: `List<Long>`（角色 ID 列表）

### 分配用户角色

- 路径: `PUT /api/sys/users/{id}/roles`
- 鉴权: `sys:user:assign-role`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `roleIds` | List\<Long\> | 是 | 角色 ID 列表 |

---

## 超级管理员操作

> 路径前缀：`/api/admin`，敏感操作需 MFA 验证。

### 发送 MFA 验证码

- 路径: `POST /api/admin/2fa/send-code`
- 鉴权: `sys:user:update`
- 说明: 发送 MFA 验证码，用于后续敏感操作

### 验证 MFA 验证码

- 路径: `POST /api/admin/2fa/verify`
- 鉴权: `sys:user:update`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `code` | String | 是 | MFA 验证码 |

**响应字段**

| 字段 | 类型 | 说明 |
|-----|------|------|
| `ticket` | String | MFA 凭证（用于后续操作） |
| `expiresAt` | LocalDateTime | 凭证过期时间 |

### 封禁用户

- 路径: `POST /api/admin/users/{id}/ban`
- 鉴权: `sys:user:ban`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `status` | Integer | 是 | 0-解封，1-封禁 |
| `mfaTicket` | String | 是 | MFA 凭证 |

### 解封用户

- 路径: `POST /api/admin/users/{id}/unban`
- 鉴权: `sys:user:unban`
- 请求体: 同封禁用户

### 调整用户等级

- 路径: `PUT /api/admin/users/{id}/level`
- 鉴权: `sys:user:adjust-level`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `level` | Integer | 是 | 目标等级 |
| `mfaTicket` | String | 是 | MFA 凭证 |

### 调整用户经验

- 路径: `PUT /api/admin/users/{id}/experience`
- 鉴权: `sys:user:adjust-experience`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `experience` | Integer | 是 | 经验值 |
| `mfaTicket` | String | 是 | MFA 凭证 |

### 接管登录

- 路径: `POST /api/admin/takeover`
- 鉴权: `sys:user:takeover`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `targetUserId` | Long | 是 | 目标用户 ID |
| `mfaTicket` | String | 是 | MFA 凭证 |

**响应字段**

| 字段 | 类型 | 说明 |
|-----|------|------|
| `takeoverToken` | String | 接管令牌 |
| `targetUserId` | Long | 目标用户 ID |
| `targetUsername` | String | 目标用户名 |
| `expiresIn` | Long | 有效时长（秒） |

### 分配角色（带审计）

- 路径: `PUT /api/admin/users/{id}/roles`
- 鉴权: `sys:user:assign-role`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `roleIds` | List\<Long\> | 是 | 角色 ID 列表 |
| `mfaTicket` | String | 是 | MFA 凭证 |

---

## 公开作者资料

> 路径前缀：`/api/users/{userId}`，无需登录。

### 查询作者资料

- 路径: `GET /api/users/{userId}/author-profile`
- 鉴权: 公开接口

---

## 用户作者申请

> 路径前缀：`/api/user/author-applications`，需登录。

### 提交作者申请

- 路径: `POST /api/user/author-applications`
- 鉴权: 需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `applyReason` | String | 是 | 申请理由，最多 512 字 |
| `contentDirection` | String | 是 | 内容方向，最多 128 字 |
| `introduction` | String | 否 | 自我介绍，最多 1024 字 |
| `sampleLinks` | List\<String\> | 否 | 作品链接，最多 10 个 |

### 查询最新申请

- 路径: `GET /api/user/author-applications/latest`
- 鉴权: 需登录

### 分页查询我的申请

- 路径: `GET /api/user/author-applications`
- 鉴权: 需登录
- 说明: 分页查询当前用户的作者申请记录

---

## 作者申请审核后台

> 路径前缀：`/api/sys/author-applications`，需要对应权限。

### 分页查询申请

- 路径: `GET /api/sys/author-applications`
- 鉴权: `sys:author-application:query`
- 说明: 分页查询作者申请列表

### 查询申请详情

- 路径: `GET /api/sys/author-applications/{id}`
- 鉴权: `sys:author-application:query`

### 审核申请

- 路径: `PUT /api/sys/author-applications/{id}/review`
- 鉴权: `sys:author-application:review`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `reviewStatus` | Integer | 是 | 审核状态：1-通过，2-拒绝，3-待补充 |
| `reviewComment` | String | 否 | 审核意见，最多 512 字 |

### 修复申请状态

- 路径: `PUT /api/sys/author-applications/{id}/repair`
- 鉴权: `sys:author-application:repair`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `targetStatus` | Integer | 是 | 目标状态：0-待审核，1-已通过，2-已拒绝，3-待补充 |
| `reviewComment` | String | 否 | 修复意见，最多 512 字 |

---

## 系统配置管理

> 路径前缀：`/api/sys/configs`，需要对应权限。

### 分页查询配置

- 路径: `GET /api/sys/configs`
- 鉴权: `sys:config:query`
- 说明: 分页查询系统配置列表

### 查询配置详情

- 路径: `GET /api/sys/configs/{id}`
- 鉴权: `sys:config:query`

### 创建配置

- 路径: `POST /api/sys/configs`
- 鉴权: `sys:config:create`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `configName` | String | 是 | 配置名称 |
| `configKey` | String | 是 | 配置键 |
| `configValue` | String | 是 | 配置值 |
| `remark` | String | 否 | 备注 |

### 更新配置

- 路径: `PUT /api/sys/configs/{id}`
- 鉴权: `sys:config:update`
- 请求体: 同创建配置

### 删除配置

- 路径: `DELETE /api/sys/configs/{id}`
- 鉴权: `sys:config:delete`

### 按键查询配置值

- 路径: `GET /api/sys/configs/key/{configKey}`
- 鉴权: `sys:config:query`
- 响应: `String`

---

## 菜单管理

> 路径前缀：`/api/sys/menus`，需要对应权限。

### 查询菜单树

- 路径: `GET /api/sys/menus/tree`
- 鉴权: `sys:menu:query`
- 响应: 树形结构

### 查询菜单详情

- 路径: `GET /api/sys/menus/{id}`
- 鉴权: `sys:menu:query`

### 创建菜单

- 路径: `POST /api/sys/menus`
- 鉴权: `sys:menu:create`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `parentId` | Long | 是 | 父菜单 ID |
| `name` | String | 是 | 菜单名称 |
| `type` | String | 是 | 菜单类型 |
| `treePath` | String | 否 | 树路径 |
| `routeName` | String | 否 | 路由名称 |
| `routePath` | String | 否 | 路由路径 |
| `component` | String | 否 | 前端组件路径 |
| `perm` | String | 否 | 权限标识 |
| `alwaysShow` | Integer | 否 | 是否始终显示 |
| `keepAlive` | Integer | 否 | 是否缓存 |
| `visible` | Integer | 否 | 是否可见 |
| `sort` | Integer | 否 | 排序值 |
| `icon` | String | 否 | 图标 |
| `redirect` | String | 否 | 重定向地址 |
| `params` | Object | 否 | 路由参数 |

### 更新菜单

- 路径: `PUT /api/sys/menus/{id}`
- 鉴权: `sys:menu:update`
- 请求体: 同创建菜单

### 删除菜单

- 路径: `DELETE /api/sys/menus/{id}`
- 鉴权: `sys:menu:delete`

---

## 角色管理

> 路径前缀：`/api/sys/roles`，需要对应权限。

### 分页查询角色

- 路径: `GET /api/sys/roles`
- 鉴权: `sys:role:query`
- 说明: 分页查询角色列表

### 查询角色详情

- 路径: `GET /api/sys/roles/{id}`
- 鉴权: `sys:role:query`

### 创建角色

- 路径: `POST /api/sys/roles`
- 鉴权: `sys:role:create`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `name` | String | 是 | 角色名称 |
| `code` | String | 是 | 角色编码 |
| `sort` | Integer | 否 | 排序值 |
| `status` | Integer | 否 | 状态 |
| `dataScope` | Integer | 否 | 数据权限范围 |

### 更新角色

- 路径: `PUT /api/sys/roles/{id}`
- 鉴权: `sys:role:update`
- 请求体: 同创建角色

### 更新角色状态

- 路径: `PUT /api/sys/roles/{id}/status`
- 鉴权: `sys:role:update`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `status` | Integer | 是 | 角色状态 |

### 删除角色

- 路径: `DELETE /api/sys/roles/{id}`
- 鉴权: `sys:role:delete`

### 查询角色菜单

- 路径: `GET /api/sys/roles/{id}/menus`
- 鉴权: `sys:role:query`
- 响应: `List<Long>`（菜单 ID 列表）

### 分配角色菜单

- 路径: `PUT /api/sys/roles/{id}/menus`
- 鉴权: `sys:role:assign-menu`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `menuIds` | List\<Long\> | 是 | 菜单 ID 列表 |

---

## 通知后台管理

> 路径前缀：`/api/sys/notices`，需要对应权限。

### 分页查询通知

- 路径: `GET /api/sys/notices`
- 鉴权: `sys:notice:query`
- 说明: 分页查询通知列表

### 查询通知详情

- 路径: `GET /api/sys/notices/{id}`
- 鉴权: `sys:notice:query`

### 创建通知

- 路径: `POST /api/sys/notices`
- 鉴权: `sys:notice:create`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `title` | String | 是 | 标题 |
| `content` | String | 是 | 内容 |
| `type` | Integer | 是 | 通知类型 |
| `level` | String | 是 | 通知级别 |
| `targetType` | Integer | 是 | 目标类型：1-全体，2-指定用户 |
| `targetUserIds` | List\<Long\> | 否 | 目标用户 ID 列表（targetType=2 时必填） |

### 更新通知

- 路径: `PUT /api/sys/notices/{id}`
- 鉴权: `sys:notice:update`
- 请求体: 同创建通知

### 发布通知

- 路径: `POST /api/sys/notices/{id}/publish`
- 鉴权: `sys:notice:publish`

### 撤回通知

- 路径: `POST /api/sys/notices/{id}/revoke`
- 鉴权: `sys:notice:revoke`

### 删除通知

- 路径: `DELETE /api/sys/notices/{id}`
- 鉴权: `sys:notice:delete`

---

## 用户通知设置

> 路径前缀：`/api/user/notification-settings`，需登录。

### 查询我的通知设置

- 路径: `GET /api/user/notification-settings`
- 鉴权: 需登录

### 批量更新通知设置

- 路径: `PUT /api/user/notification-settings`
- 鉴权: 需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `settings` | List | 是 | 通知设置列表 |

**settings 每项字段**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `type` | String | 是 | 通知类型 |
| `enabled` | Boolean | 是 | 是否启用 |

### 更新单项通知设置

- 路径: `PUT /api/user/notification-settings/{type}`
- 鉴权: 需登录

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `enabled` | Boolean | 是 | 是否启用 |

---

## 用户等级信息

> 路径前缀：`/api/user/experience`，需登录。

### 查询我的等级信息

- 路径: `GET /api/user/experience/level`
- 鉴权: 需登录

**响应字段**

| 字段 | 类型 | 说明 |
|-----|------|------|
| `level` | Integer | 当前等级 |
| `title` | String | 等级称号 |
| `experiencePoints` | Integer | 当前经验值 |
| `nextLevelThreshold` | Integer | 下一级所需经验 |
| `currentLevelThreshold` | Integer | 当前级起始经验 |

---

## 系统日志管理

> 路径前缀：`/api/sys/logs`，需要对应权限。所有接口标注 `@DisableSysLog`，操作本身不记录日志。

### 分页查询日志

- 路径: `GET /api/sys/logs`
- 鉴权: `sys:log:query`
- 说明: 分页查询系统日志，支持以下筛选参数

**查询参数说明**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `module` | String | 否 | 模块名 |
| `requestMethod` | String | 否 | 请求方法 |
| `requestUri` | String | 否 | 请求 URI |
| `ip` | String | 否 | 请求 IP |
| `createBy` | String | 否 | 操作人 |
| `createTimeStart` | DateTime | 否 | 创建时间起始 |
| `createTimeEnd` | DateTime | 否 | 创建时间截止 |

### 查询日志详情

- 路径: `GET /api/sys/logs/{id}`
- 鉴权: `sys:log:query`

### 删除日志

- 路径: `DELETE /api/sys/logs/{id}`
- 鉴权: `sys:log:delete`

### 批量清理日志

- 路径: `POST /api/sys/logs/clean`
- 鉴权: `sys:log:clean`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|------|------|
| `module` | String | 否 | 模块名 |
| `requestMethod` | String | 否 | 请求方法 |
| `requestUri` | String | 否 | 请求 URI |
| `ip` | String | 否 | 请求 IP |
| `createBy` | String | 否 | 操作人 |
| `createTimeStart` | DateTime | 否 | 创建时间起始 |
| `createTimeEnd` | DateTime | 否 | 创建时间截止 |

**响应**: 清理数量 `Long`
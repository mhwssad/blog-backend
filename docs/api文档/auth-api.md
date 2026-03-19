# Auth模块接口文档

## 基础信息

- **Base URL**: `/api`
- **认证方式**: Bearer Token (JWT)
- **响应格式**: JSON
- **文档覆盖范围**:
  - 认证接口：`/api/auth/**`
  - 后台用户、角色、菜单、配置、通知、日志接口：`/api/sys/**`
  - 用户通知中心：`/api/user/notices/**`

## 通用响应结构

所有接口响应遵循以下格式：

```json
{
  "code": 200,
  "data": {},
  "message": "成功",
  "timestamp": 1742371200000
}
```

- `code`: 状态码，200表示成功
- `data`: 响应数据
- `message`: 响应消息，默认成功文案为 `成功`
- `timestamp`: 服务端返回时间戳（毫秒）

分页查询响应：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1742371200000,
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": []
  }
}
```

- 分页结构对应 [`PageResult`](/e:/project/blog/blog-backend/src/main/java/com/cybzacg/blogbackend/core/web/PageResult.java)，固定字段为 `total`、`current`、`size`、`records`，没有 `pages` 字段

---

## 1. 认证管理 (/api/auth)

### 1.1 账号登录

**接口描述**: 使用用户名/邮箱/手机号和密码进行登录

**请求方式**: `POST /api/auth/login`

**请求参数**:

| 参数名      | 类型     | 必填 | 说明                | 示例     |
|----------|--------|----|-------------------|--------|
| username | String | 是  | 登录账号，支持用户名/邮箱/手机号 | admin  |
| password | String | 是  | 密码                | 123456 |

**请求示例**:

```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应数据** (AuthenticationToken):

| 参数名          | 类型      | 说明             |
|--------------|---------|----------------|
| tokenType    | String  | 令牌类型，如"Bearer" |
| accessToken  | String  | 访问令牌           |
| refreshToken | String  | 刷新令牌           |
| expiresIn    | Integer | 过期时间(单位：秒)     |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

---

### 1.2 账号注册

**接口描述**: 使用用户名和密码注册新账号

**请求方式**: `POST /api/auth/register`

**请求参数**:

| 参数名      | 类型     | 必填 | 说明    | 示例            |
|----------|--------|----|-------|---------------|
| username | String | 是  | 用户名   | new_user      |
| password | String | 是  | 密码    | 123456        |
| nickname | String | 否  | 昵称    | 新用户           |
| email    | String | 否  | 邮箱地址  | user@example.com |
| phone    | String | 否  | 手机号   | 13800138000    |

**请求示例**:

```json
{
  "username": "new_user",
  "password": "123456",
  "nickname": "新用户",
  "email": "user@example.com"
}
```

**响应数据**: 同"账号登录"接口

---

### 1.3 发送邮箱登录验证码

**接口描述**: 向指定邮箱发送登录验证码

**请求方式**: `POST /api/auth/email-code`

**请求参数**:

| 参数名   | 类型     | 必填 | 说明   | 示例               |
|-------|--------|----|------|------------------|
| email | String | 是  | 邮箱地址 | user@example.com |

**请求示例**:

```json
{
  "email": "user@example.com"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

---

### 1.4 邮箱验证码登录

**接口描述**: 使用邮箱和验证码进行登录

**请求方式**: `POST /api/auth/email-login`

**请求参数**:

| 参数名   | 类型     | 必填 | 说明   | 示例               |
|-------|--------|----|------|------------------|
| email | String | 是  | 邮箱地址 | user@example.com |
| code  | String | 是  | 验证码  | 123456           |

**请求示例**:

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**响应数据**: 同"账号登录"接口

---

### 1.5 刷新令牌

**接口描述**: 使用刷新令牌获取新的访问令牌

**请求方式**: `POST /api/auth/refresh`

**请求参数**:

| 参数名          | 类型     | 必填 | 说明   | 示例          |
|--------------|--------|----|------|-------------|
| refreshToken | String | 是  | 刷新令牌 | eyJhbGci... |

**请求示例**:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应数据**: 同"账号登录"接口

---

### 1.6 退出登录

**接口描述**: 退出当前登录状态，使令牌失效

**请求方式**: `POST /api/auth/logout`

**请求头**:

| 参数名           | 类型     | 必填 | 说明       | 示例                 |
|---------------|--------|----|----------|--------------------|
| Authorization | String | 是  | Bearer令牌 | Bearer eyJhbGci... |

**请求参数** (Body):

| 参数名         | 类型     | 必填 | 说明                  | 示例          |
|-------------|--------|----|---------------------|-------------|
| accessToken | String | 否  | 访问令牌（如不传则从Header获取） | eyJhbGci... |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

---

### 1.7 获取当前登录用户

**接口描述**: 获取当前登录用户的详细信息

**请求方式**: `GET /api/auth/current-user`

**请求头**: 需要Bearer令牌

**响应数据** (AuthUserInfo):

| 参数名         | 类型           | 说明     |
|-------------|--------------|--------|
| id          | Long         | 用户ID   |
| username    | String       | 用户名    |
| nickname    | String       | 昵称     |
| avatar      | String       | 头像URL  |
| email       | String       | 邮箱     |
| phone       | String       | 手机号    |
| status      | Integer      | 状态     |
| roles       | List[String] | 角色编码列表 |
| permissions | List[String] | 权限标识列表 |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "https://example.com/avatar.jpg",
    "email": "admin@example.com",
    "phone": "13800138000",
    "status": 1,
    "roles": [
      "ROLE_ADMIN"
    ],
    "permissions": [
      "sys:user:create",
      "sys:user:update"
    ]
  }
}
```

---

### 1.8 获取当前用户菜单

**接口描述**: 获取当前用户有权访问的菜单列表

**请求方式**: `GET /api/auth/current-user-menus`

**请求头**: 需要Bearer令牌

**响应数据** (List[AuthMenuInfo]):

| 参数名       | 类型      | 说明             |
|-----------|---------|----------------|
| id        | Long    | 菜单ID           |
| parentId  | Long    | 父菜单ID          |
| routeName | String  | 路由名称          |
| routePath | String  | 路由路径          |
| name      | String  | 菜单名称           |
| component | String  | 组件路径           |
| perm      | String  | 权限标识          |
| redirect  | String  | 跳转地址          |
| alwaysShow | Integer | 始终显示         |
| keepAlive | Integer | 页面缓存          |
| icon      | String  | 图标             |
| type      | String  | 类型：`C`目录 `M`菜单 `B`按钮 |
| sort      | Integer | 排序号            |
| visible   | Integer | 是否可见           |
| params    | Object  | 路由参数          |
| children  | List    | 子菜单            |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "系统管理",
      "routeName": "System",
      "routePath": "/system",
      "component": "layouts/RouteView",
      "perm": null,
      "redirect": "/system/users",
      "alwaysShow": 1,
      "keepAlive": 0,
      "icon": "system",
      "type": "C",
      "sort": 1,
      "visible": 1,
      "params": null,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "name": "用户管理",
          "routeName": "SysUser",
          "routePath": "/system/users",
          "component": "system/users/index",
          "perm": "sys:user:query",
          "redirect": null,
          "alwaysShow": 0,
          "keepAlive": 1,
          "icon": "user",
          "type": "M",
          "sort": 1,
          "visible": 1,
          "params": null
        }
      ]
    }
  ]
}
```

---

## 2. 后台用户管理 (/api/sys/users)

> 需要相应权限才能访问

### 2.1 分页查询用户

**接口描述**: 分页查询系统用户列表

**请求方式**: `GET /api/sys/users`

**权限**: `sys:user:query`

**请求参数** (Query):

| 参数名      | 类型      | 必填 | 说明        | 示例           |
|----------|---------|----|-----------|--------------|
| current  | Integer | 否  | 当前页，默认1   | 1            |
| size     | Integer | 否  | 每页数量，默认10 | 10           |
| username | String  | 否  | 用户名（模糊查询） | admin        |
| nickname | String  | 否  | 昵称（模糊查询）  | 管理员          |
| email    | String  | 否  | 邮箱（模糊查询）  | @example.com |
| status   | Integer | 否  | 状态        | 1            |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "total": 100,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "email": "admin@example.com",
        "phone": "13800138000",
        "avatar": "https://example.com/avatar.jpg",
        "gender": 1,
        "status": 1,
        "createTime": "2024-01-01T00:00:00",
        "remark": "系统管理员"
      }
    ]
  }
}
```

---

### 2.2 查询用户详情

**接口描述**: 根据ID查询用户详细信息

**请求方式**: `GET /api/sys/users/{id}`

**权限**: `sys:user:query`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**响应数据** (SysUserAdminVO):

| 参数名        | 类型      | 说明   |
|------------|---------|------|
| id         | Long    | 用户ID |
| username   | String  | 用户名  |
| nickname   | String  | 昵称   |
| email      | String  | 邮箱   |
| phone      | String  | 手机号  |
| avatar     | String  | 头像   |
| gender     | Integer | 性别   |
| birthday   | Date    | 生日   |
| status     | Integer | 状态   |
| remark     | String  | 备注   |
| createTime | Date    | 创建时间 |
| updateTime | Date    | 更新时间 |

---

### 2.3 新增用户

**接口描述**: 创建新用户

**请求方式**: `POST /api/sys/users`

**权限**: `sys:user:create`

**请求参数** (SysUserSaveRequest):

| 参数名      | 类型      | 必填 | 说明    | 示例               |
|----------|---------|----|-------|------------------|
| username | String  | 是  | 用户名   | testuser         |
| password | String  | 否  | 密码    | 123456           |
| nickname | String  | 否  | 昵称    | 测试用户             |
| email    | String  | 否  | 邮箱    | test@example.com |
| phone    | String  | 否  | 手机号   | 13900139000      |
| avatar   | String  | 否  | 头像URL | https://...      |
| gender   | Integer | 否  | 性别    | 1                |
| birthday | Date    | 否  | 生日    | 1990-01-01       |
| status   | Integer | 否  | 状态    | 1                |
| remark   | String  | 否  | 备注    | 测试账号             |

**请求示例**:

```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户",
  "email": "test@example.com"
}
```

---

### 2.4 修改用户

**接口描述**: 更新用户信息

**请求方式**: `PUT /api/sys/users/{id}`

**权限**: `sys:user:update`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**请求参数**: 同"新增用户"

---

### 2.5 修改用户状态

**接口描述**: 启用/禁用用户

**请求方式**: `PUT /api/sys/users/{id}/status`

**权限**: `sys:user:update`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**请求参数** (StatusUpdateRequest):

| 参数名    | 类型      | 必填 | 说明         | 示例 |
|--------|---------|----|------------|----|
| status | Integer | 是  | 状态：0禁用 1启用 | 1  |

**请求示例**:

```json
{
  "status": 1
}
```

---

### 2.6 重置用户密码

**接口描述**: 重置指定用户的密码

**请求方式**: `PUT /api/sys/users/{id}/password/reset`

**权限**: `sys:user:reset-password`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**请求参数** (PasswordResetRequest):

| 参数名      | 类型     | 必填 | 说明  | 示例     |
|----------|--------|----|-----|--------|
| password | String | 是  | 新密码 | 123456 |

---

### 2.7 删除用户

**接口描述**: 删除指定用户

**请求方式**: `DELETE /api/sys/users/{id}`

**权限**: `sys:user:delete`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

---

### 2.8 查询用户角色

**接口描述**: 获取用户已分配的角色ID列表

**请求方式**: `GET /api/sys/users/{id}/roles`

**权限**: `sys:user:query`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    1,
    2,
    3
  ]
}
```

---

### 2.9 分配用户角色

**接口描述**: 为用户分配角色

**请求方式**: `PUT /api/sys/users/{id}/roles`

**权限**: `sys:user:assign-role`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 用户ID | 1  |

**请求参数** (UserRoleAssignRequest):

| 参数名     | 类型         | 必填 | 说明     | 示例     |
|---------|------------|----|--------|--------|
| roleIds | List[Long] | 是  | 角色ID列表 | [1, 2] |

**请求示例**:

```json
{
  "roleIds": [
    1,
    2
  ]
}
```

---

## 3. 后台角色管理 (/api/sys/roles)

> 需要相应权限才能访问

### 3.1 分页查询角色

**接口描述**: 分页查询系统角色列表

**请求方式**: `GET /api/sys/roles`

**权限**: `sys:role:query`

**请求参数** (Query):

| 参数名     | 类型      | 必填 | 说明         | 示例    |
|---------|---------|----|------------|-------|
| current | Integer | 否  | 当前页，默认1    | 1     |
| size    | Integer | 否  | 每页数量，默认10  | 10    |
| name    | String  | 否  | 角色名称（模糊查询） | 管理员   |
| code    | String  | 否  | 角色编码（模糊查询） | ADMIN |
| status  | Integer | 否  | 状态         | 1     |

**响应数据** (PageResult[SysRoleAdminVO]):

| 参数名        | 类型      | 说明   |
|------------|---------|------|
| id         | Long    | 角色ID |
| name       | String  | 角色名称 |
| code       | String  | 角色编码 |
| sort       | Integer | 排序号  |
| status     | Integer | 状态   |
| remark     | String  | 备注   |
| createTime | Date    | 创建时间 |

---

### 3.2 查询角色详情

**接口描述**: 根据ID查询角色详细信息

**请求方式**: `GET /api/sys/roles/{id}`

**权限**: `sys:role:query`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

---

### 3.3 新增角色

**接口描述**: 创建新角色

**请求方式**: `POST /api/sys/roles`

**权限**: `sys:role:create`

**请求参数** (SysRoleSaveRequest):

| 参数名    | 类型      | 必填 | 说明   | 示例     |
|--------|---------|----|------|--------|
| name   | String  | 是  | 角色名称 | 编辑     |
| code   | String  | 是  | 角色编码 | EDITOR |
| sort   | Integer | 否  | 排序号  | 1      |
| status | Integer | 否  | 状态   | 1      |
| remark | String  | 否  | 备注   | 内容编辑角色 |

**请求示例**:

```json
{
  "name": "编辑",
  "code": "EDITOR",
  "sort": 1,
  "status": 1
}
```

---

### 3.4 修改角色

**接口描述**: 更新角色信息

**请求方式**: `PUT /api/sys/roles/{id}`

**权限**: `sys:role:update`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

**请求参数**: 同"新增角色"

---

### 3.5 修改角色状态

**接口描述**: 启用/禁用角色

**请求方式**: `PUT /api/sys/roles/{id}/status`

**权限**: `sys:role:update`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

**请求参数**: 同"修改用户状态"

---

### 3.6 删除角色

**接口描述**: 删除指定角色

**请求方式**: `DELETE /api/sys/roles/{id}`

**权限**: `sys:role:delete`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

---

### 3.7 查询角色菜单

**接口描述**: 获取角色已分配的菜单ID列表

**请求方式**: `GET /api/sys/roles/{id}/menus`

**权限**: `sys:role:query`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    1,
    2,
    5,
    10
  ]
}
```

---

### 3.8 分配角色菜单

**接口描述**: 为角色分配菜单权限

**请求方式**: `PUT /api/sys/roles/{id}/menus`

**权限**: `sys:role:assign-menu`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 角色ID | 1  |

**请求参数** (RoleMenuAssignRequest):

| 参数名     | 类型         | 必填 | 说明     | 示例        |
|---------|------------|----|--------|-----------|
| menuIds | List[Long] | 是  | 菜单ID列表 | [1, 2, 5] |

**请求示例**:

```json
{
  "menuIds": [
    1,
    2,
    5,
    10
  ]
}
```

---

## 4. 后台菜单管理 (/api/sys/menus)

> 需要相应权限才能访问

### 4.1 查询菜单树

**接口描述**: 获取完整的菜单树结构

**请求方式**: `GET /api/sys/menus/tree`

**权限**: `sys:menu:query`

**响应数据** (List[SysMenuAdminVO]):

| 参数名       | 类型      | 说明             |
|-----------|---------|----------------|
| id        | Long    | 菜单ID           |
| parentId  | Long    | 父菜单ID          |
| treePath  | String  | 树路径            |
| name      | String  | 菜单名称           |
| routeName | String  | 路由名称           |
| routePath | String  | 路由路径           |
| component | String  | 组件路径           |
| perm      | String  | 权限标识           |
| alwaysShow | Integer | 始终显示          |
| keepAlive | Integer | 页面缓存           |
| icon      | String  | 图标             |
| type      | String  | 类型：`C`目录 `M`菜单 `B`按钮 |
| sort      | Integer | 排序号            |
| visible   | Integer | 是否可见           |
| redirect  | String  | 跳转路径           |
| params    | Object  | 路由参数           |
| children  | List    | 子菜单列表          |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "treePath": "0",
      "name": "系统管理",
      "routeName": "System",
      "routePath": "/system",
      "component": "layouts/RouteView",
      "perm": null,
      "alwaysShow": 1,
      "keepAlive": 0,
      "icon": "system",
      "type": "C",
      "sort": 1,
      "visible": 1,
      "redirect": "/system/users",
      "params": null,
      "children": []
    }
  ]
}
```

---

### 4.2 查询菜单详情

**接口描述**: 根据ID查询菜单详细信息

**请求方式**: `GET /api/sys/menus/{id}`

**权限**: `sys:menu:query`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 菜单ID | 1  |

---

### 4.3 新增菜单

**接口描述**: 创建新菜单

**请求方式**: `POST /api/sys/menus`

**权限**: `sys:menu:create`

**请求参数** (SysMenuSaveRequest):

| 参数名       | 类型      | 必填 | 说明             | 示例                 |
|-----------|---------|----|----------------|--------------------|
| parentId  | Long    | 是  | 父菜单ID，根节点可传 `0` | 0                  |
| treePath  | String  | 否  | 树路径            | 0                  |
| name      | String  | 是  | 菜单名称           | 用户管理               |
| type      | String  | 是  | 菜单类型：`C`目录 `M`菜单 `B`按钮 | M                  |
| routeName | String  | 否  | 路由名称           | SysUser            |
| routePath | String  | 否  | 路由路径           | /system/users      |
| component | String  | 否  | 组件路径           | system/users/index |
| perm      | String  | 否  | 权限标识           | sys:user:query     |
| alwaysShow | Integer | 否  | 是否始终显示：1是 0否  | 0                  |
| keepAlive | Integer | 否  | 是否缓存：1是 0否    | 1                  |
| icon      | String  | 否  | 图标             | user               |
| sort      | Integer | 否  | 排序号            | 1                  |
| visible   | Integer | 否  | 是否可见：0隐藏 1显示   | 1                  |
| redirect  | String  | 否  | 跳转路径           | /system/users      |
| params    | Object  | 否  | 路由参数           | `{"id":"1"}`       |

**请求示例**:

```json
{
  "parentId": 1,
  "treePath": "0",
  "name": "用户管理",
  "type": "M",
  "routeName": "SysUser",
  "routePath": "/system/users",
  "component": "system/users/index",
  "perm": "sys:user:query",
  "alwaysShow": 0,
  "keepAlive": 1,
  "icon": "user",
  "sort": 1,
  "visible": 1
}
```

---

### 4.4 修改菜单

**接口描述**: 更新菜单信息

**请求方式**: `PUT /api/sys/menus/{id}`

**权限**: `sys:menu:update`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 菜单ID | 1  |

**请求参数**: 同"新增菜单"

---

### 4.5 删除菜单

**接口描述**: 删除指定菜单

**请求方式**: `DELETE /api/sys/menus/{id}`

**权限**: `sys:menu:delete`

**路径参数**:

| 参数名 | 类型   | 必填 | 说明   | 示例 |
|-----|------|----|------|----|
| id  | Long | 是  | 菜单ID | 1  |

---

## 5. 系统配置管理 (/api/sys/configs)

> 需要相应权限才能访问

### 5.1 分页查询配置

**接口描述**: 分页查询系统配置列表

**请求方式**: `GET /api/sys/configs`

**权限**: `sys:config:query`

**请求参数** (Query):

| 参数名        | 类型      | 必填 | 说明         | 示例       |
|------------|---------|----|------------|----------|
| current    | Integer | 否  | 当前页，默认1    | 1        |
| size       | Integer | 否  | 每页数量，默认10  | 10       |
| configName | String  | 否  | 配置名称（模糊查询） | 系统名称     |
| configKey  | String  | 否  | 配置键（模糊查询）  | sys.name |

**响应数据** (PageResult[SysConfigAdminVO]):

| 参数名         | 类型      | 说明     |
|-------------|---------|--------|
| id          | Long    | 配置ID   |
| configName  | String  | 配置名称   |
| configKey   | String  | 配置键    |
| configValue | String  | 配置值    |
| isSystem    | Integer | 是否系统配置 |
| remark      | String  | 备注     |
| createTime  | Date    | 创建时间   |

---

### 5.2 查询配置详情

**接口描述**: 根据ID查询配置详细信息

**请求方式**: `GET /api/sys/configs/{id}`

**权限**: `sys:config:query`

---

### 5.3 新增配置

**接口描述**: 创建新配置

**请求方式**: `POST /api/sys/configs`

**权限**: `sys:config:create`

**请求参数** (SysConfigSaveRequest):

| 参数名         | 类型     | 必填 | 说明   | 示例       |
|-------------|--------|----|------|----------|
| configName  | String | 是  | 配置名称 | 系统名称     |
| configKey   | String | 是  | 配置键  | sys.name |
| configValue | String | 是  | 配置值  | 博客系统     |
| remark      | String | 否  | 备注   | 系统名称配置   |

---

### 5.4 修改配置

**接口描述**: 更新配置信息

**请求方式**: `PUT /api/sys/configs/{id}`

**权限**: `sys:config:update`

---

### 5.5 删除配置

**接口描述**: 删除指定配置

**请求方式**: `DELETE /api/sys/configs/{id}`

**权限**: `sys:config:delete`

---

### 5.6 按配置键查询配置值

**接口描述**: 根据配置键获取配置值

**请求方式**: `GET /api/sys/configs/key/{configKey}`

**权限**: `sys:config:query`

**路径参数**:

| 参数名       | 类型     | 必填 | 说明  | 示例       |
|-----------|--------|----|-----|----------|
| configKey | String | 是  | 配置键 | sys.name |

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": "博客系统"
}
```

---

## 6. 通知后台管理 (/api/sys/notices)

> 需要相应权限才能访问

### 6.1 分页查询通知

**接口描述**: 分页查询系统通知列表

**请求方式**: `GET /api/sys/notices`

**权限**: `sys:notice:query`

**请求参数** (Query):

| 参数名     | 类型      | 必填 | 说明        | 示例   |
|---------|---------|----|-----------|------|
| current | Integer | 否  | 当前页，默认1   | 1    |
| size    | Integer | 否  | 每页数量，默认10 | 10   |
| title   | String  | 否  | 标题（模糊查询）  | 维护通知 |
| type    | Integer | 否  | 类型        | 1    |
| status  | Integer | 否  | 状态        | 1    |

**响应数据** (PageResult[SysNoticeAdminVO]):

| 参数名         | 类型      | 说明   |
|-------------|---------|------|
| id          | Long    | 通知ID |
| title       | String  | 标题   |
| content     | String  | 内容   |
| type        | Integer | 类型   |
| status      | Integer | 状态   |
| publishTime | Date    | 发布时间 |
| createTime  | Date    | 创建时间 |

---

### 6.2 查询通知详情

**接口描述**: 根据ID查询通知详细信息

**请求方式**: `GET /api/sys/notices/{id}`

**权限**: `sys:notice:query`

---

### 6.3 新增通知

**接口描述**: 创建新通知

**请求方式**: `POST /api/sys/notices`

**权限**: `sys:notice:create`

**请求参数** (SysNoticeSaveRequest):

| 参数名     | 类型      | 必填 | 说明 | 示例        |
|---------|---------|----|----|-----------|
| title   | String  | 是  | 标题 | 系统维护通知    |
| content | String  | 是  | 内容 | 系统将于今晚... |
| type    | Integer | 否  | 类型 | 1         |

---

### 6.4 修改通知

**接口描述**: 更新通知信息

**请求方式**: `PUT /api/sys/notices/{id}`

**权限**: `sys:notice:update`

---

### 6.5 发布通知

**接口描述**: 发布指定通知

**请求方式**: `POST /api/sys/notices/{id}/publish`

**权限**: `sys:notice:publish`

---

### 6.6 撤回通知

**接口描述**: 撤回已发布的通知

**请求方式**: `POST /api/sys/notices/{id}/revoke`

**权限**: `sys:notice:revoke`

---

### 6.7 删除通知

**接口描述**: 删除指定通知

**请求方式**: `DELETE /api/sys/notices/{id}`

**权限**: `sys:notice:delete`

---

## 7. 用户通知中心 (/api/user/notices)

### 7.1 我的通知列表

**接口描述**: 获取当前用户的通知列表

**请求方式**: `GET /api/user/notices`

**请求参数** (Query):

| 参数名     | 类型      | 必填 | 说明        | 示例 |
|---------|---------|----|-----------|----|
| current | Integer | 否  | 当前页，默认1   | 1  |
| size    | Integer | 否  | 每页数量，默认10 | 10 |
| isRead  | Integer | 否  | 是否已读      | 0  |

**响应数据** (PageResult[UserNoticeVO]):

| 参数名        | 类型      | 说明    |
|------------|---------|-------|
| id         | Long    | 通知ID  |
| noticeId   | Long    | 原通知ID |
| title      | String  | 标题    |
| content    | String  | 内容    |
| isRead     | Integer | 是否已读  |
| readTime   | Date    | 阅读时间  |
| createTime | Date    | 创建时间  |

---

### 7.2 我的通知详情

**接口描述**: 获取通知详情（自动标记为已读）

**请求方式**: `GET /api/user/notices/{id}`

---

### 7.3 我的未读数

**接口描述**: 获取当前用户的未读通知数量

**请求方式**: `GET /api/user/notices/unread-count`

**响应示例**:

```json
{
  "code": 200,
  "message": "成功",
  "data": 5
}
```

---

### 7.4 单条已读

**接口描述**: 标记指定通知为已读

**请求方式**: `POST /api/user/notices/{id}/read`

---

### 7.5 全部已读

**接口描述**: 标记所有通知为已读

**请求方式**: `POST /api/user/notices/read-all`

---

## 8. 系统日志管理 (/api/sys/logs)

> 需要相应权限才能访问

### 8.1 分页查询日志

**接口描述**: 分页查询系统日志列表

**请求方式**: `GET /api/sys/logs`

**权限**: `sys:log:query`

**请求参数** (Query):

| 参数名       | 类型      | 必填 | 说明        | 示例         |
|-----------|---------|----|-----------|------------|
| current   | Integer | 否  | 当前页，默认1   | 1          |
| size      | Integer | 否  | 每页数量，默认10 | 10         |
| module    | String  | 否  | 模块名称      | auth       |
| action    | String  | 否  | 操作类型      | LOGIN      |
| username  | String  | 否  | 操作人       | admin      |
| startTime | Date    | 否  | 开始时间      | 2024-01-01 |
| endTime   | Date    | 否  | 结束时间      | 2024-12-31 |

**响应数据** (PageResult[SysLogAdminVO]):

| 参数名           | 类型      | 说明       |
|---------------|---------|----------|
| id            | Long    | 日志ID     |
| module        | String  | 模块名称     |
| action        | String  | 操作类型     |
| description   | String  | 操作描述     |
| username      | String  | 操作人      |
| ip            | String  | IP地址     |
| location      | String  | 地理位置     |
| userAgent     | String  | 浏览器信息    |
| requestMethod | String  | 请求方法     |
| requestUrl    | String  | 请求URL    |
| executeTime   | Long    | 执行时间(ms) |
| status        | Integer | 状态       |
| createTime    | Date    | 创建时间     |

---

### 8.2 查询日志详情

**接口描述**: 根据ID查询日志详细信息

**请求方式**: `GET /api/sys/logs/{id}`

**权限**: `sys:log:query`

---

### 8.3 删除日志

**接口描述**: 删除指定日志

**请求方式**: `DELETE /api/sys/logs/{id}`

**权限**: `sys:log:delete`

---

### 8.4 按条件清理日志

**接口描述**: 根据时间范围批量清理日志

**请求方式**: `POST /api/sys/logs/clean`

**权限**: `sys:log:clean`

**请求参数** (SysLogCleanRequest):

| 参数名       | 类型   | 必填 | 说明   | 示例         |
|-----------|------|----|------|------------|
| startTime | Date | 否  | 开始时间 | 2024-01-01 |
| endTime   | Date | 否  | 结束时间 | 2024-12-31 |

**响应数据**: 返回清理的日志数量

---

## 附录

### A. 状态码说明

| 状态码 | 说明          |
|-----|-------------|
| 200 | 成功          |
| 400 | 请求参数错误      |
| 401 | 未授权/Token过期 |
| 403 | 无权限访问       |
| 404 | 资源不存在       |
| 500 | 服务器内部错误     |

### B. 通用状态说明

**用户状态 (status)**:

- 0: 禁用
- 1: 正常

**角色状态 (status)**:

- 0: 禁用
- 1: 启用

**菜单类型 (type)**:

- `C`: 目录
- `M`: 菜单
- `B`: 按钮

**菜单可见 (visible)**:

- 0: 隐藏
- 1: 显示

**通知类型 (type)**:

- 1: 系统通知
- 2: 活动通知

**通知状态 (status)**:

- 0: 草稿
- 1: 已发布
- 2: 已撤回

### C. 权限标识列表

| 模块 | 权限标识                    | 说明   |
|----|-------------------------|------|
| 用户 | sys:user:query          | 查询用户 |
| 用户 | sys:user:create         | 新增用户 |
| 用户 | sys:user:update         | 修改用户 |
| 用户 | sys:user:delete         | 删除用户 |
| 用户 | sys:user:reset-password | 重置密码 |
| 用户 | sys:user:assign-role    | 分配角色 |
| 角色 | sys:role:query          | 查询角色 |
| 角色 | sys:role:create         | 新增角色 |
| 角色 | sys:role:update         | 修改角色 |
| 角色 | sys:role:delete         | 删除角色 |
| 角色 | sys:role:assign-menu    | 分配菜单 |
| 菜单 | sys:menu:query          | 查询菜单 |
| 菜单 | sys:menu:create         | 新增菜单 |
| 菜单 | sys:menu:update         | 修改菜单 |
| 菜单 | sys:menu:delete         | 删除菜单 |
| 配置 | sys:config:query        | 查询配置 |
| 配置 | sys:config:create       | 新增配置 |
| 配置 | sys:config:update       | 修改配置 |
| 配置 | sys:config:delete       | 删除配置 |
| 通知 | sys:notice:query        | 查询通知 |
| 通知 | sys:notice:create       | 新增通知 |
| 通知 | sys:notice:update       | 修改通知 |
| 通知 | sys:notice:delete       | 删除通知 |
| 通知 | sys:notice:publish      | 发布通知 |
| 通知 | sys:notice:revoke       | 撤回通知 |
| 日志 | sys:log:query           | 查询日志 |
| 日志 | sys:log:delete          | 删除日志 |
| 日志 | sys:log:clean           | 清理日志 |

---

*文档更新时间: 2026-03-19*

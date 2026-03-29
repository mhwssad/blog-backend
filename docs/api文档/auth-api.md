# 认证与系统管理 API

这份文档给前端使用，覆盖三类场景：

- 登录、注册、刷新令牌、退出登录
- 后台管理台初始化所需的当前用户与菜单接口
- 后台系统管理页面，以及登录用户的通知中心

## 1. 快速接入

### 1.1 路由分组

| 路由前缀 | 用途 | 是否需要登录 |
| --- | --- | --- |
| `/api/auth/**` | 登录、注册、刷新令牌、获取当前登录用户 | 部分需要 |
| `/api/sys/**` | 后台管理接口 | 需要 |
| `/api/user/notices/**` | 登录用户通知中心 | 需要 |

### 1.2 匿名可访问接口

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/email-code`
- `POST /api/auth/email-login`
- `POST /api/auth/refresh`

其余接口默认都要带：

```http
Authorization: Bearer <accessToken>
```

### 1.3 统一响应

所有接口返回 `Result<T>`：

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

### 1.4 前端处理建议

| 场景 | 建议 |
| --- | --- |
| HTTP `401` | 尝试刷新令牌；刷新失败后回到登录页 |
| HTTP `403` | 已登录但无权限，提示无权限或跳到 403 页面 |
| `Result.code != 200` | 按业务失败处理，优先展示 `message` |
| 刷新接口成功 | 同时替换 `accessToken` 和 `refreshToken` |

## 2. 登录态接入流程

### 2.1 登录页 / 注册页

常用调用顺序：

1. 账号密码登录：`POST /api/auth/login`
2. 邮箱验证码登录：先发验证码，再调 `POST /api/auth/email-login`
3. 注册完成后，接口直接返回登录态，无需再调登录接口

### 2.2 应用启动恢复登录态

推荐顺序：

1. 本地存在 `accessToken` 时，先调 `GET /api/auth/current-user`
2. 成功后再调 `GET /api/auth/current-user-menus`
3. 如果 `current-user` 返回 `401`，尝试 `POST /api/auth/refresh`
4. 刷新成功后重试步骤 1 和步骤 2

### 2.3 退出登录

- 调用 `POST /api/auth/logout`
- 成功后前端清理本地令牌、用户信息、菜单和权限缓存

## 3. 认证接口

### 3.1 账号登录

- 请求：`POST /api/auth/login`
- 鉴权：否
- 用途：登录页账号密码登录
- 请求体：`AuthLoginRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | String | 是 | 支持用户名 / 邮箱 / 手机号 |
| `password` | String | 是 | 登录密码 |

- 响应：`AuthenticationToken`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tokenType` | String | 固定为 `Bearer` |
| `accessToken` | String | 访问令牌 |
| `refreshToken` | String | 刷新令牌 |
| `expiresIn` | Integer | `accessToken` 过期秒数 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 7200
  }
}
```

### 3.2 账号注册

- 请求：`POST /api/auth/register`
- 鉴权：否
- 用途：注册页
- 请求体：`AuthRegisterRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码 |
| `nickname` | String | 否 | 未传时默认使用用户名 |
| `email` | String | 否 | 邮箱 |
| `phone` | String | 否 | 手机号 |

- 响应：同 `AuthenticationToken`

### 3.3 发送邮箱验证码

- 请求：`POST /api/auth/email-code`
- 鉴权：否
- 用途：邮箱验证码登录前先发送验证码
- 请求体：`AuthEmailCodeRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | String | 是 | 邮箱地址 |

- 响应：`data = null`

### 3.4 邮箱验证码登录

- 请求：`POST /api/auth/email-login`
- 鉴权：否
- 用途：邮箱验证码登录
- 请求体：`AuthEmailLoginRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | String | 是 | 邮箱地址 |
| `code` | String | 是 | 6 位验证码 |

- 响应：同 `AuthenticationToken`

### 3.5 刷新令牌

- 请求：`POST /api/auth/refresh`
- 鉴权：否
- 用途：令牌续期
- 请求体：`AuthRefreshRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `refreshToken` | String | 是 | 刷新令牌 |

- 响应：同 `AuthenticationToken`

### 3.6 退出登录

- 请求：`POST /api/auth/logout`
- 鉴权：建议带 Bearer Token
- 用途：主动退出登录
- 请求体：`LogoutRequest`，可为空

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `accessToken` | String | 否 | 不传时默认读取 `Authorization` 请求头 |

- 前端说明：
  - 大多数场景只需要携带请求头即可。
  - 即使接口失败，前端通常也应清理本地登录态，避免残留脏状态。

### 3.7 获取当前登录用户

- 请求：`GET /api/auth/current-user`
- 鉴权：是
- 用途：应用启动初始化用户信息、权限码、角色码
- 响应：`AuthUserInfo`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像 |
| `email` | String | 邮箱 |
| `phone` | String | 手机号 |
| `status` | Integer | `0` 禁用，`1` 正常 |
| `roles` | List<String> | 角色编码列表 |
| `permissions` | List<String> | 权限标识列表 |

### 3.8 获取当前用户菜单

- 请求：`GET /api/auth/current-user-menus`
- 鉴权：是
- 用途：后台动态路由、侧边菜单、按钮权限挂载
- 响应：`List<AuthMenuInfo>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 菜单 ID |
| `parentId` | Long | 父菜单 ID |
| `name` | String | 菜单名称 |
| `type` | String | `C` 目录、`M` 菜单、`B` 按钮 |
| `routeName` | String | 路由名称 |
| `routePath` | String | 路由路径 |
| `component` | String | 前端组件路径 |
| `perm` | String | 权限标识 |
| `visible` | Integer | 是否显示，`0/1` |
| `sort` | Integer | 排序 |
| `icon` | String | 图标 |
| `redirect` | String | 重定向路径 |
| `alwaysShow` | Integer | 是否始终显示 |
| `keepAlive` | Integer | 是否缓存 |
| `params` | Object | 额外路由参数 |
| `children` | List<AuthMenuInfo> | 子节点 |

## 4. 后台应用启动流程

后台管理台通常要用到下面两组数据：

| 接口 | 用途 |
| --- | --- |
| `GET /api/auth/current-user` | 初始化头像、昵称、角色、按钮权限 |
| `GET /api/auth/current-user-menus` | 渲染动态菜单、动态路由、页面缓存配置 |

前端常见处理规则：

- `type=B` 的菜单一般不渲染路由，用于按钮权限控制。
- `visible=0` 可用于隐藏侧边栏，但仍保留路由配置。
- `keepAlive=1` 可映射前端页面缓存开关。
- `params` 可直接作为扩展路由元数据使用。

## 5. 登录用户通知中心

### 5.1 页面会用到哪些接口

| 场景 | 接口 |
| --- | --- |
| 通知列表页 | `GET /api/user/notices` |
| 右上角未读角标 | `GET /api/user/notices/unread-count` |
| 通知详情抽屉 / 详情页 | `GET /api/user/notices/{id}` |
| 单条标记已读 | `POST /api/user/notices/{id}/read` |
| 全部标记已读 | `POST /api/user/notices/read-all` |

### 5.2 我的通知列表

- 请求：`GET /api/user/notices`
- 鉴权：是
- 查询参数：`UserNoticePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码 |
| `size` | Long | 每页条数 |
| `title` | String | 标题关键词 |
| `isRead` | Integer | `0` 未读，`1` 已读 |

- 响应项：`UserNoticeVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 通知 ID |
| `title` | String | 标题 |
| `content` | String | 内容 |
| `type` | Integer | 通知类型 |
| `level` | String | 通知等级 |
| `publishTime` | DateTime | 发布时间 |
| `isRead` | Integer | 是否已读 |
| `readTime` | DateTime | 阅读时间 |

### 5.3 我的通知详情

- 请求：`GET /api/user/notices/{id}`
- 鉴权：是
- 路径参数：`id`
- 前端注意：按当前实现，读取详情会顺带更新阅读状态。

### 5.4 未读数量

- 请求：`GET /api/user/notices/unread-count`
- 鉴权：是
- 响应：`Long`

### 5.5 单条已读

- 请求：`POST /api/user/notices/{id}/read`
- 鉴权：是
- 响应：`data = null`

### 5.6 全部已读

- 请求：`POST /api/user/notices/read-all`
- 鉴权：是
- 响应：`data = null`

## 6. 后台系统管理接口

这一组接口主要对应后台管理台的系统模块。所有接口都要求登录，并且需要对应权限码。

### 6.1 用户管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询用户 | GET | `/api/sys/users` | `sys:user:query` |
| 查询用户详情 | GET | `/api/sys/users/{id}` | `sys:user:query` |
| 新增用户 | POST | `/api/sys/users` | `sys:user:create` |
| 修改用户 | PUT | `/api/sys/users/{id}` | `sys:user:update` |
| 修改用户状态 | PUT | `/api/sys/users/{id}/status` | `sys:user:update` |
| 重置用户密码 | PUT | `/api/sys/users/{id}/password/reset` | `sys:user:reset-password` |
| 删除用户 | DELETE | `/api/sys/users/{id}` | `sys:user:delete` |
| 查询用户角色 | GET | `/api/sys/users/{id}/roles` | `sys:user:query` |
| 分配用户角色 | PUT | `/api/sys/users/{id}/roles` | `sys:user:assign-role` |

#### 分页查询用户

- 请求：`GET /api/sys/users`
- 查询参数：`SysUserPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10` |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `email` | String | 邮箱 |
| `phone` | String | 手机号 |
| `status` | Integer | 状态 |

- 响应项：`SysUserAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `email` | String | 邮箱 |
| `phone` | String | 手机号 |
| `avatar` | String | 头像 |
| `gender` | Integer | 性别 |
| `birthday` | DateTime | 生日 |
| `status` | Integer | 状态 |
| `lastLoginTime` | DateTime | 最后登录时间 |
| `lastLoginIp` | String | 最后登录 IP |
| `remark` | String | 备注 |
| `roleIds` | List<Long> | 角色 ID 列表 |

#### 新增 / 修改用户

- 新增：`POST /api/sys/users`
- 修改：`PUT /api/sys/users/{id}`
- 请求体：`SysUserSaveRequest`

| 字段 | 类型 | 新增必填 | 修改必填 | 说明 |
| --- | --- | --- | --- | --- |
| `username` | String | 是 | 否 | 用户名 |
| `password` | String | 是 | 否 | 修改时不生效，重置密码走单独接口 |
| `nickname` | String | 否 | 否 | 昵称 |
| `email` | String | 否 | 否 | 邮箱 |
| `phone` | String | 否 | 否 | 手机号 |
| `avatar` | String | 否 | 否 | 头像 |
| `gender` | Integer | 否 | 否 | 性别 |
| `birthday` | DateTime | 否 | 否 | 生日 |
| `status` | Integer | 否 | 否 | 默认 `1` |
| `remark` | String | 否 | 否 | 备注 |

#### 状态、密码、角色

- 修改状态：`PUT /api/sys/users/{id}/status`

```json
{
  "status": 1
}
```

- 重置密码：`PUT /api/sys/users/{id}/password/reset`

```json
{
  "password": "123456"
}
```

- 查询角色：`GET /api/sys/users/{id}/roles`
- 分配角色：`PUT /api/sys/users/{id}/roles`

```json
{
  "roleIds": [1, 2]
}
```

### 6.2 角色管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询角色 | GET | `/api/sys/roles` | `sys:role:query` |
| 查询角色详情 | GET | `/api/sys/roles/{id}` | `sys:role:query` |
| 新增角色 | POST | `/api/sys/roles` | `sys:role:create` |
| 修改角色 | PUT | `/api/sys/roles/{id}` | `sys:role:update` |
| 修改角色状态 | PUT | `/api/sys/roles/{id}/status` | `sys:role:update` |
| 删除角色 | DELETE | `/api/sys/roles/{id}` | `sys:role:delete` |
| 查询角色菜单 | GET | `/api/sys/roles/{id}/menus` | `sys:role:query` |
| 分配角色菜单 | PUT | `/api/sys/roles/{id}/menus` | `sys:role:assign-menu` |

#### 角色分页字段

- 请求：`GET /api/sys/roles`
- 查询参数：`SysRolePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码 |
| `size` | Long | 每页条数 |
| `name` | String | 角色名称 |
| `code` | String | 角色编码 |
| `status` | Integer | 角色状态 |

- 响应项：`SysRoleAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 角色 ID |
| `name` | String | 角色名称 |
| `code` | String | 角色编码 |
| `sort` | Integer | 显示顺序 |
| `status` | Integer | 状态 |
| `dataScope` | Integer | 数据权限 |
| `menuIds` | List<Long> | 菜单 ID 列表 |

#### 新增 / 修改角色

- 新增：`POST /api/sys/roles`
- 修改：`PUT /api/sys/roles/{id}`
- 请求体：`SysRoleSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | String | 是 | 角色名称 |
| `code` | String | 是 | 角色编码 |
| `sort` | Integer | 否 | 显示顺序 |
| `status` | Integer | 否 | 默认 `1` |
| `dataScope` | Integer | 否 | 数据权限 |

#### 状态与菜单分配

- 修改状态：`PUT /api/sys/roles/{id}/status`

```json
{
  "status": 1
}
```

- 查询角色菜单：`GET /api/sys/roles/{id}/menus`
- 分配角色菜单：`PUT /api/sys/roles/{id}/menus`

```json
{
  "menuIds": [1, 2, 3]
}
```

### 6.3 菜单管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 查询菜单树 | GET | `/api/sys/menus/tree` | `sys:menu:query` |
| 查询菜单详情 | GET | `/api/sys/menus/{id}` | `sys:menu:query` |
| 新增菜单 | POST | `/api/sys/menus` | `sys:menu:create` |
| 修改菜单 | PUT | `/api/sys/menus/{id}` | `sys:menu:update` |
| 删除菜单 | DELETE | `/api/sys/menus/{id}` | `sys:menu:delete` |

#### 菜单树响应

- 请求：`GET /api/sys/menus/tree`
- 响应：`List<SysMenuAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 菜单 ID |
| `parentId` | Long | 父菜单 ID |
| `treePath` | String | 树路径 |
| `name` | String | 菜单名称 |
| `type` | String | 菜单类型 |
| `routeName` | String | 路由名称 |
| `routePath` | String | 路由路径 |
| `component` | String | 组件路径 |
| `perm` | String | 权限标识 |
| `alwaysShow` | Integer | 是否始终显示 |
| `keepAlive` | Integer | 是否缓存 |
| `visible` | Integer | 是否显示 |
| `sort` | Integer | 排序 |
| `icon` | String | 图标 |
| `redirect` | String | 跳转地址 |
| `params` | Object | 路由参数 |
| `children` | List<SysMenuAdminVO> | 子节点 |

#### 新增 / 修改菜单

- 新增：`POST /api/sys/menus`
- 修改：`PUT /api/sys/menus/{id}`
- 请求体：`SysMenuSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `parentId` | Long | 是 | 根节点传 `0` |
| `treePath` | String | 否 | 前端通常无需传，后端会重新计算 |
| `name` | String | 是 | 菜单名称 |
| `type` | String | 是 | `C` / `M` / `B` |
| `routeName` | String | 否 | 路由名称 |
| `routePath` | String | 否 | 路由路径 |
| `component` | String | 否 | 组件路径 |
| `perm` | String | 否 | 权限标识 |
| `alwaysShow` | Integer | 否 | 是否始终显示 |
| `keepAlive` | Integer | 否 | 是否缓存 |
| `visible` | Integer | 否 | 是否显示 |
| `sort` | Integer | 否 | 排序 |
| `icon` | String | 否 | 图标 |
| `redirect` | String | 否 | 跳转地址 |
| `params` | Object | 否 | 路由参数 |

### 6.4 系统配置管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询配置 | GET | `/api/sys/configs` | `sys:config:query` |
| 查询配置详情 | GET | `/api/sys/configs/{id}` | `sys:config:query` |
| 新增配置 | POST | `/api/sys/configs` | `sys:config:create` |
| 修改配置 | PUT | `/api/sys/configs/{id}` | `sys:config:update` |
| 删除配置 | DELETE | `/api/sys/configs/{id}` | `sys:config:delete` |
| 按 key 查询配置值 | GET | `/api/sys/configs/key/{configKey}` | `sys:config:query` |

#### 分页查询配置

- 请求：`GET /api/sys/configs`
- 查询参数：`SysConfigPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码 |
| `size` | Long | 每页条数 |
| `configName` | String | 配置名称 |
| `configKey` | String | 配置键 |
| `createTimeStart` | DateTime | 创建开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `createTimeEnd` | DateTime | 创建结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

- 响应项：`SysConfigAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 配置 ID |
| `configName` | String | 配置名称 |
| `configKey` | String | 配置键 |
| `configValue` | String | 配置值 |
| `remark` | String | 备注 |
| `createTime` | DateTime | 创建时间 |
| `updateTime` | DateTime | 更新时间 |

#### 新增 / 修改配置

- 请求体：`SysConfigSaveRequest`

```json
{
  "configName": "站点标题",
  "configKey": "site.title",
  "configValue": "我的博客",
  "remark": "前台站点标题"
}
```

### 6.5 后台通知管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询通知 | GET | `/api/sys/notices` | `sys:notice:query` |
| 查询通知详情 | GET | `/api/sys/notices/{id}` | `sys:notice:query` |
| 新增通知 | POST | `/api/sys/notices` | `sys:notice:create` |
| 修改通知 | PUT | `/api/sys/notices/{id}` | `sys:notice:update` |
| 发布通知 | POST | `/api/sys/notices/{id}/publish` | `sys:notice:publish` |
| 撤回通知 | POST | `/api/sys/notices/{id}/revoke` | `sys:notice:revoke` |
| 删除通知 | DELETE | `/api/sys/notices/{id}` | `sys:notice:delete` |

#### 分页查询通知

- 请求：`GET /api/sys/notices`
- 查询参数：`SysNoticePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码 |
| `size` | Long | 每页条数 |
| `title` | String | 标题 |
| `publishStatus` | Integer | 发布状态 |
| `targetType` | Integer | 目标类型 |

- 响应项：`SysNoticeAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 通知 ID |
| `title` | String | 标题 |
| `content` | String | 内容 |
| `type` | Integer | 通知类型 |
| `level` | String | 通知等级 |
| `targetType` | Integer | `1` 全体，`2` 指定用户 |
| `targetUserIds` | List<Long> | 指定用户列表 |
| `publisherId` | Long | 发布人 ID |
| `publishStatus` | Integer | 发布状态 |
| `publishTime` | DateTime | 发布时间 |
| `revokeTime` | DateTime | 撤回时间 |
| `createTime` | DateTime | 创建时间 |
| `updateTime` | DateTime | 更新时间 |

#### 新增 / 修改通知

- 请求体：`SysNoticeSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `title` | String | 是 | 通知标题 |
| `content` | String | 是 | 通知内容 |
| `type` | Integer | 是 | 通知类型 |
| `level` | String | 是 | 通知等级 |
| `targetType` | Integer | 是 | `1` 全体，`2` 指定 |
| `targetUserIds` | List<Long> | 否 | 指定用户时使用 |

### 6.6 系统日志管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询日志 | GET | `/api/sys/logs` | `sys:log:query` |
| 查询日志详情 | GET | `/api/sys/logs/{id}` | `sys:log:query` |
| 删除日志 | DELETE | `/api/sys/logs/{id}` | `sys:log:delete` |
| 按条件清理日志 | POST | `/api/sys/logs/clean` | `sys:log:clean` |

#### 分页查询日志

- 请求：`GET /api/sys/logs`
- 查询参数：`SysLogPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码 |
| `size` | Long | 每页条数 |
| `module` | String | 日志模块 |
| `requestMethod` | String | 请求方式 |
| `requestUri` | String | 请求路径 |
| `ip` | String | IP |
| `createBy` | Long | 创建人 ID |
| `createTimeStart` | DateTime | 创建开始时间 |
| `createTimeEnd` | DateTime | 创建结束时间 |

- 响应项：`SysLogAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 日志 ID |
| `module` | String | 日志模块 |
| `requestMethod` | String | 请求方式 |
| `requestParams` | String | 请求参数 |
| `responseContent` | String | 响应内容 |
| `content` | String | 日志内容 |
| `requestUri` | String | 请求路径 |
| `method` | String | 处理方法 |
| `ip` | String | IP 地址 |
| `province` | String | 省份 |
| `city` | String | 城市 |
| `executionTime` | Long | 执行耗时（ms） |
| `browser` | String | 浏览器 |
| `browserVersion` | String | 浏览器版本 |
| `os` | String | 操作系统 |
| `createBy` | Long | 创建人 ID |
| `createTime` | DateTime | 创建时间 |

#### 条件清理日志

- 请求：`POST /api/sys/logs/clean`
- 请求体：`SysLogCleanRequest`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `module` | String | 日志模块 |
| `requestMethod` | String | 请求方式 |
| `requestUri` | String | 请求路径 |
| `ip` | String | IP |
| `createBy` | Long | 创建人 ID |
| `createTimeStart` | DateTime | 创建开始时间 |
| `createTimeEnd` | DateTime | 创建结束时间 |

- 响应：`Long`，表示清理数量

## 7. 权限标识速查

| 权限标识 | 说明 |
| --- | --- |
| `sys:user:query` | 查询用户 |
| `sys:user:create` | 新增用户 |
| `sys:user:update` | 修改用户、修改状态 |
| `sys:user:delete` | 删除用户 |
| `sys:user:reset-password` | 重置用户密码 |
| `sys:user:assign-role` | 分配用户角色 |
| `sys:role:query` | 查询角色 |
| `sys:role:create` | 新增角色 |
| `sys:role:update` | 修改角色、修改状态 |
| `sys:role:delete` | 删除角色 |
| `sys:role:assign-menu` | 分配角色菜单 |
| `sys:menu:query` | 查询菜单 |
| `sys:menu:create` | 新增菜单 |
| `sys:menu:update` | 修改菜单 |
| `sys:menu:delete` | 删除菜单 |
| `sys:config:query` | 查询配置 |
| `sys:config:create` | 新增配置 |
| `sys:config:update` | 修改配置 |
| `sys:config:delete` | 删除配置 |
| `sys:notice:query` | 查询通知 |
| `sys:notice:create` | 新增通知 |
| `sys:notice:update` | 修改通知 |
| `sys:notice:publish` | 发布通知 |
| `sys:notice:revoke` | 撤回通知 |
| `sys:notice:delete` | 删除通知 |
| `sys:log:query` | 查询日志 |
| `sys:log:delete` | 删除日志 |
| `sys:log:clean` | 清理日志 |

## 8. 常见联调问题

| 问题 | 当前行为 |
| --- | --- |
| 匿名访问 `/api/sys/**` | HTTP `401` |
| 匿名访问 `/api/user/notices/**` | HTTP `401` |
| 已登录但权限不足 | HTTP `403` |
| 打开通知详情后未读数变化 | 详情接口按当前实现会更新已读状态 |
| 修改用户时传了 `password` | 不生效，必须走重置密码接口 |

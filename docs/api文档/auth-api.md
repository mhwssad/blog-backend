# Auth 模块接口文档

本文档面向前端联调，覆盖认证、后台系统管理和用户通知中心接口。

## 1. 联调约定

### 1.1 Base URL

- 统一前缀：`/api`

### 1.2 认证方式

- 匿名接口：`/api/auth/login`、`/api/auth/register`、`/api/auth/email-code`、`/api/auth/email-login`、`/api/auth/refresh`
- 登录后接口：其余 `auth`、`sys`、`user/notices` 接口
- Bearer Token 请求头：

```http
Authorization: Bearer <accessToken>
```

### 1.3 统一响应结构

所有接口统一返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | Integer | 业务状态码，`200` 表示成功 |
| message | String | 业务消息 |
| timestamp | Long | 服务端响应时间戳，毫秒 |
| data | Any | 响应数据，可能为对象、数组、分页对象或 `null` |

分页接口 `data` 结构固定为：

```json
{
  "total": 1,
  "current": 1,
  "size": 10,
  "records": []
}
```

### 1.4 常见前端处理建议

- `401`：未登录、Token 失效、Token 过期，前端应跳转登录或尝试刷新令牌。
- `403`：已登录但无权限访问，前端应提示无权限。
- `Result.code != 200`：属于业务失败，优先展示 `message`。
- 时间字段默认按后端序列化结果返回，前端统一按本地格式化处理。

## 2. 认证接口

### 2.1 账号登录

- **POST** `/api/auth/login`
- **是否鉴权**: 否
- **请求体**: `AuthLoginRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 登录账号，支持用户名/邮箱/手机号 |
| password | String | 是 | 登录密码 |

- **响应**: `AuthenticationToken`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tokenType | String | 令牌类型，固定为 `Bearer` |
| accessToken | String | 访问令牌 |
| refreshToken | String | 刷新令牌 |
| expiresIn | Integer | accessToken 过期时间，单位秒 |

- **响应示例**:

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

### 2.2 账号注册

- **POST** `/api/auth/register`
- **是否鉴权**: 否
- **请求体**: `AuthRegisterRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| nickname | String | 否 | 昵称，未传时默认使用用户名 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |

- **响应**: 同登录接口 `AuthenticationToken`

### 2.3 发送邮箱登录验证码

- **POST** `/api/auth/email-code`
- **是否鉴权**: 否
- **请求体**: `AuthEmailCodeRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| email | String | 是 | 邮箱地址 |

- **响应**: `data = null`

### 2.4 邮箱验证码登录

- **POST** `/api/auth/email-login`
- **是否鉴权**: 否
- **请求体**: `AuthEmailLoginRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| email | String | 是 | 邮箱地址 |
| code | String | 是 | 6 位验证码 |

- **响应**: 同登录接口 `AuthenticationToken`

### 2.5 刷新令牌

- **POST** `/api/auth/refresh`
- **是否鉴权**: 否
- **请求体**: `AuthRefreshRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| refreshToken | String | 是 | 刷新令牌 |

- **响应**: `AuthenticationToken`

### 2.6 退出登录

- **POST** `/api/auth/logout`
- **是否鉴权**: 建议传 Bearer Token
- **请求体**: `LogoutRequest`，可为空

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| accessToken | String | 否 | 不传时默认读取 `Authorization` 请求头 |

- **说明**:
  - 前端通常只需要带上 `Authorization` 请求头即可。
  - 成功后前端应清理本地登录态和缓存用户信息。

### 2.7 获取当前登录用户

- **GET** `/api/auth/current-user`
- **是否鉴权**: 是
- **响应**: `AuthUserInfo`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| status | Integer | 用户状态，`0` 禁用，`1` 正常 |
| roles | List<String> | 角色编码列表 |
| permissions | List<String> | 权限标识列表 |

### 2.8 获取当前用户菜单

- **GET** `/api/auth/current-user-menus`
- **是否鉴权**: 是
- **响应**: `List<AuthMenuInfo>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 菜单 ID |
| parentId | Long | 父菜单 ID |
| name | String | 菜单名称 |
| type | String | 菜单类型：`C` 目录、`M` 菜单、`B` 按钮 |
| routeName | String | 路由名称 |
| routePath | String | 路由路径 |
| component | String | 前端组件路径 |
| perm | String | 权限标识 |
| visible | Integer | 是否显示，`0/1` |
| sort | Integer | 排序 |
| icon | String | 图标 |
| redirect | String | 重定向路径 |
| alwaysShow | Integer | 是否始终显示 |
| keepAlive | Integer | 是否缓存 |
| params | Object | 路由参数 |
| children | List<AuthMenuInfo> | 子菜单 |

## 3. 后台用户管理

### 3.1 分页查询用户

- **GET** `/api/sys/users`
- **权限**: `sys:user:query`
- **查询参数**: `SysUserPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码，默认 `1` |
| size | Long | 每页条数，默认 `10` |
| username | String | 用户名 |
| nickname | String | 昵称 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| status | Integer | 状态 |

- **响应项**: `SysUserAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| avatar | String | 头像 |
| gender | Integer | 性别 |
| birthday | DateTime | 生日 |
| status | Integer | 状态 |
| lastLoginTime | DateTime | 最后登录时间 |
| lastLoginIp | String | 最后登录 IP |
| remark | String | 备注 |
| roleIds | List<Long> | 角色 ID 列表 |

### 3.2 查询用户详情

- **GET** `/api/sys/users/{id}`
- **权限**: `sys:user:query`
- **响应**: `SysUserAdminVO`

### 3.3 新增用户

- **POST** `/api/sys/users`
- **权限**: `sys:user:create`
- **请求体**: `SysUserSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 用户名 |
| password | String | 是 | 新增时必填 |
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| avatar | String | 否 | 头像 |
| gender | Integer | 否 | 性别 |
| birthday | DateTime | 否 | 生日 |
| status | Integer | 否 | 状态，默认 `1` |
| remark | String | 否 | 备注 |

- **响应**: `SysUserAdminVO`

### 3.4 修改用户

- **PUT** `/api/sys/users/{id}`
- **权限**: `sys:user:update`
- **请求体**: `SysUserSaveRequest`
- **说明**: 修改用户时 `password` 不生效，重置密码请走单独接口。

### 3.5 修改用户状态

- **PUT** `/api/sys/users/{id}/status`
- **权限**: `sys:user:update`
- **请求体**: `StatusUpdateRequest`

```json
{
  "status": 1
}
```

### 3.6 重置用户密码

- **PUT** `/api/sys/users/{id}/password/reset`
- **权限**: `sys:user:reset-password`
- **请求体**: `PasswordResetRequest`

```json
{
  "password": "123456"
}
```

### 3.7 删除用户

- **DELETE** `/api/sys/users/{id}`
- **权限**: `sys:user:delete`

### 3.8 查询用户角色

- **GET** `/api/sys/users/{id}/roles`
- **权限**: `sys:user:query`
- **响应**: `List<Long>`

### 3.9 分配用户角色

- **PUT** `/api/sys/users/{id}/roles`
- **权限**: `sys:user:assign-role`
- **请求体**: `UserRoleAssignRequest`

```json
{
  "roleIds": [1, 2]
}
```

## 4. 后台角色管理

### 4.1 分页查询角色

- **GET** `/api/sys/roles`
- **权限**: `sys:role:query`
- **查询参数**: `SysRolePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页条数 |
| name | String | 角色名称 |
| code | String | 角色编码 |
| status | Integer | 角色状态 |

- **响应项**: `SysRoleAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 角色 ID |
| name | String | 角色名称 |
| code | String | 角色编码 |
| sort | Integer | 显示顺序 |
| status | Integer | 状态 |
| dataScope | Integer | 数据权限 |
| menuIds | List<Long> | 菜单 ID 列表 |

### 4.2 查询角色详情

- **GET** `/api/sys/roles/{id}`
- **权限**: `sys:role:query`
- **响应**: `SysRoleAdminVO`

### 4.3 新增角色

- **POST** `/api/sys/roles`
- **权限**: `sys:role:create`
- **请求体**: `SysRoleSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | String | 是 | 角色名称 |
| code | String | 是 | 角色编码 |
| sort | Integer | 否 | 显示顺序 |
| status | Integer | 否 | 状态，默认 `1` |
| dataScope | Integer | 否 | 数据权限 |

### 4.4 修改角色

- **PUT** `/api/sys/roles/{id}`
- **权限**: `sys:role:update`
- **请求体**: `SysRoleSaveRequest`

### 4.5 修改角色状态

- **PUT** `/api/sys/roles/{id}/status`
- **权限**: `sys:role:update`
- **请求体**: `StatusUpdateRequest`

### 4.6 删除角色

- **DELETE** `/api/sys/roles/{id}`
- **权限**: `sys:role:delete`

### 4.7 查询角色菜单

- **GET** `/api/sys/roles/{id}/menus`
- **权限**: `sys:role:query`
- **响应**: `List<Long>`

### 4.8 分配角色菜单

- **PUT** `/api/sys/roles/{id}/menus`
- **权限**: `sys:role:assign-menu`
- **请求体**: `RoleMenuAssignRequest`

```json
{
  "menuIds": [1, 2, 3]
}
```

## 5. 后台菜单管理

### 5.1 查询菜单树

- **GET** `/api/sys/menus/tree`
- **权限**: `sys:menu:query`
- **响应**: `List<SysMenuAdminVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 菜单 ID |
| parentId | Long | 父菜单 ID |
| treePath | String | 树路径 |
| name | String | 菜单名称 |
| type | String | 菜单类型 |
| routeName | String | 路由名称 |
| routePath | String | 路由路径 |
| component | String | 组件路径 |
| perm | String | 权限标识 |
| alwaysShow | Integer | 是否始终显示 |
| keepAlive | Integer | 是否缓存 |
| visible | Integer | 是否显示 |
| sort | Integer | 排序 |
| icon | String | 图标 |
| redirect | String | 跳转地址 |
| params | Object | 路由参数 |
| children | List<SysMenuAdminVO> | 子菜单 |

### 5.2 查询菜单详情

- **GET** `/api/sys/menus/{id}`
- **权限**: `sys:menu:query`
- **响应**: `SysMenuAdminVO`

### 5.3 新增菜单

- **POST** `/api/sys/menus`
- **权限**: `sys:menu:create`
- **请求体**: `SysMenuSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| parentId | Long | 是 | 父菜单 ID，根节点传 `0` |
| treePath | String | 否 | 前端通常无需传，后端会重新计算 |
| name | String | 是 | 菜单名称 |
| type | String | 是 | `C` / `M` / `B` |
| routeName | String | 否 | 路由名称 |
| routePath | String | 否 | 路由路径 |
| component | String | 否 | 组件路径 |
| perm | String | 否 | 权限标识 |
| alwaysShow | Integer | 否 | 是否始终显示 |
| keepAlive | Integer | 否 | 是否缓存 |
| visible | Integer | 否 | 是否显示 |
| sort | Integer | 否 | 排序 |
| icon | String | 否 | 图标 |
| redirect | String | 否 | 跳转地址 |
| params | Object | 否 | 路由参数 |

### 5.4 修改菜单

- **PUT** `/api/sys/menus/{id}`
- **权限**: `sys:menu:update`
- **请求体**: `SysMenuSaveRequest`

### 5.5 删除菜单

- **DELETE** `/api/sys/menus/{id}`
- **权限**: `sys:menu:delete`

## 6. 系统配置管理

### 6.1 分页查询配置

- **GET** `/api/sys/configs`
- **权限**: `sys:config:query`
- **查询参数**: `SysConfigPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页条数 |
| configName | String | 配置名称 |
| configKey | String | 配置键 |
| createTimeStart | DateTime | 创建开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| createTimeEnd | DateTime | 创建结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

- **响应项**: `SysConfigAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 配置 ID |
| configName | String | 配置名称 |
| configKey | String | 配置键 |
| configValue | String | 配置值 |
| remark | String | 备注 |
| createTime | DateTime | 创建时间 |
| updateTime | DateTime | 更新时间 |

### 6.2 查询配置详情

- **GET** `/api/sys/configs/{id}`
- **权限**: `sys:config:query`

### 6.3 新增配置

- **POST** `/api/sys/configs`
- **权限**: `sys:config:create`
- **请求体**: `SysConfigSaveRequest`

```json
{
  "configName": "站点标题",
  "configKey": "site.title",
  "configValue": "我的博客",
  "remark": "前台站点标题"
}
```

### 6.4 修改配置

- **PUT** `/api/sys/configs/{id}`
- **权限**: `sys:config:update`
- **请求体**: `SysConfigSaveRequest`

### 6.5 删除配置

- **DELETE** `/api/sys/configs/{id}`
- **权限**: `sys:config:delete`

### 6.6 按配置键查询配置值

- **GET** `/api/sys/configs/key/{configKey}`
- **权限**: `sys:config:query`
- **响应**: `String`

## 7. 通知后台管理

### 7.1 分页查询通知

- **GET** `/api/sys/notices`
- **权限**: `sys:notice:query`
- **查询参数**: `SysNoticePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页条数 |
| title | String | 标题 |
| publishStatus | Integer | 发布状态 |
| targetType | Integer | 目标类型 |

- **响应项**: `SysNoticeAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 通知 ID |
| title | String | 标题 |
| content | String | 内容 |
| type | Integer | 通知类型 |
| level | String | 通知等级 |
| targetType | Integer | 目标类型，`1` 全体，`2` 指定 |
| targetUserIds | List<Long> | 目标用户 ID 列表 |
| publisherId | Long | 发布人 ID |
| publishStatus | Integer | 发布状态 |
| publishTime | DateTime | 发布时间 |
| revokeTime | DateTime | 撤回时间 |
| createTime | DateTime | 创建时间 |
| updateTime | DateTime | 更新时间 |

### 7.2 查询通知详情

- **GET** `/api/sys/notices/{id}`
- **权限**: `sys:notice:query`

### 7.3 新增通知

- **POST** `/api/sys/notices`
- **权限**: `sys:notice:create`
- **请求体**: `SysNoticeSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | String | 是 | 通知标题 |
| content | String | 是 | 通知内容 |
| type | Integer | 是 | 通知类型 |
| level | String | 是 | 通知等级 |
| targetType | Integer | 是 | 目标类型，`1` 全体，`2` 指定 |
| targetUserIds | List<Long> | 否 | 指定用户列表 |

### 7.4 修改通知

- **PUT** `/api/sys/notices/{id}`
- **权限**: `sys:notice:update`
- **请求体**: `SysNoticeSaveRequest`

### 7.5 发布通知

- **POST** `/api/sys/notices/{id}/publish`
- **权限**: `sys:notice:publish`
- **响应**: `data = null`

### 7.6 撤回通知

- **POST** `/api/sys/notices/{id}/revoke`
- **权限**: `sys:notice:revoke`
- **响应**: `data = null`

### 7.7 删除通知

- **DELETE** `/api/sys/notices/{id}`
- **权限**: `sys:notice:delete`

## 8. 用户通知中心

### 8.1 我的通知列表

- **GET** `/api/user/notices`
- **是否鉴权**: 是
- **查询参数**: `UserNoticePageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页条数 |
| title | String | 标题 |
| isRead | Integer | 已读状态，`0` 未读，`1` 已读 |

- **响应项**: `UserNoticeVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 通知 ID |
| title | String | 标题 |
| content | String | 内容 |
| type | Integer | 通知类型 |
| level | String | 通知等级 |
| publishTime | DateTime | 发布时间 |
| isRead | Integer | 是否已读 |
| readTime | DateTime | 阅读时间 |

### 8.2 我的通知详情

- **GET** `/api/user/notices/{id}`
- **是否鉴权**: 是
- **说明**: 获取详情时会按当前实现更新阅读状态。

### 8.3 我的未读数

- **GET** `/api/user/notices/unread-count`
- **是否鉴权**: 是
- **响应**: `Long`

### 8.4 单条已读

- **POST** `/api/user/notices/{id}/read`
- **是否鉴权**: 是

### 8.5 全部已读

- **POST** `/api/user/notices/read-all`
- **是否鉴权**: 是

## 9. 系统日志管理

### 9.1 分页查询日志

- **GET** `/api/sys/logs`
- **权限**: `sys:log:query`
- **查询参数**: `SysLogPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| current | Long | 页码 |
| size | Long | 每页条数 |
| module | String | 日志模块 |
| requestMethod | String | 请求方式 |
| requestUri | String | 请求路径 |
| ip | String | IP |
| createBy | Long | 创建人 ID |
| createTimeStart | DateTime | 创建开始时间 |
| createTimeEnd | DateTime | 创建结束时间 |

- **响应项**: `SysLogAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 日志 ID |
| module | String | 日志模块 |
| requestMethod | String | 请求方式 |
| requestParams | String | 请求参数 |
| responseContent | String | 响应内容 |
| content | String | 日志内容 |
| requestUri | String | 请求路径 |
| method | String | 处理方法 |
| ip | String | IP 地址 |
| province | String | 省份 |
| city | String | 城市 |
| executionTime | Long | 执行耗时（ms） |
| browser | String | 浏览器 |
| browserVersion | String | 浏览器版本 |
| os | String | 操作系统 |
| createBy | Long | 创建人 ID |
| createTime | DateTime | 创建时间 |

### 9.2 查询日志详情

- **GET** `/api/sys/logs/{id}`
- **权限**: `sys:log:query`

### 9.3 删除日志

- **DELETE** `/api/sys/logs/{id}`
- **权限**: `sys:log:delete`

### 9.4 按条件清理日志

- **POST** `/api/sys/logs/clean`
- **权限**: `sys:log:clean`
- **请求体**: `SysLogCleanRequest`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| module | String | 日志模块 |
| requestMethod | String | 请求方式 |
| requestUri | String | 请求路径 |
| ip | String | IP |
| createBy | Long | 创建人 ID |
| createTimeStart | DateTime | 创建开始时间 |
| createTimeEnd | DateTime | 创建结束时间 |

- **响应**: `Long`，表示清理数量。

## 10. 权限标识速查

| 权限 | 说明 |
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

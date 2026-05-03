# 认证与系统管理 API

这份文档给前端使用，覆盖三类场景：

- 登录、注册、刷新令牌、退出登录
- 后台管理台初始化所需的当前用户与菜单接口
- 后台系统管理页面，以及登录用户的通知中心

## 1. 快速接入

### 1.1 路由分组

| 路由前缀                   | 用途                  | 是否需要登录 |
|------------------------|---------------------|--------|
| `/api/auth/**`         | 登录、注册、刷新令牌、获取当前登录用户 | 部分需要   |
| `/api/auth/takeover/**` | 账号接管认证接口           | 需要     |
| `/api/users/**`        | 公开用户 / 作者主页摘要接口     | 否      |
| `/api/admin/**`        | 超级管理员操作接口           | 需要     |
| `/api/sys/**`          | 后台管理接口              | 需要     |
| `/api/user/author-applications/**` | 登录用户作者申请接口         | 需要     |
| `/api/user/notification-settings/**` | 登录用户通知设置接口         | 需要     |
| `/api/user/notices/**` | 登录用户通知中心            | 需要     |

### 1.2 匿名可访问接口

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/email-code`
- `POST /api/auth/email-login`
- `POST /api/auth/refresh`
- `GET /api/users/{userId}/author-profile`

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

| 场景                   | 建议                                  |
|----------------------|-------------------------------------|
| HTTP `401`           | 尝试刷新令牌；刷新失败后回到登录页                   |
| HTTP `403`           | 已登录但无权限，提示无权限或跳到 403 页面             |
| `Result.code != 200` | 按业务失败处理，优先展示 `message`              |
| 刷新接口成功               | 同时替换 `accessToken` 和 `refreshToken` |

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

| 字段         | 类型     | 必填 | 说明               |
|------------|--------|----|------------------|
| `username` | String | 是  | 支持用户名 / 邮箱 / 手机号 |
| `password` | String | 是  | 登录密码             |

- 响应：`AuthenticationToken`

| 字段             | 类型      | 说明                 |
|----------------|---------|--------------------|
| `tokenType`    | String  | 固定为 `Bearer`       |
| `accessToken`  | String  | 访问令牌               |
| `refreshToken` | String  | 刷新令牌               |
| `expiresIn`    | Integer | `accessToken` 过期秒数 |

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

- 当前行为补充：
    - 支持通过系统配置 `auth.login-fail.max-attempts` 和 `auth.login-fail.lock-minutes` 控制“连续失败锁定”。
    - 默认口径为连续失败 `5` 次后锁定 `15` 分钟；配置值 `<= 0` 时表示关闭该锁定能力。
    - 账号已被临时锁定时，接口会返回 `40104 / 账号已锁定`。

### 3.2 账号注册

- 请求：`POST /api/auth/register`
- 鉴权：否
- 用途：注册页
- 请求体：`AuthRegisterRequest`

| 字段         | 类型     | 必填 | 说明         |
|------------|--------|----|------------|
| `username` | String | 是  | 用户名        |
| `password` | String | 是  | 密码，8-64 位，需包含大小写字母和数字 |
| `nickname` | String | 否  | 未传时默认使用用户名 |
| `email`    | String | 否  | 邮箱         |
| `phone`    | String | 否  | 手机号        |

- 响应：同 `AuthenticationToken`
- 边界说明：
    - 用户名 / 邮箱 / 手机号仍由数据库唯一约束兜底，遇到并发注册竞争时，接口会继续返回与单线程校验一致的重复提示，而不是裸露数据库异常。
- 响应同 3.1 登录接口。

### 3.3 发送邮箱验证码

- 请求：`POST /api/auth/email-code`
- 鉴权：否
- 用途：邮箱验证码登录前先发送验证码
- 请求体：`AuthEmailCodeRequest`

| 字段      | 类型     | 必填 | 说明   |
|---------|--------|----|------|
| `email` | String | 是  | 邮箱地址 |

- 响应：`data = null`
- 当前行为补充：
    - 同一邮箱默认 `60` 秒内只能发送一次，超频会返回 `40115 / 发送过于频繁，请稍后再试`。
    - 验证码默认 `5` 分钟过期，过期后重新申请会覆盖旧验证码。

### 3.4 邮箱验证码登录

- 请求：`POST /api/auth/email-login`
- 鉴权：否
- 用途：邮箱验证码登录
- 请求体：`AuthEmailLoginRequest`

| 字段      | 类型     | 必填 | 说明     |
|---------|--------|----|--------|
| `email` | String | 是  | 邮箱地址   |
| `code`  | String | 是  | 6 位验证码 |

- 响应：同 `AuthenticationToken`
- 边界说明：
    - 过期验证码会返回 `40113 / 邮箱验证码已过期`。
    - 成功登录后，当前邮箱验证码会立即失效，避免重复消费。
- 响应同 3.1 登录接口。

### 3.5 刷新令牌

- 请求：`POST /api/auth/refresh`
- 鉴权：否
- 用途：令牌续期
- 请求体：`AuthRefreshRequest`

| 字段             | 类型     | 必填 | 说明   |
|----------------|--------|----|------|
| `refreshToken` | String | 是  | 刷新令牌 |

- 响应：同 `AuthenticationToken`
- 当前行为补充：
    - 当 `security.session.type=redis-token` 时，刷新成功会使旧的 `accessToken / refreshToken` 同步失效。
    - 当 `security.session.type=jwt` 时，刷新属于纯无状态换发，服务端不会主动回收旧 JWT。
- 响应同 3.1 登录接口。

### 3.6 退出登录

- 请求：`POST /api/auth/logout`
- 鉴权：建议带 Bearer Token
- 用途：主动退出登录
- 请求体：`LogoutRequest`，可为空

| 字段            | 类型     | 必填 | 说明                          |
|---------------|--------|----|-----------------------------|
| `accessToken` | String | 否  | 不传时默认读取 `Authorization` 请求头 |

- 前端说明：
    - 大多数场景只需要携带请求头即可。
    - 即使接口失败，前端通常也应清理本地登录态，避免残留脏状态。
    - 当 `security.session.type=redis-token` 时，退出会让当前会话对应的访问令牌和刷新令牌失效。
    - 当 `security.session.type=jwt` 时，当前实现仅作为前端幂等退出入口，服务端不会回收已签发 JWT。

### 3.7 获取当前登录用户

- 请求：`GET /api/auth/current-user`
- 鉴权：是
- 用途：应用启动初始化用户信息、权限码、角色码
- 响应：`AuthUserInfo`

| 字段            | 类型           | 说明            |
|---------------|--------------|---------------|
| `id`          | Long         | 用户 ID         |
| `username`    | String       | 用户名           |
| `nickname`    | String       | 昵称            |
| `avatar`      | String       | 头像            |
| `email`       | String       | 邮箱            |
| `phone`       | String       | 手机号           |
| `status`      | Integer      | `0` 禁用，`1` 正常 |
| `userLevel`   | Integer      | 用户等级，当前默认 `1` |
| `experiencePoints` | Integer | 当前经验值，当前默认 `0` |
| `roles`       | List<String> | 角色编码列表        |
| `permissions` | List<String> | 权限标识列表        |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://example.com/avatar/1.jpg",
    "email": "zhangsan@example.com",
    "phone": "13800000001",
    "status": 1,
    "userLevel": 2,
    "experiencePoints": 150,
    "roles": ["author", "user"],
    "permissions": ["article:create", "article:update", "article:delete", "comment:create"]
  }
}
```

### 3.8 获取当前用户菜单

- 请求：`GET /api/auth/current-user-menus`
- 鉴权：是
- 用途：后台动态路由、侧边菜单、按钮权限挂载
- 响应：`List<AuthMenuInfo>`

| 字段           | 类型                 | 说明                   |
|--------------|--------------------|----------------------|
| `id`         | Long               | 菜单 ID                |
| `parentId`   | Long               | 父菜单 ID               |
| `name`       | String             | 菜单名称                 |
| `type`       | String             | `C` 目录、`M` 菜单、`B` 按钮 |
| `routeName`  | String             | 路由名称                 |
| `routePath`  | String             | 路由路径                 |
| `component`  | String             | 前端组件路径               |
| `perm`       | String             | 权限标识                 |
| `visible`    | Integer            | 是否显示，`0/1`           |
| `sort`       | Integer            | 排序                   |
| `icon`       | String             | 图标                   |
| `redirect`   | String             | 重定向路径                |
| `alwaysShow` | Integer            | 是否始终显示               |
| `keepAlive`  | Integer            | 是否缓存                 |
| `params`     | Object             | 额外路由参数               |
| `children`   | List<AuthMenuInfo> | 子节点                  |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "系统管理",
      "type": "C",
      "routeName": "System",
      "routePath": "/system",
      "component": "Layout",
      "perm": null,
      "visible": 1,
      "sort": 1,
      "icon": "setting",
      "redirect": "/system/user",
      "alwaysShow": 1,
      "keepAlive": 0,
      "params": null,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "name": "用户管理",
          "type": "M",
          "routeName": "SystemUser",
          "routePath": "user",
          "component": "system/user/index",
          "perm": null,
          "visible": 1,
          "sort": 1,
          "icon": "user",
          "redirect": null,
          "alwaysShow": 0,
          "keepAlive": 1,
          "params": null,
          "children": [
            {
              "id": 10,
              "parentId": 2,
              "name": "用户新增",
              "type": "B",
              "routeName": null,
              "routePath": null,
              "component": null,
              "perm": "sys:user:create",
              "visible": 1,
              "sort": 1,
              "icon": null,
              "redirect": null,
              "alwaysShow": 0,
              "keepAlive": 0,
              "params": null,
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

## 4. 后台应用启动流程

后台管理台通常要用到下面两组数据：

| 接口                                 | 用途                 |
|------------------------------------|--------------------|
| `GET /api/auth/current-user`       | 初始化头像、昵称、角色、按钮权限   |
| `GET /api/auth/current-user-menus` | 渲染动态菜单、动态路由、页面缓存配置 |

前端常见处理规则：

- `type=B` 的菜单一般不渲染路由，用于按钮权限控制。
- `visible=0` 可用于隐藏侧边栏，但仍保留路由配置。
- `keepAlive=1` 可映射前端页面缓存开关。
- `params` 可直接作为扩展路由元数据使用。

## 5. 公开作者主页

### 5.1 页面会用到哪些接口

| 场景 | 接口 |
| --- | --- |
| 公开作者主页摘要 | `GET /api/users/{userId}/author-profile` |

### 5.2 查询公开作者主页摘要

- 请求：`GET /api/users/{userId}/author-profile`
- 鉴权：否
- 路径参数：`userId`
- 响应：`PublicAuthorProfileVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像 |
| `userLevel` | Integer | 用户等级 |
| `author` | Boolean | 是否具备作者身份 |
| `authorBadge` | String | 作者标识，当前作者固定返回 `author`，普通用户返回 `null` |
| `publicArticleCount` | Long | 当前可公开展示的文章数 |
| `publicSeriesCount` | Long | 当前可公开展示的系列数 |
| `showcaseArticleIds` | List<Long> | 作品展示位文章 ID 列表，当前阶段预留为空 |
| `representativeArticleIds` | List<Long> | 代表内容文章 ID 列表，当前阶段预留为空 |
| `featuredSeriesIds` | List<Long> | 系列展示位系列 ID 列表，当前阶段预留为空 |
| `featuredColumnIds` | List<Long> | 专栏展示位 ID 列表，当前阶段预留为空 |

- 响应示例：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "userId": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "https://example.com/avatar/1.jpg",
    "userLevel": 3,
    "author": true,
    "authorBadge": "author",
    "publicArticleCount": 42,
    "publicSeriesCount": 5,
    "showcaseArticleIds": [],
    "representativeArticleIds": [],
    "featuredSeriesIds": [],
    "featuredColumnIds": []
  }
}
```

- 当前行为补充：
    - 仅允许查询未删除、已启用用户。
    - 公开文章数只统计真正可匿名访问的文章：已发布、审核通过或未送审、公开可见、非访问受限、且定时发布时间已到。
    - 公开系列数只统计启用且公开可见的系列。
    - 当前阶段先返回身份与计数摘要，不在该接口内直接编排关注关系、作品卡片详情和专栏详情。

## 6. 登录用户作者申请

### 6.1 页面会用到哪些接口

| 场景 | 接口 |
| --- | --- |
| 提交作者申请 | `POST /api/user/author-applications` |
| 查看最近一次申请 | `GET /api/user/author-applications/latest` |
| 查看申请记录 | `GET /api/user/author-applications` |

### 6.2 提交作者申请

- 请求：`POST /api/user/author-applications`
- 鉴权：是
- 请求体：`UserAuthorApplicationSubmitRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `applyReason` | String | 是 | 申请说明，最长 `512` 字符 |
| `contentDirection` | String | 是 | 擅长内容方向，最长 `128` 字符 |
| `introduction` | String | 否 | 个人简介，最长 `1024` 字符 |
| `sampleLinks` | List<String> | 否 | 示例链接，最多 `10` 条，需为 `http/https` |

- 当前行为补充：
    - 最近一次申请为 `待审核` 时禁止重复提交。
    - 最近一次申请为 `待补充` 时，重新提交会复用原申请并重置为 `待审核`。
    - 当前用户已有作者角色或最近一次申请已通过时，不允许重复申请。

### 6.3 查看最近一次申请

- 请求：`GET /api/user/author-applications/latest`
- 鉴权：是
- 响应：`UserAuthorApplicationVO`，无记录时 `data = null`

### 6.4 查看我的申请记录

- 请求：`GET /api/user/author-applications`
- 鉴权：是
- 查询参数：`UserAuthorApplicationPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10`，最大 `100` |

- 响应项：`UserAuthorApplicationVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 申请 ID |
| `applyStatus` | Integer | `0` 待审核，`1` 已通过，`2` 已拒绝，`3` 待补充 |
| `applyStatusLabel` | String | 状态文案 |
| `applyReason` | String | 申请说明 |
| `contentDirection` | String | 擅长内容方向 |
| `introduction` | String | 个人简介 |
| `sampleLinks` | List<String> | 示例链接列表 |
| `reviewerId` | Long | 审核人 ID |
| `reviewComment` | String | 审核备注 |
| `submittedAt` | DateTime | 提交时间 |
| `reviewedAt` | DateTime | 审核时间 |

## 7. 登录用户通知中心

### 7.1 通知设置接口

| 场景 | 接口 |
| --- | --- |
| 查询我的通知设置 | `GET /api/user/notification-settings` |
| 批量更新通知设置 | `PUT /api/user/notification-settings` |
| 更新单类通知设置 | `PUT /api/user/notification-settings/{type}` |

#### 查询我的通知设置

- 请求：`GET /api/user/notification-settings`
- 鉴权：是
- 响应：`List<UserNotificationSettingItemVO>`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | String | 通知类型编码 |
| `label` | String | 通知类型名称 |
| `enabled` | Boolean | 是否启用 |

当前已支持的通知类型：

- `comment_me`
- `like_me`
- `collect_article`
- `follow_me`
- `private_message`
- `group_mention`
- `channel_announcement`
- `system_announcement`
- `ai_task_done`

#### 批量更新通知设置

- 请求：`PUT /api/user/notification-settings`
- 鉴权：是
- 请求体：`UserNotificationSettingBatchUpdateRequest`

```json
{
  "settings": [
    {
      "type": "follow_me",
      "enabled": false
    },
    {
      "type": "private_message",
      "enabled": true
    }
  ]
}
```

#### 更新单类通知设置

- 请求：`PUT /api/user/notification-settings/{type}`
- 鉴权：是
- 路径参数：`type`
- 请求体：`UserNotificationSettingStatusUpdateRequest`

```json
{
  "enabled": false
}
```

- 当前行为补充：
    - 用户首次访问或首次更新时，如配置不存在，后端会自动生成一份默认开启的通知偏好。
    - 非法通知类型会被拦截并返回业务错误。
    - 当前已接入偏好过滤的链路：`follow_me`、`comment_me`、`like_me`、`collect_article`、`private_message`、`group_mention`、`channel_announcement`。
    - 评论、点赞、收藏、私聊、群 @、频道公告通知会在业务事务提交后投递，通知失败不会回滚主业务链路。
    - 群 @ 第一阶段按文本中的 `@用户ID` 解析，仅对当前会话内活跃成员投递。
    - 频道公告通知在主题频道公告内容变更且新公告非空时投递给频道活跃成员。
    - 用户关闭 `system_announcement` 后，全局系统公告不进入“我的通知”列表和未读数，也不能打开详情；定向业务通知仍按各自类型的投递开关处理。

### 7.2 用户等级与经验值

| 场景 | 接口 |
| --- | --- |
| 查看当前等级信息 | `GET /api/user/experience/level` |

#### 查看当前等级信息

- 请求：`GET /api/user/experience/level`
- 鉴权：是
- 响应：`UserLevelInfoVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `level` | Integer | 当前等级 |
| `currentExperience` | Long | 当前经验值 |
| `nextLevelExperience` | Long | 下一级所需经验值 |
| `levelTitle` | String | 等级称号 |
| `progress` | Double | 升级进度，0.0 ~ 1.0 |
| `dailyExperienceLimit` | Long | 今日经验获取上限 |
| `dailyExperienceUsed` | Long | 今日已获取经验 |
| `dailyExperienceRemaining` | Long | 今日剩余可获取经验 |

- 当前行为补充：
    - 每日经验有上限配置，超出上限后当日不再累计。
    - 进度按当前经验占下一级所需经验的百分比计算。

### 7.3 页面会用到哪些接口

| 场景           | 接口                                   |
|--------------|--------------------------------------|
| 通知列表页        | `GET /api/user/notices`              |
| 右上角未读角标      | `GET /api/user/notices/unread-count` |
| 通知详情抽屉 / 详情页 | `GET /api/user/notices/{id}`         |
| 单条标记已读       | `POST /api/user/notices/{id}/read`   |
| 全部标记已读       | `POST /api/user/notices/read-all`    |

### 7.4 我的通知列表

- 请求：`GET /api/user/notices`
- 鉴权：是
- 查询参数：`UserNoticePageQuery`

| 参数       | 类型     | 说明               |
|-----------|---------|-------------------|
| `current` | Long    | 页码，默认 `1`        |
| `size`    | Long    | 每页条数，默认 `10`     |
| `title`   | String  | 标题关键词             |
| `isRead`  | Integer | `0` 未读，`1` 已读     |

- 响应项：`UserNoticeVO`

| 字段            | 类型       | 说明    |
|---------------|----------|-------|
| `id`          | Long     | 通知 ID |
| `title`       | String   | 标题    |
| `content`     | String   | 内容    |
| `type`        | Integer  | 通知类型  |
| `level`       | String   | 通知等级  |
| `publishTime` | DateTime | 发布时间  |
| `isRead`      | Integer  | 是否已读  |
| `readTime`    | DateTime | 阅读时间  |

### 7.5 我的通知详情

- 请求：`GET /api/user/notices/{id}`
- 鉴权：是
- 路径参数：`id`
- 前端注意：按当前实现，读取详情会顺带更新阅读状态。

### 7.6 未读数量

- 请求：`GET /api/user/notices/unread-count`
- 鉴权：是
- 响应：`Long`

### 7.7 单条已读

- 请求：`POST /api/user/notices/{id}/read`
- 鉴权：是
- 响应：`data = null`

### 7.8 全部已读

- 请求：`POST /api/user/notices/read-all`
- 鉴权：是
- 响应：`data = null`

## 8. 后台系统管理接口

这一组接口主要对应后台管理台的系统模块。所有接口都要求登录，并且需要对应权限码。

### 8.1 用户管理

#### 接口速览

| 场景     | 方法     | 路径                                   | 权限                        |
|--------|--------|--------------------------------------|---------------------------|
| 分页查询用户 | GET    | `/api/sys/users`                     | `sys:user:query`          |
| 查询用户详情 | GET    | `/api/sys/users/{id}`                | `sys:user:query`          |
| 新增用户   | POST   | `/api/sys/users`                     | `sys:user:create`         |
| 修改用户   | PUT    | `/api/sys/users/{id}`                | `sys:user:update`         |
| 修改用户状态 | PUT    | `/api/sys/users/{id}/status`         | `sys:user:update`         |
| 重置用户密码 | PUT    | `/api/sys/users/{id}/password/reset` | `sys:user:reset-password` |
| 删除用户   | DELETE | `/api/sys/users/{id}`                | `sys:user:delete`         |
| 查询用户角色 | GET    | `/api/sys/users/{id}/roles`          | `sys:user:query`          |
| 分配用户角色 | PUT    | `/api/sys/users/{id}/roles`          | `sys:user:assign-role`    |

#### 分页查询用户

- 请求：`GET /api/sys/users`
- 查询参数：`SysUserPageQuery`

| 参数         | 类型      | 说明           |
|------------|---------|--------------|
| `current`  | Long    | 页码，默认 `1`    |
| `size`     | Long    | 每页条数，默认 `10` |
| `username` | String  | 用户名          |
| `nickname` | String  | 昵称           |
| `email`    | String  | 邮箱           |
| `phone`    | String  | 手机号          |
| `status`   | Integer | 状态           |

- 响应项：`SysUserAdminVO`

| 字段              | 类型         | 说明       |
|-----------------|------------|----------|
| `id`            | Long       | 用户 ID    |
| `username`      | String     | 用户名      |
| `nickname`      | String     | 昵称       |
| `email`         | String     | 邮箱       |
| `phone`         | String     | 手机号      |
| `avatar`        | String     | 头像       |
| `gender`        | Integer    | 性别       |
| `birthday`      | DateTime   | 生日       |
| `status`        | Integer    | 状态       |
| `userLevel`     | Integer    | 用户等级     |
| `experiencePoints` | Integer | 经验值      |
| `levelUpdatedAt` | DateTime  | 最近一次等级变更时间 |
| `lastLoginTime` | DateTime   | 最后登录时间   |
| `lastLoginIp`   | String     | 最后登录 IP  |
| `remark`        | String     | 备注       |
| `roleIds`       | List<Long> | 角色 ID 列表 |

#### 新增 / 修改用户

- 新增：`POST /api/sys/users`
- 修改：`PUT /api/sys/users/{id}`
- 请求体：`SysUserSaveRequest`

| 字段         | 类型       | 新增必填 | 修改必填 | 说明               |
|------------|----------|------|------|------------------|
| `username` | String   | 是    | 否    | 用户名              |
| `password` | String   | 是    | 否    | 8-64 位，需包含大小写字母和数字；修改时不生效，重置密码走单独接口 |
| `nickname` | String   | 否    | 否    | 昵称               |
| `email`    | String   | 否    | 否    | 邮箱               |
| `phone`    | String   | 否    | 否    | 手机号              |
| `avatar`   | String   | 否    | 否    | 头像               |
| `gender`   | Integer  | 否    | 否    | 性别               |
| `birthday` | DateTime | 否    | 否    | 生日               |
| `status`   | Integer  | 否    | 否    | 默认 `1`           |
| `remark`   | String   | 否    | 否    | 备注               |

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
  "password": "Abc12345"
}
```

- 查询角色：`GET /api/sys/users/{id}/roles`
- 分配角色：`PUT /api/sys/users/{id}/roles`

```json
{
  "roleIds": [1, 2]
}
```

### 8.2 角色管理

#### 接口速览

| 场景     | 方法     | 路径                           | 权限                     |
|--------|--------|------------------------------|------------------------|
| 分页查询角色 | GET    | `/api/sys/roles`             | `sys:role:query`       |
| 查询角色详情 | GET    | `/api/sys/roles/{id}`        | `sys:role:query`       |
| 新增角色   | POST   | `/api/sys/roles`             | `sys:role:create`      |
| 修改角色   | PUT    | `/api/sys/roles/{id}`        | `sys:role:update`      |
| 修改角色状态 | PUT    | `/api/sys/roles/{id}/status` | `sys:role:update`      |
| 删除角色   | DELETE | `/api/sys/roles/{id}`        | `sys:role:delete`      |
| 查询角色菜单 | GET    | `/api/sys/roles/{id}/menus`  | `sys:role:query`       |
| 分配角色菜单 | PUT    | `/api/sys/roles/{id}/menus`  | `sys:role:assign-menu` |

#### 角色分页字段

- 请求：`GET /api/sys/roles`
- 查询参数：`SysRolePageQuery`

| 参数        | 类型      | 说明   |
|-----------|---------|------|
| `current` | Long    | 页码   |
| `size`    | Long    | 每页条数 |
| `name`    | String  | 角色名称 |
| `code`    | String  | 角色编码 |
| `status`  | Integer | 角色状态 |

- 响应项：`SysRoleAdminVO`

| 字段          | 类型         | 说明       |
|-------------|------------|----------|
| `id`        | Long       | 角色 ID    |
| `name`      | String     | 角色名称     |
| `code`      | String     | 角色编码     |
| `sort`      | Integer    | 显示顺序     |
| `status`    | Integer    | 状态       |
| `dataScope` | Integer    | 数据权限     |
| `menuIds`   | List<Long> | 菜单 ID 列表 |

#### 新增 / 修改角色

- 新增：`POST /api/sys/roles`
- 修改：`PUT /api/sys/roles/{id}`
- 请求体：`SysRoleSaveRequest`

| 字段          | 类型      | 必填 | 说明     |
|-------------|---------|----|--------|
| `name`      | String  | 是  | 角色名称   |
| `code`      | String  | 是  | 角色编码   |
| `sort`      | Integer | 否  | 显示顺序   |
| `status`    | Integer | 否  | 默认 `1` |
| `dataScope` | Integer | 否  | 数据权限   |

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

### 8.3 菜单管理

#### 接口速览

| 场景     | 方法     | 路径                    | 权限                |
|--------|--------|-----------------------|-------------------|
| 查询菜单树  | GET    | `/api/sys/menus/tree` | `sys:menu:query`  |
| 查询菜单详情 | GET    | `/api/sys/menus/{id}` | `sys:menu:query`  |
| 新增菜单   | POST   | `/api/sys/menus`      | `sys:menu:create` |
| 修改菜单   | PUT    | `/api/sys/menus/{id}` | `sys:menu:update` |
| 删除菜单   | DELETE | `/api/sys/menus/{id}` | `sys:menu:delete` |

#### 菜单树响应

- 请求：`GET /api/sys/menus/tree`
- 响应：`List<SysMenuAdminVO>`

| 字段           | 类型                   | 说明     |
|--------------|----------------------|--------|
| `id`         | Long                 | 菜单 ID  |
| `parentId`   | Long                 | 父菜单 ID |
| `treePath`   | String               | 树路径    |
| `name`       | String               | 菜单名称   |
| `type`       | String               | 菜单类型   |
| `routeName`  | String               | 路由名称   |
| `routePath`  | String               | 路由路径   |
| `component`  | String               | 组件路径   |
| `perm`       | String               | 权限标识   |
| `alwaysShow` | Integer              | 是否始终显示 |
| `keepAlive`  | Integer              | 是否缓存   |
| `visible`    | Integer              | 是否显示   |
| `sort`       | Integer              | 排序     |
| `icon`       | String               | 图标     |
| `redirect`   | String               | 跳转地址   |
| `params`     | Object               | 路由参数   |
| `children`   | List<SysMenuAdminVO> | 子节点    |

#### 新增 / 修改菜单

- 新增：`POST /api/sys/menus`
- 修改：`PUT /api/sys/menus/{id}`
- 请求体：`SysMenuSaveRequest`

| 字段           | 类型      | 必填 | 说明              |
|--------------|---------|----|-----------------|
| `parentId`   | Long    | 是  | 根节点传 `0`        |
| `treePath`   | String  | 否  | 前端通常无需传，后端会重新计算 |
| `name`       | String  | 是  | 菜单名称            |
| `type`       | String  | 是  | `C` / `M` / `B` |
| `routeName`  | String  | 否  | 路由名称            |
| `routePath`  | String  | 否  | 路由路径            |
| `component`  | String  | 否  | 组件路径            |
| `perm`       | String  | 否  | 权限标识            |
| `alwaysShow` | Integer | 否  | 是否始终显示          |
| `keepAlive`  | Integer | 否  | 是否缓存            |
| `visible`    | Integer | 否  | 是否显示            |
| `sort`       | Integer | 否  | 排序              |
| `icon`       | String  | 否  | 图标              |
| `redirect`   | String  | 否  | 跳转地址            |
| `params`     | Object  | 否  | 路由参数            |

### 8.4 系统配置管理

#### 接口速览

| 场景          | 方法     | 路径                                 | 权限                  |
|-------------|--------|------------------------------------|---------------------|
| 分页查询配置      | GET    | `/api/sys/configs`                 | `sys:config:query`  |
| 查询配置详情      | GET    | `/api/sys/configs/{id}`            | `sys:config:query`  |
| 新增配置        | POST   | `/api/sys/configs`                 | `sys:config:create` |
| 修改配置        | PUT    | `/api/sys/configs/{id}`            | `sys:config:update` |
| 删除配置        | DELETE | `/api/sys/configs/{id}`            | `sys:config:delete` |
| 按 key 查询配置值 | GET    | `/api/sys/configs/key/{configKey}` | `sys:config:query`  |

#### 分页查询配置

- 请求：`GET /api/sys/configs`
- 查询参数：`SysConfigPageQuery`

| 参数                | 类型       | 说明                              |
|-------------------|----------|---------------------------------|
| `current`         | Long     | 页码                              |
| `size`            | Long     | 每页条数                            |
| `configName`      | String   | 配置名称                            |
| `configKey`       | String   | 配置键                             |
| `createTimeStart` | DateTime | 创建开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `createTimeEnd`   | DateTime | 创建结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

- 响应项：`SysConfigAdminVO`

| 字段            | 类型       | 说明    |
|---------------|----------|-------|
| `id`          | Long     | 配置 ID |
| `configName`  | String   | 配置名称  |
| `configKey`   | String   | 配置键   |
| `configValue` | String   | 配置值   |
| `remark`      | String   | 备注    |
| `createTime`  | DateTime | 创建时间  |
| `updateTime`  | DateTime | 更新时间  |

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

- 内置安全相关配置键：
- `security.ip.rate-limit.per-second`：全局 IP 每秒请求限流阈值。
- 默认值：`10`。
- 配置值 `<= 0` 时表示关闭该限流。
- `auth.login-fail.max-attempts`：连续登录失败锁定阈值，默认 `5`。
- 配置值 `<= 0` 时表示关闭登录失败锁定能力。
- `auth.login-fail.lock-minutes`：登录失败达到阈值后的锁定时长（分钟），默认 `15`。

### 8.5 后台通知管理

#### 接口速览

| 场景     | 方法     | 路径                              | 权限                   |
|--------|--------|---------------------------------|----------------------|
| 分页查询通知 | GET    | `/api/sys/notices`              | `sys:notice:query`   |
| 查询通知详情 | GET    | `/api/sys/notices/{id}`         | `sys:notice:query`   |
| 新增通知   | POST   | `/api/sys/notices`              | `sys:notice:create`  |
| 修改通知   | PUT    | `/api/sys/notices/{id}`         | `sys:notice:update`  |
| 发布通知   | POST   | `/api/sys/notices/{id}/publish` | `sys:notice:publish` |
| 撤回通知   | POST   | `/api/sys/notices/{id}/revoke`  | `sys:notice:revoke`  |
| 删除通知   | DELETE | `/api/sys/notices/{id}`         | `sys:notice:delete`  |

#### 分页查询通知

- 请求：`GET /api/sys/notices`
- 查询参数：`SysNoticePageQuery`

| 参数              | 类型      | 说明   |
|-----------------|---------|------|
| `current`       | Long    | 页码   |
| `size`          | Long    | 每页条数 |
| `title`         | String  | 标题   |
| `publishStatus` | Integer | 发布状态 |
| `targetType`    | Integer | 目标类型 |

- 响应项：`SysNoticeAdminVO`

| 字段              | 类型         | 说明              |
|-----------------|------------|-----------------|
| `id`            | Long       | 通知 ID           |
| `title`         | String     | 标题              |
| `content`       | String     | 内容              |
| `type`          | Integer    | 通知类型            |
| `level`         | String     | 通知等级            |
| `targetType`    | Integer    | `1` 全体，`2` 指定用户 |
| `targetUserIds` | List<Long> | 指定用户列表          |
| `publisherId`   | Long       | 发布人 ID          |
| `publishStatus` | Integer    | 发布状态            |
| `publishTime`   | DateTime   | 发布时间            |
| `revokeTime`    | DateTime   | 撤回时间            |
| `createTime`    | DateTime   | 创建时间            |
| `updateTime`    | DateTime   | 更新时间            |

#### 新增 / 修改通知

- 请求体：`SysNoticeSaveRequest`

| 字段              | 类型         | 必填 | 说明            |
|-----------------|------------|----|---------------|
| `title`         | String     | 是  | 通知标题          |
| `content`       | String     | 是  | 通知内容          |
| `type`          | Integer    | 是  | 通知类型          |
| `level`         | String     | 是  | 通知等级          |
| `targetType`    | Integer    | 是  | `1` 全体，`2` 指定 |
| `targetUserIds` | List<Long> | 否  | 指定用户时使用       |

### 8.6 后台数据看板

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 核心概览 | GET | `/api/sys/dashboard/overview` | `sys:dashboard:query` |
| 内容统计 | GET | `/api/sys/dashboard/content` | `sys:dashboard:query` |
| 社区统计 | GET | `/api/sys/dashboard/community` | `sys:dashboard:query` |
| AI 调用统计 | GET | `/api/sys/dashboard/ai` | `sys:dashboard:query` |
| 治理统计 | GET | `/api/sys/dashboard/governance` | `sys:dashboard:query` |

通用查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `rangeType` | String | `today/week/month/all/custom`，默认 `today` |
| `startTime` | DateTime | 自定义开始时间，`rangeType=custom` 时必填 |
| `endTime` | DateTime | 自定义结束时间，`rangeType=custom` 时必填 |

指标口径：

- 范围统计使用左闭右开区间：`[startTime, endTime)`。
- `today/week/month` 分别按服务器本地日期的今日、本周一、本月一开始统计。
- `custom` 最大跨度 366 天，`all` 不限制时间范围。
- 活跃用户定义：在范围内登录、发文、评论、发送聊天消息或产生 AI 调用任一行为的去重用户。
- 发文数按文章创建时间统计；待审核文章数为当前 `review_status = 1` 的存量。
- 聊天消息数包含已撤回消息；大厅消息数统计全站会话或 `global_channel/hall_channel` 场景消息。
- AI 调用数按 `ai_usage_log` 请求记录统计，并按 `success_status` 区分成功 / 失败。
- 举报单数按 `reported_at` 统计；待处理 / 处理中为当前存量，已处理 / 已驳回按范围统计。

核心概览响应字段：

| 字段 | 说明 |
| --- | --- |
| `range` | 实际统计范围 |
| `registeredUserCount` | 范围内注册用户数 |
| `activeUserCount` | 范围内活跃用户数 |
| `authorCount` | 当前作者数量 |
| `articleCount` | 范围内发文数 |
| `pendingArticleReviewCount` | 当前待审核文章数 |
| `commentCount` | 范围内评论数 |
| `chatMessageCount` | 范围内聊天消息数 |
| `aiCallCount` | 范围内 AI 调用数 |
| `reportCount` | 范围内举报单数 |
| `pendingReportCount` | 当前待处理举报数 |

内容统计响应字段：`articleCount`、`pendingArticleReviewCount`、`commentCount`、`likeCount`、`collectCount`。

社区统计响应字段：`chatMessageCount`、`lobbyMessageCount`、`groupCount`。

AI 统计响应字段：`aiCallCount`、`aiSuccessCallCount`、`aiFailedCallCount`。

治理统计响应字段：`reportCount`、`pendingReportCount`、`processingReportCount`、`handledReportCount`、`rejectedReportCount`。

### 8.7 系统日志管理

#### 接口速览

| 场景      | 方法     | 路径                    | 权限               |
|---------|--------|-----------------------|------------------|
| 分页查询日志  | GET    | `/api/sys/logs`       | `sys:log:query`  |
| 查询日志详情  | GET    | `/api/sys/logs/{id}`  | `sys:log:query`  |
| 删除日志    | DELETE | `/api/sys/logs/{id}`  | `sys:log:delete` |
| 按条件清理日志 | POST   | `/api/sys/logs/clean` | `sys:log:clean`  |

#### 分页查询日志

- 请求：`GET /api/sys/logs`
- 查询参数：`SysLogPageQuery`

| 参数                | 类型       | 说明     |
|-------------------|----------|--------|
| `current`         | Long     | 页码     |
| `size`            | Long     | 每页条数   |
| `module`          | String   | 日志模块   |
| `requestMethod`   | String   | 请求方式   |
| `requestUri`      | String   | 请求路径   |
| `ip`              | String   | IP     |
| `createBy`        | Long     | 创建人 ID |
| `createTimeStart` | DateTime | 创建开始时间 |
| `createTimeEnd`   | DateTime | 创建结束时间 |

- 响应项：`SysLogAdminVO`

| 字段                | 类型       | 说明       |
|-------------------|----------|----------|
| `id`              | Long     | 日志 ID    |
| `module`          | String   | 日志模块     |
| `requestMethod`   | String   | 请求方式     |
| `requestParams`   | String   | 请求参数     |
| `responseContent` | String   | 响应内容     |
| `content`         | String   | 日志内容     |
| `requestUri`      | String   | 请求路径     |
| `method`          | String   | 处理方法     |
| `ip`              | String   | IP 地址    |
| `province`        | String   | 省份       |
| `city`            | String   | 城市       |
| `executionTime`   | Long     | 执行耗时（ms） |
| `browser`         | String   | 浏览器      |
| `browserVersion`  | String   | 浏览器版本    |
| `os`              | String   | 操作系统     |
| `createBy`        | Long     | 创建人 ID   |
| `createTime`      | DateTime | 创建时间     |

#### 条件清理日志

- 请求：`POST /api/sys/logs/clean`
- 请求体：`SysLogCleanRequest`

| 字段                | 类型       | 说明     |
|-------------------|----------|--------|
| `module`          | String   | 日志模块   |
| `requestMethod`   | String   | 请求方式   |
| `requestUri`      | String   | 请求路径   |
| `ip`              | String   | IP     |
| `createBy`        | Long     | 创建人 ID |
| `createTimeStart` | DateTime | 创建开始时间 |
| `createTimeEnd`   | DateTime | 创建结束时间 |

- 响应：`Long`，表示清理数量

### 8.8 作者申请后台管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询作者申请 | GET | `/api/sys/author-applications` | `sys:author-application:query` |
| 查询作者申请详情 | GET | `/api/sys/author-applications/{id}` | `sys:author-application:query` |
| 审核作者申请 | PUT | `/api/sys/author-applications/{id}/review` | `sys:author-application:review` |
| 修正作者申请状态 | PUT | `/api/sys/author-applications/{id}/repair` | `sys:author-application:repair` |

#### 分页查询作者申请

- 请求：`GET /api/sys/author-applications`
- 查询参数：`SysAuthorApplicationAdminPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10`，最大 `100` |
| `userId` | Long | 申请用户 ID |
| `applyStatus` | Integer | 申请状态 |
| `keyword` | String | 关键词，匹配申请说明、内容方向和个人简介 |

#### 作者申请详情 / 分页项

- 响应项：`SysAuthorApplicationAdminVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 申请 ID |
| `userId` | Long | 申请用户 ID |
| `username` | String | 申请用户名 |
| `nickname` | String | 申请用户昵称 |
| `applyStatus` | Integer | `0` 待审核，`1` 已通过，`2` 已拒绝，`3` 待补充 |
| `applyStatusLabel` | String | 状态文案 |
| `applyReason` | String | 申请说明 |
| `contentDirection` | String | 擅长内容方向 |
| `introduction` | String | 个人简介 |
| `sampleLinks` | List<String> | 示例链接 |
| `reviewerId` | Long | 审核人 ID |
| `reviewerUsername` | String | 审核人用户名 |
| `reviewerNickname` | String | 审核人昵称 |
| `reviewComment` | String | 审核备注 |
| `submittedAt` | DateTime | 提交时间 |
| `reviewedAt` | DateTime | 审核时间 |

#### 审核作者申请

- 请求：`PUT /api/sys/author-applications/{id}/review`
- 请求体：`SysAuthorApplicationAdminReviewRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `reviewStatus` | Integer | 是 | `1` 通过，`2` 拒绝，`3` 待补充 |
| `reviewComment` | String | 否 | 审核备注，最长 `512` 字符 |

- 当前行为补充：
    - 仅 `待审核` 状态允许审核，其他状态会被拦截。
    - 审核通过后自动授予用户 `author` 角色。
    - 审核动作会记录审核人、审核时间和审核备注。

#### 修正作者申请状态

- 请求：`PUT /api/sys/author-applications/{id}/repair`
- 请求体：`SysAuthorApplicationRepairRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetStatus` | Integer | 是 | `0` 待审核，`1` 已通过，`2` 已拒绝，`3` 待补充 |
| `reviewComment` | String | 是 | 修正备注，最长 `512` 字符 |

- 当前行为补充：
    - 该接口只允许具备 `sys:author-application:repair` 权限的超级管理员使用。
    - 可对任意当前状态执行修正，但目标状态不能与当前状态一致。
    - 修正为 `已通过` 时会补授 `author` 角色；修正为其他状态时会撤销 `author` 角色。
    - 修正动作会覆盖申请单的审核人、审核时间和审核备注，用于收口异常数据。

#### 作者差异化发文配额说明

- 当前实现已通过系统配置区分普通用户和作者的文章总量上限。
- 默认配置：
    - `article.max-count.normal-user=20`
    - `article.max-count.author=200`
- 当文章数量达到上限时，统一在现有文章创建链路中拦截。
- 若配置值为 `0`，表示该身份类型不限制文章总量。

### 8.9 经验体系管理

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 查看用户经验来源汇总 | GET | `/api/sys/experience/users/{userId}/summary` | `sys:experience:query` |
| 经验流水分页查询 | GET | `/api/sys/experience/logs` | `sys:experience:query` |
| 手动调整等级或经验 | POST | `/api/sys/experience/users/{userId}/adjust` | `sys:experience:adjust` |
| 查看经验来源配置 | GET | `/api/sys/experience/config` | `sys:experience:config` |
| 更新经验来源配置 | PUT | `/api/sys/experience/config` | `sys:experience:config` |

#### 查看用户经验来源汇总

- 请求：`GET /api/sys/experience/users/{userId}/summary`
- 路径参数：`userId`
- 响应字段：`UserExperienceSummaryVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `level` | Integer | 当前等级 |
| `currentExperience` | Long | 当前经验值 |
| `nextLevelExperience` | Long | 下一级所需经验值 |
| `dailySummary` | Map<String, Object> | 今日各来源经验汇总 |

#### 经验流水分页查询

- 请求：`GET /api/sys/experience/logs`
- 查询参数：`ExperienceLogPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10` |
| `userId` | Long | 用户 ID |
| `sourceType` | String | 经验来源类型 |

- 响应字段：`ExperienceLogVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 日志 ID |
| `userId` | Long | 用户 ID |
| `sourceType` | String | 来源类型 |
| `sourceTypeLabel` | String | 来源类型标签 |
| `experienceChange` | Long | 经验变化量 |
| `experienceBefore` | Long | 变化前经验值 |
| `experienceAfter` | Long | 变化后经验值 |
| `levelBefore` | Integer | 变化前等级 |
| `levelAfter` | Integer | 变化后等级 |
| `description` | String | 描述 |
| `createdAt` | DateTime | 创建时间 |

#### 手动调整等级或经验

- 请求：`POST /api/sys/experience/users/{userId}/adjust`
- 请求体：`UserLevelAdjustRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `adjustType` | String | 是 | `level` 或 `experience` |
| `newValue` | Long | 是 | 新的等级值或经验值 |
| `reason` | String | 否 | 调整原因 |

#### 经验来源配置

- 查看配置：`GET /api/sys/experience/config`
- 更新配置：`PUT /api/sys/experience/config`

请求体：
```json
{
  "configKey": "experience.source.article.publish",
  "configValue": "10"
}
```

### 8.10 审计日志管理

审计日志记录超级管理员的敏感操作（封禁/解封、等级调整、角色分配等），仅超级管理员可访问。

#### 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 分页查询审计日志 | GET | `/api/sys/audit-logs` | `sys:audit:query` |
| 查询审计日志详情 | GET | `/api/sys/audit-logs/{id}` | `sys:audit:query` |

#### 分页查询审计日志

- 请求：`GET /api/sys/audit-logs`
- 查询参数：`SysAuditLogPageQuery`

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10` |
| `operatorUserId` | Long | 操作人 ID |
| `targetUserId` | Long | 目标用户 ID |
| `operationType` | String | 操作类型 |

- 响应：`PageResult<SysAuditLogAdminVO>`

#### 审计日志详情 / 分页项

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键 |
| `operatorUserId` | Long | 操作人 ID |
| `operatorUsername` | String | 操作人用户名 |
| `targetUserId` | Long | 目标用户 ID |
| `targetUsername` | String | 目标用户名 |
| `operationType` | String | 操作类型 |
| `operationTypeDesc` | String | 操作类型描述 |
| `targetTypeName` | String | 目标对象类型名称 |
| `targetId` | Long | 目标对象 ID |
| `beforeState` | String | 操作前状态 |
| `afterState` | String | 操作后状态 |
| `mfaPassed` | Integer | 2FA 是否通过 |
| `requestIp` | String | 请求 IP |
| `userAgent` | String | User-Agent |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |

## 9. 权限标识速查

| 权限标识                      | 说明        |
|---------------------------|-----------|
| `sys:user:query`          | 查询用户      |
| `sys:user:create`         | 新增用户      |
| `sys:user:update`         | 修改用户、修改状态 |
| `sys:user:ban`            | 封禁用户（同时用于 2FA 发送/校验） |
| `sys:user:unban`          | 解封用户 |
| `sys:user:adjust-level`   | 调整用户等级 |
| `sys:user:adjust-experience` | 调整用户经验 |
| `sys:user:takeover`       | 账号接管 |
| `sys:user:delete`         | 删除用户      |
| `sys:user:reset-password` | 重置用户密码    |
| `sys:user:assign-role`    | 分配用户角色、带审计的角色分配    |
| `sys:author-application:query` | 查询作者申请 |
| `sys:author-application:review` | 审核作者申请 |
| `sys:author-application:repair` | 修正作者申请状态 |
| `sys:role:query`          | 查询角色      |
| `sys:role:create`         | 新增角色      |
| `sys:role:update`         | 修改角色、修改状态 |
| `sys:role:delete`         | 删除角色      |
| `sys:role:assign-menu`    | 分配角色菜单    |
| `sys:menu:query`          | 查询菜单      |
| `sys:menu:create`         | 新增菜单      |
| `sys:menu:update`         | 修改菜单      |
| `sys:menu:delete`         | 删除菜单      |
| `sys:config:query`        | 查询配置      |
| `sys:config:create`       | 新增配置      |
| `sys:config:update`       | 修改配置      |
| `sys:config:delete`       | 删除配置      |
| `sys:notice:query`        | 查询通知      |
| `sys:notice:create`       | 新增通知      |
| `sys:notice:update`       | 修改通知      |
| `sys:notice:publish`      | 发布通知      |
| `sys:notice:revoke`       | 撤回通知      |
| `sys:notice:delete`       | 删除通知      |
| `sys:dashboard:query`     | 查询数据看板    |
| `sys:log:query`           | 查询日志      |
| `sys:log:delete`          | 删除日志      |
| `sys:log:clean`           | 清理日志      |
| `sys:audit:query`         | 查询审计日志    |
| `sys:experience:query`    | 查询经验相关      |
| `sys:experience:adjust`   | 调整用户等级/经验  |
| `sys:experience:config`   | 管理经验来源配置  |

## 10. 超级管理员操作接口

这一组接口是超级管理员专属操作，包含 2FA 二次验证、用户封禁/解封、等级与经验调整、账号接管、带审计的角色分配等。

### 10.1 接口速览

| 场景 | 方法 | 路径 | 权限 |
| --- | --- | --- | --- |
| 发送2FA验证码 | POST | `/api/admin/2fa/send-code` | `sys:user:ban` |
| 校验2FA验证码 | POST | `/api/admin/2fa/verify` | `sys:user:ban` |
| 封禁用户 | POST | `/api/admin/users/{id}/ban` | `sys:user:ban` |
| 解封用户 | POST | `/api/admin/users/{id}/unban` | `sys:user:unban` |
| 调整用户等级 | PUT | `/api/admin/users/{id}/level` | `sys:user:adjust-level` |
| 调整用户经验 | PUT | `/api/admin/users/{id}/experience` | `sys:user:adjust-experience` |
| 账号接管 | POST | `/api/admin/takeover` | `sys:user:takeover` |
| 带审计的角色分配 | PUT | `/api/admin/users/{id}/roles` | `sys:user:assign-role` |

### 10.2 2FA 二次验证

#### 发送2FA验证码

- 请求：`POST /api/admin/2fa/send-code`
- 鉴权：是
- 用途：超级管理员操作前先获取2FA验证码
- 响应：`data = null`
- 当前行为补充：
    - 验证码默认 `60` 秒内只能发送一次，超频会返回业务错误。
    - 验证码默认 `5` 分钟过期。

#### 校验2FA验证码

- 请求：`POST /api/admin/2fa/verify`
- 鉴权：是
- 请求体：`MfaVerifyRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | String | 是 | 6 位验证码 |

- 响应：`MfaVerifyResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ticket` | String | 2FA 票据，用于后续敏感操作 |
| `expiresIn` | Long | 票据有效期秒数，默认 30 分钟 |

### 10.3 用户封禁与解封

#### 封禁用户

- 请求：`POST /api/admin/users/{id}/ban`
- 鉴权：是，需要 `mfaTicket`
- 请求体：`BanUserRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |
| `banReason` | String | 否 | 封禁原因 |

- 当前行为补充：
    - 封禁后会强制使目标用户当前会话失效。
    - 需要有效的 `mfaTicket` 才能执行封禁操作。

#### 解封用户

- 请求：`POST /api/admin/users/{id}/unban`
- 鉴权：是，需要 `mfaTicket`
- 请求体：`BanUserRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |
| `unbanReason` | String | 否 | 解封原因 |

### 10.4 用户等级与经验调整

#### 调整用户等级

- 请求：`PUT /api/admin/users/{id}/level`
- 鉴权：是，需要 `mfaTicket`
- 请求体：`AdjustLevelRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `level` | Integer | 是 | 目标等级 |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |

#### 调整用户经验

- 请求：`PUT /api/admin/users/{id}/experience`
- 鉴权：是，需要 `mfaTicket`
- 请求体：`AdjustExperienceRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `experience` | Long | 是 | 目标经验值 |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |

### 10.5 账号接管

- 请求：`POST /api/admin/takeover`
- 鉴权：是，需要 `mfaTicket`
- 用途：超级管理员临时接管为目标用户身份进行操作
- 请求体：`AccountTakeoverRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `targetUserId` | Long | 是 | 目标用户 ID |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |

- 响应：`AccountTakeoverResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `takeoverToken` | String | 接管令牌 |
| `expiresIn` | Long | 接管令牌有效期秒数 |

- 当前行为补充：
    - 接管令牌用于 `/api/auth/takeover/login` 接口登录。
    - 接管令牌一次性使用，使用后失效。
    - 所有敏感操作需要先通过 2FA 验证并提供 `mfaTicket`。

### 10.6 带审计的角色分配

- 请求：`PUT /api/admin/users/{id}/roles`
- 鉴权：是，需要 `mfaTicket`
- 请求体：`UserRoleAuditAssignRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `roleIds` | List<Long> | 是 | 角色 ID 列表 |
| `mfaTicket` | String | 是 | 2FA 校验通过的票据 |

- 当前行为补充：
    - 该接口在普通角色分配基础上追加审计日志。
    - 需要有效的 `mfaTicket` 才能执行操作。

## 11. 账号接管认证接口

### 11.1 使用接管令牌登录

- 请求：`POST /api/auth/takeover/login`
- 鉴权：否
- 用途：超级管理员使用接管令牌登录为目标用户身份
- 请求体：`TakeoverLoginRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `takeoverToken` | String | 是 | 接管令牌 |

- 响应：同 `AuthenticationToken`

## 12. 常见联调问题

| 问题                          | 当前行为             |
|-----------------------------|------------------|
| 匿名访问 `/api/sys/**`          | HTTP `401`       |
| 匿名访问 `/api/user/notices/**` | HTTP `401`       |
| 已登录但权限不足                    | HTTP `403`       |
| 打开通知详情后未读数变化                | 详情接口按当前实现会更新已读状态 |
| 修改用户时传了 `password`          | 不生效，必须走重置密码接口    |

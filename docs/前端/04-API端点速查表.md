# API 端点速查表

> 全部 161 个 API 端点，按模块分组。认证列中"公开"表示无需登录，"登录"表示需要登录但无特定权限，权限标识表示需要对应权限。

## 4.1 认证模块 (auth)

| # | 方法   | 路径                             | 说明      | 认证 |
|---|------|--------------------------------|---------|----|
| 1 | POST | `/api/auth/login`              | 账号登录    | 公开 |
| 2 | POST | `/api/auth/register`           | 账号注册    | 公开 |
| 3 | POST | `/api/auth/email-code`         | 发送邮箱验证码 | 公开 |
| 4 | POST | `/api/auth/email-login`        | 邮箱验证码登录 | 公开 |
| 5 | POST | `/api/auth/refresh`            | 刷新令牌    | 公开 |
| 6 | POST | `/api/auth/logout`             | 退出登录    | 登录 |
| 7 | GET  | `/api/auth/current-user`       | 获取当前用户  | 登录 |
| 8 | GET  | `/api/auth/current-user-menus` | 获取当前菜单树 | 登录 |

## 4.2 后台用户管理

| # | 方法     | 路径                                   | 说明     | 权限                      |
|---|--------|--------------------------------------|--------|-------------------------|
| 1 | GET    | `/api/sys/users`                     | 分页查询用户 | sys:user:query          |
| 2 | GET    | `/api/sys/users/{id}`                | 用户详情   | sys:user:query          |
| 3 | POST   | `/api/sys/users`                     | 新增用户   | sys:user:create         |
| 4 | PUT    | `/api/sys/users/{id}`                | 修改用户   | sys:user:update         |
| 5 | PUT    | `/api/sys/users/{id}/status`         | 修改状态   | sys:user:update         |
| 6 | PUT    | `/api/sys/users/{id}/password/reset` | 重置密码   | sys:user:reset-password |
| 7 | DELETE | `/api/sys/users/{id}`                | 删除用户   | sys:user:delete         |
| 8 | GET    | `/api/sys/users/{id}/roles`          | 查询用户角色 | sys:user:query          |
| 9 | PUT    | `/api/sys/users/{id}/roles`          | 分配角色   | sys:user:assign-role    |

## 4.3 后台角色管理

| # | 方法     | 路径                           | 说明     | 权限                   |
|---|--------|------------------------------|--------|----------------------|
| 1 | GET    | `/api/sys/roles`             | 分页查询角色 | sys:role:query       |
| 2 | GET    | `/api/sys/roles/{id}`        | 角色详情   | sys:role:query       |
| 3 | POST   | `/api/sys/roles`             | 新增角色   | sys:role:create      |
| 4 | PUT    | `/api/sys/roles/{id}`        | 修改角色   | sys:role:update      |
| 5 | PUT    | `/api/sys/roles/{id}/status` | 修改状态   | sys:role:update      |
| 6 | DELETE | `/api/sys/roles/{id}`        | 删除角色   | sys:role:delete      |
| 7 | GET    | `/api/sys/roles/{id}/menus`  | 查询角色菜单 | sys:role:query       |
| 8 | PUT    | `/api/sys/roles/{id}/menus`  | 分配菜单   | sys:role:assign-menu |

## 4.4 后台菜单管理

| # | 方法     | 路径                    | 说明   | 权限              |
|---|--------|-----------------------|------|-----------------|
| 1 | GET    | `/api/sys/menus/tree` | 菜单树  | sys:menu:query  |
| 2 | GET    | `/api/sys/menus/{id}` | 菜单详情 | sys:menu:query  |
| 3 | POST   | `/api/sys/menus`      | 新增菜单 | sys:menu:create |
| 4 | PUT    | `/api/sys/menus/{id}` | 修改菜单 | sys:menu:update |
| 5 | DELETE | `/api/sys/menus/{id}` | 删除菜单 | sys:menu:delete |

## 4.5 后台配置管理

| # | 方法     | 路径                                 | 说明     | 权限                |
|---|--------|------------------------------------|--------|-------------------|
| 1 | GET    | `/api/sys/configs`                 | 分页查询配置 | sys:config:query  |
| 2 | GET    | `/api/sys/configs/{id}`            | 配置详情   | sys:config:query  |
| 3 | POST   | `/api/sys/configs`                 | 新增配置   | sys:config:create |
| 4 | PUT    | `/api/sys/configs/{id}`            | 修改配置   | sys:config:update |
| 5 | DELETE | `/api/sys/configs/{id}`            | 删除配置   | sys:config:delete |
| 6 | GET    | `/api/sys/configs/key/{configKey}` | 按key查询 | sys:config:query  |

## 4.6 后台日志管理

| # | 方法     | 路径                    | 说明     | 权限             |
|---|--------|-----------------------|--------|----------------|
| 1 | GET    | `/api/sys/logs`       | 分页查询日志 | sys:log:query  |
| 2 | GET    | `/api/sys/logs/{id}`  | 日志详情   | sys:log:query  |
| 3 | DELETE | `/api/sys/logs/{id}`  | 删除日志   | sys:log:delete |
| 4 | POST   | `/api/sys/logs/clean` | 按条件清理  | sys:log:clean  |

## 4.7 后台通知管理

| # | 方法     | 路径                              | 说明     | 权限                 |
|---|--------|---------------------------------|--------|--------------------|
| 1 | GET    | `/api/sys/notices`              | 分页查询通知 | sys:notice:query   |
| 2 | GET    | `/api/sys/notices/{id}`         | 通知详情   | sys:notice:query   |
| 3 | POST   | `/api/sys/notices`              | 新增通知   | sys:notice:create  |
| 4 | PUT    | `/api/sys/notices/{id}`         | 修改通知   | sys:notice:update  |
| 5 | POST   | `/api/sys/notices/{id}/publish` | 发布通知   | sys:notice:publish |
| 6 | POST   | `/api/sys/notices/{id}/revoke`  | 撤回通知   | sys:notice:revoke  |
| 7 | DELETE | `/api/sys/notices/{id}`         | 删除通知   | sys:notice:delete  |

## 4.8 用户通知

| # | 方法   | 路径                               | 说明     | 认证 |
|---|------|----------------------------------|--------|----|
| 1 | GET  | `/api/user/notices`              | 我的通知列表 | 登录 |
| 2 | GET  | `/api/user/notices/{id}`         | 通知详情   | 登录 |
| 3 | GET  | `/api/user/notices/unread-count` | 未读数    | 登录 |
| 4 | POST | `/api/user/notices/{id}/read`    | 标记已读   | 登录 |
| 5 | POST | `/api/user/notices/read-all`     | 全部已读   | 登录 |

## 4.9 公开文章接口

| # | 方法  | 路径                   | 说明        | 认证 |
|---|-----|----------------------|-----------|----|
| 1 | GET | `/api/articles`      | 分页查询已发布文章 | 公开 |
| 2 | GET | `/api/articles/{id}` | 文章详情      | 公开 |

## 4.10 后台文章管理

| # | 方法     | 路径                              | 说明     | 权限                     |
|---|--------|---------------------------------|--------|------------------------|
| 1 | GET    | `/api/sys/articles`             | 分页查询文章 | content:article:query  |
| 2 | GET    | `/api/sys/articles/{id}`        | 文章详情   | content:article:query  |
| 3 | POST   | `/api/sys/articles`             | 新增文章   | content:article:create |
| 4 | PUT    | `/api/sys/articles/{id}`        | 修改文章   | content:article:update |
| 5 | PUT    | `/api/sys/articles/{id}/status` | 修改状态   | content:article:update |
| 6 | PUT    | `/api/sys/articles/{id}/access` | 配置访问权限 | content:article:access |
| 7 | DELETE | `/api/sys/articles/{id}`        | 删除文章   | content:article:delete |

## 4.11 用户文章行为

| # | 方法     | 路径                              | 说明   | 认证 |
|---|--------|---------------------------------|------|----|
| 1 | POST   | `/api/user/articles/{id}/likes` | 点赞   | 登录 |
| 2 | DELETE | `/api/user/articles/{id}/likes` | 取消点赞 | 登录 |

## 4.12 公开内容接口

| # | 方法  | 路径                     | 说明   | 认证 |
|---|-----|------------------------|------|----|
| 1 | GET | `/api/categories/tree` | 分类树  | 公开 |
| 2 | GET | `/api/tags`            | 标签列表 | 公开 |
| 3 | GET | `/api/comments`        | 评论树  | 公开 |

## 4.13 后台分类管理

| # | 方法     | 路径                                | 说明   | 权限                      |
|---|--------|-----------------------------------|------|-------------------------|
| 1 | GET    | `/api/sys/categories/tree`        | 分类树  | content:category:query  |
| 2 | GET    | `/api/sys/categories/{id}`        | 分类详情 | content:category:query  |
| 3 | POST   | `/api/sys/categories`             | 新增分类 | content:category:create |
| 4 | PUT    | `/api/sys/categories/{id}`        | 修改分类 | content:category:update |
| 5 | PUT    | `/api/sys/categories/{id}/status` | 修改状态 | content:category:update |
| 6 | DELETE | `/api/sys/categories/{id}`        | 删除分类 | content:category:delete |

## 4.14 后台标签管理

| # | 方法     | 路径                   | 说明   | 权限                 |
|---|--------|----------------------|------|--------------------|
| 1 | GET    | `/api/sys/tags`      | 标签列表 | content:tag:query  |
| 2 | GET    | `/api/sys/tags/{id}` | 标签详情 | content:tag:query  |
| 3 | POST   | `/api/sys/tags`      | 新增标签 | content:tag:create |
| 4 | PUT    | `/api/sys/tags/{id}` | 修改标签 | content:tag:update |
| 5 | DELETE | `/api/sys/tags/{id}` | 删除标签 | content:tag:delete |

## 4.15 后台评论管理

| # | 方法     | 路径                              | 说明     | 权限                     |
|---|--------|---------------------------------|--------|------------------------|
| 1 | GET    | `/api/sys/comments`             | 分页查询评论 | content:comment:query  |
| 2 | GET    | `/api/sys/comments/{id}`        | 评论详情   | content:comment:query  |
| 3 | PUT    | `/api/sys/comments/{id}/status` | 修改状态   | content:comment:update |
| 4 | DELETE | `/api/sys/comments/{id}`        | 删除评论   | content:comment:delete |

## 4.16 用户评论

| # | 方法     | 路径                              | 说明   | 认证 |
|---|--------|---------------------------------|------|----|
| 1 | POST   | `/api/user/comments/{id}/likes` | 点赞评论 | 登录 |
| 2 | DELETE | `/api/user/comments/{id}/likes` | 取消点赞 | 登录 |
| 3 | POST   | `/api/user/comments`            | 发表评论 | 登录 |
| 4 | DELETE | `/api/user/comments/{id}`       | 删除评论 | 登录 |

## 4.17 后台收藏管理

| # | 方法     | 路径                             | 说明     | 权限                        |
|---|--------|--------------------------------|--------|---------------------------|
| 1 | GET    | `/api/sys/collections/folders` | 收藏夹列表  | content:collection:query  |
| 2 | GET    | `/api/sys/collections`         | 收藏记录列表 | content:collection:query  |
| 3 | DELETE | `/api/sys/collections/{id}`    | 删除收藏   | content:collection:delete |

## 4.18 用户收藏

| # | 方法     | 路径                                  | 说明    | 认证 |
|---|--------|-------------------------------------|-------|----|
| 1 | GET    | `/api/user/collection-folders`      | 收藏夹列表 | 登录 |
| 2 | POST   | `/api/user/collection-folders`      | 新建收藏夹 | 登录 |
| 3 | PUT    | `/api/user/collection-folders/{id}` | 编辑收藏夹 | 登录 |
| 4 | DELETE | `/api/user/collection-folders/{id}` | 删除收藏夹 | 登录 |
| 5 | GET    | `/api/user/collections`             | 收藏列表  | 登录 |
| 6 | POST   | `/api/user/collections`             | 添加收藏  | 登录 |
| 7 | DELETE | `/api/user/collections/{id}`        | 删除收藏  | 登录 |

## 4.19 后台互动/足迹管理

| # | 方法     | 路径                           | 说明     | 权限                         |
|---|--------|------------------------------|--------|----------------------------|
| 1 | GET    | `/api/sys/interactions`      | 互动列表   | content:interaction:query  |
| 2 | DELETE | `/api/sys/interactions/{id}` | 删除互动   | content:interaction:delete |
| 3 | GET    | `/api/sys/footprints`        | 足迹列表   | content:footprint:query    |
| 4 | DELETE | `/api/sys/footprints/{id}`   | 删除足迹   | content:footprint:delete   |
| 5 | DELETE | `/api/sys/footprints`        | 批量清理足迹 | content:footprint:delete   |

## 4.20 用户足迹

| # | 方法     | 路径                          | 说明   | 认证 |
|---|--------|-----------------------------|------|----|
| 1 | GET    | `/api/user/footprints`      | 足迹列表 | 登录 |
| 2 | DELETE | `/api/user/footprints/{id}` | 删除足迹 | 登录 |
| 3 | DELETE | `/api/user/footprints`      | 清空足迹 | 登录 |

## 4.21 后台文件管理

| # | 方法     | 路径                            | 说明     | 权限                  |
|---|--------|-------------------------------|--------|---------------------|
| 1 | GET    | `/api/sys/files`              | 文件列表   | content:file:query  |
| 2 | GET    | `/api/sys/files/{id}`         | 文件详情   | content:file:query  |
| 3 | GET    | `/api/sys/files/upload-tasks` | 上传任务列表 | content:file:query  |
| 4 | PUT    | `/api/sys/files/{id}/status`  | 更新状态   | content:file:update |
| 5 | DELETE | `/api/sys/files/{id}`         | 删除文件   | content:file:delete |

## 4.22 用户文件

| # | 方法     | 路径                                                             | 说明     | 认证 |
|---|--------|----------------------------------------------------------------|--------|----|
| 1 | POST   | `/api/user/files/upload-tasks/init`                            | 初始化上传  | 登录 |
| 2 | POST   | `/api/user/files/upload-tasks/{uploadId}/quick-check`          | 秒传检测   | 登录 |
| 3 | POST   | `/api/user/files/upload-tasks/{uploadId}/file`                 | 普通上传   | 登录 |
| 4 | POST   | `/api/user/files/upload-tasks/{uploadId}/chunks/{chunkNumber}` | 分片上传   | 登录 |
| 5 | POST   | `/api/user/files/upload-tasks/{uploadId}/complete`             | 完成上传   | 登录 |
| 6 | GET    | `/api/user/files`                                              | 文件列表   | 登录 |
| 7 | GET    | `/api/user/files/upload-tasks`                                 | 上传任务   | 登录 |
| 8 | DELETE | `/api/user/files/{businessId}`                                 | 删除文件引用 | 登录 |

## 4.23 后台聊天管理

| #  | 方法   | 路径                                                            | 说明     | 权限                  |
|----|------|---------------------------------------------------------------|--------|---------------------|
| 1  | GET  | `/api/sys/chats/conversations`                                | 会话列表   | content:chat:query  |
| 2  | GET  | `/api/sys/chats/conversations/{id}`                           | 会话详情   | content:chat:query  |
| 3  | GET  | `/api/sys/chats/conversations/{id}/members`                   | 会话成员   | content:chat:query  |
| 4  | GET  | `/api/sys/chats/conversations/{id}/messages`                  | 消息列表   | content:chat:query  |
| 5  | GET  | `/api/sys/chats/conversations/{id}/messages/{msgId}`          | 消息详情   | content:chat:query  |
| 6  | GET  | `/api/sys/chats/conversations/{id}/messages/{msgId}/receipts` | 消息回执   | content:chat:query  |
| 7  | PUT  | `/api/sys/chats/conversations/{id}/members/{userId}/role`     | 更新成员角色 | content:chat:update |
| 8  | PUT  | `/api/sys/chats/conversations/{id}/members/{userId}/status`   | 更新成员状态 | content:chat:update |
| 9  | PUT  | `/api/sys/chats/conversations/{id}/members/{userId}/mute`     | 更新禁言   | content:chat:update |
| 10 | POST | `/api/sys/chats/conversations/{id}/messages/{msgId}/revoke`   | 撤回消息   | content:chat:update |
| 11 | PUT  | `/api/sys/chats/conversations/{id}/status`                    | 更新会话状态 | content:chat:update |

## 4.24 用户聊天

| #  | 方法     | 路径                                                 | 说明    | 认证 |
|----|--------|----------------------------------------------------|-------|----|
| 1  | GET    | `/api/user/chat/conversations`                     | 会话列表  | 登录 |
| 2  | GET    | `/api/user/chat/conversations/{id}`                | 会话详情  | 登录 |
| 3  | POST   | `/api/user/chat/single-conversations`              | 打开单聊  | 登录 |
| 4  | GET    | `/api/user/chat/conversations/{id}/messages`       | 消息历史  | 登录 |
| 5  | POST   | `/api/user/chat/messages/text`                     | 发送文本  | 登录 |
| 6  | POST   | `/api/user/chat/messages/file`                     | 发送文件  | 登录 |
| 7  | PUT    | `/api/user/chat/messages/{id}`                     | 编辑消息  | 登录 |
| 8  | POST   | `/api/user/chat/messages/{id}/revoke`              | 撤回消息  | 登录 |
| 9  | DELETE | `/api/user/chat/messages/{id}`                     | 删除消息  | 登录 |
| 10 | POST   | `/api/user/chat/conversations/{id}/read`           | 已读推进  | 登录 |
| 11 | POST   | `/api/user/chat/groups`                            | 创建群聊  | 登录 |
| 12 | GET    | `/api/user/chat/groups/{id}`                       | 群详情   | 登录 |
| 13 | GET    | `/api/user/chat/groups/{id}/members`               | 群成员   | 登录 |
| 14 | POST   | `/api/user/chat/groups/{id}/members`               | 邀请成员  | 登录 |
| 15 | PUT    | `/api/user/chat/groups/{id}/admins/{userId}`       | 设管理员  | 登录 |
| 16 | DELETE | `/api/user/chat/groups/{id}/admins/{userId}`       | 取消管理员 | 登录 |
| 17 | PUT    | `/api/user/chat/groups/{id}/owner`                 | 转让群主  | 登录 |
| 18 | PUT    | `/api/user/chat/groups/{id}/members/{userId}/mute` | 禁言    | 登录 |
| 19 | PUT    | `/api/user/chat/groups/{id}/notice`                | 更新群公告 | 登录 |
| 20 | DELETE | `/api/user/chat/groups/{id}/members/{userId}`      | 移除成员  | 登录 |
| 21 | POST   | `/api/user/chat/groups/{id}/leave`                 | 退出群聊  | 登录 |
| 22 | DELETE | `/api/user/chat/groups/{id}`                       | 解散群聊  | 登录 |

## 4.25 后台关注关系管理

| # | 方法     | 路径                       | 说明     | 权限                   |
|---|--------|--------------------------|--------|----------------------|
| 1 | GET    | `/api/sys/follows`       | 关注关系列表 | content:follow:query |
| 2 | DELETE | `/api/sys/follows/clean` | 清理异常数据 | content:follow:clean |

## 4.26 用户关注

| # | 方法     | 路径                                   | 说明     | 认证 |
|---|--------|--------------------------------------|--------|----|
| 1 | POST   | `/api/user/follows/{userId}`         | 关注     | 登录 |
| 2 | DELETE | `/api/user/follows/{userId}`         | 取消关注   | 登录 |
| 3 | GET    | `/api/user/follows`                  | 我的关注   | 登录 |
| 4 | GET    | `/api/user/fans`                     | 我的粉丝   | 登录 |
| 5 | GET    | `/api/user/follows/mutual`           | 互关状态   | 登录 |
| 6 | GET    | `/api/user/follows/count`            | 关注/粉丝数 | 登录 |
| 7 | PUT    | `/api/user/follows/{userId}/special` | 特别关注   | 登录 |
| 8 | PUT    | `/api/user/follows/{userId}/remark`  | 关注备注   | 登录 |

## 4.27 公开关注

| # | 方法  | 路径                            | 说明     | 认证 |
|---|-----|-------------------------------|--------|----|
| 1 | GET | `/api/users/{userId}/follows` | 指定用户关注 | 公开 |
| 2 | GET | `/api/users/{userId}/fans`    | 指定用户粉丝 | 公开 |

## 4.28 超级管理员操作

| # | 方法   | 路径                                     | 说明       | 权限 |
|---|------|----------------------------------------|----------|-----|
| 1 | POST | `/api/admin/2fa/send-code`             | 发送2FA验证码 | superadmin:2fa |
| 2 | POST | `/api/admin/2fa/verify`                 | 校验2FA验证码 | superadmin:2fa |
| 3 | POST | `/api/admin/users/{id}/ban`            | 封禁用户     | superadmin:ban |
| 4 | POST | `/api/admin/users/{id}/unban`          | 解封用户     | superadmin:ban |
| 5 | PUT  | `/api/admin/users/{id}/level`          | 调整用户等级   | superadmin:level |
| 6 | PUT  | `/api/admin/users/{id}/experience`     | 调整用户经验   | superadmin:experience |
| 7 | POST | `/api/admin/takeover`                  | 账号接管     | superadmin:takeover |
| 8 | PUT  | `/api/admin/users/{id}/roles`          | 带审计的角色分配 | superadmin:assign-role |

## 4.29 账号接管认证

| # | 方法   | 路径                           | 说明       | 认证 |
|---|------|------------------------------|----------|-----|
| 1 | POST | `/api/auth/takeover/login`   | 使用接管令牌登录 | 公开 |

## 4.30 公开大厅消息

| # | 方法  | 路径                              | 说明       | 认证 |
|---|-----|---------------------------------|----------|-----|
| 1 | GET | `/api/public/chat/lobby/messages` | 访客查看大厅消息 | 公开 |

## 统计

| 分类      | 端点数     |
|---------|---------|
| 认证模块    | 8       |
| 后台用户管理  | 9       |
| 后台角色管理  | 8       |
| 后台菜单管理  | 5       |
| 后台配置管理  | 6       |
| 后台日志管理  | 4       |
| 后台通知管理  | 7       |
| 用户通知    | 5       |
| 公开文章    | 2       |
| 后台文章管理  | 7       |
| 用户文章行为  | 2       |
| 公开内容    | 3       |
| 后台分类管理  | 6       |
| 后台标签管理  | 5       |
| 后台评论管理  | 4       |
| 用户评论    | 4       |
| 后台收藏管理  | 3       |
| 用户收藏    | 7       |
| 后台互动/足迹 | 5       |
| 用户足迹    | 3       |
| 后台文件管理  | 5       |
| 用户文件    | 8       |
| 后台聊天管理  | 11      |
| 用户聊天    | 22      |
| 后台关注管理  | 2       |
| 用户关注    | 8       |
| 公开关注       | 2       |
| 超级管理员操作   | 8       |
| 账号接管认证    | 1       |
| 公开大厅消息    | 1       |
| **合计**     | **161** |

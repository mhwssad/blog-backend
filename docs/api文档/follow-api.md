# 关注关系 API

这份文档面向前端的个人主页、关注列表、粉丝列表、互关状态、后台治理与公开访客查看场景。

## 1. 快速接入

### 1.1 路由范围

| 路由 | 用途 | 是否需要登录 |
| --- | --- | --- |
| `/api/user/follows/**` | 用户自己的关注操作、互关判断、统计、特别关注、备注 | 是 |
| `/api/user/fans` | 用户自己的粉丝列表 | 是 |
| `/api/users/{userId}/follows` | 公开查看指定用户的关注列表 | 否 |
| `/api/users/{userId}/fans` | 公开查看指定用户的粉丝列表 | 否 |
| `/api/sys/follows` | 后台关注关系分页查询 | 需要后台权限 |
| `/api/sys/follows/clean` | 后台异常关系清理 | 需要后台权限 |

### 1.2 当前实现规则

- 当前采用单向关注模型，不需要对方确认。
- 取关后重新关注会复用原关系记录，不新增第二条关系。
- 关注数、粉丝数当前使用实时 COUNT 统计，不写入用户表冗余字段。
- 自关注在数据库和业务层都会被拦截。
- 关注成功后会在主事务提交成功后，给被关注者补一条“新粉丝”通知；通知写入失败不会回滚关注主链路。
- 公开列表和统计都会过滤已删除、已禁用用户。
- 当前暂未将关注关系直接耦合到用户主页展示编排，也暂未引入 Redis 缓存层。

### 1.3 统一响应

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

## 2. 登录后用户接口

所有接口都要求：

```http
Authorization: Bearer <accessToken>
```

### 2.1 接口速览

| 场景 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 关注用户 | POST | `/api/user/follows/{userId}` | 幂等 |
| 取消关注 | DELETE | `/api/user/follows/{userId}` | 幂等 |
| 查询我的关注 | GET | `/api/user/follows` | 支持特别关注筛选 |
| 查询我的粉丝 | GET | `/api/user/fans` | 返回互关状态 |
| 查询互关状态 | GET | `/api/user/follows/mutual` | `targetUserId` 查询参数 |
| 查询关注统计 | GET | `/api/user/follows/count` | 返回关注数、粉丝数 |
| 设置/取消特别关注 | PUT | `/api/user/follows/{userId}/special` | 仅对有效关注关系生效 |
| 更新关注备注 | PUT | `/api/user/follows/{userId}/remark` | 空白字符串会被清空 |

### 2.2 关注与取消关注

- 关注用户：`POST /api/user/follows/{userId}`
- 取消关注：`DELETE /api/user/follows/{userId}`

成功响应：

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

关键规则：

- 已关注时再次关注保持幂等，不重复创建关系。
- 已取关时再次关注会恢复旧记录，并刷新 `followTime`。
- 已取关时再次取关保持幂等。
- 成功关注后会异步补一条新粉丝通知，不阻塞主链路响应。
- 不能关注自己。
- 目标用户不存在、已删除或已禁用时会返回业务错误。

### 2.3 我的关注列表

- 请求：`GET /api/user/follows`
- 查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10`，最大 `100` |
| `specialOnly` | Boolean | `true` 时仅返回特别关注 |

- 响应字段：`UserFollowUserVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `relationId` | Long | 关注关系 ID |
| `userId` | Long | 被关注用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像 |
| `isSpecialFollow` | Integer | 是否特别关注：`0/1` |
| `remark` | String | 备注 |
| `mutualFollow` | Integer | 是否互关：`0/1` |
| `followTime` | DateTime | 最近关注时间 |

当前行为：

- 只返回当前仍处于已关注状态的关系。
- 排序优先级为：特别关注优先，然后按最近关注时间倒序。
- 若被关注用户已删除或已禁用，该关系不会出现在列表和统计中。

### 2.4 我的粉丝列表

- 请求：`GET /api/user/fans`
- 查询参数：`current`、`size`
- 响应字段：`UserFollowUserVO`

字段说明与关注列表一致，但含义有两点差异：

- `userId` 表示粉丝用户 ID。
- `followTime` 表示对方关注我的时间。
- `isSpecialFollow` 和 `remark` 表示“我是否也关注对方，以及我对对方的特别关注/备注设置”；未互关时默认 `isSpecialFollow=0`、`remark=null`。

### 2.5 互关状态

- 请求：`GET /api/user/follows/mutual?targetUserId={id}`
- 响应字段：`UserFollowMutualVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `targetUserId` | Long | 目标用户 ID |
| `following` | Boolean | 当前用户是否已关注目标用户 |
| `followedBy` | Boolean | 目标用户是否已关注当前用户 |
| `mutualFollow` | Boolean | 是否互相关注 |

### 2.6 关注统计

- 请求：`GET /api/user/follows/count`
- 响应字段：`UserFollowCountVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `followingCount` | Long | 关注数 |
| `fanCount` | Long | 粉丝数 |

说明：

- 当前统计口径为实时 COUNT。
- 统计会过滤掉已删除、已禁用的目标用户。

### 2.7 特别关注

- 请求：`PUT /api/user/follows/{userId}/special`
- 请求体：`UserFollowSpecialUpdateRequest`

```json
{
  "specialFollow": 1
}
```

规则：

- `specialFollow` 仅允许 `0` 或 `1`。
- 仅对当前有效关注关系生效；若尚未关注，会返回业务异常。

### 2.8 备注

- 请求：`PUT /api/user/follows/{userId}/remark`
- 请求体：`UserFollowRemarkUpdateRequest`

```json
{
  "remark": "前端联调账号"
}
```

规则：

- 备注最大长度 `256`。
- 传空白字符串时会按清空备注处理。
- 仅对当前有效关注关系生效。

## 3. 公开访客接口

### 3.1 接口速览

| 场景 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 查看指定用户关注列表 | GET | `/api/users/{userId}/follows` | 匿名可访问 |
| 查看指定用户粉丝列表 | GET | `/api/users/{userId}/fans` | 匿名可访问 |

### 3.2 查询参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10`，最大 `100` |

### 3.3 响应字段

响应字段：`PublicFollowUserVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户 ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像 |
| `followTime` | DateTime | 关注发生时间 |

关键规则：

- 只允许查看“目标用户仍有效、关系仍有效、列表项用户仍有效”的公开结果。
- 公开接口不返回备注、特别关注、互关状态等仅用户本人可见的信息。
- 若路径中的 `userId` 对应用户不存在、已删除或已禁用，会返回业务错误。

## 4. 后台管理接口

所有后台接口都要求：

```http
Authorization: Bearer <adminAccessToken>
```

### 4.1 分页查询关注关系

- 请求：`GET /api/sys/follows`
- 权限：`content:follow:query`

查询参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `current` | Long | 页码，默认 `1` |
| `size` | Long | 每页条数，默认 `10`，最大 `100` |
| `followerId` | Long | 关注者用户 ID |
| `followingId` | Long | 被关注者用户 ID |
| `followStatus` | Integer | 关注状态：`0/1` |
| `specialFollow` | Integer | 是否特别关注：`0/1` |
| `source` | String | 关注来源 |
| `keyword` | String | 匹配双方用户名或昵称 |

响应字段：`FollowAdminRelationVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `relationId` | Long | 关系 ID |
| `followerId` | Long | 关注者 ID |
| `followerUsername` | String | 关注者用户名 |
| `followerNickname` | String | 关注者昵称 |
| `followerStatus` | Integer | 关注者状态 |
| `followerDeletedFlag` | Integer | 关注者是否已删除 |
| `followingId` | Long | 被关注者 ID |
| `followingUsername` | String | 被关注者用户名 |
| `followingNickname` | String | 被关注者昵称 |
| `followingStatus` | Integer | 被关注者状态 |
| `followingDeletedFlag` | Integer | 被关注者是否已删除 |
| `followStatus` | Integer | 关系状态 |
| `isSpecialFollow` | Integer | 是否特别关注 |
| `source` | String | 来源 |
| `remark` | String | 备注 |
| `followTime` | DateTime | 最近关注时间 |
| `unfollowTime` | DateTime | 最近取关时间 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 更新时间 |

说明：

- 后台分页会保留异常关系可见性，用于排查“用户已删 / 已禁用但关系仍存在”的数据。

### 4.2 异常关注清理

- 请求：`DELETE /api/sys/follows/clean`
- 权限：`content:follow:clean`
- 请求体：`FollowRelationCleanRequest`

```json
{
  "cleanInactive": true,
  "cleanDeletedUsers": false,
  "cleanDisabledUsers": true
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `cleanInactive` | Boolean | 是否清理已取关关系 |
| `cleanDeletedUsers` | Boolean | 是否清理任一端已删除或缺失用户的关系 |
| `cleanDisabledUsers` | Boolean | 是否清理任一端已禁用用户的关系 |

关键规则：

- 三个条件至少要有一个为 `true`。
- 返回值为本次命中的关系数量。
- 删除动作只收口异常关系，不参与普通用户前台取关流程。

## 5. 常见联调问题

| 问题 | 当前行为 |
| --- | --- |
| 匿名访问 `/api/user/**` 关注接口 | HTTP `401` |
| 匿名访问 `/api/users/{userId}/follows`、`/fans` | 允许 |
| 无后台权限访问 `/api/sys/follows` | HTTP `403` |
| 关注自己 | 返回业务错误 |
| 关注已禁用用户 | 返回业务错误 |
| 对未关注用户设置特别关注或备注 | 返回业务错误 |
| 重复关注同一用户 | 幂等成功，不新增第二条记录 |
| 关注成功后通知投递失败 | 不回滚关注主链路 |

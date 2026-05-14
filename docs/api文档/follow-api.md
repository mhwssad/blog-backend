# 关注关系 API - 前端参考手册

> 本文档面向前端联调开发，基于实际代码编写，涵盖关注列表、粉丝列表、公开访客查看、用户个人中心及后台管理全部接口。

---

## 目录

1. [统一说明](#1-统一说明)
2. [关注列表页](#2-关注列表页)
3. [粉丝列表页](#3-粉丝列表页)
4. [个人主页 - 公开关注列表](#4-个人主页---公开关注列表)
5. [个人主页 - 公开粉丝列表](#5-个人主页---公开粉丝列表)
6. [个人中心 - 互关状态与统计](#6-个人中心---互关状态与统计)
7. [个人中心 - 特别关注与备注](#7-个人中心---特别关注与备注)
8. [后台管理](#8-后台管理)
9. [错误码速查](#9-错误码速查)

---

## 1. 统一说明

### 1.1 统一响应格式

所有接口均返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `code` | Integer | 业务状态码，`200` 表示成功，非 `200` 表示失败 |
| `message` | String | 状态描述信息 |
| `timestamp` | Long | 响应时间戳（毫秒） |
| `data` | Object/Array | 响应数据，失败时通常为 `null` |

### 1.2 分页结构

分页查询接口的 `data` 固定为以下结构：

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
| `total` | Long | 总记录数 |
| `current` | Long | 当前页码 |
| `size` | Long | 每页条数 |
| `records` | Array | 当前页数据列表 |

### 1.3 鉴权说明

| 接口类型 | 是否需要登录 | 请求头 |
|---------|------------|--------|
| 登录用户接口 | 需要 | `Authorization: Bearer <accessToken>` |
| 公开访客接口 | 不需要 | 无 |
| 后台管理接口 | 需要 | `Authorization: Bearer <adminAccessToken>` + 后台权限 |

### 1.4 Integer 字段约定

文档中所有 `0/1` 型 Integer 字段含义如下：

| 字段 | 值 | 含义 |
|-----|---|-----|
| `isSpecialFollow` | `0` | 非特别关注 |
| `isSpecialFollow` | `1` | 特别关注 |
| `mutualFollow` | `0` | 非互关 |
| `mutualFollow` | `1` | 互相关注 |
| `followStatus` | `0` | 已取关 |
| `followStatus` | `1` | 已关注 |
| `followerStatus` / `followingStatus` | `0` | 禁用 |
| `followerStatus` / `followingStatus` | `1` | 启用 |
| `followerDeletedFlag` / `followingDeletedFlag` | `0` | 未删除 |
| `followerDeletedFlag` / `followingDeletedFlag` | `1` | 已删除 |

---

## 2. 关注列表页

> 场景：用户在自己的个人中心查看"我关注的用户"列表，支持分页和特别关注筛选。

### 2.1 获取我的关注列表

**接口信息**
- 路径: `GET /api/user/follows`
- 鉴权: 是
- 说明: 分页查询当前用户的关注列表，支持过滤特别关注

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10`，最大 `100` |
| `specialOnly` | Boolean | 否 | `true` 时仅返回特别关注，默认 `false` |

**请求示例**

```javascript
// axios
axios.get('/api/user/follows', {
  params: { current: 1, size: 10 },
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows?current=1&size=10', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "relationId": 1,
        "userId": 100,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://example.com/avatar/100.jpg",
        "isSpecialFollow": 1,
        "remark": "前端联调账号",
        "mutualFollow": 1,
        "followTime": "2026-04-15T10:30:00"
      },
      {
        "relationId": 2,
        "userId": 101,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "https://example.com/avatar/101.jpg",
        "isSpecialFollow": 0,
        "remark": null,
        "mutualFollow": 0,
        "followTime": "2026-04-10T08:20:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `relationId` | Long | 关注关系 ID |
| `userId` | Long | 被关注用户 ID |
| `username` | String | 被关注用户名 |
| `nickname` | String | 被关注用户昵称 |
| `avatar` | String | 被关注用户头像 URL |
| `isSpecialFollow` | Integer | 是否特别关注：`0/1` |
| `remark` | String | 备注 |
| `mutualFollow` | Integer | 是否互关：`0/1` |
| `followTime` | LocalDateTime | 最近关注时间，格式 `yyyy-MM-dd'T'HH:mm:ss` |

**列表排序规则**
- 特别关注的用户优先显示
- 同优先级内按最近关注时间倒序排列

**前端使用建议**

```javascript
// 特别关注筛选
const specialFollows = await axios.get('/api/user/follows', {
  params: { current: 1, size: 100, specialOnly: true },
  headers: { Authorization: `Bearer ${token}` }
});

// 全部关注列表（分页）
const follows = await axios.get('/api/user/follows', {
  params: { current: page, size: 10 },
  headers: { Authorization: `Bearer ${token}` }
});

// 渲染列表
follows.data.data.records.forEach(item => {
  console.log(`${item.nickname} ${item.isSpecialFollow === 1 ? '⭐' : ''}`);
});
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40101` | 用户不存在 | 提示用户信息无效，请重新登录 |

---

### 2.2 关注用户

**接口信息**
- 路径: `POST /api/user/follows/{userId}`
- 鉴权: 是
- 说明: 关注指定用户，幂等操作

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 被关注用户 ID |

**请求示例**

```javascript
// axios
axios.post('/api/user/follows/100', {}, {
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/100', {
  method: 'POST',
  headers: { Authorization: 'Bearer xxx' }
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

**业务规则**
- 已关注时再次关注幂等成功，不重复创建关系
- 已取关时再次关注会恢复旧记录并刷新 `followTime`
- 不能关注自己
- 目标用户不存在、已删除或已禁用时返回业务错误
- 关注成功后会给被关注者异步发送"新粉丝"通知，通知失败不回滚主链路

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40011` | 非法参数（如关注自己） | 提示不能关注自己 |
| `40401` | 目标用户不存在、已删除或已禁用 | 提示该用户不存在或已禁用 |

---

### 2.3 取消关注

**接口信息**
- 路径: `DELETE /api/user/follows/{userId}`
- 鉴权: 是
- 说明: 取消关注指定用户，幂等操作

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 被关注用户 ID |

**请求示例**

```javascript
// axios
axios.delete('/api/user/follows/100', {
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/100', {
  method: 'DELETE',
  headers: { Authorization: 'Bearer xxx' }
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

**业务规则**
- 已取关时再次取关幂等成功
- 取消关注后重新关注会复用原关系记录，不新增第二条关系

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40011` | 关注关系不存在 | 提示取消失败或已取消 |

---

## 3. 粉丝列表页

> 场景：用户在自己的个人中心查看"关注我的用户"列表。

### 3.1 获取我的粉丝列表

**接口信息**
- 路径: `GET /api/user/fans`
- 鉴权: 是
- 说明: 分页查询当前用户的粉丝列表

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10`，最大 `100` |

**请求示例**

```javascript
// axios
axios.get('/api/user/fans', {
  params: { current: 1, size: 10 },
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/fans?current=1&size=10', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "relationId": 3,
        "userId": 200,
        "username": "wangwu",
        "nickname": "王五",
        "avatar": "https://example.com/avatar/200.jpg",
        "isSpecialFollow": 1,
        "remark": null,
        "mutualFollow": 1,
        "followTime": "2026-04-18T14:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `relationId` | Long | 关注关系 ID |
| `userId` | Long | 粉丝用户 ID（关注我的人） |
| `username` | String | 粉丝用户名 |
| `nickname` | String | 粉丝昵称 |
| `avatar` | String | 粉丝头像 URL |
| `isSpecialFollow` | Integer | 我是否特别关注该粉丝：`0/1` |
| `remark` | String | 我对该粉丝的备注 |
| `mutualFollow` | Integer | 是否互关：`0/1` |
| `followTime` | LocalDateTime | 该粉丝关注我的时间 |

**字段语义说明**
- `isSpecialFollow` 和 `remark` 表示"我是否把该粉丝设为特别关注、以及我对他的备注"
- 未互关时 `isSpecialFollow=0`，`remark=null`

**前端使用建议**

```javascript
const fans = await axios.get('/api/user/fans', {
  params: { current: 1, size: 10 },
  headers: { Authorization: `Bearer ${token}` }
});

fans.data.data.records.forEach(fan => {
  const mutualIcon = fan.mutualFollow === 1 ? '🔗' : '';
  const specialIcon = fan.isSpecialFollow === 1 ? '⭐' : '';
  console.log(`${fan.nickname} ${mutualIcon} ${specialIcon}`);
});
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |

---

## 4. 个人主页 - 公开关注列表

> 场景：用户访问他人个人主页，查看该用户的关注列表（无需登录）。

### 4.1 获取指定用户的关注列表

**接口信息**
- 路径: `GET /api/users/{userId}/follows`
- 鉴权: 否
- 说明: 公开接口，无需登录即可访问，返回指定用户的关注列表

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 要查看的用户 ID |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10`，最大 `100` |

**请求示例**

```javascript
// axios（无需鉴权）
axios.get('/api/users/100/follows', {
  params: { current: 1, size: 10 }
})

// fetch
fetch('/api/users/100/follows?current=1&size=10')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "userId": 101,
        "username": "lisi",
        "nickname": "李四",
        "avatar": "https://example.com/avatar/101.jpg",
        "followTime": "2026-04-15T10:30:00"
      },
      {
        "userId": 102,
        "username": "wangwu",
        "nickname": "王五",
        "avatar": "https://example.com/avatar/102.jpg",
        "followTime": "2026-04-10T08:20:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `userId` | Long | 被关注用户 ID |
| `username` | String | 被关注用户名 |
| `nickname` | String | 被关注用户昵称 |
| `avatar` | String | 被关注用户头像 URL |
| `followTime` | LocalDateTime | 关注时间 |

**与登录用户关注列表的差异**

| 字段 | 登录用户接口 | 公开接口 |
|-----|-----------|---------|
| `relationId` | 有 | 无 |
| `isSpecialFollow` | 有 | 无 |
| `remark` | 有 | 无 |
| `mutualFollow` | 有 | 无 |

**前端使用建议**

```javascript
// 在个人主页加载用户关注列表
const userId = 100;
const response = await axios.get(`/api/users/${userId}/follows`, {
  params: { current: 1, size: 10 }
});

if (response.data.code === 200) {
  const { total, records } = response.data.data;
  console.log(`该用户关注了 ${total} 人`);
  records.forEach(user => {
    console.log(`- ${user.nickname} (@${user.username})`);
  });
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `40401` | 目标用户不存在、已删除或已禁用 | 提示该用户不存在 |

---

## 5. 个人主页 - 公开粉丝列表

> 场景：用户访问他人个人主页，查看该用户的粉丝列表（无需登录）。

### 5.1 获取指定用户的粉丝列表

**接口信息**
- 路径: `GET /api/users/{userId}/fans`
- 鉴权: 否
- 说明: 公开接口，无需登录即可访问，返回指定用户的粉丝列表

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 要查看的用户 ID |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10`，最大 `100` |

**请求示例**

```javascript
// axios（无需鉴权）
axios.get('/api/users/100/fans', {
  params: { current: 1, size: 10 }
})

// fetch
fetch('/api/users/100/fans?current=1&size=10')
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "userId": 200,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": "https://example.com/avatar/200.jpg",
        "followTime": "2026-04-18T14:00:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `userId` | Long | 粉丝用户 ID |
| `username` | String | 粉丝用户名 |
| `nickname` | String | 粉丝昵称 |
| `avatar` | String | 粉丝头像 URL |
| `followTime` | LocalDateTime | 该粉丝关注目标用户的时间 |

**前端使用建议**

```javascript
// 在个人主页加载用户粉丝列表
const userId = 100;
const response = await axios.get(`/api/users/${userId}/fans`, {
  params: { current: 1, size: 10 }
});

if (response.data.code === 200) {
  const { total, records } = response.data.data;
  console.log(`该用户有 ${total} 位粉丝`);
  records.forEach(fan => {
    console.log(`- ${fan.nickname} (@${fan.username})`);
  });
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `40401` | 目标用户不存在、已删除或已禁用 | 提示该用户不存在 |

---

## 6. 个人中心 - 互关状态与统计

> 场景：用户个人中心页面加载时展示与目标用户的互关状态、当前关注数和粉丝数。

### 6.1 查询与目标用户的互关状态

**接口信息**
- 路径: `GET /api/user/follows/mutual`
- 鉴权: 是
- 说明: 查询当前登录用户与指定用户之间的互关关系状态

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `targetUserId` | Long | 是 | 目标用户 ID |

**请求示例**

```javascript
// axios
axios.get('/api/user/follows/mutual', {
  params: { targetUserId: 100 },
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/mutual?targetUserId=100', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "targetUserId": 100,
    "following": true,
    "followedBy": true,
    "mutualFollow": true
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `targetUserId` | Long | 目标用户 ID |
| `following` | Boolean | 当前用户是否已关注目标用户 |
| `followedBy` | Boolean | 目标用户是否已关注当前用户 |
| `mutualFollow` | Boolean | 是否互相关注 |

**前端使用建议**

```javascript
// 渲染互关状态
const status = await axios.get('/api/user/follows/mutual', {
  params: { targetUserId: 100 },
  headers: { Authorization: `Bearer ${token}` }
});

const { following, followedBy, mutualFollow } = status.data.data;

if (mutualFollow) {
  console.log('你们互相关注了 🔗');
} else if (following) {
  console.log('你已关注对方');
} else if (followedBy) {
  console.log('对方关注了你，等待你回关');
} else {
  console.log('你们还没有关注关系');
}
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40401` | 目标用户不存在 | 提示用户信息无效 |

---

### 6.2 查询关注数和粉丝数

**接口信息**
- 路径: `GET /api/user/follows/count`
- 鉴权: 是
- 说明: 查询当前登录用户的关注数和粉丝数（实时统计）

**请求示例**

```javascript
// axios
axios.get('/api/user/follows/count', {
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/count', {
  headers: { Authorization: 'Bearer xxx' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "followingCount": 10,
    "fanCount": 5
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `followingCount` | Long | 关注数 |
| `fanCount` | Long | 粉丝数 |

**统计规则**
- 实时 COUNT 统计，过滤掉已删除、已禁用的目标用户
- 不使用用户表冗余字段，每次实时计算

**前端使用建议**

```javascript
const count = await axios.get('/api/user/follows/count', {
  headers: { Authorization: `Bearer ${token}` }
});

// 在个人中心展示
console.log(`关注 ${count.data.data.followingCount} | 粉丝 ${count.data.data.fanCount}`);
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |

---

## 7. 个人中心 - 特别关注与备注

> 场景：用户在个人中心对已关注用户设置特别关注或更新备注。

### 7.1 设置或取消特别关注

**接口信息**
- 路径: `PUT /api/user/follows/{userId}/special`
- 鉴权: 是
- 说明: 设置或取消对指定用户的特别关注

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 被关注用户 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `specialFollow` | Integer | 是 | 特别关注状态：`0` 取消，`1` 设置 |

**请求示例**

```javascript
// axios - 设为特别关注
axios.put('/api/user/follows/100/special', {
  specialFollow: 1
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// axios - 取消特别关注
axios.put('/api/user/follows/100/special', {
  specialFollow: 0
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/100/special', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    Authorization: 'Bearer xxx'
  },
  body: JSON.stringify({ specialFollow: 1 })
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

**业务规则**
- `specialFollow` 仅允许 `0` 或 `1`，其他值返回业务错误
- 仅对当前有效关注关系生效；若尚未关注，返回业务错误

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40011` | 特别关注状态不合法或关注关系不存在 | 提示操作失败 |

---

### 7.2 更新关注备注

**接口信息**
- 路径: `PUT /api/user/follows/{userId}/remark`
- 鉴权: 是
- 说明: 更新对指定用户的关注备注

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `userId` | Long | 是 | 被关注用户 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `remark` | String | 是 | 备注内容，最大 256 字符，传空字符串清空备注 |

**请求示例**

```javascript
// axios - 设置备注
axios.put('/api/user/follows/100/remark', {
  remark: '前端联调账号'
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// axios - 清空备注
axios.put('/api/user/follows/100/remark', {
  remark: ''
}, {
  headers: { Authorization: 'Bearer xxx' }
})

// fetch
fetch('/api/user/follows/100/remark', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    Authorization: 'Bearer xxx'
  },
  body: JSON.stringify({ remark: '前端联调账号' })
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

**业务规则**
- 备注最大长度 256 字符，超过返回业务错误
- 传空白字符串会清空备注
- 仅对当前有效关注关系生效；若尚未关注，返回业务错误

**前端使用建议**

```javascript
// 更新备注
const updateRemark = async (userId, remark) => {
  if (remark.length > 256) {
    console.error('备注长度不能超过256个字符');
    return;
  }
  
  const response = await axios.put(`/api/user/follows/${userId}/remark`, {
    remark
  }, {
    headers: { Authorization: `Bearer ${token}` }
  });
  
  if (response.data.code === 200) {
    console.log('备注更新成功');
  }
};
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40011` | 备注超长或关注关系不存在 | 提示备注长度不能超过256字符或操作失败 |

---

## 8. 后台管理

> 场景：后台运营人员查看关注关系列表、清理异常数据。

### 8.1 分页查询关注关系

**接口信息**
- 路径: `GET /api/sys/follows`
- 鉴权: 是（需要后台权限 `content:follow:query`）
- 说明: 分页查询全站关注关系，用于数据排查

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `current` | Long | 否 | 页码，默认 `1` |
| `size` | Long | 否 | 每页条数，默认 `10`，最大 `100` |
| `followerId` | Long | 否 | 关注者用户 ID |
| `followingId` | Long | 否 | 被关注者用户 ID |
| `followStatus` | Integer | 否 | 关注状态：`0` 已取关，`1` 已关注 |
| `specialFollow` | Integer | 否 | 是否特别关注：`0/1` |
| `source` | String | 否 | 关注来源 |
| `keyword` | String | 否 | 关键词，匹配关注双方用户名或昵称 |

**请求示例**

```javascript
// axios
axios.get('/api/sys/follows', {
  params: { current: 1, size: 10, followStatus: 1 },
  headers: { Authorization: 'Bearer adminToken' }
})

// fetch
fetch('/api/sys/follows?current=1&size=10&followStatus=1', {
  headers: { Authorization: 'Bearer adminToken' }
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": {
    "total": 1,
    "current": 1,
    "size": 10,
    "records": [
      {
        "relationId": 1,
        "followerId": 1,
        "followerUsername": "admin",
        "followerNickname": "管理员",
        "followerStatus": 1,
        "followerDeletedFlag": 0,
        "followingId": 100,
        "followingUsername": "zhangsan",
        "followingNickname": "张三",
        "followingStatus": 1,
        "followingDeletedFlag": 0,
        "followStatus": 1,
        "isSpecialFollow": 0,
        "source": "search",
        "remark": null,
        "followTime": "2026-04-15T10:30:00",
        "unfollowTime": null,
        "createdAt": "2026-04-15T10:30:00",
        "updatedAt": "2026-04-15T10:30:00"
      }
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `relationId` | Long | 关注关系 ID |
| `followerId` | Long | 关注者用户 ID |
| `followerUsername` | String | 关注者用户名 |
| `followerNickname` | String | 关注者昵称 |
| `followerStatus` | Integer | 关注者状态：`0` 禁用，`1` 启用 |
| `followerDeletedFlag` | Integer | 关注者是否已删除：`0/1` |
| `followingId` | Long | 被关注者用户 ID |
| `followingUsername` | String | 被关注者用户名 |
| `followingNickname` | String | 被关注者昵称 |
| `followingStatus` | Integer | 被关注者状态：`0` 禁用，`1` 启用 |
| `followingDeletedFlag` | Integer | 被关注者是否已删除：`0/1` |
| `followStatus` | Integer | 关系状态：`0` 已取关，`1` 已关注 |
| `isSpecialFollow` | Integer | 是否特别关注：`0/1` |
| `source` | String | 关注来源 |
| `remark` | String | 备注 |
| `followTime` | LocalDateTime | 最近关注时间 |
| `unfollowTime` | LocalDateTime | 最近取关时间（未取关时为 `null`） |
| `createdAt` | LocalDateTime | 关系创建时间 |
| `updatedAt` | LocalDateTime | 关系更新时间 |

**后台分页特殊说明**
- 后台分页会保留异常关系（如用户已删除但关系仍存在），用于排查数据问题
- `current` 和 `size` 不传时分别默认为 `1` 和 `10`

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40300` | 无后台权限 | 提示没有访问权限 |

---

### 8.2 异常关注关系清理

**接口信息**
- 路径: `DELETE /api/sys/follows/clean`
- 鉴权: 是（需要后台权限 `content:follow:clean`）
- 说明: 批量清理异常关注关系

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|-----|
| `cleanInactive` | Boolean | 否 | 是否清理已取关关系 |
| `cleanDeletedUsers` | Boolean | 否 | 是否清理任一端已删除或缺失用户的关系 |
| `cleanDisabledUsers` | Boolean | 否 | 是否清理任一端已禁用用户的关系 |

**请求示例**

```javascript
// axios - 清理已取关关系
axios.delete('/api/sys/follows/clean', {
  data: { cleanInactive: true },
  headers: { Authorization: 'Bearer adminToken' }
})

// axios - 清理包含已删除用户的关系
axios.delete('/api/sys/follows/clean', {
  data: { cleanDeletedUsers: true },
  headers: { Authorization: 'Bearer adminToken' }
})

// axios - 清理包含已禁用用户的关系
axios.delete('/api/sys/follows/clean', {
  data: { cleanDisabledUsers: true },
  headers: { Authorization: 'Bearer adminToken' }
})

// axios - 同时清理多个条件
axios.delete('/api/sys/follows/clean', {
  data: { cleanInactive: true, cleanDeletedUsers: true },
  headers: { Authorization: 'Bearer adminToken' }
})

// fetch
fetch('/api/sys/follows/clean', {
  method: 'DELETE',
  headers: {
    'Content-Type': 'application/json',
    Authorization: 'Bearer adminToken'
  },
  body: JSON.stringify({ cleanInactive: true })
})
```

**响应示例**

```json
{
  "code": 200,
  "message": "成功",
  "timestamp": 1774310400000,
  "data": 15
}
```

**响应说明**
- `data` 为本次清理涉及的关系数量（Long 类型）

**业务规则**
- 三个条件至少要有一个为 `true`，否则返回业务错误
- 删除动作只收口异常关系，不参与普通用户前台取关流程

**前端使用建议**

```javascript
// 执行清理前二次确认
const confirmClean = async (type) => {
  const requestBody = { [type]: true };
  
  const confirmed = window.confirm(`确定要清理${type === 'cleanInactive' ? '已取关' : type === 'cleanDeletedUsers' ? '已删除用户' : '已禁用用户'}关系吗？`);
  if (!confirmed) return;
  
  const response = await axios.delete('/api/sys/follows/clean', {
    data: requestBody,
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  
  if (response.data.code === 200) {
    alert(`清理完成，共清理 ${response.data.data} 条异常关系`);
  }
};
```

**错误码**

| code | 说明 | 前端处理 |
|-----|------|---------|
| `401` | 未登录或 Token 失效 | 跳转登录页 |
| `40300` | 无后台权限 | 提示没有访问权限 |
| `40011` | 清理条件不能为空或至少指定一个条件 | 提示至少选择一个清理条件 |

---

## 9. 错误码速查

| code | 说明 | 常见场景 | 前端处理 |
|-----|------|---------|---------|
| `200` | 成功 | 操作成功 | 正常流程 |
| `40011` | 非法参数 | 关注自己、特别关注状态不合法、备注超长、清理条件为空 | 提示具体错误原因 |
| `401` | 未认证 | 未登录、Token 失效、Token 格式错误 | 跳转登录页 |
| `40300` | 没有访问权限 | 无后台权限 | 提示没有访问权限 |
| `40400` | 请求的接口不存在 | 路径错误 | 检查接口路径 |
| `40401` | 用户不存在 | 目标用户不存在、已删除或已禁用 | 提示用户不存在 |
| `42900` | 请求过于频繁 | 触发限流 | 提示稍后再试 |

---

## 附录：接口路径速查

### 登录用户接口

| 接口 | 路径 |
|-----|-----|
| 关注用户 | `POST /api/user/follows/{userId}` |
| 取消关注 | `DELETE /api/user/follows/{userId}` |
| 我的关注列表 | `GET /api/user/follows` |
| 我的粉丝列表 | `GET /api/user/fans` |
| 互关状态 | `GET /api/user/follows/mutual?targetUserId={id}` |
| 关注统计 | `GET /api/user/follows/count` |
| 设置/取消特别关注 | `PUT /api/user/follows/{userId}/special` |
| 更新备注 | `PUT /api/user/follows/{userId}/remark` |

### 公开访客接口

| 接口 | 路径 |
|-----|-----|
| 查看指定用户关注列表 | `GET /api/users/{userId}/follows` |
| 查看指定用户粉丝列表 | `GET /api/users/{userId}/fans` |

### 后台管理接口

| 接口 | 路径 | 权限 |
|-----|-----|-----|
| 分页查询关注关系 | `GET /api/sys/follows` | `content:follow:query` |
| 异常关系清理 | `DELETE /api/sys/follows/clean` | `content:follow:clean` |

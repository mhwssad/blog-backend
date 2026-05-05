# Auth 登录流程

## 1. 登录方式概览

| 登录方式 | 接口 | 说明 |
|----------|------|------|
| 账号密码登录 | `POST /api/auth/login` | 用户名/邮箱 + 密码 |
| 邮箱验证码登录 | `POST /api/auth/email-login` | 邮箱 + 验证码 |
| Token 刷新 | `POST /api/auth/refresh` | refreshToken 换 accessToken |

## 2. 账号密码登录流程

```
┌─────────┐                                  ┌──────────────┐
│  Client │                                  │   Backend    │
└────┬────┘                                  └──────┬───────┘
     │                                             │
     │  POST /api/auth/login                      │
     │  { username, password }                     │
     │ ─────────────────────────────────────────▶ │
     │                                             │
     │                                             ├──► 校验登录失败次数（Redis）
     │                                             ├──► 校验账号状态
     │                                             ├──► AuthenticationManager.authenticate()
     │                                             │      └──► Spring Security 认证
     │                                             ├──► 失败次数清理
     │                                             ├──► TokenManager.generateToken()
     │                                             │      └──► 生成 accessToken + refreshToken
     │                                             ├──► 更新登录信息（IP、时间）
     │                                             └──► 发布用户登录事件
     │                                             │
     │  { accessToken, refreshToken, expiresIn }   │
     │ ◀───────────────────────────────────────── │
     │                                             │
```

### 2.1 登录失败处理

- **失败计数器**：基于 IP + username 组合，Redis Key = `login:fail:{ip}:{userId}`
- **锁定机制**：连续失败 5 次后锁定 15 分钟
- **失败清理**：登录成功后清除失败计数

### 2.2 Token 结构

| 字段 | 说明 |
|------|------|
| tokenType | Bearer |
| accessToken | JWT 访问令牌（默认 2 小时有效期） |
| refreshToken | JWT 刷新令牌（默认 7 天有效期） |
| expiresIn | accessToken 剩余有效期（秒） |

## 3. 邮箱验证码登录流程

```
┌─────────┐                                  ┌──────────────┐
│  Client │                                  │   Backend    │
└────┬────┘                                  └──────┬───────┘
     │                                             │
     │  POST /api/auth/email-code                  │
     │  { email }                                  │
     │ ─────────────────────────────────────────▶ │
     │                                             │
     │                                             ├──► 6位随机验证码生成
     │                                             ├──► Redis Key = login:email:code:{email}
     │                                             │      TTL = 5分钟
     │                                             └──► EmailService 发送邮件
     │                                             │
     │  200 OK                                     │
     │ ◀───────────────────────────────────────── │
     │                                             │
     │  POST /api/auth/email-login                 │
     │  { email, code }                            │
     │ ─────────────────────────────────────────▶ │
     │                                             │
     │                                             ├──► 验证码校验（Redis）
     │                                             ├──► 邮箱用户查找
     │                                             ├──► EmailCodeAuthenticationProvider
     │                                             │      └──► 认证成功后删除验证码
     │                                             ├──► TokenManager.generateToken()
     │                                             └──► 更新登录信息
     │                                             │
     │  { accessToken, refreshToken, expiresIn }   │
     │ ◀───────────────────────────────────────── │
     │                                             │
```

### 3.1 验证码机制

- **生成规则**：6 位数字，SecureRandom 生成
- **存储位置**：Redis，Key = `login:email:code:{email}`
- **有效期**：5 分钟
- **使用限制**：一次性，使用后删除

## 4. Token 刷新流程

```
┌─────────┐                                  ┌──────────────┐
│  Client │                                  │   Backend    │
└────┬────┘                                  └──────┬───────┘
     │                                             │
     │  POST /api/auth/refresh                    │
     │  { refreshToken }                           │
     │ ─────────────────────────────────────────▶ │
     │                                             │
     │                                             ├──► TokenManager.validateRefreshToken()
     │                                             │      └──► JWT 签名校验 + 类型校验
     │                                             ├──► 解析 refreshToken 获取用户信息
     │                                             └──► TokenManager.generateToken()
     │                                                  └──► 生成新的 accessToken + refreshToken
     │                                             │
     │  { accessToken, refreshToken, expiresIn }   │
     │ ◀───────────────────────────────────────── │
     │                                             │
```

## 5. 退出登录流程

```
┌─────────┐                                  ┌──────────────┐
│  Client │                                  │   Backend    │
└────┬────┘                                  └──────┬───────┘
     │                                             │
     │  POST /api/auth/logout                      │
     │  { accessToken } 或 Authorization Header    │
     │ ─────────────────────────────────────────▶ │
     │                                             │
     │                                             ├──► Token 解析
     │                                             ├──► 用户会话失效（Redis 黑名单）
     │                                             │      Key = token:blacklist:{userId}
     │                                             │      TTL = accessToken 剩余有效期
     │                                             └──► TokenManager.invalidateUserSessions()
     │                                                  └──► 标记用户所有 Token 失效
     │                                             │
     │  200 OK                                     │
     │ ◀───────────────────────────────────────── │
     │                                             │
```

## 6. Token 黑名单机制

### 6.1 JWT Token 黑名单

当用户退出登录或被强制下线时，将用户 ID 加入黑名单：

```
Key: token:blacklist:{userId}
Value: 当前时间戳（秒）
TTL: accessToken 剩余有效期
```

### 6.2 Token 校验流程

```
1. 解析 JWT Token
2. 从 Token 中提取 userId
3. 查询 Redis: GET token:blacklist:{userId}
4. 若存在且 Token 签发时间 < 黑名单加入时间 → Token 无效
5. 否则 → Token 有效
```

## 7. 关键组件

### 7.1 TokenManager 接口

| 方法 | 说明 |
|------|------|
| generateToken(Authentication) | 生成 accessToken + refreshToken |
| parseToken(String) | 解析 Token 获取 Authentication |
| validateToken(String) | 校验 Token 有效性（含黑名单） |
| validateRefreshToken(String) | 校验 RefreshToken |
| refreshToken(String) | 用 RefreshToken 换新 Token |
| invalidateUserSessions(Long) | 使指定用户所有会话失效 |

### 7.2 TokenManager 实现

| 实现类 | 条件 | 说明 |
|--------|------|------|
| JwtTokenManager | `security.session.type=jwt` | JWT 存储，Redis 做黑名单 |
| RedisTokenManager | `security.session.type=redis` | Token 完全存储在 Redis |

### 7.3 Security Filter Chain

```
请求 → TokenAuthenticationFilter
         ├──► 提取 Token（Header / RequestBody）
         ├──► TokenManager.parseToken()
         ├──► TokenManager.validateToken()
         └──► 设置 SecurityContextHolder

       → 其他 Filter...

       → AuthenticationManager.authenticate()
            ├──► DaoAuthenticationProvider（密码登录）
            └──► EmailCodeAuthenticationProvider（邮箱验证码登录）
```

## 8. 相关接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 账号密码登录 |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/email-code` | POST | 发送邮箱验证码 |
| `/api/auth/email-login` | POST | 邮箱验证码登录 |
| `/api/auth/refresh` | POST | 刷新 Token |
| `/api/auth/logout` | POST | 退出登录 |
| `/api/auth/current-user` | GET | 获取当前用户 |
| `/api/auth/current-user-menus` | GET | 获取当前用户菜单 |

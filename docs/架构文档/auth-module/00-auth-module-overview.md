# Auth 模块总览

## 1. 模块定位

Auth 模块（`module/auth`）是博客后端的核心安全服务层，负责：

- **账号管理**：用户注册、登录、邮箱验证、密码重置
- **认证授权**：JWT/Redis Token 管理、Spring Security 集成
- **RBAC 权限**：角色管理、菜单权限、用户-角色-菜单关联
- **经验等级**：用户经验值、等级计算、等级权益
- **通知体系**：系统通知、用户通知、通知偏好设置
- **作者申请**：作者资质申请、申请审核、作者权限授予
- **审计日志**：操作日志、审计追踪

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户侧接口                               │
│   POST /api/auth/login      POST /api/auth/register             │
│   POST /api/auth/email-login  GET /api/auth/current-user        │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                              │
│   AuthController          UserProfileController                │
│   PasswordResetController  PublicUserSearchController          │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│  │   AuthService  │  │ TwoFactorService│  │PasswordResetSvc│   │
│  │   登录/注册/登出 │  │   MFA 验证码    │  │  密码重置      │   │
│  └────────────────┘  └────────────────┘  └────────────────┘   │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│  │SysUserAdminSvc │  │UserProfileSvc  │  │AccountTakeover│   │
│  │  用户管理       │  │  个人资料       │  │  接管登录      │   │
│  └────────────────┘  └────────────────┘  └────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Repository    │   │  Token Manager │   │  Spring Sec     │
│   数据访问       │   │  JWT/Redis    │   │  Authentication │
└─────────────────┘   └─────────────────┘   └─────────────────┘
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  MySQL          │   │  Redis          │   │  Email Service  │
│  持久化存储       │   │  Token/黑名单   │   │  邮件发送       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 3. 目录结构

```
module/auth/
├── account/               # 账号管理子域
│   ├── authentication/    # Spring Security 认证 Provider
│   ├── controller/         # 控制器
│   ├── convert/           # MapStruct 转换器
│   ├── model/             # 请求/响应模型
│   │   ├── admin/         # 后台管理请求/VO
│   │   └── user/          # 用户侧请求/VO
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务服务
│   │   └── impl/          # 服务实现
│   └── token/             # Token 管理（JWT/Redis）
├── rbac/                  # RBAC 权限子域
│   ├── controller/       # 控制器
│   ├── convert/          # MapStruct 转换器
│   ├── model/            # 请求/响应模型
│   │   └── admin/        # 后台管理请求/VO
│   ├── repository/       # 数据访问层
│   └── service/          # 业务服务
├── experience/            # 经验等级子域
│   ├── constant/         # 常量定义
│   ├── controller/       # 控制器
│   ├── convert/          # MapStruct 转换器
│   ├── event/            # Spring Event
│   ├── level/            # 等级计算
│   ├── model/            # 请求/响应模型
│   │   ├── admin/        # 后台管理请求/VO
│   │   ├── data/         # 内部数据传输对象
│   │   └── user/         # 用户侧请求/VO
│   ├── repository/       # 数据访问层
│   └── service/         # 业务服务
├── notice/               # 通知子域
│   ├── controller/      # 控制器
│   ├── convert/         # MapStruct 转换器
│   ├── model/           # 请求/响应模型
│   │   ├── admin/       # 后台管理请求/VO
│   │   └── user/        # 用户侧请求/VO
│   ├── repository/      # 数据访问层
│   └── service/        # 业务服务
├── author/              # 作者申请子域
│   ├── controller/      # 控制器
│   ├── convert/        # MapStruct 转换器
│   ├── model/          # 请求/响应模型
│   │   ├── admin/      # 后台管理请求/VO
│   │   └── user/       # 用户侧请求/VO
│   ├── repository/     # 数据访问层
│   └── service/       # 业务服务
├── config/             # 系统配置子域
│   ├── controller/    # 控制器
│   ├── convert/      # MapStruct 转换器
│   ├── model/        # 请求/响应模型
│   │   └── admin/    # 后台管理请求/VO
│   ├── repository/   # 数据访问层
│   └── service/     # 业务服务
└── audit/            # 审计日志子域
    ├── controller/   # 控制器
    ├── convert/     # MapStruct 转换器
    ├── model/      # 请求/响应模型
    │   ├── admin/  # 后台管理请求/VO
    │   └── common/ # 通用请求/VO
    ├── repository/ # 数据访问层
    └── service/   # 业务服务
```

## 4. 核心能力矩阵

| 能力 | 组件 | 状态 |
|------|------|------|
| 账号登录 | AuthService | ✅ 完成 |
| 邮箱验证码登录 | AuthService + EmailCodeAuthenticationProvider | ✅ 完成 |
| Token 管理 | JwtTokenManager / RedisTokenManager | ✅ 完成 |
| MFA 二次验证 | TwoFactorService | ✅ 完成 |
| 密码重置 | PasswordResetService | ✅ 完成 |
| 接管登录 | AccountTakeoverService | ✅ 完成 |
| 用户管理 | SysUserAdminService | ✅ 完成 |
| 个人资料 | UserProfileService | ✅ 完成 |
| 公开用户搜索 | PublicUserSearchService | ✅ 完成 |
| 角色管理 | SysRoleAdminService | ✅ 完成 |
| 菜单管理 | SysMenuAdminService | ✅ 完成 |
| 用户-角色分配 | SysRoleAdminService | ✅ 完成 |
| 经验值管理 | UserExperienceService | ✅ 完成 |
| 经验等级计算 | LevelCalculator | ✅ 完成 |
| 系统通知 | SysNoticeAdminService | ✅ 完成 |
| 用户通知 | UserNoticeInboxService | ✅ 完成 |
| 通知偏好 | UserNotificationSettingService | ✅ 完成 |
| 作者申请 | UserAuthorApplicationService | ✅ 完成 |
| 作者申请审核 | SysAuthorApplicationAdminService | ✅ 完成 |
| 系统配置 | SysConfigService | ✅ 完成 |
| 审计日志 | SysAuditLogService | ✅ 完成 |
| 操作日志 | SysLogAdminService | ✅ 完成 |

## 5. 扩展方向

- **社交登录**：微信、GitHub 第三方登录集成
- **LDAP 集成**：企业账号体系对接
- **操作规则引擎**：基于经验值的动态权益控制
- **通知渠道扩展**：短信、PUSH 通知渠道

## 6. 相关文档

- [Auth 登录流程](./auth-login-flow.md)
- [Auth 数据模型](./auth-data-model.md)
- [Auth RBAC 系统](./auth-rbac-system.md)
- [Auth 经验等级体系](./auth-experience-system.md)
- [Auth 通知体系](./auth-notice-system.md)
- [Auth 作者申请体系](./auth-author-system.md)
